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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import skills.intTests.utils.SkillsService
import skills.intTests.utils.RestTemplateWrapper.StatefulRestTemplateInterceptor
import skills.storage.model.UserPoints
import spock.lang.Requires

import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

import static skills.intTests.utils.SkillsFactory.*

/**
 * OPTIONAL, disposable integration database ONLY. Temporary pre-5.1 remediation tooling.
 * IDE configuration: CatalogParticipationCleanupStressIT - Opt-in.
 * Enable with -Dskills.catalog.cleanup.stress=true. Size overrides:
 * projects=3, users=160, imports=2, operations=2, concurrency=6, timeoutSeconds=600
 * under the skills.catalog.cleanup.stress prefix. Never part of the routine gate.
 * Remove this suite/configuration when the remediation endpoints are retired.
 */
@Slf4j
@Requires({ Boolean.getBoolean('skills.catalog.cleanup.stress') })
@TestPropertySource(properties = ['db-scheduler.threads=3', 'db-scheduler.polling-interval=100ms',
        'skills.config.taskSchedulingDelayInSeconds=0', 'skills.config.exponentialBackOffSeconds=1'])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import([CatalogParticipationCleanupIT.ControlsConfiguration, CatalogParticipantMixedWorkloadIT.ObservationConfiguration])
class CatalogParticipationCleanupStressIT extends CatalogCleanupIntSpec {
    @Autowired CatalogParticipationCleanupIT.CleanupControls controls
    @Autowired CatalogParticipantMixedWorkloadIT.WorkloadObservation observation
    private final int deadline = Integer.getInteger('skills.catalog.cleanup.stress.timeoutSeconds', 600)
    private final Queue<Map> requests = new ConcurrentLinkedQueue<>()
    private final CountDownLatch start = new CountDownLatch(1)
    private final AtomicBoolean cancelled = new AtomicBoolean()
    private ExecutorService callers
    private ScheduledExecutorService sampler
    private final Queue<Map> samples = new ConcurrentLinkedQueue<>()
    private final Queue<String> samplingFailures = new ConcurrentLinkedQueue<>()

    def cleanup() {
        cancelled.set(true)
        start.countDown()
        controls.release.countDown()
        controls.pauseDelete = false
        controls.pauseAchievements = false
        if (callers) {
            callers.shutdown()
            if (!callers.awaitTermination(deadline, TimeUnit.SECONDS)) {
                log.error('Cleanup stress stalled: {}', diagnostics())
                callers.shutdownNow()
                assert callers.awaitTermination(30, TimeUnit.SECONDS)
            }
        }
        if (sampler) { sampler.shutdownNow(); assert sampler.awaitTermination(30, TimeUnit.SECONDS) }
        try { waitForAsyncTasksCompletion.waitForAllScheduleTasks(deadline * 1000L) }
        finally { log.info('Legacy cleanup stress report: requests={}, samples={}, workers={}, failures={}, database={}',
                requests, samples, observation.workerRuns, observation.failures, diagnostics()) }
    }

    def "optional concurrent remediation and explicit quiescent Fix preserve credit boundaries"() {
        int projectCount = Integer.getInteger('skills.catalog.cleanup.stress.projects', 3)
        int userCount = Integer.getInteger('skills.catalog.cleanup.stress.users', 160)
        int importCount = Integer.getInteger('skills.catalog.cleanup.stress.imports', 2)
        int operations = Integer.getInteger('skills.catalog.cleanup.stress.operations', 2)
        int concurrency = Integer.getInteger('skills.catalog.cleanup.stress.concurrency', 6)
        assert projectCount >= 2 && userCount >= 8 && importCount >= 2 && operations >= 1 && concurrency >= 5 && deadline > 0
        List<String> users = getRandomUsers(userCount)
        int split = userCount.intdiv(4)
        List<String> nativeUsers = users.take(split)
        List<String> visitors = users.drop(split).take(split)
        List<String> oneProjectOnly = users.drop(split * 2).take(split)
        List<String> unrelated = users.drop(split * 3)
        Map setup = fixture(projectCount, nativeUsers)
        Map<String, Map> reportingSkills = setup.destinations.collectEntries { Map destination ->
            int number = Integer.parseInt(destination.projectId.replace('TestProject', ''))
            Map skill = createSkill(number, 1, 22, 0, operations, 0, 100)
            skillsService.createSkill(skill)
            [(destination.projectId): skill]
        }
        List<Map> extraSources = (1..<importCount).collect { int index -> createSkill(1, 1, 11 + index, 0, operations + 1, 0, 100) }
        skillsService.createSkills(extraSources)
        extraSources.each { Map skill -> skillsService.exportSkillToCatalog(skill.projectId, skill.skillId) }
        List<Map> allSources = [setup.source] + extraSources
        allSources.each { Map skill -> users.each { String user -> skillsService.addSkill(skill, user, new Date() - 5) } }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        setup.destinations.each { Map destination ->
            (visitors + oneProjectOnly + unrelated).each { String user -> seedResidue(destination, user) }
            visitors.take(1).each { String user -> createService(user).initializeProjectProgress(destination.projectId) }
            extraSources.each { Map skill -> skillsService.importSkillFromCatalog(destination.projectId, 'TestSubject2', skill.projectId, skill.skillId) }
        }
        users.each { String user -> seedGlobal(user) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        SkillsService root = createRootSkillService()
        root = boundedClient(root.userName, 'aaaaaaaa', true)
        observation.reset()
        callers = Executors.newFixedThreadPool(concurrency)
        sampler = Executors.newSingleThreadScheduledExecutor()
        sampler.scheduleWithFixedDelay({
            try { samples.add(queueAndLockSample()) }
            catch (Exception failure) { samplingFailures.add(failure.toString()) }
        } as Runnable, 0, 250, TimeUnit.MILLISECONDS)
        controls.timeoutSeconds = deadline
        controls.armDelete()
        Future cleanupRun = callers.submit({
            SkillsService cleanupRoot = boundedClient(root.userName, 'aaaaaaaa', true)
            measured('cleanup-all', '*') { cleanupRoot.cleanupImportedSkillProgress([allProjects: true]) }
            measured('cleanup-targeted', setup.destinations[0].projectId) { cleanupRoot.cleanupImportedSkillProgress([projectIds: [setup.destinations[0].projectId]]) }
        } as Callable)
        assert controls.arrived.await(deadline, TimeUnit.SECONDS)
        List<Future> futures = []
        // Every lane owns its clients. The latch starts concurrent real HTTP callers, no synthetic sleeps.
        futures.add(callers.submit({
            SkillsService admin = boundedClient(skillsService.userName)
            awaitStart()
            setup.destinations.each { Map destination -> measured('finalize', destination.projectId) { admin.finalizeSkillsImportFromCatalog(destination.projectId, false) } }
        } as Callable))
        futures.add(callers.submit({
            SkillsService reporter = boundedClient(skillsService.userName)
            awaitStart()
            (1..operations).each { int round ->
                extraSources.each { Map skill -> users.each { String user -> measured('source-report', "${user}/${skill.skillId}") { reporter.addSkill(skill, user) } } }
            }
        } as Callable))
        futures.add(callers.submit({
            Map<String, SkillsService> learners = (visitors + oneProjectOnly).collectEntries { String user -> [(user): boundedClient(user)] }
            awaitStart()
            (visitors + oneProjectOnly).each { String user ->
                List<Map> destinations = user in oneProjectOnly ? setup.destinations.take(1) : setup.destinations
                destinations.each { Map destination -> measured('visit', "${user}/${destination.projectId}") { learners[user].initializeProjectProgress(destination.projectId) } }
            }
        } as Callable))
        futures.add(callers.submit({
            SkillsService reporter = boundedClient(skillsService.userName)
            awaitStart()
            setup.destinations.each { Map destination ->
                (1..operations).each { int round ->
                    nativeUsers.each { String user -> measured('native-report', "${user}/${destination.projectId}") {
                        Map result = reporter.addSkill(reportingSkills[destination.projectId], user).body
                        assert result.skillApplied
                        result
                    } }
                }
            }
        } as Callable))

        when:
        long began = System.nanoTime()
        long expires = began + TimeUnit.SECONDS.toNanos(deadline)
        start.countDown()
        futures.each { Future future -> future.get(Math.max(1L, expires - System.nanoTime()), TimeUnit.NANOSECONDS) }
        controls.release.countDown()
        cleanupRun.get(deadline, TimeUnit.SECONDS)
        long drainStart = System.nanoTime()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks(deadline * 1000L)
        long recoveryExpires = System.nanoTime() + TimeUnit.SECONDS.toNanos(deadline)
        SkillsService inspector = boundedClient(skillsService.userName)
        log.info('Cleanup stress producerMs={}, drainMs={}, diagnostics={}', (drainStart - began) / 1_000_000L,
                (System.nanoTime() - drainStart) / 1_000_000L, diagnostics())
        Map rerun = root.cleanupImportedSkillProgress([projectIds: setup.destinations*.projectId])
        assert rerun.projects.every { it.status != 'FAILED' && !(it.reason ?: '').contains('RUNNING or FAILED') }
        // Deliberately break a COMPLETED participant's imported credit, proving explicit repair is not a status short circuit.
        UserPoints broken = userPointsRepo.findByProjectIdAndUserIdAndSkillId(setup.destinations[0].projectId, visitors[0], setup.source.skillId)
        assert broken
        userPointsRepo.delete(broken)
        setup.destinations.eachWithIndex { Map destination, int index ->
            List<String> eligible = nativeUsers + visitors + (index == 0 ? oneProjectOnly : [])
            eligible.each { String user ->
                assert System.nanoTime() < recoveryExpires: "Cleanup stress recovery deadline expired: ${diagnostics()}"
                Map participant = membership(destination.projectId, user)
                List<Map> global = globalSnapshot(user)
                List<Map> nativeCredit = snapshot(destination.projectId, user).points.findAll {
                    it.skillId in [destination.nativeSkill.skillId, reportingSkills[destination.projectId].skillId]
                }
                root.fixImportedSkillProgress(destination.projectId, user)
                root.fixImportedSkillProgress(destination.projectId, user)
                assert membership(destination.projectId, user) == participant
                assert globalSnapshot(user) == global
                assert snapshot(destination.projectId, user).points.findAll {
                    it.skillId in [destination.nativeSkill.skillId, reportingSkills[destination.projectId].skillId]
                } == nativeCredit
                int importedPoints = 100 + extraSources.size() * (operations + 1) * 100
                assert inspector.getSkillSummary(user, destination.projectId).points == importedPoints + (user in nativeUsers ? (operations + 1) * 100 : 0)
                assert inspector.getSkillSummary(user, destination.projectId, 'TestSubject2').points == importedPoints
                assert inspector.getBadgeSummary(user, destination.projectId, destination.badge.badgeId).badgeAchieved
                allSources.each { Map skill -> assert userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, skill.skillId).points == (skill == setup.source ? 100 : (operations + 1) * 100) }
            }
            (unrelated + (index == 0 ? [] : oneProjectOnly)).each { String user ->
                assert snapshot(destination.projectId, user) == [points: [], awards: []]
                assert !membership(destination.projectId, user)
            }
        }

        then:
        users.every { String user -> System.nanoTime() < recoveryExpires &&
                inspector.getSkillSummary(user, setup.source.projectId).points == 100 + extraSources.size() * (operations + 1) * 100 }
        !backfillTasks()
        observation.failures.every { Map failure -> failure.expectedOverlap || failure.sqlState == '40001' }
        samplingFailures.isEmpty()
    }

    private void awaitStart() { assert start.await(deadline, TimeUnit.SECONDS) }

    private SkillsService boundedClient(String username, String password = 'password', boolean heldCleanup = false) {
        SkillsService client = createService(username, password)
        StatefulRestTemplateInterceptor interceptor = client.wsHelper.oAuthRestTemplate.interceptors.find {
            it instanceof StatefulRestTemplateInterceptor
        } as StatefulRestTemplateInterceptor
        ClientHttpRequestFactory factory = interceptor.requestFactory
        int timeoutMs = (heldCleanup ? deadline : Math.min(deadline, 30)) * 1000
        if (factory instanceof SimpleClientHttpRequestFactory) {
            factory.setConnectTimeout(timeoutMs)
            factory.setReadTimeout(timeoutMs)
        } else {
            assert factory instanceof HttpComponentsClientHttpRequestFactory
            factory.setConnectionRequestTimeout(timeoutMs)
            factory.setReadTimeout(timeoutMs)
        }
        client
    }

    private Object measured(String operation, String target, Closure action) {
        if (cancelled.get()) { throw new CancellationException('Cleanup stress failure cleanup cancelled remaining producer operations') }
        long began = System.nanoTime()
        try {
            Object result = action.call()
            requests.add([operation: operation, target: target, elapsedMs: (System.nanoTime() - began) / 1_000_000L, result: result])
            result
        } catch (Exception failure) {
            requests.add([operation: operation, target: target, failure: failure.toString(), elapsedMs: (System.nanoTime() - began) / 1_000_000L])
            throw failure
        }
    }

    private Map diagnostics() {
        queueAndLockSample() + [tasks: jdbcTemplate.queryForList('select task_name, task_instance, consecutive_failures, execution_time from scheduled_tasks order by task_name, task_instance limit 1000')]
    }

    private Map queueAndLockSample() {
        [elapsedAtNanos: System.nanoTime(),
         backlog: jdbcTemplate.queryForList('select task_name, count(*) queued, max(consecutive_failures) max_retries from scheduled_tasks group by task_name'),
         locks: jdbcTemplate.queryForList('select pid, wait_event_type, wait_event, pg_blocking_pids(pid) blockers from pg_stat_activity where datname = current_database() and wait_event_type = \'Lock\'')]
    }
}
