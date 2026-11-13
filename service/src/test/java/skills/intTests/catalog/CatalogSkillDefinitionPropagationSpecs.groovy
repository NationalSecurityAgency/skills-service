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
package skills.intTests.catalog

import org.springframework.beans.factory.annotation.Autowired
import skills.controller.result.model.LevelDefinitionRes
import skills.services.LevelDefinitionStorageService
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class CatalogSkillDefinitionPropagationSpecs extends CatalogIntSpec {

    @Autowired
    LevelDefinitionStorageService levelDefinitionStorageService

    def "update skill that has been exported to catalog"() {
        //changes should be reflected across all copies
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)

        when:
        def preEdit = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name")
        def skillNamePreEdit = skill.name
        def skillDescriptionPreEdit = skill.description
        def skillHelpUrlPreEdit = skill.helpUrl

        skill.name = "edited name"
        skill.numPerformToCompletion = 50
        skill.helpUrl = "http://newHelpUrl"
        skill.description = "updated description"
        skill.selfReportingType = SkillDef.SelfReportingType.Approval.toString()
        skill.iconClass = 'fa fa-icon-test'

        skillsService.updateSkill(skill, skill.skillId)
        def postEdit = skillsService.getCatalogSkills(project3.projectId, 10, 1, "name")

        then:
        preEdit.data[0].name == skillNamePreEdit
        preEdit.data[0].totalPoints == 250
        preEdit.data[0].numPerformToCompletion == 1
        preEdit.data[0].description == skillDescriptionPreEdit
        preEdit.data[0].helpUrl == skillHelpUrlPreEdit
        !preEdit.data[0].iconClass
        !preEdit.data[0].selfReportingType
        postEdit.data[0].name == skill.name
        postEdit.data[0].totalPoints == 12500
        postEdit.data[0].numPerformToCompletion == 50
        postEdit.data[0].description == skill.description
        postEdit.data[0].helpUrl == skill.helpUrl
        postEdit.data[0].selfReportingType == SkillDef.SelfReportingType.Approval.toString()
        postEdit.data[0].iconClass == 'fa fa-icon-test'
    }


    def "update skill that has been exported to catalog, edits should be reflected on imported copies"() {
        //changes should be reflected across all copies
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def preEdit = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])
        def skillNamePreEdit = skill.name
        def skillDescriptionPreEdit = skill.description
        def skillHelpUrlPreEdit = skill.helpUrl

        skill.name = "edited name"
        skill.numPerformToCompletion = 50
        skill.helpUrl = "http://newHelpUrl"
        skill.description = "updated description"
        skill.selfReportingType = SkillDef.SelfReportingType.Approval.toString()
        skill.iconClass = 'fa fa-icon-test'

        skillsService.updateSkill(skill, skill.skillId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def postEdit = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])

        then:
        preEdit.name == skillNamePreEdit
        preEdit.totalPoints == 250
        preEdit.numPerformToCompletion == 1
        preEdit.description == skillDescriptionPreEdit
        preEdit.helpUrl == skillHelpUrlPreEdit
        !preEdit.selfReportingType
        !preEdit.iconClass
        postEdit.name == skill.name
        postEdit.totalPoints == 12500
        postEdit.numPerformToCompletion == 50
        postEdit.description == skill.description
        postEdit.helpUrl == skill.helpUrl
        postEdit.selfReportingType == SkillDef.SelfReportingType.Approval.toString()
        postEdit.iconClass == 'fa fa-icon-test'
    }


    def "update skill imported from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def res = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)
        def importedSkill = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])
        importedSkill.name = "a new name"
        importedSkill.subjectId = p2subj1.subjectId
        skillsService.updateSkill(importedSkill, importedSkill.skillId)

        then:
        def e = thrown(Exception)
        e.getMessage().contains("errorCode:ReadOnlySkill")
    }


    def "description, helpUrl, selfReportingType, iconClass fields present on skill imported from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skill.name = "foo name"
        skill.numPerformToCompletion = 50
        skill.helpUrl = "http://newHelpUrl"
        skill.description = "updated description"
        skill.iconClass = 'fa fa-icon-test'
        skill.selfReportingType = SkillDef.SelfReportingType.Approval.toString()

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def res = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)
        def importedSkill = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])

        then:
        importedSkill.name == skill.name
        importedSkill.numPerformToCompletion == skill.numPerformToCompletion
        importedSkill.helpUrl == skill.helpUrl
        importedSkill.description == skill.description
        importedSkill.selfReportingType == skill.selfReportingType
        importedSkill.iconClass == skill.iconClass
    }


    def "changes in points per increment of exported skill are not replicated to imported skills (modifications are propagated to imported fields on async basis)"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1_skills = (1..3).collect {createSkill(1, 1, it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1_skills)
        p1_skills.each { skillsService.exportSkillToCatalog(project1.projectId, it.skillId) }

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2_skills = (1..3).collect {createSkill(2, 1, 3+it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, p2_skills)
        p2_skills.each { skillsService.exportSkillToCatalog(project2.projectId, it.skillId) }

        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, p1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        p1_skills[0].pointIncrement = 5000
        skillsService.createSkills([p1_skills[0]])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        then:
        //projectId, subjectId, skillId
        skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: p1_skills[0].skillId]).pointIncrement == 250
        skillsService.getSkill([projectId: project1.projectId, subjectId: p1subj1.subjectId, skillId: p1_skills[0].skillId]).pointIncrement == 5000
    }


    def "changes in number of occurrences of exported skill ARE replicated to imported skills (modifications are propagated to imported fields on async basis)"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1_skills = (1..3).collect {createSkill(1, 1, it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1_skills)
        p1_skills.each { skillsService.exportSkillToCatalog(project1.projectId, it.skillId) }

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2_skills = (1..3).collect {createSkill(2, 1, 3+it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, p2_skills)
        p2_skills.each { skillsService.exportSkillToCatalog(project2.projectId, it.skillId) }

        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, p1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        p1_skills[0].pointIncrement = 250
        p1_skills[0].numPerformToCompletion = 10
        skillsService.createSkills([p1_skills[0]])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        //projectId, subjectId, skillId
        skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: p1_skills[0].skillId]).totalPoints == 2500
        skillsService.getSkill([projectId: project1.projectId, subjectId: p1subj1.subjectId, skillId: p1_skills[0].skillId]).totalPoints == 2500
    }


    def "changes in the number of occurrences of an exported skill should cause changes in the level thresholds for the importing project and subject"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1_skills = (1..4).collect {createSkill(1, 1, it, 0, 1, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1_skills)
        p1_skills.each { skillsService.exportSkillToCatalog(project1.projectId, it.skillId) }

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2_skills = (1..4).collect {createSkill(2, 1, 3+it, 0, 1, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, p2_skills)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, p1_skills[0].skillId)

        List<LevelDefinitionRes> subjectLevelsPreEdit = levelDefinitionStorageService.getLevels(project2.projectId, p2subj1.subjectId)
        List<LevelDefinitionRes> projectLevelsPreEdit = levelDefinitionStorageService.getLevels(project2.projectId)

        println "subject pre: "+subjectLevelsPreEdit
        println "project pre: "+projectLevelsPreEdit

        when:
        p1_skills[0].numPerformToCompletion = 5
        skillsService.createSkills([p1_skills[0]])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        List<LevelDefinitionRes> subjectLevelsPostEdit = levelDefinitionStorageService.getLevels(project2.projectId, p2subj1.subjectId)
        List<LevelDefinitionRes> projectLevelsPostEdit = levelDefinitionStorageService.getLevels(project2.projectId)

        println "subject post: "+subjectLevelsPostEdit
        println "project post: "+projectLevelsPostEdit

        then:
        subjectLevelsPreEdit[0].pointsFrom == 125
        subjectLevelsPreEdit[1].pointsFrom == 312
        subjectLevelsPreEdit[2].pointsFrom == 562
        subjectLevelsPreEdit[3].pointsFrom == 837
        subjectLevelsPreEdit[4].pointsFrom == 1150
        projectLevelsPreEdit[0].pointsFrom == 125
        projectLevelsPreEdit[1].pointsFrom == 312
        projectLevelsPreEdit[2].pointsFrom == 562
        projectLevelsPreEdit[3].pointsFrom == 837
        projectLevelsPreEdit[4].pointsFrom == 1150
        subjectLevelsPostEdit[0].pointsFrom == 225
        subjectLevelsPostEdit[0].pointsFrom == 225
    }

}
