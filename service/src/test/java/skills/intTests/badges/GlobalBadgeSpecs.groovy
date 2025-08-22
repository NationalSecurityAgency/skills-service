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

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.storage.model.UserAchievement
import skills.storage.repos.UserAchievedLevelRepo

import static skills.intTests.utils.SkillsFactory.*

class GlobalBadgeSpecs extends DefaultIntSpec {

    @Autowired
    UserAchievedLevelRepo userAchievedLevelRepo

    def "removing level satisfies global badge for some of the existing users"() {

        def proj = createProject()
        def subj2 = createSubject(1, 2)
        def subj3 = createSubject(1, 3)
        def subj = createSubject()
        def badge = createBadge()
        def badge2 = createBadge(1, 2)


        //subj1 skills
        // 1500 total points
        // 150 for level 1
        // 375 for level 2
        List<Map> skills = createSkills(5, 1, 1, 100)
        List<Map> subj2Skills = createSkills(5, 1, 2, 100)
        List<Map> subj3Skills = createSkills(5, 1, 3, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSkills(skills)
        skillsService.createSkills(subj2Skills)
        skillsService.createSkills(subj3Skills)

        skillsService.createGlobalBadge(badge)
        skillsService.createGlobalBadge(badge2)
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])

        // Adding level 1 and skills[3] to badge 2, the level is achieved but the skill is not so the badge should not be awarded
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge2.badgeId, level: "1")
        skillsService.assignSkillToGlobalBadge(proj.projectId, badge2.badgeId, skills[2].skillId.toString())

        badge.enabled = "true"
        badge2.enabled = "true"
        skillsService.updateGlobalBadge(badge)
        skillsService.updateGlobalBadge(badge2)

        when:
        skillsService.addSkill(['projectId': proj.projectId, skillId: skills[0].skillId], "user1", new Date()).body.completed

        // User that does not get level 1 but does get the skill achievement
        skillsService.addSkill(['projectId': proj.projectId, skillId: skills[0].skillId], "user2", new Date()).body.completed

        // User that gets a different skill and should NOT get the global badge
        skillsService.addSkill(['projectId': proj.projectId, skillId: skills[1].skillId], "user3", new Date()).body.completed
        //triggers level 1
        def addSkillRes = skillsService.addSkill(['projectId': proj.projectId, skillId: skills[1].skillId], "user1", new Date()).body.completed

        def badge_level_before = skillsService.getBadgeSummary("user1", proj.projectId,  badge.badgeId,-1, true)
        skillsService.removeProjectLevelFromGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
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

        def proj = createProject()
        def subj = createSubject()
        def subj2 = createSubject(1, 2)
        def subj3 = createSubject(1, 3)
        def badge = createBadge()


        //subj1 skills
        // 1500 total points
        // 150 for level 1
        // 375 for level 2
        List<Map> skills = createSkills(5, 1, 1, 100)
        List<Map> subj2Skills = createSkills(5, 1, 2, 100)
        List<Map> subj3Skills = createSkills(5, 1, 3, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSkills(skills)
        skillsService.createSkills(subj2Skills)
        skillsService.createSkills(subj3Skills)

        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "1")
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[4].skillId])

        badge.enabled = 'true'
        skillsService.updateGlobalBadge(badge)

        when:
        skillsService.addSkill(['projectId': proj.projectId, skillId: skills[0].skillId], "user1", new Date()).body.completed
        //triggers level 1
        def addSkillRes = skillsService.addSkill(['projectId': proj.projectId, skillId: skills[1].skillId], "user1", new Date()).body.completed
        def badge_level_before_skill_removed = skillsService.getBadgeSummary("user1", proj.projectId,  badge.badgeId,-1, true)

        // Remove the skill requirement so that the user should now qualify for the badge
        skillsService.removeSkillFromGlobalBadge(proj.projectId, badge.badgeId, skills[4].skillId.toString())
        def badge_level_after_skill_removed = skillsService.getBadgeSummary("user1", proj.projectId,  badge.badgeId,-1, true)

        then:
        addSkillRes.find( { it.type == "Overall"}).level == 1
        !badge_level_before_skill_removed.badgeAchieved
        badge_level_after_skill_removed.badgeAchieved

    }


    def "achieving subject level does not satisfy global badge level dependency"(){
        def proj = createProject()

        def subj = createSubject()
        def subj2 = createSubject(1, 2)
        def badge = createBadge()

        //subj1 skills
        List<Map> skills = createSkills(3, 1, 1, 40)
        List<Map> subj2Skills = createSkills(20, 1, 2, 200)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)
        skillsService.createSkills(subj2Skills)

        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "1")

        badge.enabled = 'true'
        skillsService.updateGlobalBadge(badge)

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
        def proj = createProject()

        def subj = createSubject()
        def subj2 = createSubject(1, 2)
        def badge = createBadge()

        //subj1 skills
        List<Map> skills = createSkills(3, 1, 1, 40)
        List<Map> subj2Skills = createSkills(20, 1, 2, 200)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)
        skillsService.createSkills(subj2Skills)
        skillsService.createGlobalBadge(badge)

        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "1")
        skillsService.updateGlobalBadge(badge) // can only enable after initial creation

        badge.enabled = 'true'
        skillsService.updateGlobalBadge(badge)

        when:
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[0].skillId], "user1", new Date())
        skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[1].skillId], "user1", new Date())
        def result = skillsService.addSkill(['projectId': proj.projectId, skillId: subj2Skills[2].skillId], "user1", new Date())

        then:

        result.body.completed.find{ it.type == 'GlobalBadge' }
    }

    def "cannot disable a badge after it has been enabled"(){
        def proj = createProject()
        def subj = createSubject()
        def skills = createSkills(4)
        def badge = createBadge()
        badge.enabled = 'true'

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "1")
        skillsService.updateGlobalBadge(badge) // can only enable after initial creation

        when:
        badge = skillsService.getGlobalBadge(badge.badgeId)
        badge.enabled = 'false'
        skillsService.updateGlobalBadge(badge)

        then:
        SkillsClientException ex = thrown()
        ex.getMessage().contains("Once a Badge has been published, the only allowable value for enabled is [true]")
    }

    def "cannot enable global badge with no skills and no levels"() {
        def proj = createProject()
        def subj = createSubject()
        def skills = createSkills(4)
        def badge = createBadge()
        badge.enabled = 'false'

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.createGlobalBadge(badge)

        when:
        badge = skillsService.getGlobalBadge(badge.badgeId)
        badge.enabled = 'true'
        skillsService.updateGlobalBadge(badge)

        then:
        def ex = thrown(Exception)
    }

    def "enabling badge with only level requirements should only award badge to users who have the requisite level(s)"() {
        def proj = createProject()

        def subj = createSubject()
        def subj2 = createSubject(1, 2)
        def badge = createBadge()

        def badge2 = createBadge()

        def proj2 = createProject(2)
        def proj2subj1 = createSubject(2)
        List<Map> proj2skills = createSkills(4, 2, 1, 50)

        //subj1 skills
        List<Map> skills = createSkills(3, 1, 1, 40)
        List<Map> subj2Skills = createSkills(20, 1, 2, 200)

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
        skillsService.createGlobalBadge(badge2)
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge2.badgeId, level: "2")
        badge2.enabled = 'true'
        skillsService.updateGlobalBadge(badge2)

        badge.enabled = 'false'
        skillsService.createGlobalBadge(badge)

        when:
        List<UserAchievement> achievementsBefore = userAchievedLevelRepo.findAll()
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "5")

        badge.enabled = 'true'
        skillsService.updateGlobalBadge(badge)
        List<UserAchievement> achievementsAfter = userAchievedLevelRepo.findAll()

        def summary = skillsService.getBadgeSummary(user, proj.projectId,  badge.badgeId,-1, true)

        then:
        !achievementsBefore.find( { it.skillId == badge.badgeId})
        !achievementsAfter.find( { it.skillId == badge.badgeId})

        !summary.badgeAchieved
    }


    def "removing imported skill when original is part of the global badge"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1_skills = (1..3).collect {createSkill(1, 1, it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1_skills)
        p1_skills.each {
            skillsService.exportSkillToCatalog(project1.projectId, it.skillId)
        }

        def badge = createBadge()
        badge.enabled = 'true'
        skillsService.createGlobalBadge(badge)
        skillsService.assignSkillToGlobalBadge(projectId: project1.projectId, badgeId: badge.badgeId, skillId: p1_skills[0].skillId)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, [])

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, p1_skills[0].skillId)
        when:
        skillsService.deleteSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: p1_skills[0].skillId])

        def badgeSkills =skillsService.getGlobalBadgeSkills(badge.badgeId)
        then:
        badgeSkills.size() == 1
        badgeSkills.projectId == [project1.projectId]
        badgeSkills.skillId == [p1_skills[0].skillId]
    }

    def "cannot create global badge with same name"() {
        Map badge = [badgeId: 'global1', name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge)
        when:
        Map badge2 = [badgeId: 'global2', name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge2)
        then:
        SkillsClientException ex = thrown()
        ex.message.contains('Badge with name [Test Global Badge 1] already exists!')
    }

    def "cannot create global badge with the same badgeId"() {
        Map badge = [badgeId: 'global1', name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge)
        when:
        Map badge2 = [badgeId: 'global1', name: 'Test Global Badge 2']
        skillsService.createGlobalBadge(badge2)
        then:
        SkillsClientException ex = thrown()
        ex.message.contains('Badge with id [global1] already exists!')
    }

    def "cannot update an existing global badge to have the same badgeId"() {
        Map badge = [badgeId: 'global1', name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge)
        Map badge2 = [badgeId: 'global2', name: 'Test Global Badge 2']
        skillsService.createGlobalBadge(badge2)

        when:
        badge2.badgeId = 'global1'
        skillsService.updateGlobalBadge(badge2, 'global2')
        then:
        SkillsClientException ex = thrown()
        ex.message.contains('Badge with id [global1] already exists!')
    }

    def "cannot add a level of a private project to a global badge"() {
        Map badge = [badgeId: 'global1', name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge)

        def proj = createProject()
        skillsService.createProject(proj)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        when:
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "1")
        then:
        SkillsClientException ex = thrown()
        ex.message.contains("Projects with the private invitation only setting are not allowed to be added to a Global Badge")
    }

    def "cannot add a change a project to private invite only when the project has a level that participates in a global badge"() {
        Map badge = [badgeId: 'global1', name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge)

        def proj = createProject()
        skillsService.createProject(proj)
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "1")

        when:
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        then:
        SkillsClientException ex = thrown()
        ex.message.contains("Projects that participate in global badges cannot enable invite_only setting")
    }

    def "cannot add a skill of a private project to a global badge"() {
        Map badge = [badgeId: 'global1', name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge)

        def proj = createProject()
        def subj = createSubject()
        def skill = createSkill()
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        when:
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skill.skillId])
        then:
        SkillsClientException ex = thrown()
        ex.message.contains("Projects with the private invitation only setting are not allowed to be added to a Global Badge")
    }

    def "cannot add a change a project to private invite only when the project has a skill that participates in a global badge"() {
        Map badge = [badgeId: 'global1', name: 'Test Global Badge 1']
        skillsService.createGlobalBadge(badge)

        def proj = createProject()
        def subj = createSubject()
        def skill = createSkill()
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skill.skillId])

        when:
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        then:
        SkillsClientException ex = thrown()
        ex.message.contains("Projects that participate in global badges cannot enable invite_only setting")
    }
}
