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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException

import static skills.intTests.utils.SkillsFactory.*

class DisabledGroupSpecs extends DefaultIntSpec {

    def "can create an initially disabled skill to a disabled group"() {
        def proj = createProject()
        def subj = createSubject()
        def skillsGroup = createSkillsGroup()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        when:
        skillsGroup.enabled = false
        skillsService.createSkill(skillsGroup)
        def res = skillsService.getSkill(skillsGroup)

        then:
        res
        res.skillId == skillsGroup.skillId
        res.name == skillsGroup.name
        res.type == skillsGroup.type
        res.numSkillsInGroup == 0
        res.numSelfReportSkills == 0
        res.enabled == false
    }

    def "can add a disabled skill to a disabled group"() {
        def proj = createProject()
        def subj = createSubject()
        def skills = createSkills(3)
        def skillsGroup = createSkillsGroup(1, 1, 5)
        skillsGroup.enabled = false

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)

        when:
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skill.enabled = false
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }
        def res = skillsService.getSkill(skillsGroup)
        def groupSkills = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)

        then:
        res
        res.skillId == skillsGroup.skillId
        res.name == skillsGroup.name
        res.type == skillsGroup.type
        res.numSkillsInGroup == groupSkills.size()
        res.numSelfReportSkills == 0
        res.enabled == false

        groupSkills.size() == 3
        groupSkills.findAll { it.enabled == false }.size() == 3
    }

    def "cannot enable a disabled skill that is part of a disabled group"() {
        def proj = createProject()
        def subj = createSubject()
        def skills = createSkills(3)
        def skillsGroup = createSkillsGroup(1, 1, 5)
        skillsGroup.enabled = false

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)

        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skill.enabled = false
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }
        def res = skillsService.getSkill(skillsGroup)
        def groupSkills = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)

        when:
        skills[0].enabled = true
        skillsService.updateSkill(skills[0], skills[0].skillId)

        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Cannot enable Skill [${skills[0].skillId}] because it's SkillsGroup [${skillsGroupId}] is disabled")
    }

    def "cannot add an enabled skill to a disabled group"() {
        def proj = createProject()
        def subj = createSubject()
        def skillsGroup = createSkillsGroup(1, 1, 5)
        skillsGroup.enabled = false

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)

        when:
        String skillsGroupId = skillsGroup.skillId
        def skill = createSkill(1, 1, 1)
        skill.enabled = true
        skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)

        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Cannot enable Skill [${skill.skillId}] because it's SkillsGroup [${skillsGroupId}] is disabled")
    }

    def "cannot disable an already enabled group"() {
        def proj = createProject()
        def subj = createSubject()
        def skillsGroup = createSkillsGroup(1, 1, 5)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)

        when:
        skillsGroup.enabled = false
        skillsService.updateSkill(skillsGroup)
        then:

        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill [${skillsGroup.skillId}] has already been enabled and cannot be disabled.")
    }

    def "cannot get skill summary for a disabled group"() {
        def proj = createProject()
        def subj = createSubject()
        def skills = createSkills(3)
        def skillsGroup = createSkillsGroup(1, 1, 5)
        skillsGroup.enabled = false

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)

        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skill.enabled = false
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        skillsService.getSingleSkillSummaryForCurrentUser(proj.projectId, skillsGroupId)

        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.resBody.contains("Skill with id [${skillsGroup.skillId}] is not enabled")
    }

    def "disabled group from getSkill"() {
        def proj = createProject()
        def subj = createSubject()
        def skills = createSkills(3)
        def skillsGroup = createSkillsGroup(1, 1, 5)
        skillsGroup.enabled = false

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)

        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skill.enabled = false
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:

        def group = skillsService.getSkill(skillsGroup)
        skillsGroup.enabled = true
        skillsService.updateSkill(skillsGroup)
        def skillSummaryAfter = skillsService.getSingleSkillSummaryForCurrentUser(proj.projectId, skillsGroupId)
        def groupAfter = skillsService.getSkill(skillsGroup)
        then:

        group
        group.skillId == skillsGroupId
        group.name == skillsGroup.name
        group.type == skillsGroup.type
        group.numSkillsInGroup == skills.size()
        group.numSelfReportSkills == 0
        group.totalPoints == 0
        group.enabled == false

        groupAfter
        groupAfter.skillId == skillsGroupId
        groupAfter.name == skillsGroup.name
        groupAfter.type == skillsGroup.type
        groupAfter.numSkillsInGroup == skills.size()
        groupAfter.numSelfReportSkills == 0
        groupAfter.enabled == true
        groupAfter.totalPoints == 30

        skillSummaryAfter
        skillSummaryAfter.type == skillsGroup.type
        skillSummaryAfter.skillId == skillsGroupId
        skillSummaryAfter.totalPoints == 30
    }

    def "enabling a group with imported skills does not enabled the imported skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj2 = createSubject(2, 2)
        def p2skillsGroup = createSkillsGroup(2, 2, 10)
        p2skillsGroup.enabled = 'false'
        String skillsGroupId = p2skillsGroup.skillId
        def p2skills = createSkills(5, 2, 2, 100)
        p2skills.each { it.enabled = 'false' }
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj2, [p2skills, p2skillsGroup].flatten())

        def nonImportedChildSkill = createSkill(2, 2, 55)
        nonImportedChildSkill.enabled = 'false'
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, nonImportedChildSkill)

        skillsService.bulkImportSkillsIntoGroupFromCatalog(p2.projectId, p2subj2.subjectId, p2skillsGroup.skillId,
                p1skills.collect { [projectId: it.projectId, skillId: it.skillId] })

        when:

        def group = skillsService.getSkill(p2skillsGroup)
        def groupSkills = skillsService.getSkillsForGroup(p2.projectId, skillsGroupId)
        p2skillsGroup.enabled = true
        skillsService.updateSkill(p2skillsGroup)
        def skillSummaryAfter = skillsService.getSingleSkillSummaryForCurrentUser(p2.projectId, skillsGroupId)
        def groupAfter = skillsService.getSkill(p2skillsGroup)
        def groupSkillsAfter = skillsService.getSkillsForGroup(p2.projectId, skillsGroupId)
        then:

        group
        group.skillId == skillsGroupId
        group.name == p2skillsGroup.name
        group.type == p2skillsGroup.type
        group.numSkillsInGroup == 6
        group.numSelfReportSkills == 0
        group.totalPoints == 0
        group.enabled == false

        groupSkills
        groupSkills.size() == 6
        groupSkills.findAll { !it.enabled }.size() == 6
        groupSkills.findAll { !it.enabled && it.copiedFromProjectId == p1.projectId }.size() == 5

        groupAfter
        groupAfter.skillId == skillsGroupId
        groupAfter.name == p2skillsGroup.name
        groupAfter.type == p2skillsGroup.type
        groupAfter.numSkillsInGroup == 6
        groupAfter.numSelfReportSkills == 0
        groupAfter.enabled == true
        groupAfter.totalPoints == 10

        groupSkillsAfter
        groupSkillsAfter.size() == 6
        groupSkillsAfter.findAll { !it.enabled }.size() == 5
        groupSkillsAfter.findAll { !it.enabled && it.copiedFromProjectId == p1.projectId }.size() == 5

        skillSummaryAfter
        skillSummaryAfter.type == p2skillsGroup.type
        skillSummaryAfter.skillId == skillsGroupId
        skillSummaryAfter.totalPoints == 10
    }
}
