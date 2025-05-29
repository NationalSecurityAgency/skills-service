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
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.model.Attachment
import skills.storage.model.SkillDef
import skills.storage.repos.AttachmentRepo

import static skills.intTests.utils.SkillsFactory.*

class CatalogImportDefinitionManagementSpecs extends CatalogIntSpec {

    @Autowired
    AttachmentRepo attachmentRepo

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
        def projectsPreImport = skillsService.getProjects()
        def subjectPreImport = skillsService.getSubject(p2subj1)
        def subjectsPreImport = skillsService.getSubjects(project2.projectId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def projectPostImport1 = skillsService.getProject(project2.projectId)
        def projectsPostImport1 = skillsService.getProjects()
        def subjectPostImport1 = skillsService.getSubject(p2subj1)
        def subjectsPostImport1 = skillsService.getSubjects(project2.projectId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        def projectPostImport2 = skillsService.getProject(project2.projectId)
        def projectsPostImport2 = skillsService.getProjects()
        def subjectPostImport2 = skillsService.getSubject(p2subj1)
        def subjectsPostImport2 = skillsService.getSubjects(project2.projectId)

        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        def projectPostFinalize = skillsService.getProject(project2.projectId)
        def projectsPostFinalize = skillsService.getProjects()
        def subjectPostFinalize = skillsService.getSubject(p2subj1)
        def subjectsPostFinalize = skillsService.getSubjects(project2.projectId)

        then:
        projectPreImport.totalPoints == 250
        projectPostImport1.totalPoints == 250
        projectPostImport2.totalPoints == 250
        subjectPreImport.totalPoints == 250
        subjectPostImport1.totalPoints == 250
        subjectPostImport2.totalPoints == 250

        projectPostFinalize.totalPoints == 750
        subjectPostFinalize.totalPoints == 750

        subjectPreImport.numSkillsDisabled == 0
        subjectPostImport1.numSkillsDisabled == 1
        subjectPostImport2.numSkillsDisabled == 2
        subjectPostFinalize.numSkillsDisabled == 0

        projectPreImport.numSkillsDisabled == 0
        projectPostImport1.numSkillsDisabled == 1
        projectPostImport2.numSkillsDisabled == 2
        projectPostFinalize.numSkillsDisabled == 0

        subjectsPreImport[0].numSkillsDisabled == 0
        subjectsPostImport1[0].numSkillsDisabled == 1
        subjectsPostImport2[0].numSkillsDisabled == 2
        subjectsPostFinalize[0].numSkillsDisabled == 0

        projectsPreImport.find({ it.projectId == project1.projectId }).numSkillsDisabled == 0
        projectsPreImport.find({ it.projectId == project2.projectId }).numSkillsDisabled == 0
        projectsPostImport1.find({ it.projectId == project1.projectId }).numSkillsDisabled == 0
        projectsPostImport1.find({ it.projectId == project2.projectId }).numSkillsDisabled == 1
        projectsPostImport2.find({ it.projectId == project1.projectId }).numSkillsDisabled == 0
        projectsPostImport2.find({ it.projectId == project2.projectId }).numSkillsDisabled == 2
        projectsPostFinalize.find({ it.projectId == project1.projectId }).numSkillsDisabled == 0
        projectsPostFinalize.find({ it.projectId == project2.projectId }).numSkillsDisabled == 0
    }

    def "delete imported skill with attachments, attachments should be preserved"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skill.description = "description"
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [])

        String fileContent = 'Text in a file'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test-pdf.pdf', 'Text in a file', project1.projectId)
        String attachmentHref = uploadedAttachmentRes.href
        skill.description = "Here is a [Link](${attachmentHref})".toString()
        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, [])

        when:
        String fileContent_t1 = skillsService.downloadAttachmentAsText(attachmentHref)
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def skillRes_t1 = skillsService.getSkill(skill)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        String fileContent_t2 = skillsService.downloadAttachmentAsText(attachmentHref)
        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        def skillRes_t2 = skillsService.getSkill(skill)

        skillsService.deleteSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])
        String fileContent_t3 = skillsService.downloadAttachmentAsText(attachmentHref)
        List<Attachment> attachments_t3 = attachmentRepo.findAll().toList()
        def skillRes_t3 = skillsService.getSkill(skill)

        then:
        fileContent_t1 == fileContent
        attachments_t1.projectId == [project1.projectId]
        attachments_t1.skillId == [skill.skillId]
        skillRes_t1.description == skill.description

        fileContent_t2 == fileContent
        attachments_t2.projectId == [project1.projectId]
        attachments_t2.skillId == [skill.skillId]
        skillRes_t2.description == skill.description

        fileContent_t3 == fileContent
        attachments_t3.projectId == [project1.projectId]
        attachments_t3.skillId == [skill.skillId]
        skillRes_t3.description == skill.description
    }

    def "able to deploy skill with a missing attachment"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skill.description = "description"
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [])

        String fileContent = 'Text in a file'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test-pdf.pdf', 'Text in a file', project1.projectId)
        String attachmentHref = uploadedAttachmentRes.href
        skill.description = "Here is a [Link](${attachmentHref})".toString()
        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, [])

        attachmentRepo.deleteAll()

        when:
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        List<Attachment> attachmentsRes = attachmentRepo.findAll().toList()
        def p1SkillRes = skillsService.getSkill(skill)
        def p2SkillRes = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])

        then:
        attachmentsRes == []
        p1SkillRes.description == skill.description
        p2SkillRes.description == skill.description
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
                        [projectId: project1.projectId, skillId: p1Subj1Skills[4].skillId],
                        [projectId: project1.projectId, skillId: p1Subj2Skills[5].skillId],
                        [projectId: project3.projectId, skillId: p3Subj2Skills[7].skillId],
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
                        [projectId: project1.projectId, skillId: p1Subj1Skills[4].skillId],
                        [projectId: project1.projectId, skillId: p1Subj2Skills[5].skillId],
                ])
        def skillsBefore = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)

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

    def "do not allow disabled skills to be added to a badge"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        def p2badge1 = createBadge(2, 11)
        skillsService.createBadge(p2badge1)

        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)

        when:
        skillsService.assignSkillToBadge(project2.p.projectId, p2badge1.badgeId, project1.s1_skills[0].skillId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Skill [skill11] is not enabled")
    }

    def "retrieve finalization info"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        // group disabled skills do not contribute to the counts
        def skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 25)
        skillsService.createSkill(skillsGroup)
        skillsService.assignSkillToSkillsGroup(skillsGroup.skillId, createSkill(2, 1, 26))

        when:
        def res0 = skillsService.getCatalogFinalizeInfo(project2.p.projectId)
        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)
        def res1 = skillsService.getCatalogFinalizeInfo(project2.p.projectId)
        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[1].skillId)
        def res2 = skillsService.getCatalogFinalizeInfo(project2.p.projectId)
        skillsService.finalizeSkillsImportFromCatalog(project2.p.projectId)
        def res3 = skillsService.getCatalogFinalizeInfo(project2.p.projectId)
        then:
        res0.numSkillsToFinalize == 0
        res1.numSkillsToFinalize == 1
        res2.numSkillsToFinalize == 2
        res3.numSkillsToFinalize == 0
    }

    def "retrieve finalization info for multiple projects"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)
        def project3 = createProjWithCatalogSkills(3)

        // group disabled skills do not contribute to the counts
        def skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 25)
        skillsService.createSkill(skillsGroup)
        skillsService.assignSkillToSkillsGroup(skillsGroup.skillId, createSkill(2, 1, 26))

        when:
        def p1res0 = skillsService.getCatalogFinalizeInfo(project1.p.projectId)
        def p2res0 = skillsService.getCatalogFinalizeInfo(project2.p.projectId)
        def p3res0 = skillsService.getCatalogFinalizeInfo(project3.p.projectId)

        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)
        def p1res1 = skillsService.getCatalogFinalizeInfo(project1.p.projectId)
        def p2res1 = skillsService.getCatalogFinalizeInfo(project2.p.projectId)
        def p3res1 = skillsService.getCatalogFinalizeInfo(project3.p.projectId)

        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[1].skillId)
        skillsService.importSkillFromCatalog(project3.p.projectId, project3.s1.subjectId, project1.p.projectId, project1.s1_skills[1].skillId)
        def p1res2 = skillsService.getCatalogFinalizeInfo(project1.p.projectId)
        def p2res2 = skillsService.getCatalogFinalizeInfo(project2.p.projectId)
        def p3res2 = skillsService.getCatalogFinalizeInfo(project3.p.projectId)

        skillsService.finalizeSkillsImportFromCatalog(project2.p.projectId, false)
        def p1res3 = skillsService.getCatalogFinalizeInfo(project1.p.projectId)
        def p2res3 = skillsService.getCatalogFinalizeInfo(project2.p.projectId)
        def p3res3 = skillsService.getCatalogFinalizeInfo(project3.p.projectId)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def p1res4 = skillsService.getCatalogFinalizeInfo(project1.p.projectId)
        def p2res4 = skillsService.getCatalogFinalizeInfo(project2.p.projectId)
        def p3res4 = skillsService.getCatalogFinalizeInfo(project3.p.projectId)

        then:
        p1res0.numSkillsToFinalize == 0
        p1res0.isRunning == false
        p2res0.numSkillsToFinalize == 0
        p2res0.isRunning == false
        p3res0.numSkillsToFinalize == 0
        p3res0.isRunning == false

        p1res1.numSkillsToFinalize == 0
        p1res1.isRunning == false
        p2res1.numSkillsToFinalize == 1
        p2res1.isRunning == false
        p3res1.numSkillsToFinalize == 0
        p3res1.isRunning == false

        p1res2.numSkillsToFinalize == 0
        p1res2.isRunning == false
        p2res2.numSkillsToFinalize == 2
        p2res2.isRunning == false
        p3res2.numSkillsToFinalize == 1
        p3res2.isRunning == false

        p1res3.numSkillsToFinalize == 0
        p1res3.isRunning == false
        p2res3.numSkillsToFinalize == 2
        p2res3.isRunning == true
        p3res3.numSkillsToFinalize == 1
        p3res3.isRunning == false

        p1res4.numSkillsToFinalize == 0
        p1res4.isRunning == false
        p2res4.numSkillsToFinalize == 0
        p2res4.isRunning == false
        p3res4.numSkillsToFinalize == 1
        p3res4.isRunning == false
    }

    def "warn users when finalizing skills catalog if imported points are outside of the exiting point scheme"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj1 = SkillsFactory.createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1)
        skills[0].pointIncrement = 76
        skills[1].pointIncrement = 77
        skills[2].pointIncrement = 150
        skills[3].pointIncrement = 166
        skills[4].pointIncrement = 167
        skillsService.createProjectAndSubjectAndSkills(proj1, subj1, skills)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[0].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[1].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[2].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[3].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[4].skillId)

        def proj2 = SkillsFactory.createProject(2)
        def subj2 = SkillsFactory.createSubject(2, 2)
        def skills2 = SkillsFactory.createSkills(3, 2, 2)
        skills2[0].pointIncrement = 77
        skills2[1].pointIncrement = 130
        skills2[2].pointIncrement = 166
        skillsService.createProjectAndSubjectAndSkills(proj2, subj2, skills2)

        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[0].skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[1].skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[2].skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[3].skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[4].skillId)

        when:
        def proj2res1 = skillsService.getCatalogFinalizeInfo(proj2.projectId)
        println JsonOutput.toJson(proj2res1)
        then:
        proj2res1.projectId == "TestProject2"
        proj2res1.numSkillsToFinalize == 5
        proj2res1.projectSkillMinPoints == 77
        proj2res1.projectSkillMaxPoints == 166
        proj2res1.skillsWithOutOfBoundsPoints.size() == 2
        proj2res1.skillsWithOutOfBoundsPoints.find { it.skillName == skills[0].name }.totalPoints == skills[0].pointIncrement * skills[0].numPerformToCompletion
        proj2res1.skillsWithOutOfBoundsPoints.find { it.skillName == skills[4].name }.totalPoints == skills[4].pointIncrement * skills[4].numPerformToCompletion
    }

    def "warn users when finalizing skills catalog if imported points are outside of the exiting point scheme - no local skills means no warning"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj1 = SkillsFactory.createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1)
        skills[0].pointIncrement = 76
        skills[1].pointIncrement = 77
        skills[2].pointIncrement = 150
        skills[3].pointIncrement = 166
        skills[4].pointIncrement = 167
        skillsService.createProjectAndSubjectAndSkills(proj1, subj1, skills)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[0].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[1].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[2].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[3].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[4].skillId)

        def proj2 = SkillsFactory.createProject(2)
        def subj2 = SkillsFactory.createSubject(2, 2)
        skillsService.createProjectAndSubjectAndSkills(proj2, subj2, [])

        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[0].skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[1].skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[2].skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[3].skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[4].skillId)

        when:
        def proj2res1 = skillsService.getCatalogFinalizeInfo(proj2.projectId)
        println JsonOutput.toJson(proj2res1)
        then:
        proj2res1.projectId == "TestProject2"
        proj2res1.numSkillsToFinalize == 5
        !proj2res1.projectSkillMinPoints
        !proj2res1.projectSkillMaxPoints
        !proj2res1.skillsWithOutOfBoundsPoints
    }

    def "warn users when finalizing skills catalog if imported points are outside of the exiting point scheme - skills within range"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj1 = SkillsFactory.createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1)
        skills[0].pointIncrement = 76
        skills[1].pointIncrement = 77
        skills[2].pointIncrement = 150
        skills[3].pointIncrement = 166
        skills[4].pointIncrement = 167
        skillsService.createProjectAndSubjectAndSkills(proj1, subj1, skills)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[0].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[1].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[2].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[3].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, skills[4].skillId)

        def proj2 = SkillsFactory.createProject(2)
        def subj2 = SkillsFactory.createSubject(2, 2)
        def skills2 = SkillsFactory.createSkills(3, 2, 2)
        skills2[0].pointIncrement = 76
        skills2[1].pointIncrement = 130
        skills2[2].pointIncrement = 167
        skillsService.createProjectAndSubjectAndSkills(proj2, subj2, skills2)

        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[0].skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[1].skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[2].skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[3].skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skills[4].skillId)

        when:
        def proj2res1 = skillsService.getCatalogFinalizeInfo(proj2.projectId)
        println JsonOutput.toJson(proj2res1)
        then:
        proj2res1.projectId == "TestProject2"
        proj2res1.numSkillsToFinalize == 5
        proj2res1.projectSkillMinPoints == 76
        proj2res1.projectSkillMaxPoints == 167
        !proj2res1.skillsWithOutOfBoundsPoints
    }

    def "cannot import while finalizing"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)

        when:
        skillsService.finalizeSkillsImportFromCatalog(project2.p.projectId, false)

        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[1].skillId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Cannot import skills in the middle of the finalization process")
    }

    def "do not allow finalizing if imported skills belongs to a disabled subject"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)
        def disabledSubject = createSubject(2, 4)
        disabledSubject.enabled = false
        skillsService.createSubject(disabledSubject)

        skillsService.importSkillFromCatalog(project2.p.projectId, disabledSubject.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)

        when:
        skillsService.finalizeSkillsImportFromCatalog(project2.p.projectId, false)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Cannot finalize imported skills, there are [1] skill(s) pending finalization that belong to a disabled subject or group")
    }

    def "get all skills for a project does not include disabled skills by default"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)

        when:
        def skills = skillsService.getSkillsForProject(project2.p.projectId)
        def skillsWithDisabled = skillsService.getSkillsForProject(project2.p.projectId, "", false, true)
        then:
        skills.size() == 9
        !skills.find { it.skillId == project1.s1_skills[0].skillId }

        skillsWithDisabled.size() == 10
        skillsWithDisabled.find { it.skillId == project1.s1_skills[0].skillId }
    }

    def "cannot add a disabled skill to a badge"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)
        def badge = SkillsFactory.createBadge(2, 1)
        skillsService.createBadge(badge)
        when:
        skillsService.assignSkillToBadge([projectId: project2.p.projectId, badgeId: badge.badgeId, skillId: project1.s1_skills[0].skillId])

        then:
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Skill [${project1.s1_skills[0].skillId}] is not enabled")
    }

    def "ability to change points of the imported skill"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        Map skill = new HashMap<>(project1.s1_skills[0])
        skill.projectId = project2.p.projectId
        skill.subjectId = project1.s2.subjectId
        skill.pointIncrement = 33
        skill.enabled = "false"
        skillsService.createSkill(skill)
        def skill1 = skillsService.getSkill(skill)

        skillsService.finalizeSkillsImportFromCatalog(project2.p.projectId)
        skill.enabled = "true"
        skill.pointIncrement = 11
        skillsService.createSkill(skill)
        def skill2 = skillsService.getSkill(skill)

        then:
        skill1.totalPoints == 33 * 2
        skill2.totalPoints == 11 * 2
    }

    def "change points via update endpoint"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        Map skill = new HashMap<>(project1.s1_skills[0])
        skill.projectId = project2.p.projectId
        skill.subjectId = project1.s2.subjectId
        skillsService.updateImportedSkill(project2.p.projectId, skill.skillId, 33)
        def skill1 = skillsService.getSkill(skill)

        skillsService.finalizeSkillsImportFromCatalog(project2.p.projectId)
        skillsService.updateImportedSkill(project2.p.projectId, skill.skillId, 11)
        def skill2 = skillsService.getSkill(skill)

        then:
        skill1.totalPoints == 33 * 2
        skill2.totalPoints == 11 * 2
    }

    def "change points on imported self report skill"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def subj1 = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(2, 2)
        def skills1 = SkillsFactory.createSkills(10, 1, 1)
        def skills2 = SkillsFactory.createSkillsStartingAt(10, 11, 2, 2)

        skills2.eachWithIndex { skill, index ->
            if (index % 2 == 0) {
                skill.selfReportingType = SkillDef.SelfReportingType.Approval
            } else {
                skill.selfReportingType = SkillDef.SelfReportingType.HonorSystem
            }
        }

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills1)
        skillsService.createSkills(skills2)

        skills2.each {
            skillsService.exportSkillToCatalog(proj2.projectId, it.skillId)
        }

        skillsService.importSkillFromCatalog(proj1.projectId, subj1.subjectId, proj2.projectId, skills2[0].skillId)
        skillsService.importSkillFromCatalog(proj1.projectId, subj1.subjectId, proj2.projectId, skills2[1].skillId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        Map sellfReport1 = new HashMap<>(skills2[0])
        sellfReport1.projectId = proj1.projectId
        sellfReport1.subjectId = subj1.subjectId
        skillsService.updateImportedSkill(proj1.projectId, sellfReport1.skillId, 33)
        def skill1 = skillsService.getSkill(sellfReport1)

        Map sellfReport2 = new HashMap<>(skills2[1])
        sellfReport2.projectId = proj1.projectId
        sellfReport2.subjectId = subj1.subjectId
        skillsService.updateImportedSkill(proj1.projectId, sellfReport2.skillId, 34)
        def skill3 = skillsService.getSkill(sellfReport2)

        skillsService.finalizeSkillsImportFromCatalog(proj1.projectId)
        skillsService.updateImportedSkill(proj1.projectId, sellfReport1.skillId, 11)
        def skill2 = skillsService.getSkill(sellfReport1)

        skillsService.updateImportedSkill(proj1.projectId, sellfReport2.skillId, 12)
        def skill4 = skillsService.getSkill(sellfReport2)

        then:
        skill1.totalPoints == 33
        skill2.totalPoints == 11
        skill3.totalPoints == 34
        skill4.totalPoints == 12
    }

    def 'only points can be changed for an imported skill'() {

        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        skillsService.importSkillFromCatalogAndFinalize(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)

        Map skillTemplate = new HashMap<>(project1.s1_skills[0])
        skillTemplate.projectId = project2.p.projectId
        skillTemplate.subjectId = project1.s2.subjectId

        Closure<Map> createSkill = { String attr, def val ->
            Map toUpdateSkill = new HashMap<>(skillTemplate)
            toUpdateSkill[attr] = val
            return toUpdateSkill
        }

        expect:
        try {
            skillsService.createSkill(createSkill.call(attribute, value))
            assert !expectException
        }
        catch (SkillsClientException ex) {
            assert expectException
            assert ex.message.contains(expectedMessage)
        }

        where:
        attribute                            | value         || expectException | expectedMessage
        "name"                               | "blah blah"   || true            | "has been imported from the Global Catalog and only pointIncrement can be altered"
        "pointIncrementInterval"             | 120           || true            | "has been imported from the Global Catalog and only pointIncrement can be altered"
        "numMaxOccurrencesIncrementInterval" | 2             || true            | "has been imported from the Global Catalog and only pointIncrement can be altered"
        "numPerformToCompletion"             | 88            || true            | "has been imported from the Global Catalog and only pointIncrement can be altered"
        "version"                            | 1             || true            | "has been imported from the Global Catalog and only pointIncrement can be altered"
        "description"                        | "some"        || true            | "has been imported from the Global Catalog and only pointIncrement can be altered"
        "selfReportingType"                  | "HonorSystem" || true            | "has been imported from the Global Catalog and only pointIncrement can be altered"
        "enabled"                            | "false"       || true            | "has been imported from the Global Catalog and only pointIncrement can be altered"
        "dljalj"                             | null          || false           | null
    }

    def 'subject and project metric counts are updated after skill points are mutated'() {

        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        skillsService.importSkillFromCatalogAndFinalize(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)

        when:
        def subj1Stats = skillsService.getSubject(project2.s1)
        def subjStats = skillsService.getSubject(project2.s2)
        def projStats = skillsService.getProject(project2.p.projectId)
        skillsService.updateImportedSkill(project2.p.projectId, project1.s1_skills[0].skillId, 698)
        def subj1StatsAfter = skillsService.getSubject(project2.s1)
        def subjStatsAfter = skillsService.getSubject(project2.s2)
        def projStatsAfter = skillsService.getProject(project2.p.projectId)
        then:
        subj1Stats.totalPoints == 200 * 3
        subjStats.totalPoints == 200 * 4
        projStats.totalPoints == 200 * 10

        subj1StatsAfter.totalPoints == 200 * 3
        subjStatsAfter.totalPoints == 200 * 3 + (698 * 2)
        projStatsAfter.totalPoints == 200 * 9 + (698 * 2)
    }

    def 'subject and project metric counts are updated after skills are finalized'() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        when:
        def subj1Stats = skillsService.getSubject(project2.s1)
        def subjStats = skillsService.getSubject(project2.s2)
        def projStats = skillsService.getProject(project2.p.projectId)

        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)

        def subj1StatsAfterImport = skillsService.getSubject(project2.s1)
        def subjStatsAfterImport = skillsService.getSubject(project2.s2)
        def projStatsAfterImport = skillsService.getProject(project2.p.projectId)

        skillsService.finalizeSkillsImportFromCatalog(project2.p.projectId)

        def subj1StatsAfterFinalize = skillsService.getSubject(project2.s1)
        def subjStatsAfterFinalize = skillsService.getSubject(project2.s2)
        def projStatsAfterFinalize = skillsService.getProject(project2.p.projectId)

        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[1].skillId)

        def subj1StatsAfterAnotherImport = skillsService.getSubject(project2.s1)
        def subjStatsAfterAnotherImport = skillsService.getSubject(project2.s2)
        def projStatsAfterAnotherImport = skillsService.getProject(project2.p.projectId)

        then:
        subj1Stats.totalPoints == 200 * 3
        subjStats.totalPoints == 200 * 3
        projStats.totalPoints == 200 * 9

        subj1StatsAfterImport.totalPoints ==  200 * 3
        subjStatsAfterImport.totalPoints ==  200 * 3
        projStatsAfterImport.totalPoints == 200 * 9

        subj1StatsAfterFinalize.totalPoints ==  200 * 3
        subjStatsAfterFinalize.totalPoints ==  200 * 4
        projStatsAfterFinalize.totalPoints == 200 * 10

        subj1StatsAfterAnotherImport.totalPoints ==  200 * 3
        subjStatsAfterAnotherImport.totalPoints ==  200 * 4
        projStatsAfterAnotherImport.totalPoints == 200 * 10
    }

    def 'only enabled skills must contribute'() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        skillsService.importSkillFromCatalogAndFinalize(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)
        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[1].skillId)

        when:
        def subj1_t0 = skillsService.getSubject(project2.s1)
        def subj2_t0 = skillsService.getSubject(project2.s2)
        def proj_t0 = skillsService.getProject(project2.p.projectId)

        skillsService.updateImportedSkill(project2.p.projectId, project1.s1_skills[0].skillId, 33)

        def subj1_t1 = skillsService.getSubject(project2.s1)
        def subj2_t1 = skillsService.getSubject(project2.s2)
        def proj_t1 = skillsService.getProject(project2.p.projectId)

        then:
        subj1_t0.totalPoints == 200 * 3
        subj2_t0.totalPoints == 200 * 4
        proj_t0.totalPoints == 200 * 10

        subj1_t1.totalPoints == 200 * 3
        subj2_t1.totalPoints == 200 * 3 + (33 * 2)
        proj_t1.totalPoints == 200 * 9 +  + (33 * 2)
    }

    def 'disabled imported skills do not contribute to overall points - only imported skills'() {
        def project1 = createProjWithCatalogSkills(1)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProject(project2)
        skillsService.createSubject(p2subj1)

        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId,
                [
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[0].skillId],
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[1].skillId],
                ])

        when:
        def subj1_t0 = skillsService.getSubject(p2subj1)
        def proj_t0 = skillsService.getProject(project2.projectId)

        skillsService.updateImportedSkill(project2.projectId, project1.s1_skills[0].skillId, 33)

        def subj1_t1 = skillsService.getSubject(p2subj1)
        def proj_t1 = skillsService.getProject(project2.projectId)

        then:
        subj1_t0.totalPoints == 0
        proj_t0.totalPoints == 0

        subj1_t1.totalPoints == 0
        proj_t1.totalPoints == 0
    }

    def 'try to import skill from non-existent project'() {
        def project1 = createProjWithCatalogSkills(1)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProject(project2)
        skillsService.createSubject(p2subj1)


        when:
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId,
                [
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[0].skillId],
                        [projectId: "projectIdDoesNotExist", skillId: project1.s1_skills[0].skillId],
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[1].skillId],
                ])
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Failed to find project [projectIdDoesNotExist]")
    }

    def 'try to import skill that does not exist'() {
        def project1 = createProjWithCatalogSkills(1)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProject(project2)
        skillsService.createSubject(p2subj1)


        when:
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId,
                [
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[0].skillId],
                        [projectId: project1.p.projectId, skillId: "skillThatDoesNotExist"],
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[1].skillId],
                ])
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Skill [skillThatDoesNotExist] from project [TestProject1] has not been shared to the catalog")
    }

    def "cannot import when number of skills to be imported would exceed max skills per subject"() {
        def project1 = createProjWithCatalogSkills(1)
        /*
        def subj1_skills = (1..3).collect {createSkill(projNum, 1, projNum * 10 + it, 0,numPerformToCompletion, 480, 100) }
        def subj2_skills = (1..3).collect {createSkill(projNum, 2, projNum * 10 + it + 3, 0, numPerformToCompletion, 480, 100) }
        def subj3_skills = (1..3).collect {createSkill(projNum, 3, projNum * 10 + it + 6, 0, numPerformToCompletion, 480, 100) }
         */

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skills = createSkills(99, 2, 1)
        skillsService.createProject(project2)
        skillsService.createSubject(p2subj1)
        skillsService.createSkills(p2skills)

        when:
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId,
                [
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[0].skillId],
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[1].skillId],
                ])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Each Subject is limited to [100] Skills, currently [TestSubject1] has [99] Skills, importing [2] would exceed the maximum, errorCode:MaxSkillsThreshold")
    }

    def "cannot import when number of skills to be imported would exceed max skills per subject including pending finalization skills"() {
        def project1 = createProjWithCatalogSkills(1)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skills = createSkillsStartingAt(95, 9000, 2, 1)
        skillsService.createProject(project2)
        skillsService.createSubject(p2subj1)
        skillsService.createSkills(p2skills)
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId,
                [
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[0].skillId],
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[1].skillId],
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[2].skillId],
                        [projectId: project1.p.projectId, skillId: project1.s2_skills[0].skillId],
                ])

        when:
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId,
                [
                        [projectId: project1.p.projectId, skillId: project1.s2_skills[1].skillId],
                        [projectId: project1.p.projectId, skillId: project1.s2_skills[2].skillId],
                ])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Each Subject is limited to [100] Skills, currently [TestSubject1] has [99] Skills, importing [2] would exceed the maximum, errorCode:MaxSkillsThreshold")
    }

    def "cannot import when number of skills to be imported would exceed max skills per subject including group skills"() {
        def project1 = createProjWithCatalogSkills(1)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skills = createSkills(95, 2, 1)
        def group = createSkillsGroup(2, 1, 10001)
        def groupSkills = createSkillsStartingAt(4, 1001, 2, 1)

        skillsService.createProject(project2)
        skillsService.createSubject(p2subj1)
        skillsService.createSkills(p2skills)
        skillsService.createSkill(group)
        groupSkills.each {
            skillsService.assignSkillToSkillsGroup(group.skillId, it)
        }
        group.enabled = 'true'
        skillsService.updateSkill(group, null)

        when:
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId,
                [
                        [projectId: project1.p.projectId, skillId: project1.s2_skills[1].skillId],
                        [projectId: project1.p.projectId, skillId: project1.s2_skills[2].skillId],
                ])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Each Subject is limited to [100] Skills, currently [TestSubject1] has [99] Skills, importing [2] would exceed the maximum, errorCode:MaxSkillsThreshold")
    }

    def "cannot import when number of skills to be imported would exceed max skills per subject including disabled group skills"() {
        def project1 = createProjWithCatalogSkills(1)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skills = createSkills(95, 2, 1)
        def group = createSkillsGroup(2, 1, 10001)
        def groupSkills = createSkillsStartingAt(4, 1001, 2, 1)

        skillsService.createProject(project2)
        skillsService.createSubject(p2subj1)
        skillsService.createSkills(p2skills)
        skillsService.createSkill(group)
        groupSkills.each {
            skillsService.assignSkillToSkillsGroup(group.skillId, it)
        }

        when:
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId,
                [
                        [projectId: project1.p.projectId, skillId: project1.s2_skills[1].skillId],
                        [projectId: project1.p.projectId, skillId: project1.s2_skills[2].skillId],
                ])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Each Subject is limited to [100] Skills, currently [TestSubject1] has [99] Skills, importing [2] would exceed the maximum, errorCode:MaxSkillsThreshold")
    }

    def "removing the original skill updates UserPoints and subject/project definition points"() {
        def project1 = createProjWithCatalogSkills(1)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId,
                [
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[0].skillId],
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[1].skillId],
                ])

        List<String> users = getRandomUsers(2)
        skillsService.addSkill(project1.s1_skills[0], users[0])
        skillsService.addSkill(project1.s1_skills[1], users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def usr_1_proj2_summary_t0 = skillsService.getSkillSummary(users[0], project2.projectId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(usr_1_proj2_summary_t0))
        when:
        skillsService.deleteSkill(project1.s1_skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def usr_1_proj2_summary_t1 = skillsService.getSkillSummary(users[0], project2.projectId)
//        println JsonOutput.prettyPrint(JsonOutput.toJson(usr_1_proj2_summary_t1))
        then:
        usr_1_proj2_summary_t0.totalPoints == 400
        usr_1_proj2_summary_t0.points == 200
        usr_1_proj2_summary_t0.subjects[0].totalPoints == 400

        usr_1_proj2_summary_t1.totalPoints == 200
        usr_1_proj2_summary_t1.points == 100
        usr_1_proj2_summary_t1.subjects[0].totalPoints == 200
        projDefRepo.findByProjectId(project2.projectId).totalPoints == 200
        skillDefRepo.findByProjectIdAndSkillId(project2.projectId, p2subj1.subjectId).totalPoints == 200
    }

    def "removing the imported skill updates UserPoints and subject/project definition points"() {
        def project1 = createProjWithCatalogSkills(1)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId,
                [
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[0].skillId],
                        [projectId: project1.p.projectId, skillId: project1.s1_skills[1].skillId],
                ])

        List<String> users = getRandomUsers(2)
        skillsService.addSkill(project1.s1_skills[0], users[0])
        skillsService.addSkill(project1.s1_skills[1], users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def usr_1_proj2_summary_t0 = skillsService.getSkillSummary(users[0], project2.projectId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(usr_1_proj2_summary_t0))
        when:
        skillsService.deleteSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: project1.s1_skills[0].skillId])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def usr_1_proj2_summary_t1 = skillsService.getSkillSummary(users[0], project2.projectId)
        then:
        usr_1_proj2_summary_t0.totalPoints == 400
        usr_1_proj2_summary_t0.points == 200
        usr_1_proj2_summary_t0.subjects[0].totalPoints == 400

        usr_1_proj2_summary_t1.totalPoints == 200
        usr_1_proj2_summary_t1.points == 100
        usr_1_proj2_summary_t1.subjects[0].totalPoints == 200
        projDefRepo.findByProjectId(project2.projectId).totalPoints == 200
        skillDefRepo.findByProjectIdAndSkillId(project2.projectId, p2subj1.subjectId).totalPoints == 200
    }

    def "do not allow export if the project doesn't have sufficient points"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 50)

        skillsService.createProject(project1)
        skillsService.createSubject(p1subj1)
        skillsService.createSkill(skill)

        when:
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        then:
        SkillsClientException skillsClientException = thrown(SkillsClientException)
        skillsClientException.message.contains("Insufficient project points, export to catalog is disallowed")
    }

    def "do not allow export if the subject doesn't have sufficient points"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 50)
        def skill2 = createSkill(1, 2, 1, 0, 1, 0, 200)

        skillsService.createProject(project1)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p1subj2)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)

        when:
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        then:
        SkillsClientException skillsClientException = thrown(SkillsClientException)
        skillsClientException.message.contains("Insufficient Subject points, export to catalog is disallowed")
    }
}
