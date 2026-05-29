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
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints

import static skills.intTests.utils.SkillsFactory.*

class BatchUpdateUserAchievementsSpec extends DefaultIntSpec {

    def "user points are updated for skill, subject and project"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100)
        p1skills.eachWithIndex { it, index ->
            it.pointIncrement = 111 * (index + 1)
            it.numPerformToCompletion = 1 * (index + 1)
            it.pointIncrementInterval = 0
        }

        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        List<SkillsService> users = getRandomUsers(3).collect { createService(it)}

        users[0].addSkill(p1skills[1])

        users[1].addSkill(p1skills[1])
        users[1].addSkill(p1skills[1])
        users[1].addSkill(p1skills[2])
        users[1].addSkill(p1skills[2])
        users[1].addSkill(p1skills[2])
        users[1].addSkill(p1skills[3])
        users[1].addSkill(p1skills[4])

        users[2].addSkill(p1skills[0])
        users[2].addSkill(p1skills[4])
        users[2].addSkill(p1skills[4])
        users[2].addSkill(p1skills[4])

        when:
        def user1_before = users[0].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def user2_before = users[1].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def user3_before = users[2].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        List<UserPoints> points_before = userPointsRepo.findAll()
        List<UserAchievement> achievements_before = userAchievedRepo.findAll()
        skillsService.batchUpdateSkills(p1.projectId, [
                pointIncrement: 651,
                skills: p1skills[1..3].skillId
        ])
        def user1_after = users[0].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def user2_after = users[1].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        def user3_after = users[2].getSubjectSummaryForCurrentUser(p1.projectId, p1subj1.subjectId)
        List<UserPoints> points_after = userPointsRepo.findAll()
        List<UserAchievement> achievements_after = userAchievedRepo.findAll()

        def total_before = 111 + 222*2 + 333*3 + 444*4 + 555*5; // 6105
        def total_after = 111 + 651*2 + 651*3 + 651*4 + 555*5; // 8745
        then:
        // Before: Level points based on total_before (6105):
        //   10% -> 611
        //   25% -> 1526
        //   45% -> 2747
        //   67% -> 4090
        //   92% -> 5616

        // user1
        user1_before.totalPoints == total_before
        user1_before.points == 222
        user1_before.todaysPoints == 222
        user1_before.skillsAchieved == 0
        user1_before.skillsLevel == 0
        user1_before.levelPoints == 222

        user1_before.skills.totalPoints == [111, 222*2, 333*3, 444*4, 555*5]
        user1_before.skills.points == [0, 222, 0, 0, 0]
        user1_before.skills.todaysPoints == [0, 222, 0, 0, 0]

        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && !it.skillId }.points == 222
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == 222
        points_before.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId }.points == 222

        // user2
        user2_before.totalPoints == total_before
        user2_before.points == 222*2 + 333*3 + 444 + 555
        user2_before.todaysPoints == 222*2 + 333*3 + 444 + 555 // 2442
        user2_before.skillsAchieved == 2
        user2_before.skillsLevel == 2
        user2_before.levelPoints == user2_before.points - 1526

        user2_before.skills.totalPoints == [111, 222*2, 333*3, 444*4, 555*5]
        user2_before.skills.points == [0, 222*2, 333*3, 444, 555]
        user2_before.skills.todaysPoints == [0, 222*2, 333*3, 444, 555]

        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && !it.skillId }.points == user2_before.points
        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user2_before.points
        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId }.points == 222*2
        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[2].skillId }.points == 333*3
        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.points == 444
        points_before.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 555

        achievements_before.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && !it.skillId && it.level }).level.sort() == [1, 2]
        achievements_before.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId && it.level }).level.sort() == [1, 2]

        // user 3
        user3_before.totalPoints == total_before
        user3_before.points == 111 + 555*3
        user3_before.todaysPoints == 111 + 555*3 // 1776
        user3_before.skillsAchieved == 1
        user3_before.skillsLevel == 2
        user3_before.levelPoints == user3_before.points - 1526

        user3_before.skills.totalPoints == [111, 222*2, 333*3, 444*4, 555*5]
        user3_before.skills.points == [111, 0, 0, 0, 555*3]
        user3_before.skills.todaysPoints == [111, 0, 0, 0, 555*3]

        points_before.find { it.userId == users[2].userName && it.projectId == p1.projectId && !it.skillId }.points == user3_before.points
        points_before.find { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user3_before.points
        points_before.find { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId }.points == 111
        points_before.find { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 555*3

        achievements_before.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && !it.skillId && it.level }).level.sort() == [1, 2]
        achievements_before.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId && it.level }).level.sort() == [1, 2]


        // After: Level points based on total_after (8745):
        //   10% -> 874
        //   25% -> 2186
        //   45% -> 3935
        //   67% -> 5859
        //   92% -> 8045
        user1_after.totalPoints == total_after
        user1_after.points == 651
        user1_after.todaysPoints == 651
        user1_after.skillsAchieved == 0
        user1_after.skillsLevel == 0
        user1_after.levelPoints == 651

        user1_after.skills.totalPoints == [111, 651*2, 651*3, 651*4, 555*5]
        user1_after.skills.points == [0, 651, 0, 0, 0]
        user1_after.skills.todaysPoints == [0, 651, 0, 0, 0]

        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && !it.skillId }.points == 651
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == 651
        points_after.find { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId }.points == 651

        // user2
        user2_after.totalPoints == total_after
        user2_after.points == 651*2 + 651*3 + 651 + 555
        user2_after.todaysPoints == 651*2 + 651*3 + 651 + 555 // 4461
        user2_after.skillsAchieved == 2
        user2_after.skillsLevel == 3
        user2_after.levelPoints == user2_after.points - 3935

        user2_after.skills.totalPoints == [111, 651*2, 651*3, 651*4, 555*5]
        user2_after.skills.points == [0, 651*2, 651*3, 651, 555]
        user2_after.skills.todaysPoints == [0, 651*2, 651*3, 651, 555]

        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && !it.skillId }.points == user2_after.points
        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user2_after.points
        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[1].skillId }.points == 651*2
        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[2].skillId }.points == 651*3
        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[3].skillId }.points == 651
        points_after.find { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 555

        achievements_after.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId && it.level }).level.sort() == [1, 2, 3]
        achievements_after.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && !it.skillId && it.level }).level.sort() == [1, 2, 3]

        // user 3
        user3_after.totalPoints == total_after
        user3_after.points == 111 + 555*3
        user3_after.todaysPoints == 111 + 555*3 // 1776
        user3_after.skillsAchieved == 1
        user3_after.skillsLevel == 1
        user3_after.levelPoints == user3_after.points - 874

        user3_after.skills.totalPoints == [111, 651*2, 651*3, 651*4, 555*5]
        user3_after.skills.points == [111, 0, 0, 0, 555*3]
        user3_after.skills.todaysPoints == [111, 0, 0, 0, 555*3]

        points_after.find { it.userId == users[2].userName && it.projectId == p1.projectId && !it.skillId }.points == user3_after.points
        points_after.find { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId }.points == user3_after.points
        points_after.find { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == p1skills[0].skillId }.points == 111
        points_after.find { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == p1skills[4].skillId }.points == 555*3

        achievements_after.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && !it.skillId && it.level }).level.sort() == [1]
        achievements_after.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == p1subj1.subjectId && it.level }).level.sort() == [1]
    }

    def "complex batch update across multiple subjects and users"() {
        given:
        def project = createProject(1)

        // Subject A: Basic skills
        def subjA = createSubject(1, 10)
        def skillsA = createSkills(3, 1, 10, 100) // skillA0, A1, A2
        skillsA.eachWithIndex { s, i ->
            s.pointIncrement = 100 * (i + 1)
            s.numPerformToCompletion = 1
            s.pointIncrementInterval = 0
        }
        skillsService.createProjectAndSubjectAndSkills(project, subjA, skillsA)

        // Subject B: Advanced skills
        def subjB = createSubject(1, 20)
        def skillsB = createSkills(3, 1, 20, 200) // skillB0, B1, B2
        skillsB.eachWithIndex { s, i ->
            s.pointIncrement = 200 * (i + 1)
            s.numPerformToCompletion = 1
            s.pointIncrementInterval = 0
        }
        skillsService.createProjectAndSubjectAndSkills(null, subjB, skillsB)

        // 5 Users with different progress patterns
        List<SkillsService> users = getRandomUsers(5).collect { createService(it) }

        // User 0: Only Subject A, partially completed
        users[0].addSkill(skillsA[0])

        // User 1: Subject A fully, Subject B partially
        skillsA.each { users[1].addSkill(it) }
        users[1].addSkill(skillsB[0])

        // User 2: Subject B fully, Subject A partially
        skillsB.each { users[2].addSkill(it) }
        users[2].addSkill(skillsA[0])

        // User 3: Mix of both, heavily invested in high-value skills
        users[3].addSkill(skillsA[2])
        users[3].addSkill(skillsB[2])

        // User 4: No skills yet

        when:
        // Batch update only for skills within Subject A to comply with subject constraints
        // A[0] (100) -> 500, A[1] (200) -> 500
        skillsService.batchUpdateSkills(project.projectId, [
                pointIncrement: 500,
                skills: [skillsA[0].skillId, skillsA[1].skillId]
        ])

        def results = users.collect { u ->
            [
                    name: u.userName,
                    subjA: u.getSubjectSummaryForCurrentUser(project.projectId, subjA.subjectId),
                    subjB: u.getSubjectSummaryForCurrentUser(project.projectId, subjB.subjectId)
            ]
        }
        List<UserPoints> points_after = userPointsRepo.findAll()
        List<UserAchievement> achievements_after = userAchievedRepo.findAll()

        then:
        // User 0: Had A[0](100). Now A[0](500).
        results[0].subjA.points == 500
        results[0].subjB.points == 0
        points_after.find { it.userId == users[0].userName && it.projectId == project.projectId && it.skillId == skillsA[0].skillId }.points == 500

        // User 1: Had A[0](100)+A[1](200)+A[2](300) + B[0](200) = 800
        // Now: A[0](500)+A[1](500)+A[2](300) + B[0](200) = 1500
        results[1].subjA.points == 1300
        results[1].subjB.points == 200
        points_after.find { it.userId == users[1].userName && it.projectId == project.projectId && it.skillId == skillsA[0].skillId }.points == 500
        points_after.find { it.userId == users[1].userName && it.projectId == project.projectId && it.skillId == skillsA[1].skillId }.points == 500

        // User 2: Had B[0](200)+B[1](400)+B[2](600) + A[0](100) = 1300
        // Now: B[0](200)+B[1](400)+B[2](600) + A[0](500) = 1700
        results[2].subjA.points == 500
        results[2].subjB.points == 1200
        points_after.find { it.userId == users[2].userName && it.projectId == project.projectId && it.skillId == skillsA[0].skillId }.points == 500

        // User 3: Had A[2](300) + B[2](600)*2 = 1500. No change (didn't earn A[0] or A[1]).
        results[3].subjA.points == 300
        results[3].subjB.points == 600

        // User 4: Still 0
        results[4].subjA.points == 0
        results[4].subjB.points == 0

        // Validate achievements for User 2 who saw a point increase
        // They should have achievements for Subject A and Project overall
        achievements_after.findAll( { it.userId == users[2].userName && it.projectId == project.projectId && it.skillId == subjA.subjectId && it.level }).size() > 0
        achievements_after.findAll( { it.userId == users[2].userName && it.projectId == project.projectId && !it.skillId && it.level }).size() > 0
    }
}