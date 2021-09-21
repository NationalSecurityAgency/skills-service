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

class GlobalBadgeSpecs extends DefaultIntSpec {

    SkillsService supervisorService

    def setup() {
        supervisorService = createSupervisor()
    }

    def "remvoing level satisfies global badge for some of the existing users"() {

        // TODO: add another users that do not have level 1 but have same skill achievement so should get the badge
        // TODO: add another user that has another skill achieved and so should NOT get the badge
        def proj = SkillsFactory.createProject()
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj3 = SkillsFactory.createSubject(1, 3)
        def subj = SkillsFactory.createSubject()
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
        supervisorService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
        supervisorService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])


        when:
        println skillsService.addSkill(['projectId': proj.projectId, skillId: skills[0].skillId], "user1", new Date()).body.completed
        //triggers level 1
        def addSkillRes = skillsService.addSkill(['projectId': proj.projectId, skillId: skills[1].skillId], "user1", new Date()).body.completed

        def badge_level_before = skillsService.getBadgeSummary("user1", proj.projectId,  badge.badgeId,-1, true)
        supervisorService.removeProjectLevelFromGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "3")
        def badge_level_after = skillsService.getBadgeSummary("user1", proj.projectId,  badge.badgeId,-1, true)

        then:
        addSkillRes.find( { it.type == "Overall"}).level == 1
        !badge_level_before.badgeAchieved
        !badge_level_before.dateAchieved

        badge_level_after.badgeAchieved
        badge_level_after.dateAchieved
    }

    // TODO: create another badge with similar level requirement but a different skill that is not achieved and make sure badge is not mistakenly awarded
    // TODO: add another test where level achievement is met but required skill is not; remove that skill from the requirement and verify that badge is awarded

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
}
