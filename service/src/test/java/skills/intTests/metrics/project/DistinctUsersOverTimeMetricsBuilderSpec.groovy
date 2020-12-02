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
package skills.intTests.metrics.project


import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.MetricsParams

class DistinctUsersOverTimeMetricsBuilderSpec extends DefaultIntSpec {

    String metricsId = "distinctUsersOverTimeForProject"

    def "must supply start param"() {
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
        body.explanation == "Metrics[${metricsId}]: Must supply start param"
    }

    def "start param must be within last 3 years"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        Map props = [:]
        use(TimeCategory) {
            props[MetricsParams.P_START_TIMESTAMP] = (3.years.ago - 1.day).time
        }

        when:
        skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation.contains("Metrics[${metricsId}]: start param timestamp must be within last 3 years.")
    }

    def "start param in the future will not return anything"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        List<Date> days
        List<String> users = (1..10).collect { "user$it" }
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
        use(TimeCategory) {
            props[MetricsParams.P_START_TIMESTAMP] = 1.day.from.now.time
        }

        when:
        def res5Days = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        !res5Days
    }

    def "number of users growing over few days"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }


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

        when:
        def res5Days = skillsService.getMetricsData(proj.projectId, metricsId, getProps(5))
        def res6Days = skillsService.getMetricsData(proj.projectId, metricsId, getProps(6))
        def res4Days = skillsService.getMetricsData(proj.projectId, metricsId, getProps(4))
        def res3Days = skillsService.getMetricsData(proj.projectId, metricsId, getProps(3))
        then:
        res5Days.size() == 6
        res5Days.collect({ it.count }) == [0, 1, 2, 3, 4, 5,]
        res5Days.collect({ it.value }) == days.collect({ it.time })

        res6Days.size() == 7
        res6Days.collect({ it.count }) == [0, 0, 1, 2, 3, 4, 5,]

        res4Days.size() == 5
        res4Days.collect({ it.count }) == [1, 2, 3, 4, 5,]
        res4Days.collect({ it.value }) == days.subList(1, days.size()).collect({ it.time })

        res3Days.size() == 4
        res3Days.collect({ it.count }) == [2, 3, 4, 5,]
        res3Days.collect({ it.value }) == days.subList(2, days.size()).collect({ it.time })
    }

    def "number of users growing over few days - specific skill"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 10 }

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

        when:
        def res5Days = skillsService.getMetricsData(proj.projectId, metricsId, getProps(5, skills[1].skillId))
        def res6Days = skillsService.getMetricsData(proj.projectId, metricsId, getProps(6, skills[1].skillId))
        def res4Days = skillsService.getMetricsData(proj.projectId, metricsId, getProps(4, skills[1].skillId))
        def skill3FiveDays = skillsService.getMetricsData(proj.projectId, metricsId, getProps(5, skills[2].skillId))
        then:
        res5Days.size() == 6
        res5Days.collect({ it.count }) == [0, 0, 2, 3, 4, 5]
        res5Days.collect({ it.value }) == days.collect({ it.time })

        res6Days.size() == 7
        res6Days.collect({ it.count }) == [0, 0, 0, 2, 3, 4, 5]

        res4Days.size() == 5
        res4Days.collect({ it.count }) == [0, 2, 3, 4, 5]
        res4Days.collect({ it.value }) == days.subList(1, days.size()).collect({ it.time })

        skill3FiveDays.size() == 6
        skill3FiveDays.collect({ it.count }) == [0, 0, 0, 3, 4, 5]
        skill3FiveDays.collect({ it.value }) == days.collect({ it.time })
    }


    private Map getProps(int numDaysAgo, String skillId = null) {
        Map props = [:]
        use(TimeCategory) {
            props[MetricsParams.P_START_TIMESTAMP] = numDaysAgo.days.ago.time
            if (skillId) {
                props[MetricsParams.P_SKILL_ID] = skillId
            }
        }
        return props
    }
}
