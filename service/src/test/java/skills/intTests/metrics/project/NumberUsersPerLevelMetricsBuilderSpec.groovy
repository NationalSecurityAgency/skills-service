package skills.intTests.metrics.project

import groovy.json.JsonOutput
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class NumberUsersPerLevelMetricsBuilderSpec extends DefaultIntSpec {

    String metricsId = "numUsersPerLevelChartBuilder"

    def "number of users achieved per level"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        achieveLevelForUsers(skills, 4, 1)
        achieveLevelForUsers(skills, 2, 2)
        achieveLevelForUsers(skills, 4, 3)
        achieveLevelForUsers(skills, 5, 4)
        achieveLevelForUsers(skills, 1, 5)

        Map props = [:]

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        println JsonOutput.toJson(res)
        then:
        res.size() == 5
        res[0].value == "Level 1"
        res[0].count == 4
        res[1].value == "Level 2"
        res[1].count == 2
        res[2].value == "Level 3"
        res[2].count == 4
        res[3].value == "Level 4"
        res[3].count == 5
        res[4].value == "Level 5"
        res[4].count == 1
    }

    private void achieveLevelForUsers(List<Map> skills, int numUsers, int level) {
        (1..numUsers).each {
            String user = "user${it}_level${level}"
            achieveLevel(skills, user, level)
        }
    }

    private void achieveLevel(List<Map> skills, String user, int level) {
        boolean found = false
        int skillIndex = 0
        do {
            def res = skillsService.addSkill([projectId: skills[skillIndex].projectId, skillId: skills[skillIndex].skillId], user)
            println "${user} for [${level}] ${res}"
            found = res.body.completed.find({ it.type == "Overall" })?.level == level
            skillIndex++
        } while (!found)
    }
}
