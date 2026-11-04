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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.SkillDef

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Temporary pre-5.1 fault/overlap acceptance, using the shared integration context. */
@Import(ControlsConfiguration)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogParticipationCleanupIT extends CatalogCleanupIntSpec {
    @Autowired CleanupControls controls
    ExecutorService callers

    def setup() {
        controls.pointDeleteCalls.clear()
        controls.achievementDeleteCalls.clear()
    }

    def cleanup() {
        controls.release.countDown()
        controls.backfillRelease.countDown()
        controls.failProject = null
        controls.pauseDelete = false
        controls.pauseAchievements = false
        controls.pauseRepair = false
        controls.pauseBackfill = false
        if (callers) {
            callers.shutdown()
            if (!callers.awaitTermination(90, TimeUnit.SECONDS)) {
                callers.shutdownNow()
                assert callers.awaitTermination(30, TimeUnit.SECONDS)
            }
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
    }

    def "failure after point deletes rolls back one project while earlier and later projects commit"() {
        Map setup = fixture(3)
        List<String> users = getRandomUsers(2)
        setup.destinations.each { Map destination -> users.each { String user -> seedResidue(destination, user) } }
        Map before = users.collectEntries { String user -> [(user): snapshot('TestProject3', user)] }
        controls.failProject = 'TestProject3'
        SkillsService root = createRootSkillService()
        ListAppender<ILoggingEvent> logs = captureCleanupLogs()

        when:
        Map result = root.cleanupImportedSkillProgress([projectIds: setup.destinations*.projectId])

        then:
        result.projects*.status == ['CLEANED', 'FAILED', 'CLEANED']
        result.projects[1].deletedRecords == 0
        result.projects[1].deletedPointRows == 0
        result.projects[1].deletedAchievementRows == 0
        setup.destinations.every { Map destination ->
            controls.pointDeleteCalls[destination.projectId].get() == 1 &&
                    controls.achievementDeleteCalls[destination.projectId].get() == 1
        }
        cleanupOutcomeLogs(logs).size() == 3
        ILoggingEvent failureLog = cleanupOutcomeLogs(logs).find { ILoggingEvent event -> event.level == Level.ERROR }
        failureLog.formattedMessage.contains('projectId=[TestProject3] status=[FAILED] reason=[Project transaction rolled back')
        failureLog.formattedMessage.contains('elapsedMs=[')
        failureLog.formattedMessage.endsWith('deletedRecords=[0] deletedPointRows=[0] deletedAchievementRows=[0]')
        failureLog.throwableProxy != null
        cleanupOutcomeLogs(logs).findAll { ILoggingEvent event -> event.level == Level.INFO }.every { ILoggingEvent event ->
            event.formattedMessage.contains('status=[CLEANED]') && event.formattedMessage.endsWith('deletedRecords=[14] deletedPointRows=[6] deletedAchievementRows=[8]')
        }
        users.every { String user -> snapshot('TestProject3', user) == before[user] &&
                snapshot('TestProject2', user) == [points: [], awards: []] && snapshot('TestProject4', user) == [points: [], awards: []] }

        when:
        controls.failProject = null
        Map retry = root.cleanupImportedSkillProgress([projectIds: ['TestProject3']])

        then:
        retry.deletedRecords == 14
        retry.deletedPointRows == 6
        retry.deletedAchievementRows == 8
    }

    def "overlapping cleanup calls count only their actual committed deletions"() {
        Map setup = fixture(1)
        List<String> users = getRandomUsers(2)
        users.each { String user -> seedResidue(setup.destinations[0], user) }
        SkillsService firstRoot = createRootSkillService()
        SkillsService secondRoot = createRootSkillService()
        controls.armDelete()
        callers = Executors.newSingleThreadExecutor()

        when:
        Future<Map> first = callers.submit({ firstRoot.cleanupImportedSkillProgress([projectIds: ['TestProject2']]) } as Callable<Map>)
        assert controls.arrived.await(60, TimeUnit.SECONDS)
        Map second = secondRoot.cleanupImportedSkillProgress([projectIds: ['TestProject2']])
        controls.release.countDown()
        Map resumed = first.get(60, TimeUnit.SECONDS)

        then:
        second.projects*.status == ['CLEANED']
        second.deletedRecords == 14
        second.deletedPointRows == 6
        second.deletedAchievementRows == 8
        resumed.projects*.status == ['SKIPPED']
        resumed.deletedRecords == 0
        resumed.deletedPointRows == 0
        resumed.deletedAchievementRows == 0
        users.every { String user -> snapshot('TestProject2', user) == [points: [], awards: []] }
    }

    def "membership committed before deletes protects #qualification at deletion time"() {
        Map setup = fixture(1)
        List<String> users = getRandomUsers(2)
        SkillsService learner = createService(users[0])
        SkillsService root = createRootSkillService()
        setup.destinations.each { Map destination -> users.each { String user -> seedResidue(destination, user) } }
        skillsService.addSkill(setup.source, users[0])
        seedGlobal(users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        List<Map> global = globalSnapshot(users[0])
        controls.armDelete()
        callers = Executors.newSingleThreadExecutor()

        when:
        Future<Map> cleanup = callers.submit({ root.cleanupImportedSkillProgress([projectIds: ['TestProject2']]) } as Callable<Map>)
        assert controls.arrived.await(60, TimeUnit.SECONDS)
        if (qualification == 'native award') {
            skillsService.addSkill(setup.destinations[0].nativeSkill, users[0])
        } else {
            learner.initializeProjectProgress('TestProject2')
        }
        // Real worker can complete before deletion starts; cleanup holds no credit/init lock.
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map participant = membership('TestProject2', users[0])
        Map committed = snapshot('TestProject2', users[0])
        controls.release.countDown()
        Map result = cleanup.get(60, TimeUnit.SECONDS)

        then:
        result.deletedRecords == 7
        snapshot('TestProject2', users[0]) == committed
        membership('TestProject2', users[0]) == participant
        globalSnapshot(users[0]) == global
        snapshot('TestProject2', users[1]) == [points: [], awards: []]
        skillsService.getSkillSummary(users[0], 'TestProject2').points == (qualification == 'native award' ? 200 : 100)

        where:
        qualification << ['native award', 'participation and backfill']
    }

    def "membership committed between the two deletes preserves achievements and supports explicit repair"() {
        Map setup = fixture(1)
        String user = getRandomUsers(1)[0]
        skillsService.addSkill(setup.source, user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        seedResidue(setup.destinations[0], user)
        seedGlobal(user)
        Map source = snapshot(setup.source.projectId, user)
        List<Map> global = globalSnapshot(user)
        List<Map> awards = snapshot('TestProject2', user).awards
        SkillsService learner = createService(user)
        SkillsService root = createRootSkillService()
        queueBackfill()
        controls.armAchievements()
        callers = Executors.newSingleThreadExecutor()

        when:
        Future<Map> cleanup = callers.submit({ root.cleanupImportedSkillProgress([projectIds: ['TestProject2']]) } as Callable<Map>)
        assert controls.arrived.await(60, TimeUnit.SECONDS)
        Map initialized = learner.initializeProjectProgress('TestProject2')
        Map participant = membership('TestProject2', user)
        controls.release.countDown()
        Map result = cleanup.get(60, TimeUnit.SECONDS)

        then:
        initialized.backfillStatus == 'PENDING'
        !initialized.ready
        result.deletedRecords == 3
        result.deletedPointRows == 3
        result.deletedAchievementRows == 0
        snapshot('TestProject2', user) == [points: [], awards: awards]
        membership('TestProject2', user) == participant
        globalSnapshot(user) == global
        snapshot(setup.source.projectId, user) == source

        when:
        root.fixImportedSkillProgress('TestProject2', user)

        then:
        skillsService.getSkillSummary(user, 'TestProject2').points == 100
        membership('TestProject2', user) == participant
        globalSnapshot(user) == global

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        membership('TestProject2', user).status == BackfillStatus.COMPLETED
        skillsService.getSkillSummary(user, 'TestProject2').points == 100

        cleanup:
        controls.release.countDown()
        releaseBackfill()
    }

    def "native award committed between deletes preserves native credit and achievements while removing the other user"() {
        Map setup = fixture(1)
        Map destination = setup.destinations[0]
        List<String> users = getRandomUsers(2)
        Date earned = new Date().clearTime() - 7
        skillsService.addSkill(setup.source, users[0], earned)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        // No existing points for the reporting user: the native transaction can commit between
        // statements without waiting on point rows already deleted by the cleanup transaction.
        seedResidue(destination, users[0], false)
        seedResidue(destination, users[1])
        seedGlobal(users[0])
        Map source = snapshot(setup.source.projectId, users[0])
        Integer sourceId = skillDefRepo.findByProjectIdAndSkillId(setup.source.projectId, setup.source.skillId).id
        List<Map> sourceEvents = events(sourceId, users[0])
        List<Map> global = globalSnapshot(users[0])
        SkillsService reporter = createService(users[0])
        SkillsService root = createRootSkillService()
        queueBackfill()
        controls.armAchievements()
        callers = Executors.newSingleThreadExecutor()

        when:
        Future<Map> cleanup = callers.submit({ root.cleanupImportedSkillProgress([projectIds: [destination.projectId]]) } as Callable<Map>)
        assert controls.arrived.await(60, TimeUnit.SECONDS)
        assert reporter.addSkill(destination.nativeSkill).body.skillApplied
        Map committed = snapshot(destination.projectId, users[0])
        Map participant = membership(destination.projectId, users[0])
        controls.release.countDown()
        Map result = cleanup.get(60, TimeUnit.SECONDS)

        then:
        result.deletedPointRows == 3
        result.deletedAchievementRows == 4
        snapshot(destination.projectId, users[0]) == committed
        snapshot(destination.projectId, users[1]) == [points: [], awards: []]
        membership(destination.projectId, users[0]) == participant
        participant.status == BackfillStatus.PENDING
        createService(users[0]).getSkillsSummaryForCurrentUser(destination.projectId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, users[0], destination.nativeSkill.skillId).points == 100
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], destination.projectId, destination.nativeSkill.skillId).size() == 1
        snapshot(setup.source.projectId, users[0]) == source
        events(sourceId, users[0]) == sourceEvents
        globalSnapshot(users[0]) == global

        when:
        root.fixImportedSkillProgress(destination.projectId, users[0])

        then:
        createService(users[0]).getSkillsSummaryForCurrentUser(destination.projectId).points == 200
        createService(users[0]).getSubjectSummaryForCurrentUser(destination.projectId, 'TestSubject2').points == 100
        snapshot(destination.projectId, users[0]).points.findAll { Map row -> row.skillId == destination.nativeSkill.skillId } ==
                committed.points.findAll { Map row -> row.skillId == destination.nativeSkill.skillId }
        snapshot(destination.projectId, users[0]).awards.findAll { Map row -> row.skillId == destination.nativeSkill.skillId } ==
                committed.awards.findAll { Map row -> row.skillId == destination.nativeSkill.skillId }
        membership(destination.projectId, users[0]) == participant
        globalSnapshot(users[0]) == global

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        createService(users[0]).getSkillsSummaryForCurrentUser(destination.projectId).points == 200
        membership(destination.projectId, users[0]).status == BackfillStatus.COMPLETED

        cleanup:
        controls.release.countDown()
        releaseBackfill()
    }

    def "cleanup completes while a real backfill holds uncommitted reused credit and Fix preserves the settled result"() {
        Map setup = reuseFixture()
        Map destination = setup.destinations[0]
        List<String> users = getRandomUsers(2)
        Date earned = new Date().clearTime() - 7
        users.each { String user -> skillsService.addSkill(setup.source, user, earned); seedGlobal(user) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        seedReusedProgress(destination, users[0], 100, 100, earned)
        ([destination.imported] + destination.importedCopies).each { SkillDef skill -> seedPoints(destination.projectId, users[0], skill, 17, earned) }
        seedReusedProgress(destination, users[1], 0, 100, earned)
        users.each { String user -> seedReusedProgress(setup.destinations[1], user, 0, 100, earned) }
        Map before = users.collectEntries { String user -> [(user): snapshot(destination.projectId, user)] }
        Map otherProject = users.collectEntries { String user -> [(user): snapshot('TestProject3', user)] }
        Map source = users.collectEntries { String user -> [(user): snapshot(setup.source.projectId, user)] }
        Integer sourceId = skillDefRepo.findByProjectIdAndSkillId(setup.source.projectId, setup.source.skillId).id
        Map sourceEvents = users.collectEntries { String user -> [(user): events(sourceId, user)] }
        Map globals = users.collectEntries { String user -> [(user): globalSnapshot(user)] }
        SkillsService root = createRootSkillService()
        controls.armBackfill(destination.projectId, users[0])

        when:
        createService(users[0]).initializeProjectProgress(destination.projectId)
        assert controls.backfillArrived.await(60, TimeUnit.SECONDS)
        Map participant = membership(destination.projectId, users[0])
        // The worker has executed the real skill-credit writes, but has not computed totals or
        // committed. Cleanup must finish without deleting or waiting on that member's credit.
        Map result = root.cleanupImportedSkillProgress([projectIds: [destination.projectId]])

        then:
        participant.status == BackfillStatus.PENDING
        result.projects*.status == ['CLEANED']
        result.deletedPointRows == before[users[1]].points.size() - 2
        result.deletedAchievementRows == before[users[1]].awards.size() - 2
        snapshot(destination.projectId, users[0]) == before[users[0]]
        snapshot(destination.projectId, users[1]) ==
                [points: before[users[1]].points.findAll { Map row -> row.skillId in destination.importedCopies*.skillId },
                 awards: before[users[1]].awards.findAll { Map row -> row.skillId in destination.importedCopies*.skillId }]
        membership(destination.projectId, users[0]) == participant
        users.every { String user -> snapshot('TestProject3', user) == otherProject[user] &&
                snapshot(setup.source.projectId, user) == source[user] && events(sourceId, user) == sourceEvents[user] && globalSnapshot(user) == globals[user] }

        when:
        controls.backfillRelease.countDown()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map settled = snapshot(destination.projectId, users[0])
        Map completed = membership(destination.projectId, users[0])
        root.fixImportedSkillProgress(destination.projectId, users[0])

        then:
        assertDisplayedProgress(destination, users[0], [200, 100, 100, 100], 500, [5, 5, 5, 5], 5, true, true)
        completed.status == BackfillStatus.COMPLETED
        snapshot(destination.projectId, users[0]) == settled
        membership(destination.projectId, users[0]) == completed
        List<String> nativeIds = [destination.nativeSkill.skillId, destination.nativeCopy.skillId]
        settled.points.findAll { Map row -> row.skillId in nativeIds } == before[users[0]].points.findAll { Map row -> row.skillId in nativeIds }
        settled.awards.findAll { Map row -> row.skillId in nativeIds } == before[users[0]].awards.findAll { Map row -> row.skillId in nativeIds }
        users.every { String user -> globalSnapshot(user) == globals[user] && snapshot('TestProject3', user) == otherProject[user] }

        cleanup:
        controls.backfillRelease.countDown()
    }

    def "Fix restores actual cleanup losses across reused imports groups badges and levels without repairing other pairs"() {
        List<String> users = getRandomUsers(3)
        Map setup = reuseFixture(['TestProject2': [users[2]], 'TestProject3': [users[2]]])
        Date earned = new Date().clearTime() - 7
        users.each { String user -> skillsService.addSkill(setup.source, user, earned); seedGlobal(user) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        setup.destinations.each { Map projectFixture -> users.take(2).each { String user -> seedReusedProgress(projectFixture, user, 100, 100, earned) } }
        Map protectedRows = setup.destinations.collectEntries { Map projectFixture -> [(projectFixture.projectId): snapshot(projectFixture.projectId, users[2])] }
        Map sources = users.collectEntries { String user -> [(user): snapshot(setup.source.projectId, user)] }
        Integer sourceId = skillDefRepo.findByProjectIdAndSkillId(setup.source.projectId, setup.source.skillId).id
        Map sourceEvents = users.collectEntries { String user -> [(user): events(sourceId, user)] }
        Map globals = users.collectEntries { String user -> [(user): globalSnapshot(user)] }
        SkillsService root = createRootSkillService()

        when:
        Map result = root.cleanupImportedSkillProgress([projectIds: setup.destinations*.projectId])

        then:
        result.projects*.status == ['CLEANED', 'CLEANED']
        setup.destinations.each { Map projectFixture ->
            assert snapshot(projectFixture.projectId, users[2]) == protectedRows[projectFixture.projectId]
            users.take(2).each { String user ->
                Map remaining = snapshot(projectFixture.projectId, user)
                List<String> retainedIds = [projectFixture.nativeSkill.skillId, projectFixture.nativeCopy.skillId] + projectFixture.importedCopies*.skillId
                assert remaining.points*.skillId.toSet() == retainedIds.toSet()
                assert remaining.awards*.skillId.toSet() == retainedIds.toSet()
                assert remaining.points.size() == 4 && remaining.awards.size() == 4
                assert !membership(projectFixture.projectId, user)
            }
        }

        when:
        Map destination = setup.destinations[0]
        Map otherProject = users.collectEntries { String user -> [(user): snapshot('TestProject3', user)] }
        Map otherUser = snapshot(destination.projectId, users[1])
        Map retainedCredit = snapshot(destination.projectId, users[0])
        queueBackfill()
        createService(users[0]).initializeProjectProgress(destination.projectId)
        Map participant = membership(destination.projectId, users[0])
        root.fixImportedSkillProgress(destination.projectId, users[0])

        then:
        participant.status == BackfillStatus.PENDING
        membership(destination.projectId, users[0]) == participant
        assertDisplayedProgress(destination, users[0], [200, 100, 100, 100], 500, [5, 5, 5, 5], 5, true, true)
        assertLevels(destination.projectId, users[0], null, 5)
        destination.subjects.each { SkillDef subject ->
            assertLevels(destination.projectId, users[0], subject.skillId, 5)
            assert userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, users[0], subject.skillId).points ==
                    (subject.skillId == 'TestSubject1' ? 200 : 100)
        }
        [destination.nativeGroup, destination.importedGroup].each { SkillDef group ->
            assert userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], destination.projectId, group.skillId).size() == 1
        }
        [destination.badge, destination.nativeBadge].each { Map badge ->
            assert userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], destination.projectId, badge.badgeId).first().achievedOn == earned
        }
        snapshot(destination.projectId, users[0]).points.findAll { Map row -> row.id in retainedCredit.points*.id } == retainedCredit.points
        snapshot(destination.projectId, users[0]).awards.findAll { Map row -> row.id in retainedCredit.awards*.id } == retainedCredit.awards
        snapshot(destination.projectId, users[1]) == otherUser
        users.every { String user -> snapshot('TestProject3', user) == otherProject[user] &&
                snapshot(setup.source.projectId, user) == sources[user] && events(sourceId, user) == sourceEvents[user] && globalSnapshot(user) == globals[user] }

        when:
        Map repaired = snapshot(destination.projectId, users[0])
        root.fixImportedSkillProgress(destination.projectId, users[0])

        then:
        snapshot(destination.projectId, users[0]) == repaired
        membership(destination.projectId, users[0]) == participant
        globalSnapshot(users[0]) == globals[users[0]]

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        snapshot(destination.projectId, users[0]) == repaired
        membership(destination.projectId, users[0]).status == BackfillStatus.COMPLETED

        cleanup:
        releaseBackfill()
    }

    def "completion validation rolls back overtaken Fix and explicit retry recovers without membership mutation"() {
        Map setup = fixture(1)
        String user = getRandomUsers(1)[0]
        skillsService.addSkill(setup.source, user)
        createService(user).initializeProjectProgress('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        userPointsRepo.delete(userPointsRepo.findByProjectIdAndUserIdAndSkillId('TestProject2', user, setup.source.skillId))
        Map before = snapshot('TestProject2', user)
        Map participant = membership('TestProject2', user)
        seedGlobal(user)
        List<Map> global = globalSnapshot(user)
        SkillsService root = createRootSkillService()
        controls.armRepair()
        callers = Executors.newSingleThreadExecutor()

        when:
        Future<Boolean> repair = callers.submit({
            try { root.fixImportedSkillProgress('TestProject2', user); return false }
            catch (SkillsClientException failure) { assert failure.message.contains('finalization overlap'); return true }
        } as Callable<Boolean>)
        assert controls.arrived.await(60, TimeUnit.SECONDS)
        skillsService.changeSetting('TestProject2', 'catalog.finalize.state', [projectId: 'TestProject2', setting: 'catalog.finalize.state', value: 'FAILED'])
        controls.release.countDown()

        then:
        repair.get(60, TimeUnit.SECONDS)
        snapshot('TestProject2', user) == before
        membership('TestProject2', user) == participant
        globalSnapshot(user) == global

        when:
        controls.pauseRepair = false
        skillsService.changeSetting('TestProject2', 'catalog.finalize.state', [projectId: 'TestProject2', setting: 'catalog.finalize.state', value: 'COMPLETED'])
        root.fixImportedSkillProgress('TestProject2', user)

        then:
        skillsService.getSkillSummary(user, 'TestProject2').points == 100
        membership('TestProject2', user) == participant
        globalSnapshot(user) == global
    }

    @TestConfiguration
    static class ControlsConfiguration {
        @Bean CleanupControls cleanupControls() { new CleanupControls() }
    }

    @Aspect
    static class CleanupControls {
        final Map<String, AtomicInteger> pointDeleteCalls = new ConcurrentHashMap<>()
        final Map<String, AtomicInteger> achievementDeleteCalls = new ConcurrentHashMap<>()
        volatile String failProject
        volatile boolean pauseDelete
        volatile boolean pauseAchievements
        volatile boolean pauseRepair
        volatile boolean pauseBackfill
        String backfillProjectId
        String backfillUserId
        int timeoutSeconds = 90
        volatile CountDownLatch arrived = new CountDownLatch(1)
        volatile CountDownLatch release = new CountDownLatch(1)
        volatile CountDownLatch backfillArrived = new CountDownLatch(1)
        volatile CountDownLatch backfillRelease = new CountDownLatch(1)

        void armDelete() { arrived = new CountDownLatch(1); release = new CountDownLatch(1); pauseDelete = true }
        void armAchievements() { arrived = new CountDownLatch(1); release = new CountDownLatch(1); pauseAchievements = true }
        void armRepair() { arrived = new CountDownLatch(1); release = new CountDownLatch(1); pauseRepair = true }
        void armBackfill(String projectId, String userId) {
            backfillArrived = new CountDownLatch(1)
            backfillRelease = new CountDownLatch(1)
            backfillProjectId = projectId
            backfillUserId = userId
            pauseBackfill = true
        }

        @Around('execution(* skills.storage.repos.CatalogParticipationCleanupRepo.deletePoints(..))')
        Object beforeDeletes(ProceedingJoinPoint call) {
            pointDeleteCalls.computeIfAbsent((String) call.args[0], { String projectId -> new AtomicInteger() }).incrementAndGet()
            if (pauseDelete) {
                pauseDelete = false
                arrived.countDown()
                assert release.await(timeoutSeconds, TimeUnit.SECONDS)
            }
            call.proceed()
        }

        @Around('execution(* skills.storage.repos.CatalogParticipationCleanupRepo.deleteAchievements(..))')
        Object failAfterPoints(ProceedingJoinPoint call) {
            achievementDeleteCalls.computeIfAbsent((String) call.args[0], { String projectId -> new AtomicInteger() }).incrementAndGet()
            if (call.args[0] == failProject) { throw new IllegalStateException('Injected after point deletion') }
            if (pauseAchievements) {
                pauseAchievements = false
                arrived.countDown()
                assert release.await(timeoutSeconds, TimeUnit.SECONDS)
            }
            call.proceed()
        }

        @Around('execution(* skills.services.CatalogImportedCreditService.reconcileTotalsAndAchievements(..))')
        Object repaired(ProceedingJoinPoint call) {
            // Ordinary backfill calls the generated three-argument Groovy overload;
            // explicit Fix passes the fourth argument and must not trip this worker barrier.
            if (pauseBackfill && (call.args.length == 3 || call.args[3] == false) &&
                    call.args[0] == backfillProjectId && call.args[1] == backfillUserId) {
                pauseBackfill = false
                backfillArrived.countDown()
                assert backfillRelease.await(timeoutSeconds, TimeUnit.SECONDS)
            }
            Object result = call.proceed()
            if (pauseRepair && call.args.length == 4 && call.args[3] == true) {
                arrived.countDown()
                assert release.await(timeoutSeconds, TimeUnit.SECONDS)
            }
            result
        }
    }
}
