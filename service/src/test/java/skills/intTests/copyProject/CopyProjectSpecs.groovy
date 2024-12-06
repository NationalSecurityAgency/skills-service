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
package skills.intTests.copyProject

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.settings.Settings
import skills.skillLoading.RankingLoader
import skills.storage.model.LevelDef
import skills.storage.model.Attachment
import skills.storage.model.QuizDefParent
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName
import skills.storage.repos.LevelDefRepo

import skills.storage.repos.AttachmentRepo
import skills.utils.GroovyToJavaByteUtils

import static skills.intTests.utils.SkillsFactory.*

class CopyProjectSpecs extends CopyIntSpec {

    def "copy project with majority of features utilized"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        // edit levels
        def levels = skillsService.getLevels(p1.projectId, null).sort() { it.level }
        levels.each { def levelToEdit ->
            levelToEdit.percent = levelToEdit.percent + 1
            skillsService.editLevel(p1.projectId, null, levelToEdit.level as String, levelToEdit)
        }
        def projLevels = skillsService.getLevels(p1.projectId, null).sort() { it.level }

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        def group1 = createSkillsGroup(1, 1, 22)
        def group2 = createSkillsGroup(1, 1, 33)
        skillsService.createSubject(p1subj1)
        skillsService.createSkills([p1Skills[0..2], group1, group2].flatten())

        p1Skills[3..7].each {
            skillsService.assignSkillToSkillsGroup(group1.skillId, it)
        }
        p1Skills[8..9].each {
            skillsService.assignSkillToSkillsGroup(group2.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        def p1SkillsSubj2 = createSkills(5, 1, 2, 22)
        def group3 = createSkillsGroup(1, 2, 33)
        skillsService.createSubject(p1subj2)

        // edit subject
        def subj1Level = skillsService.getLevels(p1.projectId, p1subj2.subjectId).sort() { it.level }
        subj1Level.each { def levelToEdit ->
            levelToEdit.percent = levelToEdit.percent + 2
            skillsService.editLevel(p1.projectId, p1subj1.subjectId, levelToEdit.level as String, levelToEdit)
        }

        skillsService.createSkills([p1SkillsSubj2, group3].flatten())

        def badge = SkillsFactory.createBadge(1, 50)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge(p1.projectId, badge.badgeId, p1Skills[0].skillId)
        skillsService.assignSkillToBadge(p1.projectId, badge.badgeId, p1Skills[1].skillId)
        skillsService.assignSkillToBadge(p1.projectId, badge.badgeId, p1SkillsSubj2[1].skillId)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills.get(0).skillId, p1Skills.get(2).skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills.get(2).skillId, p1SkillsSubj2.get(2).skillId)

        skillsService.reuseSkills(p1.projectId, [p1Skills[1].skillId], p1subj2.subjectId)
        skillsService.reuseSkills(p1.projectId, [p1Skills[7].skillId], p1subj2.subjectId, group3.skillId)

        def subj1UpdatedLevels = skillsService.getLevels(p1.projectId, p1subj1.subjectId).sort() { it.level }
        def subj2UpdatedLevels = skillsService.getLevels(p1.projectId, p1subj2.subjectId).sort() { it.level }

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def copiedProjStats = skillsService.getProject(projToCopy.projectId)
        def copiedSubject1 = skillsService.getSubject([subjectId: p1subj1.subjectId, projectId: projToCopy.projectId])
        def copiedSubj1Skills = skillsService.getSkillsForSubject(projToCopy.projectId, p1subj1.subjectId)
        def copiedGroup1Skills = skillsService.getSkillsForGroup(projToCopy.projectId, group1.skillId)
        def copiedGroup2Skills = skillsService.getSkillsForGroup(projToCopy.projectId, group2.skillId)

        def copiedSubject2 = skillsService.getSubject([subjectId: p1subj2.subjectId, projectId: projToCopy.projectId])
        def copiedSubj2Skills = skillsService.getSkillsForSubject(projToCopy.projectId, p1subj2.subjectId)
        def copiedGroup3Skills = skillsService.getSkillsForGroup(projToCopy.projectId, group3.skillId)

        def copiedBadge = skillsService.getBadge(projToCopy.projectId, badge.badgeId)
        def copiedDeps = skillsService.getDependencyGraph(projToCopy.projectId)

        def copiedProjLevels = skillsService.getLevels(projToCopy.projectId, null).sort() { it.level }
        def copiedSubj1UpdatedLevels = skillsService.getLevels(p1.projectId, p1subj1.subjectId).sort() { it.level }
        def copiedSubj2UpdatedLevels = skillsService.getLevels(p1.projectId, p1subj2.subjectId).sort() { it.level }

        then:
        copiedProjStats.name == projToCopy.name
        copiedProjStats.numSubjects == 2
        copiedProjStats.numSkills == 15
        copiedProjStats.totalPoints == (100 * 10) + (5 * 22)
        copiedProjStats.numSkillsReused == 2
        copiedProjStats.totalPointsReused == 200

        copiedSubject1.name == p1subj1.name
        copiedSubject1.subjectId == p1subj1.subjectId
        copiedSubject1.numGroups == 2
        copiedSubject1.numSkills == 10
        copiedSubject1.totalPoints == (100 * 10)
        copiedSubject1.numSkillsReused == 0
        copiedSubject1.totalPointsReused == 0

        copiedSubj1Skills.skillId == [p1Skills[0..2], group1, group2].flatten().skillId
        copiedGroup1Skills.skillId == p1Skills[3..7].skillId
        copiedGroup2Skills.skillId == p1Skills[8..9].skillId

        copiedSubject2.name == p1subj2.name
        copiedSubject2.subjectId == p1subj2.subjectId
        copiedSubject2.numGroups == 1
        copiedSubject2.numSkills == 5
        copiedSubject2.totalPoints == (22 * 5)
        copiedSubject2.numSkillsReused == 2
        copiedSubject2.totalPointsReused == 200

        copiedSubj2Skills.skillId == [[p1SkillsSubj2, group3].flatten().skillId, SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0)].flatten()
        copiedGroup3Skills.skillId == [SkillReuseIdUtil.addTag(p1Skills[7].skillId, 0)]

        copiedBadge.name == badge.name
        copiedBadge.projectId == projToCopy.projectId
        copiedBadge.badgeId == badge.badgeId
        copiedBadge.totalPoints == (100 * 2) + (22 * 1)
        copiedBadge.numSkills == 3

        validateGraph(copiedDeps, [
                new Edge(from: p1Skills[0].skillId, to: p1Skills[2].skillId),
                new Edge(from: p1Skills[2].skillId, to: p1SkillsSubj2[2].skillId),
        ])

        copiedProjLevels.percent == projLevels.percent

        copiedSubj1UpdatedLevels.percent == subj1UpdatedLevels.percent
        copiedSubj2UpdatedLevels.percent == subj2UpdatedLevels.percent
    }

    def "subject attributes are properly copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        p1subj1.description = "Very important Stuff"
        p1subj1.helpUrl = "http://www.greatlink.com"
        p1subj1.iconClass = "fas fa-address-card"
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def copiedSubjs = skillsService.getSubjects(projToCopy.projectId)
        then:
        copiedSubjs.subjectId == [p1subj1.subjectId]
        def copiedSubj = copiedSubjs[0]
        copiedSubj.name == p1subj1.name
        copiedSubj.description == p1subj1.description
        copiedSubj.helpUrl == p1subj1.helpUrl
        copiedSubj.iconClass == p1subj1.iconClass
    }

    def "custom icons are properly copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        ClassPathResource resource = new ClassPathResource("/dot2.png")
        def file = resource.getFile()
        skillsService.uploadIcon([projectId:(p1.projectId)], file)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)
        def originalIcons = skillsService.getIconCssForProject(p1)
        def copiedIcons = skillsService.getIconCssForProject(projToCopy)

        then:
        originalIcons[0].filename == "dot2.png"
        originalIcons[0].cssClassname == "TestProject1-dot2png"
        copiedIcons[0].filename == "dot2.png"
        copiedIcons[0].cssClassname == "TestProject2-dot2png"
    }

    def "subjects display order is copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1subj3 = createSubject(1, 3)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])
        skillsService.createSubject(p1subj2)
        skillsService.createSubject(p1subj3)

        skillsService.changeSubjectDisplayOrder(p1subj3, 0)

        when:
        def originalSubjs = skillsService.getSubjects(p1.projectId).sort { it.displayOrder }
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def copiedSubjs = skillsService.getSubjects(projToCopy.projectId).sort { it.displayOrder }
        then:
        originalSubjs.subjectId == [p1subj3.subjectId, p1subj1.subjectId, p1subj2.subjectId]
        copiedSubjs.subjectId == [p1subj3.subjectId, p1subj1.subjectId, p1subj2.subjectId]
        copiedSubjs.displayOrder == [1, 2, 3]
    }

    def "settings are copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        skillsService.addOrUpdateProjectSetting(p1.projectId, RankingLoader.PROJ_ADMINS_RANK_AND_LEADERBOARD_OPT_OUT_PREF, true.toString())
        skillsService.addOrUpdateProjectSetting(p1.projectId, "project.displayName", "blah")

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def copiedSettings = skillsService.getProjectSettings(projToCopy.projectId).sort { it.setting }
        then:
        copiedSettings.setting == [RankingLoader.PROJ_ADMINS_RANK_AND_LEADERBOARD_OPT_OUT_PREF, "project.displayName", Settings.PROJECT_COMMUNITY_VALUE.settingName, Settings.USER_PROJECT_ROLE.settingName]
        copiedSettings.value == ["true", "blah", "All Dragons", RoleName.ROLE_PROJECT_ADMIN.toString()]
        copiedSettings.projectId == [projToCopy.projectId, projToCopy.projectId, projToCopy.projectId, projToCopy.projectId]
    }

    def "copied project should not be discoverable by default"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        skillsService.addOrUpdateProjectSetting(p1.projectId, RankingLoader.PROJ_ADMINS_RANK_AND_LEADERBOARD_OPT_OUT_PREF, true.toString())
        skillsService.addOrUpdateProjectSetting(p1.projectId, Settings.PRODUCTION_MODE.settingName, true.toString())
        skillsService.addOrUpdateProjectSetting(p1.projectId, "project.displayName", "blah")

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def copiedSettings = skillsService.getProjectSettings(projToCopy.projectId).sort { it.setting }
        then:
        copiedSettings.projectId == [projToCopy.projectId, projToCopy.projectId, projToCopy.projectId, projToCopy.projectId]
        copiedSettings.setting == [RankingLoader.PROJ_ADMINS_RANK_AND_LEADERBOARD_OPT_OUT_PREF, "project.displayName", Settings.PROJECT_COMMUNITY_VALUE.settingName, Settings.USER_PROJECT_ROLE.settingName]
        copiedSettings.value == ["true", "blah", "All Dragons", RoleName.ROLE_PROJECT_ADMIN.toString()]
    }

    def "group attributes are properly copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 22, 0, 12, 512, 18,)
        def skill2 = createSkill(1, 1, 23, 0, 12, 512, 18,)

        def group1 = createSkillsGroup(1, 1, 4)
        group1.description = "blah 1"
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [group1])
        skillsService.assignSkillToSkillsGroup(group1.skillId, skill1)
        skillsService.assignSkillToSkillsGroup(group1.skillId, skill2)
        group1.numSkillsRequired = 1
        skillsService.createSkill(group1)

        def group2 = createSkillsGroup(1, 1, 5)
        group2.description = "something else"
        skillsService.createSkill(group2)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def original1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: group1.skillId])
        def original2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: group2.skillId])

        def copied1 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: group1.skillId])
        def copied2 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: group2.skillId])

        then:
        original1.description == group1.description
        copied1.description == group1.description
        copied1.skillId == group1.skillId
        copied1.name == group1.name
        copied1.type == SkillDef.ContainerType.SkillsGroup.toString()
        copied1.totalPoints == (18 * 12) * 2
        copied1.numSkillsRequired == 1

        original2.description == group2.description
        copied2.description == group2.description
        copied2.skillId == group2.skillId
        copied2.name == group2.name
        copied2.type == SkillDef.ContainerType.SkillsGroup.toString()
        copied2.totalPoints == 0
        copied2.numSkillsRequired == -1

    }

    def "group with multiple skills and partial requirement of most skills is properly copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skills = createSkills(10, 1, 1, 100)
        def group1 = createSkillsGroup(1, 1, 40)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [group1])
        skills.each {
            skillsService.assignSkillToSkillsGroup(group1.skillId, it)
        }
        group1.numSkillsRequired = 9
        skillsService.createSkill(group1)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def original1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: group1.skillId])

        def copied1 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: group1.skillId])

        then:
        original1.description == group1.description
        copied1.description == group1.description
        copied1.skillId == group1.skillId
        copied1.name == group1.name
        copied1.type == SkillDef.ContainerType.SkillsGroup.toString()
        copied1.totalPoints == (10 * 100)
        copied1.numSkillsRequired == 9
    }

    def "badge properties are copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skills = createSkills(6, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, skills)

        def badge1 = createBadge(1, 1)
        badge1.description = "blah 1"
        badge1.helpUrl = "http://www.greatlink.com"
        badge1.iconClass = "fas fa-adjust"
        skillsService.createBadge(badge1)
        skills[0..2].each {
            skillsService.assignSkillToBadge(p1.projectId, badge1.badgeId, it.skillId)
        }
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = createBadge(1, 2)
        badge2.description = "blah 1"
        badge2.helpUrl = "http://www.someotherlink.com"
        Date oneWeekAgo = new Date() - 7
        Date oneWeekInTheFuture = new Date() + 7
        badge2.startDate = oneWeekAgo
        badge2.endDate = oneWeekInTheFuture
        skillsService.createBadge(badge2)
        skills[2..5].each {
            skillsService.assignSkillToBadge(p1.projectId, badge2.badgeId, it.skillId)
        }

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def copied1 = skillsService.getBadge([projectId: projToCopy.projectId, badgeId: badge1.badgeId])
        def copied2 = skillsService.getBadge([projectId: projToCopy.projectId, badgeId: badge2.badgeId])

        then:
        copied1.description == badge1.description
        copied1.badgeId == badge1.badgeId
        copied1.name == badge1.name
        copied1.helpUrl == badge1.helpUrl
        copied1.numSkills == 3
        copied1.totalPoints == 10 * 3
        copied1.enabled == "true"
        copied1.iconClass == badge1.iconClass
        !copied1.startDate
        !copied1.endDate

        copied2.description == badge2.description
        copied2.badgeId == badge2.badgeId
        copied2.name == badge2.name
        copied2.helpUrl == badge2.helpUrl
        copied2.numSkills == 4
        copied2.totalPoints == 10 * 4
        copied2.enabled == "false"
        copied2.startDate == oneWeekAgo.format("MM-dd-yyy")
        copied2.endDate == oneWeekInTheFuture.format("MM-dd-yyy")
        copied2.iconClass == "fa fa-question-circle" // default
    }

    def "badge display order is copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skills = createSkills(6, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, skills)

        def badge1 = createBadge(1, 1)
        skillsService.createBadge(badge1)
        def badge2 = createBadge(1, 2)
        skillsService.createBadge(badge2)
        def badge3 = createBadge(1, 3)
        skillsService.createBadge(badge3)

        skillsService.changeBadgeDisplayOrder(badge3, 0)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def orig = skillsService.getBadges(p1.projectId).sort { it.displayOrder }
        def copied1 = skillsService.getBadges(projToCopy.projectId).sort { it.displayOrder }

        then:
        orig.badgeId == [badge3.badgeId, badge1.badgeId, badge2.badgeId]
        copied1.badgeId == [badge3.badgeId, badge1.badgeId, badge2.badgeId]
        copied1.displayOrder == [1, 2, 3]
    }

    def "copy live badge with no skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skills = createSkills(6, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, skills)

        def badge1 = createBadge(1, 1)
        badge1.description = "blah 1"
        badge1.helpUrl = "http://www.greatlink.com"
        badge1.iconClass = "fas fa-adjust"
        skillsService.createBadge(badge1)
        skills[0..2].each {
            skillsService.assignSkillToBadge(p1.projectId, badge1.badgeId, it.skillId)
        }
        badge1.enabled = true
        skillsService.createBadge(badge1)
        skills[0..2].each {
            skillsService.removeSkillFromBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: it.skillId])
        }

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)
        def copied1 = skillsService.getBadge([projectId: projToCopy.projectId, badgeId: badge1.badgeId])

        then:
        copied1.description == badge1.description
        copied1.badgeId == badge1.badgeId
        copied1.name == badge1.name
        copied1.helpUrl == badge1.helpUrl
        copied1.numSkills == 0
        copied1.totalPoints == 0
        copied1.enabled == "false"
        copied1.iconClass == badge1.iconClass
        !copied1.startDate
        !copied1.endDate
    }

    def "validate dependencies were copied"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        def group1 = createSkillsGroup(1, 1, 22)
        def group2 = createSkillsGroup(1, 1, 33)
        skillsService.createSubject(p1subj1)
        skillsService.createSkills([p1Skills[0..3], group1, group2].flatten())
        p1Skills[4..9].each {
            skillsService.assignSkillToSkillsGroup(group1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        def p1SkillsSubj2 = createSkills(5, 1, 2, 22)
        def group3 = createSkillsGroup(1, 2, 33)
        skillsService.createSubject(p1subj2)
        skillsService.createSkills([p1SkillsSubj2[0..2], group3].flatten())
        p1SkillsSubj2[3..4].each {
            skillsService.assignSkillToSkillsGroup(group3.skillId, it)
        }

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills.get(0).skillId, p1Skills.get(2).skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills.get(2).skillId, p1Skills.get(5).skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills.get(3).skillId, p1SkillsSubj2.get(0).skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills.get(3).skillId, p1SkillsSubj2.get(4).skillId)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)
        def copiedDeps = skillsService.getDependencyGraph(projToCopy.projectId)
        then:
        validateGraph(copiedDeps, [
                new Edge(from: p1Skills[0].skillId, to: p1Skills[2].skillId),
                new Edge(from: p1Skills[2].skillId, to: p1Skills[5].skillId),
                new Edge(from: p1Skills[3].skillId, to: p1SkillsSubj2[0].skillId),
                new Edge(from: p1Skills[3].skillId, to: p1SkillsSubj2[4].skillId),
        ])
    }

    def "validate cross-project dependencies are NOT copied"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 40
        }

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)
        proj2_skills.each {
            it.pointIncrement = 50
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)

        when:
        // proj2 copy
        def proj2ToCopy = createProject(3)
        skillsService.copyProject(proj2.projectId, proj2ToCopy)

        // proj1 copy
        def proj1ToCopy = createProject(4)
        skillsService.copyProject(proj1.projectId, proj1ToCopy)

        def copiedDeps = skillsService.getDependencyGraph(proj2ToCopy.projectId)
        def copiedDeps1 = skillsService.getDependencyGraph(proj1ToCopy.projectId)
        def originalDeps = skillsService.getDependencyGraph(proj2.projectId)
        then:
        !copiedDeps.edges
        !copiedDeps.nodes

        !copiedDeps1.edges
        !copiedDeps1.nodes

        validateGraph(originalDeps, [
                new Edge(from: proj2_skills.get(0).skillId, to: proj1_skills.get(0).skillId),
        ])
    }

    def "projects copied by a root user must be pinned to the user"() {
        SkillsService rootUser = createRootSkillService()
        def p1 = createProject(1)
        rootUser.createProject(p1)
        // UI pins - so have to simulate
        rootUser.pinProject(p1.projectId)

        when:
        def projToCopy = createProject(2)
        rootUser.copyProject(p1.projectId, projToCopy)

        def projects = rootUser.getProjects()
        then:
        projects.projectId == [p1.projectId, projToCopy.projectId]
    }

    // there are also a number of tests validating that users' quiz achievements are reflected in the copied project
    // these tests were added to QuizSkillAssignmentAndUserAchievementsSpecs
    def "quiz-based skills are copied"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        def survey = QuizDefFactory.createQuizSurvey(2)
        skillsService.createQuizDef(survey)
        def surveyQuestions = [QuizDefFactory.createSingleChoiceSurveyQuestion(2, 1, 2)]
        skillsService.createQuizQuestionDefs(surveyQuestions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[3].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[3].quizId = survey.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(proj.projectId, projToCopy)

        def apiSkills = skillsService.getSkillSummary(skillsService.userName, projToCopy.projectId, subj.subjectId)
        then:
        apiSkills.skills.selfReporting?.type == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, QuizDefParent.QuizType.Survey.toString(), null]
        apiSkills.skills.selfReporting?.quizId == [quiz.quizId, null, null, survey.quizId, null]
        apiSkills.skills.selfReporting?.quizName == [quiz.name, null, null, survey.name, null]
        apiSkills.skills.selfReporting?.numQuizQuestions == [5, 0, null, 1, null]
    }

    def "copy project with badges in the learning path"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createSubject(p1subj1)
        skillsService.createSkills(p1Skills[0..9])

        def badge = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge(p1.projectId, badge.badgeId, p1Skills[0].skillId)
        badge.enabled = true
        skillsService.createBadge(badge)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge(p1.projectId, badge2.badgeId, p1Skills[1].skillId)
        badge2.enabled = true
        skillsService.createBadge(badge2)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills.get(0).skillId, p1Skills.get(2).skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills.get(4).skillId, badge.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills.get(5).skillId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge.badgeId, badge2.badgeId)

        List expected = [
                new Edge(from: p1Skills[0].skillId, to: p1Skills[2].skillId),
                new Edge(from: p1Skills[4].skillId, to: badge.badgeId),
                new Edge(from: p1Skills[5].skillId, to: badge2.badgeId),
                new Edge(from: badge.badgeId, to: badge2.badgeId)
        ]
        when:
        def projToCopy = createProject(2)
        def originalDeps = skillsService.getDependencyGraph(p1.projectId)
        skillsService.copyProject(p1.projectId, projToCopy)
        def copiedDeps = skillsService.getDependencyGraph(projToCopy.projectId)
        then:
        validateGraph(originalDeps, expected)
        validateGraph(copiedDeps, expected)
    }

    def "copy a project that has attachments in skill's description - attachments should be copied"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def p1subj1 = createSubject(1, 1)
        skillsService.createSubject(p1subj1)

        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)
        def result = skillsService.uploadAttachment(resource, p1.projectId, null, null)
        String attachmentHref = result.href

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachmentHref})".toString()
        skillsService.createSkills(p1Skills)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def origProjSkill = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def copyProjSkill = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        then:
        origProjSkill.description == "Here is a [Link](${attachmentHref})"

        attachments.size() == 2
        Attachment originalAttachment = attachments.find {  attachmentHref.contains(it.uuid)}
        Attachment newAttachment = attachments.find { !attachmentHref.contains(it.uuid)}

        originalAttachment.projectId == p1.projectId

        newAttachment.projectId == projToCopy.projectId
        copyProjSkill.description == "Here is a [Link](/api/download/${newAttachment.uuid})"

        SkillsService.FileAndHeaders fileAndHeaders = skillsService.downloadAttachment("/api/download/${newAttachment.uuid}")
        File file = fileAndHeaders.file
        file
        file.bytes == contents.getBytes()
    }

    def "copy a project that has attachments in multiple skills' description - attachments should be copied"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def p1subj1 = createSubject(1, 1)
        skillsService.createSubject(p1subj1)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)
        def attachment3Href = attachFileAndReturnHref(p1.projectId)
        def attachment4Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p1Subj2Skills = createSkills(2, 1, 2, 100)
        p1Subj2Skills[0].description = "Here is a [Link](${attachment3Href})".toString()
        p1Subj2Skills[1].description = "Here is a [Link](${attachment4Href})".toString()
        skillsService.createSkills(p1Subj2Skills)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])
        def origProjSkill3 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Subj2Skills[0].skillId])
        def origProjSkill4 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Subj2Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def copyProjSkill2 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])
        def copyProjSkill3 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj2.subjectId, skillId: p1Subj2Skills[0].skillId])
        def copyProjSkill4 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj2.subjectId, skillId: p1Subj2Skills[1].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"
        origProjSkill3.description == "Here is a [Link](${attachment3Href})"
        origProjSkill4.description == "Here is a [Link](${attachment4Href})"

        attachments.size() == 8
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        Attachment originalAttachment3 = attachments.find {  attachment3Href.contains(it.uuid)}
        Attachment originalAttachment4 = attachments.find {  attachment4Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId
        originalAttachment3.projectId == p1.projectId
        originalAttachment4.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid) && !attachment3Href.contains(it.uuid) && !attachment4Href.contains(it.uuid)
        }

        List<String> copiedDescriptions = newAttachments.collect( {"Here is a [Link](/api/download/${it.uuid})".toString() })
        copiedDescriptions.contains(copyProjSkill1.description)
        copiedDescriptions.contains(copyProjSkill2.description)
        copiedDescriptions.contains(copyProjSkill3.description)
        copiedDescriptions.contains(copyProjSkill4.description)

        newAttachments.each {
            assert it.projectId == projToCopy.projectId
        }
    }

    def "copy a project that has the same attachment in multiple skills' description - attachments should be copied"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def p1subj1 = createSubject(1, 1)
        skillsService.createSubject(p1subj1)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSkills(p1Skills)

        def p1Subj2Skills = createSkills(2, 1, 2, 100)
        p1Subj2Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Subj2Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Subj2Skills)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])
        def origProjSkill3 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Subj2Skills[0].skillId])
        def origProjSkill4 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: p1Subj2Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def copyProjSkill2 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])
        def copyProjSkill3 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj2.subjectId, skillId: p1Subj2Skills[0].skillId])
        def copyProjSkill4 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj2.subjectId, skillId: p1Subj2Skills[1].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](/api/download/${attachments.find { it.projectId == p1.projectId && it.skillId == p1Skills[1].skillId }.uuid})"
        origProjSkill3.description == "Here is a [Link](/api/download/${attachments.find { it.projectId == p1.projectId && it.skillId == p1Subj2Skills[0].skillId }.uuid})"
        origProjSkill4.description == "Here is a [Link](/api/download/${attachments.find { it.projectId == p1.projectId && it.skillId == p1Subj2Skills[1].skillId }.uuid})"

        attachments.size() == 8
        attachments.findAll { it.projectId == p1.projectId }.size() == 4
        List<Attachment> newAttachments = attachments.findAll { it.projectId == projToCopy.projectId }
        newAttachments.size() == 4

        List<String> copiedDescriptions = newAttachments.collect( {"Here is a [Link](/api/download/${it.uuid})".toString() })
        copiedDescriptions.contains(copyProjSkill1.description)
        copiedDescriptions.contains(copyProjSkill2.description)
        copiedDescriptions.contains(copyProjSkill3.description)
        copiedDescriptions.contains(copyProjSkill4.description)

        newAttachments.each {
            assert it.projectId == projToCopy.projectId
        }
    }

    def "copy a project that has attachments in subject's description - attachments should be copied"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        String contents = 'Test is a test'
        String attachmentHref = attachFileAndReturnHref(p1.projectId, contents)

        def p1subj1 = createSubject(1, 1)
        p1subj1.description = "Here is a [Link](${attachmentHref})".toString()
        skillsService.createSubject(p1subj1)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def origSubj = skillsService.getSubject([projectId: p1.projectId, subjectId: p1subj1.subjectId])
        def copySubj = skillsService.getSubject([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId])

        List<Attachment> attachments = attachmentRepo.findAll()
        then:
        origSubj.description == "Here is a [Link](${attachmentHref})"

        attachments.size() == 2
        Attachment originalAttachment = attachments.find {  attachmentHref.contains(it.uuid)}
        Attachment newAttachment = attachments.find { !attachmentHref.contains(it.uuid)}

        originalAttachment.projectId == p1.projectId

        newAttachment.projectId == projToCopy.projectId
        copySubj.description == "Here is a [Link](/api/download/${newAttachment.uuid})"

        SkillsService.FileAndHeaders fileAndHeaders = skillsService.downloadAttachment("/api/download/${newAttachment.uuid}")
        File file = fileAndHeaders.file
        file
        file.bytes == contents.getBytes()
    }

    def "copy a project that has attachments in badge's description - attachments should be copied"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        String contents = 'Test is a test'
        String attachmentHref = attachFileAndReturnHref(p1.projectId, contents)

        def badge = SkillsFactory.createBadge(1, 50)
        badge.description = "Here is a [Link](${attachmentHref})".toString()
        skillsService.createBadge(badge)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def origBadge = skillsService.getBadge([projectId: p1.projectId, badgeId: badge.badgeId])
        def copyBadge = skillsService.getBadge([projectId: projToCopy.projectId, badgeId: badge.badgeId])

        List<Attachment> attachments = attachmentRepo.findAll()
        then:
        origBadge.description == "Here is a [Link](${attachmentHref})"

        attachments.size() == 2
        Attachment originalAttachment = attachments.find {  attachmentHref.contains(it.uuid)}
        Attachment newAttachment = attachments.find { !attachmentHref.contains(it.uuid)}

        originalAttachment.projectId == p1.projectId

        newAttachment.projectId == projToCopy.projectId
        copyBadge.description == "Here is a [Link](/api/download/${newAttachment.uuid})"

        SkillsService.FileAndHeaders fileAndHeaders = skillsService.downloadAttachment("/api/download/${newAttachment.uuid}")
        File file = fileAndHeaders.file
        file
        file.bytes == contents.getBytes()
    }

    def "copied percent-based levels do not have from/to points set in DB"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        Integer projRefId = projDefRepo.findByProjectId(projToCopy.projectId).id
        List<LevelDef> levelDefList = levelDefRepo.findAllByProjectRefId(projRefId).sort { it.level}
        then:
        levelDefList.size() == 5
        !levelDefList[0].pointsFrom
        !levelDefList[0].pointsTo
        !levelDefList[1].pointsFrom
        !levelDefList[1].pointsTo
        !levelDefList[2].pointsFrom
        !levelDefList[2].pointsTo
        !levelDefList[3].pointsFrom
        !levelDefList[3].pointsTo
        !levelDefList[4].pointsFrom
        !levelDefList[4].pointsTo
    }

    def "user actions history"() {
        def p1 = createProject(1)
        p1.projectId = "newproject"
        def p1subj1 = createSubject(1, 1)
        p1subj1.projectId = p1.projectId
        def p1Skills = createSkills(1, 1, 1, 100)
        p1Skills[0].projectId = p1.projectId
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        when:
        def projToCopy = createProject(2)
        projToCopy.projectId = "newprojectcopy"
        skillsService.copyProject(p1.projectId, projToCopy)

        def origProj = skillsService.getUserActionsForProject(p1.projectId, 10, 1, "projectId", true)
        def copyProj = skillsService.getUserActionsForProject(projToCopy.projectId, 10, 1, "projectId", true)
        then:
        origProj.count == 3
        origProj.totalCount == 3
        origProj.data.itemId.sort() == [p1.projectId, p1subj1.subjectId, p1Skills[0].skillId].sort()

        origProj.count == 3
        copyProj.totalCount == 3
        copyProj.data.itemId.sort() == [projToCopy.projectId, p1subj1.subjectId, p1Skills[0].skillId].sort()
    }

    def "copy project with an attachment in description, then remove the copy then copy again"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        String contents = 'Test is a test'
        String attachmentHref = attachFileAndReturnHref(p1.projectId, contents)

        def p1subj1 = createSubject(1, 1)
        p1subj1.description = "Here is a [Link](${attachmentHref})".toString()
        skillsService.createSubject(p1subj1)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def origSubj = skillsService.getSubject([projectId: p1.projectId, subjectId: p1subj1.subjectId])
        def copySubj = skillsService.getSubject([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId])

        List<Attachment> attachments = attachmentRepo.findAll()
        assert attachments.size() == 2
        Attachment originalAttachment = attachments.find {  attachmentHref.contains(it.uuid)}
        Attachment newAttachment = attachments.find { !attachmentHref.contains(it.uuid)}

        assert origSubj.description == "Here is a [Link](${attachmentHref})"
        assert copySubj.description == "Here is a [Link](/api/download/${newAttachment.uuid})"

        assert originalAttachment.projectId == p1.projectId
        assert newAttachment.projectId == projToCopy.projectId

        skillsService.deleteProject(projToCopy.projectId)

        List<Attachment> attachmentsAfterDelete = attachmentRepo.findAll()
        assert attachmentsAfterDelete.size() == 1
        attachmentsAfterDelete.find {  attachmentHref.contains(it.uuid)}

        def secondCopy = createProject(3)
        skillsService.copyProject(p1.projectId, secondCopy)

        def origSubjAfterSecondCopy = skillsService.getSubject([projectId: p1.projectId, subjectId: p1subj1.subjectId])
        def copySubjAfterSecondCopy = skillsService.getSubject([projectId: secondCopy.projectId, subjectId: p1subj1.subjectId])
        List<Attachment> attachmentsAfterSecondCopy = attachmentRepo.findAll()
        then:
        origSubjAfterSecondCopy.description == "Here is a [Link](${attachmentHref})"

        attachmentsAfterSecondCopy.size() == 2
        Attachment originalAttachmentAfterSecondCopy = attachmentsAfterSecondCopy.find {  attachmentHref.contains(it.uuid)}
        Attachment newAttachmentAfterSecondCopy = attachmentsAfterSecondCopy.find { !attachmentHref.contains(it.uuid)}

        originalAttachmentAfterSecondCopy.projectId == p1.projectId

        newAttachmentAfterSecondCopy.projectId == secondCopy.projectId
        copySubjAfterSecondCopy.description == "Here is a [Link](/api/download/${newAttachmentAfterSecondCopy.uuid})"

        SkillsService.FileAndHeaders fileAndHeaders = skillsService.downloadAttachment("/api/download/${newAttachmentAfterSecondCopy.uuid}")
        File file = fileAndHeaders.file
        file
        file.bytes == contents.getBytes()
    }

    static class Edge {
        String from
        String to
    }

    private void validateGraph(def graph, List<Edge> expectedGraphRel) {
        def skill0IdMap0_before = graph.nodes.collectEntries { [it.skillId, it.id] }
        assert graph.edges.collect { "${it.fromId}->${it.toId}" }.sort() == expectedGraphRel.collect {
            "${skill0IdMap0_before.get(it.from)}->${skill0IdMap0_before.get(it.to)}"
        }.sort()
    }
}
