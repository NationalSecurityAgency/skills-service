package skills.intTests.metrics

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class SkillUsageNavigatorChartBuilderSpec extends DefaultIntSpec {

    def "skill usage navigator"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(10)
//        skills.each { it.numPerformToCompletion = 1 }

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        println skillsService.addSkill([projectId: skills[0].projectId, skillId: skills[0].skillId], userId, new Date())
        println skillsService.addSkill([projectId: skills[0].projectId, skillId: skills[1].skillId], userId, new Date())

        when:
        def res = skillsService.getMetricsData(skills[0].projectId, "skillUsageNavigatorChartBuilder")
        then:
        res
    }
}
