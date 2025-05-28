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

    def "can enable a disabled skill that is part of a disabled group"() {
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
}
