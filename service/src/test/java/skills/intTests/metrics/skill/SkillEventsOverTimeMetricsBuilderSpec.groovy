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
package skills.intTests.metrics.skill

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.MetricsParams

class SkillEventsOverTimeMetricsBuilderSpec  extends DefaultIntSpec {

    String metricsId = "skillEventsOverTimeChartBuilder"

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

    def "number events over time"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 5 }

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
        res[0].countsByDay.collect { it.num } == [1, 2, 3, 4, 5]
        res[0].countsByDay.collect { new Date(it.timestamp) } == [days[1], days[2], days[3], days[4], days[5]]

        res[1].countsByDay.collect { it.num } == [2, 3, 4, 5]
        res[1].countsByDay.collect { new Date(it.timestamp) } == [days[2], days[3], days[4], days[5]]

        res[2].countsByDay.collect { it.num } == [3, 4, 5]
        res[2].countsByDay.collect { new Date(it.timestamp) } == [days[3], days[4], days[5]]

        res[3].countsByDay.collect { it.num } == [4, 5]
        res[3].countsByDay.collect { new Date(it.timestamp) } == [days[4], days[5]]

        res[4].countsByDay.collect { it.num } == [5]
        res[4].countsByDay.collect { new Date(it.timestamp) } == [days[5]]

        res[5].countsByDay.collect { it.num } == []
        res[6].countsByDay.collect { it.num } == []
        res[7].countsByDay.collect { it.num } == []
        res[8].countsByDay.collect { it.num } == []
        res[9].countsByDay.collect { it.num } == []
    }

}
