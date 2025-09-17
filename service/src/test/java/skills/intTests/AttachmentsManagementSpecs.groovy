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
import skills.intTests.utils.QuizDefFactory
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.quiz.QuizQuestionType
import skills.storage.model.Attachment
import skills.storage.model.SkillDef
import skills.storage.repos.AttachmentRepo

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class AttachmentsManagementSpecs extends DefaultIntSpec {

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
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, project1.projectId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, project1.projectId)
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
        attachments_t1.projectId == [project1.projectId, project1.projectId]
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
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [skill])
        skillsService.createSubject(p1subj2)
        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, project1.projectId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, project1.projectId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()
        skill.description = descriptionWithAttachments

        skillsService.updateSkill(skill)
        skillsService.createSubject(p1subj2)

        when:
        skillsService.reuseSkills(project1.projectId, [skill.skillId], p1subj2.subjectId)

        List<Attachment> attachments = attachmentRepo.findAll().toList()
        def skill1Res = skillsService.getSkill([skillId: skill.skillId, projectId: project1.projectId, subjectId: p1subj1.subjectId])
        def skill2Res = skillsService.getSkill([skillId: SkillReuseIdUtil.addTag(skill.skillId, 0), projectId: project1.projectId, subjectId: p1subj2.subjectId])

        String fileContent3= 'Text in a file3'
        def uploadedAttachment3Res = skillsService.uploadAttachment('test3-pdf.pdf', fileContent3, project1.projectId)
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
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [skill])

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, project1.projectId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, project1.projectId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()
        skill.description = descriptionWithAttachments
        skillsService.updateSkill(skill)


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
        def uploadedAttachment3Res = skillsService.uploadAttachment('test3-pdf.pdf', fileContent3, project1.projectId)
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

    def "when project is edited with the same description no new attachments are created"() {
        def project1 = createProject(1)
        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def p1subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [skill])

        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, project1.projectId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, project1.projectId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()
        project1.description = descriptionWithAttachments
        skillsService.updateProject(project1)

        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def projectRes_t1 = skillsService.getProjectDescription(project1.projectId)

        skillsService.updateProject(project1, project1.projectId)

        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        def projectRes_t2 = skillsService.getProjectDescription(project1.projectId)

        then:
        projectRes_t1.description == descriptionWithAttachments
        attachments_t1.projectId == [project1.projectId, project1.projectId]
        attachments_t1.skillId.sort() == [null, null].sort()

        attachments_t2.projectId == [project1.projectId, project1.projectId]
        attachments_t2.skillId.sort() == [null, null].sort()
        projectRes_t2.description == descriptionWithAttachments

        attachments_t1.uuid == attachments_t2.uuid
    }

    def "when project is edited and description's one attachment was replaced by another attachment"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [skill])

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, project1.projectId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, project1.projectId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()

        project1.description = descriptionWithAttachments
        skillsService.updateProject(project1)

        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def projectRes_t1 = skillsService.getProjectDescription(project1.projectId)

        String fileContent3 = 'Text in a file3'
        def uploadedAttachment3Res = skillsService.uploadAttachment('test3-pdf.pdf', fileContent3, project1.projectId)
        String attachment3Href = uploadedAttachment3Res.href
        String descriptionWithAttachments1 =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File3.pdf](${attachment3Href})").toString()
        project1.description = descriptionWithAttachments1
        skillsService.updateProject(project1, project1.projectId)

        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        def projectRes_t2 = skillsService.getProjectDescription(project1.projectId)

        List<Attachment> newAttachments = attachments_t2.findAll {
            !attachments_t1.find { Attachment inner -> inner.uuid == it.uuid}
        }
        then:
        projectRes_t1.description == descriptionWithAttachments
        attachments_t1.projectId == [project1.projectId, project1.projectId]
        attachments_t1.skillId.sort() == [null, null].sort()

        attachments_t2.projectId == [project1.projectId, project1.projectId, project1.projectId]
        attachments_t2.skillId.sort() == [null, null, null].sort()
        projectRes_t2.description == descriptionWithAttachments1

        newAttachments.projectId == [project1.projectId]
        projectRes_t2.description.contains(newAttachments[0].uuid)
    }

    def "when a subject is edited with the same description no new attachments are created"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [skill])

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, project1.projectId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, project1.projectId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()

        p1subj1.description = descriptionWithAttachments
        skillsService.updateSubject(p1subj1)

        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def res_t1 = skillsService.getSubject(p1subj1)

        skillsService.updateSubject(p1subj1, p1subj1.subjectId)

        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        def res_t2 = skillsService.getSubject(p1subj1)

        then:
        res_t1.description == descriptionWithAttachments
        attachments_t1.projectId == [project1.projectId, project1.projectId]
        attachments_t1.skillId.sort() == [p1subj1.subjectId, p1subj1.subjectId].sort()
        attachments_t1.quizId.sort() == [null, null].sort()

        attachments_t2.projectId == [project1.projectId, project1.projectId]
        attachments_t2.skillId.sort() == [p1subj1.subjectId, p1subj1.subjectId].sort()
        attachments_t2.quizId.sort() == [null, null].sort()
        res_t2.description == descriptionWithAttachments

        attachments_t1.uuid == attachments_t2.uuid
    }

    def "when a subject is edited and description's one attachment was replaced by another attachment"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [skill])

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, project1.projectId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, project1.projectId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()
        p1subj1.description = descriptionWithAttachments
        skillsService.updateSubject(p1subj1)

        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def res_t1 = skillsService.getSubject(p1subj1)

        String fileContent3 = 'Text in a file3'
        def uploadedAttachment3Res = skillsService.uploadAttachment('test3-pdf.pdf', fileContent3, project1.projectId)
        String attachment3Href = uploadedAttachment3Res.href
        String descriptionWithAttachments1 =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File3.pdf](${attachment3Href})").toString()
        p1subj1.description = descriptionWithAttachments1
        skillsService.updateSubject(p1subj1, p1subj1.subjectId)

        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        def res_t2 = skillsService.getSubject(p1subj1)

        List<Attachment> newAttachments = attachments_t2.findAll {
            !attachments_t1.find { Attachment inner -> inner.uuid == it.uuid}
        }
        then:
        res_t1.description == descriptionWithAttachments
        attachments_t1.projectId == [project1.projectId, project1.projectId]
        attachments_t1.skillId.sort() == [p1subj1.subjectId, p1subj1.subjectId].sort()
        attachments_t1.quizId.sort() == [null, null].sort()

        attachments_t2.projectId == [project1.projectId, project1.projectId, project1.projectId]
        attachments_t2.skillId.sort() == [p1subj1.subjectId, p1subj1.subjectId, p1subj1.subjectId].sort()
        attachments_t2.quizId.sort() == [null, null, null].sort()
        res_t2.description == descriptionWithAttachments1

        newAttachments.projectId == [project1.projectId]
        newAttachments.skillId == [p1subj1.subjectId]
        res_t2.description.contains(newAttachments[0].uuid)
    }

    def "when a badge is edited with the same description no new attachments are created"() {
        def proj = createProject(1)
        skillsService.createProject(proj)

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, proj.projectId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, proj.projectId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()

        def badge = createBadge(1, 1)
        badge.description = descriptionWithAttachments
        skillsService.createBadge(badge)

        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def res_t1 = skillsService.getBadge(badge)

        skillsService.updateBadge(badge, badge.subjectId)

        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        def res_t2 = skillsService.getBadge(badge)

        then:
        res_t1.description == descriptionWithAttachments
        attachments_t1.projectId == [proj.projectId, proj.projectId]
        attachments_t1.skillId.sort() == [badge.badgeId, badge.badgeId].sort()
        attachments_t1.quizId.sort() == [null, null].sort()

        attachments_t2.projectId == [proj.projectId, proj.projectId]
        attachments_t2.skillId.sort() == [badge.badgeId, badge.badgeId].sort()
        attachments_t2.quizId.sort() == [null, null].sort()
        res_t2.description == descriptionWithAttachments

        attachments_t1.uuid == attachments_t2.uuid
    }

    def "when a badge is edited and description's one attachment was replaced by another attachment"() {
        def proj = createProject(1)
        skillsService.createProject(proj)

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, proj.projectId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, proj.projectId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()

        def badge = createBadge(1, 1)
        badge.description = descriptionWithAttachments
        skillsService.createBadge(badge)

        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def res_t1 = skillsService.getBadge(badge)

        String fileContent3 = 'Text in a file3'
        def uploadedAttachment3Res = skillsService.uploadAttachment('test3-pdf.pdf', fileContent3, proj.projectId)
        String attachment3Href = uploadedAttachment3Res.href
        String descriptionWithAttachments1 =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File3.pdf](${attachment3Href})").toString()
        badge.description = descriptionWithAttachments1
        skillsService.updateBadge(badge, badge.subjectId)

        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        def res_t2 = skillsService.getBadge(badge)

        List<Attachment> newAttachments = attachments_t2.findAll {
            !attachments_t1.find { Attachment inner -> inner.uuid == it.uuid}
        }
        then:
        res_t1.description == descriptionWithAttachments
        attachments_t1.projectId == [proj.projectId, proj.projectId]
        attachments_t1.skillId.sort() == [badge.badgeId, badge.badgeId].sort()
        attachments_t1.quizId.sort() == [null, null].sort()

        attachments_t2.projectId == [proj.projectId, proj.projectId, proj.projectId]
        attachments_t2.skillId.sort() == [badge.badgeId, badge.badgeId, badge.badgeId].sort()
        attachments_t2.quizId.sort() == [null, null, null].sort()
        res_t2.description == descriptionWithAttachments1

        newAttachments.skillId == [badge.badgeId]
        res_t2.description.contains(newAttachments[0].uuid)
    }

    def "when a skill is edited with the same description no new attachments are created"() {
        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, proj.projectId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, proj.projectId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()


        skill.description = descriptionWithAttachments
        skillsService.createSkill(skill)
        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def res_t1 = skillsService.getSkill([skillId: skill.skillId, projectId: proj.projectId, subjectId: subj.subjectId])

        skillsService.updateSkill(skill, skill.skillId)

        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        def res_t2 = skillsService.getSkill([skillId: skill.skillId, projectId: proj.projectId, subjectId: subj.subjectId])

        then:
        res_t1.description == descriptionWithAttachments
        attachments_t1.projectId == [proj.projectId, proj.projectId]
        attachments_t1.skillId.sort() == [skill.skillId, skill.skillId].sort()
        attachments_t1.quizId.sort() == [null, null].sort()

        attachments_t2.projectId == [proj.projectId, proj.projectId]
        attachments_t2.skillId.sort() == [skill.skillId, skill.skillId].sort()
        attachments_t2.quizId.sort() == [null, null].sort()
        res_t2.description == descriptionWithAttachments

        attachments_t1.uuid == attachments_t2.uuid
    }

    def "when a skill is edited and description's one attachment was replaced by another attachment"() {
        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, proj.projectId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, proj.projectId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()

        skill.description = descriptionWithAttachments
        skillsService.updateSkill(skill, skill.skillId)
        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def res_t1 = skillsService.getSkill([skillId: skill.skillId, projectId: proj.projectId, subjectId: subj.subjectId])

        String fileContent3 = 'Text in a file3'
        def uploadedAttachment3Res = skillsService.uploadAttachment('test3-pdf.pdf', fileContent3, proj.projectId)
        String attachment3Href = uploadedAttachment3Res.href
        String descriptionWithAttachments1 =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File3.pdf](${attachment3Href})").toString()
        skill.description = descriptionWithAttachments1
        skillsService.updateSkill(skill, skill.skillId)

        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        def res_t2 = skillsService.getSkill([skillId: skill.skillId, projectId: proj.projectId, subjectId: subj.subjectId])

        List<Attachment> newAttachments = attachments_t2.findAll {
            !attachments_t1.find { Attachment inner -> inner.uuid == it.uuid}
        }
        then:
        res_t1.description == descriptionWithAttachments
        attachments_t1.projectId == [proj.projectId, proj.projectId]
        attachments_t1.skillId.sort() == [skill.skillId, skill.skillId].sort()
        attachments_t1.quizId.sort() == [null, null].sort()

        attachments_t2.projectId == [proj.projectId, proj.projectId, proj.projectId]
        attachments_t2.skillId.sort() == [skill.skillId, skill.skillId, skill.skillId].sort()
        attachments_t2.quizId.sort() == [null, null, null].sort()
        res_t2.description == descriptionWithAttachments1

        newAttachments.skillId == [skill.skillId]
        res_t2.description.contains(newAttachments[0].uuid)
    }

    def "approval request justification attachments properly get project id and skill id assigned"() {
        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skill.selfReportingType = SkillDef.SelfReportingType.Approval.toString()
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, proj.projectId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test1-pdf.pdf', fileContent2, proj.projectId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()

        when:
        skillsService.addSkill([skillId: skill.skillId, projectId: proj.projectId, subjectId: subj.subjectId], "user1", new Date(), descriptionWithAttachments)

        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()

        then:
        attachments_t1.projectId == [proj.projectId, proj.projectId]
        attachments_t1.skillId.sort() == [skill.skillId, skill.skillId].sort()
        attachments_t1.quizId.sort() == [null, null].sort()
    }

    def "quiz updated and description with attachments stayed the same - no new attachments are created"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, null, null, quiz.quizId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test2-pdf.pdf', fileContent2, null, null, quiz.quizId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()


        quiz.description = descriptionWithAttachments
        skillsService.createQuizDef(quiz, quiz.quizId)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))

        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def quizInfo_t1 = skillsService.getQuizDef(quiz.quizId)
        skillsService.createQuizDef(quiz, quiz.quizId)

        def quizInfo_t2 = skillsService.getQuizDef(quiz.quizId)
        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        then:
        quizInfo_t1.description == descriptionWithAttachments
        quizInfo_t2.description == descriptionWithAttachments

        attachments_t2.uuid == attachments_t1.uuid

        attachments_t1.quizId == [quiz.quizId, quiz.quizId]
        attachments_t1.skillId == [null, null]
        attachments_t1.projectId == [null, null]
        attachments_t1.filename.sort() == ['test1-pdf.pdf', 'test2-pdf.pdf'].sort()

        attachments_t2.quizId == [quiz.quizId, quiz.quizId]
        attachments_t2.skillId == [null, null]
        attachments_t2.projectId == [null, null]
        attachments_t2.filename.sort() == ['test1-pdf.pdf', 'test2-pdf.pdf'].sort()
    }

    def "quiz updated and description's one attachment was replaced by another attachment"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'

        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, null, null, quiz.quizId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test2-pdf.pdf', fileContent2, null, null, quiz.quizId)

        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href


        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()


        quiz.description = descriptionWithAttachments
        skillsService.createQuizDef(quiz, quiz.quizId)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))

        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def quizInfo_t1 = skillsService.getQuizDef(quiz.quizId)

        String fileContent3 = 'Text in a file3'
        def uploadedAttachment3Res = skillsService.uploadAttachment('test3-pdf.pdf', fileContent3, null, null, quiz.quizId)
        String attachment3Href = uploadedAttachment3Res.href
        String descriptionWithAttachments1 =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File3.pdf](${attachment3Href})").toString()
        quiz.description = descriptionWithAttachments1
        skillsService.createQuizDef(quiz, quiz.quizId)

        def quizInfo_t2 = skillsService.getQuizDef(quiz.quizId)
        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        List<Attachment> newAttachments = attachments_t2.findAll {
            !attachments_t1.find { Attachment inner -> inner.uuid == it.uuid}
        }
        then:
        quizInfo_t1.description == descriptionWithAttachments
        quizInfo_t2.description == descriptionWithAttachments1

        attachments_t1.quizId == [quiz.quizId, quiz.quizId]
        attachments_t1.skillId == [null, null]
        attachments_t1.projectId == [null, null]
        attachments_t1.filename.sort() == ['test1-pdf.pdf', 'test2-pdf.pdf'].sort()

        attachments_t2.quizId == [quiz.quizId, quiz.quizId, quiz.quizId]
        attachments_t2.skillId == [null, null, null]
        attachments_t2.projectId == [null, null, null]
        attachments_t2.filename.sort() == ['test1-pdf.pdf', 'test2-pdf.pdf', 'test3-pdf.pdf'].sort()

        newAttachments.quizId == [quiz.quizId]
        quizInfo_t2.description.contains(newAttachments[0].uuid)
    }

    def "quiz question updated and the description with attachments stayed the same - no new attachments are created"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, null, null, quiz.quizId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test2-pdf.pdf', fileContent2, null, null, quiz.quizId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String questionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()

        def qDef = QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice)
        qDef.question = questionWithAttachments
        def question = skillsService.createQuizQuestionDef(qDef).body

        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def quizInfo_t1 = skillsService.getQuizQuestionDef(quiz.quizId, question.id)

        question.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(question)

        def quizInfo_t2 = skillsService.getQuizQuestionDef(quiz.quizId, question.id)
        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        then:
        quizInfo_t1.question == questionWithAttachments
        quizInfo_t2.question == questionWithAttachments

        attachments_t2.uuid == attachments_t1.uuid

        attachments_t1.quizId == [quiz.quizId, quiz.quizId]
        attachments_t1.skillId == [null, null]
        attachments_t1.projectId == [null, null]
        attachments_t1.filename.sort() == ['test1-pdf.pdf', 'test2-pdf.pdf'].sort()

        attachments_t2.quizId == [quiz.quizId, quiz.quizId]
        attachments_t2.skillId == [null, null]
        attachments_t2.projectId == [null, null]
        attachments_t2.filename.sort() == ['test1-pdf.pdf', 'test2-pdf.pdf'].sort()
    }

    def "quiz question updated and description's one attachment was replaced by another attachment"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, null, null, quiz.quizId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test2-pdf.pdf', fileContent2, null, null, quiz.quizId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String questionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()

        def qDef = QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice)
        qDef.question = questionWithAttachments
        def question = skillsService.createQuizQuestionDef(qDef).body

        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def quizInfo_t1 = skillsService.getQuizQuestionDef(quiz.quizId, question.id)

        String fileContent3 = 'Text in a file3'
        def uploadedAttachment3Res = skillsService.uploadAttachment('test3-pdf.pdf', fileContent3, null, null, quiz.quizId)
        String attachment3Href = uploadedAttachment3Res.href
        String questionWithAttachments1 =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File3.pdf](${attachment3Href})").toString()
        question.question = questionWithAttachments1
        question.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(question)

        def quizInfo_t2 = skillsService.getQuizQuestionDef(quiz.quizId, question.id)
        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()

        List<Attachment> newAttachments = attachments_t2.findAll {
            !attachments_t1.find { Attachment inner -> inner.uuid == it.uuid}
        }
        then:
        quizInfo_t1.question == questionWithAttachments
        quizInfo_t2.question == questionWithAttachments1

        attachments_t1.quizId == [quiz.quizId, quiz.quizId]
        attachments_t1.skillId == [null, null]
        attachments_t1.projectId == [null, null]
        attachments_t1.filename.sort() == ['test1-pdf.pdf', 'test2-pdf.pdf'].sort()

        attachments_t2.quizId == [quiz.quizId, quiz.quizId, quiz.quizId]
        attachments_t2.skillId == [null, null, null]
        attachments_t2.projectId == [null, null, null]
        attachments_t2.filename.sort() == ['test1-pdf.pdf', 'test2-pdf.pdf', 'test3-pdf.pdf'].sort()

        newAttachments.quizId == [quiz.quizId]
        quizInfo_t2.question.contains(newAttachments[0].uuid)
    }

}

