package skills.intTests.reportSkills

import org.joda.time.DateTime
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class ReportSkills_GlobalBadgeSkillsSpecs extends DefaultIntSpec {

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

    def "give credit if all dependencies were fulfilled"(){
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: projId, subjectId: subj, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1*60, numMaxOccurrencesIncrementInterval: 1, dependentSkillsIds: [skill1.skillId, skill2.skillId, skill3.skillId]]

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

        DateTime dt = new DateTime().minusDays(4)

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId], "user1", dt.toDate()).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId], "user1", dt.plusDays(1).toDate()).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId], "user1", dt.plusDays(2).toDate()).body
        def resSkill4 = skillsService.addSkill([projectId: projId, skillId: skill4.skillId], "user1", dt.plusDays(3).toDate()).body

        then:
        resSkill1.skillApplied && !resSkill1.completed.find { it.id == badgeId}
        resSkill2.skillApplied && !resSkill2.completed.find { it.id == badgeId}
        resSkill3.skillApplied && !resSkill3.completed.find { it.id == badgeId}
        resSkill4.skillApplied && resSkill4.completed.find { it.id == badgeId}

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }


}
