package skills.intTests.supervisor

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
            rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
            nonSupervisorSkillsService = createService(nonRootUserId)

            if (!rootSkillsService.isRoot()) {
                rootSkillsService.grantRoot()
            }
            rootSkillsService.grantSupervisorRole(skillsService.wsHelper.username)
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
}
