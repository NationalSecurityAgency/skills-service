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
package skills.intTests.badges


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import spock.lang.IgnoreRest

class GlobalBadgeSpecs extends DefaultIntSpec {

    SkillsService supervisorService

    def setup() {
        supervisorService = createSupervisor()
    }

    def "removing level satisfies global badge for some of the existing users"() {

        def proj = SkillsFactory.createProject()
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj3 = SkillsFactory.createSubject(1, 3)
        def subj = SkillsFactory.createSubject()
        def badge = SkillsFactory.createBadge()
        def badge2 = SkillsFactory.createBadge(1, 2)


        //subj1 skills
        // 1500 total points
        // 150 for level 1
        // 375 for level 2
        List<Map> skills = SkillsFactory.createSkills(5, 1, 1, 100)
        List<Map> subj2Skills = SkillsFactory.createSkills(5, 1, 2, 100)
        List<Map> subj3Skills = SkillsFactory.createSkills(5, 1, 3, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSkills(skills)
        skillsService.createSkills(subj2Skills)
        skillsService.createSkills(subj3Skills)

        badge.enabled = 'true'
        badge2.enabled = 'true'
        supervisorService.createGlobalBadge(badge)
        supervisorService.createGlobalBadge(badge2)
        supervisorService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
        supervisorService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])

        // Adding level 1 and skills[3] to badge 2, the level is achieved but the skill is not so the badge should not be awarded
        supervisorService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge2.badgeId, level: "1")
        supervisorService.assignSkillToGlobalBadge(proj.projectId, badge2.badgeId, skills[2].skillId.toString())

        when:
        skillsService.addSkill(['projectId': proj.projectId, skillId: skills[0].skillId], "user1", new Date()).body.completed

        // User that does not get level 1 but does get the skill achievement
        skillsService.addSkill(['projectId': proj.projectId, skillId: skills[0].skillId], "user2", new Date()).body.completed

        // User that gets a different skill and should NOT get the global badge
        skillsService.addSkill(['projectId': proj.projectId, skillId: skills[1].skillId], "user3", new Date()).body.completed
        //triggers level 1
        def addSkillRes = skillsService.addSkill(['projectId': proj.projectId, skillId: skills[1].skillId], "user1", new Date()).body.completed

        def badge_level_before = skillsService.getBadgeSummary("user1", proj.projectId,  badge.badgeId,-1, true)
        supervisorService.removeProjectLevelFromGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
        def badge_level_after = skillsService.getBadgeSummary("user1", proj.projectId,  badge.badgeId,-1, true)
        def badge_summary_user2 = skillsService.getBadgeSummary("user2", proj.projectId,  badge.badgeId,-1, true)
        def badge_summary_user3 = skillsService.getBadgeSummary("user3", proj.projectId,  badge.badgeId,-1, true)
        def badge2_summary_user1 = skillsService.getBadgeSummary("user1", proj.projectId,  badge2.badgeId,-1, true)

        then:
        addSkillRes.find( { it.type == "Overall"}).level == 1
        !badge_level_before.badgeAchieved
        !badge_level_before.dateAchieved

        badge_level_after.badgeAchieved
        badge_level_after.dateAchieved
        badge_summary_user2.badgeAchieved
        badge_summary_user2.dateAchieved
        !badge_summary_user3.badgeAchieved
        !badge_summary_user3.dateAchieved
        !badge2_summary_user1.badgeAchieved
    }

    def "removing skill requirement from badge where level achievement is met should award badge"() {

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj3 = SkillsFactory.createSubject(1, 3)
        def badge = SkillsFactory.createBadge()


        //subj1 skills
        // 1500 total points
        // 150 for level 1
        // 375 for level 2
        List<Map> skills = SkillsFactory.createSkills(5, 1, 1, 100)
        List<Map> subj2Skills = SkillsFactory.createSkills(5, 1, 2, 100)
        List<Map> subj3Skills = SkillsFactory.createSkills(5, 1, 3, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSkills(skills)
        skillsService.createSkills(subj2Skills)
        skillsService.createSkills(subj3Skills)

        badge.enabled = 'true'
        supervisorService.createGlobalBadge(badge)
        supervisorService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "1")
        supervisorService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[4].skillId])

        when:
        skillsService.addSkill(['projectId': proj.projectId, skillId: skills[0].skillId], "user1", new Date()).body.completed
        //triggers level 1
        def addSkillRes = skillsService.addSkill(['projectId': proj.projectId, skillId: skills[1].skillId], "user1", new Date()).body.completed
        def badge_level_before_skill_removed = skillsService.getBadgeSummary("user1", proj.projectId,  badge.badgeId,-1, true)

        // Remove the skill requirement so that the user should now qualify for the badge
        supervisorService.removeSkillFromGlobalBadge(proj.projectId, badge.badgeId, skills[4].skillId.toString())
        def badge_level_after_skill_removed = skillsService.getBadgeSummary("user1", proj.projectId,  badge.badgeId,-1, true)

        then:
        addSkillRes.find( { it.type == "Overall"}).level == 1
        !badge_level_before_skill_removed.badgeAchieved
        badge_level_after_skill_removed.badgeAchieved

    }


    def "achieving subject level does not satisfy global badge level dependency"(){
        def proj = SkillsFactory.createProject()

        def subj = SkillsFactory.createSubject()
        def subj2 = SkillsFactory.createSubject(1, 2)
        def badge = SkillsFactory.createBadge()

        //subj1 skills
        List<Map> skills = SkillsFactory.createSkills(3, 1, 1, 40)
        List<Map> subj2Skills = SkillsFactory.createSkills(20, 1, 2, 200)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)
        skillsService.createSkills(subj2Skills)
        badge.enabled = 'true'
        supervisorService.createGlobalBadge(badge)

        supervisorService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "1")

        when:
        def res1 = skillsService.addSkill(['projectId': proj.projectId, skillId: skills[0].skillId], "user1", new Date())
        def res2 = skillsService.addSkill(['projectId': proj.projectId, skillId: skills[1].skillId], "user1", new Date())
        def res3 = skillsService.addSkill(['projectId': proj.projectId, skillId: skills[2].skillId], "user1", new Date())

        then:

        res1.body.completed.find{ it.type == 'Subject' && it.level == 1}
        !res1.body.completed.find{ it.type == 'GlobalBadge' }
        !res2.body.completed.find{ it.type == 'GlobalBadge' }
        !res3.body.completed.find{ it.type == 'GlobalBadge' }
    }

    def "achieving project level satisfies global badge level dependency"(){
        def proj = SkillsFactory.createProject()

        def subj = SkillsFactory.createSubject()
        def subj2 = SkillsFactory.createSubject(1, 2)
        def badge = SkillsFactory.createBadge()

        //subj1 skills
        List<Map> skills = SkillsFactory.createSkills(3, 1, 1, 40)
        List<Map> subj2Skills = SkillsFactory.createSkills(20, 1, 2, 200)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)
        skillsService.createSkills(subj2Skills)
        badge.enabled = 'true'
        supervisorService.createGlobalBadge(badge)

        supervisorService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "1")

        when:
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[0].skillId], "user1", new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[1].skillId], "user1", new Date())
        def result = skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[2].skillId], "user1", new Date())

        then:

        result.body.completed.find{ it.type == 'GlobalBadge' }
    }

    def "cannot disable a badge after it has been enabled"(){
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        def badge = SkillsFactory.createBadge()
        badge.enabled = 'true'

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        supervisorService.createGlobalBadge(badge)

        when:
        badge = supervisorService.getGlobalBadge(badge.badgeId)
        badge.enabled = 'false'
        supervisorService.createGlobalBadge(badge)

        then:
        SkillsClientException ex = thrown()
        ex.getMessage().contains("Once a Badge has been published, the only allowable value for enabled is [true]")
    }

    def "cannot enable global badge with no skills and no levels"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        def badge = SkillsFactory.createBadge()
        badge.enabled = 'false'

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        supervisorService.createGlobalBadge(badge)

        when:
        badge = supervisorService.getGlobalBadge(badge.badgeId)
        badge.enabled = 'true'
        supervisorService.createGlobalBadge(badge)

        then:
        def ex = thrown(Exception)
    }

    def "enabling badge with only level requirements should only award badge to users who have the requisite level(s)"() {
        def proj = SkillsFactory.createProject()

        def subj = SkillsFactory.createSubject()
        def subj2 = SkillsFactory.createSubject(1, 2)
        def badge = SkillsFactory.createBadge()

        def badge2 = SkillsFactory.createBadge()

        def proj2 = SkillsFactory.createProject(2)
        def proj2subj1 = SkillsFactory.createSubject(2)
        List<Map> proj2skills = SkillsFactory.createSkills(4, 2, 1, 50)

        //subj1 skills
        List<Map> skills = SkillsFactory.createSkills(3, 1, 1, 40)
        List<Map> subj2Skills = SkillsFactory.createSkills(20, 1, 2, 200)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)
        skillsService.createSkills(subj2Skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2subj1)
        skillsService.createSkills(proj2skills)


        //create user
        def users = getRandomUsers(6)
        String user = users[0]
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[0].skillId], user, new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[1].skillId], user, new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[2].skillId], user, new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[3].skillId], user, new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[4].skillId], user, new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[5].skillId], user, new Date())


        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[0].skillId], users[1], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[1].skillId], users[1], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[2].skillId], users[1], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[3].skillId], users[1], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[4].skillId], users[1], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[5].skillId], users[1], new Date())

        skillsService.addSkill(['projectId': proj2.projectId, skillId: proj2skills[0].skillId], users[1], new Date())
        skillsService.addSkill(['projectId': proj2.projectId, skillId: proj2skills[1].skillId], users[1], new Date())
        skillsService.addSkill(['projectId': proj2.projectId, skillId: proj2skills[2].skillId], users[1], new Date())
        skillsService.addSkill(['projectId': proj2.projectId, skillId: proj2skills[3].skillId], users[1], new Date())

        skillsService.addSkill(['projectId': proj2.projectId, skillId: proj2skills[0].skillId], users[2], new Date())
        skillsService.addSkill(['projectId': proj2.projectId, skillId: proj2skills[1].skillId], users[2], new Date())

        skillsService.addSkill(['projectId': proj2.projectId, skillId: proj2skills[0].skillId], users[3], new Date())

        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[0].skillId], users[4], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[1].skillId], users[4], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[2].skillId], users[4], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[3].skillId], users[4], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[4].skillId], users[4], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[5].skillId], users[4], new Date())

        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[0].skillId], users[5], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[1].skillId], users[5], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[2].skillId], users[5], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[3].skillId], users[5], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[4].skillId], users[5], new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[5].skillId], users[5], new Date())

        badge2.badgeId = 'global2'
        badge2.name = 'Global 2'
        badge2.enabled = 'false'
        supervisorService.createGlobalBadge(badge2)
        supervisorService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge2.badgeId, level: "2")
        badge2.enabled = 'true'
        supervisorService.createGlobalBadge(badge2)

        badge.enabled = 'false'
        supervisorService.createGlobalBadge(badge)
        def summaryBeforeEnabling = skillsService.getBadgeSummary(user, proj.projectId,  badge.badgeId,-1, true)

        supervisorService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "5")


        when:

        badge.enabled = 'true'
        supervisorService.createGlobalBadge(badge)

        def summary = skillsService.getBadgeSummary(user, proj.projectId,  badge.badgeId,-1, true)

        then:
        !summaryBeforeEnabling.badgeAchieved
        !summary.badgeAchieved
    }

    def "changing project level satisfies global badge level dependency"(){
        def proj = SkillsFactory.createProject()

        def subj = SkillsFactory.createSubject()
        def subj2 = SkillsFactory.createSubject(1, 2)
        def badge = SkillsFactory.createBadge()

        //subj1 skills
        List<Map> skills = SkillsFactory.createSkills(3, 1, 1, 40)
        List<Map> subj2Skills = SkillsFactory.createSkills(20, 1, 2, 200)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)
        skillsService.createSkills(subj2Skills)
        badge.enabled = 'true'
        supervisorService.createGlobalBadge(badge)

        supervisorService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "5")

        when:
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[0].skillId], "user1", new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[1].skillId], "user1", new Date())
        def result = skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[2].skillId], "user1", new Date())

        supervisorService.changeProjectLevelOnGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, currentLevel: "5", newLevel: "1"])

        def badgeSummary = skillsService.getBadgeSummary("user1", proj.projectId, badge.badgeId, -1, true)
        println badgeSummary
        then:
        !result.body.completed.find{ it.type == 'GlobalBadge' }
        badgeSummary.projectLevelsAndSkillsSummaries[0].projectLevel.requiredLevel == 1
        badgeSummary.badgeAchieved == true
    }
}
