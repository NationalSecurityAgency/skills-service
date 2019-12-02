package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class UserAdminMetricsSpec extends DefaultIntSpec {

    def "get metrics for a user without any skills"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def metrics = skillsService.getUserStats(proj1.projectId, userId)
        then:
        metrics.numSkills == 0
        metrics.userTotalPoints == 0
    }

    def "get metrics for a user with skills"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 35
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date())

        when:
        def metrics = skillsService.getUserStats(proj1.projectId, userId)
        then:
        metrics.numSkills == 2
        metrics.userTotalPoints == 70
    }
}
