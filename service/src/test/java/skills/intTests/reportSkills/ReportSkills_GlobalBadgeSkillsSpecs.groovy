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

import org.joda.time.DateTime
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class ReportSkills_GlobalBadgeSkillsSpecs extends DefaultIntSpec {

    String projId = SkillsFactory.defaultProjId
    String badgeId = 'GlobalBadge1'

    String ultimateRoot = 'jh@dojo.com'
    SkillsService rootSkillsService
    String nonRootUserId = 'foo@bar.com'
    SkillsService nonSupervisorSkillsService

    def setup(){
        skillsService.deleteProjectIfExist(projId)
        rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
        nonSupervisorSkillsService = createService(nonRootUserId)

        if (!rootSkillsService.isRoot()) {
            rootSkillsService.grantRoot()
        }
        rootSkillsService.grantSupervisorRole(skillsService.wsHelper.username)
    }

    def cleanup() {
        rootSkillsService?.removeSupervisorRole(skillsService.wsHelper.username)
    }

    def "give credit if all dependencies were fulfilled"(){
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: projId, subjectId: subj, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1*60, numMaxOccurrencesIncrementInterval: 1, dependentSkillsIds: [skill1.skillId, skill2.skillId, skill3.skillId]]

        Map badge = [badgeId: badgeId, name: 'Test Global Badge 1']
        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId, skill4.skillId]

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "3")
        requiredSkillsIds.each { skillId ->
            skillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }

        DateTime dt = new DateTime().minusDays(4)

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId], "user1", dt.toDate()).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId], "user1", dt.plusDays(1).toDate()).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId], "user1", dt.plusDays(2).toDate()).body
        def resSkill4 = skillsService.addSkill([projectId: projId, skillId: skill4.skillId], "user1", dt.plusDays(3).toDate()).body

        then:
        resSkill1.skillApplied && !resSkill1.completed.find { it.id == badgeId}
        resSkill2.skillApplied && !resSkill2.completed.find { it.id == badgeId}
        resSkill3.skillApplied && !resSkill3.completed.find { it.id == badgeId}
        resSkill4.skillApplied && resSkill4.completed.find { it.id == badgeId}

        cleanup:
        skillsService.deleteGlobalBadge(badgeId)
    }

    def "global badge awarded to users meeting level requirements after enabling"() {
        def proj = SkillsFactory.createProject()
        def proj2 = SkillsFactory.createProject(2)
        def subj = SkillsFactory.createSubject()
        def subj2 = SkillsFactory.createSubject(2)
        def skills = SkillsFactory.createSkills(20)
        def skills2 = SkillsFactory.createSkills(10, 2)
        def badge = [badgeId: badgeId, name: 'Test Global Badge 1', enabled: 'false']

        skillsService.createProject(proj)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)

        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "1")
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj2.projectId, badgeId: badge.badgeId, level: "1")

        (0..9).each {
            if (it == 0) {
                skillsService.addSkill([skillId: skills.get(it).skillId, projectId: proj.projectId], "user2", new Date())
                skillsService.addSkill([skillId: skills2.get(it).skillId, projectId: proj2.projectId], "user2", new Date())
            }
            skillsService.addSkill([skillId: skills.get(it).skillId, projectId: proj.projectId], "user1", new Date())
            skillsService.addSkill([skillId: skills2.get(it).skillId, projectId: proj2.projectId], "user1", new Date())
        }

        def proj1Level = skillsService.getUserLevel(proj.projectId, "user1")
        def proj2Level = skillsService.getUserLevel(proj2.projectId, "user1")

        assert proj1Level > 0
        assert proj2Level > 0

        when:
        def user1SummaryBeforeEnable = skillsService.getBadgesSummary("user1", proj.projectId)
        badge.enabled = true
        skillsService.createGlobalBadge(badge, badge.badgeId)

        def user1Summary = skillsService.getBadgesSummary("user1", proj.projectId)
        def user2Summary = skillsService.getBadgesSummary("user2", proj.projectId)

        then:
        !user1SummaryBeforeEnable[0].badgeAchieved
        user1Summary[0].badgeId == 'GlobalBadge1'
        user1Summary[0].badgeAchieved
        user2Summary[0].badgeId == 'GlobalBadge1'
        !user2Summary[0].badgeAchieved
    }

    def "global badge awarded to users meeting skill requirements after enabling"() {
        def proj = SkillsFactory.createProject()
        def proj2 = SkillsFactory.createProject(2)
        def subj = SkillsFactory.createSubject()
        def subj2 = SkillsFactory.createSubject(2)
        def skills = SkillsFactory.createSkills(20)
        def skills2 = SkillsFactory.createSkills(10, 2)
        def badge = [badgeId: badgeId, name: 'Test Global Badge 1', enabled: 'false']

        skillsService.createProject(proj)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)

        skillsService.createGlobalBadge(badge)
        skillsService.assignSkillToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId)
        skillsService.assignSkillToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[1].skillId)
        skillsService.assignSkillToGlobalBadge(projectId: proj2.projectId, badgeId: badge.badgeId, skillId: skills2[0].skillId)

        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user2", new Date())
        skillsService.addSkill([skillId: skills.get(1).skillId, projectId: proj2.projectId], "user2", new Date())

        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user1", new Date())
        skillsService.addSkill([skillId: skills.get(1).skillId, projectId: proj.projectId], "user1", new Date())
        skillsService.addSkill([skillId: skills2.get(0).skillId, projectId: proj2.projectId], "user1", new Date())

        when:
        def user1SummaryBeforeEnable = skillsService.getBadgesSummary("user1", proj.projectId)
        badge.enabled = true
        skillsService.createGlobalBadge(badge, badge.badgeId)

        def user1Summary = skillsService.getBadgesSummary("user1", proj.projectId)
        def user2Summary = skillsService.getBadgesSummary("user2", proj.projectId)

        then:
        !user1SummaryBeforeEnable[0].badgeAchieved
        user1Summary[0].badgeId == 'GlobalBadge1'
        user1Summary[0].badgeAchieved
        user2Summary[0].badgeId == 'GlobalBadge1'
        !user2Summary[0].badgeAchieved
    }

    def "global badge awarded to users meeting skill and level requirements after enabling"() {
        def proj = SkillsFactory.createProject()
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)
        def subj = SkillsFactory.createSubject()
        def subj2 = SkillsFactory.createSubject(2)
        def subj3 = SkillsFactory.createSubject(3)

        def skills = SkillsFactory.createSkills(20)
        def skills2 = SkillsFactory.createSkills(10, 2)
        def skills3 = SkillsFactory.createSkills(10, 3)

        def badge = [badgeId: badgeId, name: 'Test Global Badge 1', enabled: 'false']

        skillsService.createProject(proj)
        skillsService.createProject(proj2)
        skillsService.createProject(proj3)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)
        skillsService.createSkills(skills3)

        skillsService.createGlobalBadge(badge)
        skillsService.assignSkillToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId)
        skillsService.assignSkillToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[1].skillId)
        skillsService.assignSkillToGlobalBadge(projectId: proj2.projectId, badgeId: badge.badgeId, skillId: skills2[0].skillId)
        skillsService.assignProjectLevelToGlobalBadge([projectId: proj3.projectId, badgeId: badge.badgeId, level: "1"])

        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user2", new Date())
        skillsService.addSkill([skillId: skills.get(1).skillId, projectId: proj2.projectId], "user2", new Date())
        skillsService.addSkill([skillId: skills3.get(0).skillId, projectId: proj3.projectId], "user2", new Date())

        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user1", new Date())
        skillsService.addSkill([skillId: skills.get(1).skillId, projectId: proj.projectId], "user1", new Date())
        skillsService.addSkill([skillId: skills2.get(0).skillId, projectId: proj2.projectId], "user1", new Date())

        (0..6).each {
            skillsService.addSkill([skillId: skills3.get(it).skillId, projectId: proj3.projectId], "user1", new Date())
        }

        when:
        def user1SummaryBeforeEnable = skillsService.getBadgesSummary("user1", proj.projectId)

        badge.enabled = true
        skillsService.createGlobalBadge(badge, badge.badgeId)

        def user1Summary = skillsService.getBadgesSummary("user1", proj.projectId)
        def user2Summary = skillsService.getBadgesSummary("user2", proj.projectId)

        then:
        !user1SummaryBeforeEnable[0].badgeAchieved
        user1Summary[0].badgeId == 'GlobalBadge1'
        user1Summary[0].badgeAchieved
        user2Summary[0].badgeId == 'GlobalBadge1'
        !user2Summary[0].badgeAchieved
    }

}
