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
package skills.intTests.metrics.subjects

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.MetricsParams
import spock.lang.Shared

class UsersByLevelForSubjectOverTimeMetricsBuilderSpec extends DefaultIntSpec {

    String metricsId = "usersByLevelForSubjectOverTimeChartBuilder"
    @Shared
    List<Date> dates

    def setupSpec() {
        use(TimeCategory) {
            dates = (10..0).collect({
                return it.days.ago;
            })
        }
    }

    def "no skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        Map props = [:]
        props[MetricsParams.P_SUBJECT_ID] = subj.subjectId

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        !res
    }

    def "must supply subject id param"() {
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
        body.explanation == "Metrics[${metricsId}]: Must supply subjectId param"
    }

    def "number achievements over time"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 1 }


        skillsService.createProject(proj)
        skillsService.createSubject(subj)
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
        props[MetricsParams.P_SUBJECT_ID] = subj.subjectId

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        println JsonOutput.prettyPrint(JsonOutput.toJson(res))

        then:
        res
        res[0].counts.collect { it.count } == [0, 1, 2, 3, 4, 5]
        res[0].counts.collect { new Date(it.value) } == [days[0], days[1], days[2], days[3], days[4], days[5]]

        res[1].counts.collect { it.count } == [0, 3, 4, 5]
        res[1].counts.collect { new Date(it.value) } == [days[2], days[3], days[4], days[5]]

        res[2].counts.collect { it.count } == [0, 5]
        res[2].counts.collect { new Date(it.value) } == [days[4], days[5]]
    }
}
