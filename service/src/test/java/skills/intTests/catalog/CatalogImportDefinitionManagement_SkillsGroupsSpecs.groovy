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
package skills.intTests.catalog

import groovy.json.JsonOutput
import skills.intTests.utils.SkillsFactory

import static skills.intTests.utils.SkillsFactory.*

class CatalogImportDefinitionManagement_SkillsGroupsSpecs extends CatalogIntSpec {

    def "import group skill from catalog"() {
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
        p2skillsGroup.enabled = true
        skillsService.createSkill(p2skillsGroup)

        when:
        def project_t0 = skillsService.getProject(p2.projectId)
        def projects_t0 = skillsService.getProjects()
        def subject_t0 = skillsService.getSubject(p2subj1)
        def subjects_t0 = skillsService.getSubjects(p2.projectId)
        def p2subj1Skills_t0 = skillsService.getSkillsForSubject(p2.projectId, p2subj1.subjectId)
        def p2skillsGroupSkills_t0 = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)


        skillsService.bulkImportSkillsIntoGroupFromCatalog(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                [[projectId: p1.projectId, skillId: p1Skills[0].skillId]])

        def project_t1 = skillsService.getProject(p2.projectId)
        def projects_t1 = skillsService.getProjects()
        def subject_t1 = skillsService.getSubject(p2subj1)
        def subjects_t1 = skillsService.getSubjects(p2.projectId)
        def p2subj1Skills_t1 = skillsService.getSkillsForSubject(p2.projectId, p2subj1.subjectId)
        def p2skillsGroupSkills_t1 = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(p2skillsGroupSkills_t1))

        skillsService.finalizeSkillsImportFromCatalog(p2.projectId)

        def project_t2 = skillsService.getProject(p2.projectId)
        def projects_t2 = skillsService.getProjects()
        def subject_t2 = skillsService.getSubject(p2subj1)
        def subjects_t2 = skillsService.getSubjects(p2.projectId)
        def p2subj1Skills_t2 = skillsService.getSkillsForSubject(p2.projectId, p2subj1.subjectId)
        def p2skillsGroupSkills_t2 = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)

        then:
        project_t0.totalPoints == 1000
        projects_t0.totalPoints == [300, 1000]
        subject_t0.totalPoints == 1000
        subjects_t0.totalPoints == [1000]
        p2subj1Skills_t0.skillId == [p2skillsGroup.skillId]
        p2skillsGroupSkills_t0.skillId == [gSkill1.skillId, gSkill2.skillId]

        project_t1.totalPoints == 1000
        projects_t1.totalPoints == [300, 1000]
        subject_t1.totalPoints == 1000
        subjects_t1.totalPoints == [1000]
        p2subj1Skills_t1.skillId == [p2skillsGroup.skillId]
        p2skillsGroupSkills_t1.skillId == [gSkill1.skillId, gSkill2.skillId, p1Skills[0].skillId]
        p2skillsGroupSkills_t1.enabled == [true, true, false]

        subject_t2.totalPoints == 1100
        project_t2.totalPoints == 1100
        projects_t2.totalPoints == [300, 1100]
        subjects_t2.totalPoints == [1100]
        p2subj1Skills_t2.skillId == [p2skillsGroup.skillId]
        p2skillsGroupSkills_t2.skillId == [gSkill1.skillId, gSkill2.skillId, p1Skills[0].skillId]
        p2skillsGroupSkills_t2.enabled == [true, true, true]

        // validate that group was updated too
        skillDefRepo.findByProjectIdAndSkillId(p2.projectId, p2skillsGroup.skillId).totalPoints == 1100
    }

}
