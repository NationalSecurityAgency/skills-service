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
package skills.intTests

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.model.UserAchievement
import skills.storage.repos.UserAchievedLevelRepo

class SkillsGroupAchievementSpecs extends DefaultIntSpec {

    @Autowired
    UserAchievedLevelRepo achievedRepo

    def "achieve group skill - all skills required"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        def groupChildren = allSkills[1..2]
        groupChildren.each { skill ->
            skill.pointIncrement = 100
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        String userId = 'user1'
        String projectId = proj.projectId
        String subjectId = subj.subjectId
        groupChildren.each { skill ->
            def res = skillsService.addSkill([projectId: projectId, skillId: skill.skillId], userId, new Date())
            assert res.body.skillApplied
            assert res.body.completed.find { it.id == skill.skillId }
        }
        def subjectSummary = skillsService.getSkillSummary(userId, projectId, subjectId)
        List<UserAchievement> groupAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skillsGroupId)

        then:
        groupAchievements
        groupAchievements.size() == 1
        groupAchievements[0].userId == userId
        groupAchievements[0].projectId == projectId
        groupAchievements[0].skillId == skillsGroupId

        subjectSummary
        subjectSummary.skills
        subjectSummary.skills.size() == 1
        subjectSummary.skills[0].skillId == skillsGroupId
        subjectSummary.skills[0].points == 200
        subjectSummary.skills[0].totalPoints == 200
        subjectSummary.skills[0].children
        subjectSummary.skills[0].children.size() == 2
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[0].skillId }
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[0].skillId }.points == 100
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[0].skillId }.totalPoints == 100
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[1].skillId }
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[1].skillId }.points == 100
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[1].skillId }.totalPoints == 100
        subjectSummary.totalSkills == 2
    }

    def "achieve group skill - optional skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        def groupChildren = allSkills[1..2]
        groupChildren.each { skill ->
            skill.pointIncrement = 100
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }
        skillsGroup.numSkillsRequired = 1
        skillsService.updateSkill(skillsGroup, null)

        when:
        String userId = 'user1'
        String projectId = proj.projectId
        String skillId = groupChildren.first().skillId
        String subjectId = subj.subjectId
        def res = skillsService.addSkill([projectId: projectId, skillId: skillId], userId, new Date())
        assert res.body.skillApplied
        assert res.body.completed.find { it.id == skillId }
        def subjectSummary = skillsService.getSkillSummary(userId, projectId, subjectId)
        List<UserAchievement> groupAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skillsGroupId)

        then:
        groupAchievements
        groupAchievements.size() == 1
        groupAchievements[0].userId == userId
        groupAchievements[0].projectId == projectId
        groupAchievements[0].skillId == skillsGroupId
        groupAchievements[0].pointsWhenAchieved == groupChildren.first().pointIncrement * groupChildren.first().numPerformToCompletion

        subjectSummary
        subjectSummary.skills
        subjectSummary.skills.size() == 1
        subjectSummary.skills[0].skillId == skillsGroupId
        subjectSummary.skills[0].points == 100
        subjectSummary.skills[0].totalPoints == 100 * groupChildren.size()
        subjectSummary.skills[0].children
        subjectSummary.skills[0].children.size() == groupChildren.size()
        subjectSummary.skills[0].children.find { it.skillId = skillId }
        subjectSummary.skills[0].children.find { it.skillId = skillId }.points == 100
        subjectSummary.skills[0].children.find { it.skillId = skillId }.totalPoints == 100
        subjectSummary.totalSkills == 2
    }

    def "achieve group skill after lowering numSkillsRequired"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        def groupChildren = allSkills[1..2]
        groupChildren.each { skill ->
            skill.pointIncrement = 100
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        String userId = 'user1'
        String projectId = proj.projectId
        String skillId = groupChildren.first().skillId
        String subjectId = subj.subjectId

        def res = skillsService.addSkill([projectId: projectId, skillId: skillId], userId, new Date())

        List<UserAchievement> groupAchievementsBefore = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skillsGroupId)

        skillsGroup.numSkillsRequired = 1
        skillsService.updateSkill(skillsGroup, null)

        assert res.body.skillApplied
        assert res.body.completed.find { it.id == skillId }
        def subjectSummary = skillsService.getSkillSummary(userId, projectId, subjectId)
        List<UserAchievement> groupAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skillsGroupId)

        then:
        !groupAchievementsBefore
        groupAchievementsBefore.size() == 0

        groupAchievements
        groupAchievements.size() == 1
        groupAchievements[0].userId == userId
        groupAchievements[0].projectId == projectId
        groupAchievements[0].skillId == skillsGroupId

        subjectSummary
        subjectSummary.skills
        subjectSummary.skills.size() == 1
        subjectSummary.skills[0].skillId == skillsGroupId
        subjectSummary.skills[0].points == 100
        subjectSummary.skills[0].totalPoints == 100 * groupChildren.size()
        subjectSummary.skills[0].children
        subjectSummary.skills[0].children.size() == groupChildren.size()
        subjectSummary.skills[0].children.find { it.skillId = skillId }
        subjectSummary.skills[0].children.find { it.skillId = skillId }.points == 100
        subjectSummary.skills[0].children.find { it.skillId = skillId }.totalPoints == 100
        subjectSummary.totalSkills == 2
    }

    def "cannot achieve child skills if group is not enabled"() {
        // NOTE: this test is likely OBE as of v1.10.X and can likely be removed
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsGroup.enabled = false
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        def groupChildren = allSkills[1..2]
        groupChildren.each { skill ->
            skill.pointIncrement = 100
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        String userId = 'user1'
        String projectId = proj.projectId
        String skillId = groupChildren.first().skillId

        def res = skillsService.addSkill([projectId: projectId, skillId: skillId], userId, new Date())

        then:
        !res.body.skillApplied
        !res.body.completed.find { it.id == skillId }
    }

    def "group achieved when child skill gets deleted, all skills required and now user has all skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(4)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        def groupChildren = allSkills[1..3]
        groupChildren.each { skill ->
            skill.pointIncrement = 100
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        String userId = 'user1'
        String projectId = proj.projectId
        String subjectId = subj.subjectId

        def res1 = skillsService.addSkill([projectId: projectId, skillId: groupChildren[0].skillId], userId, new Date())
        def res2 = skillsService.addSkill([projectId: projectId, skillId: groupChildren[1].skillId], userId, new Date())

        List<UserAchievement> groupAchievementsBefore = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skillsGroupId)

        skillsService.deleteSkill(groupChildren[2])

        assert res1.body.skillApplied
        assert res1.body.completed.find { it.id == groupChildren[0].skillId }
        assert res2.body.skillApplied
        assert res2.body.completed.find { it.id == groupChildren[1].skillId }
        def subjectSummary = skillsService.getSkillSummary(userId, projectId, subjectId)
        List<UserAchievement> groupAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skillsGroupId)

        then:
        !groupAchievementsBefore
        groupAchievementsBefore.size() == 0

        groupAchievements
        groupAchievements.size() == 1
        groupAchievements[0].userId == userId
        groupAchievements[0].projectId == projectId
        groupAchievements[0].skillId == skillsGroupId

        subjectSummary
        subjectSummary.skills
        subjectSummary.skills.size() == 1
        subjectSummary.skills[0].skillId == skillsGroupId
        subjectSummary.skills[0].points == 200
        subjectSummary.skills[0].totalPoints == 200
        subjectSummary.skills[0].children
        subjectSummary.skills[0].children.size() == 2
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[0].skillId }
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[0].skillId }.points == 100
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[0].skillId }.totalPoints == 100
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[1].skillId }
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[1].skillId }.points == 100
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[1].skillId }.totalPoints == 100
        subjectSummary.totalSkills == 2
    }

    def "deleting child skill updates points and achievements properly"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(4)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        def groupChildren = allSkills[1..3]
        groupChildren.each { skill ->
            skill.pointIncrement = 100
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        String userId = 'user1'
        String projectId = proj.projectId
        String subjectId = subj.subjectId
        groupChildren.each { skill ->
            def res = skillsService.addSkill([projectId: projectId, skillId: skill.skillId], userId, new Date())
            assert res.body.skillApplied
            assert res.body.completed.find { it.id == skill.skillId }
        }
        def subjectSummary = skillsService.getSkillSummary(userId, projectId, subjectId)
        List<UserAchievement> groupAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skillsGroupId)
        List<UserAchievement> subjectAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, subjectId)

        skillsService.deleteSkill(groupChildren[2])
        def subjectSummaryAfter = skillsService.getSkillSummary(userId, projectId, subjectId)
        List<UserAchievement> groupAchievementsAfter = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skillsGroupId)
        List<UserAchievement> subjectAchievementsAfter = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, subjectId)

        then:
        groupAchievements
        groupAchievements.size() == 1
        groupAchievements[0].userId == userId
        groupAchievements[0].projectId == projectId
        groupAchievements[0].skillId == skillsGroupId

        subjectAchievements
        subjectAchievements.size() == 5
        subjectAchievements.find { it.level == 1 && it.userId == userId && it.projectId == projectId && it.skillId == subjectId }
        subjectAchievements.find { it.level == 2 && it.userId == userId && it.projectId == projectId && it.skillId == subjectId }
        subjectAchievements.find { it.level == 3 && it.userId == userId && it.projectId == projectId && it.skillId == subjectId }
        subjectAchievements.find { it.level == 4 && it.userId == userId && it.projectId == projectId && it.skillId == subjectId }
        subjectAchievements.find { it.level == 5 && it.userId == userId && it.projectId == projectId && it.skillId == subjectId }

        subjectSummary
        subjectSummary.points == 300
        subjectSummary.todaysPoints == 300
        subjectSummary.totalPoints == 300
        subjectSummary.skills
        subjectSummary.skills.size() == 1
        subjectSummary.skills[0].skillId == skillsGroupId
        subjectSummary.skills[0].points == 300
        subjectSummary.skills[0].totalPoints == 300
        subjectSummary.skills[0].todaysPoints == 300
        subjectSummary.skills[0].children
        subjectSummary.skills[0].children.size() == 3
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[0].skillId }
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[0].skillId }.points == 100
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[0].skillId }.totalPoints == 100
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[1].skillId }
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[1].skillId }.points == 100
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[1].skillId }.totalPoints == 100
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[2].skillId }
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[2].skillId }.points == 100
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[2].skillId }.totalPoints == 100
        subjectSummary.totalSkills == 3

        subjectSummaryAfter
        subjectSummaryAfter.points == 200
        subjectSummaryAfter.todaysPoints == 200
        subjectSummaryAfter.totalPoints == 200
        subjectSummaryAfter.skills
        subjectSummaryAfter.skills.size() == 1
        subjectSummaryAfter.skills[0].skillId == skillsGroupId
        subjectSummaryAfter.skills[0].points == 200
        subjectSummaryAfter.skills[0].todaysPoints == 200
        subjectSummaryAfter.skills[0].totalPoints == 200
        subjectSummaryAfter.skills[0].children
        subjectSummaryAfter.skills[0].children.size() == 2
        subjectSummaryAfter.skills[0].children.find { it.skillId == groupChildren[0].skillId }
        subjectSummaryAfter.skills[0].children.find { it.skillId == groupChildren[0].skillId }.points == 100
        subjectSummaryAfter.skills[0].children.find { it.skillId == groupChildren[0].skillId }.totalPoints == 100
        subjectSummaryAfter.skills[0].children.find { it.skillId == groupChildren[1].skillId }
        subjectSummaryAfter.skills[0].children.find { it.skillId == groupChildren[1].skillId }.points == 100
        subjectSummaryAfter.skills[0].children.find { it.skillId == groupChildren[1].skillId }.totalPoints == 100
        subjectSummaryAfter.totalSkills == 2

        groupAchievementsAfter
        groupAchievementsAfter.size() == 1
        groupAchievementsAfter[0].userId == userId
        groupAchievementsAfter[0].projectId == projectId
        groupAchievementsAfter[0].skillId == skillsGroupId

        subjectAchievementsAfter
        subjectAchievementsAfter.size() == 5
        subjectAchievementsAfter.find { it.level == 1 && it.userId == userId && it.projectId == projectId && it.skillId == subjectId }
        subjectAchievementsAfter.find { it.level == 2 && it.userId == userId && it.projectId == projectId && it.skillId == subjectId }
        subjectAchievementsAfter.find { it.level == 3 && it.userId == userId && it.projectId == projectId && it.skillId == subjectId }
        subjectAchievementsAfter.find { it.level == 4 && it.userId == userId && it.projectId == projectId && it.skillId == subjectId }
        subjectAchievementsAfter.find { it.level == 5 && it.userId == userId && it.projectId == projectId && it.skillId == subjectId }
    }

    def "group achieved when child skill occurrences are decreased, all skills required and now user has all skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        def groupChildren = allSkills[1..2]
        groupChildren[1].numPerformToCompletion = 2
        groupChildren.each { skill ->
            skill.pointIncrement = 100
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        String userId = 'user1'
        String projectId = proj.projectId
        String subjectId = subj.subjectId
        groupChildren.each { skill ->
            def res = skillsService.addSkill([projectId: projectId, skillId: skill.skillId], userId, new Date())
            assert res.body.skillApplied
            if (skill.skillId == groupChildren[1].skillId) {
                assert !res.body.completed.find { it.id == skill.skillId }
            } else {
                assert res.body.completed.find { it.id == skill.skillId }
            }
        }
        List<UserAchievement> groupAchievementsBefore = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skillsGroupId)

        groupChildren[1].numPerformToCompletion = 1
        skillsService.updateSkill(groupChildren[1], null)

        def subjectSummary = skillsService.getSkillSummary(userId, projectId, subjectId)
        List<UserAchievement> groupAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skillsGroupId)

        then:
        !groupAchievementsBefore
        groupAchievementsBefore.size() == 0

        groupAchievements
        groupAchievements.size() == 1
        groupAchievements[0].userId == userId
        groupAchievements[0].projectId == projectId
        groupAchievements[0].skillId == skillsGroupId

        subjectSummary
        subjectSummary.skills
        subjectSummary.skills.size() == 1
        subjectSummary.skills[0].skillId == skillsGroupId
        subjectSummary.skills[0].points == 200
        subjectSummary.skills[0].totalPoints == 200
        subjectSummary.skills[0].children
        subjectSummary.skills[0].children.size() == 2
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[0].skillId }
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[0].skillId }.points == 100
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[0].skillId }.totalPoints == 100
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[1].skillId }
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[1].skillId }.points == 100
        subjectSummary.skills[0].children.find { it.skillId == groupChildren[1].skillId }.totalPoints == 100
        subjectSummary.totalSkills == 2
    }

    def "cannot earn more points than the skills group numSkillsRequired will allow"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        def groupChildren = allSkills[1..2]
        groupChildren.each { skill ->
            skill.pointIncrement = 100
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }
        skillsGroup.numSkillsRequired = 1
        skillsService.updateSkill(skillsGroup, null)

        when:
        String userId = 'user1'
        String projectId = proj.projectId
        String subjectId = subj.subjectId
        groupChildren.each { skill ->
            def res = skillsService.addSkill([projectId: projectId, skillId: skill.skillId], userId, new Date())
            assert res.body.skillApplied
            assert res.body.completed.find { it.id == skill.skillId }
        }

        def subjectSummary = skillsService.getSkillSummary(userId, projectId, subjectId)
        List<UserAchievement> groupAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skillsGroupId)

        then:
        groupAchievements
        groupAchievements.size() == 1
        groupAchievements[0].userId == userId
        groupAchievements[0].projectId == projectId
        groupAchievements[0].skillId == skillsGroupId

        subjectSummary
        subjectSummary.skills
        subjectSummary.skills.size() == 1
        subjectSummary.skills[0].skillId == skillsGroupId
        subjectSummary.skills[0].points == 100
        subjectSummary.skills[0].totalPoints == groupChildren[0].pointIncrement * groupChildren[0].numPerformToCompletion * groupChildren.size()
        subjectSummary.skills[0].children
        subjectSummary.skills[0].children.size() == groupChildren.size()
        subjectSummary.skills[0].children.find { it.skillId = groupChildren[0].skillId }
        subjectSummary.skills[0].children.find { it.skillId = groupChildren[0].skillId }.points == 100
        subjectSummary.skills[0].children.find { it.skillId = groupChildren[0].skillId }.totalPoints == 100
        subjectSummary.skills[0].children.find { it.skillId = groupChildren[1].skillId }
        subjectSummary.skills[0].children.find { it.skillId = groupChildren[1].skillId }.points == 100
        subjectSummary.skills[0].children.find { it.skillId = groupChildren[1].skillId }.totalPoints == 100
        subjectSummary.totalSkills == 2
    }

    def "cannot add skill for a skills group itself"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        def groupChildren = allSkills[1..2]
        groupChildren.each { skill ->
            skill.pointIncrement = 100
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        String userId = 'user1'
        String projectId = proj.projectId
        String skillId = skillsGroupId

        def res = skillsService.addSkill([projectId: projectId, skillId: skillId], userId, new Date())

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("Failed to report skill event because skill definition does not exist") // only looks for skillDef.type == SkillDef.ContainerType.Skill
    }

}
