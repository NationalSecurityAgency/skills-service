/**
 * Copyright 2026 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.intTests.catalog

import com.github.kagkarlsson.scheduler.task.TaskInstance
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.intTests.utils.QuizDefFactory
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.repos.ProjectParticipationRepo
import skills.storage.repos.UserPointsRepo.ImportedCreditDiscrepancy
import skills.tasks.data.CatalogFinalizeRequest
import skills.tasks.data.CatalogParticipantBackfillRequest
import skills.tasks.data.ImportedSkillAchievement
import spock.lang.IgnoreRest
import spock.util.concurrent.PollingConditions

import javax.sql.DataSource
import java.sql.Connection
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Stream

import static skills.intTests.utils.SkillsFactory.*

/**
 * Regression cases for catalog participation races, asserting required outcomes before and after fixes.
 * Test-only advice pauses real calls or injects an interrupted-run/acknowledgment failure; it does
 * not manufacture credit or replace the production copy, propagation, or scheduler logic.
 * Final assertions state the required outcome, rather than asserting the known defect.
 */
@TestPropertySource(properties = [
        'db-scheduler.threads=3',
        'skills.config.taskMaxRetries=2',
        'skills.config.exponentialBackOffSeconds=1',
        'skills.config.exponentialBackOffRate=1',
])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(BoundaryConfiguration)
class CatalogParticipantRaceFailuresIT extends CatalogIntSpec {
    @Autowired RaceBoundaries boundaries
    @Autowired ProjectParticipationRepo participationRepo
    @Autowired DataSource dataSource

    private List<String> users
    private List<Gate> gates = []
    private Map<String, String> triggers = [:]
    private List<Connection> lockConnections = []
    private ExecutorService callers
    private final Date historicalDate = new Date(System.currentTimeMillis() - 3 * 86400000L)

    def setup() {
        boundaries.hooks.clear()
        users = getRandomUsers(5)
        callers = Executors.newFixedThreadPool(3)
    }

    def cleanup() {
        // Always release barriers so failed race assertions do not strand requests or workers.
        gates.each { Gate gate -> gate.open() }
        lockConnections.each { Connection connection ->
            connection.createStatement().execute('select pg_advisory_unlock_all()')
            connection.close()
        }
        callers.shutdown()
        assert callers.awaitTermination(30, TimeUnit.SECONDS)
        triggers.each { String name, String table -> removeTrigger(name, table) }
        jdbcTemplate.update("update scheduled_tasks set execution_time = now() where task_name in ('catalog-participant-backfill', 'imported-skill-achievement', 'finalize-catalog-imports', 'remove-skill-events-for-user')")
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        boundaries.hooks.clear()
    }

    def "a visit qualified using old import state must receive the concurrently finalized batch"() {
        Map fixture = fixture()
        SkillsService learner = createService(users[0])
        skillsService.addSkill(fixture.next, users[0], historicalDate)
        importNext(fixture)
        Gate beforeActivation = gate()
        intercept('BatchOperationsTransactionalAccessor.completeCatalogFinalization', [fixture.projectId]) { BoundaryInvocation invocation ->
            beforeActivation.pause()
            invocation.callRealMethod()
        }
        long qualificationLock = 4073101L
        holdAdvisoryLock(qualificationLock)
        trigger('race_qualification', 'project_participation', "if NEW.project_id = '${fixture.projectId}' and NEW.user_id = '${users[0]}' then perform pg_advisory_xact_lock(${qualificationLock}); end if; return NEW;")

        when: 'qualification has chosen COMPLETED against the old, empty enabled import'
        Future<Map> qualification = submit { learner.initializeProjectProgress(fixture.projectId) }
        awaitAdvisoryWait(qualificationLock)
        Future<Map> finalizationRequest = submit { skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false) }
        // A fixed start boundary waits for qualification. Without that protection, let
        // bulk processing reach its old pre-activation boundary before releasing the visit.
        new PollingConditions(timeout: 15).eventually {
            assert beforeActivation.hasArrived() || jdbcTemplate.queryForObject('''
                select count(*) from pg_stat_activity finalization
                where finalization.datname = current_database()
                  and finalization.wait_event_type = 'Lock'
                  and lower(finalization.query) like '%f_select_lock_and_insert%'
                  and exists(select 1 from pg_locks qualification
                      where qualification.locktype = 'advisory'
                        and qualification.objid = ? and not qualification.granted
                        and qualification.pid = any(pg_blocking_pids(finalization.pid)))
                ''', Integer, qualificationLock) > 0
        }
        releaseAdvisoryLock(qualificationLock)
        Map initialized = qualification.get(20, TimeUnit.SECONDS)
        Map finalized = finalizationRequest.get(20, TimeUnit.SECONDS)
        beforeActivation.awaitArrival()
        beforeActivation.open()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then: 'bulk or catch-up must own the new participant without another visit'
        initialized.backfillStatus == 'COMPLETED'
        finalized.body.success
        summary.points == 100
    }

    def "concurrent finalization requests must admit only one run"() {
        Map fixture = fixture()
        importNext(fixture)
        queueTasks('finalize-catalog-imports')
        Gate firstRequestBeforeStateWrite = gate()
        AtomicBoolean pauseFirstAdmission = new AtomicBoolean(true)
        intercept('SettingsService.saveSetting', [{ Object request ->
            request.projectId == fixture.projectId && request.setting == 'catalog.finalize.state' && request.value == 'RUNNING'
        }]) { BoundaryInvocation invocation ->
            if (pauseFirstAdmission.compareAndSet(true, false)) {
                firstRequestBeforeStateWrite.pause()
            }
            invocation.callRealMethod()
        }
        SkillsService otherAdmin = createService(skillsService.userName)

        when:
        Future<Map> first = submit { requestOutcome { skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false) } }
        firstRequestBeforeStateWrite.awaitArrival()
        Future<Map> second = submit { requestOutcome { otherAdmin.finalizeSkillsImportFromCatalog(fixture.projectId, false) } }
        // Without serialization, let the second request finish so the regression fails
        // on the outcome assertions rather than timing out at a lock-wait barrier.
        new PollingConditions(timeout: 15).eventually {
            assert second.isDone() || jdbcTemplate.queryForObject(
                    "select count(*) from pg_stat_activity where datname = current_database() and wait_event_type = 'Lock' and lower(query) like '%f_select_lock_and_insert%'", Integer) > 0
        }
        firstRequestBeforeStateWrite.open()
        List<Map> outcomes = [first.get(25, TimeUnit.SECONDS), second.get(25, TimeUnit.SECONDS)]
        Map rejected = outcomes.find { Map outcome -> !outcome.success }
        int admittedTasks = taskCount('finalize-catalog-imports')

        then: 'the losing request is rejected specifically because finalization is already running'
        outcomes.count { Map outcome -> outcome.success } == 1
        outcomes.count { Map outcome -> !outcome.success } == 1
        rejected.status == HttpStatus.BAD_REQUEST
        rejected.response.body.success == false
        rejected.response.body.projectId == fixture.projectId
        rejected.response.body.explanation == "Catalog import finalize is already running for [${fixture.projectId}]"
        admittedTasks == 1
    }

    def "bulk aggregate replacement must retain a local award committed while it waits for the row"() {
        Map fixture = fixture()
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        skillsService.addSkill(fixture.local, users[0], historicalDate)
        skillsService.addSkill(fixture.next, users[0], historicalDate)
        importNext(fixture)
        Gate localAwardBeforeCommit = gate()
        Gate bulkBeforeAggregates = gate()
        intercept('CatalogParticipantBackfillService.qualify', [fixture.projectId, users[0]]) { BoundaryInvocation invocation ->
            localAwardBeforeCommit.pause()
            invocation.callRealMethod()
        }
        intercept('BatchOperationsTransactionalAccessor.handleCatalogPointsAndAchievementsForSubject', []) { BoundaryInvocation invocation ->
            bulkBeforeAggregates.pause()
            invocation.callRealMethod()
        }

        when: 'definition updates finish before the local transaction takes foreign-key row locks'
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        bulkBeforeAggregates.awaitArrival()
        Future<Map> localAward = submit { skillsService.addSkill(fixture.otherLocal, users[0]).body }
        localAwardBeforeCommit.awaitArrival()
        // Sum before the local award commits, then verify the bulk UPDATE waits on its aggregate row.
        bulkBeforeAggregates.open()
        awaitSqlLockWait('%pointstoupdate%')
        localAwardBeforeCommit.open()
        localAward.get(20, TimeUnit.SECONDS)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then: 'two native 100-point awards plus the new imported 100 points'
        summary.subjects[0].points == 300
        summary.points == 300
    }

    def "an older enabled-only backfill must not replace totals for the new pre-activation batch"() {
        Map fixture = fixture()
        SkillsService learner = createService(users[0])
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        skillsService.addSkill(fixture.next, users[0], historicalDate)
        importNext(fixture)
        Gate oldWorkerBeforeAggregates = gate()
        Gate newRunBeforeActivation = gate()
        AtomicReference<Integer> firstCopiedPointId = new AtomicReference<>()
        intercept('BatchOperationsTransactionalAccessor.createSubjectUserPointsForSingleNewUser', [users[0], fixture.projectId, fixture.subjectId]) { BoundaryInvocation invocation ->
            Integer copiedPointId = userPointsRepo.findByProjectIdAndUserIdAndSkillId(fixture.projectId, users[0], fixture.source.skillId).id
            firstCopiedPointId.compareAndSet(null, copiedPointId)
            oldWorkerBeforeAggregates.pause()
            invocation.callRealMethod()
        }
        intercept('BatchOperationsTransactionalAccessor.completeCatalogFinalization', [fixture.projectId]) { BoundaryInvocation invocation ->
            newRunBeforeActivation.pause()
            invocation.callRealMethod()
        }

        when:
        learner.initializeProjectProgress(fixture.projectId)
        oldWorkerBeforeAggregates.awaitArrival()
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        newRunBeforeActivation.awaitArrival()
        if (finalizationCompletesBeforeWorker) {
            newRunBeforeActivation.open()
            awaitFinalization(fixture.projectId, 'COMPLETED')
        }
        oldWorkerBeforeAggregates.open()
        BackfillStatus statusBeforeActivation = null
        Integer oldPointsBeforeActivation = null
        if (!finalizationCompletesBeforeWorker) {
            // Also allow the broken worker to complete so the regression fails on an
            // outcome assertion instead of waiting forever for a retry that never occurs.
            new PollingConditions(timeout: 20).eventually {
                assert participationRepo.readStatus(fixture.projectId, users[0]) == BackfillStatus.COMPLETED ||
                        backfillTasks().any { Map task -> task.consecutive_failures > 0 }
            }
            statusBeforeActivation = participationRepo.readStatus(fixture.projectId, users[0])
            oldPointsBeforeActivation = userPointsRepo.findByProjectIdAndUserIdAndSkillId(
                    fixture.projectId, users[0], fixture.source.skillId)?.points ?: 0
        }
        newRunBeforeActivation.open()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then: 'both imported skills have 100 earned points'
        summary.subjects[0].points == 200
        summary.points == 200
        participationRepo.readStatus(fixture.projectId, users[0]) == BackfillStatus.COMPLETED
        // The overtaken attempt must roll back its copied row, including when A has
        // already completed and only the changed enabled-import set exposes the overlap.
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(fixture.projectId, users[0], fixture.source.skillId).id != firstCopiedPointId.get()
        finalizationCompletesBeforeWorker || statusBeforeActivation == BackfillStatus.PENDING
        finalizationCompletesBeforeWorker || oldPointsBeforeActivation == 0

        where:
        finalizationCompletesBeforeWorker << [false, true]
    }

    def "a queued source event already copied by #entry backfill must not add its points again"() {
        Map fixture = fixture()
        SkillsService learner = createService(users[0])
        queueTasks('imported-skill-achievement')
        skillsService.addSkill(fixture.source, users[0], historicalDate)

        when:
        qualify(fixture, learner, entry)
        awaitStatus(fixture.projectId, users[0], BackfillStatus.COMPLETED)
        releaseTasks('imported-skill-achievement')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then:
        summary.points == (entry == 'award' ? 200 : 100)

        where:
        entry << ['visit', 'award']
    }

    def "an older queued source request must reconcile the latest completion without adding credit again"() {
        Map fixture = fixture()
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        Gate oldRequest = gate()
        AtomicBoolean pauseFirst = new AtomicBoolean(true)
        intercept('ImportedSkillAchievementTaskExecutor.execute', []) { BoundaryInvocation invocation ->
            if (pauseFirst.compareAndSet(true, false)) {
                oldRequest.pause()
            }
            invocation.callRealMethod()
        }

        when:
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        oldRequest.awaitArrival()
        skillsService.addSkill(fixture.source, users[0], new Date(historicalDate.time + 1000))
        skillsService.addSkill(fixture.source, users[0], new Date(historicalDate.time + 2000))
        new PollingConditions(timeout: 20).eventually {
            assert learner.getSkillsSummaryForCurrentUser(fixture.projectId).points == 300
        }
        oldRequest.open()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then:
        summary.points == 300
        summary.subjects[0].points == 300
        summary.skillsAchieved == 1
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], fixture.projectId, fixture.source.skillId).size() == 1
    }

    def "a queued source award deleted before propagation must not restore deleted credit"() {
        Map fixture = fixture(true, 1)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        queueTasks('imported-skill-achievement')
        skillsService.addSkill(fixture.source, users[0], historicalDate)

        when:
        skillsService.deleteSkillEvent(fixture.source + [userId: users[0], timestamp: historicalDate.time])
        releaseTasks('imported-skill-achievement')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then:
        summary.points == 0
        summary.subjects[0].points == 0
        summary.skillsAchieved == 0
    }

    def "a delayed source reset request must retain credit earned after the reset"() {
        Map fixture = fixture(true, 1)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        queueTasks('remove-skill-events-for-user')

        when:
        skillsService.deleteAllSkillEvents([projectId: fixture.source.projectId, userId: users[0]])
        Date newDate = new Date(historicalDate.time + 1000)
        skillsService.addSkill(fixture.source, users[0], newDate)
        releaseTasks('remove-skill-events-for-user')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then:
        summary.points == 100
        summary.subjects[0].points == 100
        summary.skillsAchieved == 1
        List<UserAchievement> awards = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], fixture.projectId, fixture.source.skillId)
        awards.size() == 1
        awards.first().achievedOn == newDate
    }

    def "reconciliation updates same-project reuse when its imported source credit changes"() {
        Map fixture = fixture()
        SkillsService learner = createService(users[0])
        Map secondSubject = createSubject(2, 2)
        skillsService.createSubject(secondSubject)
        skillsService.reuseSkillInAnotherSubject(fixture.projectId, fixture.source.skillId, secondSubject.subjectId)
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        learner.initializeProjectProgress(fixture.projectId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        assert learner.getSkillsSummaryForCurrentUser(fixture.projectId).points == 200

        when:
        skillsService.addSkill(fixture.source, users[0], new Date(historicalDate.time + 1000))
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then:
        summary.points == 400
        summary.subjects.find { Map subject -> subject.subjectId == fixture.subjectId }.points == 200
        summary.subjects.find { Map subject -> subject.subjectId == secondSubject.subjectId }.points == 200
        summary.skillsAchieved == 0
    }

    def "finalization replay must not increment an event already included in bulk copy"() {
        Map fixture = fixture(false)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        Gate beforePointsCopy = gate()
        intercept('BatchOperationsTransactionalAccessor.copySkillUserPointsToTheImportedProjects', [fixture.projectId]) { BoundaryInvocation invocation ->
            beforePointsCopy.pause()
            invocation.callRealMethod()
        }

        when:
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        beforePointsCopy.awaitArrival()
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        beforePointsCopy.open()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then:
        summary.points == 100
    }

    def "a source event after activation must not be delivered by both ordinary scheduling and replay"() {
        Map fixture = fixture(false)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        Gate committedActivationBeforeReturn = gate()
        intercept('BatchOperationsTransactionalAccessor.completeCatalogFinalization', [fixture.projectId]) { BoundaryInvocation invocation ->
            invocation.callRealMethod()
            committedActivationBeforeReturn.pause()
            null
        }

        when: 'activation is committed but the finalization end/replay window has not closed'
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        committedActivationBeforeReturn.awaitArrival()
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        committedActivationBeforeReturn.open()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then:
        summary.points == 100
    }

    def "a disabled import must not receive source credit until activation"() {
        Map fixture = fixture(false, 1)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)

        when: 'the source completes while its destination copy is disabled'
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map disabledSummary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then:
        learner.getSingleSkillSummaryForCurrentUser(fixture.source.projectId, fixture.source.skillId).points == 100
        disabledSummary.points == 0
        disabledSummary.subjects[0].points == 0
        disabledSummary.skillsAchieved == 0
        // Disabled credit is not exposed in learner summaries; verify no hidden rows were awarded.
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(fixture.projectId, users[0], fixture.source.skillId) == null
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], fixture.projectId, fixture.source.skillId).isEmpty()

        when: 'activation makes that source credit applicable'
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map activatedSummary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then:
        activatedSummary.points == 100
        activatedSummary.subjects[0].points == 100
        activatedSummary.skillsAchieved == 1
    }

    def "a source award uncommitted during activation catch-up must schedule synchronization after activation"() {
        Map fixture = fixture(false)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        Gate sourceBeforeCommit = gate()
        intercept('CatalogParticipantBackfillService.qualify', [fixture.source.projectId, users[0]]) { BoundaryInvocation invocation ->
            sourceBeforeCommit.pause()
            invocation.callRealMethod()
        }

        when: 'the source has evaluated ordinary scheduling, but its credit/tasks remain uncommitted'
        Future<Map> sourceAward = submit { skillsService.addSkill(fixture.source, users[0], historicalDate).body }
        sourceBeforeCommit.awaitArrival()
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        awaitFinalization(fixture.projectId, 'COMPLETED')
        sourceBeforeCommit.open()
        assert sourceAward.get(20, TimeUnit.SECONDS).skillApplied
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then:
        summary.points == 100
        summary.subjects[0].points == 100
    }

    def "source completion between points and achievement copy must create only one destination skill award"() {
        Map fixture = fixture(false, 2)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        Gate beforeAchievementCopy = gate()
        intercept('BatchOperationsTransactionalAccessor.copySkillAchievementsToTheImportedProjects', [fixture.projectId]) { BoundaryInvocation invocation ->
            beforeAchievementCopy.pause()
            invocation.callRealMethod()
        }

        when:
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        beforeAchievementCopy.awaitArrival()
        skillsService.addSkill(fixture.source, users[0], new Date(historicalDate.time + 1000))
        beforeAchievementCopy.open()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)
        List<UserAchievement> skillAwards = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], fixture.projectId, fixture.source.skillId)

        then:
        summary.points == 200
        // The API does not expose duplicate persistence rows for a single skill completion.
        skillAwards.size() == 1
    }

    def "retrying a failed bulk run must recover source events from its earlier copy window"() {
        Map fixture = fixture(false)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        Gate afterCommittedPoints = gate()
        AtomicBoolean failFirstRun = new AtomicBoolean(true)
        intercept('BatchOperationsTransactionalAccessor.copySkillAchievementsToTheImportedProjects', [fixture.projectId]) { BoundaryInvocation invocation ->
            if (failFirstRun.compareAndSet(true, false)) {
                afterCommittedPoints.pause()
                throw new IllegalStateException('Injected interruption after the independently committed points copy')
            }
            invocation.callRealMethod()
        }

        when:
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        afterCommittedPoints.awaitArrival()
        skillsService.addSkill(fixture.source, users[0], new Date(historicalDate.time + 1000))
        afterCommittedPoints.open()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        assert skillsService.getSetting(fixture.projectId, 'catalog.finalize.state').value == 'FAILED'
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId)
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then:
        summary.points == 200
    }

    def "scheduler retry after activation but before replay must recover the original run's uncopied event"() {
        Map fixture = fixture(false)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        Gate afterCommittedPoints = gate()
        intercept('BatchOperationsTransactionalAccessor.copySkillAchievementsToTheImportedProjects', [fixture.projectId]) { BoundaryInvocation invocation ->
            afterCommittedPoints.pause()
            invocation.callRealMethod()
        }
        AtomicBoolean interruptBeforeReplay = new AtomicBoolean(true)
        intercept('SkillCatalogFinalizationService.applyEventsThatWereReportedDuringTheFinalizationRun', []) { BoundaryInvocation invocation ->
            if (interruptBeforeReplay.compareAndSet(true, false)) {
                throw new IllegalStateException('Injected interruption after activation commit, before replay')
            }
            invocation.callRealMethod()
        }

        when:
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        afterCommittedPoints.awaitArrival()
        skillsService.addSkill(fixture.source, users[0], new Date(historicalDate.time + 1000))
        afterCommittedPoints.open()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then:
        summary.points == 200
    }

    def "a later finalization must not lose pending catch-up when the old project sweep is awaiting acknowledgment"() {
        Map fixture = fixture(false)
        SkillsService firstLearner = createService(users[0])
        SkillsService lateLearner = createService(users[1])
        [users[0], users[1]].each { String userId -> skillsService.addSkill(fixture.source, userId, historicalDate) }
        Gate firstCopy = gate()
        Gate secondCopy = gate()
        Gate oldSweepBeforeAcknowledgment = gate()
        AtomicInteger copies = new AtomicInteger()
        intercept('BatchOperationsTransactionalAccessor.copySkillUserPointsToTheImportedProjects', [fixture.projectId]) { BoundaryInvocation invocation ->
            (copies.incrementAndGet() == 1 ? firstCopy : secondCopy).pause()
            invocation.callRealMethod()
        }
        AtomicBoolean stopOldSweep = new AtomicBoolean(true)
        intercept('CatalogParticipantBackfillExecutor.execute', []) { BoundaryInvocation invocation ->
            TaskInstance<CatalogParticipantBackfillRequest> task = invocation.getArgument(0)
            invocation.callRealMethod()
            if (!task.data.userId && stopOldSweep.compareAndSet(true, false)) {
                oldSweepBeforeAcknowledgment.pause()
            }
            null
        }

        when:
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        firstCopy.awaitArrival()
        firstLearner.initializeProjectProgress(fixture.projectId)
        firstCopy.open()
        oldSweepBeforeAcknowledgment.awaitArrival()
        skillsService.addSkill(fixture.next, users[1], historicalDate)
        importNext(fixture)
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        secondCopy.awaitArrival()
        lateLearner.initializeProjectProgress(fixture.projectId)
        secondCopy.open()
        awaitFinalization(fixture.projectId, 'COMPLETED')
        oldSweepBeforeAcknowledgment.open()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = lateLearner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then: 'the late learner needs both the old and new imported skill'
        summary.points == 200
        participationRepo.readStatus(fixture.projectId, users[1]) == BackfillStatus.COMPLETED
    }

    def "expected waiting during finalization must not exhaust incremental work for a previously enabled import"() {
        Map fixture = fixture()
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        importNext(fixture)
        Gate lengthyFinalization = gate()
        intercept('BatchOperationsTransactionalAccessor.copySkillUserPointsToTheImportedProjects', [fixture.projectId]) { BoundaryInvocation invocation ->
            lengthyFinalization.pause()
            invocation.callRealMethod()
        }

        when:
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        lengthyFinalization.awaitArrival()
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        awaitTaskCount('imported-skill-achievement', 0)
        lengthyFinalization.open()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then: 'finalizing the other skill does not copy or replay this enabled skill'
        summary.points == 100
    }

    def "incremental redelivery after credit commit but before acknowledgment must not add credit twice"() {
        Map fixture = fixture()
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        AtomicBoolean interruptAcknowledgment = new AtomicBoolean(true)
        intercept('ImportedSkillAchievementTaskExecutor.execute', []) { BoundaryInvocation invocation ->
            invocation.callRealMethod()
            if (interruptAcknowledgment.compareAndSet(true, false)) {
                throw new IllegalStateException('Injected interruption after credit commit, before task acknowledgment')
            }
            null
        }

        when:
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then:
        summary.points == 100
    }

    def "historical quiz awards and a concurrent visit must both commit despite opposite lock order"() {
        Map fixture = fixture()
        List<SkillsService> learners = users.take(2).collect { String userId -> createService(userId) }
        Map quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(1, 1, 2))
        learners.each { SkillsService learner -> passQuiz(learner, quiz) }
        Gate batchAfterFirstQualification = gate()
        AtomicBoolean pauseFirstQualification = new AtomicBoolean(true)
        AtomicReference<String> firstUser = new AtomicReference<>()
        intercept('CatalogParticipantBackfillService.qualify', [fixture.projectId]) { BoundaryInvocation invocation ->
            BackfillStatus status = invocation.callRealMethod()
            if (pauseFirstQualification.compareAndSet(true, false)) {
                firstUser.set(invocation.getArgument(1) as String)
                batchAfterFirstQualification.pause()
            }
            status
        }

        when:
        // Unlike the ordinary bulk-report loop, historical quiz assignment retains one
        // enclosing skill-save transaction across both users' qualification calls.
        Future<Map> batch = submit { requestOutcome {
            skillsService.createSkill(fixture.local + [selfReportingType: 'Quiz', quizId: quiz.quizId, numPerformToCompletion: 1])
        } }
        batchAfterFirstQualification.awaitArrival()
        SkillsService visitor = learners.find { SkillsService learner -> learner.userName != firstUser.get() }
        Future<Map> visit = submit { requestOutcome { visitor.initializeProjectProgress(fixture.projectId) } }
        awaitSqlLockWait('%f_select_lock_and_insert%')
        batchAfterFirstQualification.open()
        List<Map> outcomes = [batch.get(25, TimeUnit.SECONDS), visit.get(25, TimeUnit.SECONDS)]
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then: 'an HTTP database/deadlock error is an outcome assertion, not an uncaught test error'
        outcomes.every { Map outcome -> outcome.success }
        visitor.getSkillsSummaryForCurrentUser(fixture.projectId).points == 100
    }

    def "achievement-only source credit must not be classified as empty during #entry qualification"() {
        Map fixture = fixture()
        SkillsService learner = createService(users[0])
        SkillDef sourceDef = skillDefRepo.findByProjectIdAndSkillId(fixture.source.projectId, fixture.source.skillId)
        userAchievedRepo.save(new UserAchievement(userId: users[0], projectId: sourceDef.projectId,
                skillId: sourceDef.skillId, skillRefId: sourceDef.id, pointsWhenAchieved: sourceDef.totalPoints,
                achievedOn: historicalDate))

        when:
        qualify(fixture, learner, entry)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then:
        summary.skillsAchieved == 1

        where:
        entry << ['visit', 'award']
    }

    def "an initialized learner's incremental worker must retain a batch finalized after its state check"() {
        Map fixture = fixture(true, 1)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        assert participationRepo.readStatus(fixture.projectId, users[0]) == BackfillStatus.COMPLETED
        skillsService.addSkill(fixture.next, users[0], historicalDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        importNext(fixture)
        Gate incrementalAfterStateCheck = gate()
        Gate bulkBeforeActivation = gate()
        AtomicBoolean pauseIncremental = new AtomicBoolean(true)
        AtomicReference<Integer> firstReconciledPointId = new AtomicReference<>()
        intercept('CatalogImportedCreditService.reconcile', [users[0], { Object skill ->
            skill.projectId == fixture.projectId && skill.skillId == fixture.source.skillId
        }]) { BoundaryInvocation invocation ->
            // The handler has already checked finalization and terminal participation.
            if (pauseIncremental.compareAndSet(true, false)) {
                incrementalAfterStateCheck.pause()
            }
            invocation.callRealMethod()
            firstReconciledPointId.compareAndSet(null, userPointsRepo.findByProjectIdAndUserIdAndSkillId(
                    fixture.projectId, users[0], fixture.source.skillId).id)
            null
        }
        intercept('BatchOperationsTransactionalAccessor.completeCatalogFinalization', [fixture.projectId]) { BoundaryInvocation invocation ->
            bulkBeforeActivation.pause()
            invocation.callRealMethod()
        }

        when:
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        incrementalAfterStateCheck.awaitArrival()
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        bulkBeforeActivation.awaitArrival()
        if (finalizationCompletesBeforeWorker) {
            bulkBeforeActivation.open()
            awaitFinalization(fixture.projectId, 'COMPLETED')
        }
        incrementalAfterStateCheck.open()
        // Permit either acknowledgment/handoff or a retryable rollback before activation.
        if (!finalizationCompletesBeforeWorker) {
            new PollingConditions(timeout: 20).eventually {
                assert taskCount('imported-skill-achievement') == 0 || jdbcTemplate.queryForObject('''
                    select count(*) from scheduled_tasks
                    where task_name = 'imported-skill-achievement' and consecutive_failures > 0
                    ''', Integer) > 0
            }
        }
        bulkBeforeActivation.open()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)
        Map originalImport = learner.getSingleSkillSummaryForCurrentUser(fixture.projectId, fixture.source.skillId)
        Map newImport = learner.getSingleSkillSummaryForCurrentUser(fixture.projectId, fixture.next.skillId)

        then: 'matching skill rows must also have exact committed aggregates and levels'
        originalImport.points == 100
        newImport.points == 100
        summary.subjects[0].points == 200
        summary.points == 200
        summary.skillsLevel == 1
        summary.subjects[0].skillsLevel == 1
        summary.skillsAchieved == 1
        // Even an already activated batch must invalidate and roll back the older attempt.
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(fixture.projectId, users[0], fixture.source.skillId).id != firstReconciledPointId.get()

        where:
        finalizationCompletesBeforeWorker << [false, true]
    }

    def "a source commit after an empty-import finalization snapshot must retain durable delivery"() {
        Map fixture = fixture(true, 1)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        Gate completionAfterSnapshot = gate()
        Gate sourceTaskBeforeAcknowledgment = gate()
        AtomicInteger completionPid = new AtomicInteger()
        AtomicBoolean pauseSnapshot = new AtomicBoolean(true)
        intercept('UserPointsRepo.findCatalogCreditDiscrepancies', [fixture.projectId]) { BoundaryInvocation invocation ->
            // Exhaust the real query before the source commits; preserve precisely its
            // returned snapshot while pausing inside the completion transaction.
            List<ImportedCreditDiscrepancy> snapshot = invocation.callRealMethod().withCloseable { Stream<ImportedCreditDiscrepancy> stream ->
                stream.toList()
            }
            if (pauseSnapshot.compareAndSet(true, false)) {
                completionPid.set(jdbcTemplate.queryForObject('select pg_backend_pid()', Integer))
                completionAfterSnapshot.pause()
            }
            snapshot.stream()
        }
        Integer importedId = skillDefRepo.findByProjectIdAndSkillId(fixture.projectId, fixture.source.skillId).id
        AtomicBoolean pauseDelivery = new AtomicBoolean(true)
        intercept('ImportedSkillAchievementTaskExecutor.execute', []) { BoundaryInvocation invocation ->
            TaskInstance<ImportedSkillAchievement> task = invocation.getArgument(0)
            invocation.callRealMethod()
            if (task.data.rawSkillId == importedId && task.data.userId == users[0] && pauseDelivery.compareAndSet(true, false)) {
                sourceTaskBeforeAcknowledgment.pause()
            }
            null
        }

        when: 'an explicit run with no disabled imports still publishes RUNNING and a completion handoff'
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        completionAfterSnapshot.awaitArrival()
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        // A correct activation fence can block delivery; an unfenced worker can return.
        awaitBoundaryOrLockWait(sourceTaskBeforeAcknowledgment, completionPid.get())
        completionAfterSnapshot.open()
        sourceTaskBeforeAcknowledgment.open()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)
        Map sourceSummary = learner.getSingleSkillSummaryForCurrentUser(fixture.source.projectId, fixture.source.skillId)
        List<UserAchievement> awards = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], fixture.projectId, fixture.source.skillId)

        then:
        sourceSummary.points == 100
        summary.points == 100
        summary.subjects[0].points == 100
        summary.skillsAchieved == 1
        awards.size() == 1
        awards.first().achievedOn == historicalDate
    }

    def "historical quiz assignment and finalization admission must commit without a project-init lock cycle"() {
        Map fixture = fixture()
        SkillsService learner = createService(users[0])
        SkillsService otherAdmin = createService(skillsService.userName)
        Map quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(1, 1, 2))
        passQuiz(learner, quiz)
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        importNext(fixture)
        Gate skillSaveHoldingProject = gate()
        AtomicInteger skillSavePid = new AtomicInteger()
        AtomicBoolean pauseSkillSave = new AtomicBoolean(true)
        intercept('LockingService.lockProject', [fixture.projectId]) { BoundaryInvocation invocation ->
            Object result = invocation.callRealMethod()
            if (pauseSkillSave.compareAndSet(true, false)) {
                skillSavePid.set(jdbcTemplate.queryForObject('select pg_backend_pid()', Integer))
                skillSaveHoldingProject.pause()
            }
            result
        }

        when: 'skill save holds project before qualification, while admission attempts that project lock'
        Future<Map> assignment = submit { requestOutcome {
            skillsService.createSkill(fixture.local + [selfReportingType: 'Quiz', quizId: quiz.quizId, numPerformToCompletion: 1])
        } }
        skillSaveHoldingProject.awaitArrival()
        Future<Map> admission = submit { requestOutcome { otherAdmin.finalizeSkillsImportFromCatalog(fixture.projectId, false) } }
        new PollingConditions(timeout: 15).eventually {
            assert admission.isDone() || hasLockWaitBlockedBy(skillSavePid.get())
        }
        skillSaveHoldingProject.open()
        List<Map> outcomes = [assignment.get(25, TimeUnit.SECONDS), admission.get(25, TimeUnit.SECONDS)]
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)
        Map quizSkill = learner.getSingleSkillSummaryForCurrentUser(fixture.projectId, fixture.local.skillId)

        then: 'database lock errors are captured HTTP outcomes, not uncaught future failures'
        outcomes.every { Map outcome -> outcome.success }
        quizSkill.points == 100
        summary.subjects[0].points == 200
        summary.points == 200
        participationRepo.readStatus(fixture.projectId, users[0]) == BackfillStatus.COMPLETED
    }

    def "an older finalization retry must not complete a newly admitted run"() {
        Map fixture = fixture(false)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(fixture.projectId)
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        skillsService.addSkill(fixture.next, users[0], historicalDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Gate staleRetryBeforeCompletion = gate()
        Gate newRunBeforeCopy = gate()
        AtomicBoolean failOriginalReplay = new AtomicBoolean(true)
        AtomicReference<String> originalTaskId = new AtomicReference<>()
        AtomicInteger originalExecutions = new AtomicInteger()
        AtomicBoolean retryReturned = new AtomicBoolean()
        intercept('FinalizeCatalogSkillsImportExecutor.execute', []) { BoundaryInvocation invocation ->
            TaskInstance<CatalogFinalizeRequest> task = invocation.getArgument(0)
            originalTaskId.compareAndSet(null, task.id)
            boolean original = task.id == originalTaskId.get()
            int attempt = original ? originalExecutions.incrementAndGet() : 0
            invocation.callRealMethod()
            if (original && attempt > 1) {
                retryReturned.set(true)
            }
            null
        }
        intercept('SkillCatalogFinalizationService.applyEventsThatWereReportedDuringTheFinalizationRun', []) { BoundaryInvocation invocation ->
            if (failOriginalReplay.compareAndSet(true, false)) {
                throw new IllegalStateException('Injected interruption after activation, before original task acknowledgment')
            }
            invocation.callRealMethod()
        }
        intercept('BatchOperationsTransactionalAccessor.completeCatalogFinalization', [fixture.projectId, { Object imports -> imports.isEmpty() }]) { BoundaryInvocation invocation ->
            // The older retry has already observed no disabled imports. A correct
            // recovery path may acknowledge it without calling completion at all.
            staleRetryBeforeCompletion.pause()
            invocation.callRealMethod()
        }
        Integer nextSourceId = skillDefRepo.findByProjectIdAndSkillId(fixture.next.projectId, fixture.next.skillId).id
        intercept('BatchOperationsTransactionalAccessor.copySkillUserPointsToTheImportedProjects', [fixture.projectId, [nextSourceId]]) { BoundaryInvocation invocation ->
            newRunBeforeCopy.pause()
            invocation.callRealMethod()
        }

        when:
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        new PollingConditions(timeout: 20).eventually {
            assert staleRetryBeforeCompletion.hasArrived() || retryReturned.get()
        }
        importNext(fixture)
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        newRunBeforeCopy.awaitArrival()
        staleRetryBeforeCompletion.open()
        new PollingConditions(timeout: 20).eventually { assert retryReturned.get() }
        String stateWhileNewRunIsPaused = skillsService.getSetting(fixture.projectId, 'catalog.finalize.state').value
        newRunBeforeCopy.open()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser(fixture.projectId)

        then: 'only the newly admitted run may publish its completion'
        stateWhileNewRunIsPaused == 'RUNNING'
        skillsService.getSetting(fixture.projectId, 'catalog.finalize.state').value == 'COMPLETED'
        summary.subjects[0].points == 200
        summary.points == 200
    }

    def "a finishing sweep must preserve later pending ownership when source credit is #credit"() {
        Map fixture = fixture(false)
        SkillsService firstLearner = createService(users[0])
        SkillsService lateLearner = createService(users[1])
        skillsService.addSkill(fixture.source, users[0], historicalDate)
        if (credit == 'already matching bulk credit') {
            // Point-only evidence qualifies this learner for both bulk runs without
            // creating participation before any enabled import or running finalization.
            skillsService.addSkill(fixture.local, users[1], historicalDate)
            skillsService.addSkill(fixture.source, users[1], historicalDate)
            skillsService.addSkill(fixture.next, users[1], historicalDate)
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        assert participationRepo.readStatus(fixture.projectId, users[1]) == null
        Gate firstCopy = gate()
        Gate secondCopy = gate()
        Gate oldSweepBeforeAcknowledgment = gate()
        AtomicInteger copies = new AtomicInteger()
        intercept('BatchOperationsTransactionalAccessor.copySkillUserPointsToTheImportedProjects', [fixture.projectId]) { BoundaryInvocation invocation ->
            (copies.incrementAndGet() == 1 ? firstCopy : secondCopy).pause()
            invocation.callRealMethod()
        }
        AtomicBoolean pauseOldSweep = new AtomicBoolean(true)
        intercept('CatalogParticipantBackfillExecutor.execute', []) { BoundaryInvocation invocation ->
            TaskInstance<CatalogParticipantBackfillRequest> task = invocation.getArgument(0)
            invocation.callRealMethod()
            if (!task.data.userId && pauseOldSweep.compareAndSet(true, false)) {
                oldSweepBeforeAcknowledgment.pause()
            }
            null
        }

        when:
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        firstCopy.awaitArrival()
        firstLearner.initializeProjectProgress(fixture.projectId)
        firstCopy.open()
        oldSweepBeforeAcknowledgment.awaitArrival()
        assert participationRepo.readStatus(fixture.projectId, users[1]) == null
        importNext(fixture)
        skillsService.finalizeSkillsImportFromCatalog(fixture.projectId, false)
        secondCopy.awaitArrival()
        Map deferred = lateLearner.initializeProjectProgress(fixture.projectId)
        secondCopy.open()
        awaitFinalization(fixture.projectId, 'COMPLETED')
        awaitTaskCount('finalize-catalog-imports', 0)
        oldSweepBeforeAcknowledgment.open()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = lateLearner.getSkillsSummaryForCurrentUser(fixture.projectId)
        Map refreshed = lateLearner.initializeProjectProgress(fixture.projectId)

        then: 'completion must not depend on a discrepancy task or another qualifying award'
        deferred.backfillStatus == 'PENDING'
        deferred.ready == false
        summary.subjects[0].points == expectedPoints
        summary.points == expectedPoints
        refreshed.backfillStatus == 'COMPLETED'
        refreshed.ready == true

        where:
        credit                         | expectedPoints
        'empty'                        | 0
        'already matching bulk credit' | 300
    }

    private Map fixture(boolean finalize = true, int occurrences = 3) {
        Map source = createSkill(1, 1, 11, 0, occurrences, 0, 100)
        Map next = createSkill(1, 1, 12, 0, 3, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [source, next])
        [source, next].each { Map skill -> skillsService.exportSkillToCatalog(skill.projectId, skill.skillId) }
        Map local = createSkill(2, 1, 21, 0, 5, 0, 100)
        Map otherLocal = createSkill(2, 1, 22, 0, 5, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(2), createSubject(2, 1), [local, otherLocal])
        skillsService.bulkImportSkillsFromCatalog('TestProject2', 'TestSubject1', [source])
        if (finalize) {
            skillsService.finalizeSkillsImportFromCatalog('TestProject2')
        }
        [projectId: 'TestProject2', subjectId: 'TestSubject1', source: source, next: next, local: local, otherLocal: otherLocal]
    }

    private void importNext(Map fixture) {
        skillsService.bulkImportSkillsFromCatalog(fixture.projectId, fixture.subjectId, [fixture.next])
    }

    private Map qualify(Map fixture, SkillsService learner, String entry) {
        entry == 'visit' ? learner.initializeProjectProgress(fixture.projectId) : skillsService.addSkill(fixture.local, learner.userName).body
    }

    private static void passQuiz(SkillsService learner, Map quiz) {
        Map attempt = learner.startQuizAttempt(quiz.quizId).body
        learner.reportQuizAnswer(quiz.quizId, attempt.id, attempt.questions[0].answerOptions[0].id)
        assert learner.completeQuizAttempt(quiz.quizId, attempt.id).body.passed
    }

    private Future<Map> submit(Closure<Map> action) {
        callers.submit(action as Callable<Map>)
    }

    private static Map requestOutcome(Closure action) {
        try {
            [success: true, response: action.call()]
        } catch (SkillsClientException exception) {
            [success: false, status: exception.httpStatus, response: exception.res,
             error: exception.resBody ?: exception.message]
        }
    }

    private Gate gate() {
        Gate result = new Gate()
        gates.add(result)
        result
    }

    private void trigger(String name, String table, String body) {
        triggers[name] = table
        // An interrupted IDE run may leave this spec's named barrier behind.
        removeTrigger(name, table)
        installTrigger(name, table, body)
    }

    private void queueTasks(String taskName) {
        String name = queueTriggerName(taskName)
        trigger(name, 'scheduled_tasks', "if NEW.task_name = '${taskName}' then NEW.execution_time := now() + interval '1 hour'; end if; return NEW;")
    }

    private void releaseTasks(String taskName) {
        String name = queueTriggerName(taskName)
        removeTrigger(name, triggers.remove(name))
        jdbcTemplate.update('update scheduled_tasks set execution_time = now() where task_name = ?', taskName)
    }

    private static String queueTriggerName(String taskName) {
        'race_queue_' + taskName.replace('-', '_')
    }

    private int taskCount(String taskName) {
        jdbcTemplate.queryForObject('select count(*) from scheduled_tasks where task_name = ?', Integer, taskName)
    }

    private void awaitTaskCount(String taskName, int expected) {
        new PollingConditions(timeout: 20).eventually { assert taskCount(taskName) == expected }
    }

    private void awaitStatus(String projectId, String userId, BackfillStatus expected) {
        new PollingConditions(timeout: 20).eventually { assert participationRepo.readStatus(projectId, userId) == expected }
    }

    private void awaitFinalization(String projectId, String expected) {
        new PollingConditions(timeout: 20).eventually { assert skillsService.getSetting(projectId, 'catalog.finalize.state').value == expected }
    }

    private void awaitSqlLockWait(String queryPattern) {
        new PollingConditions(timeout: 15).eventually {
            assert jdbcTemplate.queryForObject("select count(*) from pg_stat_activity where datname = current_database() and wait_event_type = 'Lock' and lower(query) like ?", Integer, queryPattern) > 0
        }
    }

    private boolean hasLockWaitBlockedBy(int blockingPid) {
        jdbcTemplate.queryForObject('''
            select count(*) from pg_stat_activity
            where datname = current_database() and wait_event_type = 'Lock'
              and ? = any(pg_blocking_pids(pid))
            ''', Integer, blockingPid) > 0
    }

    private void awaitBoundaryOrLockWait(Gate boundary, int blockingPid) {
        new PollingConditions(timeout: 15).eventually {
            assert boundary.hasArrived() || hasLockWaitBlockedBy(blockingPid)
        }
    }

    private void holdAdvisoryLock(long key) {
        Connection connection = dataSource.connection
        connection.createStatement().execute("select pg_advisory_lock(${key})")
        lockConnections.add(connection)
    }

    private void releaseAdvisoryLock(long key) {
        lockConnections.each { Connection connection -> connection.createStatement().execute("select pg_advisory_unlock(${key})") }
    }

    private void awaitAdvisoryWait(long key) {
        new PollingConditions(timeout: 15).eventually {
            assert jdbcTemplate.queryForObject("select count(*) from pg_locks where locktype = 'advisory' and objid = ? and not granted", Integer, key) > 0
        }
    }

    private void intercept(String method, List expectedArguments, Closure action) {
        boundaries.hooks[method] = { BoundaryInvocation invocation ->
            boolean matches = expectedArguments.withIndex().every { Object expected, int index ->
                Object actual = invocation.getArgument(index)
                expected instanceof Closure ? expected.call(actual) : expected == actual
            }
            matches ? action.call(invocation) : invocation.callRealMethod()
        }
    }

    @TestConfiguration
    static class BoundaryConfiguration {
        @Bean
        RaceBoundaries catalogRaceBoundaries() { new RaceBoundaries() }
    }

    // Outside Spring's transaction advice: proceed() returns after a standalone worker/stage commits.
    @Aspect
    @Order(0)
    static class RaceBoundaries {
        final Map<String, Closure> hooks = new ConcurrentHashMap<>()

        @Around('''execution(* skills.services.admin.BatchOperationsTransactionalAccessor.*(..)) ||
                execution(* skills.services.CatalogParticipantBackfillService.qualify(..)) ||
                execution(* skills.services.CatalogImportedCreditService.reconcile(..)) ||
                execution(* skills.services.LockingService.lockProject(..)) ||
                execution(* skills.storage.repos.UserPointsRepo.findCatalogCreditDiscrepancies(..)) ||
                execution(* skills.services.settings.SettingsService.saveSetting(..)) ||
                execution(* skills.services.admin.SkillCatalogFinalizationService.applyEventsThatWereReportedDuringTheFinalizationRun(..)) ||
                execution(* skills.tasks.executors.FinalizeCatalogSkillsImportExecutor.execute(..)) ||
                execution(* skills.tasks.executors.CatalogParticipantBackfillExecutor.execute(..)) ||
                execution(* skills.tasks.executors.ImportedSkillAchievementTaskExecutor.execute(..))''')
        Object atBoundary(ProceedingJoinPoint joinPoint) {
            String method = "${joinPoint.signature.declaringType.simpleName}.${joinPoint.signature.name}"
            Closure hook = hooks[method]
            hook ? hook.call(new BoundaryInvocation(joinPoint: joinPoint)) : joinPoint.proceed()
        }
    }

    private static class BoundaryInvocation {
        ProceedingJoinPoint joinPoint

        Object callRealMethod() { joinPoint.proceed() }
        Object getArgument(int index) { joinPoint.args[index] }
    }

    private static class Gate {
        private final CountDownLatch arrived = new CountDownLatch(1)
        private final CountDownLatch released = new CountDownLatch(1)

        void pause() {
            arrived.countDown()
            assert released.await(30, TimeUnit.SECONDS): 'Test boundary was not released'
        }

        void awaitArrival() {
            assert arrived.await(20, TimeUnit.SECONDS): 'Production call did not reach the requested boundary'
        }

        boolean hasArrived() { arrived.count == 0 }

        void open() { released.countDown() }
    }
}
