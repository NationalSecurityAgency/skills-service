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
package skills.intTests.reportSkills


import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.storage.model.UserAchievement
import skills.storage.repos.UserAchievedLevelRepo

class ReportSkills_AchievementsSpecs extends DefaultIntSpec {

    String projId = SkillsFactory.defaultProjId

    @Autowired
    UserAchievedLevelRepo userAchievementRepo

    def setup() {
        skillsService.deleteProjectIfExist(projId)
    }

    def "event date is used for the achievement date"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200

        def badge = SkillsFactory.createBadge(1, 1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge(proj.projectId, badge.badgeId, skills[0].skillId)

        Date date = new Date() - 60
        when:
        def res = skillsService.addSkill([projectId: projId, skillId: skills[0].skillId], "user0", date)
        List<UserAchievement> achievements = userAchievementRepo.findAll()
        then:
        res.body.skillApplied
        res.body.completed.size() == 12
        res.body.completed.findAll { it.type == "Skill" }.collect { it.id } == ["skill1"]
        res.body.completed.findAll { it.type == "Subject" }.collect { "${it.id}:${it.level}" } == ["TestSubject1:1", "TestSubject1:2", "TestSubject1:3", "TestSubject1:4", "TestSubject1:5"]
        res.body.completed.findAll { it.type == "Overall" }.collect { it.level } == [1, 2, 3, 4, 5]
        res.body.completed.findAll { it.type == "Badge" }.collect { it.id } == ["badge1"]

        achievements.size() == 12
        achievements.find { it.skillId == "skill1" }.achievedOn == date

        achievements.each {
            assert it.achievedOn == date
        }
    }

    def "last event's date must be used for the achievement date no matter of reporting order"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 2
        skills[1].pointIncrement = 2000

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date1 = new Date() - 60
        Date date2 = new Date() - 30
        when:
        skillsService.addSkill([projectId: projId, skillId: skills[0].skillId], "user0", date2)
        def res = skillsService.addSkill([projectId: projId, skillId: skills[0].skillId], "user0", date1)
        List<UserAchievement> achievements = userAchievementRepo.findAll()
        then:
        res.body.skillApplied
        res.body.completed.findAll { it.type == "Skill" }.collect { it.id } == [skills[0].skillId]

        achievements.find { it.skillId == skills[0].skillId }.achievedOn == date2
    }


    def "last event's date must be used for the achievement date no matter of reporting order - multiple events and multiple users"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 3
        skills[1].pointIncrement = 200
        skills[1].numPerformToCompletion = 4
        skills[2].pointIncrement = 200
        skills[2].numPerformToCompletion = 2

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date1 = new Date() - 60
        Date date2 = new Date() - 30
        Date date3 = new Date() - 20
        Date date4 = new Date() - 10
        when:
        // user 1 achieves skill in order
        String user1 = "user1"
        skillsService.addSkill([projectId: projId, skillId: skills[0].skillId], user1, date1)
        skillsService.addSkill([projectId: projId, skillId: skills[0].skillId], user1, date2)
        def user1ResSkill1 = skillsService.addSkill([projectId: projId, skillId: skills[0].skillId], user1, date3)

        // user 1 almost achieves skill 2
        skillsService.addSkill([projectId: projId, skillId: skills[1].skillId], user1, date1)
        skillsService.addSkill([projectId: projId, skillId: skills[1].skillId], user1, date1)
        def user1ResSkill2 = skillsService.addSkill([projectId: projId, skillId: skills[1].skillId], user1, date3)

        // user 1 achieves skill 3 in reverse date order
        skillsService.addSkill([projectId: projId, skillId: skills[2].skillId], user1, date2)
        def user1ResSkill3 = skillsService.addSkill([projectId: projId, skillId: skills[2].skillId], user1, date1)

        // user 2 achieves skill 1 with older event date
        String user2 = "user2"
        skillsService.addSkill([projectId: projId, skillId: skills[0].skillId], user2, date4)
        skillsService.addSkill([projectId: projId, skillId: skills[0].skillId], user2, date2)
        def user2ResSkill1 = skillsService.addSkill([projectId: projId, skillId: skills[0].skillId], user2, date1)

        // user 2 achieves skill 1 out of order
        skillsService.addSkill([projectId: projId, skillId: skills[1].skillId], user2, date2)
        skillsService.addSkill([projectId: projId, skillId: skills[1].skillId], user2, date4)
        skillsService.addSkill([projectId: projId, skillId: skills[1].skillId], user2, date1)
        def user2ResSkill2 = skillsService.addSkill([projectId: projId, skillId: skills[1].skillId], user2, date3)

        // user 3 achieves skill 3 out of order
        String user3 = "user3"
        skillsService.addSkill([projectId: projId, skillId: skills[2].skillId], user3, date2)
        def user3ResSkill3 = skillsService.addSkill([projectId: projId, skillId: skills[2].skillId], user3, date1)

        List<UserAchievement> achievements = userAchievementRepo.findAll()
        then:

        // user 1
        user1ResSkill1.body.completed.findAll { it.type == "Skill" }.collect { it.id } == [skills[0].skillId]
        !user1ResSkill2.body.completed.findAll { it.type == "Skill" }
        user1ResSkill3.body.completed.findAll { it.type == "Skill" }.collect { it.id } == [skills[2].skillId]

        achievements.find { it.userId == user1 && it.skillId == skills[0].skillId }.achievedOn == date3
        !achievements.find { it.userId == user1 && it.skillId == skills[1].skillId }
        achievements.find { it.userId == user1 && it.skillId == skills[2].skillId }.achievedOn == date2

        // user 2
        user2ResSkill1.body.completed.findAll { it.type == "Skill" }.collect { it.id } == [skills[0].skillId]
        user2ResSkill2.body.completed.findAll { it.type == "Skill" }.collect { it.id } == [skills[1].skillId]

        achievements.find { it.userId == user2 && it.skillId == skills[0].skillId }.achievedOn == date4
        achievements.find { it.userId == user2 && it.skillId == skills[1].skillId }.achievedOn == date4

        // user 3
        user3ResSkill3.body.completed.findAll { it.type == "Skill" }.collect { it.id } == [skills[2].skillId]

        achievements.find { it.userId == user3 && it.skillId == skills[2].skillId }.achievedOn == date2
    }

    def "achieved date for levels should come from the latest skill event"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2,)
        skills[0].pointIncrement = 200
        skills[1].pointIncrement = 2000 // only skill 1 matters when it comes to levels

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date1 = new Date() - 60
        Date date2 = new Date() - 30
        when:
        def res = skillsService.addSkill([projectId: projId, skillId: skills[0].skillId], "user0", date2)
        def res1 = skillsService.addSkill([projectId: projId, skillId: skills[1].skillId], "user0", date1)

        List<UserAchievement> achievements = userAchievementRepo.findAll()
        then:
        res.body.skillApplied
        res.body.completed.size() == 1
        res.body.completed.findAll { it.type == "Skill" }.collect { it.id } == [skills[0].skillId]

        res1.body.skillApplied
        res1.body.completed.size() == 11
        res1.body.completed.findAll { it.type == "Skill" }.collect { it.id } == [skills[1].skillId]
        res1.body.completed.findAll { it.type == "Subject" }.collect { "${it.id}:${it.level}" } == ["TestSubject1:1", "TestSubject1:2", "TestSubject1:3", "TestSubject1:4", "TestSubject1:5"]
        res1.body.completed.findAll { it.type == "Overall" }.collect { it.level } == [1, 2, 3, 4, 5]

        achievements.size() == 12
        achievements.find { it.skillId == skills[0].skillId }.achievedOn == date2
        achievements.find { it.skillId == skills[1].skillId }.achievedOn == date1

        achievements.each {
            if (it.skillId != skills[1].skillId) {
                assert it.achievedOn == date2
            }
        }
    }


}
