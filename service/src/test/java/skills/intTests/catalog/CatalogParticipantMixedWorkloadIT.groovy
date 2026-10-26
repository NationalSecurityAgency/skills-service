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
import groovy.util.logging.Slf4j
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.Order
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import skills.intTests.utils.SkillsService
import skills.intTests.utils.RestTemplateWrapper.StatefulRestTemplateInterceptor
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.UserAchievement
import skills.storage.repos.ProjectParticipationRepo
import spock.util.concurrent.PollingConditions

import java.sql.SQLException
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CompletionService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import static skills.intTests.utils.SkillsFactory.*

/**
 * A fixed workload with naturally varying caller/worker order. Pauses AFTER committed bulk
 * points copies guarantee reporting and qualification occur inside two real finalization
 * runs, without holding database locks or replacing production credit/scheduler behavior.
 *
 * CI: run this class with no JVM overrides. The default is one user in each of the four
 * user categories (4 total), retaining both destinations, both import batches and every correctness
 * assertion. Target runtime is about 30 seconds including Spring startup; the generous
 * 120-second producer/drain deadline is a hang guard, not a performance assertion. This
 * context polls the real scheduler every 100 ms and schedules tasks without an intentional
 * delay to avoid idle test-harness time; real asynchronous workers still execute normally.
 *
 * Local stress (no source edits): run this class in IntelliJ, open Edit Configurations,
 * select the default/FORM profile and paste these VM options:
 *   -Dskills.catalog.mixed.usersPerCategory=25 -Dskills.catalog.mixed.timeoutSeconds=600 -Dskills.catalog.mixed.seed=31003
 * This exercises 100 users with the same exact-outcome/deadlock checks. The local
 * "CatalogParticipantMixedWorkloadIT - Stress" configuration supplies the size/deadline.
 * For a larger run use usersPerCategory=100 and timeoutSeconds=1200 (400 users). Rerun with
 * seeds 31004 and 31005 for different operation orders; the seed does not fix thread
 * scheduling. Run serially against the service test database because fixtures are reset.
 * Use FORM for larger workloads; PKI is limited by the available test certificates.
 *
 * Look for "Catalog mixed workload report" in the console: request p50/p95/max, producer
 * and queue-drain time, sampled queue peak and worker attempts/failures. Worker wall times
 * include deliberate test pauses. Timings are diagnostics, not machine-dependent gates.
 */
@Slf4j
@TestPropertySource(properties = [
        'db-scheduler.threads=3',
        'db-scheduler.polling-interval=100ms',
        'skills.config.taskSchedulingDelayInSeconds=0',
        'skills.config.exponentialBackOffSeconds=1',
        'skills.config.exponentialBackOffRate=1',
])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(ObservationConfiguration)
class CatalogParticipantMixedWorkloadIT extends CatalogIntSpec {
    @Autowired WorkloadObservation observation
    @Autowired ProjectParticipationRepo participationRepo

    private final int categorySize = Integer.getInteger('skills.catalog.mixed.usersPerCategory', 1)
    private final int timeoutSeconds = Integer.getInteger('skills.catalog.mixed.timeoutSeconds', 120)
    private final long seed = Long.getLong('skills.catalog.mixed.seed', 31003L)
    private final Date historicalDate = new Date(System.currentTimeMillis() - 3 * 86400000L)
    private final List<String> projectIds = ['TestProject2', 'TestProject3']
    private final List<Gate> gates = []
    private final List<CountDownLatch> phases = []
    private final AtomicBoolean cancelled = new AtomicBoolean()
    private final Queue<Map> requests = new ConcurrentLinkedQueue<>()
    private final AtomicInteger peakQueue = new AtomicInteger()
    private final Queue<String> samplingFailures = new ConcurrentLinkedQueue<>()
    private ExecutorService callers
    private CompletionService completions
    private ScheduledExecutorService sampler
    private List<String> users = []
    private long workloadStarted
    private long producersFinished
    private long converged

    def cleanup() {
        cancelled.set(true)
        phases.each { CountDownLatch phase -> while (phase.count > 0) { phase.countDown() } }
        synchronized (gates) { gates.each { Gate gate -> gate.open() } }
        if (callers) {
            callers.shutdown()
            if (!callers.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                log.error('Mixed workload callers did not terminate: {}', diagnostics())
                callers.shutdownNow()
                assert callers.awaitTermination(30, TimeUnit.SECONDS)
            }
        }
        if (sampler) {
            sampler.shutdownNow()
            assert sampler.awaitTermination(30, TimeUnit.SECONDS)
        }
        try {
            waitForAsyncTasksCompletion.waitForAllScheduleTasks(timeoutSeconds * 1000L)
        } finally {
            observation.copyGates.clear()
            log.info('Catalog mixed workload report: {}', report())
        }
    }

    def "mixed reports visits and successive imports converge without deadlocks or unrelated user credit"() {
        assert categorySize > 0 && timeoutSeconds > 0
        users = getRandomUsers(categorySize * 4)
        List<String> established = users.take(categorySize)
        List<String> visitors = users.drop(categorySize).take(categorySize)
        List<String> localQualifiers = users.drop(categorySize * 2).take(categorySize)
        List<String> sourceOnly = users.drop(categorySize * 3)
        // Reuse authenticated learners across fixture setup, the visit lane and final reads.
        // They are only used by one caller at a time; reporting/import lanes have separate clients.
        Map<String, SkillsService> learners = users.collectEntries { String user -> [(user): boundedClient(user)] }
        List<Map> sourceSkills = (11..13).collect { int id -> createSkill(1, 1, id, 0, 3, 0, 100) }
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), sourceSkills)
        sourceSkills.each { Map skill ->
            skillsService.exportSkillToCatalog(skill.projectId, skill.skillId)
            users.each { String user -> assert skillsService.addSkill(skill, user, historicalDate).body.skillApplied }
        }
        List<Map> nativeSkills = (2..3).collect { int project -> createSkill(project, 1, project * 10 + 1, 0, 3, 0, 100) }
        Map<String, Map> badges = [:]
        nativeSkills.each { Map skill ->
            int project = skill.projectId == 'TestProject2' ? 2 : 3
            skillsService.createProjectAndSubjectAndSkills(createProject(project), createSubject(project, 1), [skill])
            importSkill(skill.projectId, sourceSkills[0])
            skillsService.finalizeSkillsImportFromCatalog(skill.projectId)
            Map badge = createBadge(project, 1)
            skillsService.createBadge(badge)
            skillsService.assignSkillToBadge(skill.projectId, badge.badgeId, sourceSkills[0].skillId)
            skillsService.updateBadge(badge + [enabled: 'true'])
            badges[skill.projectId] = badge
            established.each { String user -> learners[user].initializeProjectProgress(skill.projectId) }
            waitForAsyncTasksCompletion.waitForAllScheduleTasks()
            importSkill(skill.projectId, sourceSkills[1])
        }
        Map<String, Date> establishedCreated = [:]
        projectIds.each { String project ->
            established.each { String user -> establishedCreated["${project}/${user}".toString()] = participationRepo.findByProjectIdAndUserId(project, user).created }
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        observation.reset()

        // Each lane has its own authenticated clients; the client helper is not shared across threads.
        SkillsService sourceReporter = boundedClient(skillsService.userName)
        SkillsService nativeReporter = boundedClient(skillsService.userName)
        SkillsService importer = boundedClient(skillsService.userName)
        List<Gate> firstGates = armCopies()
        CountDownLatch firstWave = phase(3)
        CountDownLatch nextBatch = phase(1)
        CountDownLatch secondWave = phase(3)
        CountDownLatch start = phase(1)
        callers = Executors.newFixedThreadPool(4)
        completions = new ExecutorCompletionService(callers)
        sampler = Executors.newSingleThreadScheduledExecutor()
        sampler.scheduleWithFixedDelay({
            try {
                int queued = jdbcTemplate.queryForObject('select count(*) from scheduled_tasks', Integer)
                peakQueue.accumulateAndGet(queued, { int previous, int current -> Math.max(previous, current) })
            } catch (Exception failure) {
                samplingFailures.add(failure.toString())
            }
        } as Runnable, 0, 250, TimeUnit.MILLISECONDS)
        workloadStarted = System.nanoTime()

        when:
        try {
            projectIds.each { String project -> measured('finalize-admission', project) { skillsService.finalizeSkillsImportFromCatalog(project, false) } }
            firstGates.each { Gate gate -> gate.awaitArrival(timeoutSeconds) }
            List<Future> futures = []
            futures.add(submit {
                await(start)
                (1..2).each { int round ->
                    if (round == 2) { await(nextBatch) }
                    shuffled(users, round).each { String user ->
                        sourceSkills.each { Map skill ->
                            Map result = measured('source-report', "${user}/${skill.skillId}") { sourceReporter.addSkill(skill, user, reportDate(round)).body }
                            assert result.skillApplied
                        }
                    }
                    (round == 1 ? firstWave : secondWave).countDown()
                }
            })
            futures.add(submit {
                await(start)
                (1..2).each { int round ->
                    if (round == 2) { await(nextBatch) }
                    shuffled(established + localQualifiers, round + 10).each { String user ->
                        nativeSkills.each { Map skill ->
                            Map result = measured('native-report', "${user}/${skill.projectId}") { nativeReporter.addSkill(skill, user, reportDate(round)).body }
                            assert result.skillApplied
                        }
                    }
                    (round == 1 ? firstWave : secondWave).countDown()
                }
            })
            futures.add(submit {
                await(start)
                (1..2).each { int round ->
                    if (round == 2) { await(nextBatch) }
                    shuffled(established + visitors, round + 20).each { String user ->
                        projectIds.each { String project ->
                            Map result = measured('initialize', "${user}/${project}") { learners[user].initializeProjectProgress(project) }
                            assert !result.ready
                            // A first-batch sweep may already have completed a visitor before batch two starts.
                            List<String> statuses = established.contains(user) ? ['COMPLETED'] :
                                    (round == 1 ? ['PENDING'] : ['PENDING', 'COMPLETED'])
                            assert result.backfillStatus in statuses
                        }
                    }
                    (round == 1 ? firstWave : secondWave).countDown()
                }
            })
            futures.add(submit {
                await(start)
                await(firstWave)
                firstGates.each { Gate gate -> gate.open() }
                new PollingConditions(timeout: timeoutSeconds).eventually {
                    projectIds.each { String project -> assert importer.getSetting(project, 'catalog.finalize.state').value == 'COMPLETED' }
                }
                // The next batch can overtake replay/sweeps from the first run; do not drain the queue here.
                List<Gate> secondGates = armCopies()
                projectIds.each { String project ->
                    measured('import-definition', project) { importSkill(project, sourceSkills[2], importer) }
                    measured('finalize-admission', project) { importer.finalizeSkillsImportFromCatalog(project, false) }
                }
                secondGates.each { Gate gate -> gate.awaitArrival(timeoutSeconds) }
                nextBatch.countDown()
                await(secondWave)
                secondGates.each { Gate gate -> gate.open() }
            })
            start.countDown()
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
            // Consume in completion order so a failed lane is surfaced immediately even if another awaits its phase.
            futures.each {
                Future completed = completions.poll(Math.max(1L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)
                assert completed != null: 'Mixed workload producers exceeded their completion deadline'
                completed.get()
            }
            producersFinished = System.nanoTime()
            waitForAsyncTasksCompletion.waitForAllScheduleTasks(Math.max(1L, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())))
            converged = System.nanoTime()
        } catch (Throwable failure) {
            log.error('Catalog mixed workload failed: report={}, state={}', report(), diagnostics(), failure)
            throw failure
        }

        then:
        verifyOutcomes {
            assert samplingFailures.empty
            assert observation.failures.findAll { Map failure -> !failure.expectedOverlap && failure.sqlState != '40001' }.empty
            assert !observation.failures.any { Map failure -> failure.sqlState == '40P01' }
            assert observation.workerRuns.count { Map run -> run.method == 'FinalizeCatalogSkillsImportExecutor' } == 4
            assert observation.workerRuns.findAll { Map run -> run.method == 'FinalizeCatalogSkillsImportExecutor' }.every { Map run -> run.success }
            projectIds.each { String project ->
                assert skillsService.getSetting(project, 'catalog.finalize.state').value == 'COMPLETED'
                int multiplier = project == 'TestProject2' ? 1 : 2
                (established + visitors + localQualifiers).each { String user ->
                    SkillsService learner = learners[user]
                    int nativePoints = visitors.contains(user) ? 0 : 200
                    // Default levels are 10/25/45/67/92%; only scaled native-plus-imported credit reaches 92%.
                    int expectedLevel = multiplier == 2 && nativePoints > 0 ? 5 : 4
                    Map summary = learner.getSkillsSummaryForCurrentUser(project)
                    assert summary.points == 900 * multiplier + nativePoints
                    assert summary.totalPoints == 900 * multiplier + 300
                    assert summary.skillsAchieved == 3
                    assert summary.skillsLevel == expectedLevel
                    assert summary.subjects[0].points == 900 * multiplier + nativePoints
                    assert summary.subjects[0].skillsAchieved == 3
                    assert summary.subjects[0].skillsLevel == expectedLevel
                    assert summary.badges.numBadgesCompleted == 1
                    assert summary.badges.recentlyAwardedBadges.badgeId == [badges[project].badgeId]
                    assert Instant.parse(summary.badges.recentlyAwardedBadges[0].achievedOn).toEpochMilli() == reportDate(2).time
                    sourceSkills.each { Map skill ->
                        assert learner.getSingleSkillSummaryForCurrentUser(project, skill.skillId).points == 300 * multiplier
                        // Award uniqueness/date are persistence guarantees not fully exposed by the skill summary.
                        List<UserAchievement> awards = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, project, skill.skillId)
                        assert awards.size() == 1
                        assert awards[0].achievedOn == reportDate(2)
                    }
                    Map local = nativeSkills.find { Map skill -> skill.projectId == project }
                    assert learner.getSingleSkillSummaryForCurrentUser(project, local.skillId).points == nativePoints
                    assert participationRepo.readStatus(project, user) == BackfillStatus.COMPLETED
                    if (established.contains(user)) {
                        assert participationRepo.findByProjectIdAndUserId(project, user).created == establishedCreated["${project}/${user}".toString()]
                    }
                }
                sourceOnly.each { String user ->
                    Map summary = learners[user].getSkillsSummaryForCurrentUser(project)
                    assert summary.points == 0
                    assert summary.skillsAchieved == 0
                    assert summary.skillsLevel == 0
                    assert summary.badges.numBadgesCompleted == 0
                    assert !participationRepo.existsByProjectIdAndUserId(project, user)
                    assert userPointsRepo.findByProjectIdAndUserId(project, user).empty
                    sourceSkills.each { Map skill -> assert userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, project, skill.skillId).empty }
                }
            }
            users.each { String user ->
                Map source = learners[user].getSkillsSummaryForCurrentUser('TestProject1')
                assert source.points == 900
                assert source.skillsAchieved == 3
                assert source.skillsLevel == 5
            }
        }
    }

    private void importSkill(String project, Map skill, SkillsService admin = skillsService) {
        admin.bulkImportSkillsFromCatalog(project, 'TestSubject1', [skill])
        if (project == 'TestProject3') { admin.updateImportedSkill(project, skill.skillId, 200) }
    }

    private Date reportDate(int round) { new Date(historicalDate.time + round * 60000L) }

    private List<String> shuffled(List<String> values, int salt) {
        List<String> result = new ArrayList<>(values)
        Collections.shuffle(result, new Random(seed + salt))
        result
    }

    private List<Gate> armCopies() {
        projectIds.collect { String project ->
            Gate gate = new Gate(timeoutSeconds: timeoutSeconds)
            synchronized (gates) { gates.add(gate) }
            observation.copyGates[project] = gate
            gate
        }
    }

    private Future submit(Closure action) { completions.submit(action as Callable) }

    private CountDownLatch phase(int count) {
        CountDownLatch phase = new CountDownLatch(count)
        phases.add(phase)
        phase
    }

    private SkillsService boundedClient(String username) {
        SkillsService client = createService(username)
        StatefulRestTemplateInterceptor interceptor = client.wsHelper.oAuthRestTemplate.interceptors.find {
            it instanceof StatefulRestTemplateInterceptor
        } as StatefulRestTemplateInterceptor
        // Configure the original factory retained by both the authenticated client and its CSRF interceptor.
        ClientHttpRequestFactory factory = interceptor.requestFactory
        int requestTimeoutMs = Math.min(timeoutSeconds, 30) * 1000
        if (factory instanceof SimpleClientHttpRequestFactory) {
            factory.setConnectTimeout(requestTimeoutMs)
            factory.setReadTimeout(requestTimeoutMs)
        } else {
            assert factory instanceof HttpComponentsClientHttpRequestFactory
            factory.setConnectionRequestTimeout(requestTimeoutMs)
            factory.setReadTimeout(requestTimeoutMs)
        }
        client
    }

    private void verifyOutcomes(Closure assertions) {
        try {
            assertions.call()
        } catch (Throwable failure) {
            log.error('Catalog mixed workload outcomes failed: report={}, state={}', report(), diagnostics(), failure)
            throw failure
        }
    }

    private void await(CountDownLatch latch) {
        assert latch.await(timeoutSeconds, TimeUnit.SECONDS): 'Mixed workload phase did not complete'
        assert !cancelled.get(): 'Mixed workload cancelled during cleanup'
    }

    private Object measured(String operation, String target, Closure action) {
        long started = System.nanoTime()
        Map request = [operation: operation, target: target, success: false]
        try {
            assert !cancelled.get(): 'Mixed workload cancelled during cleanup'
            Object result = action.call()
            request.success = true
            return result
        } catch (Throwable failure) {
            request.error = failure.toString()
            throw failure
        } finally {
            request.elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
            requests.add(request)
        }
    }

    private Map report() {
        Map latencies = requests.toList().groupBy { Map request -> request.operation }.collectEntries { String operation, List<Map> values ->
            List<Long> durations = values.collect { Map request -> (Long) request.elapsedMs }.sort()
            [(operation): [count: durations.size(), p50Ms: durations[(int) Math.ceil(durations.size() * 0.50) - 1],
                           p95Ms: durations[(int) Math.ceil(durations.size() * 0.95) - 1], maxMs: durations.last()]]
        }
        [usersPerCategory: categorySize, seed: seed, timeoutSeconds: timeoutSeconds,
         producerMs: producersFinished ? TimeUnit.NANOSECONDS.toMillis(producersFinished - workloadStarted) : null,
         drainMs: converged ? TimeUnit.NANOSECONDS.toMillis(converged - producersFinished) : null,
         sampledPeakQueue: peakQueue.get(), latencies: latencies,
         failedRequests: requests.findAll { Map request -> !request.success }, failures: observation.failures.toList(),
         workerAttempts: observation.workerRuns.toList().groupBy { Map run -> run.method }.collectEntries { String method, List<Map> runs ->
             [(method): [attempts: runs.size(), failed: runs.count { Map run -> !run.success },
                         wallMsIncludingTestPauses: runs.sum { Map run -> run.elapsedMs }]]
         }, samplingFailures: samplingFailures.toList()]
    }

    private Map diagnostics() {
        [tasks: jdbcTemplate.queryForList('select task_name, task_instance, picked, execution_time, consecutive_failures from scheduled_tasks order by task_name, task_instance'),
         databaseActivity: jdbcTemplate.queryForList('''select pid, state, wait_event_type, wait_event,
             pg_blocking_pids(pid) as blockers, query from pg_stat_activity
             where datname = current_database() and pid <> pg_backend_pid() and state <> 'idle' '''),
         participation: projectIds.collectEntries { String project ->
             [(project): users.collectEntries { String user -> [(user): participationRepo.readStatus(project, user)] }]
         }]
    }

    @TestConfiguration
    static class ObservationConfiguration {
        @Bean WorkloadObservation catalogMixedWorkloadObservation() { new WorkloadObservation() }
    }

    // Outside transaction advice: the pause follows a committed copy and sees commit-time failures.
    @Aspect
    @Order(0)
    static class WorkloadObservation {
        final Map<String, Gate> copyGates = new ConcurrentHashMap<>()
        final Queue<Map> failures = new ConcurrentLinkedQueue<>()
        final Queue<Map> workerRuns = new ConcurrentLinkedQueue<>()

        void reset() { failures.clear(); workerRuns.clear(); copyGates.clear() }

        @Around('''execution(* skills.services.admin.BatchOperationsTransactionalAccessor.*(..)) ||
                execution(* skills.tasks.executors.FinalizeCatalogSkillsImportExecutor.execute(..)) ||
                execution(* skills.tasks.executors.CatalogFinalizeReplayExecutor.execute(..)) ||
                execution(* skills.tasks.executors.CatalogParticipantBackfillExecutor.execute(..)) ||
                execution(* skills.tasks.executors.ImportedSkillAchievementTaskExecutor.execute(..))''')
        Object observe(ProceedingJoinPoint invocation) {
            String method = invocation.signature.declaringType.simpleName
            boolean worker = invocation.signature.name == 'execute'
            TaskInstance task = worker ? (TaskInstance) invocation.args[0] : null
            long started = System.nanoTime()
            boolean success = false
            try {
                Object result = invocation.proceed()
                if (invocation.signature.name == 'copySkillUserPointsToTheImportedProjects') {
                    Gate gate = copyGates[(String) invocation.args[0]]
                    if (gate && gate.claimed.compareAndSet(false, true)) { gate.pause() }
                }
                success = true
                return result
            } catch (Throwable failure) {
                Throwable cause = failure
                String sqlState = null
                boolean expectedOverlap = false
                while (cause != null) {
                    if (cause instanceof SQLException) { sqlState = cause.SQLState }
                    if (cause instanceof IllegalStateException &&
                            (cause.message?.startsWith('Expected transient catalog finalization overlap') ||
                             cause.message?.startsWith('Catalog finalization is not complete for'))) { expectedOverlap = true }
                    cause = cause.cause
                }
                failures.add([method: "${method}.${invocation.signature.name}".toString(), taskId: task?.id,
                              sqlState: sqlState, expectedOverlap: expectedOverlap, error: failure.toString()])
                throw failure
            } finally {
                if (worker) {
                    workerRuns.add([method: method, taskId: task.id, success: success,
                                    elapsedMs: TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)])
                }
            }
        }
    }

    private static class Gate {
        int timeoutSeconds
        final AtomicBoolean claimed = new AtomicBoolean()
        private final CountDownLatch arrived = new CountDownLatch(1)
        private final CountDownLatch released = new CountDownLatch(1)

        void pause() {
            arrived.countDown()
            assert released.await(timeoutSeconds, TimeUnit.SECONDS): 'Bulk copy boundary was not released'
        }

        void awaitArrival(int timeout) {
            assert arrived.await(timeout, TimeUnit.SECONDS): 'Finalization did not reach the committed points-copy boundary'
        }

        void open() { released.countDown() }
    }
}
