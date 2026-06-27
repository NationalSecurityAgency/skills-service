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


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints

import static skills.intTests.utils.SkillsFactory.*

class BatchUpdateUserAchievements_ApprovalToHonorSpec extends DefaultIntSpec {

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

    def "pending approval request are applied when switching from skills from Approval-based to Honor reporting type"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 111, 2)
        p1skills.each { it.pointIncrementInterval = 0 }
        p1skills[0..2].each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 222)
        p1skillsSubj2[0..1].each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        skillsService.addSkill(p1skills[0], users[0].userName, new Date(), "msg", true) // bypasses approval when reported by admin
        users[0].addSkill(p1skills[0])
        users[0].addSkill(p1skills[1])
        users[0].addSkill(p1skills[3])
        users[0].addSkill(p1skills[3])
        users[0].addSkill(p1skills[4])
        users[0].addSkill(p1skillsSubj2[0])
        users[0].addSkill(p1skillsSubj2[3])

        skillsService.addSkill(p1skillsSubj2[1], users[1].userName, new Date(), "completed advanced task", true) // bypasses approval when reported by admin
        users[1].addSkill(p1skillsSubj2[1])
        users[1].addSkill(p1skillsSubj2[2])
        users[1].addSkill(p1skillsSubj2[3])
        users[1].addSkill(p1skills[2])
        users[1].addSkill(p1skills[3])
        users[1].addSkill(p1skills[4])
        
        when:

        def user1_before = users[0].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def user2_before = users[1].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def approvals_before = skillsService.getApprovals(p1.projectId, 20, 1, 'requestedOn', false, '', '')
        List<UserPoints> points_before = userPointsRepo.findAll()
        List<UserAchievement> achievements_before = userAchievedRepo.findAll()
        List<UserPerformedSkill> performedSkills_before = userPerformedSkillRepo.findAll()
        skillsService.batchUpdateSkills(p1.projectId, [
                selfReportingType: SkillDef.SelfReportingType.HonorSystem.toString(),
                skills: p1skills[0..1].skillId
        ])
        def user1_after = users[0].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def user2_after = users[1].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def approvals_after = skillsService.getApprovals(p1.projectId, 20, 1, 'requestedOn', false, '', '')
        List<UserPoints> points_after = userPointsRepo.findAll()
        List<UserAchievement> achievements_after = userAchievedRepo.findAll()
        List<UserPerformedSkill> performedSkills_after = userPerformedSkillRepo.findAll()

        def subj1_total_before = 111 * 2 * 5; // 1,110
        def subj2_total_before = 222 * 4
        then:
        // Before:
        // subj1: total 1,110; 10% -> 111, 25% -> 277, 45% -> 500, 67% -> 744, 92% -> 1021
        // subj2: total 888; 10% -> 89, 25% -> 222, 45% -> 400, 67% -> 595, 92% -> 817
        // overall: total 1,998; 10% -> 200, 25% -> 500, 45% -> 899, 67% -> 1339, 92% -> 1838

        // user1
        user1_before.totalPoints == subj1_total_before
        user1_before.points == 111 * 4
        user1_before.todaysPoints == user1_before.points
        user1_before.skillsAchieved == 1
        user1_before.skillsLevel == 2
        user1_before.levelPoints == user1_before.points - 277

        user1_before.skills.totalPoints == [111*2, 111*2, 111*2, 111*2, 111*2]
        user1_before.skills.points == [111, 0, 0, 111*2, 111*1]
        user1_before.skills.todaysPoints == user1_before.skills.points

        approvals_before.data.findAll { it.userId == users[0].userName && it.projectId == p1.projectId }.skillId.sort() == [p1skills[0].skillId, p1skills[1].skillId, p1skillsSubj2[0].skillId].sort()

        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && !it.skillId }.points == user1_before.skills.points.sum() + 222
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user1_before.skills.points.sum()
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId }.points == 111
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.points == 111 * 2
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 111
        achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && p1skills.skillId.contains(it.skillId) })?.skillId == [p1skills[3].skillId]
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId}.size() == 1
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId}.size() == 0
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[2].skillId}.size() == 0
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId}.size() == 2
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId}.size() == 1

        // user2
        user2_before.totalPoints == subj1_total_before
        user2_before.points == 111 * 2
        user2_before.todaysPoints == user2_before.points
        user2_before.skillsAchieved == 0
        user2_before.skillsLevel == 1
        user2_before.levelPoints == user2_before.points - 111

        user2_before.skills.totalPoints == [111 * 2, 111 * 2, 111 * 2, 111 * 2, 111 * 2]
        user2_before.skills.points == [0, 0, 0, 111 * 1, 111 * 1]
        user2_before.skills.todaysPoints == user2_before.skills.points

        approvals_before.data.findAll { it.userId == users[1].userName && it.projectId == p1.projectId }.skillId.sort() == [p1skills[2].skillId].sort()

        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && !it.skillId }.points == user2_before.skills.points.sum() + 222 * 3
        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user2_before.skills.points.sum()
        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.points == 111
        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 111
        achievements_before.findAll({ it.userId == users[1].userName && it.projectId == p1.projectId && p1skills.skillId.contains(it.skillId) })?.skillId == []
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId }.size() == 0
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId }.size() == 0
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[2].skillId }.size() == 0
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.size() == 1
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.size() == 1


        // AFTER ---------------------------
        // subj1: total 1,110; 10% -> 111, 25% -> 277, 45% -> 499, 67% -> 744, 92% -> 1021
        // subj2: total 888; 10% -> 89, 25% -> 222, 45% -> 400, 67% -> 595, 92% -> 817
        // overall: total 1,998; 10% -> 200, 25% -> 500, 45% -> 899, 67% -> 1339, 92% -> 1838

        // user1
        user1_after.totalPoints == subj1_total_before
        user1_after.points == 111 * 6
        user1_after.todaysPoints == user1_after.points
        user1_after.skillsAchieved == 2
        user1_after.skillsLevel == 3
        user1_after.levelPoints == user1_after.points - 499

        user1_after.skills.totalPoints == [111*2, 111*2, 111*2, 111*2, 111*2]
        user1_after.skills.points == [111*2, 111, 0, 111*2, 111*1]
        user1_after.skills.todaysPoints == user1_after.skills.points

        approvals_after.data.findAll { it.userId == users[0].userName && it.projectId == p1.projectId }.skillId.sort() == [p1skillsSubj2[0].skillId].sort()

        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && !it.skillId }.points == user1_after.skills.points.sum() + 222
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user1_after.skills.points.sum()
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId }.points == 111 * 2
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId }.points == 111
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.points == 111 * 2
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 111
        achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && p1skills.skillId.contains(it.skillId) })?.skillId?.sort() == [p1skills[0].skillId, p1skills[3].skillId].sort()
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId}.size() == 2
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId}.size() == 1
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[2].skillId}.size() == 0
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId}.size() == 2
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId}.size() == 1

        // user2
        user2_after.totalPoints == subj1_total_before
        user2_after.points == 111 * 2
        user2_after.todaysPoints == user2_after.points
        user2_after.skillsAchieved == 0
        user2_after.skillsLevel == 1
        user2_after.levelPoints == user2_after.points - 111

        user2_after.skills.totalPoints == [111 * 2, 111 * 2, 111 * 2, 111 * 2, 111 * 2]
        user2_after.skills.points == [0, 0, 0, 111 * 1, 111 * 1]
        user2_after.skills.todaysPoints == user2_after.skills.points

        approvals_after.data.findAll { it.userId == users[1].userName && it.projectId == p1.projectId }.skillId.sort() == [p1skills[2].skillId].sort()

        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && !it.skillId }.points == user2_after.skills.points.sum() + 222 * 3
        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user2_after.skills.points.sum()
        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.points == 111
        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 111
        achievements_after.findAll({ it.userId == users[1].userName && it.projectId == p1.projectId && p1skills.skillId.contains(it.skillId) })?.skillId == []
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId }.size() == 0
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId }.size() == 0
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[2].skillId }.size() == 0
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.size() == 1
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.size() == 1

    }

    def "pending approval request are applied when switching from skills from Approval-based to Honor reporting type AND numPerformToCompletion is increased"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 111, 2)
        p1skills.each { it.pointIncrementInterval = 0 }
        p1skills[0..2].each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 222)
        p1skillsSubj2[0..1].each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        skillsService.addSkill(p1skills[0], users[0].userName, new Date(), "msg", true) // bypasses approval when reported by admin
        users[0].addSkill(p1skills[0])
        users[0].addSkill(p1skills[1])
        users[0].addSkill(p1skills[3])
        users[0].addSkill(p1skills[3])
        users[0].addSkill(p1skills[4])
        users[0].addSkill(p1skillsSubj2[0])
        users[0].addSkill(p1skillsSubj2[3])

        skillsService.addSkill(p1skillsSubj2[1], users[1].userName, new Date(), "completed advanced task", true) // bypasses approval when reported by admin
        users[1].addSkill(p1skillsSubj2[1])
        users[1].addSkill(p1skillsSubj2[2])
        users[1].addSkill(p1skillsSubj2[3])
        users[1].addSkill(p1skills[2])
        users[1].addSkill(p1skills[3])
        users[1].addSkill(p1skills[4])

        when:
        def user1_before = users[0].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def user2_before = users[1].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def approvals_before = skillsService.getApprovals(p1.projectId, 20, 1, 'requestedOn', false, '', '')
        List<UserPoints> points_before = userPointsRepo.findAll()
        List<UserAchievement> achievements_before = userAchievedRepo.findAll()
        List<UserPerformedSkill> performedSkills_before = userPerformedSkillRepo.findAll()
        int newNumPerformToCompletion = 6
        skillsService.batchUpdateSkills(p1.projectId, [
                selfReportingType: SkillDef.SelfReportingType.HonorSystem.toString(),
                numPerformToCompletion: newNumPerformToCompletion,
                skills: p1skills[0..1].skillId
        ])
        def user1_after = users[0].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def user2_after = users[1].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def approvals_after = skillsService.getApprovals(p1.projectId, 20, 1, 'requestedOn', false, '', '')
        List<UserPoints> points_after = userPointsRepo.findAll()
        List<UserAchievement> achievements_after = userAchievedRepo.findAll()
        List<UserPerformedSkill> performedSkills_after = userPerformedSkillRepo.findAll()

        def subj1_total_before = 111 * 2 * 5; // 1,110
        def subj1_total_after = 111 * 2 * 3 + 111 * 6 * 2 // 1998
        then:
        // Before:
        // subj1: total 1,110; 10% -> 111, 25% -> 277, 45% -> 500, 67% -> 744, 92% -> 1021
        // subj2: total 888; 10% -> 89, 25% -> 222, 45% -> 400, 67% -> 595, 92% -> 817
        // overall: total 1,998; 10% -> 200, 25% -> 500, 45% -> 899, 67% -> 1339, 92% -> 1838

        // user1
        user1_before.totalPoints == subj1_total_before
        user1_before.points == 111 * 4
        user1_before.todaysPoints == user1_before.points
        user1_before.skillsAchieved == 1
        user1_before.skillsLevel == 2
        user1_before.levelPoints == user1_before.points - 277

        user1_before.skills.totalPoints == [111*2, 111*2, 111*2, 111*2, 111*2]
        user1_before.skills.points == [111, 0, 0, 111*2, 111*1]
        user1_before.skills.todaysPoints == user1_before.skills.points

        approvals_before.data.findAll { it.userId == users[0].userName && it.projectId == p1.projectId }.skillId.sort() == [p1skills[0].skillId, p1skills[1].skillId, p1skillsSubj2[0].skillId].sort()

        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && !it.skillId }.points == user1_before.skills.points.sum() + 222
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user1_before.skills.points.sum()
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId }.points == 111
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.points == 111 * 2
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 111
        achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && p1skills.skillId.contains(it.skillId) })?.skillId == [p1skills[3].skillId]
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId}.size() == 1
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId}.size() == 0
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[2].skillId}.size() == 0
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId}.size() == 2
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId}.size() == 1

        // user2
        user2_before.totalPoints == subj1_total_before
        user2_before.points == 111 * 2
        user2_before.todaysPoints == user2_before.points
        user2_before.skillsAchieved == 0
        user2_before.skillsLevel == 1
        user2_before.levelPoints == user2_before.points - 111

        user2_before.skills.totalPoints == [111 * 2, 111 * 2, 111 * 2, 111 * 2, 111 * 2]
        user2_before.skills.points == [0, 0, 0, 111 * 1, 111 * 1]
        user2_before.skills.todaysPoints == user2_before.skills.points

        approvals_before.data.findAll { it.userId == users[1].userName && it.projectId == p1.projectId }.skillId.sort() == [p1skills[2].skillId].sort()

        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && !it.skillId }.points == user2_before.skills.points.sum() + 222 * 3
        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user2_before.skills.points.sum()
        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.points == 111
        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 111
        achievements_before.findAll({ it.userId == users[1].userName && it.projectId == p1.projectId && p1skills.skillId.contains(it.skillId) })?.skillId == []
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId }.size() == 0
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId }.size() == 0
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[2].skillId }.size() == 0
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.size() == 1
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.size() == 1


        // AFTER ---------------------------
        // subj2: total 1998; 10% -> 199, 25% -> 499, 45% -> 899, 67% -> 1339, 92% -> 1838

        // user1
        user1_after.totalPoints == subj1_total_after
        user1_after.points == 111 * 6
        user1_after.todaysPoints == user1_after.points
        user1_after.skillsAchieved == 1
        user1_after.skillsLevel == 2
        user1_after.levelPoints == user1_after.points - 499

        user1_after.skills.totalPoints == [111*6, 111*6, 111*2, 111*2, 111*2]
        user1_after.skills.points == [111*2, 111, 0, 111*2, 111*1]
        user1_after.skills.todaysPoints == user1_after.skills.points

        approvals_after.data.findAll { it.userId == users[0].userName && it.projectId == p1.projectId }.skillId.sort() == [p1skillsSubj2[0].skillId].sort()

        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && !it.skillId }.points == user1_after.skills.points.sum() + 222
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user1_after.skills.points.sum()
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId }.points == 111 * 2
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId }.points == 111
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.points == 111 * 2
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 111
        achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && p1skills.skillId.contains(it.skillId) })?.skillId?.sort() == [p1skills[3].skillId].sort()
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId}.size() == 2
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId}.size() == 1
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[2].skillId}.size() == 0
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId}.size() == 2
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId}.size() == 1

        // user2
        user2_after.totalPoints == subj1_total_after
        user2_after.points == 111 * 2
        user2_after.todaysPoints == user2_after.points
        user2_after.skillsAchieved == 0
        user2_after.skillsLevel == 1
        user2_after.levelPoints == user2_after.points - 199

        user2_after.skills.totalPoints == [111 * 6, 111 * 6, 111 * 2, 111 * 2, 111 * 2]
        user2_after.skills.points == [0, 0, 0, 111 * 1, 111 * 1]
        user2_after.skills.todaysPoints == user2_after.skills.points

        approvals_after.data.findAll { it.userId == users[1].userName && it.projectId == p1.projectId }.skillId.sort() == [p1skills[2].skillId].sort()

        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && !it.skillId }.points == user2_after.skills.points.sum() + 222 * 3
        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user2_after.skills.points.sum()
        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.points == 111
        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 111
        achievements_after.findAll({ it.userId == users[1].userName && it.projectId == p1.projectId && p1skills.skillId.contains(it.skillId) })?.skillId == []
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId }.size() == 0
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId }.size() == 0
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[2].skillId }.size() == 0
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.size() == 1
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.size() == 1

    }

    def "pending approval request are applied when switching from skills from Approval-based to Honor reporting type AND numPerformToCompletion is decreased"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 111, 2)
        p1skills.each { it.pointIncrementInterval = 0 }
        p1skills[0..2].each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 222)
        p1skillsSubj2[0..1].each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        skillsService.addSkill(p1skills[0], users[0].userName, new Date(), "msg", true) // bypasses approval when reported by admin
        users[0].addSkill(p1skills[0])
        users[0].addSkill(p1skills[1])
        users[0].addSkill(p1skills[3])
        users[0].addSkill(p1skills[3])
        users[0].addSkill(p1skills[4])
        users[0].addSkill(p1skillsSubj2[0])
        users[0].addSkill(p1skillsSubj2[3])

        skillsService.addSkill(p1skillsSubj2[1], users[1].userName, new Date(), "completed advanced task", true) // bypasses approval when reported by admin
        users[1].addSkill(p1skillsSubj2[1])
        users[1].addSkill(p1skillsSubj2[2])
        users[1].addSkill(p1skillsSubj2[3])
        users[1].addSkill(p1skills[2])
        users[1].addSkill(p1skills[3])
        users[1].addSkill(p1skills[4])

        when:

        def user1_before = users[0].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def user2_before = users[1].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def approvals_before = skillsService.getApprovals(p1.projectId, 20, 1, 'requestedOn', false, '', '')
        List<UserPoints> points_before = userPointsRepo.findAll()
        List<UserAchievement> achievements_before = userAchievedRepo.findAll()
        List<UserPerformedSkill> performedSkills_before = userPerformedSkillRepo.findAll()
        int newNumPerformToCompletion = 1
        skillsService.batchUpdateSkills(p1.projectId, [
                selfReportingType: SkillDef.SelfReportingType.HonorSystem.toString(),
                numPerformToCompletion: newNumPerformToCompletion,
                skills: p1skills[0..1].skillId
        ])
        def user1_after = users[0].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def user2_after = users[1].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def approvals_after = skillsService.getApprovals(p1.projectId, 20, 1, 'requestedOn', false, '', '')
        List<UserPoints> points_after = userPointsRepo.findAll()
        List<UserAchievement> achievements_after = userAchievedRepo.findAll()
        List<UserPerformedSkill> performedSkills_after = userPerformedSkillRepo.findAll()

        def subj1_total_before = 111 * 2 * 5; // 1,110
        def subj1_total_after = 111 * 2 * 3 + 111 * 1 * 2 // 888
        then:
        // Before:
        // subj1: total 1,110; 10% -> 111, 25% -> 277, 45% -> 500, 67% -> 744, 92% -> 1021
        // subj2: total 888; 10% -> 89, 25% -> 222, 45% -> 400, 67% -> 595, 92% -> 817
        // overall: total 1,998; 10% -> 200, 25% -> 500, 45% -> 899, 67% -> 1339, 92% -> 1838

        // user1
        user1_before.totalPoints == subj1_total_before
        user1_before.points == 111 * 4
        user1_before.todaysPoints == user1_before.points
        user1_before.skillsAchieved == 1
        user1_before.skillsLevel == 2
        user1_before.levelPoints == user1_before.points - 277

        user1_before.skills.totalPoints == [111*2, 111*2, 111*2, 111*2, 111*2]
        user1_before.skills.points == [111, 0, 0, 111*2, 111*1]
        user1_before.skills.todaysPoints == user1_before.skills.points

        approvals_before.data.findAll { it.userId == users[0].userName && it.projectId == p1.projectId }.skillId.sort() == [p1skills[0].skillId, p1skills[1].skillId, p1skillsSubj2[0].skillId].sort()

        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && !it.skillId }.points == user1_before.skills.points.sum() + 222
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user1_before.skills.points.sum()
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId }.points == 111
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.points == 111 * 2
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 111
        achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && p1skills.skillId.contains(it.skillId) })?.skillId == [p1skills[3].skillId]
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId}.size() == 1
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId}.size() == 0
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[2].skillId}.size() == 0
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId}.size() == 2
        performedSkills_before.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId}.size() == 1

        // user2
        user2_before.totalPoints == subj1_total_before
        user2_before.points == 111 * 2
        user2_before.todaysPoints == user2_before.points
        user2_before.skillsAchieved == 0
        user2_before.skillsLevel == 1
        user2_before.levelPoints == user2_before.points - 111

        user2_before.skills.totalPoints == [111 * 2, 111 * 2, 111 * 2, 111 * 2, 111 * 2]
        user2_before.skills.points == [0, 0, 0, 111 * 1, 111 * 1]
        user2_before.skills.todaysPoints == user2_before.skills.points

        approvals_before.data.findAll { it.userId == users[1].userName && it.projectId == p1.projectId }.skillId.sort() == [p1skills[2].skillId].sort()

        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && !it.skillId }.points == user2_before.skills.points.sum() + 222 * 3
        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user2_before.skills.points.sum()
        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.points == 111
        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 111
        achievements_before.findAll({ it.userId == users[1].userName && it.projectId == p1.projectId && p1skills.skillId.contains(it.skillId) })?.skillId == []
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId }.size() == 0
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId }.size() == 0
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[2].skillId }.size() == 0
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.size() == 1
        performedSkills_before.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.size() == 1


        // AFTER ---------------------------
        // subj1: total 888; 10% -> 89, 25% -> 222, 45% -> 399, 67% -> 595, 92% -> 817

        // user1
        user1_after.totalPoints == subj1_total_after
        user1_after.points == 111 * 5
        user1_after.todaysPoints == user1_after.points
        user1_after.skillsAchieved == 3
        user1_after.skillsLevel == 3
        user1_after.levelPoints == user1_after.points - 399

        user1_after.skills.totalPoints == [111*1, 111*1, 111*2, 111*2, 111*2]
        user1_after.skills.points == [111, 111, 0, 111*2, 111*1]
        user1_after.skills.todaysPoints == user1_after.skills.points

        approvals_after.data.findAll { it.userId == users[0].userName && it.projectId == p1.projectId }.skillId.sort() == [p1skillsSubj2[0].skillId].sort()

        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && !it.skillId }.points == user1_after.skills.points.sum() + 222
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user1_after.skills.points.sum()
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId }.points == 111
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId }.points == 111
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.points == 111 * 2
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 111
        achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && p1skills.skillId.contains(it.skillId) })?.skillId?.sort() == [p1skills[0].skillId, p1skills[1].skillId, p1skills[3].skillId].sort()
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId}.size() == 1
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId}.size() == 1
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[2].skillId}.size() == 0
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId}.size() == 2
        performedSkills_after.findAll { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId}.size() == 1

        // user2
        user2_after.totalPoints == subj1_total_after
        user2_after.points == 111 * 2
        user2_after.todaysPoints == user2_after.points
        user2_after.skillsAchieved == 0
        user2_after.skillsLevel == 2
        user2_after.levelPoints == user2_after.points - 222

        user2_after.skills.totalPoints == [111 * 1, 111 * 1, 111 * 2, 111 * 2, 111 * 2]
        user2_after.skills.points == [0, 0, 0, 111 * 1, 111 * 1]
        user2_after.skills.todaysPoints == user2_after.skills.points - 222

        approvals_after.data.findAll { it.userId == users[1].userName && it.projectId == p1.projectId }.skillId.sort() == [p1skills[2].skillId].sort()

        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && !it.skillId }.points == user2_after.skills.points.sum() + 222 * 3
        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user2_after.skills.points.sum()
        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.points == 111
        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 111
        achievements_after.findAll({ it.userId == users[1].userName && it.projectId == p1.projectId && p1skills.skillId.contains(it.skillId) })?.skillId == []
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId }.size() == 0
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId }.size() == 0
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[2].skillId }.size() == 0
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.size() == 1
        performedSkills_after.findAll { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.size() == 1

    }

}