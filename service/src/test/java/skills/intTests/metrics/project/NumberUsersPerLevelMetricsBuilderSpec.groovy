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
import skills.intTests.utils.SkillsService
import skills.metrics.builders.MetricsParams
import spock.lang.IgnoreRest

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

        List<String> users = new ArrayList<>(getRandomUsers(16))
        List<String> usersCopy = new ArrayList<>(users)

        achieveLevelForUsers(usersCopy, skills, 4, 1)
        achieveLevelForUsers(usersCopy, skills, 2, 2)
        achieveLevelForUsers(usersCopy, skills, 4, 3)
        achieveLevelForUsers(usersCopy, skills, 5, 4)
        achieveLevelForUsers(usersCopy,skills, 1, 5)

        Map projectProps = [:]
        Map subjectProps = [(MetricsParams.P_SUBJECT_ID): subj.subjectId]

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, projectProps)
        def res1 = skillsService.getMetricsData(proj.projectId, metricsId, subjectProps)

        skillsService.archiveUsers([users[2]], proj.projectId)

        def resAfterArchive = skillsService.getMetricsData(proj.projectId, metricsId, projectProps)
        def res1AfterArchive = skillsService.getMetricsData(proj.projectId, metricsId, subjectProps)

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

        resAfterArchive.size() == 5
        resAfterArchive[0].value == "Level 1"
        resAfterArchive[0].count == 3
        resAfterArchive[1].value == "Level 2"
        resAfterArchive[1].count == 2
        resAfterArchive[2].value == "Level 3"
        resAfterArchive[2].count == 4
        resAfterArchive[3].value == "Level 4"
        resAfterArchive[3].count == 5
        resAfterArchive[4].value == "Level 5"
        resAfterArchive[4].count == 1

        res1AfterArchive.size() == 5
        res1AfterArchive[0].value == "Level 1"
        res1AfterArchive[0].count == 3
        res1AfterArchive[1].value == "Level 2"
        res1AfterArchive[1].count == 2
        res1AfterArchive[2].value == "Level 3"
        res1AfterArchive[2].count == 4
        res1AfterArchive[3].value == "Level 4"
        res1AfterArchive[3].count == 5
        res1AfterArchive[4].value == "Level 5"
        res1AfterArchive[4].count == 1
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

        List<String> users = new ArrayList<>(getRandomUsers(30))
        List<String> usersCopy = new ArrayList<>(users)

        achieveLevelForUsers(usersCopy, skills, 4, 1, "Subject")
        achieveLevelForUsers(usersCopy, skills, 2, 2, "Subject")
        achieveLevelForUsers(usersCopy, skills, 4, 3, "Subject")
        achieveLevelForUsers(usersCopy, skills, 5, 4, "Subject")
        achieveLevelForUsers(usersCopy, skills, 1, 5, "Subject")

        achieveLevelForUsers(usersCopy, skillsSubj1, 1, 1, "Subject")
        achieveLevelForUsers(usersCopy, skillsSubj1, 4, 2, "Subject")
        achieveLevelForUsers(usersCopy, skillsSubj1, 2, 3, "Subject")
        achieveLevelForUsers(usersCopy, skillsSubj1, 1, 4, "Subject")
        achieveLevelForUsers(usersCopy, skillsSubj1, 2, 5, "Subject")

        Map subjectProps = [(MetricsParams.P_SUBJECT_ID): subj.subjectId]
        Map subject1Props = [(MetricsParams.P_SUBJECT_ID): subj1.subjectId]

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, subjectProps)
        def res1 = skillsService.getMetricsData(proj.projectId, metricsId, subject1Props)

        skillsService.archiveUsers([users[2]], proj.projectId)

        def resAfterArchive = skillsService.getMetricsData(proj.projectId, metricsId, subjectProps)
        def res1AfterArchive = skillsService.getMetricsData(proj.projectId, metricsId, subjectProps)

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

        resAfterArchive.size() == 5
        resAfterArchive[0].value == "Level 1"
        resAfterArchive[0].count == 3
        resAfterArchive[1].value == "Level 2"
        resAfterArchive[1].count == 2
        resAfterArchive[2].value == "Level 3"
        resAfterArchive[2].count == 4
        resAfterArchive[3].value == "Level 4"
        resAfterArchive[3].count == 5
        resAfterArchive[4].value == "Level 5"
        resAfterArchive[4].count == 1

        res1AfterArchive.size() == 5
        res1AfterArchive[0].value == "Level 1"
        res1AfterArchive[0].count == 3
        res1AfterArchive[1].value == "Level 2"
        res1AfterArchive[1].count == 2
        res1AfterArchive[2].value == "Level 3"
        res1AfterArchive[2].count == 4
        res1AfterArchive[3].value == "Level 4"
        res1AfterArchive[3].count == 5
        res1AfterArchive[4].value == "Level 5"
        res1AfterArchive[4].count == 1
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

        List<String> users = new ArrayList<>(getRandomUsers(30))
        List<String> usersCopy = new ArrayList<>(users)
        List<String> usersCopy1 = new ArrayList<>(users)

        achieveLevelForUsers(usersCopy, skills, 4, 1, "Subject")
        achieveLevelForUsers(usersCopy, skills, 2, 2, "Subject")
        achieveLevelForUsers(usersCopy, skills, 4, 3, "Subject")
        achieveLevelForUsers(usersCopy, skills, 5, 4, "Subject")

        achieveLevelForUsers(usersCopy1, skillsSubj1, 1, 1, "Subject")
        achieveLevelForUsers(usersCopy1, skillsSubj1, 4, 2, "Subject")
        achieveLevelForUsers(usersCopy1, skillsSubj1, 2, 3, "Subject")
        achieveLevelForUsers(usersCopy1, skillsSubj1, 1, 4, "Subject")
        achieveLevelForUsers(usersCopy1, skillsSubj1, 2, 5, "Subject")
        achieveLevelForUsers(usersCopy1, skillsSubj1, 1, 6, "Subject")

        Map projectProps = [:]
        Map subjectProps = [(MetricsParams.P_SUBJECT_ID): subj.subjectId]
        Map subject1Props = [(MetricsParams.P_SUBJECT_ID): subj1.subjectId]

        when:
        def resOverall = skillsService.getMetricsData(proj.projectId, metricsId, projectProps)
        def resSubj1 = skillsService.getMetricsData(proj.projectId, metricsId, subjectProps)
        def resSubj2 = skillsService.getMetricsData(proj.projectId, metricsId, subject1Props)

        skillsService.archiveUsers([users[2]], proj.projectId)

        def resOverallAfterArchive = skillsService.getMetricsData(proj.projectId, metricsId, projectProps)
        def resSubj1AfterArchive = skillsService.getMetricsData(proj.projectId, metricsId, subjectProps)
        def resSubj2AfterArchive = skillsService.getMetricsData(proj.projectId, metricsId, subject1Props)


        then:
        resOverall.size() == 4
        resOverall[0].value == "Level 1"
        resOverall[0].count == 4
        resOverall[1].value == "Level 2"
        resOverall[1].count == 6
        resOverall[2].value == "Level 3"
        resOverall[2].count == 2
        resOverall[3].value == "Level 4"
        resOverall[3].count == 3

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

        resOverallAfterArchive.size() == 4
        resOverallAfterArchive[0].value == "Level 1"
        resOverallAfterArchive[0].count == 3
        resOverallAfterArchive[1].value == "Level 2"
        resOverallAfterArchive[1].count == 6
        resOverallAfterArchive[2].value == "Level 3"
        resOverallAfterArchive[2].count == 2
        resOverallAfterArchive[3].value == "Level 4"
        resOverallAfterArchive[3].count == 3

        resSubj1AfterArchive.size() == 4
        resSubj1AfterArchive[0].value == "Level 1"
        resSubj1AfterArchive[0].count == 3
        resSubj1AfterArchive[1].value == "Level 2"
        resSubj1AfterArchive[1].count == 2
        resSubj1AfterArchive[2].value == "Level 3"
        resSubj1AfterArchive[2].count == 4
        resSubj1AfterArchive[3].value == "Level 4"
        resSubj1AfterArchive[3].count == 5

        resSubj2AfterArchive.size() == 6
        resSubj2AfterArchive[0].value == "Level 1"
        resSubj2AfterArchive[0].count == 1
        resSubj2AfterArchive[1].value == "Level 2"
        resSubj2AfterArchive[1].count == 3
        resSubj2AfterArchive[2].value == "Level 3"
        resSubj2AfterArchive[2].count == 2
        resSubj2AfterArchive[3].value == "Level 4"
        resSubj2AfterArchive[3].count == 1
        resSubj2AfterArchive[4].value == "Level 5"
        resSubj2AfterArchive[4].count == 2
        resSubj2AfterArchive[5].value == "Level 6"
        resSubj2AfterArchive[5].count == 1
    }

    def "number of users achieved per level for user tag"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> users = new ArrayList<>(getRandomUsers(16))
        List<String> usersCopy = new ArrayList<>(users)

        SkillsService rootUser = createRootSkillService()
        users.each { userId ->
            rootUser.saveUserTag(userId, "tag1", ["value1"])
        }

        achieveLevelForUsers(usersCopy, skills, 4, 1)
        achieveLevelForUsers(usersCopy, skills, 2, 2)
        achieveLevelForUsers(usersCopy, skills, 4, 3)
        achieveLevelForUsers(usersCopy, skills, 5, 4)
        achieveLevelForUsers(usersCopy,skills, 1, 5)

        Map projectProps = [:]
        Map tagProps = [(MetricsParams.P_TAG_KEY): "tag1", (MetricsParams.P_TAG_FILTER): "value1"]

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, projectProps)
        def res1 = skillsService.getMetricsData(proj.projectId, metricsId, tagProps)

        skillsService.archiveUsers([users[2]], proj.projectId)

        def resAfterArchive = skillsService.getMetricsData(proj.projectId, metricsId, projectProps)
        def res1AfterArchive = skillsService.getMetricsData(proj.projectId, metricsId, tagProps)

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

        resAfterArchive.size() == 5
        resAfterArchive[0].value == "Level 1"
        resAfterArchive[0].count == 3
        resAfterArchive[1].value == "Level 2"
        resAfterArchive[1].count == 2
        resAfterArchive[2].value == "Level 3"
        resAfterArchive[2].count == 4
        resAfterArchive[3].value == "Level 4"
        resAfterArchive[3].count == 5
        resAfterArchive[4].value == "Level 5"
        resAfterArchive[4].count == 1

        res1AfterArchive.size() == 5
        res1AfterArchive[0].value == "Level 1"
        res1AfterArchive[0].count == 3
        res1AfterArchive[1].value == "Level 2"
        res1AfterArchive[1].count == 2
        res1AfterArchive[2].value == "Level 3"
        res1AfterArchive[2].count == 4
        res1AfterArchive[3].value == "Level 4"
        res1AfterArchive[3].count == 5
        res1AfterArchive[4].value == "Level 5"
        res1AfterArchive[4].count == 1
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
        boolean found = false
        int skillIndex = 0
        while (!found) {
            def res = skillsService.addSkill([projectId: skills[skillIndex].projectId, skillId: skills[skillIndex].skillId], user)
            found = res.body.completed.findAll({ it.type == type })?.find { it.level == level }
            skillIndex++
        }
    }
}
