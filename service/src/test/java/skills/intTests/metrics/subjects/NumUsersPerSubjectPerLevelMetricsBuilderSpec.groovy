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
import groovy.time.TimeCategory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import spock.lang.IgnoreRest
import spock.lang.Shared

class NumUsersPerSubjectPerLevelMetricsBuilderSpec extends DefaultIntSpec {

    String metricsId = "numUsersPerSubjectPerLevelChartBuilder"
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

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())

        Map props = [:]

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        res.size() == 1
        res[0].subject == 'Test Subject #1'
        res[0].numUsersPerLevels.size() == 5
        res[0].numUsersPerLevels[0].level == 1
        res[0].numUsersPerLevels[0].numberUsers == 0
        res[0].numUsersPerLevels[1].level == 2
        res[0].numUsersPerLevels[1].numberUsers == 0
        res[0].numUsersPerLevels[2].level == 3
        res[0].numUsersPerLevels[2].numberUsers == 0
        res[0].numUsersPerLevels[3].level == 4
        res[0].numUsersPerLevels[3].numberUsers == 0
        res[0].numUsersPerLevels[4].level == 5
        res[0].numUsersPerLevels[4].numberUsers == 0
    }

    def "num users per level"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(5)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def subj1 = SkillsFactory.createSubject(1, 2)
        List<Map> skillsSubj1 = SkillsFactory.createSkills(5, 1, 2)
        skillsSubj1.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createSubject(subj1)
        skillsService.createSkills(skillsSubj1)

        List<String> users = new ArrayList<>(getRandomUsers(10))
        List<String> usersCopy = new ArrayList<>(users)
        achieveLevelForUsers(usersCopy, skills, 2, 1, "Subject")
        achieveLevelForUsers(usersCopy, skills, 1, 2, "Subject")
        achieveLevelForUsers(usersCopy, skills, 4, 3, "Subject")
        achieveLevelForUsers(usersCopy, skillsSubj1, 1, 1, "Subject")

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, [:])
        then:
        res.size() == 2
        def subj1Res = res.find( { it.subject == 'Test Subject #1'})
        subj1Res
        subj1Res.numUsersPerLevels.size() == 5
        subj1Res.numUsersPerLevels[0].level == 1
        subj1Res.numUsersPerLevels[0].numberUsers == 2
        subj1Res.numUsersPerLevels[1].level == 2
        subj1Res.numUsersPerLevels[1].numberUsers == 1
        subj1Res.numUsersPerLevels[2].level == 3
        subj1Res.numUsersPerLevels[2].numberUsers == 4
        subj1Res.numUsersPerLevels[3].level == 4
        subj1Res.numUsersPerLevels[3].numberUsers == 0
        subj1Res.numUsersPerLevels[4].level == 5
        subj1Res.numUsersPerLevels[4].numberUsers == 0

        def subj2Res = res.find( { it.subject == 'Test Subject #2'})
        subj2Res
        subj2Res.numUsersPerLevels.size() == 5
        subj2Res.numUsersPerLevels[0].level == 1
        subj2Res.numUsersPerLevels[0].numberUsers == 1
        subj2Res.numUsersPerLevels[1].level == 2
        subj2Res.numUsersPerLevels[1].numberUsers == 0
        subj2Res.numUsersPerLevels[2].level == 3
        subj2Res.numUsersPerLevels[2].numberUsers == 0
        subj2Res.numUsersPerLevels[3].level == 4
        subj2Res.numUsersPerLevels[3].numberUsers == 0
        subj2Res.numUsersPerLevels[4].level == 5
        subj2Res.numUsersPerLevels[4].numberUsers == 0
    }

    def "num users per level - more than just default levels"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(20)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def subj1 = SkillsFactory.createSubject(1, 2)
        List<Map> skillsSubj1 = SkillsFactory.createSkills(5, 1, 2)
        skillsSubj1.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createSubject(subj1)
        skillsService.createSkills(skillsSubj1)

        skillsService.addLevel(proj.projectId, subj.subjectId, [percent: 94])
        skillsService.addLevel(proj.projectId, subj.subjectId, [percent: 99])

        List<String> users = new ArrayList<>(getRandomUsers(15))
        List<String> usersCopy = new ArrayList<>(users)
        achieveLevelForUsers(usersCopy, skills, 2, 1, "Subject")
        achieveLevelForUsers(usersCopy, skills, 1, 2, "Subject")
        achieveLevelForUsers(usersCopy, skills, 4, 3, "Subject")
        achieveLevelForUsers(usersCopy, skills, 1, 6, "Subject")
        achieveLevelForUsers(usersCopy, skills, 2, 7, "Subject")
        achieveLevelForUsers(usersCopy, skillsSubj1, 1, 1, "Subject")

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, [:])
        res.each {
            println JsonOutput.toJson(it)
        }
        then:
        res.size() == 2
        def subj1Res = res.find( { it.subject == 'Test Subject #1'})
        subj1Res
        subj1Res.numUsersPerLevels.size() == 7
        subj1Res.numUsersPerLevels[0].level == 1
        subj1Res.numUsersPerLevels[0].numberUsers == 2
        subj1Res.numUsersPerLevels[1].level == 2
        subj1Res.numUsersPerLevels[1].numberUsers == 1
        subj1Res.numUsersPerLevels[2].level == 3
        subj1Res.numUsersPerLevels[2].numberUsers == 4
        subj1Res.numUsersPerLevels[3].level == 4
        subj1Res.numUsersPerLevels[3].numberUsers == 0
        subj1Res.numUsersPerLevels[4].level == 5
        subj1Res.numUsersPerLevels[4].numberUsers == 0
        subj1Res.numUsersPerLevels[5].level == 6
        subj1Res.numUsersPerLevels[5].numberUsers == 1
        subj1Res.numUsersPerLevels[6].level == 7
        subj1Res.numUsersPerLevels[6].numberUsers == 2

        def subj2Res = res.find( { it.subject == 'Test Subject #2'})
        subj2Res
        subj2Res.numUsersPerLevels.size() == 5
        subj2Res.numUsersPerLevels[0].level == 1
        subj2Res.numUsersPerLevels[0].numberUsers == 1
        subj2Res.numUsersPerLevels[1].level == 2
        subj2Res.numUsersPerLevels[1].numberUsers == 0
        subj2Res.numUsersPerLevels[2].level == 3
        subj2Res.numUsersPerLevels[2].numberUsers == 0
        subj2Res.numUsersPerLevels[3].level == 4
        subj2Res.numUsersPerLevels[3].numberUsers == 0
        subj2Res.numUsersPerLevels[4].level == 5
        subj2Res.numUsersPerLevels[4].numberUsers == 0
    }

    private void achieveLevelForUsers(List<String> users, List<Map> skills, int numUsers, int level, String type = "Overall") {
        List<String> usersToUse = (1..numUsers).collect({
            String user = users.pop()
            assert user
            return user
        })

        usersToUse.each {
            achieveLevel(skills, it, level, type)
        }
    }

    private void achieveLevel(List<Map> skills, String user, int level, String type = "Overall") {
        use(TimeCategory) {
            boolean found = false
            int skillIndex = 0
            while (!found) {
                def res = skillsService.addSkill([projectId: skills[skillIndex].projectId, skillId: skills[skillIndex].skillId], user, dates[level])
                found = res.body.completed.findAll({ it.type == type })?.find { it.level == level }
                skillIndex++
            }
        }
    }
}
