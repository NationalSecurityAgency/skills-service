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
package skills.intTests


import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.storage.model.Attachment
import skills.storage.repos.AttachmentRepo

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class SkillDefinitionSpecs extends DefaultIntSpec {

    @Autowired
    AttachmentRepo attachmentRepo

    def "skills copied on UI that have a single attachment in the description will need that attachment to be duplicated for the copied skill"() {
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

        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        skill2.description = "Here is a [Link](${attachmentHref})".toString()

        when:
        String fileContent_t1 = skillsService.downloadAttachmentAsText(attachmentHref)
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def skillRes_t1 = skillsService.getSkill(skill)

        skillsService.createSkill(skill2)
        String fileContent_t2 = skillsService.downloadAttachmentAsText(attachmentHref)
        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        def skillRes_t2 = skillsService.getSkill(skill)
        def skill2Res_t2 = skillsService.getSkill(skill2)

        skillsService.deleteSkill(skill2)
        String fileContent_t3 = skillsService.downloadAttachmentAsText(attachmentHref)
        List<Attachment> attachments_t3 = attachmentRepo.findAll().toList()
        def skillRes_t3 = skillsService.getSkill(skill)

        then:
        fileContent_t1 == fileContent
        attachments_t1.projectId == [project1.projectId]
        attachments_t1.skillId == [skill.skillId]
        skillRes_t1.description == skill.description

        fileContent_t2 == fileContent
        attachments_t2.projectId == [project1.projectId, project1.projectId]
        attachments_t2.skillId.sort() == [skill.skillId, skill2.skillId].sort()
        attachments_t2[0].uuid != attachments_t2[1].uuid
        skillRes_t2.description == skill.description
        skillRes_t2.description.contains(attachments_t2.find { it.skillId == skill.skillId}.uuid)
        skill2Res_t2.description.contains(attachments_t2.find { it.skillId == skill2.skillId}.uuid)

        fileContent_t3 == fileContent
        attachments_t3.projectId == [project1.projectId]
        attachments_t3.skillId == [skill.skillId]
        skillRes_t3.description == skill.description
        skillRes_t3.description.contains(attachments_t2[0].uuid)
    }

    def "skills copied on UI that have multiple attachments in descriptions will need those attachments to be duplicated for the copied skill"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skill.description = "description"
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [])

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, project1.projectId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, project1.projectId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href
        skill.description = ("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()
        skillsService.createSkill(skill)

        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        skill2.description = skill.description.toString()

        when:
        String fileContent_t1 = skillsService.downloadAttachmentAsText(attachmentHref)
        String file2Content_t1 = skillsService.downloadAttachmentAsText(attachment2Href)
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def skillRes_t1 = skillsService.getSkill([skillId: skill.skillId, projectId: project1.projectId, subjectId: p1subj1.subjectId])

        skillsService.createSkill(skill2)
        String fileContent_t2 = skillsService.downloadAttachmentAsText(attachmentHref)
        String file2Content_t2 = skillsService.downloadAttachmentAsText(attachment2Href)
        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        def skillRes_t2 = skillsService.getSkill([skillId: skill.skillId, projectId: project1.projectId, subjectId: p1subj1.subjectId])
        def skill2Res_t2 = skillsService.getSkill([skillId: skill2.skillId, projectId: project1.projectId, subjectId: p1subj1.subjectId])

        skillsService.deleteSkill(skill2)
        String fileContent_t3 = skillsService.downloadAttachmentAsText(attachmentHref)
        String file2Content_t3 = skillsService.downloadAttachmentAsText(attachment2Href)
        List<Attachment> attachments_t3 = attachmentRepo.findAll().toList()
        def skillRes_t3 = skillsService.getSkill([skillId: skill.skillId, projectId: project1.projectId, subjectId: p1subj1.subjectId])
        then:
        fileContent_t1 == fileContent1
        file2Content_t1 == fileContent2
        attachments_t1.projectId == [project1.projectId, project1.projectId]
        attachments_t1.skillId == [skill.skillId, skill.skillId]
        skillRes_t1.description == skill.description

        fileContent_t2 == fileContent1
        file2Content_t2 == fileContent2
        attachments_t2.projectId == [project1.projectId, project1.projectId, project1.projectId, project1.projectId]
        attachments_t2.skillId.sort() == [skill.skillId, skill2.skillId, skill.skillId, skill2.skillId].sort()
        skillRes_t2.description == skill.description
        List<Attachment> skill1Attachments = attachments_t2.findAll { it.skillId == skill.skillId}
        List<Attachment> skill2Attachments = attachments_t2.findAll { it.skillId == skill2.skillId}
        skillRes_t2.description.contains(skill1Attachments[0].uuid)
        skillRes_t2.description.contains(skill1Attachments[1].uuid)
        skill2Res_t2.description.contains(skill2Attachments[0].uuid)
        skill2Res_t2.description.contains(skill2Attachments[1].uuid)

        attachments_t3.projectId == [project1.projectId, project1.projectId]
        attachments_t3.skillId == [skill.skillId, skill.skillId]
        skillRes_t3.description == skill.description
        skillRes_t3.description.contains(skill1Attachments[0].uuid)
        skillRes_t3.description.contains(skill1Attachments[1].uuid)
        fileContent_t3 == fileContent1
        file2Content_t3 == fileContent2
    }

    def "when skill is edited projectId and skillId is properly set in the attachment in the description "() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        String descriptionWithoutAttachments = "description"
        skill.description = descriptionWithoutAttachments
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [skill])

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, null)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, null)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href



        when:
        String fileContent_t1 = skillsService.downloadAttachmentAsText(attachmentHref)
        String file2Content_t1 = skillsService.downloadAttachmentAsText(attachment2Href)
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def skillRes_t1 = skillsService.getSkill([skillId: skill.skillId, projectId: project1.projectId, subjectId: p1subj1.subjectId])

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()
        skill.description = descriptionWithAttachments

        skillsService.updateSkill(skill, skill.skillId)

        String fileContent_t2 = skillsService.downloadAttachmentAsText(attachmentHref)
        String file2Content_t2 = skillsService.downloadAttachmentAsText(attachment2Href)
        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        def skillRes_t2 = skillsService.getSkill([skillId: skill.skillId, projectId: project1.projectId, subjectId: p1subj1.subjectId])

        then:
        fileContent_t1 == fileContent1
        file2Content_t1 == fileContent2
        attachments_t1.projectId == [null, null]
        attachments_t1.skillId == [null, null]
        skillRes_t1.description == descriptionWithoutAttachments

        fileContent_t2 == fileContent1
        file2Content_t2 == fileContent2
        attachments_t2.projectId == [project1.projectId, project1.projectId]
        attachments_t2.skillId.sort() == [skill.skillId, skill.skillId].sort()
        skillRes_t2.description == descriptionWithAttachments
        List<Attachment> skill1Attachments = attachments_t2.findAll { it.skillId == skill.skillId}
        skillRes_t2.description.contains(skill1Attachments[0].uuid)
        skillRes_t2.description.contains(skill1Attachments[1].uuid)
    }

    def "when skill is reused attachments are not copied but are also reused and are linked to the original skill"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, null)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, null)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()
        skill.description = descriptionWithAttachments

        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [skill])
        skillsService.createSubject(p1subj2)

        when:
        skillsService.reuseSkills(project1.projectId, [skill.skillId], p1subj2.subjectId)

        List<Attachment> attachments = attachmentRepo.findAll().toList()
        def skill1Res = skillsService.getSkill([skillId: skill.skillId, projectId: project1.projectId, subjectId: p1subj1.subjectId])
        def skill2Res = skillsService.getSkill([skillId: SkillReuseIdUtil.addTag(skill.skillId, 0), projectId: project1.projectId, subjectId: p1subj2.subjectId])

        String fileContent3= 'Text in a file3'
        def uploadedAttachment3Res = skillsService.uploadAttachment('test3-pdf.pdf', fileContent3, null)
        String attachment3Href = uploadedAttachment3Res.href
        String description2WithAttachment = "Just one: [File3.pdf](${attachment3Href})".toString()
        skill.description = description2WithAttachment
        skillsService.updateSkill(skill, skill.skillId)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        def skill1Res_t2 = skillsService.getSkill([skillId: skill.skillId, projectId: project1.projectId, subjectId: p1subj1.subjectId])
        def skill2Res_t2 = skillsService.getSkill([skillId: SkillReuseIdUtil.addTag(skill.skillId, 0), projectId: project1.projectId, subjectId: p1subj2.subjectId])

        then:
        attachments.projectId == [project1.projectId, project1.projectId]
        attachments.skillId.sort() == [skill.skillId, skill.skillId].sort()
        skill1Res.description == descriptionWithAttachments
        skill2Res.description == descriptionWithAttachments

        attachments_t2.projectId == [project1.projectId, project1.projectId, project1.projectId]
        attachments_t2.skillId.sort() == [skill.skillId, skill.skillId, skill.skillId].sort()
        skill1Res_t2.description == description2WithAttachment
        skill2Res_t2.description == description2WithAttachment
    }

    def "when skill is imported attachments are not copied but are reused and are linked to the original skill"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, null)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, null)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()
        skill.description = descriptionWithAttachments

        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [skill])

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, [])

        when:
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        List<Attachment> attachments = attachmentRepo.findAll().toList()
        def skill1Res = skillsService.getSkill([skillId: skill.skillId, projectId: project1.projectId, subjectId: p1subj1.subjectId])
        def skill2Res = skillsService.getSkill([skillId: skill.skillId, projectId: project2.projectId, subjectId: p2subj1.subjectId])

        String fileContent3= 'Text in a file3'
        def uploadedAttachment3Res = skillsService.uploadAttachment('test3-pdf.pdf', fileContent3, null)
        String attachment3Href = uploadedAttachment3Res.href
        String description2WithAttachment = "Just one: [File3.pdf](${attachment3Href})".toString()
        skill.description = description2WithAttachment
        skillsService.updateSkill(skill, skill.skillId)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        def skill1Res_t2 = skillsService.getSkill([skillId: skill.skillId, projectId: project1.projectId, subjectId: p1subj1.subjectId])
        def skill2Res_t2 = skillsService.getSkill([skillId: skill.skillId, projectId: project2.projectId, subjectId: p2subj1.subjectId])

        then:
        attachments.projectId == [project1.projectId, project1.projectId]
        attachments.skillId.sort() == [skill.skillId, skill.skillId].sort()
        skill1Res.description == descriptionWithAttachments
        skill2Res.description == descriptionWithAttachments

        attachments_t2.projectId == [project1.projectId, project1.projectId, project1.projectId]
        attachments_t2.skillId.sort() == [skill.skillId, skill.skillId, skill.skillId].sort()
        skill1Res_t2.description == description2WithAttachment
        skill2Res_t2.description == description2WithAttachment
    }

}

