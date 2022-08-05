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

import groovy.json.JsonOutput
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.settings.Settings
import skills.skillLoading.RankingLoader
import skills.storage.model.SkillDef
import spock.lang.IgnoreRest

import static skills.intTests.utils.SkillsFactory.*

class CopyProjectSpecs extends DefaultIntSpec {

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

        skillsService.assignDependency([projectId: p1.projectId, skillId: p1Skills.get(0).skillId, dependentSkillId: p1Skills.get(2).skillId])
        skillsService.assignDependency([projectId: p1.projectId, skillId: p1Skills.get(2).skillId, dependentSkillId: p1SkillsSubj2.get(2).skillId])

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
        copiedSettings.projectId == [projToCopy.projectId, projToCopy.projectId]
        copiedSettings.setting == [RankingLoader.PROJ_ADMINS_RANK_AND_LEADERBOARD_OPT_OUT_PREF, "project.displayName"]
        copiedSettings.value == ["true", "blah"]
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
        copiedSettings.projectId == [projToCopy.projectId, projToCopy.projectId]
        copiedSettings.setting == [RankingLoader.PROJ_ADMINS_RANK_AND_LEADERBOARD_OPT_OUT_PREF, "project.displayName"]
        copiedSettings.value == ["true", "blah"]
    }

    def "skill attributes are properly copied - skill under a subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 22, 0, 12, 512, 18,)
        skill1.description = "blah blah blah"
        skill1.helpUrl = "/ok/that/is/good"
        skill1.selfReportingType = SkillDef.SelfReportingType.Approval
        skill1.justificationRequired = true

        def skill2 = createSkill(1, 1, 23, 0, 13, 458, 55,)
        skill2.description = "something else"
        skill2.helpUrl = "http://www.djleaje.org"
        skill2.selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skill2.justificationRequired = false
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [skill1, skill2])

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def originalSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill1.skillId])
        def originalSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill2.skillId])

        def copiedSkill1 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: skill1.skillId])
        def copiedSkill2 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: skill2.skillId])
        then:
        copiedSkill1.skillId == originalSkill1.skillId
        copiedSkill1.projectId == projToCopy.projectId
        copiedSkill1.name == originalSkill1.name
        copiedSkill1.version == originalSkill1.version
        copiedSkill1.displayOrder == originalSkill1.displayOrder
        copiedSkill1.totalPoints == originalSkill1.totalPoints
        copiedSkill1.pointIncrement == originalSkill1.pointIncrement
        copiedSkill1.pointIncrementInterval == originalSkill1.pointIncrementInterval
        copiedSkill1.numMaxOccurrencesIncrementInterval == originalSkill1.numMaxOccurrencesIncrementInterval
        copiedSkill1.numPerformToCompletion == originalSkill1.numPerformToCompletion
        copiedSkill1.type == originalSkill1.type
        copiedSkill1.selfReportingType == originalSkill1.selfReportingType
        copiedSkill1.enabled == originalSkill1.enabled
        copiedSkill1.justificationRequired == originalSkill1.justificationRequired
        copiedSkill1.numMaxOccurrencesIncrementInterval == originalSkill1.numMaxOccurrencesIncrementInterval
        copiedSkill1.description == originalSkill1.description
        copiedSkill1.helpUrl == originalSkill1.helpUrl
        !copiedSkill1.groupName
        !copiedSkill1.groupId
        !copiedSkill1.readOnly
        !copiedSkill1.reusedSkill
        !copiedSkill1.thisSkillWasReusedElsewhere

        copiedSkill2.skillId == originalSkill2.skillId
        copiedSkill2.projectId == projToCopy.projectId
        copiedSkill2.name == originalSkill2.name
        copiedSkill2.version == originalSkill2.version
        copiedSkill2.displayOrder == originalSkill2.displayOrder
        copiedSkill2.totalPoints == originalSkill2.totalPoints
        copiedSkill2.pointIncrement == originalSkill2.pointIncrement
        copiedSkill2.pointIncrementInterval == originalSkill2.pointIncrementInterval
        copiedSkill2.numMaxOccurrencesIncrementInterval == originalSkill2.numMaxOccurrencesIncrementInterval
        copiedSkill2.numPerformToCompletion == originalSkill2.numPerformToCompletion
        copiedSkill2.type == originalSkill2.type
        copiedSkill2.selfReportingType == originalSkill2.selfReportingType
        copiedSkill2.enabled == originalSkill2.enabled
        copiedSkill2.justificationRequired == originalSkill2.justificationRequired
        copiedSkill2.numMaxOccurrencesIncrementInterval == originalSkill2.numMaxOccurrencesIncrementInterval
        copiedSkill2.description == originalSkill2.description
        copiedSkill2.helpUrl == originalSkill2.helpUrl
        !copiedSkill2.groupName
        !copiedSkill2.groupId
        !copiedSkill2.readOnly
        !copiedSkill2.reusedSkill
        !copiedSkill2.thisSkillWasReusedElsewhere
    }

    def "skill attributes are properly copied - skill under a group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 22, 0, 12, 512, 18,)
        skill1.description = "blah blah blah"
        skill1.helpUrl = "/ok/that/is/good"
        skill1.selfReportingType = SkillDef.SelfReportingType.Approval
        skill1.justificationRequired = true

        def skill2 = createSkill(1, 1, 23, 0, 13, 458, 55,)
        skill2.description = "something else"
        skill2.helpUrl = "http://www.djleaje.org"
        skill2.selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skill2.justificationRequired = false

        def group = createSkillsGroup(1, 1, 4)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [group])
        skillsService.assignSkillToSkillsGroup(group.skillId, skill1)
        skillsService.assignSkillToSkillsGroup(group.skillId, skill2)

        def group2 = createSkillsGroup(1, 1, 5)
        def skill3 = createSkill(1, 1, 24, 0, 13, 458, 55,)
        skill3.description = "something else"
        skill3.helpUrl = "http://www.djleaje.org"
        skill3.selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skill3.justificationRequired = false
        skillsService.createSkill(group2)
        skillsService.assignSkillToSkillsGroup(group2.skillId, skill3)


        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def originalSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill1.skillId])
        def originalSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill2.skillId])
        def originalSkill3 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill3.skillId])

        def copiedSkill1 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: skill1.skillId])
        def copiedSkill2 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: skill2.skillId])
        def copiedSkill3 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: skill3.skillId])
        then:
        copiedSkill1.skillId == originalSkill1.skillId
        copiedSkill1.projectId == projToCopy.projectId
        copiedSkill1.name == originalSkill1.name
        copiedSkill1.version == originalSkill1.version
        copiedSkill1.displayOrder == originalSkill1.displayOrder
        copiedSkill1.totalPoints == originalSkill1.totalPoints
        copiedSkill1.pointIncrement == originalSkill1.pointIncrement
        copiedSkill1.pointIncrementInterval == originalSkill1.pointIncrementInterval
        copiedSkill1.numMaxOccurrencesIncrementInterval == originalSkill1.numMaxOccurrencesIncrementInterval
        copiedSkill1.numPerformToCompletion == originalSkill1.numPerformToCompletion
        copiedSkill1.type == originalSkill1.type
        copiedSkill1.selfReportingType == originalSkill1.selfReportingType
        copiedSkill1.enabled == originalSkill1.enabled
        copiedSkill1.justificationRequired == originalSkill1.justificationRequired
        copiedSkill1.numMaxOccurrencesIncrementInterval == originalSkill1.numMaxOccurrencesIncrementInterval
        copiedSkill1.description == originalSkill1.description
        copiedSkill1.helpUrl == originalSkill1.helpUrl
        copiedSkill1.groupName == group.name
        copiedSkill1.groupId == group.skillId
        !copiedSkill1.readOnly
        !copiedSkill1.reusedSkill
        !copiedSkill1.thisSkillWasReusedElsewhere

        copiedSkill2.skillId == originalSkill2.skillId
        copiedSkill2.projectId == projToCopy.projectId
        copiedSkill2.name == originalSkill2.name
        copiedSkill2.version == originalSkill2.version
        copiedSkill2.displayOrder == originalSkill2.displayOrder
        copiedSkill2.totalPoints == originalSkill2.totalPoints
        copiedSkill2.pointIncrement == originalSkill2.pointIncrement
        copiedSkill2.pointIncrementInterval == originalSkill2.pointIncrementInterval
        copiedSkill2.numMaxOccurrencesIncrementInterval == originalSkill2.numMaxOccurrencesIncrementInterval
        copiedSkill2.numPerformToCompletion == originalSkill2.numPerformToCompletion
        copiedSkill2.type == originalSkill2.type
        copiedSkill2.selfReportingType == originalSkill2.selfReportingType
        copiedSkill2.enabled == originalSkill2.enabled
        copiedSkill2.justificationRequired == originalSkill2.justificationRequired
        copiedSkill2.numMaxOccurrencesIncrementInterval == originalSkill2.numMaxOccurrencesIncrementInterval
        copiedSkill2.description == originalSkill2.description
        copiedSkill2.helpUrl == originalSkill2.helpUrl
        copiedSkill2.groupName == group.name
        copiedSkill2.groupId == group.skillId
        !copiedSkill2.readOnly
        !copiedSkill2.reusedSkill
        !copiedSkill2.thisSkillWasReusedElsewhere

        copiedSkill3.skillId == originalSkill3.skillId
        copiedSkill3.projectId == projToCopy.projectId
        copiedSkill3.name == originalSkill3.name
        copiedSkill3.version == originalSkill3.version
        copiedSkill3.displayOrder == originalSkill3.displayOrder
        copiedSkill3.totalPoints == originalSkill3.totalPoints
        copiedSkill3.pointIncrement == originalSkill3.pointIncrement
        copiedSkill3.pointIncrementInterval == originalSkill3.pointIncrementInterval
        copiedSkill3.numMaxOccurrencesIncrementInterval == originalSkill3.numMaxOccurrencesIncrementInterval
        copiedSkill3.numPerformToCompletion == originalSkill3.numPerformToCompletion
        copiedSkill3.type == originalSkill3.type
        copiedSkill3.selfReportingType == originalSkill3.selfReportingType
        copiedSkill3.enabled == originalSkill3.enabled
        copiedSkill3.justificationRequired == originalSkill3.justificationRequired
        copiedSkill3.numMaxOccurrencesIncrementInterval == originalSkill3.numMaxOccurrencesIncrementInterval
        copiedSkill3.description == originalSkill3.description
        copiedSkill3.helpUrl == originalSkill3.helpUrl
        copiedSkill3.groupName == group2.name
        copiedSkill3.groupId == group2.skillId
        !copiedSkill3.readOnly
        !copiedSkill3.reusedSkill
        !copiedSkill3.thisSkillWasReusedElsewhere
    }

    def "skill display order is copied"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.moveSkillDown(p1Skills[0])

        when:
        def originalSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId).sort { it.displayOrder }
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def copiedSkills = skillsService.getSkillsForSubject(projToCopy.projectId, p1subj1.subjectId).sort { it.displayOrder }

        then:
        originalSkills.skillId == [p1Skills[1].skillId, p1Skills[0].skillId, p1Skills[2].skillId]
        copiedSkills.skillId == [p1Skills[1].skillId, p1Skills[0].skillId, p1Skills[2].skillId]
        copiedSkills.displayOrder == [1, 2, 3]
    }

    def "copied skill version must be reset to 0"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills[0].version = 0
        p1Skills[1].version = 1
        p1Skills[2].version = 2
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        when:
        def originalSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId).sort { it.displayOrder }
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def copiedSkills = skillsService.getSkillsForSubject(projToCopy.projectId, p1subj1.subjectId).sort { it.displayOrder }

        then:
        originalSkills.version == [0, 1, 2]
        copiedSkills.version == [0, 0, 0]
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

        skillsService.assignDependency([projectId: p1.projectId, skillId: p1Skills.get(0).skillId, dependentSkillId: p1Skills.get(2).skillId])
        skillsService.assignDependency([projectId: p1.projectId, skillId: p1Skills.get(2).skillId, dependentSkillId: p1Skills.get(5).skillId])
        skillsService.assignDependency([projectId: p1.projectId, skillId: p1Skills.get(3).skillId, dependentSkillId: p1SkillsSubj2.get(0).skillId])
        skillsService.assignDependency([projectId: p1.projectId, skillId: p1Skills.get(3).skillId, dependentSkillId: p1SkillsSubj2.get(4).skillId])

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

    def "validate reused skills were copied"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        p1Skills[0].description = "blah blah blah"
        p1Skills[0].helpUrl = "/ok/that/is/good"
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        p1Skills[0].justificationRequired = true

        p1Skills[1].description = "something else"
        p1Skills[1].helpUrl = "http://www.djleaje.org"
        p1Skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        p1Skills[1].justificationRequired = false

        def group1 = createSkillsGroup(1, 1, 22)
        skillsService.createSubject(p1subj1)
        skillsService.createSkills([p1Skills[0..3], group1].flatten())
        p1Skills[4..9].each {
            skillsService.assignSkillToSkillsGroup(group1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        def group3 = createSkillsGroup(1, 2, 33)
        skillsService.createSubject(p1subj2)
        skillsService.createSkills([group3])

        // group in the same subject
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, group1.skillId)
        // different subject
        skillsService.reuseSkills(p1.projectId, [p1Skills[1].skillId], p1subj2.subjectId)
        // group in a different subject
        skillsService.reuseSkills(p1.projectId, [p1Skills[7].skillId], p1subj2.subjectId, group3.skillId)

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def copiedGroup1Skills = skillsService.getSkillsForGroup(projToCopy.projectId, group1.skillId)
        def originalReusedSkillInGroup1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def copiedReusedSkillInGroup1 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])

        def copiedSubj2Skills = skillsService.getSkillsForSubject(projToCopy.projectId, p1subj2.subjectId)
        def copiedGroup3Skills = skillsService.getSkillsForGroup(projToCopy.projectId, group3.skillId)

        def originalReusedSkillInSubj2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])
        def copiedReusedSkillInSubj2 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0)])

        println JsonOutput.prettyPrint(JsonOutput.toJson(copiedReusedSkillInGroup1))
        then:
        copiedGroup1Skills.skillId == [p1Skills[4..9].skillId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)].flatten()

        copiedReusedSkillInGroup1.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        copiedReusedSkillInGroup1.name == p1Skills[0].name
        copiedReusedSkillInGroup1.reusedSkill == true
        copiedReusedSkillInGroup1.groupId == group1.skillId
        copiedReusedSkillInGroup1.pointIncrement == originalReusedSkillInGroup1.pointIncrement
        copiedReusedSkillInGroup1.pointIncrementInterval == originalReusedSkillInGroup1.pointIncrementInterval
        copiedReusedSkillInGroup1.numMaxOccurrencesIncrementInterval == originalReusedSkillInGroup1.numMaxOccurrencesIncrementInterval
        copiedReusedSkillInGroup1.numPerformToCompletion == originalReusedSkillInGroup1.numPerformToCompletion
        copiedReusedSkillInGroup1.type == originalReusedSkillInGroup1.type
        copiedReusedSkillInGroup1.enabled == true
        copiedReusedSkillInGroup1.description == originalReusedSkillInGroup1.description
        copiedReusedSkillInGroup1.helpUrl == originalReusedSkillInGroup1.helpUrl
        copiedReusedSkillInGroup1.selfReportingType == originalReusedSkillInGroup1.selfReportingType
        copiedReusedSkillInGroup1.justificationRequired == originalReusedSkillInGroup1.justificationRequired

        copiedSubj2Skills.skillId == [group3.skillId, SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0)]
        copiedGroup3Skills.skillId == [SkillReuseIdUtil.addTag(p1Skills[7].skillId, 0)]

        copiedReusedSkillInSubj2.skillId == SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0)
        copiedReusedSkillInSubj2.name == p1Skills[1].name
        copiedReusedSkillInSubj2.reusedSkill == true
        !copiedReusedSkillInSubj2.groupId
        copiedReusedSkillInSubj2.pointIncrement == originalReusedSkillInSubj2.pointIncrement
        copiedReusedSkillInSubj2.pointIncrementInterval == originalReusedSkillInSubj2.pointIncrementInterval
        copiedReusedSkillInSubj2.numMaxOccurrencesIncrementInterval == originalReusedSkillInSubj2.numMaxOccurrencesIncrementInterval
        copiedReusedSkillInSubj2.numPerformToCompletion == originalReusedSkillInSubj2.numPerformToCompletion
        copiedReusedSkillInSubj2.type == originalReusedSkillInSubj2.type
        copiedReusedSkillInSubj2.enabled == true
        copiedReusedSkillInSubj2.description == originalReusedSkillInSubj2.description
        copiedReusedSkillInSubj2.helpUrl == originalReusedSkillInSubj2.helpUrl
        copiedReusedSkillInSubj2.selfReportingType == originalReusedSkillInSubj2.selfReportingType
        copiedReusedSkillInSubj2.justificationRequired == originalReusedSkillInSubj2.justificationRequired
    }

    def "do not copy disabled imported skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def gSkill1 = createSkill(2, 1, 10, 0, 50)
        def gSkill2 = createSkill(2, 1, 11, 0, 50)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill1)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill2)

        skillsService.bulkImportSkillsIntoGroupFromCatalog(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                [[projectId: p1.projectId, skillId: p1Skills[0].skillId]])
        skillsService.bulkImportSkillsFromCatalog(p2.projectId, p2subj1.subjectId,
                [[projectId: p1.projectId, skillId: p1Skills[1].skillId]])

        when:
        def projToCopy = createProject(3)
        skillsService.copyProject(p2.projectId, projToCopy)

        def origProjStats = skillsService.getProject(p2.projectId)
        def copiedProjStats = skillsService.getProject(projToCopy.projectId)

        def originalSubj = skillsService.getSkillsForSubject(p2.projectId, p2subj1.subjectId)
        def copiedSubj = skillsService.getSkillsForSubject(projToCopy.projectId, p2subj1.subjectId)

        def originalGroup = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)
        def copiedGroup = skillsService.getSkillsForGroup(projToCopy.projectId, p2skillsGroup.skillId)

        then:
        origProjStats.numSkills == 2
        origProjStats.numSkillsDisabled == 2

        copiedProjStats.numSkills == 2
        copiedProjStats.numSkillsDisabled == 0

        originalSubj.skillId == [p2skillsGroup.skillId, p1Skills[1].skillId]
        copiedSubj.skillId == [p2skillsGroup.skillId]

        originalGroup.skillId == [gSkill1.skillId, gSkill2.skillId, p1Skills[0].skillId]
        copiedGroup.skillId == [gSkill1.skillId, gSkill2.skillId]
    }

    def "do not copy finalized imported skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def gSkill1 = createSkill(2, 1, 10, 0, 50)
        def gSkill2 = createSkill(2, 1, 11, 0, 50)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill1)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill2)

        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                [[projectId: p1.projectId, skillId: p1Skills[0].skillId]])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId,
                [[projectId: p1.projectId, skillId: p1Skills[1].skillId]])

        when:
        def projToCopy = createProject(3)
        skillsService.copyProject(p2.projectId, projToCopy)

        def origProjStats = skillsService.getProject(p2.projectId)
        def copiedProjStats = skillsService.getProject(projToCopy.projectId)

        def originalSubj = skillsService.getSkillsForSubject(p2.projectId, p2subj1.subjectId)
        def copiedSubj = skillsService.getSkillsForSubject(projToCopy.projectId, p2subj1.subjectId)

        def originalGroup = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)
        def copiedGroup = skillsService.getSkillsForGroup(projToCopy.projectId, p2skillsGroup.skillId)

        then:
        origProjStats.numSkills == 4
        origProjStats.numSkillsDisabled == 0

        copiedProjStats.numSkills == 2
        copiedProjStats.numSkillsDisabled == 0

        originalSubj.skillId == [p2skillsGroup.skillId, p1Skills[1].skillId]
        copiedSubj.skillId == [p2skillsGroup.skillId]

        originalGroup.skillId == [gSkill1.skillId, gSkill2.skillId, p1Skills[0].skillId]
        copiedGroup.skillId == [gSkill1.skillId, gSkill2.skillId]
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
