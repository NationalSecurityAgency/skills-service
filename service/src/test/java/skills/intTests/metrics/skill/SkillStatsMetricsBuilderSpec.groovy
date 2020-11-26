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

class SkillStatsMetricsBuilderSpec extends DefaultIntSpec {

    String metricsId = "singleSkillCountsChartBuilder"

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
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 2 }

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
                        if (index % 2 == 0) {
                            skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, date - 6.days)
                        }
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
        res[0].numUsersAchieved == 4
        new Date(res[0].lastAchieved) == days[4]
        res[0].numUsersInProgress == 1

        res[1].numUsersAchieved == 4
        new Date(res[1].lastAchieved) == days[4]
        res[1].numUsersInProgress == 1

        res[2].numUsersAchieved == 4
        new Date(res[2].lastAchieved) == days[4]
        res[2].numUsersInProgress == 1

        res[3].numUsersAchieved == 4
        new Date(res[3].lastAchieved) == days[4]
        res[3].numUsersInProgress == 1

        res[4].numUsersAchieved == 0
        !res[4].lastAchieved
        res[4].numUsersInProgress == 5

        res[5].numUsersAchieved == 0
        !res[5].lastAchieved
        res[5].numUsersInProgress == 0

        res[6].numUsersAchieved == 0
        !res[6].lastAchieved
        res[6].numUsersInProgress == 0

        res[7].numUsersAchieved == 0
        !res[7].lastAchieved
        res[7].numUsersInProgress == 0

        res[8].numUsersAchieved == 0
        !res[8].lastAchieved
        res[8].numUsersInProgress == 0

        res[9].numUsersAchieved == 0
        !res[9].lastAchieved
        res[9].numUsersInProgress == 0
    }
}
