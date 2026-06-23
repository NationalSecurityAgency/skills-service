/**
 * Copyright 2026 SkillTree
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
package skills.intTests.batchUpdateSkills

import groovy.json.JsonOutput
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsService
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints

import static skills.intTests.utils.SkillsFactory.*

class BatchUpdateUserAchievements_SkillGroupsSpec extends DefaultIntSpec {

    List<SkillsService> users

    def setup() {
        users = getRandomUsers(5).collect { createService(it)}
        // project 2 shouldn't affect project 1 used in the actual tests
        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skills = createSkills(5, 2, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2skills)

        def p2subj2 = createSubject(2, 2)
        def p2skillsSubj2 = createSkills(5, 2, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(null, p2subj2, p2skillsSubj2)

        users[0].addSkill(p2skills[1])

        users[1].addSkill(p2skills[0])
        users[1].addSkill(p2skills[1])
        users[1].addSkill(p2skills[2])
        users[1].addSkill(p2skills[0])
        users[1].addSkill(p2skills[2])
        users[1].addSkill(p2skills[3])
        users[1].addSkill(p2skills[4])

        users[2].addSkill(p2skills[0])
        users[2].addSkill(p2skills[3])
        users[2].addSkill(p2skills[3])
        users[2].addSkill(p2skills[3])
    }

    def "achievements and points are updated - pointIncrement is increased and numPerformToCompletion is decreased"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(10, 1, 1, 100)

        p1Subj1Skills.eachWithIndex { it, index ->
            it.pointIncrement = 111 * (index + 1)
            it.numPerformToCompletion = 1 * (index + 1)
            it.pointIncrementInterval = 0
        }

        def underSubj1Skills = p1Subj1Skills[0..2]
        def underGroup1Skills = p1Subj1Skills[3..5]
        def underGroup2Skills = p1Subj1Skills[6..9]
        println underGroup2Skills.collect { "${it.pointIncrement}*${it.numPerformToCompletion}"}.join("+")
        def subj1Group1 = createSkillsGroup(1, 1, 11)
        def subj1Group2 = createSkillsGroup(1, 1, 12)

        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1,
                [underSubj1Skills[0], underSubj1Skills[1], subj1Group1, subj1Group2, underSubj1Skills[2]])
        underGroup1Skills.each { skillsService.assignSkillToSkillsGroup(subj1Group1.skillId, it)}
        underGroup2Skills.each { skillsService.assignSkillToSkillsGroup(subj1Group2.skillId, it)}

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(7, 1, 2, 222)
        p1skillsSubj2.eachWithIndex { it, index ->
            it.pointIncrementInterval = 0
        }
        def underSubj2Skills = p1skillsSubj2[0..2]
        def underGroup3Skills = p1skillsSubj2[3..6]
        def subj2Group3 = createSkillsGroup(1, 2, 23)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, [underSubj2Skills[0], underSubj2Skills[1], underSubj2Skills[2], subj2Group3])
        underGroup3Skills.each { skillsService.assignSkillToSkillsGroup(subj2Group3.skillId, it)}

        users[0].addSkill(p1Subj1Skills[1]) // pointIncrement = 222
        users[0].addSkill(underGroup1Skills[0]) // pointIncrement = 444
        users[0].addSkill(underGroup1Skills[0]) // pointIncrement = 444
        users[0].addSkill(underGroup1Skills[1]) // pointIncrement = 555
        users[0].addSkill(underGroup1Skills[1]) // pointIncrement = 555
        users[0].addSkill(underGroup1Skills[1]) // pointIncrement = 555
        users[0].addSkill(underGroup1Skills[1]) // pointIncrement = 555
        users[0].addSkill(underGroup1Skills[1]) // pointIncrement = 555
        users[0].addSkill(underGroup2Skills[0]) // pointIncrement = 777
        users[0].addSkill(underGroup2Skills[2]) // pointIncrement = 999
        users[0].addSkill(underGroup2Skills[2]) // pointIncrement = 999
        users[0].addSkill(underGroup2Skills[2]) // pointIncrement = 999
        users[0].addSkill(underGroup3Skills[1]) // pointIncrement = 222
        users[0].addSkill(underSubj2Skills[1]) // pointIncrement = 222

        users[1].addSkill(p1Subj1Skills[1])
        users[1].addSkill(p1Subj1Skills[1])
        users[1].addSkill(p1Subj1Skills[2])
        users[1].addSkill(p1Subj1Skills[2])
        users[1].addSkill(p1Subj1Skills[2])
        users[1].addSkill(p1Subj1Skills[3])
        users[1].addSkill(p1Subj1Skills[4])

        users[2].addSkill(p1Subj1Skills[0])
        users[2].addSkill(p1Subj1Skills[4])
        users[2].addSkill(p1Subj1Skills[4])
        users[2].addSkill(p1Subj1Skills[4])

        when:
        def user1Subj1Before = users[0].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def user1Subj2Before = users[0].getSubjectSummaryForCurrentUser(p1.projectId, p1subj2.subjectId)
        def user1Group1Before = users[0].getSkillsGroupSummary(p1.projectId, subj1Group1.skillId)
        def user1Group2Before = users[0].getSkillsGroupSummary(p1.projectId, subj1Group2.skillId)
        List<UserPoints> points_before = userPointsRepo.findAll()
        List<UserAchievement> achievements_before = userAchievedRepo.findAll()
        int newPntIncrement = 651
        int newNumPerformToCompletion = 2
        skillsService.batchUpdateSkills(p1.projectId, [
                pointIncrement: newPntIncrement,
                numPerformToCompletion: newNumPerformToCompletion,
                skills: underGroup1Skills.skillId
        ])
        def user1Subj1After = users[0].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def user1Subj2After = users[0].getSubjectSummaryForCurrentUser(p1.projectId, p1subj2.subjectId)
        def user1Group1After = users[0].getSkillsGroupSummary(p1.projectId, subj1Group1.skillId)
        def user1Group2After = users[0].getSkillsGroupSummary(p1.projectId, subj1Group2.skillId)
        List<UserPoints> points_after = userPointsRepo.findAll()
        List<UserAchievement> achievements_after = userAchievedRepo.findAll()

        int totalGroup1Before = 444*4 + 555*5 + 666*6 // 8,547
        int totalGroup2Before = 777*7 + 888*8 + 999*9 + 1110*10 // 32,634
        int totalSubj1Before = 111 + 222*2 + 333*3 + totalGroup1Before + totalGroup2Before; // 42,735
        int totalSubj2Before = 222 * 7 // 1,554
        int totalProjBefore = totalGroup1Before + totalGroup2Before // 41,181

        int totalGroup1After= newNumPerformToCompletion*newPntIncrement*3 // 12
        int totalSubj1After = 111 + 222*2 + 333*3 + totalGroup1After + totalGroup2Before // 34,200
        int totalProjAfter = totalSubj1After + totalGroup2Before // 66,834

        then:
        // Before:
        //  subj1=(42,735) 10% -> 4273; 25% -> 10683; 45% -> 19230; 67% -> 28632; 92% -> 39316
        //  subj2=(1,554) 10% -> 155; 25% -> 388; 45% -> 699; 67% -> 1041; 92% -> 1429
        //  proj=(41,181) 10% -> 4118; 25% -> 10295; 45% -> 18531; 67% -> 27591; 92% -> 37886

        // After:
        //  subj1=(42,735) 10% -> 4273; 25% -> 10683; 45% -> 19230; 67% -> 28632; 92% -> 39316
        //  subj2=(34,200) 10% -> 3420; 25% -> 8550; 45% -> 15390; 67% -> 22914; 92% -> 31464
        //  proj=(66,834) 10% -> 6683; 25% -> 16708; 45% -> 30075; 67% -> 44778; 92% -> 61487

        user1Group1Before.totalPoints == totalGroup1Before
        user1Group2Before.totalPoints == totalGroup2Before
        user1Subj1Before.totalPoints == totalSubj1Before

        user1Group1Before.points == 444*2 + 555*5 // 3,663
        user1Group2Before.points == 777*1 + 999*3 // 3,774
        user1Subj1Before.points == user1Group1Before.points + user1Group2Before.points + 222 // 7,659
        user1Subj1Before.skillsLevel == 1
        user1Group1Before.totalSkills == 3
        user1Group1Before.skillsAchieved == 1

        user1Group1Before.skills.totalPoints == [444*4, 555*5, 666*6]
        user1Group1Before.skills.pointIncrement == [444, 555, 666]
        user1Group1Before.skills.points == [444*2, 555*5, 0]
        user1Group1Before.skills.todaysPoints == user1Group1Before.skills.points

        user1Group2Before.skills.totalPoints == [777*7, 888*8, 999*9, 1110*10]
        user1Group2Before.skills.pointIncrement ==  [777, 888, 999, 1110]
        user1Group2Before.skills.points ==  [777*1, 0, 999*3, 0]
        user1Group2Before.skills.todaysPoints == user1Group2Before.skills.points

        user1Subj2Before.totalPoints == totalSubj2Before
        user1Subj2Before.points == 222*2
        user1Subj2Before.skillsLevel == 2

        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && !it.skillId }.points == user1Subj1Before.points + user1Subj2Before.points
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user1Subj1Before.points
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj2.subjectId }.points == user1Subj2Before.points
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[0].skillId }.points == 444*2
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[1].skillId }.points == 555*5
        !points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[2].skillId }

        achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId && it.level }).level.sort() == [1]
        achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj2.subjectId && it.level }).level.sort() == [1, 2]
        achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && !it.skillId && it.level }).level.sort() == [1]
        achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && p1Subj1Skills.skillId.contains(it.skillId) })?.skillId?.sort() == [underGroup1Skills[1].skillId]
        !achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == subj1Group1.skillId})

        // AFTER
        user1Group1After.totalPoints == totalGroup1After
        user1Group2After.totalPoints == totalGroup2Before
        user1Subj1After.totalPoints == totalSubj1After

        user1Group1After.points == newPntIncrement*2 + newPntIncrement*2 // 2,604
        user1Group2After.points == 777*1 + 999*3 // 3,774
        user1Subj1After.points == user1Group1After.points + user1Group2After.points + 222 // 6,600
        user1Subj1After.skillsLevel == 1
        user1Group1After.totalSkills == 3
        user1Group1After.skillsAchieved == 2

        user1Group1After.skills.totalPoints == [newPntIncrement*newNumPerformToCompletion, newPntIncrement*newNumPerformToCompletion, newPntIncrement*newNumPerformToCompletion]
        user1Group1After.skills.pointIncrement == [newPntIncrement, newPntIncrement, newPntIncrement]
        user1Group1After.skills.points == [newPntIncrement*2, newPntIncrement*2, 0]
        user1Group1After.skills.todaysPoints == user1Group1After.skills.points

        user1Group2After.skills.totalPoints == [777*7, 888*8, 999*9, 1110*10]
        user1Group2After.skills.pointIncrement ==  [777, 888, 999, 1110]
        user1Group2After.skills.points ==  [777*1, 0, 999*3, 0]
        user1Group2After.skills.todaysPoints == user1Group2After.skills.points

        user1Subj2After.totalPoints == totalSubj2Before
        user1Subj2After.points == 222*2
        user1Subj2After.skillsLevel == 2

        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && !it.skillId }.points == user1Subj1After.points + user1Subj2After.points
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user1Subj1After.points
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj2.subjectId }.points == user1Subj2After.points
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[0].skillId }.points == newPntIncrement*2
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[1].skillId }.points == newPntIncrement*2
        !points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[2].skillId }

        achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId && it.level }).level.sort() == [1]
        achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj2.subjectId && it.level }).level.sort() == [1, 2]
        achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && !it.skillId && it.level }).level.sort() == [1]
        achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && p1Subj1Skills.skillId.contains(it.skillId) })?.skillId?.sort() == [underGroup1Skills[0].skillId, underGroup1Skills[1].skillId]
        !achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == subj1Group1.skillId})
    }

    def "decreasing numPerformToCompletion causes group to be achieved"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(10, 1, 1, 100)
        p1Subj1Skills.each { it.pointIncrementInterval = 0; it.numPerformToCompletion = 3 }
        def underSubj1Skills = p1Subj1Skills[0..2]
        def underGroup1Skills = p1Subj1Skills[3..5]
        def underGroup2Skills = p1Subj1Skills[6..9]
        def subj1Group1 = createSkillsGroup(1, 1, 11)
        def subj1Group2 = createSkillsGroup(1, 1, 12)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [underSubj1Skills[0], underSubj1Skills[1], subj1Group1, subj1Group2, underSubj1Skills[2]])
        underGroup1Skills.each { skillsService.assignSkillToSkillsGroup(subj1Group1.skillId, it)}
        underGroup2Skills.each { skillsService.assignSkillToSkillsGroup(subj1Group2.skillId, it)}

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(7, 1, 2, 222)
        p1skillsSubj2.each { it.pointIncrementInterval = 0; it.numPerformToCompletion = 3 }
        def underSubj2Skills = p1skillsSubj2[0..2]
        def underGroup3Skills = p1skillsSubj2[3..6]
        def subj2Group3 = createSkillsGroup(1, 2, 23)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, [underSubj2Skills[0], underSubj2Skills[1], underSubj2Skills[2], subj2Group3])
        underGroup3Skills.each { skillsService.assignSkillToSkillsGroup(subj2Group3.skillId, it)}

        users[0].addSkill(p1Subj1Skills[0])
        users[0].addSkill(p1Subj1Skills[0])
        users[0].addSkill(p1Subj1Skills[0])
        users[0].addSkill(p1Subj1Skills[1])
        users[0].addSkill(p1Subj1Skills[1])
        users[0].addSkill(p1Subj1Skills[1])

        users[0].addSkill(underGroup1Skills[0])
        users[0].addSkill(underGroup1Skills[0])
        users[0].addSkill(underGroup1Skills[0])
        users[0].addSkill(underGroup1Skills[1])
        users[0].addSkill(underGroup1Skills[1])
        users[0].addSkill(underGroup1Skills[2])
        users[0].addSkill(underGroup1Skills[2])

        users[1].addSkill(p1Subj1Skills[0])
        users[1].addSkill(p1Subj1Skills[0])
        users[1].addSkill(p1Subj1Skills[0])
        users[1].addSkill(p1Subj1Skills[1])
        users[1].addSkill(p1Subj1Skills[1])
        users[1].addSkill(p1Subj1Skills[1])

        users[1].addSkill(underGroup1Skills[0])
        users[1].addSkill(underGroup1Skills[0])
        users[1].addSkill(underGroup1Skills[0])
        users[1].addSkill(underGroup1Skills[1])
        users[1].addSkill(underGroup1Skills[1])
        users[1].addSkill(underGroup1Skills[1])
        users[1].addSkill(underGroup1Skills[2])
        users[1].addSkill(underGroup1Skills[2])
        users[1].addSkill(underGroup1Skills[2])

        users[2].addSkill(underGroup1Skills[0])
        users[2].addSkill(underGroup1Skills[0])
        users[2].addSkill(underGroup1Skills[1])
        users[2].addSkill(underGroup1Skills[1])
        users[2].addSkill(underGroup1Skills[1])
        users[2].addSkill(underGroup1Skills[2])
        users[2].addSkill(underGroup1Skills[2])
        users[2].addSkill(underGroup1Skills[2])

        when:
        def user1Group1Before = users[0].getSkillsGroupSummary(p1.projectId, subj1Group1.skillId)
        def user2Group1Before = users[1].getSkillsGroupSummary(p1.projectId, subj1Group1.skillId)
        def user3Group1Before = users[2].getSkillsGroupSummary(p1.projectId, subj1Group1.skillId)
        List<UserPoints> points_before = userPointsRepo.findAll()
        List<UserAchievement> achievements_before = userAchievedRepo.findAll()
        List<UserPerformedSkill> performedSkills_before = userPerformedSkillRepo.findAll()

        int newNumPerformToCompletion = 2
        skillsService.batchUpdateSkills(p1.projectId, [
                numPerformToCompletion: newNumPerformToCompletion,
                skills: [underGroup1Skills[1].skillId, underGroup1Skills[2].skillId]
        ])
        def user1Group1After = users[0].getSkillsGroupSummary(p1.projectId, subj1Group1.skillId)
        def user2Group1After = users[1].getSkillsGroupSummary(p1.projectId, subj1Group1.skillId)
        def user3Group1After = users[2].getSkillsGroupSummary(p1.projectId, subj1Group1.skillId)
        List<UserPoints> points_after = userPointsRepo.findAll()
        List<UserAchievement> achievements_after = userAchievedRepo.findAll()
        List<UserPerformedSkill> performedSkills_after = userPerformedSkillRepo.findAll()

        then:
        user1Group1Before.totalSkills == 3
        user1Group1Before.skillsAchieved == 1
        user1Group1Before.totalPoints == 900
        user1Group1Before.points == 700
        user1Group1Before.skills.totalPoints == [300, 300, 300]
        user1Group1Before.skills.points == [300, 200, 200]

        user2Group1Before.totalSkills == 3
        user2Group1Before.skillsAchieved == 3
        user2Group1Before.totalPoints == 900
        user2Group1Before.points == 900
        user2Group1Before.skills.totalPoints == [300, 300, 300]
        user2Group1Before.skills.points == [300, 300, 300]

        user3Group1Before.totalSkills == 3
        user3Group1Before.skillsAchieved == 2
        user3Group1Before.totalPoints == 900
        user3Group1Before.points == 800
        user3Group1Before.skills.totalPoints == [300, 300, 300]
        user3Group1Before.skills.points == [200, 300, 300]

        achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && underGroup1Skills.skillId.contains(it.skillId) })?.skillId?.sort() == [underGroup1Skills[0].skillId]
        !achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == subj1Group1.skillId})
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[0].skillId}?.size() == 3
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[1].skillId}?.size() == 2
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[2].skillId}?.size() == 2

        achievements_before.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && underGroup1Skills.skillId.contains(it.skillId) })?.skillId?.sort() == [underGroup1Skills[0].skillId, underGroup1Skills[1].skillId, underGroup1Skills[2].skillId]
        achievements_before.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == subj1Group1.skillId})
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[0].skillId}?.size() == 3
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[1].skillId}?.size() == 3
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[2].skillId}?.size() == 3

        achievements_before.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && underGroup1Skills.skillId.contains(it.skillId) })?.skillId?.sort() == [underGroup1Skills[1].skillId, underGroup1Skills[2].skillId]
        !achievements_before.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == subj1Group1.skillId})
        performedSkills_before.findAll { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[0].skillId}?.size() == 2
        performedSkills_before.findAll { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[1].skillId}?.size() == 3
        performedSkills_before.findAll { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[2].skillId}?.size() == 3

        // AFTER
        user1Group1After.totalSkills == 3
        user1Group1After.skillsAchieved == 3
        user1Group1After.totalPoints == 700
        user1Group1After.points == 700
        user1Group1After.skills.totalPoints == [300, 200, 200]
        user1Group1After.skills.points == [300, 200, 200]

        user2Group1After.totalSkills == 3
        user2Group1After.skillsAchieved == 3
        user2Group1After.totalPoints == 700
        user2Group1After.points == 700
        user2Group1After.skills.totalPoints == [300, 200, 200]
        user2Group1After.skills.points == [300, 200, 200]

        user3Group1After.totalSkills == 3
        user3Group1After.skillsAchieved == 2
        user3Group1After.totalPoints == 700
        user3Group1After.points == 600
        user3Group1After.skills.totalPoints == [300, 200, 200]
        user3Group1After.skills.points == [200, 200, 200]

        achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && underGroup1Skills.skillId.contains(it.skillId) })?.skillId?.sort() == [underGroup1Skills[0].skillId, underGroup1Skills[1].skillId, underGroup1Skills[2].skillId]
        achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == subj1Group1.skillId})
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[0].skillId}?.size() == 3
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[1].skillId}?.size() == 2
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[2].skillId}?.size() == 2

        achievements_after.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && underGroup1Skills.skillId.contains(it.skillId) })?.skillId?.sort() == [underGroup1Skills[0].skillId, underGroup1Skills[1].skillId, underGroup1Skills[2].skillId]
        achievements_after.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == subj1Group1.skillId})
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[0].skillId}?.size() == 3
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[1].skillId}?.size() == 2
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[2].skillId}?.size() == 2

        achievements_after.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && underGroup1Skills.skillId.contains(it.skillId) })?.skillId?.sort() == [underGroup1Skills[1].skillId, underGroup1Skills[2].skillId]
        !achievements_after.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == subj1Group1.skillId})
        performedSkills_after.findAll { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[0].skillId}?.size() == 2
        performedSkills_after.findAll { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[1].skillId}?.size() == 2
        performedSkills_after.findAll { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[2].skillId}?.size() == 2
    }

    def "increasing numPerformToCompletion causes group achievement to be removed"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(10, 1, 1, 100)
        p1Subj1Skills.each { it.pointIncrementInterval = 0; it.numPerformToCompletion = 3 }
        def underSubj1Skills = p1Subj1Skills[0..2]
        def underGroup1Skills = p1Subj1Skills[3..5]
        def underGroup2Skills = p1Subj1Skills[6..9]
        def subj1Group1 = createSkillsGroup(1, 1, 11)
        def subj1Group2 = createSkillsGroup(1, 1, 12)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [underSubj1Skills[0], underSubj1Skills[1], subj1Group1, subj1Group2, underSubj1Skills[2]])
        underGroup1Skills.each { skillsService.assignSkillToSkillsGroup(subj1Group1.skillId, it)}
        underGroup2Skills.each { skillsService.assignSkillToSkillsGroup(subj1Group2.skillId, it)}

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(7, 1, 2, 222)
        p1skillsSubj2.each { it.pointIncrementInterval = 0; it.numPerformToCompletion = 3 }
        def underSubj2Skills = p1skillsSubj2[0..2]
        def underGroup3Skills = p1skillsSubj2[3..6]
        def subj2Group3 = createSkillsGroup(1, 2, 23)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, [underSubj2Skills[0], underSubj2Skills[1], underSubj2Skills[2], subj2Group3])
        underGroup3Skills.each { skillsService.assignSkillToSkillsGroup(subj2Group3.skillId, it)}

        users[0].addSkill(p1Subj1Skills[0])
        users[0].addSkill(p1Subj1Skills[0])
        users[0].addSkill(p1Subj1Skills[0])
        users[0].addSkill(p1Subj1Skills[1])
        users[0].addSkill(p1Subj1Skills[1])
        users[0].addSkill(p1Subj1Skills[1])

        users[0].addSkill(underGroup1Skills[0])
        users[0].addSkill(underGroup1Skills[0])
        users[0].addSkill(underGroup1Skills[0])
        users[0].addSkill(underGroup1Skills[1])
        users[0].addSkill(underGroup1Skills[1])
        users[0].addSkill(underGroup1Skills[2])
        users[0].addSkill(underGroup1Skills[2])

        users[1].addSkill(p1Subj1Skills[0])
        users[1].addSkill(p1Subj1Skills[0])
        users[1].addSkill(p1Subj1Skills[0])
        users[1].addSkill(p1Subj1Skills[1])
        users[1].addSkill(p1Subj1Skills[1])
        users[1].addSkill(p1Subj1Skills[1])

        users[1].addSkill(underGroup1Skills[0])
        users[1].addSkill(underGroup1Skills[0])
        users[1].addSkill(underGroup1Skills[0])
        users[1].addSkill(underGroup1Skills[1])
        users[1].addSkill(underGroup1Skills[1])
        users[1].addSkill(underGroup1Skills[1])
        users[1].addSkill(underGroup1Skills[2])
        users[1].addSkill(underGroup1Skills[2])
        users[1].addSkill(underGroup1Skills[2])

        users[2].addSkill(underGroup1Skills[0])
        users[2].addSkill(underGroup1Skills[0])
        users[2].addSkill(underGroup1Skills[1])
        users[2].addSkill(underGroup1Skills[1])
        users[2].addSkill(underGroup1Skills[1])
        users[2].addSkill(underGroup1Skills[2])
        users[2].addSkill(underGroup1Skills[2])
        users[2].addSkill(underGroup1Skills[2])

        when:
        def user1Group1Before = users[0].getSkillsGroupSummary(p1.projectId, subj1Group1.skillId)
        def user2Group1Before = users[1].getSkillsGroupSummary(p1.projectId, subj1Group1.skillId)
        def user3Group1Before = users[2].getSkillsGroupSummary(p1.projectId, subj1Group1.skillId)
        List<UserPoints> points_before = userPointsRepo.findAll()
        List<UserAchievement> achievements_before = userAchievedRepo.findAll()
        List<UserPerformedSkill> performedSkills_before = userPerformedSkillRepo.findAll()

        int newNumPerformToCompletion = 4
        skillsService.batchUpdateSkills(p1.projectId, [
                numPerformToCompletion: newNumPerformToCompletion,
                skills: [underGroup1Skills[1].skillId, underGroup1Skills[2].skillId]
        ])
        def user1Group1After = users[0].getSkillsGroupSummary(p1.projectId, subj1Group1.skillId)
        def user2Group1After = users[1].getSkillsGroupSummary(p1.projectId, subj1Group1.skillId)
        def user3Group1After = users[2].getSkillsGroupSummary(p1.projectId, subj1Group1.skillId)
        List<UserPoints> points_after = userPointsRepo.findAll()
        List<UserAchievement> achievements_after = userAchievedRepo.findAll()
        List<UserPerformedSkill> performedSkills_after = userPerformedSkillRepo.findAll()

        then:
        user1Group1Before.totalSkills == 3
        user1Group1Before.skillsAchieved == 1
        user1Group1Before.totalPoints == 900
        user1Group1Before.points == 700
        user1Group1Before.skills.totalPoints == [300, 300, 300]
        user1Group1Before.skills.points == [300, 200, 200]

        user2Group1Before.totalSkills == 3
        user2Group1Before.skillsAchieved == 3
        user2Group1Before.totalPoints == 900
        user2Group1Before.points == 900
        user2Group1Before.skills.totalPoints == [300, 300, 300]
        user2Group1Before.skills.points == [300, 300, 300]

        user3Group1Before.totalSkills == 3
        user3Group1Before.skillsAchieved == 2
        user3Group1Before.totalPoints == 900
        user3Group1Before.points == 800
        user3Group1Before.skills.totalPoints == [300, 300, 300]
        user3Group1Before.skills.points == [200, 300, 300]

        achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && underGroup1Skills.skillId.contains(it.skillId) })?.skillId?.sort() == [underGroup1Skills[0].skillId]
        !achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == subj1Group1.skillId})
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[0].skillId}?.size() == 3
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[1].skillId}?.size() == 2
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[2].skillId}?.size() == 2

        achievements_before.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && underGroup1Skills.skillId.contains(it.skillId) })?.skillId?.sort() == [underGroup1Skills[0].skillId, underGroup1Skills[1].skillId, underGroup1Skills[2].skillId]
        achievements_before.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == subj1Group1.skillId})
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[0].skillId}?.size() == 3
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[1].skillId}?.size() == 3
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[2].skillId}?.size() == 3

        achievements_before.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && underGroup1Skills.skillId.contains(it.skillId) })?.skillId?.sort() == [underGroup1Skills[1].skillId, underGroup1Skills[2].skillId]
        !achievements_before.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == subj1Group1.skillId})
        performedSkills_before.findAll { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[0].skillId}?.size() == 2
        performedSkills_before.findAll { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[1].skillId}?.size() == 3
        performedSkills_before.findAll { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[2].skillId}?.size() == 3

        // AFTER
        user1Group1After.totalSkills == 3
        user1Group1After.skillsAchieved == 1
        user1Group1After.totalPoints == 1100
        user1Group1After.points == 700
        user1Group1After.skills.totalPoints == [300, 400, 400]
        user1Group1After.skills.points == [300, 200, 200]

        user2Group1After.totalSkills == 3
        user2Group1After.skillsAchieved == 1
        user2Group1After.totalPoints == 1100
        user2Group1After.points == 900
        user2Group1After.skills.totalPoints == [300, 400, 400]
        user2Group1After.skills.points == [300, 300, 300]

        user3Group1After.totalSkills == 3
        user3Group1After.skillsAchieved == 0
        user3Group1After.totalPoints == 1100
        user3Group1After.points == 800
        user3Group1After.skills.totalPoints == [300, 400, 400]
        user3Group1After.skills.points == [200, 300, 300]

        achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && underGroup1Skills.skillId.contains(it.skillId) })?.skillId?.sort() == [underGroup1Skills[0].skillId]
        !achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == subj1Group1.skillId})
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[0].skillId}?.size() == 3
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[1].skillId}?.size() == 2
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[2].skillId}?.size() == 2

        achievements_after.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && underGroup1Skills.skillId.contains(it.skillId) })?.skillId?.sort() == [underGroup1Skills[0].skillId]

        // group achievement are kept in place for now but the approach may change in the future, will keep the check here
        achievements_after.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == subj1Group1.skillId})
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[0].skillId}?.size() == 3
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[1].skillId}?.size() == 3
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[2].skillId}?.size() == 3

        achievements_after.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && underGroup1Skills.skillId.contains(it.skillId) })?.skillId?.sort() == []
        !achievements_after.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == subj1Group1.skillId})
        performedSkills_after.findAll { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[0].skillId}?.size() == 2
        performedSkills_after.findAll { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[1].skillId}?.size() == 3
        performedSkills_after.findAll { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == underGroup1Skills[2].skillId}?.size() == 3
    }

}