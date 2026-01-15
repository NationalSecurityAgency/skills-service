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

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.UserPoints
import skills.storage.repos.SkillRelDefRepo

class SkillsGroupSpecs extends DefaultIntSpec {

    DateTimeFormatter DTF = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").withZoneUTC()

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

        res2
        res2.skillId == skillsGroup.skillId
        res2.name == skillsGroup.name
        res2.type == skillsGroup.type
        res2.numSkillsInGroup == 0
        res2.numSelfReportSkills == 0
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

        res2
        res2.skillId == skillsGroup.skillId
        res2.name == skillsGroup.name
        res2.type == skillsGroup.type
        res2.numSkillsInGroup == 0
        res2.numSelfReportSkills == 0
    }

    void "create and add more than one skill with same total point values to SkillsGroup" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        when:
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }
        def res = skillsService.getSkill(skillsGroup)
        def groupSkills = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        def subjSkills = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)

        then:
        res
        res.skillId == skillsGroup.skillId
        res.name == skillsGroup.name
        res.type == skillsGroup.type
        res.numSkillsInGroup == groupSkills.size()
        res.numSelfReportSkills == 0

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
    }

    void "change display order of skills under a group" () {
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
        def groupSkills = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        skillsService.moveSkillUp(skills[2])
        def groupSkillsAfterFirstMove = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        skillsService.moveSkillDown(skills[0])
        def groupSkillsAfterSecondMove = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        skillsService.moveSkillDown(skills[0])
        def groupSkillsAfterThirdMove = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)

        then:
        groupSkills.collect { it.skillId } == ['skill1', 'skill2', 'skill3']
        groupSkillsAfterFirstMove.collect { it.skillId } == ['skill1', 'skill3', 'skill2']
        groupSkillsAfterSecondMove.collect { it.skillId } == ['skill3', 'skill1', 'skill2']
        groupSkillsAfterThirdMove.collect { it.skillId } == ['skill3', 'skill2', 'skill1']
    }

    void "SkillsGroup with < 2 child skills" () {
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
        def groupSkills = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)

        then:
        groupSkills.size() == 1

        groupSkills.get(0).skillId == skills.get(0).skillId
        groupSkills.get(0).projectId == proj.projectId
        groupSkills.get(0).name == skills.get(0).name
        groupSkills.get(0).version == skills.get(0).version
        groupSkills.get(0).displayOrder == 1
        groupSkills.get(0).totalPoints == skills.get(0).pointIncrement * skills.get(0).numPerformToCompletion
    }

    void "A Skill Group cannot have 0 required skills" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        skillsGroup.numSkillsRequired = 0
        skillsService.updateSkill(skillsGroup, null)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("A Skill Group must have at least 1 required skill.")
    }

    void "a SkillsGroup when not all skills are required, and not all have the same # of points is allowed" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2)
        def skill3WithDiffNumPoints = SkillsFactory.createSkill(1, 1, 3)
        skill3WithDiffNumPoints.pointIncrement = 100
        skills.add(skill3WithDiffNumPoints)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        skillsGroup.numSkillsRequired = skills.size()-1
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
        res.numSkillsRequired == skills.size()-1
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

    void "can update a SkillsGroup to have a different # of points when not all skills are required" () {
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
        skillsGroup.numSkillsRequired = skills.size()-1
        skillsService.updateSkill(skillsGroup, null)

        when:

        def skill3WithDiffNumPoints = skills[2]
        skill3WithDiffNumPoints.pointIncrement = 100
        skillsService.updateSkill(skill3WithDiffNumPoints, null)

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
        res.numSkillsRequired == skills.size()-1
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

    void "can update a child skill's points to a value different than the other child skills when not all skills are required" () {
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
        skillsGroup.numSkillsRequired = skills.size() - 1
        skillsService.updateSkill(skillsGroup, null)

        def skillWithDifferentPoints = skills.first()
        skillWithDifferentPoints.pointIncrement = 321
        skillsService.updateSkill(skillWithDifferentPoints, null)
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
        res.numSkillsRequired == skills.size()-1
        res.enabled == true
        res.totalPoints == groupSkills[0].totalPoints + groupSkills[1].totalPoints + groupSkills[2].totalPoints

        groupSkills.size() == 3

        groupSkills.get(0).skillId == skills.get(0).skillId
        groupSkills.get(0).projectId == proj.projectId
        groupSkills.get(0).name == skills.get(0).name
        groupSkills.get(0).version == skills.get(0).version
        groupSkills.get(0).displayOrder == 1
        groupSkills.get(0).totalPoints == 321 * skills.get(0).numPerformToCompletion

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
    }

    void "a SkillsGroup when all skills are required, and not all skills have the same # of points" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2)
        def skill3WithDiffNumPoints = SkillsFactory.createSkill(1, 1, 3)
        skill3WithDiffNumPoints.pointIncrement = 100
        skills.add(skill3WithDiffNumPoints)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        skillsGroup.numSkillsRequired = skills.size()
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

    void "a SkillsGroup when numSkillsRequired == -1, and not all skills have the same # of points" () {
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

    void "a SkillsGroup when not all skills are required, but all skills have the same # of points" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def childSkills = SkillsFactory.createSkills(3)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        childSkills.each { childSkill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, childSkill)
        }

        when:
        skillsGroup.numSkillsRequired = childSkills.size() - 1
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
        res.totalPoints == (childSkills.get(0).pointIncrement * childSkills.get(0).numPerformToCompletion) * childSkills.size()
        res.enabled == true

        groupSkills.size() == 3

        groupSkills.get(0).skillId == childSkills.get(0).skillId
        groupSkills.get(0).projectId == proj.projectId
        groupSkills.get(0).name == childSkills.get(0).name
        groupSkills.get(0).version == childSkills.get(0).version
        groupSkills.get(0).displayOrder == 1
        groupSkills.get(0).totalPoints == childSkills.get(0).pointIncrement * childSkills.get(0).numPerformToCompletion

        groupSkills.get(1).skillId == childSkills.get(1).skillId
        groupSkills.get(1).projectId == proj.projectId
        groupSkills.get(1).name == childSkills.get(1).name
        groupSkills.get(1).version == childSkills.get(1).version
        groupSkills.get(1).displayOrder == 2
        groupSkills.get(1).totalPoints == childSkills.get(0).pointIncrement * childSkills.get(0).numPerformToCompletion

        groupSkills.get(2).skillId == childSkills.get(2).skillId
        groupSkills.get(2).projectId == proj.projectId
        groupSkills.get(2).name == childSkills.get(2).name
        groupSkills.get(2).version == childSkills.get(2).version
        groupSkills.get(2).displayOrder == 3
        groupSkills.get(2).totalPoints == childSkills.get(0).pointIncrement * childSkills.get(0).numPerformToCompletion
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

    def "a skill group does not allow the number of child skills to cause the maximum skills per subject to be exceeded"() {
            def proj = SkillsFactory.createProject()
            def subj = SkillsFactory.createSubject()
            def skills = SkillsFactory.createSkills(5)
            def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 6)

            skillsService.createProject(proj)
            skillsService.createSubject(subj)
            skillsService.createSkill(skillsGroup)

            def otherSkills = SkillsFactory.createSkillsStartingAt(96, 7)
            skillsService.createSkills(otherSkills)

            when:
            String skillsGroupId = skillsGroup.skillId
            skills.each { skill ->
                skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
            }

            then:
            def e = thrown(SkillsClientException)
            e.message.contains('explanation:Each Subject is limited to [100] Skills, errorCode:MaxSkillsThreshold')
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
        skillsService.updateSkill(skillsGroup1, null)

        def skillsGroup2 = allSkills[3]
        skillsGroup2.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup2)
        String skillsGroup2Id = skillsGroup2.skillId
        def group2Children = allSkills[4..6]
        group2Children.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroup2Id, skill)
        }
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
        pointAfterNumSkillsRequiredReduced == 20
    }

    def "group info is returned for child skill on skill endpoint"() {
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

        def res = skillsService.getSkill(children[0])

        then:
        res
        res.enabled == true
        res.groupName == skillsGroup.name
        res.groupId == skillsGroup.skillId
    }

    def "totalPoints are always included for groups"() {
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

    def "can delete a child skill that would cause the group to have less than 2 skills"() {
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
        skillsService.updateSkill(skillsGroup, null)

        when:
        group1Children.each { skill ->
            skillsService.deleteSkill(skill)
        }
        def groupSkillsAfterDelete = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)

        then:
        !groupSkillsAfterDelete
    }

    def "child skills are enabled with the parent group by default"() {
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

        def groupSkillsEnabled = skillsService.getSkillsForGroup(proj.projectId, skillsGroupId)
        def subjSkillsEnabled = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        def groupEnabled = skillsService.getSkill(skillsGroup)
        def child1Enabled = skillsService.getSkill(children[0])
        def child2Enabled = skillsService.getSkill(children[1])
        then:

        groupSkillsEnabled.every { it.enabled == true }
        subjSkillsEnabled.every { it.enabled == true }
        groupEnabled.enabled == true
        child1Enabled.enabled == true
        child2Enabled.enabled == true
    }

    void "skills under SkillsGroup are returned in project's skills endpoint" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(4) // first one is group
        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'

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

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    void "subject-to-skill SkillRelDef is removed when skill is deleted" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(4) // first one is group
        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'

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

        skillsService.updateSkill(skillsGroup, null)

        List<String> users = getRandomUsers(7)
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[1].skillId], users.first(), new Date(), "Please approve this 1!")
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[2].skillId], users.first(), new Date(), "Please approve this 2!")

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

    void "change group's skillId - new value propagates to child skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def allSkills = SkillsFactory.createSkills(3) // first one is group

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)

        String originalSkillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(originalSkillsGroupId, allSkills[1])
        skillsService.assignSkillToSkillsGroup(originalSkillsGroupId, allSkills[2])

        when:
        skillsGroup.skillId = "newCoolId"
        skillsService.updateSkill(skillsGroup, originalSkillsGroupId)

        def s1 = skillsService.getSkill(allSkills[1])
        def s2 = skillsService.getSkill(allSkills[2])
        then:
        s1.groupId == "newCoolId"
        s2.groupId == "newCoolId"
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

        skillsService.updateSkill(skillsGroup, null)

        List<String> users = getRandomUsers(7)
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[1].skillId], users.first(), new Date(), "Please approve this 1!")
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[2].skillId], users.first(), new Date(), "Please approve this 2!")

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

    def "removing an event for a skill under a group must adjust users group, subject and project points"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def allSkills = SkillsFactory.createSkills(20) // first one is group
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        // first group
        allSkills[1].pointIncrement = 100
        allSkills[1].numPerformToCompletion = 10
        allSkills[2].pointIncrement = 100
        allSkills[2].numPerformToCompletion = 10
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[1])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[2])
        skillsService.createSkill(allSkills[3])
        skillsService.updateSkill(skillsGroup, null)

        // 2nd group
        allSkills[4].pointIncrement = 33
        allSkills[4].numPerformToCompletion = 10
        allSkills[4].pointIncrementInterval = 0
        allSkills[5].pointIncrement = 33
        allSkills[5].numPerformToCompletion = 10
        allSkills[5].pointIncrementInterval = 0
        allSkills[6].pointIncrement = 33
        allSkills[6].numPerformToCompletion = 10
        allSkills[6].pointIncrementInterval = 0
        def skillsGroup2 = SkillsFactory.createSkillsGroup(1, 1, 30)
        String skillsGroupId1 = skillsGroup2.skillId
        skillsService.createSkill(skillsGroup2)
        skillsService.assignSkillToSkillsGroup(skillsGroupId1, allSkills[4])
        skillsService.assignSkillToSkillsGroup(skillsGroupId1, allSkills[5])
        skillsService.assignSkillToSkillsGroup(skillsGroupId1, allSkills[6])
        skillsService.updateSkill(skillsGroup2, null)

        // 2nd subject
        def subj2 = SkillsFactory.createSubject(1, 2)
        def subj2_skills = SkillsFactory.createSkills(11, 1, 2)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)

        List<String> users = getRandomUsers(1)
        List<Date> dates = (1..12).collect { new Date() -it }.sort()
        List<Date> days = dates.collect { new Date(it.time).clearTime() }

        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[1].skillId], users.first(), dates[0])
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[1].skillId], users.first(), dates[1])
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[1].skillId], users.first(), dates[2])

        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[2].skillId], users.first(), dates[0])
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[2].skillId], users.first(), dates[1])

        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[5].skillId], users.first(), dates[2])
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[5].skillId], users.first(), dates[3])
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[5].skillId], users.first(), dates[3])
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[5].skillId], users.first(), dates[4])

        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[6].skillId], users.first(), dates[4])
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[6].skillId], users.first(), dates[4])
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[6].skillId], users.first(), dates[4])

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users.first(), dates[0])
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users.first(), dates[1])
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[3].skillId], users.first(), dates[2])

        Closure<List<UserPoints>> getPoints = { String user, String projectId, String skillId ->
            List<UserPoints> points = userPointsRepo.findAll()
                    .findAll({ it.userId == user && it.projectId == projectId && it.skillId == skillId })
                    .sort({ it.day })
            return points
        }

        when:
        List<UserPoints> u0_s1_t0 = getPoints.call(users[0], proj.projectId, allSkills[1].skillId)
        List<UserPoints> u0_s2_t0 = getPoints.call(users[0], proj.projectId, allSkills[2].skillId)
        List<UserPoints> u0_g1_t0 = getPoints.call(users[0], proj.projectId, skillsGroup.skillId)
        List<UserPoints> u0_g2_t0 = getPoints.call(users[0], proj.projectId, skillsGroup2.skillId)
        List<UserPoints> u0_subj1_t0 = getPoints.call(users[0], proj.projectId, subj.subjectId)
        List<UserPoints> u0_subj2_t0 = getPoints.call(users[0], proj.projectId, subj2.subjectId)
        List<UserPoints> u0_p1_t0 = getPoints.call(users[0], proj.projectId, null)

        skillsService.deleteSkillEvent([projectId: proj.projectId, skillId: allSkills[1].skillId, userId: users[0], timestamp: dates[1].time] )

        List<UserPoints> u0_s1_t1 = getPoints.call(users[0], proj.projectId, allSkills[1].skillId)
        List<UserPoints> u0_s2_t1 = getPoints.call(users[0], proj.projectId, allSkills[2].skillId)
        List<UserPoints> u0_g1_t1 = getPoints.call(users[0], proj.projectId, skillsGroup.skillId)
        List<UserPoints> u0_g2_t1 = getPoints.call(users[0], proj.projectId, skillsGroup2.skillId)
        List<UserPoints> u0_subj1_t1 = getPoints.call(users[0], proj.projectId, subj.subjectId)
        List<UserPoints> u0_subj2_t1 = getPoints.call(users[0], proj.projectId, subj2.subjectId)
        List<UserPoints> u0_p1_t1 = getPoints.call(users[0], proj.projectId, null)

        then:
        u0_s1_t0.points == [300]
        u0_s2_t0.points == [200]
        !u0_g1_t0.points
        !u0_g2_t0.points
        u0_subj1_t0.points == [500 + 231]
        u0_subj2_t0.points == [30]
        u0_p1_t0.points == [500 + 231 + 30]

        u0_s1_t1.points == [200]
        u0_s2_t1.points == [200]
        !u0_g1_t1.points
        !u0_g2_t1.points
        u0_subj1_t1.points == [400 + 231]
        u0_subj2_t1.points == [30]
        u0_p1_t1.points == [400 + 231 + 30]
    }

    def "points awarded for group skill must have last earned date when fetching subject users"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        def allSkills = SkillsFactory.createSkills(4) // first one is group
        allSkills[1].pointIncrement = 100
        allSkills[2].pointIncrement = 100
        allSkills[3].pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[1])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[2])
        skillsService.createSkill(allSkills[3])

        skillsService.updateSkill(skillsGroup, null)
        def user = getRandomUsers(1)[0]

        when:
        Date date = new Date()
        skillsService.addSkill([projectId: proj.projectId, skillId: allSkills[1].skillId], user, date)
        def subjectUsers = skillsService.getSubjectUsers(proj.projectId, subj.subjectId)

        then:
        subjectUsers.data[0].userId == user
        subjectUsers.data[0].lastUpdated == DTF.print(date.time)
    }

    void "get subject for SkillsGroup" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)

        when:

        def subject = skillsService.getSubject([subjectId: subj.subjectId, projectId: proj.projectId])
        def subjectForGround = skillsService.getSubjectForGroup(proj.projectId, skillsGroup.skillId)

        then:
        subject
        subject.skillId == subj.skillId
        subject.name == subj.name
    }

    void "SkillsGroup sanitizes name appropriately" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        skillsGroup.name = '<span style="font-size: 72px"><em>Fact & Fiction</em></span>'

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)

        def allSkills = SkillsFactory.createSkills(4)
        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[1])
        skillsService.assignSkillToSkillsGroup(skillsGroupId, allSkills[2])

        when:
        def subjectSkills = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId, true)
        def groupSkill = skillsService.getSkill(skillsGroup)
        def skillsInGroup = skillsService.getSkillsForGroup(proj.projectId, skillsGroup.skillId)

        then:
        subjectSkills
        subjectSkills[0].name == 'Fact & Fiction'
        groupSkill.name == 'Fact & Fiction'
        skillsInGroup[0].name == 'Test Skill 2'
        skillsInGroup[0].groupName == 'Fact & Fiction'
        skillsInGroup[1].name == 'Test Skill 3'
        skillsInGroup[1].groupName == 'Fact & Fiction'
    }

    void "SkillsGroup allows for markdown in descriptions" () {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skillsGroup = SkillsFactory.createSkillsGroup()
        skillsGroup.description = '<span style="font-size: 72px"><em>Fact & Fiction</em></span>'

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)

        when:

        def result = skillsService.getSkillDescription(proj.projectId, skillsGroup.skillId)

        then:
        result
        result.description == '<span style="font-size: 72px"><em>Fact & Fiction</em></span>'
    }
}
