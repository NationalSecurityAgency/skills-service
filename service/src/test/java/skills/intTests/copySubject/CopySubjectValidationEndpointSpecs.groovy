/**
 * Copyright 2024 SkillTree
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
package skills.intTests.copySubject

import org.springframework.beans.factory.annotation.Value
import skills.intTests.copyProject.CopyIntSpec
import skills.intTests.utils.SkillsService

import static skills.intTests.utils.SkillsFactory.*

class CopySubjectValidationEndpointSpecs extends CopyIntSpec {

    @Value('#{"${skills.config.ui.maxSubjectsPerProject}"}')
    int maxSubjectsPerProject

    def "validate subject name is unique - name lowercase"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        p2subj1.subjectId = 'someOther'
        p2subj1.name = p2subj1.name.toLowerCase()
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["Subject with name [${p1subj1.name}] already exists."]
    }

    def "validate subject name is unique - name uppercase"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        p2subj1.subjectId = 'someOther'
        p2subj1.name = p2subj1.name.toUpperCase()
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["Subject with name [${p1subj1.name}] already exists."]
    }

    def "validate subject id is unique - id lowercase"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        p2subj1.name = 'Other name'
        p2subj1.subjectId = p2subj1.subjectId.toLowerCase()
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["Id [${p1subj1.subjectId}] already exists."]
    }

    def "validate subject id is unique - id uppercase"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        p2subj1.name = 'Other name'
        p2subj1.subjectId = p2subj1.subjectId.toUpperCase()
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["Id [${p1subj1.subjectId}] already exists."]
    }

    def "validate subject id is unique due to an existing skill id"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2skill1 = createSkill(2, 2, 1)
        p2skill1.skillId = p1subj1.subjectId
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skill1])

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["Id [${p1subj1.subjectId}] already exists."]
    }

    def "validate that there is no skill id collisions"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        p2Subj1Skills[1].skillId = p1Subj1Skills[2].skillId
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Subj1Skills)

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["The following IDs already exist in the destination project: ${p1Subj1Skills[2].skillId}."]
    }

    def "validate that there is no skill id collisions - multiple collisions"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        p2Subj1Skills[0].skillId = p1Subj1Skills[2].skillId
        p2Subj1Skills[1].skillId = p1Subj1Skills[1].skillId
        p2Subj1Skills[2].skillId = p1Subj1Skills[0].skillId
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Subj1Skills)

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors.sort() == ["The following IDs already exist in the destination project: ${p1Subj1Skills.skillId.sort().join(", ")}."]
    }

    def "validate that there is no skill id and name collisions - multiple collisions"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        p2Subj1Skills[0].skillId = p1Subj1Skills[2].skillId
        p2Subj1Skills[1].skillId = p1Subj1Skills[1].skillId
        p2Subj1Skills[2].skillId = p1Subj1Skills[0].skillId
        p2Subj1Skills[0].name = p1Subj1Skills[2].name
        p2Subj1Skills[1].name = p1Subj1Skills[1].name
        p2Subj1Skills[2].name = p1Subj1Skills[0].name
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Subj1Skills)

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors.sort() == [
                "The following IDs already exist in the destination project: ${p1Subj1Skills.skillId.sort().join(", ")}.",
                "The following names already exist in the destination project: ${p1Subj1Skills.name.sort().join(", ")}."
        ].sort()
    }

    def "validate that there is no skill id and name collisions - only return up to 10 names/ids in the error"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(15, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(15, 2, 2, 100)
        p2Subj1Skills.eachWithIndex{ Map entry, int i ->
            entry.skillId = p1Subj1Skills[i].skillId
            entry.name = p1Subj1Skills[i].name
        }
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Subj1Skills)

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors.sort() == [
                "The following IDs already exist in the destination project: ${p1Subj1Skills.skillId.sort().subList(0, 10).join(", ")}.",
                "The following names already exist in the destination project: ${p1Subj1Skills.name.sort().subList(0, 10).join(", ")}."
        ].sort()
    }

    def "validate that there is no skill id collisions for skills under a group in destination project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        p2Subj1Skills[1].skillId = p1Subj1Skills[2].skillId
        def group = createSkillsGroup(2, 2, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [group])
        skillsService.assignSkillToSkillsGroup(group.skillId, p2Subj1Skills[0])
        skillsService.assignSkillToSkillsGroup(group.skillId, p2Subj1Skills[1])
        skillsService.assignSkillToSkillsGroup(group.skillId, p2Subj1Skills[2])

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["The following IDs already exist in the destination project: ${p1Subj1Skills[2].skillId}."]
    }

    def "validate that there is no skill id collisions for skills under a group in orig project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        def group = createSkillsGroup(1, 1, 4)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [group])
        skillsService.assignSkillToSkillsGroup(group.skillId, p1Subj1Skills[0])
        skillsService.assignSkillToSkillsGroup(group.skillId, p1Subj1Skills[1])
        skillsService.assignSkillToSkillsGroup(group.skillId, p1Subj1Skills[2])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        p2Subj1Skills[1].skillId = p1Subj1Skills[2].skillId
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Subj1Skills)

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["The following IDs already exist in the destination project: ${p1Subj1Skills[2].skillId}."]
    }

    def "validate that there is no skill id collisions due to group id in the orig project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        def group = createSkillsGroup(1, 1, 4)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [group])
        skillsService.assignSkillToSkillsGroup(group.skillId, p1Subj1Skills[0])
        skillsService.assignSkillToSkillsGroup(group.skillId, p1Subj1Skills[1])
        skillsService.assignSkillToSkillsGroup(group.skillId, p1Subj1Skills[2])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        p2Subj1Skills[1].skillId = group.skillId
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Subj1Skills)

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["The following IDs already exist in the destination project: ${group.skillId}."]
    }

    def "validate that there is no skill id collisions badge id"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def badge = createBadge(2, 1)
        badge.badgeId = p1Subj1Skills[2].skillId
        skillsService.createProject(p2)
        skillsService.createBadge(badge)

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["The following IDs already exist in the destination project: ${p1Subj1Skills[2].skillId}."]
    }

    def "validate that there is no skill id collisions due to the imported skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])

        def p3 = createProject(3)
        def p3subj1 = createSubject(3, 4)
        def p3Subj1Skills = createSkills(3, 3, 4, 100)
        p3Subj1Skills[0].skillId = p1Subj1Skills[2].skillId
        skillsService.createProjectAndSubjectAndSkills(p3, p3subj1, p3Subj1Skills)
        skillsService.exportSkillToCatalog(p3.projectId, p3Subj1Skills[0].skillId as String)

        skillsService.importSkillFromCatalog(p2.projectId, p2subj1.subjectId, p3.projectId, p3Subj1Skills[0].skillId)

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["The following IDs already exist in the destination project: ${p3Subj1Skills[0].skillId}."]
    }

    def "validate that there is no skill id collisions due to group id in the dest project"() {
        def group = createSkillsGroup(2, 2, 4)

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        p1Subj1Skills[0].skillId = group.skillId
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [group])
        skillsService.assignSkillToSkillsGroup(group.skillId, p2Subj1Skills[0])
        skillsService.assignSkillToSkillsGroup(group.skillId, p2Subj1Skills[1])
        skillsService.assignSkillToSkillsGroup(group.skillId, p2Subj1Skills[2])

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["The following IDs already exist in the destination project: ${group.skillId}."]
    }

    def "validate that there is no name collisions for skills under a group in destination project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        p2Subj1Skills[1].name = p1Subj1Skills[2].name
        def group = createSkillsGroup(2, 2, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [group])
        skillsService.assignSkillToSkillsGroup(group.skillId, p2Subj1Skills[0])
        skillsService.assignSkillToSkillsGroup(group.skillId, p2Subj1Skills[1])
        skillsService.assignSkillToSkillsGroup(group.skillId, p2Subj1Skills[2])

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["The following names already exist in the destination project: ${p1Subj1Skills[2].name}."]
    }

    def "validate that there is no name collisions for skills under a group in orig project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        def group = createSkillsGroup(1, 1, 4)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [group])
        skillsService.assignSkillToSkillsGroup(group.skillId, p1Subj1Skills[0])
        skillsService.assignSkillToSkillsGroup(group.skillId, p1Subj1Skills[1])
        skillsService.assignSkillToSkillsGroup(group.skillId, p1Subj1Skills[2])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        p2Subj1Skills[1].name = p1Subj1Skills[2].name
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Subj1Skills)

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["The following names already exist in the destination project: ${p1Subj1Skills[2].name}."]
    }

    def "validate that there is no name collisions due to group id in the orig project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        def group = createSkillsGroup(1, 1, 4)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [group])
        skillsService.assignSkillToSkillsGroup(group.skillId, p1Subj1Skills[0])
        skillsService.assignSkillToSkillsGroup(group.skillId, p1Subj1Skills[1])
        skillsService.assignSkillToSkillsGroup(group.skillId, p1Subj1Skills[2])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        p2Subj1Skills[1].name = group.name
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Subj1Skills)

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["The following names already exist in the destination project: ${group.name}."]
    }

    def "validate that there is no name collisions badge id"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def badge = createBadge(2, 1)
        badge.name = p1Subj1Skills[2].name
        skillsService.createProject(p2)
        skillsService.createBadge(badge)

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["The following names already exist in the destination project: ${p1Subj1Skills[2].name}."]
    }

    def "validate that there is no name collisions due to the imported skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])

        def p3 = createProject(3)
        def p3subj1 = createSubject(3, 4)
        def p3Subj1Skills = createSkills(3, 3, 4, 100)
        p3Subj1Skills[0].name = p1Subj1Skills[2].name
        skillsService.createProjectAndSubjectAndSkills(p3, p3subj1, p3Subj1Skills)
        skillsService.exportSkillToCatalog(p3.projectId, p3Subj1Skills[0].skillId as String)

        skillsService.importSkillFromCatalog(p2.projectId, p2subj1.subjectId, p3.projectId, p3Subj1Skills[0].skillId)

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["The following names already exist in the destination project: ${p3Subj1Skills[0].name}."]
    }

    def "validate that there is no name collisions due to group id in the dest project"() {
        def group = createSkillsGroup(2, 2, 4)

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        p1Subj1Skills[0].name = group.name
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [group])
        skillsService.assignSkillToSkillsGroup(group.skillId, p2Subj1Skills[0])
        skillsService.assignSkillToSkillsGroup(group.skillId, p2Subj1Skills[1])
        skillsService.assignSkillToSkillsGroup(group.skillId, p2Subj1Skills[2])

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["The following names already exist in the destination project: ${group.name}."]
    }

    def "validate that there is no skill name collisions"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        p2Subj1Skills[1].name = p1Subj1Skills[0].name
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Subj1Skills)

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["The following names already exist in the destination project: ${p2Subj1Skills[1].name}."]
    }

    def "do not allow to copy from community protected project to a non-community protected project"() {
        SkillsService rootSkillsService = createRootSkillService()
        SkillsService dragonUser = createService(getRandomUsers(1).first())
        rootSkillsService.saveUserTag(dragonUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        dragonUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        dragonUser.createProject(p2)

        when:
        def res = dragonUser.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["Subjects from Divine Dragon projects cannot be copied to All Dragons projects."]
    }


    def "validate subject limit can not be bypassed"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def subjectsToMake = maxSubjectsPerProject + 1
        skillsService.createProject(p2)
        for(def x = 1; x <= maxSubjectsPerProject; x++ ) {
            def newSubj = createSubject(2, x + 1)
            def subjSkills = createSkills(3, 2, x + 1, 100)
            skillsService.createSubjectAndSkills(newSubj, subjSkills)
        }

        when:
        def res = skillsService.validateCopySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        then:
        res.isAllowed == false
        res.validationErrors == ["Each Project is limited to [25] Subjects, currently [TestProject2] has [25] Subjects, copying [3] would exceed the maximum"]
    }
}
