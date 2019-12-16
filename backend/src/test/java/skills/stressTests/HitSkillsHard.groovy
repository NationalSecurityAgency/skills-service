package skills.stressTests


import callStack.profiler.ProfThreadPool
import groovy.util.logging.Slf4j
import org.slf4j.impl.StaticLoggerBinder
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService

import java.util.concurrent.Callable
import java.util.concurrent.Future

@Slf4j
class HitSkillsHard {

    int numProjects = 10
    int subjPerProject = 6
    int skillsPerSubject = 50
    int badgesPerProject = 10
    int hasDependenciesEveryNProjects = 5
    int numUsersPerApp = 100
    int numConcurrentThreads = 5

    static void main(String[] args) {
        HitSkillsHard hitSkillsHard = new HitSkillsHard()
        hitSkillsHard.run()
    }

    CreateSkillsDef createSkillsDef = new CreateSkillsDef(
            numProjects: numProjects,
            subjPerProject: subjPerProject,
            skillsPerSubject: skillsPerSubject,
            badgesPerProject: badgesPerProject,
            hasDependenciesEveryNProjects: hasDependenciesEveryNProjects,
//            remove: true
    )

    UserAndDateFactory userAndDateFactory = new UserAndDateFactory(
            numUsers: numUsersPerApp,
            numDates: 365
    )

    void run() {
        printSetupParams()
//        createSkillsDef.create()

        ProfThreadPool profThreadPool = new ProfThreadPool("reportEventsPool", numConcurrentThreads)
        profThreadPool.warnIfFull = false

        try {
            List<Future> futures = numConcurrentThreads.times {
                profThreadPool.submit({ ->
                    reportEvents()
                } as Callable);
            }
            // recommended to use following statement to ensure the execution of all tasks.
            futures.each { it.get() }
            log.info("Started All Threads")
        } finally {
            profThreadPool.shutdown()
        }
    }

    StatsHelper statsHelper = new StatsHelper()

    void reportEvents() {
        try {
            log.info("Thread [{}] started", Thread.currentThread().name)
            while (true) {
                CreateSkillsDef.RandomLookupKey randomLookupKey = createSkillsDef.randomLookupKey()
                while (randomLookupKey.projId == "Project90") {
                    randomLookupKey = createSkillsDef.randomLookupKey()
                }

                SkillsService service = SkillServiceFactory.getService(randomLookupKey.projId)
                statsHelper.startEvent()
                try {
                    service.addSkill([projectId: randomLookupKey.projId, skillId: randomLookupKey.skillId], userAndDateFactory.userId, userAndDateFactory.date)
                } catch (SkillsClientException skillsClientException) {
                    if (skillsClientException.message.contains("Skill definition does not exist.")) {
                        // that's ok
                        log.error("Swallowed", skillsClientException)
                    } else {
                        log.error("Throwing Exception", skillsClientException)
                        throw skillsClientException
                    }

                }
                statsHelper.endEvent()
            }
        } catch (Exception e) {
            log.error("Failed Thread", e)
        } finally {
            log.info("Thread Finished")
        }
    }


    void printSetupParams() {
        List<String> res = [
                "\n--------------------------------------",
                "Number of projects: ${numProjects}",
                "Number of subjects per project: ${subjPerProject}",
                "Number of badges per project: ${badgesPerProject}",
                "Every N project has dependencies: ${hasDependenciesEveryNProjects}",
                "Number of skills per subject: ${skillsPerSubject}",
                "Number of users per project: ${numUsersPerApp}",
                "Number of concurrent skill reporters: ${numConcurrentThreads}",
                "--------------------------------------",
        ]
        log.info(res.join("\n"))
    }
}
