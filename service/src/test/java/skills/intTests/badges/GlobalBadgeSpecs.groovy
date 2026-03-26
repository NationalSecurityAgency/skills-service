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

import groovy.json.JsonOutput
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.UserAchievement
import skills.storage.repos.UserAchievedLevelRepo
import spock.lang.IgnoreIf

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

    def 'global badge available skills includes regular skills and group skills'() {
        def proj = createProject()
        def subj = createSubject()
        def skills = createSkills(3)
        def skillsGroup = createSkillsGroup(1, 1, 4)
        def regularSkill = createSkill(1, 1, 5)
        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createGlobalBadge(badge)
        skillsService.createSkill(regularSkill)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        def availableSkills = skillsService.getAvailableSkillsForGlobalBadge(badgeId, "")

        then:
        availableSkills
        availableSkills.suggestedSkills.size() == 4
        availableSkills.suggestedSkills.find { it.skillId == skills[0].skillId }
        availableSkills.suggestedSkills.find { it.skillId == skills[1].skillId }
        availableSkills.suggestedSkills.find { it.skillId == skills[2].skillId }
        availableSkills.suggestedSkills.find { it.skillId == regularSkill.skillId }
    }

    def "retrieve users for global badge"() {

        def proj = createProject()
        def subj2 = createSubject(1, 2)
        def subj3 = createSubject(1, 3)
        def subj = createSubject()
        def badge = createBadge()

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
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])

        badge.enabled = "true"
        skillsService.updateGlobalBadge(badge)

        List<SkillsService> users = getRandomUsers(3).collect { createService(it)}

        when:
        assert users[0].addSkill(skills[0]).body.skillApplied

        // User that does not get level 1 but does get the skill achievement
        assert users[1].addSkill(skills[0]).body.skillApplied

        // User that gets a different skill and should NOT get the global badge
        assert users[2].addSkill(skills[1]).body.skillApplied

        //triggers level 1
        assert users[0].addSkill(skills[1]).body.completed.find { it.id == "OVERALL"}.level == 1

        def badgeUsers = skillsService.getGlobalBadgeUsers(badge.badgeId, 10, 1, 'totalProgress', false)

        then:
        badgeUsers
        badgeUsers.count == 3
        badgeUsers.totalCount == 3
        badgeUsers.totalPoints == 1
        badgeUsers.totalLevels == 3
        badgeUsers.data[0].userId == users[0].userName
        badgeUsers.data[0].skillsAchieved == 1
        badgeUsers.data[0].numLevelsAchieved == 1
        badgeUsers.data[0].totalProgress == 2
        badgeUsers.data[1].userId == users[1].userName
        badgeUsers.data[1].skillsAchieved == 1
        badgeUsers.data[1].numLevelsAchieved == 0
        badgeUsers.data[1].totalProgress == 1
        badgeUsers.data[2].userId == users[2].userName
        badgeUsers.data[2].skillsAchieved == 0
        badgeUsers.data[2].numLevelsAchieved == 0
        badgeUsers.data[2].totalProgress == 0
    }

    def "get total levels for badge with multiple projects"() {

        def proj = createProject()
        def subj2 = createSubject(1, 2)
        def subj3 = createSubject(1, 3)
        def subj = createSubject()
        def proj2 = createProject(2)
        def proj2subj = createSubject(2, 1)
        def proj2subj2 = createSubject(2, 2)
        def badge = createBadge()

        List<Map> skills = createSkills(5, 1, 1, 100)
        List<Map> subj2Skills = createSkills(5, 1, 2, 100)
        List<Map> subj3Skills = createSkills(5, 1, 3, 100)
        List<Map> proj2skills = createSkills(5, 2, 1, 100)
        List<Map> proj2subj2Skills = createSkills(5, 2, 2, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSkills(skills)
        skillsService.createSkills(subj2Skills)
        skillsService.createSkills(subj3Skills)
        skillsService.createProject(proj2)
        skillsService.createSubject(proj2subj)
        skillsService.createSubject(proj2subj2)
        skillsService.createSkills(proj2skills)
        skillsService.createSkills(proj2subj2Skills)

        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "2")
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj2.projectId, badgeId: badge.badgeId, level: "1")
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])

        badge.enabled = "true"
        skillsService.updateGlobalBadge(badge)

        List<SkillsService> users = getRandomUsers(1).collect { createService(it)}

        when:
        for(def x = 0; x < 5; x++) {
            assert users[0].addSkill(skills[x]).body.skillApplied
            assert users[0].addSkill(subj2Skills[x]).body.skillApplied
            assert users[0].addSkill(subj3Skills[x]).body.skillApplied
            assert users[0].addSkill(proj2skills[x]).body.skillApplied
            assert users[0].addSkill(proj2subj2Skills[x]).body.skillApplied
        }

        def badgeUsers = skillsService.getGlobalBadgeUsers(badge.badgeId)

        then:
        badgeUsers
        badgeUsers.count == 1
        badgeUsers.totalCount == 1
        badgeUsers.totalPoints == 1
        badgeUsers.totalLevels == 3
        badgeUsers.data[0].skillsAchieved == 1
        badgeUsers.data[0].numLevelsAchieved == badgeUsers.totalLevels
        badgeUsers.data[0].totalProgress == 4 // 3 levels + 1 skill
        badgeUsers.data[0].userId == users[0].userName
    }

    def "sort by total value with paging"() {

        def proj = createProject()
        def subj2 = createSubject(1, 2)
        def subj3 = createSubject(1, 3)
        def subj = createSubject()
        def badge = createBadge()

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
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[1].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[2].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: subj2Skills[0].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: subj2Skills[1].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: subj2Skills[2].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: subj3Skills[0].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: subj3Skills[1].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: subj3Skills[2].skillId])

        badge.enabled = "true"
        skillsService.updateGlobalBadge(badge)

        List<SkillsService> users = getRandomUsers(7).collect { createService(it)}

        when:
        for(def x = 0; x < 5; x++) {
            assert users[0].addSkill(skills[x]).body.skillApplied
            assert users[0].addSkill(subj2Skills[x]).body.skillApplied
            assert users[0].addSkill(subj3Skills[x]).body.skillApplied
            assert users[1].addSkill(skills[x]).body.skillApplied
            assert users[1].addSkill(subj2Skills[x]).body.skillApplied
            assert users[2].addSkill(skills[x]).body.skillApplied
        }

        assert users[2].addSkill(subj2Skills[0]).body.skillApplied
        assert users[3].addSkill(skills[0]).body.skillApplied
        assert users[3].addSkill(skills[1]).body.skillApplied
        assert users[3].addSkill(skills[2]).body.skillApplied
        assert users[3].addSkill(skills[3]).body.skillApplied
        assert users[4].addSkill(skills[0]).body.skillApplied
        assert users[4].addSkill(skills[1]).body.skillApplied
        assert users[4].addSkill(skills[2]).body.skillApplied
        assert users[5].addSkill(skills[0]).body.skillApplied
        assert users[5].addSkill(skills[1]).body.skillApplied
        assert users[6].addSkill(skills[0]).body.skillApplied

        def badgeUsers = skillsService.getGlobalBadgeUsers(badge.badgeId, 5, 1, 'totalProgress', false)
        def badgeUsersPg2 = skillsService.getGlobalBadgeUsers(badge.badgeId, 5, 2, 'totalProgress', false)
        def badgeUsersDesc = skillsService.getGlobalBadgeUsers(badge.badgeId, 5, 1, 'totalProgress', true)
        def badgeUsersDescPg2 = skillsService.getGlobalBadgeUsers(badge.badgeId, 5, 2, 'totalProgress', true)

        then:
        badgeUsers
        badgeUsersPg2
        badgeUsersDesc
        badgeUsersDescPg2
        badgeUsers.count == 7
        badgeUsersPg2.count == 7
        badgeUsers.data.userId == users[0..4].userName
        badgeUsersPg2.data.userId == users[5..6].userName
        badgeUsersDesc.count == 7
        badgeUsersDescPg2.count == 7
        badgeUsersDesc.data.userId == users[2..6].reverse().userName
        badgeUsersDescPg2.data.userId == users[0..1].reverse().userName
    }

    def "sort by date with paging"() {

        def proj = createProject()
        def subj2 = createSubject(1, 2)
        def subj3 = createSubject(1, 3)
        def subj = createSubject()
        def badge = createBadge()

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
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[1].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[2].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: subj2Skills[0].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: subj2Skills[1].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: subj2Skills[2].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: subj3Skills[0].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: subj3Skills[1].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: subj3Skills[2].skillId])

        badge.enabled = "true"
        skillsService.updateGlobalBadge(badge)

        List<SkillsService> users = getRandomUsers(7).collect { createService(it)}

        when:
        for(def x = 0; x < 7; x++) {
            assert users[x].addSkill(skills[0]).body.skillApplied
            sleep(10)
        }

        def badgeUsers = skillsService.getGlobalBadgeUsers(badge.badgeId, 5, 1, 'lastUpdated', true)
        def badgeUsersPg2 = skillsService.getGlobalBadgeUsers(badge.badgeId, 5, 2, 'lastUpdated', true)
        def badgeUsersDesc = skillsService.getGlobalBadgeUsers(badge.badgeId, 5, 1, 'lastUpdated', false)
        def badgeUsersDescPg2 = skillsService.getGlobalBadgeUsers(badge.badgeId, 5, 2, 'lastUpdated', false)

        then:
        badgeUsers
        badgeUsersPg2
        badgeUsersDesc
        badgeUsersDescPg2
        badgeUsers.count == 7
        badgeUsersPg2.count == 7
        badgeUsers.data.userId == users[0..4].userName
        badgeUsersPg2.data.userId == users[5..6].userName
        badgeUsersDesc.count == 7
        badgeUsersDescPg2.count == 7
        badgeUsersDesc.data.userId == users[2..6].reverse().userName
        badgeUsersDescPg2.data.userId == users[0..1].reverse().userName
    }

    def "get badge users with combo of skills, levels and both"() {

        def badge = createBadge()
        skillsService.createGlobalBadge(badge)

        def projectIds = []
        Map<String, List> skillIds = new HashMap<String, List>()

        for(def x = 1; x <= 5; x++) {
            def proj = createProject(x)
            def subj1 = createSubject(x, 1)
            def subj2 = createSubject(x, 2)
            def subj3 = createSubject(x, 3)
            List<Map> skills = createSkills(5, x, 1, 100)

            projectIds.push(proj.projectId)

            skillsService.createProject(proj)
            skillsService.createSubject(subj1)
            skillsService.createSubject(subj2)
            skillsService.createSubject(subj3)
            skillsService.createSkills(skills)

            skillIds[proj.projectId] = skills

        }

        def proj1Id = projectIds[0]
        def proj2Id = projectIds[1]
        def proj3Id = projectIds[2]
        def proj4Id = projectIds[3]
        def proj5Id = projectIds[4]
        def proj1Skills = skillIds[proj1Id]
        def proj2Skills = skillIds[proj2Id]
        def proj3Skills = skillIds[proj3Id]
        def proj4Skills = skillIds[proj4Id]
        def proj5Skills = skillIds[proj5Id]

        skillsService.assignSkillToGlobalBadge([projectId: projectIds[0], badgeId: badge.badgeId, skillId: proj1Skills[0].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: projectIds[0], badgeId: badge.badgeId, skillId: proj1Skills[1].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: projectIds[1], badgeId: badge.badgeId, skillId: proj2Skills[0].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: projectIds[1], badgeId: badge.badgeId, skillId: proj2Skills[1].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: projectIds[1], badgeId: badge.badgeId, skillId: proj2Skills[2].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: projectIds[1], badgeId: badge.badgeId, skillId: proj2Skills[3].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: projectIds[2], badgeId: badge.badgeId, skillId: proj3Skills[0].skillId])
        skillsService.assignSkillToGlobalBadge([projectId: projectIds[2], badgeId: badge.badgeId, skillId: proj3Skills[1].skillId])
        skillsService.assignProjectLevelToGlobalBadge(projectId: projectIds[2], badgeId: badge.badgeId, level: "3")
        skillsService.assignProjectLevelToGlobalBadge(projectId: projectIds[3], badgeId: badge.badgeId, level: "2")
        skillsService.assignProjectLevelToGlobalBadge(projectId: projectIds[4], badgeId: badge.badgeId, level: "5")

        badge.enabled = "true"
        skillsService.updateGlobalBadge(badge)

        List<SkillsService> users = getRandomUsers(6).collect { createService(it)}

        when:
        for(def x = 0; x < 5; x++) {
            assert users[0].addSkill(proj1Skills[x]).body.skillApplied
            assert users[0].addSkill(proj2Skills[x]).body.skillApplied
            assert users[0].addSkill(proj3Skills[x]).body.skillApplied
            assert users[0].addSkill(proj4Skills[x]).body.skillApplied
            assert users[0].addSkill(proj5Skills[x]).body.skillApplied

            assert users[1].addSkill(proj1Skills[x]).body.skillApplied
            assert users[1].addSkill(proj2Skills[x]).body.skillApplied
            assert users[1].addSkill(proj3Skills[x]).body.skillApplied
            assert users[1].addSkill(proj4Skills[x]).body.skillApplied

            assert users[2].addSkill(proj1Skills[x]).body.skillApplied
            assert users[2].addSkill(proj2Skills[x]).body.skillApplied
            assert users[2].addSkill(proj3Skills[x]).body.skillApplied

            assert users[3].addSkill(proj1Skills[x]).body.skillApplied
            assert users[3].addSkill(proj2Skills[x]).body.skillApplied

            assert users[4].addSkill(proj4Skills[x]).body.skillApplied
            assert users[5].addSkill(proj5Skills[x]).body.skillApplied
        }

        def badgeUsers = skillsService.getGlobalBadgeUsers(badge.badgeId, 10, 1, 'totalProgress', false)

        then:
        badgeUsers
        badgeUsers.count == 6
        badgeUsers.totalCount == 6
        badgeUsers.totalPoints == 8
        badgeUsers.totalLevels == 10
        badgeUsers.data[0].skillsAchieved == 8
        badgeUsers.data[0].numLevelsAchieved == 10
        badgeUsers.data[0].totalProgress == 18
        badgeUsers.data[0].userId == users[0].userName

        badgeUsers.data[1].skillsAchieved == 8
        badgeUsers.data[1].numLevelsAchieved == 5
        badgeUsers.data[1].totalProgress == 13
        badgeUsers.data[1].userId == users[1].userName

        badgeUsers.data[2].skillsAchieved == 8
        badgeUsers.data[2].numLevelsAchieved == 3
        badgeUsers.data[2].totalProgress == 11
        badgeUsers.data[2].userId == users[2].userName

        badgeUsers.data[3].skillsAchieved == 6
        badgeUsers.data[3].numLevelsAchieved == 0
        badgeUsers.data[3].totalProgress == 6
        badgeUsers.data[3].userId == users[3].userName

        badgeUsers.data[4].skillsAchieved == 0
        badgeUsers.data[4].numLevelsAchieved == 5
        badgeUsers.data[4].totalProgress == 5
        badgeUsers.data[4].userId == users[5].userName

        badgeUsers.data[5].skillsAchieved == 0
        badgeUsers.data[5].numLevelsAchieved == 2
        badgeUsers.data[5].totalProgress == 2
        badgeUsers.data[5].userId == users[4].userName

    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "retrieve users for global badge - sort by userId and page"() {

        def proj = createProject()
        def subj2 = createSubject(1, 2)
        def subj3 = createSubject(1, 3)
        def subj = createSubject()
        def badge = createBadge()

        List<Map> skills = createSkills(5, 1, 1, 100)
        List<Map> subj2Skills = createSkills(5, 1, 2, 100)
        List<Map> subj3Skills = createSkills(5, 1, 3, 100)

        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skillsService.createProjectAndSubjectAndSkills(null, subj2, subj2Skills)
        skillsService.createProjectAndSubjectAndSkills(null, subj3, subj3Skills)

        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])

        badge.enabled = "true"
        skillsService.updateGlobalBadge(badge)

        List<SkillsService> users = getRandomUsers(9).collect { createService(it) }

        when:
        users.each {
            assert it.addSkill(skills[0]).body.skillApplied
        }

        def badgeUsers = skillsService.getGlobalBadgeUsers(badge.badgeId, 5, 1,  'userId', true)
        def badgeUsersPg2 = skillsService.getGlobalBadgeUsers(badge.badgeId, 5, 2,  'userId', true)

        def badgeUsersDesc = skillsService.getGlobalBadgeUsers(badge.badgeId, 5, 1,  'userId', false)
        def badgeUsersPg2Desc = skillsService.getGlobalBadgeUsers(badge.badgeId, 5, 2,  'userId', false)

        def badgeUsers_smallPg = skillsService.getGlobalBadgeUsers(badge.badgeId, 3, 1,  'userId', true)
        def badgeUsersPg2_smallPg = skillsService.getGlobalBadgeUsers(badge.badgeId, 3, 2,  'userId', true)
        def badgeUsersPg3_smallPg = skillsService.getGlobalBadgeUsers(badge.badgeId, 3, 3,  'userId', true)

        List<String> userNames = users.collect { it.userName }.sort()
        List<String> userNamesReversed = userNames.reverse()
        then:
        badgeUsers
        badgeUsersPg2
        badgeUsers.count == 9
        badgeUsers.totalCount == 9
        badgeUsers.data.userId == userNames[0..4]

        badgeUsersPg2.count == 9
        badgeUsersPg2.totalCount == 9
        badgeUsersPg2.data.userId == userNames[5..8]

        badgeUsersDesc.count == 9
        badgeUsersDesc.totalCount == 9
        badgeUsersDesc.data.userId == userNamesReversed[0..4]

        badgeUsersPg2Desc.count == 9
        badgeUsersPg2Desc.totalCount == 9
        badgeUsersPg2Desc.data.userId == userNamesReversed[5..8]

        badgeUsers_smallPg.count == 9
        badgeUsers_smallPg.totalCount == 9
        badgeUsers_smallPg.data.userId == userNames[0..2]

        badgeUsersPg2_smallPg.count == 9
        badgeUsersPg2_smallPg.totalCount == 9
        badgeUsersPg2_smallPg.data.userId == userNames[3..5]

        badgeUsersPg3_smallPg.count == 9
        badgeUsersPg3_smallPg.totalCount == 9
        badgeUsersPg3_smallPg.data.userId == userNames[6..8]
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "filter users for global badge"() {

        def proj = createProject()
        def subj2 = createSubject(1, 2)
        def subj3 = createSubject(1, 3)
        def subj = createSubject()
        def badge = createBadge()

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
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])

        badge.enabled = "true"
        skillsService.updateGlobalBadge(badge)

        List<SkillsService> users = getRandomUsers(5).collect { createService(it) }

        when:
        for(def x = 0; x < 5; x++) {
            assert users[x].addSkill(skills[0]).body.skillApplied
        }

        def sortedUsers = users.sort{ it.userName }

        def badgeUsers = skillsService.getGlobalBadgeUsers(badge.badgeId, 10, 1)
        def badgeUsersFiltered = skillsService.getGlobalBadgeUsers(badge.badgeId, 10, 1, 'userId', true, sortedUsers[4].userName)

        then:
        badgeUsers
        badgeUsersFiltered
        badgeUsers.count == 5
        badgeUsersFiltered.count == 1
        badgeUsers.totalCount == 5
        badgeUsersFiltered.totalCount == 1
        badgeUsers.data[0].userId == sortedUsers[0].userName
        badgeUsers.data[1].userId == sortedUsers[1].userName
        badgeUsers.data[2].userId == sortedUsers[2].userName
        badgeUsers.data[3].userId == sortedUsers[3].userName
        badgeUsers.data[4].userId == sortedUsers[4].userName
        badgeUsersFiltered.data[0].userId == sortedUsers[4].userName
    }

    def "filter users for global badge by tag"() {

        def proj = createProject()
        def subj2 = createSubject(1, 2)
        def subj3 = createSubject(1, 3)
        def subj = createSubject()
        def badge = createBadge()

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
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])

        badge.enabled = "true"
        skillsService.updateGlobalBadge(badge)

        List<SkillsService> users = getRandomUsers(5).collect { createService(it) }
        skillsService.grantRoot()
        users[0..4].eachWithIndex { user, idx ->
            String tagValue = "tag${idx}"
            skillsService.saveUserTag(user.userName, "dutyOrganization", [tagValue]);
        }

        when:
        for(def x = 0; x < 5; x++) {
            assert users[x].addSkill(skills[0]).body.skillApplied
        }

        def badgeUsers = skillsService.getGlobalBadgeUsers(badge.badgeId, 10, 1)
        def badgeUsersFiltered = skillsService.getGlobalBadgeUsers(badge.badgeId, 10, 1, 'userId', true, '', 'tag4')

        then:
        badgeUsers
        badgeUsersFiltered
        badgeUsers.count == 5
        badgeUsersFiltered.count == 1
        badgeUsers.totalCount == 5
        badgeUsersFiltered.totalCount == 1
        badgeUsersFiltered.data[0].userTag == 'tag4'
    }

    def "filter users for global badge by tag with paging and sorting"() {

        def proj = createProject()
        def subj2 = createSubject(1, 2)
        def subj3 = createSubject(1, 3)
        def subj = createSubject()
        def badge = createBadge()

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
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])

        badge.enabled = "true"
        skillsService.updateGlobalBadge(badge)

        List<SkillsService> users = getRandomUsers(30).collect { createService(it) }
        skillsService.grantRoot()
        List<String> userIdsWithTag = []
        users[0..29].eachWithIndex { user, idx ->
            String tagValue = "tag${idx}"
            skillsService.saveUserTag(user.userName, "dutyOrganization", [tagValue]);
            if(tagValue.contains('tag2')) {
                userIdsWithTag.add(user.userName)
            }
        }

        when:
        for(def x = 0; x < 30; x++) {
            assert users[x].addSkill(skills[0]).body.skillApplied
        }

        def badgeUsers = skillsService.getGlobalBadgeUsers(badge.badgeId, 10, 1)
        def badgeUsersFiltered = skillsService.getGlobalBadgeUsers(badge.badgeId, 10, 1, 'userId', true, '', 'tag2')
        def badgeUsersFilteredPg2 = skillsService.getGlobalBadgeUsers(badge.badgeId, 10, 2, 'userId', true, '', 'tag2')
        def badgeUsersFilteredDesc = skillsService.getGlobalBadgeUsers(badge.badgeId, 10, 1, 'userId', false, '', 'tag2')
        def badgeUsersFilteredDescPg2 = skillsService.getGlobalBadgeUsers(badge.badgeId, 10, 2, 'userId', false, '', 'tag2')

        then:
        badgeUsers
        badgeUsersFiltered
        badgeUsers.count == 30
        badgeUsersFiltered.count == 11
        badgeUsers.totalCount == 30
        badgeUsersFiltered.totalCount == 11
        badgeUsersFiltered.data[0].userTag.contains('tag2')
        badgeUsersFilteredPg2.data[0].userTag.contains('tag2')
        badgeUsersFilteredDesc.data[0].userTag.contains('tag2')
        badgeUsersFilteredDescPg2.data[0].userTag.contains('tag2')
    }

    def "filter users for global badge by tag and user with paging"() {

        def proj = createProject()
        def subj2 = createSubject(1, 2)
        def subj3 = createSubject(1, 3)
        def subj = createSubject()
        def badge = createBadge()

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
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])

        badge.enabled = "true"
        skillsService.updateGlobalBadge(badge)

        List<SkillsService> users = getRandomUsers(30).collect { createService(it) }
        skillsService.grantRoot()
        users[0..29].eachWithIndex { user, idx ->
            String tagValue = "tag${idx}"
            skillsService.saveUserTag(user.userName, "dutyOrganization", [tagValue]);
        }

        when:
        for(def x = 0; x < 30; x++) {
            assert users[x].addSkill(skills[0]).body.skillApplied
        }

        def badgeUsers = skillsService.getGlobalBadgeUsers(badge.badgeId, 10, 1)
        def badgeUsersFiltered = skillsService.getGlobalBadgeUsers(badge.badgeId, 10, 1, 'userId', true, users[15].userName, 'tag15')

        then:
        badgeUsers
        badgeUsersFiltered
        badgeUsers.count == 30
        badgeUsersFiltered.count == 1
        badgeUsers.totalCount == 30
        badgeUsersFiltered.totalCount == 1
        badgeUsersFiltered.data[0].userTag.contains('tag15')
        badgeUsersFiltered.data[0].userId == users[15].userName
    }

    def "Can not exceed maximum page size"() {

        def proj = createProject()
        def subj = createSubject()
        def badge = createBadge()

        List<Map> skills = createSkills(5, 1, 1, 100)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])

        badge.enabled = "true"
        skillsService.updateGlobalBadge(badge)

        List<SkillsService> users = getRandomUsers(3).collect { createService(it) }
        skillsService.grantRoot()
        users[0..2].eachWithIndex { user, idx ->
            String tagValue = "tag${idx}"
            skillsService.saveUserTag(user.userName, "dutyOrganization", [tagValue]);
        }

        when:
        for(def x = 0; x < 3; x++) {
            assert users[x].addSkill(skills[0]).body.skillApplied
        }

        def result = skillsService.getGlobalBadgeUsers(badge.badgeId, 250, 1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.resBody.contains("Cannot ask for more than 200 items, provided=[250]")

    }
}
