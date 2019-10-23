package skills.intTests

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.repos.LevelDefRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SettingRepo

import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class ConcurrencySpecs extends DefaultIntSpec {

    def "do not create duplicates when changing project setting concurrently"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)

        def settingsToSave = (0..50).collect {
            [
                    setting: "testSetting${it}".toString(),
                    value: [projectId: proj1.projectId, setting: "testSetting${it}".toString(), value: "true"]
            ]
        }

        int numThreads = 10
        when:
        List<Thread> threads = (1..numThreads).collect {
            Thread.start {
                settingsToSave.each {
                    skillsService.changeSetting(proj1.projectId, it.setting, it.value)
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        // should not throw an exception of javax.persistence.NonUniqueResultException: query did not return a unique result
        settingsToSave.each {
            skillsService.getSetting(proj1.projectId, it.setting)
        }

        then:
        true
    }

    @Autowired
    SettingRepo settingRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    LevelDefRepo levelDefRepo

    def "do not create duplicates projects or settings when concurrently inserting projects"() {
        def projectsToSave = (0..100).collect { SkillsFactory.createProject(it) }
        int numThreads = 5
        when:
        List<Thread> threads = (1..numThreads).collect {
            Thread.start {
                projectsToSave.each {
                    try {
                        skillsService.createProject(it)
                    } catch (SkillsClientException e) {
                        // should throw dup projects, that's what we are trying to break
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<String> settingsAsStrings = settingRepo.findAll().collect({"${it.projectId}-${it.userId}"})
        List<String> projectIds = projDefRepo.findAll().collect({it.projectId.toLowerCase()})
        List<String> levels = levelDefRepo.findAll().collect {"${it.projectId}-${it.level}"}
        then:
        settingsAsStrings.sort() == settingsAsStrings.unique().sort()
        projectIds.sort() == projectIds.unique().sort()
        levels.sort() == levels.unique().sort()
    }


    def "/api endpoints for non-existent users create UserAttr rows, concurrent requests should not cause errors"() {
        def proj = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()
        List<Map> skills = SkillsFactory.createSkills(2)

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        int numUsers = 100
        List<String> users = (1..numUsers).collect { "CreateNewUserAttrTestsUser${it}".toString() }
        int numThreads = 5
        AtomicInteger exceptionCount = new AtomicInteger()
        when:
        List<Thread> threads = (1..numThreads).collect {
            Thread.start {
                users.each {
                    try {
                        skillsService.getRank(it, proj.projectId)
                    } catch (SkillsClientException e) {
                        exceptionCount.incrementAndGet()
                        log.error(e)
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        then:
        exceptionCount.get() == 0

        List<String> ussrAttrs = userAttrsRepo.findAll().findAll({it.userId.toLowerCase().startsWith("CreateNewUserAttrTestsUser".toLowerCase())}).collect({ it.userId })
        ussrAttrs.size() == numUsers
        ussrAttrs.sort() == ussrAttrs.unique().sort()
    }
}
