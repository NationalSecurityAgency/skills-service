/**
 * Copyright 2026 SkillTree
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
package skills.intTests.batchUpdateSkills

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class BatchUpdateSkillsValidationSpec extends DefaultIntSpec {

    def "should return error when skills are not found"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)
        String missingSkillId = "non_existent_skill_2"

        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                skills: [p1skills[0].skillId, missingSkillId],
                pointIncrement: 100
        ])

        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("Skills not found")
        ex.message.contains(missingSkillId)
        !ex.message.contains(p1skills[0].skillId)
    }

    def "should return error when skill is not of skill type"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def p1SkillGroup = createSkillsGroup(1, 1, 10)
        skillsService.createSkill(p1SkillGroup)

        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                skills: [p1SkillGroup.skillId],
                pointIncrement: 100
        ])

        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("Only skill type is supported. [${p1SkillGroup.skillId}] is [${SkillDef.ContainerType.SkillsGroup}]")
    }

    def "should return error when skills belong to different subjects"() {
        given:
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1skills1 = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills1)

        def p1subj2 = createSubject(1, 2)
        def p1skills2 = createSkills(1, 1, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skills2)

        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                skills: [p1skills1[0].skillId, p1skills2[0].skillId],
                pointIncrement: 100
        ])

        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("All provided skills must belong to the same subject. Skill [${p1skills2[0].skillId}] is not a child of [${p1subj1.subjectId}]")
    }

    def "should return error when trying to enable imported skill"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)
        skillsService.exportSkillToCatalog(p1.projectId, p1skills[0].skillId)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProject(p2)
        skillsService.createSubject(p2subj1)

        skillsService.bulkImportSkillsFromCatalog(p2.projectId, p2subj1.subjectId, [
                [projectId: p1.projectId, skillId: p1skills[0].skillId]
        ])
        skillsService.finalizeSkillsImportFromCatalog(p2.projectId)

        when:
        skillsService.batchUpdateSkills(p2.projectId, [
                skills: [p1skills[0].skillId],
                pointIncrement: 100
        ])

        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("Cannot batch update imported skills")
    }

    def "should return error when self reporting type is not supported"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                skills: [p1skills[0].skillId],
                selfReportingType: SkillDef.SelfReportingType.Quiz.toString() // Only Approval and HonorSystem are supported in batch
        ])

        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("Only [Approval] or [HonorSystem] are supported")
    }

    def "should return error when skills belong to different skill groups"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)

        def group1 = createSkillsGroup(1, 1, 10)
        def group2 = createSkillsGroup(1, 1, 20)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [group1, group2])

        def p1skills = createSkills(2, 1, 1, 100)
        skillsService.assignSkillToSkillsGroup(group1.skillId, p1skills[0])
        skillsService.assignSkillToSkillsGroup(group2.skillId, p1skills[1])

        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                skills: [p1skills[0].skillId, p1skills[1].skillId],
                pointIncrement: 100
        ])

        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("All provided skills must belong to the same skills group")
    }

}