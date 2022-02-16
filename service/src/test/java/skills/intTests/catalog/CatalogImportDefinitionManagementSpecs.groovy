package skills.intTests.catalog

import groovy.json.JsonOutput
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import spock.lang.IgnoreRest

import static skills.intTests.utils.SkillsFactory.*

class CatalogImportDefinitionManagementSpecs extends DefaultIntSpec {

    def "import skill from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 250)

        def p2_skill = createSkill(2, 1, 5, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(p2_skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        when:
        def projectPreImport = skillsService.getProject(project2.projectId)
        def subjectPreImport = skillsService.getSubject(p2subj1)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def projectPostImport1 = skillsService.getProject(project2.projectId)
        def subjectPostImport1 = skillsService.getSubject(p2subj1)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        def projectPostImport2 = skillsService.getProject(project2.projectId)
        def subjectPostImport2 = skillsService.getSubject(p2subj1)

        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        def projectPostFinalize = skillsService.getProject(project2.projectId)
        def subjectPostFinalize = skillsService.getSubject(p2subj1)

        then:
        projectPreImport.totalPoints == 250
        projectPostImport1.totalPoints == 250
        projectPostImport2.totalPoints == 250
        subjectPreImport.totalPoints == 250
        subjectPostImport1.totalPoints == 250
        subjectPostImport2.totalPoints == 250

        projectPostFinalize.totalPoints == 750
        subjectPostFinalize.totalPoints == 750
    }

    def "import skill from catalog - multiple subjects - multiple projects - multiple skills"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(8, 1, 1, 88)
        def p1subj2 = createSubject(1, 2)
        def p1Subj2Skills = createSkills(6, 1, 2, 99)
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1Subj1Skills)
        skillsService.createSubject(p1subj2)
        skillsService.createSkills(p1Subj2Skills)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Subj1Skills = createSkills(2, 2, 1, 88)
        def p2subj2 = createSubject(2, 2)
        def p2Subj2Skills = createSkills(2, 2, 2, 99)
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, p2Subj1Skills)
        skillsService.createSubject(p2subj2)
        skillsService.createSkills(p2Subj2Skills)

        def project3 = createProject(3)
        def p3subj1 = createSubject(3, 1)
        def p3Subj1Skills = createSkills(8, 3, 1, 24)
        def p3subj2 = createSubject(3, 2)
        def p3Subj2Skills = createSkills(8, 3, 2, 35)
        skillsService.createProjectAndSubjectAndSkills(project3, p3subj1, p3Subj1Skills)
        skillsService.createSubject(p3subj2)
        skillsService.createSkills(p3Subj2Skills)

        skillsService.exportSkillToCatalog(project1.projectId, p1Subj1Skills[4].skillId)
        skillsService.exportSkillToCatalog(project1.projectId, p1Subj1Skills[5].skillId)
        skillsService.exportSkillToCatalog(project1.projectId, p1Subj2Skills[4].skillId)
        skillsService.exportSkillToCatalog(project1.projectId, p1Subj2Skills[5].skillId)
        skillsService.exportSkillToCatalog(project3.projectId, p3Subj2Skills[6].skillId)
        skillsService.exportSkillToCatalog(project3.projectId, p3Subj2Skills[7].skillId)

        when:
        def projectPreImport = skillsService.getProject(project2.projectId)
        def subject1PreImport = skillsService.getSubject(p2subj1)
        def subject2PreImport = skillsService.getSubject(p2subj2)
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId,
                [
                        [ projectId: project1.projectId, skillId: p1Subj1Skills[4].skillId],
                        [ projectId: project1.projectId, skillId: p1Subj2Skills[5].skillId],
                        [ projectId: project3.projectId, skillId: p3Subj2Skills[7].skillId],
                ])
        def projectPostImport = skillsService.getProject(project2.projectId)
        def subject1PostImport = skillsService.getSubject(p2subj1)
        def subject2PostImport = skillsService.getSubject(p2subj2)

        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)

        def projectPostFinalize = skillsService.getProject(project2.projectId)
        def subject1PostFinalize = skillsService.getSubject(p2subj1)
        def subject2PostFinalize = skillsService.getSubject(p2subj2)

        then:
        projectPreImport.totalPoints == 374
        subject1PreImport.totalPoints == 176
        subject2PreImport.totalPoints == 198

        projectPostImport.totalPoints == 374
        subject1PostImport.totalPoints == 176
        subject2PostImport.totalPoints == 198

        projectPostFinalize.totalPoints == 374 + 88 + 99 + 35
        subject1PostFinalize.totalPoints == 176 + 88 + 99 + 35
        subject2PostFinalize.totalPoints == 198
    }

    def "import skill from catalog into project with group where group has child skills"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 250)

        def p2_skill = createSkill(2, 1, 5, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(p2_skill)

        def skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 6)
        def p2_gskill1 = createSkill(2, 1, 7, 0, 1, 0, 80)
        def p2_gskill2 = createSkill(2, 1, 8, 0, 1, 0, 80)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skillsService.assignSkillToSkillsGroup(skillsGroupId, p2_gskill1)
        skillsService.assignSkillToSkillsGroup(skillsGroupId, p2_gskill2)
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)


        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        when:
        def projectPreImport = skillsService.getProject(project2.projectId)
        def subjectPreImport = skillsService.getSubject(p2subj1)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def projectPostImport1 = skillsService.getProject(project2.projectId)
        def subjectPostImport1 = skillsService.getSubject(p2subj1)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        def projectPostImport2 = skillsService.getProject(project2.projectId)
        def subjectPostImport2 = skillsService.getSubject(p2subj1)

        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        def projectPostFinalize = skillsService.getProject(project2.projectId)
        def subjectPostFinalize = skillsService.getSubject(p2subj1)

        then:
        projectPreImport.totalPoints == 410
        projectPostImport1.totalPoints == 410
        projectPostImport2.totalPoints == 410
        subjectPreImport.totalPoints == 410
        subjectPostImport1.totalPoints == 410
        subjectPostImport2.totalPoints == 410

        projectPostFinalize.totalPoints == 910
        subjectPostFinalize.totalPoints == 910
    }

    def "imported skills are disabled until finalized"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(8, 1, 1, 88)
        def p1subj2 = createSubject(1, 2)
        def p1Subj2Skills = createSkills(6, 1, 2, 99)
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1Subj1Skills)
        skillsService.createSubject(p1subj2)
        skillsService.createSkills(p1Subj2Skills)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Subj1Skills = createSkills(2, 2, 1, 88)
        def p2subj2 = createSubject(2, 2)
        def p2Subj2Skills = createSkills(2, 2, 2, 99)
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, p2Subj1Skills)
        skillsService.createSubject(p2subj2)
        skillsService.createSkills(p2Subj2Skills)

        skillsService.exportSkillToCatalog(project1.projectId, p1Subj1Skills[4].skillId)
        skillsService.exportSkillToCatalog(project1.projectId, p1Subj1Skills[5].skillId)
        skillsService.exportSkillToCatalog(project1.projectId, p1Subj2Skills[4].skillId)
        skillsService.exportSkillToCatalog(project1.projectId, p1Subj2Skills[5].skillId)

        when:
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId,
                [
                        [ projectId: project1.projectId, skillId: p1Subj1Skills[4].skillId],
                        [ projectId: project1.projectId, skillId: p1Subj2Skills[5].skillId],
                ])
        def skillsBefore = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)
        println JsonOutput.toJson(skillsBefore)

        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)

        def skillsAfter = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)

        then:
        skillsBefore.size() == 4
        skillsBefore.find { it.skillId == p1Subj1Skills[4].skillId }.enabled == false
        skillsBefore.find { it.skillId == p1Subj2Skills[5].skillId }.enabled == false
        skillsBefore.find { it.skillId == p2Subj1Skills[0].skillId }.enabled == true
        skillsBefore.find { it.skillId == p2Subj1Skills[1].skillId }.enabled == true

        skillsAfter.size() == 4
        skillsAfter.find { it.skillId == p1Subj1Skills[4].skillId }.enabled == true
        skillsAfter.find { it.skillId == p1Subj2Skills[5].skillId }.enabled == true
        skillsAfter.find { it.skillId == p2Subj1Skills[0].skillId }.enabled == true
        skillsAfter.find { it.skillId == p2Subj1Skills[1].skillId }.enabled == true
    }
}
