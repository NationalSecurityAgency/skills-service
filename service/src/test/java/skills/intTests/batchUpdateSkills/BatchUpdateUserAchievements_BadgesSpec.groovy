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


import org.apache.commons.lang3.tuple.Pair
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsService
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints

import static skills.intTests.utils.SkillsFactory.*

class BatchUpdateUserAchievements_BadgesSpec extends DefaultIntSpec {

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

    def "badges are achieved when numPerformToCompletion is decreased - pointIncrement is increased too but has no real effect on badge"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100, 3)
        p1skills.each { it.pointIncrementInterval = 0 }
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22, 2)
        p1skillsSubj2.each { it.pointIncrementInterval = 0 }
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        // two badges
        def badge1 = createBadge(1, 11)
        skillsService.createBadge(badge1)
        def badge2 = createBadge(1, 12)
        skillsService.createBadge(badge2)

        skillsService.assignSkillsToBadge(p1.projectId, badge1.badgeId,
                [p1skills[0].skillId, p1skills[1].skillId, p1skills[2].skillId, p1skillsSubj2[0].skillId, p1skillsSubj2[1].skillId ])
        skillsService.assignSkillsToBadge(p1.projectId, badge2.badgeId,
                [p1skills[1].skillId, p1skills[2].skillId, p1skills[3].skillId, p1skillsSubj2[1].skillId, p1skillsSubj2[2].skillId ])

        badge1.enabled = true
        badge2.enabled = true
        skillsService.updateBadge(badge1, badge1.badgeId)
        skillsService.updateBadge(badge2, badge2.badgeId)

        Closure reportSkills = { SkillsService user, List<Pair<Object, Integer>> skillsAndNumToReport ->
            skillsAndNumToReport.each {
                def skill = it.key
                it.value.times {
                    user.addSkill(skill)
                }
            }
        }

        // user 0 - almost achieved badge 1, achieved badge 2
        reportSkills(users[0], [
                Pair.of(p1skills[0], 2), Pair.of(p1skills[1], 3),
                Pair.of(p1skills[2], 3), Pair.of(p1skills[3], 3),
                Pair.of(p1skillsSubj2[0], 2), Pair.of(p1skillsSubj2[1], 2), Pair.of(p1skillsSubj2[2], 2)
        ])

        // user 1 - achieved both badges
        reportSkills(users[1], [
                Pair.of(p1skills[0], 3), Pair.of(p1skills[1], 3),
                Pair.of(p1skills[2], 3),Pair.of(p1skills[3], 3),
                Pair.of(p1skillsSubj2[0], 2), Pair.of(p1skillsSubj2[1], 2), Pair.of(p1skillsSubj2[2], 2)
        ])

        // user 2 - almost achieved badge 1, achieved badge 2
        reportSkills(users[2], [
                Pair.of(p1skills[0], 2), Pair.of(p1skills[1], 3),
                Pair.of(p1skills[2], 3),Pair.of(p1skills[3], 3),
                Pair.of(p1skillsSubj2[0], 1), Pair.of(p1skillsSubj2[1], 2), Pair.of(p1skillsSubj2[2], 2)
        ])

        // user 3 - almost achieved badge 1 and badge 2
        reportSkills(users[3], [
                Pair.of(p1skills[0], 2), Pair.of(p1skills[1], 2),
                Pair.of(p1skills[2], 3),Pair.of(p1skills[3], 3),
                Pair.of(p1skillsSubj2[0], 2), Pair.of(p1skillsSubj2[1], 2), Pair.of(p1skillsSubj2[2], 2)
        ])

        // user 4 - not achieved badge 1 and badge 2
        reportSkills(users[4], [
                Pair.of(p1skills[0], 2), Pair.of(p1skills[1], 2),
                Pair.of(p1skills[2], 3),Pair.of(p1skills[3], 3),
                Pair.of(p1skillsSubj2[0], 2), Pair.of(p1skillsSubj2[1], 1), Pair.of(p1skillsSubj2[2], 2)
        ])


        when:
        def u0_before = users[0].getBadgesSummary(null, p1.projectId)
        def u1_before = users[1].getBadgesSummary(null, p1.projectId)
        def u2_before = users[2].getBadgesSummary(null, p1.projectId)
        def u3_before = users[3].getBadgesSummary(null, p1.projectId)
        def u4_before = users[4].getBadgesSummary(null, p1.projectId)
        List<UserAchievement> achievements_before = userAchievedRepo.findAll()
        int newPntIncr = 222
        int newNumPerformToCompletion = 1
        skillsService.batchUpdateSkills(p1.projectId, [
                pointIncrement        : newPntIncr,
                numPerformToCompletion: newNumPerformToCompletion,
                skills                : [p1skills[0].skillId, p1skills[1].skillId]
        ])
        def u0_after = users[0].getBadgesSummary(null, p1.projectId)
        def u1_after = users[1].getBadgesSummary(null, p1.projectId)
        def u2_after = users[2].getBadgesSummary(null, p1.projectId)
        def u3_after = users[3].getBadgesSummary(null, p1.projectId)
        def u4_after = users[4].getBadgesSummary(null, p1.projectId)
        List<UserAchievement> achievements_after = userAchievedRepo.findAll()
        then:
        u0_before.find { it.badgeId == badge1.badgeId }.badgeAchieved == false
        u0_before.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 4
        u0_before.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        u0_before.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 5
        u0_after.find { it.badgeId == badge1.badgeId }.badgeAchieved == true
        u0_after.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 5
        u0_after.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        u0_after.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 5
        !achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })
        achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })

        u1_before.find { it.badgeId == badge1.badgeId }.badgeAchieved == true
        u1_before.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 5
        u1_before.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        u1_before.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 5
        u1_after.find { it.badgeId == badge1.badgeId }.badgeAchieved == true
        u1_after.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 5
        u1_after.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        u1_after.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 5
        achievements_before.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        achievements_after.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        achievements_before.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })
        achievements_after.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })

        u2_before.find { it.badgeId == badge1.badgeId }.badgeAchieved == false
        u2_before.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 3
        u2_before.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        u2_before.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 5
        u2_after.find { it.badgeId == badge1.badgeId }.badgeAchieved == false
        u2_after.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 4
        u2_after.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        u2_after.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 5
        !achievements_before.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        !achievements_after.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        achievements_before.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })
        achievements_after.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })

        u3_before.find { it.badgeId == badge1.badgeId }.badgeAchieved == false
        u3_before.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 3
        u3_before.find { it.badgeId == badge2.badgeId }.badgeAchieved == false
        u3_before.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 4
        u3_after.find { it.badgeId == badge1.badgeId }.badgeAchieved == true
        u3_after.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 5
        u3_after.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        u3_after.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 5
        !achievements_before.findAll( { it.userId == users[3].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        achievements_after.findAll( { it.userId == users[3].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        !achievements_before.findAll( { it.userId == users[3].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })
        achievements_after.findAll( { it.userId == users[3].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })

        u4_before.find { it.badgeId == badge1.badgeId }.badgeAchieved == false
        u4_before.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 2
        u4_before.find { it.badgeId == badge2.badgeId }.badgeAchieved == false
        u4_before.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 3
        u4_after.find { it.badgeId == badge1.badgeId }.badgeAchieved == false
        u4_after.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 4
        u4_after.find { it.badgeId == badge2.badgeId }.badgeAchieved == false
        u4_after.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 4
        !achievements_before.findAll( { it.userId == users[4].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        !achievements_after.findAll( { it.userId == users[4].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        !achievements_before.findAll( { it.userId == users[4].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })
        !achievements_after.findAll( { it.userId == users[4].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })
    }

    def "badges are retained when numPerformToCompletion is increased"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100, 3)
        p1skills.each { it.pointIncrementInterval = 0 }
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22, 2)
        p1skillsSubj2.each { it.pointIncrementInterval = 0 }
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        // two badges
        def badge1 = createBadge(1, 11)
        skillsService.createBadge(badge1)
        def badge2 = createBadge(1, 12)
        skillsService.createBadge(badge2)

        skillsService.assignSkillsToBadge(p1.projectId, badge1.badgeId,
                [p1skills[0].skillId, p1skills[1].skillId, p1skills[2].skillId, p1skillsSubj2[0].skillId, p1skillsSubj2[1].skillId ])
        skillsService.assignSkillsToBadge(p1.projectId, badge2.badgeId,
                [p1skills[1].skillId, p1skills[2].skillId, p1skills[3].skillId, p1skillsSubj2[1].skillId, p1skillsSubj2[2].skillId ])

        badge1.enabled = true
        badge2.enabled = true
        skillsService.updateBadge(badge1, badge1.badgeId)
        skillsService.updateBadge(badge2, badge2.badgeId)

        Closure reportSkills = { SkillsService user, List<Pair<Object, Integer>> skillsAndNumToReport ->
            skillsAndNumToReport.each {
                def skill = it.key
                it.value.times {
                    user.addSkill(skill)
                }
            }
        }

        // user 0 - almost achieved badge 1, achieved badge 2
        reportSkills(users[0], [
                Pair.of(p1skills[0], 2), Pair.of(p1skills[1], 3),
                Pair.of(p1skills[2], 3), Pair.of(p1skills[3], 3),
                Pair.of(p1skillsSubj2[0], 2), Pair.of(p1skillsSubj2[1], 2), Pair.of(p1skillsSubj2[2], 2)
        ])

        // user 1 - achieved both badges
        reportSkills(users[1], [
                Pair.of(p1skills[0], 3), Pair.of(p1skills[1], 3),
                Pair.of(p1skills[2], 3),Pair.of(p1skills[3], 3),
                Pair.of(p1skillsSubj2[0], 2), Pair.of(p1skillsSubj2[1], 2), Pair.of(p1skillsSubj2[2], 2)
        ])

        // user 2 - almost achieved badge 1, achieved badge 2
        reportSkills(users[2], [
                Pair.of(p1skills[0], 2), Pair.of(p1skills[1], 3),
                Pair.of(p1skills[2], 3),Pair.of(p1skills[3], 3),
                Pair.of(p1skillsSubj2[0], 1), Pair.of(p1skillsSubj2[1], 2), Pair.of(p1skillsSubj2[2], 2)
        ])

        // user 3 - almost achieved badge 1 and badge 2
        reportSkills(users[3], [
                Pair.of(p1skills[0], 2), Pair.of(p1skills[1], 2),
                Pair.of(p1skills[2], 3),Pair.of(p1skills[3], 3),
                Pair.of(p1skillsSubj2[0], 2), Pair.of(p1skillsSubj2[1], 2), Pair.of(p1skillsSubj2[2], 2)
        ])

        // user 4 - not achieved badge 1 and badge 2
        reportSkills(users[4], [
                Pair.of(p1skills[0], 2), Pair.of(p1skills[1], 2),
                Pair.of(p1skills[2], 3),Pair.of(p1skills[3], 3),
                Pair.of(p1skillsSubj2[0], 2), Pair.of(p1skillsSubj2[1], 1), Pair.of(p1skillsSubj2[2], 2)
        ])


        when:
        def u0_before = users[0].getBadgesSummary(null, p1.projectId)
        def u1_before = users[1].getBadgesSummary(null, p1.projectId)
        def u2_before = users[2].getBadgesSummary(null, p1.projectId)
        def u3_before = users[3].getBadgesSummary(null, p1.projectId)
        def u4_before = users[4].getBadgesSummary(null, p1.projectId)
        List<UserAchievement> achievements_before = userAchievedRepo.findAll()
        int newNumPerformToCompletion = 5
        skillsService.batchUpdateSkills(p1.projectId, [
                numPerformToCompletion: newNumPerformToCompletion,
                skills                : [p1skills[0].skillId, p1skills[1].skillId]
        ])
        def u0_after = users[0].getBadgesSummary(null, p1.projectId)
        def u1_after = users[1].getBadgesSummary(null, p1.projectId)
        def u2_after = users[2].getBadgesSummary(null, p1.projectId)
        def u3_after = users[3].getBadgesSummary(null, p1.projectId)
        def u4_after = users[4].getBadgesSummary(null, p1.projectId)
        List<UserAchievement> achievements_after = userAchievedRepo.findAll()
        then:
        u0_before.find { it.badgeId == badge1.badgeId }.badgeAchieved == false
        u0_before.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 4
        u0_before.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        u0_before.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 5
        u0_after.find { it.badgeId == badge1.badgeId }.badgeAchieved == false
        u0_after.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 3
        u0_after.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        u0_after.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 4
        !achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        !achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        achievements_before.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })
        achievements_after.findAll( { it.userId == users[0].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })

        u1_before.find { it.badgeId == badge1.badgeId }.badgeAchieved == true
        u1_before.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 5
        u1_before.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        u1_before.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 5
        u1_after.find { it.badgeId == badge1.badgeId }.badgeAchieved == true
        u1_after.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 3
        u1_after.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        u1_after.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 4
        achievements_before.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        achievements_after.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        achievements_before.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })
        achievements_after.findAll( { it.userId == users[1].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })

        u2_before.find { it.badgeId == badge1.badgeId }.badgeAchieved == false
        u2_before.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 3
        u2_before.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        u2_before.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 5
        u2_after.find { it.badgeId == badge1.badgeId }.badgeAchieved == false
        u2_after.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 2
        u2_after.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        u2_after.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 4
        !achievements_before.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        !achievements_after.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        achievements_before.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })
        achievements_after.findAll( { it.userId == users[2].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })

        u3_before.find { it.badgeId == badge1.badgeId }.badgeAchieved == false
        u3_before.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 3
        u3_before.find { it.badgeId == badge2.badgeId }.badgeAchieved == false
        u3_before.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 4
        u3_after.find { it.badgeId == badge1.badgeId }.badgeAchieved == false
        u3_after.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 3
        u3_after.find { it.badgeId == badge2.badgeId }.badgeAchieved == false
        u3_after.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 4
        !achievements_before.findAll( { it.userId == users[3].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        !achievements_after.findAll( { it.userId == users[3].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        !achievements_before.findAll( { it.userId == users[3].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })
        !achievements_after.findAll( { it.userId == users[3].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })

        u4_before.find { it.badgeId == badge1.badgeId }.badgeAchieved == false
        u4_before.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 2
        u4_before.find { it.badgeId == badge2.badgeId }.badgeAchieved == false
        u4_before.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 3
        u4_after.find { it.badgeId == badge1.badgeId }.badgeAchieved == false
        u4_after.find { it.badgeId == badge1.badgeId }.numSkillsAchieved == 2
        u4_after.find { it.badgeId == badge2.badgeId }.badgeAchieved == false
        u4_after.find { it.badgeId == badge2.badgeId }.numSkillsAchieved == 3
        !achievements_before.findAll( { it.userId == users[4].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        !achievements_after.findAll( { it.userId == users[4].userName && it.projectId == p1.projectId && it.skillId == badge1.badgeId })
        !achievements_before.findAll( { it.userId == users[4].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })
        !achievements_after.findAll( { it.userId == users[4].userName && it.projectId == p1.projectId && it.skillId == badge2.badgeId })
    }

}