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
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.ProjectSortingService
import skills.services.settings.SettingsDataAccessor
import skills.storage.model.Setting
import skills.storage.model.SkillDef
import skills.storage.repos.LevelDefRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SettingRepo
import skills.storage.repos.SkillDefRepo
import spock.lang.IgnoreIf

import java.util.concurrent.atomic.AtomicInteger

@Slf4j
@IgnoreIf({ SpockSettings.DB_NOT_POSTGRESQL })
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
        assert !settingRepo.findAll().findAll { !it.settingGroup.startsWith("public_") }
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
        assert !settingRepo.findAll().findAll { !it.settingGroup.startsWith("public_") }
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

        List<String> settingsAsStrings = settingRepo.findAll().collect({ "${it.projectId}-${it.userRefId}" })
        List<String> projectIds = projDefRepo.findAll().collect({ it.projectId })
        List<String> projectNames = projDefRepo.findAll().collect({ it.name })
        List<String> levels = levelDefRepo.findAll().collect { "${it.projectRefId}-${it.level}" }
        then:
        settingsAsStrings.sort() == settingsAsStrings.unique().sort()
        projectIds.collect { it.toLowerCase() }.sort() == projectIds.collect { it.toLowerCase() }.unique().sort()
        projectNames.sort() == projectNames.unique().sort()
        levels.collect { it.toLowerCase() }.sort() == levels.collect { it.toLowerCase() }.unique().sort()
        projectIds.size() == numProj
    }

    @Autowired
    SettingsDataAccessor settingsDataAccessor

    def "move projects' order concurrently - move down"() {
        assert !settingRepo.findAll().findAll { !it.settingGroup.startsWith("public_") }
        int numThreads = 2
        int numProj = 25

        (1..numProj).collect {
            def proj = SkillsFactory.createProject(it)
            skillsService.createProject(proj)
            return proj
        }
        AtomicInteger numExceptions = new AtomicInteger()
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..20).each {
                    def project = skillsService.getProjects().first()
                    try {
                        skillsService.moveProjectDown(project)
                    } catch (Exception e) {
                        numExceptions.incrementAndGet()
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<Setting> sortOrder = settingsDataAccessor.getUserProjectSettingsForGroup(skillsService.userName, ProjectSortingService.PROJECT_SORT_GROUP)

        then:
        sortOrder.collect({it.value}).sort() == sortOrder.collect({it.value}).unique().sort()
        numExceptions.get() == 0
    }

    def "move projects' order concurrently - move up"() {
        int numThreads = 2
        int numProj = 25

        (1..numProj).collect {
            def proj = SkillsFactory.createProject(it)
            skillsService.createProject(proj)
            return proj
        }
        AtomicInteger numExceptions = new AtomicInteger()
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..20).each {
                    def project = skillsService.getProjects().last()
                    try {
                        skillsService.moveProjectUp(project)
                    } catch (Exception e) {
                        numExceptions.incrementAndGet()
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<Setting> sortOrder = settingsDataAccessor.getUserProjectSettingsForGroup(skillsService.userName, ProjectSortingService.PROJECT_SORT_GROUP)

        then:
        sortOrder.collect({it.value}).sort() == sortOrder.collect({it.value}).unique().sort()
        numExceptions.get() == 0
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
        subjects.collect({ it.skillId.toLowerCase() }).sort() == subjects.collect({
            it.skillId.toLowerCase()
        }).unique().sort()
        subjects.size() == numSubjects
    }

    def "move subjects' order concurrently - move down"() {
        def proj = SkillsFactory.createProject()
        int numThreads = 5
        int numSubjects = 25

        skillsService.createProject(proj)
        (1..numSubjects).each {
            def subject = SkillsFactory.createSubject(1, it)
            skillsService.createSubject(subject)
        }
        AtomicInteger exceptionCount = new AtomicInteger()
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..20).each {
                    def subjects = skillsService.getSubjects(proj.projectId)
                    try {
                        skillsService.moveSubjectDown(subjects.first())
                    } catch (SkillsClientException e) {
                        exceptionCount.incrementAndGet()
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<SkillDef> subjects = skillDefRepo.findAllByProjectIdAndType(proj.projectId, SkillDef.ContainerType.Subject)
        then:
        exceptionCount.get() == 0
        subjects.collect({ it.displayOrder}).sort() == subjects.collect({ it.displayOrder}).unique().sort()
    }

    def "move subjects' order concurrently - move up"() {
        def proj = SkillsFactory.createProject()
        int numThreads = 5
        int numSubjects = 25

        skillsService.createProject(proj)
        (1..numSubjects).each {
            def subject = SkillsFactory.createSubject(1, it)
            skillsService.createSubject(subject)
        }
        AtomicInteger exceptionCount = new AtomicInteger()
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..20).each {
                    def subjects = skillsService.getSubjects(proj.projectId)
                    try {
                        skillsService.moveSubjectUp(subjects.last())
                    } catch (SkillsClientException e) {
                        exceptionCount.incrementAndGet()
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<SkillDef> subjects = skillDefRepo.findAllByProjectIdAndType(proj.projectId, SkillDef.ContainerType.Subject)
        then:
        exceptionCount.get() == 0
        subjects.collect({ it.displayOrder}).sort() == subjects.collect({ it.displayOrder}).unique().sort()
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
        badges.collect({ it.skillId.toLowerCase() }).sort() == badges.collect({
            it.skillId.toLowerCase()
        }).unique().sort()
        badges.size() == numberBadges
    }

    def "move badges' order concurrently - move down"() {
        def proj = SkillsFactory.createProject()
        int numThreads = 5
        int numSubjects = 25

        skillsService.createProject(proj)
        (1..numSubjects).each {
            def badge = SkillsFactory.createBadge(1, it)
            skillsService.createBadge(badge)
        }
        AtomicInteger exceptionCount = new AtomicInteger()
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..20).each {
                    def badges = skillsService.getBadges(proj.projectId)
                    try {
                        skillsService.moveBadgeDown(badges.first())
                    } catch (SkillsClientException e) {
                        exceptionCount.incrementAndGet()
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<SkillDef> badges = skillDefRepo.findAllByProjectIdAndType(proj.projectId, SkillDef.ContainerType.Badge)
        then:
        exceptionCount.get() == 0
        badges.collect({ it.displayOrder}).sort() == badges.collect({ it.displayOrder}).unique().sort()
    }

    def "move badges' order concurrently - move up"() {
        def proj = SkillsFactory.createProject()
        int numThreads = 5
        int numSubjects = 25

        skillsService.createProject(proj)
        (1..numSubjects).each {
            def badge = SkillsFactory.createBadge(1, it)
            skillsService.createBadge(badge)
        }
        AtomicInteger exceptionCount = new AtomicInteger()
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..20).each {
                    def badges = skillsService.getBadges(proj.projectId)
                    try {
                        skillsService.moveBadgeUp(badges.last())
                    } catch (SkillsClientException e) {
                        exceptionCount.incrementAndGet()
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<SkillDef> badges = skillDefRepo.findAllByProjectIdAndType(proj.projectId, SkillDef.ContainerType.Badge)
        then:
        exceptionCount.get() == 0
        badges.collect({ it.displayOrder}).sort() == badges.collect({ it.displayOrder}).unique().sort()
    }


    def "do not create duplicate global badges"() {
        SkillsService supervisorSkillsService = createSupervisorService()

        int numThreads = 5
        int numberBadges = 25

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
        badges.collect({ it.skillId.toLowerCase() }).sort() == badges.collect({
            it.skillId.toLowerCase()
        }).unique().sort()
        badges.size() == numberBadges
    }

    private SkillsService createSupervisorService() {
        String ultimateRoot = 'jh@dojo.com'
        SkillsService rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
        rootSkillsService.grantRoot()
        String supervisorUserId = 'foo@bar.com'
        SkillsService supervisorSkillsService = createService(supervisorUserId)
        rootSkillsService.grantSupervisorRole(supervisorUserId)
        return supervisorSkillsService
    }

    def "move global badges' order concurrently - move down"() {
        SkillsService supervisorSkillsService = createSupervisorService()

        int numThreads = 5
        int numItems = 25

        (1..numItems).each {
            def badge = SkillsFactory.createBadge(1, it)
            supervisorSkillsService.createGlobalBadge(badge)
        }
        AtomicInteger exceptionCount = new AtomicInteger()
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..20).each {
                    def badges = supervisorSkillsService.getAllGlobalBadges()
                    try {
                        supervisorSkillsService.moveGlobalBadgeDown(badges.first())
                    } catch (SkillsClientException e) {
                        exceptionCount.incrementAndGet()
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<SkillDef> badges = skillDefRepo.findAllByProjectIdAndType(null, SkillDef.ContainerType.GlobalBadge)
        then:
        exceptionCount.get() == 0
        badges.collect({ it.displayOrder}).sort() == badges.collect({ it.displayOrder}).unique().sort()
    }

    def "move global badges' order concurrently - move up"() {
        SkillsService supervisorSkillsService = createSupervisorService()

        int numThreads = 5
        int numItems = 25

        (1..numItems).each {
            def badge = SkillsFactory.createBadge(1, it)
            supervisorSkillsService.createGlobalBadge(badge)
        }
        AtomicInteger exceptionCount = new AtomicInteger()
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..20).each {
                    def badges = supervisorSkillsService.getAllGlobalBadges()
                    try {
                        supervisorSkillsService.moveGlobalBadgeUp(badges.last())
                    } catch (SkillsClientException e) {
                        exceptionCount.incrementAndGet()
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<SkillDef> badges = skillDefRepo.findAllByProjectIdAndType(null, SkillDef.ContainerType.GlobalBadge)
        then:
        exceptionCount.get() == 0
        badges.collect({ it.displayOrder}).sort() == badges.collect({ it.displayOrder}).unique().sort()
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
        skills.collect({ it.skillId.toLowerCase() }).sort() == skills.collect({
            it.skillId.toLowerCase()
        }).unique().sort()
        skills.size() == numSkills
    }

    def "move skills' order concurrently - move down"() {
        def proj = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()
        skillsService.createProject(proj)
        skillsService.createSubject(subject)

        int numThreads = 5
        int numSkills = 20
        (1..numSkills).each {
            def skill = SkillsFactory.createSkill(1, 1, it)
            skillsService.createSkill(skill)
        }
        AtomicInteger exceptionCount = new AtomicInteger()
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..20).each {
                    def skills = skillsService.getSkillsForSubject(proj.projectId, subject.subjectId)
                    skills.first().subjectId = subject.subjectId
                    try {
                        skillsService.moveSkillDown(skills.first())
                    } catch (SkillsClientException e) {
                        exceptionCount.incrementAndGet()
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<SkillDef> skills = skillDefRepo.findAllByProjectIdAndType(proj.projectId, SkillDef.ContainerType.Skill)
        then:
        exceptionCount.get() == 0
        skills.collect({ it.displayOrder}).sort() == skills.collect({ it.displayOrder}).unique().sort()
    }

    def "move skills' order concurrently - move up"() {
        def proj = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()
        skillsService.createProject(proj)
        skillsService.createSubject(subject)

        int numThreads = 5
        int numSkills = 20
        (1..numSkills).each {
            def skill = SkillsFactory.createSkill(1, 1, it)
            skillsService.createSkill(skill)
        }
        AtomicInteger exceptionCount = new AtomicInteger()
        when:
        List<Thread> threads = (1..numThreads).collect { int threadNum ->
            Thread.start {
                (1..20).each {
                    def skills = skillsService.getSkillsForSubject(proj.projectId, subject.subjectId)
                    skills.last().subjectId = subject.subjectId
                    try {
                        skillsService.moveSkillUp(skills.last())
                    } catch (SkillsClientException e) {
                        exceptionCount.incrementAndGet()
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        List<SkillDef> skills = skillDefRepo.findAllByProjectIdAndType(proj.projectId, SkillDef.ContainerType.Skill)
        then:
        exceptionCount.get() == 0
        skills.collect({ it.displayOrder}).sort() == skills.collect({ it.displayOrder}).unique().sort()
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
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
