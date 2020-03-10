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
package skills.intTests.supervisor

import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class SupervisorEditSpecs extends DefaultIntSpec {

        String projId = SkillsFactory.defaultProjId
        String badgeId = 'GlobalBadge1'

        String ultimateRoot = 'jh@dojo.com'
        SkillsService rootSkillsService
        String nonRootUserId = 'foo@bar.com'
        SkillsService nonSupervisorSkillsService

        def setup(){
            skillsService.deleteProjectIfExist(projId)
            skillsService.deleteProjectIfExist("${projId}2")
            rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
            nonSupervisorSkillsService = createService(nonRootUserId)

            if (!rootSkillsService.isRoot()) {
                rootSkillsService.grantRoot()
            }
            rootSkillsService.grantSupervisorRole(skillsService.wsHelper.username)

            skillsService.deleteGlobalIcon([filename: "dot2.png"])
        }

        def cleanup() {
            rootSkillsService?.removeSupervisorRole(skillsService.wsHelper.username)
        }

    def 'global badge creation'() {
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: projId, subjectId: subj, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1*60, numMaxOccurrencesIncrementInterval: 1, dependentSkillsIds: [skill1.skillId, skill2.skillId, skill3.skillId]]

        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']
        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId, skill4.skillId]

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "3")
        requiredSkillsIds.each { skillId ->
            skillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }

        def res = skillsService.getGlobalBadge(badgeId)

        then:
        res
        res.badgeId == badgeId
        res.projectId == null
        res.name == 'Test Global Badge 1'
        res.numSkills == 4
        res.requiredSkills.size() == 4
        res.requiredProjectLevels.size() == 1

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }

    def "remove skill from a global badge"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        List<Map> skills = SkillsFactory.createSkills(3,)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createGlobalBadge(badge)

        def globalAssingment = [projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(1).skillId]
        when:
        skillsService.assignSkillToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId)
        skillsService.assignSkillToGlobalBadge(globalAssingment)
        def beforeBadge = skillsService.getGlobalBadge(badge.badgeId)
        def skillsBefore = skillsService.getGlobalBadgeSkills(badge.badgeId)
        skillsService.removeSkillFromGlobalBadge(globalAssingment)
        def afterBadge = skillsService.getGlobalBadge(badge.badgeId)
        def skillsAfter = skillsService.getGlobalBadgeSkills(badge.badgeId)
        then:
        beforeBadge.requiredSkills.collect { it.skillId }.sort() == ["skill1", "skill2"]
        afterBadge.requiredSkills.collect { it.skillId }.sort() == ["skill1"]

        skillsBefore.collect { it.skillId }.sort() == ["skill1", "skill2"]
        skillsAfter.collect { it.skillId }.sort() == ["skill1"]
    }

    def "remove level from a global badge"() {
        def proj = SkillsFactory.createProject()
        def proj2 = SkillsFactory.createProject(2)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createProject(proj2)

        skillsService.createGlobalBadge(badge)

        def globalAssingment = [projectId: proj2.projectId, badgeId: badge.badgeId, level: "3"]

        when:
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "1")
        skillsService.assignProjectLevelToGlobalBadge(globalAssingment)

        def beforeBadge = skillsService.getGlobalBadge(badge.badgeId)
        skillsService.removeProjectLevelFromGlobalBadge(globalAssingment)
        def afterBadge = skillsService.getGlobalBadge(badge.badgeId)
        then:
        beforeBadge.requiredProjectLevels.sort(){it.projectId}.collect({"${it.projectId}-${it.level}".toString()}) == ["TestProject1-1", "TestProject2-3"]
        afterBadge.requiredProjectLevels.sort(){it.projectId}.collect({"${it.projectId}-${it.level}".toString()}) == ["TestProject1-1"]
    }

    def "get project's levels"() {
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        when:
        def res = skillsService.getLevelsForProject(proj.projectId)
        then:
        res.collect { it.level } == [1, 2, 3, 4, 5]
    }

    def 'users without SUPERVISOR role cannot create global badges'() {

        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']

        when:
        nonSupervisorSkillsService.createGlobalBadge(badge)

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'global badge delete'() {

        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge)
        def res = skillsService.getAllGlobalBadges()
        assert res.size() == 1

        when:
        skillsService.deleteGlobalBadge(badgeId)
        res = skillsService.getAllGlobalBadges()

        then:
        !res
    }

    def 'global badge name already exists endpoint'() {
        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge)

        when:
        def res = skillsService.doesGlobalBadgeNameExists(badge.name)

        then:
        res

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }

    def 'cannot create global badge where the name already exists'() {
        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge)

        when:
        Map badge2 = [badgeId: 'differentIdSameName', name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge2)

        then:
        SkillsClientException ex = thrown()
        ex.message.contains('Badge with name [Test Global Badge 1] already exists!')

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }

    def 'global badge id already exists endpoint'() {
        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge)

        when:
        def res = skillsService.doesGlobalBadgeIdExists(badge.badgeId)

        then:
        res

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }

    def 'update global badge id'() {
        String origBadgeId = 'origBadgeId'
        Map badge = [badgeId: origBadgeId, name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge)

        when:
        badge['badgeId'] = badgeId
        def res = skillsService.createGlobalBadge(badge, origBadgeId)
        def res2 = skillsService.getGlobalBadge(badgeId)

        then:
        res
        res2

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }

    def 'update global badge name'() {
        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge)

        when:
        String newName = 'new name'
        badge['name'] = newName
        def res = skillsService.createGlobalBadge(badge, badgeId)
        def res2 = skillsService.getGlobalBadge(badgeId)

        then:
        res
        res2.name == newName

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }

    def 'update global badge name and id'() {
        String origBadgeId = 'origBadgeId'
        Map badge = [badgeId: origBadgeId, name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge)

        when:
        String newName = 'new name'
        badge['name'] = newName
        badge['badgeId'] = badgeId
        def res = skillsService.createGlobalBadge(badge, origBadgeId)
        def res2 = skillsService.getGlobalBadge(badgeId)

        then:
        res
        res2.name == newName

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }

    def 'global badge cannot have the same skill added twice'() {
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: skill1.skillId)

        when:
        skillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: skill1.skillId)

        then:
        SkillsClientException ex = thrown()
        ex.message.contains("errorCode:ConstraintViolation")
        ex.message.contains("explanation:Provided skill id has already been added to this global badge.") || ex.message.contains("explanation:Data Integrity Violation")

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }

    def 'global badge cannot have more than one level for the same project'() {
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: skill1.skillId)

        when:
        skillsService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "3")

        then:
        SkillsClientException ex = thrown()
        ex.message.contains("errorCode:ConstraintViolation")
        ex.message.contains("explanation:Provided project already has a level assigned for this global badge.") || ex.message.contains("explanation:Data Integrity Violation")

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }

    def 'project admin cannot delete skill referenced by global badge'() {
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: skill1.skillId)

        when:
        skillsService.deleteSkill([projectId: projId, subjectId: subj, skillId: "skill1"])

        then:
        SkillsClientException ex = thrown()
        ex.message.contains("Skill with id [skill1] cannot be deleted as it is currently referenced by one or more global badges")

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }

    def 'project admin cannot delete subject referenced by global badge'() {
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: skill1.skillId)

        when:
        skillsService.deleteSubject([projectId: projId, subjectId: subj])

        then:
        SkillsClientException ex = thrown()
        ex.message.contains("Subject with id [${subj}] cannot be deleted as it is currently referenced by one or more global badges")

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }

    def 'project admin cannot delete project level referenced by global badge'() {
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "5")
        skillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: skill1.skillId)

        when:
        skillsService.deleteLevel(projId)

        then:
        SkillsClientException ex = thrown()
        ex.message.contains("Level [5] for project with id [TestProject1] cannot be deleted as it is currently referenced by one or more global badges")

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }

    def 'project admin cannot delete project referenced by global badge'() {
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: skill1.skillId)

        when:
        skillsService.deleteProject(projId)

        then:
        SkillsClientException ex = thrown()
        ex.message.contains("Project with id [TestProject1] cannot be deleted as it is currently referenced by one or more global badges")

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }
    def "upload global icon"(){
        ClassPathResource resource = new ClassPathResource("/dot2.png")

        when:
        def file = resource.getFile()
        def result = skillsService.uploadGlobalIcon(file)

        then:
        result
        result.success
        result.cssClassName == "GLOBAL-dot2png"
        result.name == "dot2.png"
    }

    def "delete global icon"(){
        ClassPathResource resource = new ClassPathResource("/dot2.png")

        when:
        def file = resource.getFile()
        skillsService.uploadGlobalIcon(file)
        skillsService.deleteGlobalIcon([filename: "dot2.png"])
        def result = skillsService.getIconCssForGlobalIcons()

        then:
        !result
    }

    def "get css for global icons"(){
        ClassPathResource resource = new ClassPathResource("/dot2.png")

        when:
        def file = resource.getFile()
        skillsService.uploadGlobalIcon(file)
        def result = skillsService.getIconCssForGlobalIcons()
        def clientDisplayCssResult = skillsService.getCustomClientDisplayCss()

        then:
        result == [[filename:'dot2.png', cssClassname:"GLOBAL-dot2png"]]
        clientDisplayCssResult.toString().startsWith(".GLOBAL-dot2png {\tbackground-image: url(")
    }

    def 'global badge lookups do not return inception project or skills'() {
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: projId, subjectId: subj, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1*60, numMaxOccurrencesIncrementInterval: 1, dependentSkillsIds: [skill1.skillId, skill2.skillId, skill3.skillId]]
        Map skill5 = [projectId: projId, subjectId: subj, skillId: "skill5", name  : "Test Skill 5", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']
        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId, skill4.skillId]

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createProject([projectId: "${projId}2".toString(), name: "Test Project 2"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "3")
        requiredSkillsIds.each { skillId ->
            skillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }

        def res = skillsService.getGlobalBadge(badgeId)
        def inception = rootSkillsService.getProject("Inception")
        def availableProjects = skillsService.getAvailableProjectsForGlobalBadge(badgeId)
        def availableSkills = skillsService.getAvailableSkillsForGlobalBadge(badgeId, "")

        then:
        res
        res.badgeId == badgeId
        res.projectId == null
        res.name == 'Test Global Badge 1'
        res.numSkills == 4
        res.requiredSkills.size() == 4
        res.requiredProjectLevels.size() == 1
        inception
        availableProjects
        !availableProjects.find { it.projectId == 'Inception'}
        availableSkills
        !availableSkills.suggestedSkills.find { it.projectId == "Inception" }

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }
}
