package skills.intTests.metrics.skill


import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.MetricsParams

class NumUserAchievedOverTimeMetricsBuilderSpec extends DefaultIntSpec {

    String metricsId = "numUserAchievedOverTimeChartBuilder"

    def "must supply skillId param"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        Map props = [:]

        when:
        skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation == "Metrics[${metricsId}]: Must supply skillId param"
    }

    def "number achievements over time"() {
        List<String> users = (1..10).collect { "user$it" }
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 1 }


        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        List<Date> days

        use(TimeCategory) {
            days = (5..0).collect { int day -> day.days.ago }
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    skills.subList(0, index).each { skill ->
                        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        Map props = [:]
        props[MetricsParams.P_SKILL_ID] = skills[0].skillId

        when:
        List res = skills.collect {
            props[MetricsParams.P_SKILL_ID] = it.skillId
            return skillsService.getMetricsData(proj.projectId, metricsId, props)
        }
        then:
        res[0].achievementCounts.collect { it.num } == [1, 2, 3, 4, 5]
        res[0].achievementCounts.collect { new Date(it.timestamp) } == [days[1], days[2], days[3], days[4], days[5]]

        res[1].achievementCounts.collect { it.num } == [2, 3, 4, 5]
        res[1].achievementCounts.collect { new Date(it.timestamp) } == [days[2], days[3], days[4], days[5]]

        res[2].achievementCounts.collect { it.num } == [3, 4, 5]
        res[2].achievementCounts.collect { new Date(it.timestamp) } == [days[3], days[4], days[5]]

        res[3].achievementCounts.collect { it.num } == [4, 5]
        res[3].achievementCounts.collect { new Date(it.timestamp) } == [days[4], days[5]]

        res[4].achievementCounts.collect { it.num } == [5]
        res[4].achievementCounts.collect { new Date(it.timestamp) } == [days[5]]

        res[5].achievementCounts.collect { it.num } == []
        res[6].achievementCounts.collect { it.num } == []
        res[7].achievementCounts.collect { it.num } == []
        res[8].achievementCounts.collect { it.num } == []
        res[9].achievementCounts.collect { it.num } == []
    }
}
