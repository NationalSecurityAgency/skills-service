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
package skills.intTests

import groovy.time.TimeCategory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.intTests.utils.TestUtils
import spock.lang.Specification

import java.text.DateFormat

class RuleSetDeletionsSpecs  extends DefaultIntSpec {

    TestUtils testUtils = new TestUtils()
    DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd")

    String projId = SkillsFactory.defaultProjId

    List<String> sampleUserIds // loaded from system props

    def setup() {
        skillsService.deleteProjectIfExist(projId)
        sampleUserIds = System.getProperty("sampleUserIds", "tom|||dick|||harry")?.split("\\|\\|\\|").sort()
        assert sampleUserIds?.size() >= 3, 'These tests require at least 3 users'
    }

    def "when skilled is removed total score should properly update"() {
        String subj = "testSubj"

        Map skill1 = [projectId     : projId, subjectId: subj, skillId: "skill1", name: "Test Skill 1", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId     : projId, subjectId: subj, skillId: "skill2", name: "Test Skill 2", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 2, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId     : projId, subjectId: subj, skillId: "skill3", name: "Test Skill 3", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 3, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        def subject = skillsService.getSubject([projectId: projId, subjectId: subj])

        skillsService.deleteSkill([projectId: projId, subjectId: subj, skillId: "skill2"])

        def subjectAfterDelete = skillsService.getSubject([projectId: projId, subjectId: subj])

        then:
        subject.totalPoints == 60
        subjectAfterDelete.totalPoints == 40
    }

    def "when a skill is removed the project total score should properly update"() {
        String subj = "testSubj"

        Map skill1 = [projectId     : projId, subjectId: subj, skillId: "skill1", name: "Test Skill 1", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId     : projId, subjectId: subj, skillId: "skill2", name: "Test Skill 2", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 2, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId     : projId, subjectId: subj, skillId: "skill3", name: "Test Skill 3", type: "Skill",
                      pointIncrement: 10, numPerformToCompletion: 3, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        def project = skillsService.getProject(projId)

        skillsService.deleteSkill([projectId: projId, subjectId: subj, skillId: "skill2"])

        def projectAfterDelete = skillsService.getProject(projId)

        then:
        project.totalPoints == 60
        projectAfterDelete.totalPoints == 40
    }

    def "when subject is deleted project's total points must be re-calculated"(){
        List<Map> subj1 = (1..3).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: it, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: it, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..5).collect { [projectId: projId, subjectId: "subj3", skillId: "23${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: it, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj1.first().subjectId, name: "Test Subject 1"])
        skillsService.createSubject([projectId: projId, subjectId: subj2.first().subjectId, name: "Test Subject 2"])
        skillsService.createSubject([projectId: projId, subjectId: subj3.first().subjectId, name: "Test Subject 3"])

        subj1.each { Map params ->
            skillsService.createSkill(params)
        }
        subj2.each {
            skillsService.createSkill(it)
        }
        subj3.each {
            skillsService.createSkill(it)
        }

        def project = skillsService.getProject(projId)
        skillsService.deleteSubject([projectId: projId, subjectId: subj2.first().subjectId])
        def projectAfterDelete = skillsService.getProject(projId)

        then:
        project.totalPoints == 31
        projectAfterDelete.totalPoints == 21
    }

    def "when subject is deleted display order should be rebuilt to start with 0"(){
        List<Map> subj1 = (1..1).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: it, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..1).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: it, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..1).collect { [projectId: projId, subjectId: "subj3", skillId: "23${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: it, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj1.first().subjectId, name: "Test Subject 1"])
        skillsService.createSubject([projectId: projId, subjectId: subj2.first().subjectId, name: "Test Subject 2"])
        skillsService.createSubject([projectId: projId, subjectId: subj3.first().subjectId, name: "Test Subject 3"])

        subj1.each { Map params ->
            skillsService.createSkill(params)
        }
        subj2.each {
            skillsService.createSkill(it)
        }
        subj3.each {
            skillsService.createSkill(it)
        }

        def subjects = skillsService.getSubjects(projId)
        skillsService.deleteSubject([projectId: projId, subjectId: subj1.first().subjectId])
        def subjectsAfterDelete = skillsService.getSubjects(projId)

        then:
        subjects.size() == 3
        subjects.get(0).displayOrder == 0
        subjects.get(1).displayOrder == 1
        subjects.get(1).subjectId == "subj2"
        subjects.get(2).displayOrder == 2
        subjects.get(2).subjectId == "subj3"

        subjectsAfterDelete.size() == 2
        subjectsAfterDelete.get(0).displayOrder == 0
        subjectsAfterDelete.get(0).subjectId == "subj2"
        subjectsAfterDelete.get(1).displayOrder == 1
        subjectsAfterDelete.get(1).subjectId == "subj3"
    }

    def "validate that user point history was properly adjusted when a skill is removed"(){
        List<Map> subj1 = (1..3).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj1.first().subjectId, name: "Test Subject 1"])
        skillsService.createSubject([projectId: projId, subjectId: subj2.first().subjectId, name: "Test Subject 2"])
        subj1.each { Map params ->
            skillsService.createSkill(params)
        }
        subj2.each { Map params ->
            skillsService.createSkill(params)
        }

        List<Date> dates = []
        use (TimeCategory) {
            (0..7).each {
                Date theDate = it.days.ago
                dates.add(theDate)
            }
        }
        dates.sort()

        dates.eachWithIndex { Date theDate, int index ->
            skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], sampleUserIds.get(0), theDate)
            skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], sampleUserIds.get(1), theDate)
            skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], sampleUserIds.get(2), theDate)

            if ((index % 2) == 0) {
                skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], sampleUserIds.get(0), theDate)
                skillsService.addSkill([projectId: projId, skillId: subj2.get(2).skillId], sampleUserIds.get(0), theDate)
            }
            if ((index % 3) == 0) {
                skillsService.addSkill([projectId: projId, skillId: subj2.get(1).skillId], sampleUserIds.get(0), theDate)
                skillsService.addSkill([projectId: projId, skillId: subj2.get(3).skillId], sampleUserIds.get(0), theDate)
            }
        }

        String user1Id = sampleUserIds.get(0)
        String user2Id = sampleUserIds.get(1)
        String user3Id = sampleUserIds.get(2)
        def user1PointHistory = skillsService.getPointHistory(user1Id, projId)
        def user2PointHistory = skillsService.getPointHistory(user2Id, projId)
        def user3PointHistory = skillsService.getPointHistory(user3Id, projId)

        Closure getPoints = { def ptsHistory, int index -> ptsHistory.pointsHistory.find { df.parse(it.dayPerformed) == dates.get(index) }.points }

        when:
        skillsService.deleteSkill([projectId: projId, subjectId: subj1.get(0).subjectId, skillId: subj1.get(0).skillId])
        def afterRemovalUser1PointHistory = skillsService.getPointHistory(user1Id, projId)
        def afterRemovalUser2PointHistory = skillsService.getPointHistory(user2Id, projId)
        def afterRemovalUser3PointHistory = skillsService.getPointHistory(user3Id, projId)

        then:
        getPoints.call(user1PointHistory, 0) == 80
        getPoints.call(user1PointHistory, 1) == 90
        getPoints.call(user1PointHistory, 2) == 130
        getPoints.call(user1PointHistory, 3) == 180
        getPoints.call(user1PointHistory, 4) == 220
        getPoints.call(user1PointHistory, 5) == 230
        getPoints.call(user1PointHistory, 6) == 310
        getPoints.call(user1PointHistory, 7) == 320

        getPoints.call(afterRemovalUser1PointHistory, 0) == 70
        getPoints.call(afterRemovalUser1PointHistory, 1) == 70
        getPoints.call(afterRemovalUser1PointHistory, 2) == 100
        getPoints.call(afterRemovalUser1PointHistory, 3) == 140
        getPoints.call(afterRemovalUser1PointHistory, 4) == 170
        getPoints.call(afterRemovalUser1PointHistory, 5) == 170
        getPoints.call(afterRemovalUser1PointHistory, 6) == 240
        getPoints.call(afterRemovalUser1PointHistory, 7) == 240

        getPoints.call(user2PointHistory, 0) == 10
        getPoints.call(user2PointHistory, 1) == 20
        getPoints.call(user2PointHistory, 2) == 30
        getPoints.call(user2PointHistory, 3) == 40
        getPoints.call(user2PointHistory, 4) == 50
        getPoints.call(user2PointHistory, 5) == 60
        getPoints.call(user2PointHistory, 6) == 70
        getPoints.call(user2PointHistory, 7) == 80

        !afterRemovalUser2PointHistory.pointsHistory

        getPoints.call(user3PointHistory, 0) == 10
        getPoints.call(user3PointHistory, 1) == 20
        getPoints.call(user3PointHistory, 2) == 30
        getPoints.call(user3PointHistory, 3) == 40
        getPoints.call(user3PointHistory, 4) == 50
        getPoints.call(user3PointHistory, 5) == 60
        getPoints.call(user3PointHistory, 6) == 70
        getPoints.call(user3PointHistory, 7) == 80

        getPoints.call(afterRemovalUser3PointHistory, 0) == 10
        getPoints.call(afterRemovalUser3PointHistory, 1) == 20
        getPoints.call(afterRemovalUser3PointHistory, 2) == 30
        getPoints.call(afterRemovalUser3PointHistory, 3) == 40
        getPoints.call(afterRemovalUser3PointHistory, 4) == 50
        getPoints.call(afterRemovalUser3PointHistory, 5) == 60
        getPoints.call(afterRemovalUser3PointHistory, 6) == 70
        getPoints.call(afterRemovalUser3PointHistory, 7) == 80
    }

    def "when skill is removed properly adjust points for users that achieved points for that skill"(){
        List<Map> subj1 = [10,30,60].collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: it, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = [10,20,30,40].collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: it, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj1.first().subjectId, name: "Test Subject 1"])
        skillsService.createSubject([projectId: projId, subjectId: subj2.first().subjectId, name: "Test Subject 2"])
        subj1.each { Map params ->
            skillsService.createSkill(params)
        }
        subj2.each { Map params ->
            skillsService.createSkill(params)
        }

        skillsService.createSkill([projectId: projId, subjectId: "subj1", skillId: "s15".toString(), name: "subj1 5".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1])

        String user1Id = sampleUserIds.get(0)
        String user2Id = sampleUserIds.get(1)

        // SUBJECT 1
        // user 1 has skills in all 3
        skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], user1Id, new Date()) //1 //10
        skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], user1Id, new Date()) //2+1 // 20 + 30
        skillsService.addSkill([projectId: projId, skillId: subj1.get(2).skillId], user1Id, new Date()) //3+2+1 // 60 + 30 + 10
        skillsService.addSkill([projectId: projId, skillId: "s15"], user1Id, new Date()) //3+2+1 // 60 + 30 + 10 + 5

        // user 2 has 1s and 2nd only
        skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], user2Id, new Date()) //1 // 10
        skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], user2Id, new Date()) //2+1 // 20 + 10

        // SUBJECT 2
        skillsService.addSkill([projectId: projId, skillId: subj2.get(0).skillId], user1Id, new Date()) //3+2+1+1 // 60 + 30 + 10 + 10
        skillsService.addSkill([projectId: projId, skillId: subj2.get(0).skillId], user2Id, new Date()) //2+1+1 // 20 + 10 + 10 + 5



        def user1OverallSummary = skillsService.getSkillSummary(user1Id, projId)
        def user1Subj1Summary = skillsService.getSkillSummary(user1Id, projId, subj1.get(0).subjectId)
        def user1Subj2Summary = skillsService.getSkillSummary(user1Id, projId, subj2.get(0).subjectId)
        def user1PerformedSkills = skillsService.getPerformedSkills(user1Id, projId)



        def user2OverallSummary = skillsService.getSkillSummary(user2Id, projId)
        def user2Subj1Summary = skillsService.getSkillSummary(user2Id, projId, subj1.get(0).subjectId)
        def user2Subj2Summary = skillsService.getSkillSummary(user2Id, projId, subj2.get(0).subjectId)
        def user2PerformedSkills = skillsService.getPerformedSkills(user2Id, projId)

        when:

        skillsService.deleteSkill([projectId: projId, subjectId: subj1.get(0).subjectId, skillId: subj1.get(0).skillId])

        def afterDeletionUser1OverallSummary = skillsService.getSkillSummary(user1Id, projId)
        def afterDeletionUser1Subj1Summary = skillsService.getSkillSummary(user1Id, projId, subj1.get(0).subjectId)
        def afterDeletionUser1Subj2Summary = skillsService.getSkillSummary(user1Id, projId, subj2.get(0).subjectId)
        def afterDeletionUser1PerformedSkills = skillsService.getPerformedSkills(user1Id, projId)

        def afterDeletionUser2OverallSummary = skillsService.getSkillSummary(user2Id, projId)
        def afterDeletionUser2Subj1Summary = skillsService.getSkillSummary(user2Id, projId, subj1.get(0).subjectId)
        def afterDeletionUser2Subj2Summary = skillsService.getSkillSummary(user2Id, projId, subj2.get(0).subjectId)
        def afterDeletionUser2PerformedSkills = skillsService.getPerformedSkills(user2Id, projId)


        then:

        // ----- user 1 -----
        user1OverallSummary.skillsLevel == 3
        user1OverallSummary.points == 115
        user1OverallSummary.totalPoints == 205
        user1OverallSummary.levelTotalPoints == 45
        user1OverallSummary.todaysPoints == 115
        user1OverallSummary.subjects.size() == 2
        user1OverallSummary.subjects.find { it.subjectId == "subj1" }.skillsLevel == 5
        user1OverallSummary.subjects.find { it.subjectId == "subj1" }.points == 105
        user1OverallSummary.subjects.find { it.subjectId == "subj1" }.levelTotalPoints == -1
        user1OverallSummary.subjects.find { it.subjectId == "subj1" }.todaysPoints == 105
        user1OverallSummary.subjects.find { it.subjectId == "subj2" }.skillsLevel == 1
        user1OverallSummary.subjects.find { it.subjectId == "subj2" }.points == 10
        user1OverallSummary.subjects.find { it.subjectId == "subj2" }.levelTotalPoints == 15
        user1OverallSummary.subjects.find { it.subjectId == "subj2" }.todaysPoints == 10

        user1Subj1Summary.skillsLevel == 5
        user1Subj1Summary.points == 105
        user1Subj1Summary.levelTotalPoints == -1
        user1Subj1Summary.todaysPoints == 105
        user1Subj1Summary.skills.collect { it.skillId }.sort() == ["s110", "s130", "s15", "s160"]

        user1Subj2Summary.skillsLevel == 1
        user1Subj2Summary.points == 10
        user1Subj2Summary.levelTotalPoints == 15
        user1Subj2Summary.todaysPoints == 10
        user1Subj2Summary.skills.collect { it.skillId }.sort() == ["s210", "s220", "s230", "s240"]

        user1PerformedSkills.data.collect { it.skillId }.sort() == ["s110", "s130", "s15", "s160", "s210"]

        afterDeletionUser1OverallSummary.skillsLevel == 3
        afterDeletionUser1OverallSummary.points == 105
        afterDeletionUser1OverallSummary.levelTotalPoints == 43 // should count up to the 4th level since 3rd level was already achieved
        afterDeletionUser1OverallSummary.levelPoints == 18
        afterDeletionUser1OverallSummary.todaysPoints == 105
        afterDeletionUser1OverallSummary.subjects.size() == 2
        afterDeletionUser1OverallSummary.subjects.find { it.subjectId == "subj1" }.skillsLevel == 5
        afterDeletionUser1OverallSummary.subjects.find { it.subjectId == "subj1" }.points == 95
        afterDeletionUser1OverallSummary.subjects.find { it.subjectId == "subj1" }.levelTotalPoints == -1
        afterDeletionUser1OverallSummary.subjects.find { it.subjectId == "subj1" }.todaysPoints == 95
        afterDeletionUser1OverallSummary.subjects.find { it.subjectId == "subj2" }.skillsLevel == 1
        afterDeletionUser1OverallSummary.subjects.find { it.subjectId == "subj2" }.points == 10
        afterDeletionUser1OverallSummary.subjects.find { it.subjectId == "subj2" }.levelTotalPoints == 15
        afterDeletionUser1OverallSummary.subjects.find { it.subjectId == "subj2" }.todaysPoints == 10

        afterDeletionUser1Subj1Summary.skillsLevel == 5
        afterDeletionUser1Subj1Summary.points == 95
        afterDeletionUser1Subj1Summary.levelTotalPoints == -1
        afterDeletionUser1Subj1Summary.todaysPoints == 95
        afterDeletionUser1Subj1Summary.skills.collect { it.skillId }.sort() == ["s130", "s15", "s160"]

        afterDeletionUser1Subj2Summary.skillsLevel == 1
        afterDeletionUser1Subj2Summary.points == 10
        afterDeletionUser1Subj2Summary.levelTotalPoints == 15
        afterDeletionUser1Subj2Summary.todaysPoints == 10
        afterDeletionUser1Subj2Summary.skills.collect { it.skillId }.sort() == ["s210", "s220", "s230", "s240"]

        afterDeletionUser1PerformedSkills.data.collect { it.skillId }.sort() == ["s130", "s15", "s160", "s210"]

        // ----- user 2 -----
        user2OverallSummary.skillsLevel == 1
        user2OverallSummary.points == 50
        user2OverallSummary.totalPoints == 205
        user2OverallSummary.levelTotalPoints == 31
        user2OverallSummary.todaysPoints == 50
        user2OverallSummary.subjects.size() == 2
        user2OverallSummary.subjects.find { it.subjectId == "subj1" }.skillsLevel == 2
        user2OverallSummary.subjects.find { it.subjectId == "subj1" }.points == 40
        user2OverallSummary.subjects.find { it.subjectId == "subj1" }.levelTotalPoints == 21
        user2OverallSummary.subjects.find { it.subjectId == "subj1" }.levelPoints == 14
        user2OverallSummary.subjects.find { it.subjectId == "subj1" }.todaysPoints == 40
        user2OverallSummary.subjects.find { it.subjectId == "subj2" }.skillsLevel == 1
        user2OverallSummary.subjects.find { it.subjectId == "subj2" }.points == 10
        user2OverallSummary.subjects.find { it.subjectId == "subj2" }.levelTotalPoints == 15
        user2OverallSummary.subjects.find { it.subjectId == "subj2" }.levelPoints == 0
        user2OverallSummary.subjects.find { it.subjectId == "subj2" }.todaysPoints == 10


        user2Subj1Summary.skillsLevel == 2
        user2Subj1Summary.points == 40
        user2Subj1Summary.levelTotalPoints == 21
        user2Subj1Summary.levelPoints == 14
        user2Subj1Summary.todaysPoints == 40
        user2Subj1Summary.skills.collect { it.skillId }.sort() == ["s110", "s130", "s15", "s160"]

        user2Subj2Summary.skillsLevel == 1
        user2Subj2Summary.points == 10
        user2Subj2Summary.levelTotalPoints == 15
        user2Subj2Summary.levelPoints == 0
        user2Subj2Summary.todaysPoints == 10
        user2Subj2Summary.skills.collect { it.skillId }.sort() == ["s210", "s220", "s230", "s240"]

        user2PerformedSkills.data.collect { it.skillId }.sort() == ["s110", "s130", "s210"]

        afterDeletionUser2OverallSummary.skillsLevel == 1
        afterDeletionUser2OverallSummary.points == 40
        afterDeletionUser2OverallSummary.levelTotalPoints == 29 // should count up to the 4th level since 3rd level was already achieved
        afterDeletionUser2OverallSummary.levelPoints == 21
        afterDeletionUser2OverallSummary.todaysPoints == 40
        afterDeletionUser2OverallSummary.subjects.size() == 2
        afterDeletionUser2OverallSummary.subjects.find { it.subjectId == "subj1" }.skillsLevel == 2
        afterDeletionUser2OverallSummary.subjects.find { it.subjectId == "subj1" }.points == 30
        afterDeletionUser2OverallSummary.subjects.find { it.subjectId == "subj1" }.levelTotalPoints == 19
        afterDeletionUser2OverallSummary.subjects.find { it.subjectId == "subj1" }.levelPoints == 7
        afterDeletionUser2OverallSummary.subjects.find { it.subjectId == "subj1" }.todaysPoints == 30
        afterDeletionUser2OverallSummary.subjects.find { it.subjectId == "subj2" }.skillsLevel == 1
        afterDeletionUser2OverallSummary.subjects.find { it.subjectId == "subj2" }.points == 10
        afterDeletionUser2OverallSummary.subjects.find { it.subjectId == "subj2" }.levelTotalPoints == 15
        afterDeletionUser2OverallSummary.subjects.find { it.subjectId == "subj2" }.todaysPoints == 10

        afterDeletionUser2Subj1Summary.skillsLevel == 2
        afterDeletionUser2Subj1Summary.points == 30
        afterDeletionUser2Subj1Summary.levelTotalPoints == 19
        afterDeletionUser2Subj1Summary.levelPoints == 7
        afterDeletionUser2Subj1Summary.todaysPoints == 30
        afterDeletionUser2Subj1Summary.skills.collect { it.skillId }.sort() == ["s130", "s15", "s160"]

        afterDeletionUser2Subj2Summary.skillsLevel == 1
        afterDeletionUser2Subj2Summary.points == 10
        afterDeletionUser2Subj2Summary.levelTotalPoints == 15
        afterDeletionUser2Subj2Summary.todaysPoints == 10
        afterDeletionUser2Subj2Summary.skills.collect { it.skillId }.sort() == ["s210", "s220", "s230", "s240"]

        afterDeletionUser2PerformedSkills.data.collect { it.skillId }.sort() == ["s130", "s210"]
    }

    def "when subject is removed properly adjust points for users that achieved points for that skill"() {
        List<Map> subj1 = [10,30,60].collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: it, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = [10,20,30,40].collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: it, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj1.first().subjectId, name: "Test Subject 1"])
        skillsService.createSubject([projectId: projId, subjectId: subj2.first().subjectId, name: "Test Subject 2"])
        subj1.each { Map params ->
            skillsService.createSkill(params)
        }
        subj2.each { Map params ->
            skillsService.createSkill(params)
        }
        skillsService.createSkill([projectId: projId, subjectId: "subj1", skillId: "s15".toString(), name: "subj1 5".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1])

        // SUBJECT 1
        // user 1 has skills in all 3
        skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], sampleUserIds.get(0), new Date())
        skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], sampleUserIds.get(0), new Date())
        skillsService.addSkill([projectId: projId, skillId: subj1.get(2).skillId], sampleUserIds.get(0), new Date())
        skillsService.addSkill([projectId: projId, skillId: "s15"], sampleUserIds.get(0), new Date())

        // user 2 has 1s and 2nd only
        skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], sampleUserIds.get(1), new Date())
        skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], sampleUserIds.get(1), new Date())

        // SUBJECT 2
        skillsService.addSkill([projectId: projId, skillId: subj2.get(0).skillId], sampleUserIds.get(0), new Date())
        skillsService.addSkill([projectId: projId, skillId: subj2.get(0).skillId], sampleUserIds.get(1), new Date())

        String user1Id = sampleUserIds.get(0)

        def user1OverallSummary = skillsService.getSkillSummary(user1Id, projId)
        def user1Subj1Summary = skillsService.getSkillSummary(user1Id, projId, subj1.get(0).subjectId)
        def user1Subj2Summary = skillsService.getSkillSummary(user1Id, projId, subj2.get(0).subjectId)
        def user1PerformedSkills = skillsService.getPerformedSkills(user1Id, projId)

        String user2Id = sampleUserIds.get(1)

        def user2OverallSummary = skillsService.getSkillSummary(user2Id, projId)
        def user2Subj1Summary = skillsService.getSkillSummary(user2Id, projId, subj1.get(0).subjectId)
        def user2Subj2Summary = skillsService.getSkillSummary(user2Id, projId, subj2.get(0).subjectId)
        def user2PerformedSkills = skillsService.getPerformedSkills(user2Id, projId)

        when:

        skillsService.deleteSubject([projectId: projId, subjectId: subj1.get(0).subjectId])

        def afterDeletionUser1OverallSummary = skillsService.getSkillSummary(user1Id, projId)
        def afterDeletionUser1Subj2Summary = skillsService.getSkillSummary(user1Id, projId, subj2.get(0).subjectId)
        def afterDeletionUser1PerformedSkills = skillsService.getPerformedSkills(user1Id, projId)

        def afterDeletionUser2OverallSummary = skillsService.getSkillSummary(user2Id, projId)
        def afterDeletionUser2Subj2Summary = skillsService.getSkillSummary(user2Id, projId, subj2.get(0).subjectId)
        def afterDeletionUser2PerformedSkills = skillsService.getPerformedSkills(user2Id, projId)

        then:

        // ----- user 1 -----
        user1OverallSummary.skillsLevel == 3
        user1OverallSummary.points == 115
        user1OverallSummary.totalPoints == 205
        user1OverallSummary.levelTotalPoints == 45
        user1OverallSummary.todaysPoints == 115
        user1OverallSummary.subjects.size() == 2
        user1OverallSummary.subjects.find { it.subjectId == "subj1" }.skillsLevel == 5
        user1OverallSummary.subjects.find { it.subjectId == "subj1" }.points == 105
        user1OverallSummary.subjects.find { it.subjectId == "subj1" }.levelTotalPoints == -1
        user1OverallSummary.subjects.find { it.subjectId == "subj1" }.todaysPoints == 105
        user1OverallSummary.subjects.find { it.subjectId == "subj2" }.skillsLevel == 1
        user1OverallSummary.subjects.find { it.subjectId == "subj2" }.points == 10
        user1OverallSummary.subjects.find { it.subjectId == "subj2" }.levelTotalPoints == 15
        user1OverallSummary.subjects.find { it.subjectId == "subj2" }.todaysPoints == 10

        user1Subj1Summary.skillsLevel == 5
        user1Subj1Summary.points == 105
        user1Subj1Summary.levelTotalPoints == -1
        user1Subj1Summary.todaysPoints == 105
        user1Subj1Summary.skills.collect { it.skillId }.sort() == ["s110", "s130", "s15", "s160"]

        user1Subj2Summary.skillsLevel == 1
        user1Subj2Summary.points == 10
        user1Subj2Summary.levelTotalPoints == 15
        user1Subj2Summary.todaysPoints == 10
        user1Subj2Summary.skills.collect { it.skillId }.sort() == ["s210", "s220", "s230", "s240"]

        user1PerformedSkills.data.collect { it.skillId }.sort() == ["s110", "s130", "s15", "s160", "s210"]

        afterDeletionUser1OverallSummary.skillsLevel == 3
        afterDeletionUser1OverallSummary.points == 10
        afterDeletionUser1OverallSummary.levelTotalPoints == 57 // should count up to the 4th level since 3rd level was already achieved
        afterDeletionUser1OverallSummary.levelPoints == 0
        afterDeletionUser1OverallSummary.todaysPoints == 10
        afterDeletionUser1OverallSummary.subjects.size() == 1
        afterDeletionUser1OverallSummary.subjects.find { it.subjectId == "subj2" }.skillsLevel == 1
        afterDeletionUser1OverallSummary.subjects.find { it.subjectId == "subj2" }.points == 10
        afterDeletionUser1OverallSummary.subjects.find { it.subjectId == "subj2" }.levelTotalPoints == 15
        afterDeletionUser1OverallSummary.subjects.find { it.subjectId == "subj2" }.todaysPoints == 10

        afterDeletionUser1Subj2Summary.skillsLevel == 1
        afterDeletionUser1Subj2Summary.points == 10
        afterDeletionUser1Subj2Summary.levelTotalPoints == 15
        afterDeletionUser1Subj2Summary.todaysPoints == 10
        afterDeletionUser1Subj2Summary.skills.collect { it.skillId }.sort() == ["s210", "s220", "s230", "s240"]

        afterDeletionUser1PerformedSkills.data.collect { it.skillId }.sort() == ["s210"]

        // ----- user 2 -----
        user2OverallSummary.skillsLevel == 1
        user2OverallSummary.points == 50
        user2OverallSummary.totalPoints == 205
        user2OverallSummary.levelTotalPoints == 31
        user2OverallSummary.todaysPoints == 50
        user2OverallSummary.subjects.size() == 2
        user2OverallSummary.subjects.find { it.subjectId == "subj1" }.skillsLevel == 2
        user2OverallSummary.subjects.find { it.subjectId == "subj1" }.points == 40
        user2OverallSummary.subjects.find { it.subjectId == "subj1" }.levelTotalPoints == 21
        user2OverallSummary.subjects.find { it.subjectId == "subj1" }.levelPoints == 14
        user2OverallSummary.subjects.find { it.subjectId == "subj1" }.todaysPoints == 40
        user2OverallSummary.subjects.find { it.subjectId == "subj2" }.skillsLevel == 1
        user2OverallSummary.subjects.find { it.subjectId == "subj2" }.points == 10
        user2OverallSummary.subjects.find { it.subjectId == "subj2" }.levelTotalPoints == 15
        user2OverallSummary.subjects.find { it.subjectId == "subj2" }.levelPoints == 0
        user2OverallSummary.subjects.find { it.subjectId == "subj2" }.todaysPoints == 10


        user2Subj1Summary.skillsLevel == 2
        user2Subj1Summary.points == 40
        user2Subj1Summary.levelTotalPoints == 21
        user2Subj1Summary.levelPoints == 14
        user2Subj1Summary.todaysPoints == 40
        user2Subj1Summary.skills.collect { it.skillId }.sort() == ["s110", "s130", "s15", "s160"]

        user2Subj2Summary.skillsLevel == 1
        user2Subj2Summary.points == 10
        user2Subj2Summary.levelTotalPoints == 15
        user2Subj2Summary.levelPoints == 0
        user2Subj2Summary.todaysPoints == 10
        user2Subj2Summary.skills.collect { it.skillId }.sort() == ["s210", "s220", "s230", "s240"]

        user2PerformedSkills.data.collect { it.skillId }.sort() == ["s110", "s130", "s210"]

        afterDeletionUser2OverallSummary.skillsLevel == 1
        afterDeletionUser2OverallSummary.points == 10
        afterDeletionUser2OverallSummary.levelTotalPoints == 15 // should count up to the 4th level since 3rd level was already achieved
        afterDeletionUser2OverallSummary.levelPoints == 0
        afterDeletionUser2OverallSummary.todaysPoints == 10
        afterDeletionUser2OverallSummary.subjects.size() == 1
        afterDeletionUser2OverallSummary.subjects.find { it.subjectId == "subj2" }.skillsLevel == 1
        afterDeletionUser2OverallSummary.subjects.find { it.subjectId == "subj2" }.points == 10
        afterDeletionUser2OverallSummary.subjects.find { it.subjectId == "subj2" }.levelTotalPoints == 15
        afterDeletionUser2OverallSummary.subjects.find { it.subjectId == "subj2" }.todaysPoints == 10

        afterDeletionUser2Subj2Summary.skillsLevel == 1
        afterDeletionUser2Subj2Summary.points == 10
        afterDeletionUser2Subj2Summary.levelTotalPoints == 15
        afterDeletionUser2Subj2Summary.todaysPoints == 10
        afterDeletionUser2Subj2Summary.skills.collect { it.skillId }.sort() == ["s210", "s220", "s230", "s240"]

        afterDeletionUser2PerformedSkills.data.collect { it.skillId }.sort() == ["s210"]
    }


    def "validate that user point history was properly adjusted when a subject is removed"(){
        List<Map> subj1 = (1..3).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..3).collect { [projectId: projId, subjectId: "subj3", skillId: "s3${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj1.first().subjectId, name: "Test Subject 1"])
        skillsService.createSubject([projectId: projId, subjectId: subj2.first().subjectId, name: "Test Subject 2"])
        skillsService.createSubject([projectId: projId, subjectId: subj3.first().subjectId, name: "Test Subject 3"])
        subj1.each { Map params ->
            skillsService.createSkill(params)
        }
        subj2.each { Map params ->
            skillsService.createSkill(params)
        }
        subj3.each { Map params ->
            skillsService.createSkill(params)
        }

        List<Date> dates = []
        use (TimeCategory) {
            (0..7).each {
                Date theDate = it.days.ago
                dates.add(theDate)
            }
        }
        dates.sort()

        dates.eachWithIndex { Date theDate, int index ->
            skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], sampleUserIds.get(0), theDate)
            skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], sampleUserIds.get(1), theDate)
            skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], sampleUserIds.get(2), theDate)

            skillsService.addSkill([projectId: projId, skillId: subj3.get(1).skillId], sampleUserIds.get(0), theDate)
            skillsService.addSkill([projectId: projId, skillId: subj3.get(1).skillId], sampleUserIds.get(1), theDate)
            skillsService.addSkill([projectId: projId, skillId: subj3.get(1).skillId], sampleUserIds.get(2), theDate)

            if ((index % 2) == 0) {
                skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], sampleUserIds.get(0), theDate)
                skillsService.addSkill([projectId: projId, skillId: subj2.get(2).skillId], sampleUserIds.get(0), theDate)
            }
            if ((index % 3) == 0) {
                skillsService.addSkill([projectId: projId, skillId: subj2.get(1).skillId], sampleUserIds.get(0), theDate)
                skillsService.addSkill([projectId: projId, skillId: subj2.get(3).skillId], sampleUserIds.get(0), theDate)
            }
        }

        String user1Id = sampleUserIds.get(0)
        String user2Id = sampleUserIds.get(1)
        String user3Id = sampleUserIds.get(2)
        def user1PointHistory = skillsService.getPointHistory(user1Id, projId)
        def user1PointHistorySubj1 = skillsService.getPointHistory(user1Id, projId, subj1.first().subjectId)
        def user1PointHistorySubj2 = skillsService.getPointHistory(user1Id, projId, subj2.first().subjectId)
        def user1PointHistorySubj3 = skillsService.getPointHistory(user1Id, projId, subj3.first().subjectId)

        def user2PointHistory = skillsService.getPointHistory(user2Id, projId)
        def user2PointHistorySubj1 = skillsService.getPointHistory(user2Id, projId, subj1.first().subjectId)
        def user2PointHistorySubj2 = skillsService.getPointHistory(user2Id, projId, subj2.first().subjectId)
        def user2PointHistorySubj3 = skillsService.getPointHistory(user2Id, projId, subj3.first().subjectId)

        def user3PointHistory = skillsService.getPointHistory(user3Id, projId)
        def user3PointHistorySubj1 = skillsService.getPointHistory(user3Id, projId, subj1.first().subjectId)
        def user3PointHistorySubj2 = skillsService.getPointHistory(user3Id, projId, subj2.first().subjectId)
        def user3PointHistorySubj3 = skillsService.getPointHistory(user3Id, projId, subj3.first().subjectId)

        Closure getPoints = { def ptsHistory, int index -> ptsHistory.pointsHistory.find { df.parse(it.dayPerformed) == dates.get(index) }.points }

        when:
        skillsService.deleteSubject([projectId: projId, subjectId: subj1.get(0).subjectId])
        def afterRemovalUser1PointHistory = skillsService.getPointHistory(user1Id, projId)
        def afterRemovalUser1PointHistorySubj1 = skillsService.getPointHistory(user1Id, projId, subj1.first().subjectId)
        def afterRemovalUser1PointHistorySubj2 = skillsService.getPointHistory(user1Id, projId, subj2.first().subjectId)
        def afterRemovalUser1PointHistorySubj3 = skillsService.getPointHistory(user1Id, projId, subj3.first().subjectId)

        def afterRemovalUser2PointHistory = skillsService.getPointHistory(user2Id, projId)
        def afterRemovalUser2PointHistorySubj1 = skillsService.getPointHistory(user2Id, projId, subj1.first().subjectId)
        def afterRemovalUser2PointHistorySubj2 = skillsService.getPointHistory(user2Id, projId, subj2.first().subjectId)
        def afterRemovalUser2PointHistorySubj3 = skillsService.getPointHistory(user2Id, projId, subj3.first().subjectId)

        def afterRemovalUser3PointHistory = skillsService.getPointHistory(user3Id, projId)
        def afterRemovalUser3PointHistorySubj1 = skillsService.getPointHistory(user3Id, projId, subj1.first().subjectId)
        def afterRemovalUser3PointHistorySubj2 = skillsService.getPointHistory(user3Id, projId, subj2.first().subjectId)
        def afterRemovalUser3PointHistorySubj3 = skillsService.getPointHistory(user3Id, projId, subj3.first().subjectId)

        then:

        // ---- user 1 -----
        getPoints.call(user1PointHistory, 0) == 90
        getPoints.call(user1PointHistory, 1) == 110
        getPoints.call(user1PointHistory, 2) == 160
        getPoints.call(user1PointHistory, 3) == 220
        getPoints.call(user1PointHistory, 4) == 270
        getPoints.call(user1PointHistory, 5) == 290
        getPoints.call(user1PointHistory, 6) == 380
        getPoints.call(user1PointHistory, 7) == 400

        getPoints.call(user1PointHistorySubj1, 0) == 20
        getPoints.call(user1PointHistorySubj1, 1) == 30
        getPoints.call(user1PointHistorySubj1, 2) == 50
        getPoints.call(user1PointHistorySubj1, 3) == 60
        getPoints.call(user1PointHistorySubj1, 4) == 80
        getPoints.call(user1PointHistorySubj1, 5) == 90
        getPoints.call(user1PointHistorySubj1, 6) == 110
        getPoints.call(user1PointHistorySubj1, 7) == 120

        getPoints.call(user1PointHistorySubj2, 0) == 60
        getPoints.call(user1PointHistorySubj2, 1) == 60
        getPoints.call(user1PointHistorySubj2, 2) == 80
        getPoints.call(user1PointHistorySubj2, 3) == 120
        getPoints.call(user1PointHistorySubj2, 4) == 140
        getPoints.call(user1PointHistorySubj2, 5) == 140
        getPoints.call(user1PointHistorySubj2, 6) == 200
        getPoints.call(user1PointHistorySubj2, 7) == 200

        getPoints.call(user1PointHistorySubj3, 0) == 10
        getPoints.call(user1PointHistorySubj3, 1) == 20
        getPoints.call(user1PointHistorySubj3, 2) == 30
        getPoints.call(user1PointHistorySubj3, 3) == 40
        getPoints.call(user1PointHistorySubj3, 4) == 50
        getPoints.call(user1PointHistorySubj3, 5) == 60
        getPoints.call(user1PointHistorySubj3, 6) == 70
        getPoints.call(user1PointHistorySubj3, 7) == 80

        getPoints.call(afterRemovalUser1PointHistory, 0) == 70
        getPoints.call(afterRemovalUser1PointHistory, 1) == 80
        getPoints.call(afterRemovalUser1PointHistory, 2) == 110
        getPoints.call(afterRemovalUser1PointHistory, 3) == 160
        getPoints.call(afterRemovalUser1PointHistory, 4) == 190
        getPoints.call(afterRemovalUser1PointHistory, 5) == 200
        getPoints.call(afterRemovalUser1PointHistory, 6) == 270
        getPoints.call(afterRemovalUser1PointHistory, 7) == 280

        !afterRemovalUser1PointHistorySubj1.pointsHistory

        getPoints.call(afterRemovalUser1PointHistorySubj2, 0) == 60
        getPoints.call(afterRemovalUser1PointHistorySubj2, 1) == 60
        getPoints.call(afterRemovalUser1PointHistorySubj2, 2) == 80
        getPoints.call(afterRemovalUser1PointHistorySubj2, 3) == 120
        getPoints.call(afterRemovalUser1PointHistorySubj2, 4) == 140
        getPoints.call(afterRemovalUser1PointHistorySubj2, 5) == 140
        getPoints.call(afterRemovalUser1PointHistorySubj2, 6) == 200
        getPoints.call(afterRemovalUser1PointHistorySubj2, 7) == 200

        getPoints.call(afterRemovalUser1PointHistorySubj3, 0) == 10
        getPoints.call(afterRemovalUser1PointHistorySubj3, 1) == 20
        getPoints.call(afterRemovalUser1PointHistorySubj3, 2) == 30
        getPoints.call(afterRemovalUser1PointHistorySubj3, 3) == 40
        getPoints.call(afterRemovalUser1PointHistorySubj3, 4) == 50
        getPoints.call(afterRemovalUser1PointHistorySubj3, 5) == 60
        getPoints.call(afterRemovalUser1PointHistorySubj3, 6) == 70
        getPoints.call(afterRemovalUser1PointHistorySubj3, 7) == 80

        // ---- user 2 -----

        getPoints.call(user2PointHistory, 0) == 20
        getPoints.call(user2PointHistory, 1) == 40
        getPoints.call(user2PointHistory, 2) == 60
        getPoints.call(user2PointHistory, 3) == 80
        getPoints.call(user2PointHistory, 4) == 100
        getPoints.call(user2PointHistory, 5) == 120
        getPoints.call(user2PointHistory, 6) == 140
        getPoints.call(user2PointHistory, 7) == 160

        getPoints.call(user2PointHistorySubj1, 0) == 10
        getPoints.call(user2PointHistorySubj1, 1) == 20
        getPoints.call(user2PointHistorySubj1, 2) == 30
        getPoints.call(user2PointHistorySubj1, 3) == 40
        getPoints.call(user2PointHistorySubj1, 4) == 50
        getPoints.call(user2PointHistorySubj1, 5) == 60
        getPoints.call(user2PointHistorySubj1, 6) == 70
        getPoints.call(user2PointHistorySubj1, 7) == 80

        !user2PointHistorySubj2.pointsHistory

        getPoints.call(user2PointHistorySubj3, 0) == 10
        getPoints.call(user2PointHistorySubj3, 1) == 20
        getPoints.call(user2PointHistorySubj3, 2) == 30
        getPoints.call(user2PointHistorySubj3, 3) == 40
        getPoints.call(user2PointHistorySubj3, 4) == 50
        getPoints.call(user2PointHistorySubj3, 5) == 60
        getPoints.call(user2PointHistorySubj3, 6) == 70
        getPoints.call(user2PointHistorySubj3, 7) == 80

        getPoints.call(afterRemovalUser2PointHistory, 0) == 10
        getPoints.call(afterRemovalUser2PointHistory, 1) == 20
        getPoints.call(afterRemovalUser2PointHistory, 2) == 30
        getPoints.call(afterRemovalUser2PointHistory, 3) == 40
        getPoints.call(afterRemovalUser2PointHistory, 4) == 50
        getPoints.call(afterRemovalUser2PointHistory, 5) == 60
        getPoints.call(afterRemovalUser2PointHistory, 6) == 70
        getPoints.call(afterRemovalUser2PointHistory, 7) == 80

        !afterRemovalUser2PointHistorySubj1.pointsHistory
        !afterRemovalUser2PointHistorySubj2.pointsHistory

        getPoints.call(afterRemovalUser2PointHistorySubj3, 0) == 10
        getPoints.call(afterRemovalUser2PointHistorySubj3, 1) == 20
        getPoints.call(afterRemovalUser2PointHistorySubj3, 2) == 30
        getPoints.call(afterRemovalUser2PointHistorySubj3, 3) == 40
        getPoints.call(afterRemovalUser2PointHistorySubj3, 4) == 50
        getPoints.call(afterRemovalUser2PointHistorySubj3, 5) == 60
        getPoints.call(afterRemovalUser2PointHistorySubj3, 6) == 70
        getPoints.call(afterRemovalUser2PointHistorySubj3, 7) == 80

        // ---- user 3 -----
        getPoints.call(user3PointHistory, 0) == 20
        getPoints.call(user3PointHistory, 1) == 40
        getPoints.call(user3PointHistory, 2) == 60
        getPoints.call(user3PointHistory, 3) == 80
        getPoints.call(user3PointHistory, 4) == 100
        getPoints.call(user3PointHistory, 5) == 120
        getPoints.call(user3PointHistory, 6) == 140
        getPoints.call(user3PointHistory, 7) == 160

        getPoints.call(user3PointHistorySubj1, 0) == 10
        getPoints.call(user3PointHistorySubj1, 1) == 20
        getPoints.call(user3PointHistorySubj1, 2) == 30
        getPoints.call(user3PointHistorySubj1, 3) == 40
        getPoints.call(user3PointHistorySubj1, 4) == 50
        getPoints.call(user3PointHistorySubj1, 5) == 60
        getPoints.call(user3PointHistorySubj1, 6) == 70
        getPoints.call(user3PointHistorySubj1, 7) == 80

        !user3PointHistorySubj2.pointsHistory

        getPoints.call(user3PointHistorySubj3, 0) == 10
        getPoints.call(user3PointHistorySubj3, 1) == 20
        getPoints.call(user3PointHistorySubj3, 2) == 30
        getPoints.call(user3PointHistorySubj3, 3) == 40
        getPoints.call(user3PointHistorySubj3, 4) == 50
        getPoints.call(user3PointHistorySubj3, 5) == 60
        getPoints.call(user3PointHistorySubj3, 6) == 70
        getPoints.call(user3PointHistorySubj3, 7) == 80

        getPoints.call(afterRemovalUser3PointHistory, 0) == 10
        getPoints.call(afterRemovalUser3PointHistory, 1) == 20
        getPoints.call(afterRemovalUser3PointHistory, 2) == 30
        getPoints.call(afterRemovalUser3PointHistory, 3) == 40
        getPoints.call(afterRemovalUser3PointHistory, 4) == 50
        getPoints.call(afterRemovalUser3PointHistory, 5) == 60
        getPoints.call(afterRemovalUser3PointHistory, 6) == 70
        getPoints.call(afterRemovalUser3PointHistory, 7) == 80

        !afterRemovalUser3PointHistorySubj1.pointsHistory
        !afterRemovalUser3PointHistorySubj2.pointsHistory

        getPoints.call(afterRemovalUser3PointHistorySubj3, 0) == 10
        getPoints.call(afterRemovalUser3PointHistorySubj3, 1) == 20
        getPoints.call(afterRemovalUser3PointHistorySubj3, 2) == 30
        getPoints.call(afterRemovalUser3PointHistorySubj3, 3) == 40
        getPoints.call(afterRemovalUser3PointHistorySubj3, 4) == 50
        getPoints.call(afterRemovalUser3PointHistorySubj3, 5) == 60
        getPoints.call(afterRemovalUser3PointHistorySubj3, 6) == 70
        getPoints.call(afterRemovalUser3PointHistorySubj3, 7) == 80
    }

    def "if subject is deleted it shouldn't affect point history of other projects"() {
        String proj2 = "proj2"
        skillsService.deleteProjectIfExist(proj2)

        List<Map> subj1 = (1..2).collect {
            [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        }
        List<Map> subj2 = (1..2).collect {
            [projectId: proj2, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        }

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj1.first().subjectId, name: "Test Subject 1"])

        skillsService.createProject([projectId: proj2, name: "Test Project2"])
        skillsService.createSubject([projectId: proj2, subjectId: subj2.first().subjectId, name: "Test Subject 2"])

        subj1.each { Map params ->
            skillsService.createSkill(params)
        }
        subj2.each { Map params ->
            skillsService.createSkill(params)
        }

        List<Date> dates
        use(TimeCategory) {
            dates = [1.day.ago, 2.days.ago].sort()
        }
        use(TimeCategory) {
            skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], sampleUserIds.get(0), dates.get(0))
            skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], sampleUserIds.get(0), dates.get(1))

            skillsService.addSkill([projectId: proj2, skillId: subj2.get(0).skillId], sampleUserIds.get(0), dates.get(0))
            skillsService.addSkill([projectId: proj2, skillId: subj2.get(0).skillId], sampleUserIds.get(0), dates.get(1))
        }

        String user1Id = sampleUserIds.get(0)

        def user1PointHistory = skillsService.getPointHistory(user1Id, projId)
        def user1PointHistorySubj1 = skillsService.getPointHistory(user1Id, projId, subj1.first().subjectId)

        def user1PointHistoryProj1 = skillsService.getPointHistory(user1Id, proj2)
        def user1PointHistorySubj1PRoj1 = skillsService.getPointHistory(user1Id, proj2, subj2.first().subjectId)

        Closure getPoints = { def ptsHistory, int index -> ptsHistory.pointsHistory.find { df.parse(it.dayPerformed) == dates.get(index) }.points }

        when:
        skillsService.deleteSubject([projectId: projId, subjectId: subj1.get(0).subjectId])

        def afterRemovalUser1PointHistory = skillsService.getPointHistory(user1Id, projId)
        def afterRemovalUser1PointHistorySubj1 = skillsService.getPointHistory(user1Id, projId, subj1.first().subjectId)

        def afterRemovalUser1PointHistoryProj1 = skillsService.getPointHistory(user1Id, proj2)
        def afterRemovalUser1PointHistorySubj1PRoj1 = skillsService.getPointHistory(user1Id, proj2, subj2.first().subjectId)

        then:
        getPoints.call(user1PointHistory, 0) == 10
        getPoints.call(user1PointHistory, 1) == 20

        getPoints.call(user1PointHistorySubj1, 0) == 10
        getPoints.call(user1PointHistorySubj1, 1) == 20

        getPoints.call(user1PointHistoryProj1, 0) == 20
        getPoints.call(user1PointHistoryProj1, 1) == 40

        getPoints.call(user1PointHistorySubj1PRoj1, 0) == 20
        getPoints.call(user1PointHistorySubj1PRoj1, 1) == 40

        !afterRemovalUser1PointHistory.pointHistory
        !afterRemovalUser1PointHistorySubj1.pointHistory

        getPoints.call(afterRemovalUser1PointHistoryProj1, 0) == 20
        getPoints.call(afterRemovalUser1PointHistoryProj1, 1) == 40

        getPoints.call(afterRemovalUser1PointHistorySubj1PRoj1, 0) == 20
        getPoints.call(afterRemovalUser1PointHistorySubj1PRoj1, 1) == 40
    }


    def "if skill is deleted it shouldn't affect point history of other projects"() {
        String proj2 = "proj2"
        skillsService.deleteProjectIfExist(proj2)

        List<Map> subj1 = (1..2).collect {
            [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        }
        List<Map> subj2 = (1..2).collect {
            [projectId: proj2, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        }

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj1.first().subjectId, name: "Test Subject 1"])

        skillsService.createProject([projectId: proj2, name: "Test Project2"])
        skillsService.createSubject([projectId: proj2, subjectId: subj2.first().subjectId, name: "Test Subject 2"])

        subj1.each { Map params ->
            skillsService.createSkill(params)
        }
        subj2.each { Map params ->
            skillsService.createSkill(params)
        }

        List<Date> dates
        use(TimeCategory) {
            dates = [1.day.ago, 2.days.ago].sort()
        }
        use(TimeCategory) {
            skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], sampleUserIds.get(0), dates.get(0))
            skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], sampleUserIds.get(0), dates.get(1))

            skillsService.addSkill([projectId: proj2, skillId: subj2.get(0).skillId], sampleUserIds.get(0), dates.get(0))
            skillsService.addSkill([projectId: proj2, skillId: subj2.get(0).skillId], sampleUserIds.get(0), dates.get(1))
        }

        String user1Id = sampleUserIds.get(0)

        def user1PointHistory = skillsService.getPointHistory(user1Id, projId)
        def user1PointHistorySubj1 = skillsService.getPointHistory(user1Id, projId, subj1.first().subjectId)

        def user1PointHistoryProj1 = skillsService.getPointHistory(user1Id, proj2)
        def user1PointHistorySubj1PRoj1 = skillsService.getPointHistory(user1Id, proj2, subj2.first().subjectId)

        Closure getPoints = { def ptsHistory, int index -> ptsHistory.pointsHistory.find { df.parse(it.dayPerformed) == dates.get(index) }.points }

        when:
        skillsService.deleteSkill([projectId: projId, subjectId: subj1.get(0).subjectId, skillId: subj1.get(0).skillId])

        def afterRemovalUser1PointHistory = skillsService.getPointHistory(user1Id, projId)
        def afterRemovalUser1PointHistorySubj1 = skillsService.getPointHistory(user1Id, projId, subj1.first().subjectId)

        def afterRemovalUser1PointHistoryProj1 = skillsService.getPointHistory(user1Id, proj2)
        def afterRemovalUser1PointHistorySubj1PRoj1 = skillsService.getPointHistory(user1Id, proj2, subj2.first().subjectId)

        then:
        getPoints.call(user1PointHistory, 0) == 10
        getPoints.call(user1PointHistory, 1) == 20

        getPoints.call(user1PointHistorySubj1, 0) == 10
        getPoints.call(user1PointHistorySubj1, 1) == 20

        getPoints.call(user1PointHistoryProj1, 0) == 20
        getPoints.call(user1PointHistoryProj1, 1) == 40

        getPoints.call(user1PointHistorySubj1PRoj1, 0) == 20
        getPoints.call(user1PointHistorySubj1PRoj1, 1) == 40

        !afterRemovalUser1PointHistory.pointHistory
        !afterRemovalUser1PointHistorySubj1.pointHistory

        getPoints.call(afterRemovalUser1PointHistoryProj1, 0) == 20
        getPoints.call(afterRemovalUser1PointHistoryProj1, 1) == 40

        getPoints.call(afterRemovalUser1PointHistorySubj1PRoj1, 0) == 20
        getPoints.call(afterRemovalUser1PointHistorySubj1PRoj1, 1) == 40
    }


    def "if user belongs to 2 projects and 1 is removed user should still be part of the other project"(){
        String proj2 = "proj2"
        skillsService.deleteProjectIfExist(proj2)

        List<Map> subj1 = (1..2).collect {
            [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 10, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        }
        List<Map> subj2 = (1..2).collect {
            [projectId: proj2, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        }

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj1.first().subjectId, name: "Test Subject 1"])

        skillsService.createProject([projectId: proj2, name: "Test Project2"])
        skillsService.createSubject([projectId: proj2, subjectId: subj2.first().subjectId, name: "Test Subject 2"])

        subj1.each { Map params ->
            skillsService.createSkill(params)
        }
        subj2.each { Map params ->
            skillsService.createSkill(params)
        }

        List<Date> dates
        use(TimeCategory) {
            dates = [1.day.ago, 2.days.ago].sort()
        }
        use(TimeCategory) {
            skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], sampleUserIds.get(0), dates.get(0))
            skillsService.addSkill([projectId: projId, skillId: subj1.get(0).skillId], sampleUserIds.get(0), dates.get(1))

            skillsService.addSkill([projectId: proj2, skillId: subj2.get(0).skillId], sampleUserIds.get(0), dates.get(0))
            skillsService.addSkill([projectId: proj2, skillId: subj2.get(0).skillId], sampleUserIds.get(0), dates.get(1))
        }

        String user1Id = sampleUserIds.get(0)

        def user1PointHistory = skillsService.getPointHistory(user1Id, projId)
        def user1PointHistorySubj1 = skillsService.getPointHistory(user1Id, projId, subj1.first().subjectId)

        def user1PointHistoryProj1 = skillsService.getPointHistory(user1Id, proj2)
        def user1PointHistorySubj1PRoj1 = skillsService.getPointHistory(user1Id, proj2, subj2.first().subjectId)

        Closure getPoints = { def ptsHistory, int index -> ptsHistory.pointsHistory.find { df.parse(it.dayPerformed) == dates.get(index) }.points }

        when:
        skillsService.deleteProjectIfExist(projId)

        def afterRemovalUser1PointHistory
        def afterRemovalUser1PointHistorySubj1

        try {
            afterRemovalUser1PointHistory = skillsService.getPointHistory(user1Id, projId)
            afterRemovalUser1PointHistorySubj1 = skillsService.getPointHistory(user1Id, projId, subj1.first().subjectId)
        } catch(Exception e) {
            // should fail w/ 403 since the projId no longer exists
        }

        def afterRemovalUser1PointHistoryProj1 = skillsService.getPointHistory(user1Id, proj2)
        def afterRemovalUser1PointHistorySubj1PRoj1 = skillsService.getPointHistory(user1Id, proj2, subj2.first().subjectId)

        then:
        getPoints.call(user1PointHistory, 0) == 10
        getPoints.call(user1PointHistory, 1) == 20

        getPoints.call(user1PointHistorySubj1, 0) == 10
        getPoints.call(user1PointHistorySubj1, 1) == 20

        getPoints.call(user1PointHistoryProj1, 0) == 20
        getPoints.call(user1PointHistoryProj1, 1) == 40

        getPoints.call(user1PointHistorySubj1PRoj1, 0) == 20
        getPoints.call(user1PointHistorySubj1PRoj1, 1) == 40

        !afterRemovalUser1PointHistory?.pointHistory
        !afterRemovalUser1PointHistorySubj1?.pointHistory

        getPoints.call(afterRemovalUser1PointHistoryProj1, 0) == 20
        getPoints.call(afterRemovalUser1PointHistoryProj1, 1) == 40

        getPoints.call(afterRemovalUser1PointHistorySubj1PRoj1, 0) == 20
        getPoints.call(afterRemovalUser1PointHistorySubj1PRoj1, 1) == 40
    }
}
