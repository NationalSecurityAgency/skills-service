/**
 * Copyright 2020 SkillTree
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
package skills.intTests


import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.RandomUtils
import org.apache.commons.lang3.time.StopWatch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.PlatformTransactionManager
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.services.UserEventService
import skills.services.events.SkillEventsService
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserEventsRepo
import spock.lang.Ignore

import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Slf4j
class BenchmarkTransactionUserEventSpec extends DefaultIntSpec {

    @Autowired
    UserEventsRepo userEventsRepo

    @Autowired
    UserEventService eventService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillEventsService skillsManagementFacade

    @Value('#{"${skills.config.compactDailyEventsOlderThan}"}')
    int maxDailyDays

    @Autowired
    private PlatformTransactionManager transactionManager;

    static class ProjectContainer{
        String projectId
        String subjectId
        List<Map> skills = []
    }

    private addEvent(String projectId, String skillId, String userId, Date date){
        skillsManagementFacade.reportSkill(projectId, skillId, userId, false, date);
    }

    List<ProjectContainer> populateData(){
        def containers = []
        (42..49).each {
            ProjectContainer container = new ProjectContainer()
            Map proj = SkillsFactory.createProject(it)
            container.projectId = proj.projectId
            Map subject = SkillsFactory.createSubject(it, it)
            container.subjectId = subject.subjectId

            skillsService.createProject(proj)
            skillsService.createSubject(subject, true)

            def skills = SkillsFactory.createSkills(25, it, it)

            skillsService.createSkills(skills)
            container.skills = skills
            containers.add(container)
        }

        return containers
    }

    @Ignore
    def "populate user events and run compaction"() {
        log.info("populating sample data")
        List<ProjectContainer> projects = populateData()
        List<String> randos = getRandomUsers(2000)

        log.info("priming users")
        randos.each {
            skillsService.addSkill(projects[0].skills[0], it, new Date())
        }

        Executor executor = Executors.newFixedThreadPool(2)
        List<Integer> daysBack = [4,5,6,7]

        int eventsToAdd = 3000000
        log.info("adding events")
        for (int i = 0; i < eventsToAdd; i++) {
            final int idx = i;
            executor.submit(new Callable<Boolean>() {
                @Override
                Boolean call() throws Exception {
                    Date date = LocalDateTime.now().minusDays(RandomUtils.nextInt(0, daysBack.size())).toDate()
                    ProjectContainer container = projects.get(RandomUtils.nextInt(0, projects.size()))
                    String user = randos.get(RandomUtils.nextInt(0, randos.size()))
                    def skill = container.skills.get(RandomUtils.nextInt(0, container.skills.size()))
                    addEvent(container.projectId, skill.skillId, user, date)
                    if (idx > 0 && idx % 20000 == 0) {
                        log.info("added $idx of $eventsToAdd")
                    }
                    return true
                }
            })
        }

        executor.shutdown()
        executor.awaitTermination(3, TimeUnit.HOURS)

        when:
        Runtime.getRuntime().gc()
        long memoryInUseBeforeCompaction = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        ThreadPoolExecutor dockerStats = new ScheduledThreadPoolExecutor(1)
        dockerStats.scheduleAtFixedRate(new Runnable() {
            @Override
            void run() {
                /*
                only relevant if running against a local postgres instance

                def proc = "docker stats pg_int_test --no-stream".execute()
                log.info("\n"+proc.text)
                */
                Runtime.getRuntime().gc()
            }
        }, 0, 30, TimeUnit.SECONDS)

        StopWatch sw = new StopWatch()
        sw.start()

        eventService.compactDailyEvents()

        sw.stop()
        Duration duration = Duration.of(sw.getTime(TimeUnit.MILLISECONDS), ChronoUnit.MILLIS)
        long memoryInUseAfterCompaction = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        println "took ${duration.toString()} time to compact $eventsToAdd events for ${projects.size()} projects and ${randos.size()} users"
        println "memory in use before compaction [${FileUtils.byteCountToDisplaySize(memoryInUseBeforeCompaction)}], " +
                "memory in use after compaction [${FileUtils.byteCountToDisplaySize(memoryInUseAfterCompaction)}]"
        dockerStats.shutdownNow()
        dockerStats.awaitTermination(2, TimeUnit.MINUTES)

        then:
        true
    }

}
