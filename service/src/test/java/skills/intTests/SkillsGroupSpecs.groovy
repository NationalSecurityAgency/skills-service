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
import skills.intTests.utils.SkillsFactory

class SkillsGroupSpecs extends DefaultIntSpec {

    void "create and get initial SkillsGroup" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)

        when:
        def res = skillsService.getSkill(skillsGroup)

        then:
        res
        res.skillId == skillsGroup.skillId
        res.name == skillsGroup.name
        res.type == skillsGroup.type
        res.numSkillsInGroup == 0
        res.numSelfReportSkills == 0
        res.enabled == 'false'
    }

    void "cannot convert an existing Skill To a SkillsGroup " () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skill = SkillsFactory.createSkill(1)
        def skillsGroup = SkillsFactory.createSkillsGroup()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        when:
        skillsService.createSkill(skillsGroup)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("Cannot convert an existing Skill to a Skill Group, or existing Skill Group to Skill")
    }

    void "create and add skills to SkillsGroup" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        def res = skillsService.getSkill(skillsGroup)
        def groupSkills = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        def subjSkills = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        groupSkills.sort() { it.skillId }

        then:
        res
        res.skillId == skillsGroup.skillId
        res.name == skillsGroup.name
        res.type == skillsGroup.type
        res.numSkillsInGroup == groupSkills.size()
        res.numSelfReportSkills == 0
        res.enabled == 'false'
        
        groupSkills.size() == 3

        groupSkills.get(0).skillId == skills.get(0).skillId
        groupSkills.get(0).projectId == proj.projectId
        groupSkills.get(0).name == skills.get(0).name
        groupSkills.get(0).version == skills.get(0).version
        groupSkills.get(0).displayOrder == 1
        groupSkills.get(0).totalPoints == skills.get(0).pointIncrement * skills.get(0).numPerformToCompletion

        groupSkills.get(1).skillId == skills.get(1).skillId
        groupSkills.get(1).projectId == proj.projectId
        groupSkills.get(1).name == skills.get(1).name
        groupSkills.get(1).version == skills.get(1).version
        groupSkills.get(1).displayOrder == 2
        groupSkills.get(1).totalPoints == skills.get(1).pointIncrement * skills.get(1).numPerformToCompletion

        groupSkills.get(2).skillId == skills.get(2).skillId
        groupSkills.get(2).projectId == proj.projectId
        groupSkills.get(2).name == skills.get(2).name
        groupSkills.get(2).version == skills.get(2).version
        groupSkills.get(2).displayOrder == 3
        groupSkills.get(2).totalPoints == skills.get(2).pointIncrement * skills.get(2).numPerformToCompletion

        subjSkills
        subjSkills.size() == 1
        subjSkills.get(0).skillId == skillsGroup.skillId
        subjSkills.get(0).name == skillsGroup.name
        subjSkills.get(0).type == skillsGroup.type
        subjSkills.get(0).numSkillsInGroup == groupSkills.size()
        subjSkills.get(0).numSelfReportSkills == 0
        subjSkills.get(0).enabled == 'false'
    }

    void "create and then update SkillsGroup name" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)

        when:
        def res1 = skillsService.getSkill(skillsGroup)

        String origName = skillsGroup.name
        skillsGroup.name = 'New Group Name'
        skillsService.updateSkill(skillsGroup, null)

        def res2 = skillsService.getSkill(skillsGroup)

        then:
        res1
        res1.skillId == skillsGroup.skillId
        res1.name == origName
        res1.type == skillsGroup.type
        res1.numSkillsInGroup == 0
        res1.numSelfReportSkills == 0
        res1.enabled == 'false'

        res2
        res2.skillId == skillsGroup.skillId
        res2.name == skillsGroup.name
        res2.type == skillsGroup.type
        res2.numSkillsInGroup == 0
        res2.numSelfReportSkills == 0
        res2.enabled == 'false'
    }

    void "create and then update SkillsGroup ID" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)

        when:
        def res1 = skillsService.getSkill(skillsGroup)

        String origId = skillsGroup.skillId
        skillsGroup.id = 'NewId'
        skillsService.updateSkill(skillsGroup, null)

        def res2 = skillsService.getSkill(skillsGroup)

        then:
        res1
        res1.skillId == origId
        res1.name == skillsGroup.name
        res1.type == skillsGroup.type
        res1.numSkillsInGroup == 0
        res1.numSelfReportSkills == 0
        res1.enabled == 'false'

        res2
        res2.skillId == skillsGroup.skillId
        res2.name == skillsGroup.name
        res2.type == skillsGroup.type
        res2.numSkillsInGroup == 0
        res2.numSelfReportSkills == 0
        res2.enabled == 'false'
    }

    void "create and add more than one skill with same total point values to SkillsGroup, then successfully enable" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        def res = skillsService.getSkill(skillsGroup)
        def groupSkills = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        def subjSkills = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        groupSkills.sort() { it.skillId }

        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)
        def res2 = skillsService.getSkill(skillsGroup)

        then:
        res
        res.skillId == skillsGroup.skillId
        res.name == skillsGroup.name
        res.type == skillsGroup.type
        res.numSkillsInGroup == groupSkills.size()
        res.numSelfReportSkills == 0
        res.enabled == 'false'

        groupSkills.size() == 3

        groupSkills.get(0).skillId == skills.get(0).skillId
        groupSkills.get(0).projectId == proj.projectId
        groupSkills.get(0).name == skills.get(0).name
        groupSkills.get(0).version == skills.get(0).version
        groupSkills.get(0).displayOrder == 1
        groupSkills.get(0).totalPoints == skills.get(0).pointIncrement * skills.get(0).numPerformToCompletion

        groupSkills.get(1).skillId == skills.get(1).skillId
        groupSkills.get(1).projectId == proj.projectId
        groupSkills.get(1).name == skills.get(1).name
        groupSkills.get(1).version == skills.get(1).version
        groupSkills.get(1).displayOrder == 2
        groupSkills.get(1).totalPoints == skills.get(1).pointIncrement * skills.get(1).numPerformToCompletion

        groupSkills.get(2).skillId == skills.get(2).skillId
        groupSkills.get(2).projectId == proj.projectId
        groupSkills.get(2).name == skills.get(2).name
        groupSkills.get(2).version == skills.get(2).version
        groupSkills.get(2).displayOrder == 3
        groupSkills.get(2).totalPoints == skills.get(2).pointIncrement * skills.get(2).numPerformToCompletion

        subjSkills
        subjSkills.size() == 1
        subjSkills.get(0).skillId == skillsGroup.skillId
        subjSkills.get(0).name == skillsGroup.name
        subjSkills.get(0).type == skillsGroup.type
        subjSkills.get(0).numSkillsInGroup == groupSkills.size()
        subjSkills.get(0).numSelfReportSkills == 0
        subjSkills.get(0).enabled == 'false'

        res2
        res2.skillId == skillsGroup.skillId
        res2.name == skillsGroup.name
        res2.type == skillsGroup.type
        res2.numSkillsInGroup == groupSkills.size()
        res2.numSelfReportSkills == 0
        res2.enabled == 'true'
    }

    void "can enable a SkillsGroup with only 1 required child skill, but must have at least 2 child skills" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)
        skillsGroup.numSkillsRequired = 1

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)
        def res = skillsService.getSkill(skillsGroup)
        def groupSkills = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        groupSkills.sort() { it.skillId }

        then:
        res
        res.skillId == skillsGroup.skillId
        res.name == skillsGroup.name
        res.type == skillsGroup.type
        res.numSkillsInGroup == groupSkills.size()
        res.numSelfReportSkills == 0
        res.numSkillsRequired == 1
        res.enabled == 'true'
        res.totalPoints == groupSkills[0].totalPoints

        groupSkills.size() == 2

        groupSkills.get(0).skillId == skills.get(0).skillId
        groupSkills.get(0).projectId == proj.projectId
        groupSkills.get(0).name == skills.get(0).name
        groupSkills.get(0).version == skills.get(0).version
        groupSkills.get(0).displayOrder == 1
        groupSkills.get(0).totalPoints == skills.get(0).pointIncrement * skills.get(0).numPerformToCompletion

        groupSkills.get(1).skillId == skills.get(1).skillId
        groupSkills.get(1).projectId == proj.projectId
        groupSkills.get(1).name == skills.get(1).name
        groupSkills.get(1).version == skills.get(1).version
        groupSkills.get(1).displayOrder == 2
        groupSkills.get(1).totalPoints == skills.get(1).pointIncrement * skills.get(1).numPerformToCompletion
    }

    void "cannot enable a SkillsGroup with < 2 child skills" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("A Skill Group must have at least 2 skills in order to be enabled.")
    }

    void "cannot enable a SkillsGroup with < 1 required skill" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)
        skillsGroup.numSkillsRequired = 0

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("A Skill Group must have at least 1 required skill in order to be enabled.")
    }

    void "cannot enable a SkillsGroup when not all skills are required, and not all have the same # of points" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2)
        def skill3WithDiffNumPoints = SkillsFactory.createSkill(1, 1, 3)
        skill3WithDiffNumPoints.pointIncrement = 100
        skills.add(skill3WithDiffNumPoints)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)
        skillsGroup.numSkillsRequired = skills.size()-1

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("All skills that belong to the Skill Group must have the same total value when all skills are not required to be completed.")
    }

    void "cannot update a child skill's points to a value different than the other child skills when not all skills are required" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)
        skillsGroup.numSkillsRequired = skills.size() - 1

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)
        def res = skillsService.getSkill(skillsGroup)
        def groupSkills = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        groupSkills.sort() { it.skillId }

        def skillWithDifferentPoints = skills.first()
        skillWithDifferentPoints.pointIncrement = 321
        skillsService.updateSkill(skillWithDifferentPoints, null)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("All skills that belong to the Skill Group must have the same total value when all skills are not required to be completed.")
    }

    void "can enable a SkillsGroup when all skills are required, and not all skills have the same # of points" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2)
        def skill3WithDiffNumPoints = SkillsFactory.createSkill(1, 1, 3)
        skill3WithDiffNumPoints.pointIncrement = 100
        skills.add(skill3WithDiffNumPoints)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)
        skillsGroup.numSkillsRequired = skills.size()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)
        def res = skillsService.getSkill(skillsGroup)
        def groupSkills = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        groupSkills.sort() { it.skillId }

        then:
        res
        res.skillId == skillsGroup.skillId
        res.name == skillsGroup.name
        res.type == skillsGroup.type
        res.numSkillsInGroup == groupSkills.size()
        res.numSelfReportSkills == 0
        res.numSkillsRequired == skills.size()
        res.enabled == 'true'
        res.totalPoints == groupSkills[0].totalPoints + groupSkills[1].totalPoints + groupSkills[2].totalPoints

        groupSkills.size() == 3

        groupSkills.get(0).skillId == skills.get(0).skillId
        groupSkills.get(0).projectId == proj.projectId
        groupSkills.get(0).name == skills.get(0).name
        groupSkills.get(0).version == skills.get(0).version
        groupSkills.get(0).displayOrder == 1
        groupSkills.get(0).totalPoints == skills.get(0).pointIncrement * skills.get(0).numPerformToCompletion

        groupSkills.get(1).skillId == skills.get(1).skillId
        groupSkills.get(1).projectId == proj.projectId
        groupSkills.get(1).name == skills.get(1).name
        groupSkills.get(1).version == skills.get(1).version
        groupSkills.get(1).displayOrder == 2
        groupSkills.get(1).totalPoints == skills.get(1).pointIncrement * skills.get(1).numPerformToCompletion

        groupSkills.get(2).skillId == skills.get(2).skillId
        groupSkills.get(2).projectId == proj.projectId
        groupSkills.get(2).name == skills.get(2).name
        groupSkills.get(2).version == skills.get(2).version
        groupSkills.get(2).displayOrder == 3
        groupSkills.get(2).totalPoints == 100 * skills.get(2).numPerformToCompletion
    }

    void "can enable a SkillsGroup when numSkillsRequired == -1, and not all skills have the same # of points" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2)
        def skill3WithDiffNumPoints = SkillsFactory.createSkill(1, 1, 3)
        skill3WithDiffNumPoints.pointIncrement = 100
        skills.add(skill3WithDiffNumPoints)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)
        skillsGroup.numSkillsRequired = -1

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)
        def res = skillsService.getSkill(skillsGroup)
        def groupSkills = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        groupSkills.sort() { it.skillId }

        then:
        res
        res.skillId == skillsGroup.skillId
        res.name == skillsGroup.name
        res.type == skillsGroup.type
        res.numSkillsInGroup == groupSkills.size()
        res.numSelfReportSkills == 0
        res.numSkillsRequired == -1
        res.enabled == 'true'
        res.totalPoints == groupSkills[0].totalPoints + groupSkills[1].totalPoints + groupSkills[2].totalPoints

        groupSkills.size() == 3

        groupSkills.get(0).skillId == skills.get(0).skillId
        groupSkills.get(0).projectId == proj.projectId
        groupSkills.get(0).name == skills.get(0).name
        groupSkills.get(0).version == skills.get(0).version
        groupSkills.get(0).displayOrder == 1
        groupSkills.get(0).totalPoints == skills.get(0).pointIncrement * skills.get(0).numPerformToCompletion

        groupSkills.get(1).skillId == skills.get(1).skillId
        groupSkills.get(1).projectId == proj.projectId
        groupSkills.get(1).name == skills.get(1).name
        groupSkills.get(1).version == skills.get(1).version
        groupSkills.get(1).displayOrder == 2
        groupSkills.get(1).totalPoints == skills.get(1).pointIncrement * skills.get(1).numPerformToCompletion

        groupSkills.get(2).skillId == skills.get(2).skillId
        groupSkills.get(2).projectId == proj.projectId
        groupSkills.get(2).name == skills.get(2).name
        groupSkills.get(2).version == skills.get(2).version
        groupSkills.get(2).displayOrder == 3
        groupSkills.get(2).totalPoints == 100 * skills.get(2).numPerformToCompletion
    }

    void "can enable a SkillsGroup when not all skills are required, but all skills have the same # of points" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)
        skillsGroup.numSkillsRequired = skills.size() - 1

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)
        def res = skillsService.getSkill(skillsGroup)
        def groupSkills = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        groupSkills.sort() { it.skillId }

        then:
        res
        res.skillId == skillsGroup.skillId
        res.name == skillsGroup.name
        res.type == skillsGroup.type
        res.numSkillsInGroup == groupSkills.size()
        res.numSelfReportSkills == 0
        res.numSkillsRequired == 2
        res.totalPoints == (skills.get(0).pointIncrement * skills.get(0).numPerformToCompletion) * res.numSkillsRequired
        res.enabled == 'true'

        groupSkills.size() == 3

        groupSkills.get(0).skillId == skills.get(0).skillId
        groupSkills.get(0).projectId == proj.projectId
        groupSkills.get(0).name == skills.get(0).name
        groupSkills.get(0).version == skills.get(0).version
        groupSkills.get(0).displayOrder == 1
        groupSkills.get(0).totalPoints == skills.get(0).pointIncrement * skills.get(0).numPerformToCompletion

        groupSkills.get(1).skillId == skills.get(1).skillId
        groupSkills.get(1).projectId == proj.projectId
        groupSkills.get(1).name == skills.get(1).name
        groupSkills.get(1).version == skills.get(1).version
        groupSkills.get(1).displayOrder == 2
        groupSkills.get(1).totalPoints == skills.get(0).pointIncrement * skills.get(0).numPerformToCompletion

        groupSkills.get(2).skillId == skills.get(2).skillId
        groupSkills.get(2).projectId == proj.projectId
        groupSkills.get(2).name == skills.get(2).name
        groupSkills.get(2).version == skills.get(2).version
        groupSkills.get(2).displayOrder == 3
        groupSkills.get(2).totalPoints == skills.get(0).pointIncrement * skills.get(0).numPerformToCompletion
    }

    def "delete SkillsGroup"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)
        skillsGroup.enabled = 'true'
        skillsGroup.numSkillsRequired = skills.size() - 1

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        def subjSkillsBefore = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        List<Boolean> idsExistBefore = skills.collect { skillsService.doesEntityExist(proj.projectId, it.skillId) }
        idsExistBefore.add(skillsService.doesEntityExist(proj.projectId, skillsGroupId))

        when:
        skillsService.deleteSkill(skillsGroup)
        def subjSkillsAfter = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        List<Boolean> idsExistAfter = skills.collect { skillsService.doesEntityExist(proj.projectId, it.skillId) }
        idsExistAfter.add(skillsService.doesEntityExist(proj.projectId, skillsGroupId))

        then:
        subjSkillsBefore
        idsExistBefore.every { it }
        subjSkillsAfter.every { !it }
    }

}
