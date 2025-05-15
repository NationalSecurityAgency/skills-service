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
package skills.intTests.moveSkills

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException

import static skills.intTests.utils.SkillsFactory.*

class MoveSkillsValidationSpecs extends DefaultIntSpec {

    def "validate skill is not already in the group"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])
        skillsService.createSkill(p1subj1g1)
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId, p1subj1g1.skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill with id [${p1Skills[0].skillId}] already exists under [${p1subj1g1.skillId}]")
    }

    def "validate skill is not already in the subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill with id [${p1Skills[0].skillId}] already exists under [${p1subj1.subjectId}]")
    }

    def "all skill ids that are moved must come from the same parent"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1SkillsSubj2 = createSkills(3, 1, 2, 100)
        skillsService.createSkills(p1SkillsSubj2)

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId, p1SkillsSubj2[0].skillId], p1subj1.subjectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill with id [${p1Skills[0].skillId}] already exists under [${p1subj1.subjectId}]")
    }

    def "cannot move if a finalization is pending"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skills = createSkills(10, 2, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2skills)
        def p2ExportedSkills = p2skills[3..7]
        p2ExportedSkills.each { skillsService.exportSkillToCatalog(it.projectId, it.skillId) }

        skillsService.bulkImportSkillsFromCatalog(p1.projectId, p1subj1.subjectId, p2ExportedSkills.collect { [projectId: it.projectId, skillId: it.skillId] })

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Cannot move skills while finalization is pending")
    }

    def "cannot move if a finalization is running"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p2Skills = createSkills(3, 1, 2, 100, 5)
        skillsService.createSkills(p2Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skills = createSkills(10, 2, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2skills)
        def p2ExportedSkills = p2skills[3..7]
        p2ExportedSkills.each { skillsService.exportSkillToCatalog(it.projectId, it.skillId) }

        skillsService.bulkImportSkillsFromCatalog(p1.projectId, p1subj1.subjectId, p2ExportedSkills.collect { [projectId: it.projectId, skillId: it.skillId] })

        when:
        skillsService.finalizeSkillsImportFromCatalog(p1.projectId, false)
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Cannot move skills while finalization is running")
    }

    def "cannot move an enabled skill to a disabled subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        p1subj2.enabled = false
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill with id [${p1Skills[0].skillId}] is enabled and cannot be moved to a disabled destination [${p1subj2.subjectId}]")
    }
}
