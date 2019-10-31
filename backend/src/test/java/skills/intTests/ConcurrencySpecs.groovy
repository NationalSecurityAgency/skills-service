package skills.intTests

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef
import skills.storage.repos.LevelDefRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SettingRepo
import skills.storage.repos.SkillDefRepo

import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class ConcurrencySpecs extends DefaultIntSpec {


    @Autowired
    SettingRepo settingRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    LevelDefRepo levelDefRepo

    @Autowired
    SkillDefRepo skillDefRepo

    def "do not create duplicates when changing project setting concurrently"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)

        def settingsToSave = (0..50).collect {
            [
                    setting: "testSetting${it}".toString(),
                    value  : [projectId: proj1.projectId, setting: "testSetting${it}".toString(), value: "true"]
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

    def "do not create duplicates projects or settings when concurrently inserting projects"() {
        int numThreads = 5
        int numProj = 25
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..numProj).each {
                    try {
                        def proj = SkillsFactory.createProject(it)
                        proj.projectId = uppperCaseOneChar(proj.projectId, threadNum)
                        proj.name = uppperCaseOneChar(proj.name, threadNum)

                        skillsService.createProject(proj)
                    } catch (SkillsClientException e) {
                        // should throw dup projects, that's what we are trying to break
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<String> settingsAsStrings = settingRepo.findAll().collect({ "${it.projectId}-${it.userId}" })
        List<String> projectIds = projDefRepo.findAll().collect({ it.projectId })
        List<String> projectNames = projDefRepo.findAll().collect({ it.name })
        List<String> levels = levelDefRepo.findAll().collect { "${it.projectId}-${it.level}" }
        then:
        settingsAsStrings.sort() == settingsAsStrings.unique().sort()
        projectIds.collect {it.toLowerCase()}.sort() == projectIds.collect {it.toLowerCase()}.unique().sort()
        projectNames.sort() == projectNames.unique().sort()
        levels.collect {it.toLowerCase()}.sort() == levels.collect {it.toLowerCase()}.unique().sort()
        projectIds.size() == numProj
    }


    private String uppperCaseOneChar(String str, int charToUpper) {
        String res = str.toLowerCase()
        charToUpper = charToUpper % str.size()
        "${res.substring(0, charToUpper)}${res[charToUpper].toUpperCase()}${res.substring(charToUpper + 1, res.size())}"
    }


    def "do not create duplicate subjects"() {
        def proj = SkillsFactory.createProject()
        int numThreads = 5
        int numSubjects = 25

        skillsService.createProject(proj)
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..numSubjects).each {
                    try {
                        def subject = SkillsFactory.createSubject(1, it)
                        subject.subjectId = uppperCaseOneChar(subject.subjectId, threadNum)
                        subject.name = uppperCaseOneChar(subject.name, threadNum)

                        skillsService.createSubject(subject)
                    } catch (SkillsClientException e) {
                        // should throw dup projects, that's what we are trying to break
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<SkillDef> subjects = skillDefRepo.findAllByProjectIdAndType(proj.projectId, SkillDef.ContainerType.Subject)
        then:
        subjects.collect({ it.skillId.toLowerCase() }).sort() == subjects.collect({ it.skillId.toLowerCase() }).unique().sort()
        subjects.size() == numSubjects
    }

    def "do not create duplicate badges"() {
        def proj = SkillsFactory.createProject()
        int numThreads = 5
        int numberBadges = 25

        skillsService.createProject(proj)
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..numberBadges).each {
                    try {
                        def badge = SkillsFactory.createBadge(1, it)
                        badge.badgeId = uppperCaseOneChar(badge.badgeId, threadNum)
                        badge.name = uppperCaseOneChar(badge.name, threadNum)

                        skillsService.createBadge(badge)
                    } catch (SkillsClientException e) {
                        // should throw dup projects, that's what we are trying to break
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<SkillDef> badges = skillDefRepo.findAllByProjectIdAndType(proj.projectId, SkillDef.ContainerType.Badge)
        then:
        badges.collect({ it.skillId.toLowerCase() }).sort() == badges.collect({ it.skillId.toLowerCase() }).unique().sort()
        badges.size() == numberBadges
    }

    def "do not create duplicate global badges"() {
        String ultimateRoot = 'jh@dojo.com'
        SkillsService rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
        rootSkillsService.grantRoot()
        String supervisorUserId = 'foo@bar.com'
        SkillsService supervisorSkillsService = createService(supervisorUserId)
        rootSkillsService.grantSupervisorRole(supervisorUserId)

        def proj = SkillsFactory.createProject()
        int numThreads = 5
        int numberBadges = 25

        skillsService.createProject(proj)
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..numberBadges).each {
                    try {
                        def badge = SkillsFactory.createBadge(1, it)
                        badge.badgeId = uppperCaseOneChar(badge.badgeId, threadNum)
                        badge.name = uppperCaseOneChar(badge.name, threadNum)

                        supervisorSkillsService.createGlobalBadge(badge)
                    } catch (SkillsClientException e) {
                        // should throw dup projects, that's what we are trying to break
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<SkillDef> badges = skillDefRepo.findAllByProjectIdAndType(null, SkillDef.ContainerType.GlobalBadge)
        then:
        badges.collect({ it.skillId.toLowerCase() }).sort() == badges.collect({ it.skillId.toLowerCase() }).unique().sort()
        badges.size() == numberBadges
    }

    def "do not create duplicate skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        int numThreads = 5
        int numSkills = 20

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..numSkills).each {
                    try {
                        def skill = SkillsFactory.createSkill(1, 1, it)
                        skill.skillId = uppperCaseOneChar(skill.skillId, threadNum)
                        skill.name = uppperCaseOneChar(skill.name, threadNum)

                        skillsService.createSkill(skill)
                    } catch (SkillsClientException e) {
                        // should throw dup projects, that's what we are trying to break
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<SkillDef> skills = skillDefRepo.findAllByProjectIdAndType(proj.projectId, SkillDef.ContainerType.Skill)
        then:
        skills.collect({ it.skillId.toLowerCase() }).sort() == skills.collect({ it.skillId.toLowerCase() }).unique().sort()
        skills.size() == numSkills
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

        List<String> ussrAttrs = userAttrsRepo.findAll().findAll({
            it.userId.toLowerCase().startsWith("CreateNewUserAttrTestsUser".toLowerCase())
        }).collect({ it.userId })
        ussrAttrs.size() == numUsers
        ussrAttrs.sort() == ussrAttrs.unique().sort()
    }

    def "/api endpoints with concurrent admin endpoints do not affect permissions"() {
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
            int threadNum = it
            Thread.start {
                users.each {
                    try {
                        if (threadNum % 2 != 0) {
                            skillsService.getProjectUsers(proj.projectId)
                        } else {
                            skillsService.suggestClientUsers('CreateNewUserAttrTestsUser')
                        }
                    } catch (SkillsClientException e) {
                        exceptionCount.incrementAndGet()
                        log.error("Error in thread [${threadNum}]", e)
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        then:
        exceptionCount.get() == 0
    }

}
