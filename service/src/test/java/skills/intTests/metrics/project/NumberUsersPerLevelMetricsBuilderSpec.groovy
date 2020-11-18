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


import groovy.json.JsonOutput
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.MetricsParams

class NumberUsersPerLevelMetricsBuilderSpec extends DefaultIntSpec {

    String metricsId = "numUsersPerLevelChartBuilder"

    def "empty res"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Map props = [:]
        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        props[MetricsParams.P_SUBJECT_ID] = subj.subjectId
        def res1 = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        res.size() == 5
        res[0].value == "Level 1"
        res[0].count == 0
        res[1].value == "Level 2"
        res[1].count == 0
        res[2].value == "Level 3"
        res[2].count == 0
        res[3].value == "Level 4"
        res[3].count == 0
        res[4].value == "Level 5"
        res[4].count == 0

        res1.size() == 5
        res1[0].value == "Level 1"
        res1[0].count == 0
        res1[1].value == "Level 2"
        res1[1].count == 0
        res1[2].value == "Level 3"
        res1[2].count == 0
        res1[3].value == "Level 4"
        res1[3].count == 0
        res1[4].value == "Level 5"
        res1[4].count == 0
    }

    def "number of users achieved per level"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        achieveLevelForUsers(skills, 4, 1)
        achieveLevelForUsers(skills, 2, 2)
        achieveLevelForUsers(skills, 4, 3)
        achieveLevelForUsers(skills, 5, 4)
        achieveLevelForUsers(skills, 1, 5)

        Map props = [:]

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        props[MetricsParams.P_SUBJECT_ID] = subj.subjectId
        def res1 = skillsService.getMetricsData(proj.projectId, metricsId, props)

//        println JsonOutput.toJson(res1)
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

        res1.size() == 5
        res1[0].value == "Level 1"
        res1[0].count == 4
        res1[1].value == "Level 2"
        res1[1].count == 2
        res1[2].value == "Level 3"
        res1[2].count == 4
        res1[3].value == "Level 4"
        res1[3].count == 5
        res1[4].value == "Level 5"
        res1[4].count == 1
    }

    def "number of users achieved per level for subject"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def subj1 = SkillsFactory.createSubject(1, 2)
        List<Map> skillsSubj1 = SkillsFactory.createSkills(10, 1, 2)
        skillsSubj1.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createSubject(subj1)
        skillsService.createSkills(skillsSubj1)

        achieveLevelForUsers(skills, 4, 1, "Subject")
        achieveLevelForUsers(skills, 2, 2, "Subject")
        achieveLevelForUsers(skills, 4, 3, "Subject")
        achieveLevelForUsers(skills, 5, 4, "Subject")
        achieveLevelForUsers(skills, 1, 5, "Subject")

        achieveLevelForUsers(skillsSubj1, 1, 1, "Subject")
        achieveLevelForUsers(skillsSubj1, 4, 2, "Subject")
        achieveLevelForUsers(skillsSubj1, 2, 3, "Subject")
        achieveLevelForUsers(skillsSubj1, 1, 4, "Subject")
        achieveLevelForUsers(skillsSubj1, 2, 5, "Subject")

        Map props = [:]
        props[MetricsParams.P_SUBJECT_ID] = subj.subjectId
        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsParams.P_SUBJECT_ID] = subj1.subjectId
        def res1 = skillsService.getMetricsData(proj.projectId, metricsId, props)

//        println JsonOutput.toJson(res1)
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

        res1.size() == 5
        res1[0].value == "Level 1"
        res1[0].count == 1
        res1[1].value == "Level 2"
        res1[1].count == 4
        res1[2].value == "Level 3"
        res1[2].count == 2
        res1[3].value == "Level 4"
        res1[3].count == 1
        res1[4].value == "Level 5"
        res1[4].count == 2
    }

    def "number of users achieved per level for subject - different number of levels"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def subj1 = SkillsFactory.createSubject(1, 2)
        List<Map> skillsSubj1 = SkillsFactory.createSkills(15, 1, 2)
        skillsSubj1.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createSubject(subj1)
        skillsService.createSkills(skillsSubj1)

        skillsService.deleteLevel(proj.projectId, subj.subjectId)
        Map levelProps = [percent: 98, name: 'TwoEagles', iconClass: 'fas fa-two-eagles']
        skillsService.addLevel(proj.projectId, subj1.subjectId, levelProps)
        skillsService.deleteLevel(proj.projectId)

        println JsonOutput.toJson(skillsService.getLevels(proj.projectId, subj1.subjectId))

        achieveLevelForUsers(skills, 4, 1, "Subject")
        achieveLevelForUsers(skills, 2, 2, "Subject")
        achieveLevelForUsers(skills, 4, 3, "Subject")
        achieveLevelForUsers(skills, 5, 4, "Subject")

        achieveLevelForUsers(skillsSubj1, 1, 1, "Subject")
        achieveLevelForUsers(skillsSubj1, 4, 2, "Subject")
        achieveLevelForUsers(skillsSubj1, 2, 3, "Subject")
        achieveLevelForUsers(skillsSubj1, 1, 4, "Subject")
        achieveLevelForUsers(skillsSubj1, 2, 5, "Subject")
        achieveLevelForUsers(skillsSubj1, 1, 6, "Subject")

        Map props = [:]

        when:
        def resOverall = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsParams.P_SUBJECT_ID] = subj.subjectId
        def resSubj1 = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsParams.P_SUBJECT_ID] = subj1.subjectId
        def resSubj2 = skillsService.getMetricsData(proj.projectId, metricsId, props)

        then:
        resOverall.size() == 4
        resOverall[0].value == "Level 1"
        resOverall[0].count == 5
        resOverall[1].value == "Level 2"
        resOverall[1].count == 6
        resOverall[2].value == "Level 3"
        resOverall[2].count == 5
        resOverall[3].value == "Level 4"
        resOverall[3].count == 1

        resSubj1.size() == 4
        resSubj1[0].value == "Level 1"
        resSubj1[0].count == 4
        resSubj1[1].value == "Level 2"
        resSubj1[1].count == 2
        resSubj1[2].value == "Level 3"
        resSubj1[2].count == 4
        resSubj1[3].value == "Level 4"
        resSubj1[3].count == 5

        resSubj2.size() == 6
        resSubj2[0].value == "Level 1"
        resSubj2[0].count == 1
        resSubj2[1].value == "Level 2"
        resSubj2[1].count == 4
        resSubj2[2].value == "Level 3"
        resSubj2[2].count == 2
        resSubj2[3].value == "Level 4"
        resSubj2[3].count == 1
        resSubj2[4].value == "Level 5"
        resSubj2[4].count == 2
        resSubj2[5].value == "Level 6"
        resSubj2[5].count == 1
    }

    private void achieveLevelForUsers(List<Map> skills, int numUsers, int level, String type = "Overall") {
        (1..numUsers).each {
            String user = "user${it}_level${level}"
            achieveLevel(skills, user, level, type)
        }
    }

    private void achieveLevel(List<Map> skills, String user, int level, String type = "Overall") {
        boolean found = false
        int skillIndex = 0
        while (!found) {
            def res = skillsService.addSkill([projectId: skills[skillIndex].projectId, skillId: skills[skillIndex].skillId], user)
            println "${user} for [${level}] ${res}"
            found = res.body.completed.findAll({ it.type == type })?.find { it.level == level }
            skillIndex++
        }
    }
}
