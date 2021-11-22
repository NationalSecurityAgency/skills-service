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
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.SkillRelDefRepo

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
        res.enabled == false
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
        exception.message.contains("Skill with id [skill1] with type [Skill] already exists! Requested to create skill with type of [SkillsGroup]")
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
        res.enabled == false
        
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
        subjSkills.get(0).enabled == false
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
        res1.enabled == false

        res2
        res2.skillId == skillsGroup.skillId
        res2.name == skillsGroup.name
        res2.type == skillsGroup.type
        res2.numSkillsInGroup == 0
        res2.numSelfReportSkills == 0
        res2.enabled == false
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
        res1.enabled == false

        res2
        res2.skillId == skillsGroup.skillId
        res2.name == skillsGroup.name
        res2.type == skillsGroup.type
        res2.numSkillsInGroup == 0
        res2.numSelfReportSkills == 0
        res2.enabled == false
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
        res.enabled == false

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
        subjSkills.get(0).enabled == false

        res2
        res2.skillId == skillsGroup.skillId
        res2.name == skillsGroup.name
        res2.type == skillsGroup.type
        res2.numSkillsInGroup == groupSkills.size()
        res2.numSelfReportSkills == 0
        res2.enabled == true
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
        res.enabled == true
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

    void "cannot update a SkillsGroup to have a different # of points when not all skills are required" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)
        skillsGroup.numSkillsRequired = skills.size()-1

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        when:

        def skill3WithDiffNumPoints = skills[2]
        skill3WithDiffNumPoints.pointIncrement = 100
        skillsService.updateSkill(skill3WithDiffNumPoints, null)

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
        res.enabled == true
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
        res.enabled == true
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
        res.enabled == true

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

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }
        skillsGroup.numSkillsRequired = skills.size() - 1
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

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
        !subjSkillsAfter
        idsExistAfter.every { !it }
    }

    def "delete subject with a SkillsGroup"() {
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
        skillsGroup.numSkillsRequired = skills.size() - 1
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        def subjSkillsBefore = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        List<Boolean> idsExistBefore = skills.collect { skillsService.doesEntityExist(proj.projectId, it.skillId) }
        idsExistBefore.add(skillsService.doesEntityExist(proj.projectId, skillsGroupId))

        when:
        skillsService.deleteSubject(subj)
        List<Boolean> idsExistAfter = skills.collect { skillsService.doesEntityExist(proj.projectId, it.skillId) }
        idsExistAfter.add(skillsService.doesEntityExist(proj.projectId, skillsGroupId))
        idsExistAfter.add(skillsService.doesEntityExist(proj.projectId, subj.subjectId))

        then:
        subjSkillsBefore
        idsExistBefore.every { it }
        idsExistAfter.every { !it }
    }

    def "delete SkillsGroup and a child skill and verify proper display order is maintained for both"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(7)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup1 = allSkills[0]
        skillsGroup1.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup1)
        String skillsGroup1Id = skillsGroup1.skillId
        def group1Children = allSkills[1..2]
        group1Children.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroup1Id, skill)
        }
        skillsGroup1.enabled = 'true'
        skillsService.updateSkill(skillsGroup1, null)

        def skillsGroup2 = allSkills[3]
        skillsGroup2.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup2)
        String skillsGroup2Id = skillsGroup2.skillId
        def group2Children = allSkills[4..6]
        group2Children.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroup2Id, skill)
        }
        skillsGroup2.enabled = 'true'
        skillsService.updateSkill(skillsGroup2, null)

        def subjSkillsBefore = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        def skillsGroup1DisplayOrderBefore = subjSkillsBefore.find { it.skillId == skillsGroup1Id }.displayOrder
        def skillsGroup2DisplayOrderBefore = subjSkillsBefore.find { it.skillId == skillsGroup2Id }.displayOrder

        def skillsGroup2ChildrenResBefore = skillsService.getSkillsForGroup(proj.projectId, skillsGroup2Id)

        when:
        skillsService.deleteSkill(skillsGroup1)
        def subjSkillsAfter = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        def skillsGroup2DisplayOrderAfter = subjSkillsAfter.find { it.skillId == skillsGroup2Id }.displayOrder

        skillsService.deleteSkill(group2Children[1])
        def skillsGroup2ChildrenResAfter = skillsService.getSkillsForGroup(proj.projectId, skillsGroup2Id)

        then:
        // skill group order
        skillsGroup1DisplayOrderBefore == 1
        skillsGroup2DisplayOrderBefore == 2
        skillsGroup2DisplayOrderAfter == 1

        // skills group 2 children order
        skillsGroup2ChildrenResBefore.size() == 3
        skillsGroup2ChildrenResBefore[0].skillId == group2Children[0].skillId
        skillsGroup2ChildrenResBefore[0].displayOrder == 1
        skillsGroup2ChildrenResBefore[1].skillId == group2Children[1].skillId
        skillsGroup2ChildrenResBefore[1].displayOrder == 2
        skillsGroup2ChildrenResBefore[2].skillId == group2Children[2].skillId
        skillsGroup2ChildrenResBefore[2].displayOrder == 3

        skillsGroup2ChildrenResAfter.size() == 2
        skillsGroup2ChildrenResAfter[0].skillId == group2Children[0].skillId
        skillsGroup2ChildrenResAfter[0].displayOrder == 1
        skillsGroup2ChildrenResAfter[1].skillId == group2Children[2].skillId
        skillsGroup2ChildrenResAfter[1].displayOrder == 2
    }

    def "validate totalPoints on SkillsGroup"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(4)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        def children = allSkills[1..3]

        int initialPoints = skillsService.getSkill(skillsGroup).totalPoints

        when:

        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, children[0])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, children[1])
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)
        int pointAfterSecondChild = skillsService.getSkill(skillsGroup).totalPoints

        skillsService.assignSkillToSkillsGroup(skillsGroupId, children[2])
        int pointAfterThirdChild = skillsService.getSkill(skillsGroup).totalPoints

        skillsService.deleteSkill(children[1])
        int pointAfterOneChildDeleted = skillsService.getSkill(skillsGroup).totalPoints

        skillsGroup.numSkillsRequired = 1
        skillsService.updateSkill(skillsGroup, null)
        int pointAfterNumSkillsRequiredReduced = skillsService.getSkill(skillsGroup).totalPoints

        then:
        initialPoints == 0
        pointAfterSecondChild == 20
        pointAfterThirdChild == 30
        pointAfterOneChildDeleted == 20
        pointAfterNumSkillsRequiredReduced == 10
    }

    def "group info is returned for child skill on skill endpoint"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsGroup.enabled = 'false'
        skillsService.createSkill(skillsGroup)
        def children = allSkills[1..2]

        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, children[0])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, children[1])

        when:

        def res = skillsService.getSkill(children[0])

        then:
        res
        res.enabled == false
        res.groupName == skillsGroup.name
        res.groupId == skillsGroup.skillId
    }

    def "totalPoints are returned for skills endpoint for disabled group, but not subjects or project"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        def children = allSkills[1..2]

        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, children[0])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, children[1])

        when:

        int skillsGroupPoints = skillsService.getSkill(skillsGroup).totalPoints
        def subjects = skillsService.getSubjects(proj.projectId)
        def projects = skillsService.getProjects()
        def subjSkills = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)

        then:
        subjSkills
        subjSkills.size() == 1
        subjSkills[0].totalPoints == 20
        skillsGroupPoints == 20

        subjects
        subjects.size() == 1
        subjects[0].totalPoints == 0

        projects
        projects.size() == 1
        projects[0].totalPoints == 0
    }

    def "totalPoints on are always included for enabled groups"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        def children = allSkills[1..2]

        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, children[0])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, children[1])

        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        when:

        int skillPoints = skillsService.getSkill(skillsGroup).totalPoints
        def subjects = skillsService.getSubjects(proj.projectId)
        def projects = skillsService.getProjects()
        def subjSkills = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)

        then:
        subjSkills
        subjSkills.size() == 1
        subjSkills[0].totalPoints == 20
        skillPoints == 20

        subjects
        subjects.size() == 1
        subjects[0].totalPoints == 20

        projects
        projects.size() == 1
        projects[0].totalPoints == 20
    }

    def "once a skills group is live it cannot have less than 2 skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroup1Id = skillsGroup.skillId
        def group1Children = allSkills[1..2]
        group1Children.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroup1Id, skill)
        }
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        when:
        // delete one of the two skills will cause the group to only have one skill left
        skillsService.deleteSkill(allSkills[2])

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("A Skill Group must have at least 2 skills in order to be enabled.")
    }

    def "when deleting a child skill, if the numRequired == child.size(), then set numRequired = -1"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(4)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroup1Id = skillsGroup.skillId
        def group1Children = allSkills[1..3]
        group1Children.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroup1Id, skill)
        }
        skillsGroup.enabled = 'true'
        skillsGroup.numSkillsRequired = 2
        skillsService.updateSkill(skillsGroup, null)
        int numSkillsRequiredBeforeDelete = skillsService.getSkill(skillsGroup).numSkillsRequired

        when:
        // delete one of the three skills will cause the group to only have two skills left, it was
        // decided that if deleting caused # skills to == numSkillsRequired to set numSkillsRequired to -1
        skillsService.deleteSkill(allSkills[2])
        int numSkillsRequiredAfterDelete = skillsService.getSkill(skillsGroup).numSkillsRequired

        then:
        numSkillsRequiredBeforeDelete == 2
        numSkillsRequiredAfterDelete == -1
    }

    def "cannot delete a child skill that would cause the group to have less than 2 skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroup1Id = skillsGroup.skillId
        def group1Children = allSkills[1..2]
        group1Children.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroup1Id, skill)
        }
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)
        int numSkillsRequiredBeforeDelete = skillsService.getSkill(skillsGroup).numSkillsRequired

        when:
        skillsService.deleteSkill(allSkills[2])

        then:
        numSkillsRequiredBeforeDelete == -1
        def exception = thrown(SkillsClientException)
        exception.message.contains("A Skill Group must have at least 2 skills in order to be enabled.")
    }

    def "can delete a child skill that would cause the group to have less than 2 skills when group is disabled"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        def group1Children = allSkills[1..2]
        group1Children.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }
        skillsGroup.enabled = 'false'
        skillsService.updateSkill(skillsGroup, null)

        when:
        group1Children.each { skill ->
            skillsService.deleteSkill(skill)
        }
        def groupSkillsAfterDelete = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)

        then:
        !groupSkillsAfterDelete
    }

    def "deleting child skill of a disabled group will update totalPoints for the group, but not subjects or project"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(4)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        def children = allSkills[1..3]

        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, children[0])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, children[1])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, children[2])

        int skillsGroupPointsBefore = skillsService.getSkill(skillsGroup).totalPoints
        def subjectsBefore = skillsService.getSubjects(proj.projectId)
        def projectsBefore = skillsService.getProjects()
        def subjSkillsBefore = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        when:

        skillsService.deleteSkill(children[1])

        int skillsGroupPointsAfter = skillsService.getSkill(skillsGroup).totalPoints
        def subjectsAfter = skillsService.getSubjects(proj.projectId)
        def projectsAfter = skillsService.getProjects()
        def subjSkillsAfter = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)

        then:
        subjSkillsBefore
        subjSkillsBefore.size() == 1
        subjSkillsBefore[0].totalPoints == 30
        skillsGroupPointsBefore == 30

        subjectsBefore
        subjectsBefore.size() == 1
        subjectsBefore[0].totalPoints == 0

        projectsBefore
        projectsBefore.size() == 1
        projectsBefore[0].totalPoints == 0


        subjSkillsAfter
        subjSkillsAfter.size() == 1
        subjSkillsAfter[0].totalPoints == 20
        skillsGroupPointsAfter == 20

        subjectsAfter
        subjectsAfter.size() == 1
        subjectsAfter[0].totalPoints == 0

        projectsAfter
        projectsAfter.size() == 1
        projectsAfter[0].totalPoints == 0
    }

    def "numGroups includes enabled groups and numSkills includes child skills for enabled groups child skills but does not include child skills of for disabled groups"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(7)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup1 = allSkills[0]
        skillsGroup1.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup1)
        def children1 = allSkills[1..2]

        String skillsGroupId1 = skillsGroup1.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId1, children1[0])
        skillsService.assignSkillToSkillsGroup(skillsGroupId1, children1[1])

        skillsGroup1.enabled = 'true'
        skillsService.updateSkill(skillsGroup1, null)

        def skillsGroup2 = allSkills[3]
        skillsGroup2.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup2)
        def children2 = allSkills[4..5]

        String skillsGroupId2 = skillsGroup2.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId2, children2[0])
        skillsService.assignSkillToSkillsGroup(skillsGroupId2, children2[1])

        skillsGroup2.enabled = 'false'
        skillsService.updateSkill(skillsGroup2, null)


        skillsService.createSkill(allSkills[6])  // regular skill

        when:

        def subjects = skillsService.getSubjects(proj.projectId)
        def projects = skillsService.getProjects()

        then:

        subjects
        subjects.size() == 1
        subjects[0].numSkills == 3  // two group child skills, one regular skill (2 disabled group child skills not included)
        subjects[0].numGroups == 1  // one enabled, one disabled

        projects
        projects.size() == 1
        projects[0].numSkills == 3  // two group child skills, one regular skill (2 disabled group child skills not included)
        projects[0].numGroups == 1  // one enabled, one disabled
    }

    void "update multiple skills at the same time" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)
        skillsGroup.numSkillsRequired = skills.size()-1

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        def resBefore = skillsService.getSkill(skillsGroup)
        def groupSkillsBefore = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        groupSkillsBefore.sort() { it.skillId }

        when:

        skillsService.syncPointsForSkillsGroup(proj.projectId, subj.subjectId, skillsGroupId, [pointIncrement: 100, numPerformToCompletion: skills[0].numPerformToCompletion])

        def resAfter = skillsService.getSkill(skillsGroup)
        def groupSkillsAfter = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        groupSkillsAfter.sort() { it.skillId }

        then:
        resBefore
        resBefore.skillId == skillsGroup.skillId
        resBefore.name == skillsGroup.name
        resBefore.type == skillsGroup.type
        resBefore.numSkillsInGroup == 3
        resBefore.numSelfReportSkills == 0
        resBefore.numSkillsRequired == 2
        resBefore.enabled == true
        resBefore.totalPoints == 20

        groupSkillsBefore.size() == 3

        groupSkillsBefore.get(0).skillId == skills.get(0).skillId
        groupSkillsBefore.get(0).projectId == proj.projectId
        groupSkillsBefore.get(0).name == skills.get(0).name
        groupSkillsBefore.get(0).version == skills.get(0).version
        groupSkillsBefore.get(0).displayOrder == 1
        groupSkillsBefore.get(0).totalPoints == 10

        groupSkillsBefore.get(1).skillId == skills.get(1).skillId
        groupSkillsBefore.get(1).projectId == proj.projectId
        groupSkillsBefore.get(1).name == skills.get(1).name
        groupSkillsBefore.get(1).version == skills.get(1).version
        groupSkillsBefore.get(1).displayOrder == 2
        groupSkillsBefore.get(1).totalPoints == 10

        groupSkillsBefore.get(2).skillId == skills.get(2).skillId
        groupSkillsBefore.get(2).projectId == proj.projectId
        groupSkillsBefore.get(2).name == skills.get(2).name
        groupSkillsBefore.get(2).version == skills.get(2).version
        groupSkillsBefore.get(2).displayOrder == 3
        groupSkillsBefore.get(2).totalPoints == 10

        resAfter
        resAfter.skillId == skillsGroup.skillId
        resAfter.name == skillsGroup.name
        resAfter.type == skillsGroup.type
        resAfter.numSkillsInGroup == 3
        resAfter.numSelfReportSkills == 0
        resAfter.numSkillsRequired == 2
        resAfter.enabled == true
        resAfter.totalPoints == 200

        groupSkillsAfter.size() == 3

        groupSkillsAfter.get(0).skillId == skills.get(0).skillId
        groupSkillsAfter.get(0).projectId == proj.projectId
        groupSkillsAfter.get(0).name == skills.get(0).name
        groupSkillsAfter.get(0).version == skills.get(0).version
        groupSkillsAfter.get(0).displayOrder == 1
        groupSkillsAfter.get(0).totalPoints == 100

        groupSkillsAfter.get(1).skillId == skills.get(1).skillId
        groupSkillsAfter.get(1).projectId == proj.projectId
        groupSkillsAfter.get(1).name == skills.get(1).name
        groupSkillsAfter.get(1).version == skills.get(1).version
        groupSkillsAfter.get(1).displayOrder == 2
        groupSkillsAfter.get(1).totalPoints == 100

        groupSkillsAfter.get(2).skillId == skills.get(2).skillId
        groupSkillsAfter.get(2).projectId == proj.projectId
        groupSkillsAfter.get(2).name == skills.get(2).name
        groupSkillsAfter.get(2).version == skills.get(2).version
        groupSkillsAfter.get(2).displayOrder == 3
        groupSkillsAfter.get(2).totalPoints == 100
    }

    def "child skills are enabled/disabled with the parent group"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsGroup.enabled = 'false'
        skillsService.createSkill(skillsGroup)
        def children = allSkills[1..2]

        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, children[0])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, children[1])


        when:

        def groupSkillsInitial = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        def subjSkillsInitial = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        def groupInitial = skillsService.getSkill(skillsGroup)
        def child1Initial = skillsService.getSkill(children[0])
        def child2Initial = skillsService.getSkill(children[1])

        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        def groupSkillsEnabled = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        def subjSkillsEnabled = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        def groupEnabled = skillsService.getSkill(skillsGroup)
        def child1Enabled = skillsService.getSkill(children[0])
        def child2Enabled = skillsService.getSkill(children[1])

        skillsGroup.enabled = 'false'
        skillsService.updateSkill(skillsGroup, null)

        def groupSkillsDisabled = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        def subjSkillsDisabled = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        def groupDisabled = skillsService.getSkill(skillsGroup)
        def child1Disabled = skillsService.getSkill(children[0])
        def child2Disabled = skillsService.getSkill(children[1])

        then:
        groupSkillsInitial.every { it.enabled == false }
        subjSkillsInitial.every { it.enabled == false }
        groupInitial.enabled == false
        child1Initial.enabled == false
        child2Initial.enabled == false

        groupSkillsEnabled.every { it.enabled == true }
        subjSkillsEnabled.every { it.enabled == true }
        groupEnabled.enabled == true
        child1Enabled.enabled == true
        child2Enabled.enabled == true

        groupSkillsDisabled.every { it.enabled == false }
        subjSkillsDisabled.every { it.enabled == false }
        groupDisabled.enabled == false
        child1Disabled.enabled == false
        child2Disabled.enabled == false
    }

    void "skills under SkillsGroup are returned in project's skills endpoint" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def allSkills = SkillsFactory.createSkills(4) // first one is group

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[1])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[2])
        // regular skill
        skillsService.createSkill(allSkills[3])

        when:
        def res = skillsService.getSkillsForProject(proj.projectId)

        then:
        res.size() == 3
        res.collect { it.skillId }.sort() == [ allSkills[1].skillId, allSkills[2].skillId, allSkills[3].skillId, ]
    }

    void "skills under SkillsGroup are available to be used as dependencies" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def allSkills = SkillsFactory.createSkills(4) // first one is group

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[1])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[2])
        // regular skill
        skillsService.createSkill(allSkills[3])

        when:
        def res = skillsService.getSkillsAvailableForDependency(proj.projectId)

        then:
        res.size() == 3
        res.collect { it.skillId }.sort() == [ allSkills[1].skillId, allSkills[2].skillId, allSkills[3].skillId, ]
    }

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    void "subject-to-skill SkillRelDef is removed when skill is deleted" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def allSkills = SkillsFactory.createSkills(4) // first one is group

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[1])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[2])
        // regular skill
        skillsService.createSkill(allSkills[3])

        List<SkillRelDef> before = skillRelDefRepo.findAll()
        when:
        skillsService.deleteSkill(allSkills[1])
        List<SkillRelDef> after = skillRelDefRepo.findAll()
        then:
        before.findAll { it.child.skillId ==  allSkills[1].skillId }.collect { "${it.type}-${it.parent.skillId}"}.sort() == ["GroupSkillToSubject-TestSubject1", "SkillsGroupRequirement-skill1"]
        before.findAll { it.child.skillId ==  allSkills[2].skillId }.collect { "${it.type}-${it.parent.skillId}"}.sort() == ["GroupSkillToSubject-TestSubject1", "SkillsGroupRequirement-skill1"]

        !after.findAll { it.child.skillId ==  allSkills[1].skillId }.collect { "${it.type}-${it.parent.skillId}"}
        after.findAll { it.child.skillId ==  allSkills[2].skillId }.collect { "${it.type}-${it.parent.skillId}"}.sort() == ["GroupSkillToSubject-TestSubject1", "SkillsGroupRequirement-skill1"]
    }

    void "group's skills are eligible for self-reporting approval"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def allSkills = SkillsFactory.createSkills(3) // first one is group
        allSkills[1].selfReportingType = SkillDef.SelfReportingType.Approval
        allSkills[2].selfReportingType = SkillDef.SelfReportingType.Approval
        allSkills[1].pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[1])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[2])

        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        List<String> users = getRandomUsers(7)
        println skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[1].skillId], users.first(), new Date(), "Please approve this 1!")
        println skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[2].skillId], users.first(), new Date(), "Please approve this 2!")

        when:
        def res = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)

        def approvals1 = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals1.data.collect { it.id })

        def approvalsHistoryPg1 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false)

        then:
        res.data.size() == 2
        res.data.find { it.skillId  == allSkills[1].skillId}
        res.data.find { it.skillId  == allSkills[2].skillId}

        approvalsHistoryPg1.totalCount == 2
        approvalsHistoryPg1.count == 2
        approvalsHistoryPg1.data.size() == 2
    }

    void "when type is not provided must default to type=Skill and execute all of the skill's validation"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def allSkills = SkillsFactory.createSkills(2) // first one is group
        allSkills[1].type = null
        allSkills[1].pointIncrement = 0

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId

        when:
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[1])

        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("pointIncrement must be > 0")
    }

    void "skills ids cannot have the same skillId as an existing group"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def allSkills = SkillsFactory.createSkills(2) // first one is group
        allSkills[1].skillId = skillsGroup.skillId

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId

        when:
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[1])

        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("Skill with id [skill1] with type [SkillsGroup] already exists! Requested to create skill with type of [Skill]")
    }

    void "groups ids cannot have the same skillId as an existing skill"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def allSkills = SkillsFactory.createSkills(2) // first one is group
        skillsGroup.skillId = allSkills[1].skillId

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(allSkills[1])

        when:
        skillsService.createSkill(skillsGroup)

        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("Skill with id [skill2] with type [Skill] already exists! Requested to create skill with type of [SkillsGroup]")
    }

    void "groups ids cannot have the same skillId as an existing group's skill"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup(1 , 1, 1)
        def skillsGroup2 = SkillsFactory.createSkillsGroup(1 , 1, 2)
        def allSkills = SkillsFactory.createSkills(3) // first one is group
        skillsGroup2.skillId = allSkills[2].skillId

        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[2])

        when:
        skillsService.createSkill(skillsGroup2)

        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("Skill with id [skill3] with type [Skill] already exists! Requested to create skill with type of [SkillsGroup]")
    }

    void "when self reporting - group can  have very little points as long as the entire subject and project has sufficient points"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def allSkills = SkillsFactory.createSkills(4) // first one is group
        allSkills[1].selfReportingType = SkillDef.SelfReportingType.Approval
        allSkills[2].selfReportingType = SkillDef.SelfReportingType.Approval
        allSkills[1].pointIncrement = 1
        allSkills[2].pointIncrement = 1
        allSkills[3].pointIncrement = 500

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[1])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[2])
        skillsService.createSkill(allSkills[3])

        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        List<String> users = getRandomUsers(7)
        println skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[1].skillId], users.first(), new Date(), "Please approve this 1!")
        println skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[2].skillId], users.first(), new Date(), "Please approve this 2!")

        when:
        def res = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)

        def approvals1 = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals1.data.collect { it.id })

        def approvalsHistoryPg1 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false)

        then:
        res.data.size() == 2
        res.data.find { it.skillId  == allSkills[1].skillId}
        res.data.find { it.skillId  == allSkills[2].skillId}

        approvalsHistoryPg1.totalCount == 2
        approvalsHistoryPg1.count == 2
        approvalsHistoryPg1.data.size() == 2
    }
}
