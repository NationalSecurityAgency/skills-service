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
package skills.intTests.copyProject

import org.springframework.http.HttpStatus
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.storage.model.Attachment
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class CopyMarkdownWithAttachmentsSpecs extends CopyIntSpec {

    def "new project creation does not allow markdown with attachments"() {
        def p1 = createProject(1)
        p1.description = "Here is a [Link](/api/download/8ab81f77-3484-4f5a-ae58-ae4e7143b449)"

        when:
        skillsService.createProject(p1)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Attachments in the description are not allowed when creating a new project")
    }

    def "paste markdown with attachment to another project: by editing a project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        skillsService.createProjectAndSubjectAndSkills(p2, null, null)

        when:
        p2.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateProject(p2, p2.projectId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProj = skillsService.getProjectDescription(p2.projectId)

        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        p2.description = copyProj.description
        skillsService.updateProject(p2, p2.projectId)
        skillsService.updateProject(p2, p2.projectId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        copyProj.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].projectId == p2.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to another project: by editing a project and changing projectId at the same time"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        skillsService.createProjectAndSubjectAndSkills(p2, null, null)

        when:
        p2.description = "Here is a [Link](${attachment1Href})".toString()
        String originalProjId = p2.projectId
        p2.projectId = "newProjectId"
        skillsService.updateProject(p2, originalProjId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProj = skillsService.getProjectDescription(p2.projectId)

        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        p2.description = copyProj.description
        skillsService.updateProject(p2, p2.projectId)
        skillsService.updateProject(p2, p2.projectId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        copyProj.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].projectId == p2.projectId
        !newAttachments[0].skillId
        !newAttachments[0].quizId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to the same project: by editing a project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        skillsService.createProjectAndSubjectAndSkills(p2, null, null)

        when:
        p1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateProject(p1, p1.projectId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def updatedProj = skillsService.getProjectDescription(p1.projectId)

        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        p1.description = updatedProj.description
        skillsService.updateProject(p1, p1.projectId)
        skillsService.updateProject(p1, p1.projectId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        updatedProj.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].projectId == p1.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to the same project: by editing a project and changing projectId at the same time"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        skillsService.createProjectAndSubjectAndSkills(p2, null, null)

        when:
        p1.description = "Here is a [Link](${attachment1Href})".toString()
        String origProjId = p1.projectId
        p1.projectId = "newProjId"
        skillsService.updateProject(p1, origProjId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def updatedProj = skillsService.getProjectDescription(p1.projectId)

        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        p1.description = updatedProj.description
        skillsService.updateProject(p1, p1.projectId)
        skillsService.updateProject(p1, p1.projectId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        updatedProj.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].projectId == p1.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "editing project's id that has markdown with attachments must not duplicate attachments"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        p1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateProject(p1)

        when:
        List<Attachment> attachmentsBefore = attachmentRepo.findAll()
        def projDescBefore = skillsService.getProjectDescription(p1.projectId)
        String origId = p1.projectId
        p1.projectId = "newId"
        skillsService.updateProject(p1, origId)
        def projDescAfter = skillsService.getProjectDescription(p1.projectId)
        List<Attachment> attachmentsAfter = attachmentRepo.findAll()

        then:
        projDescBefore.description == "Here is a [Link](${attachment1Href})"
        projDescAfter.description == "Here is a [Link](${attachment1Href})"

        attachmentsBefore.size() == 1
        attachment1Href.contains(attachmentsBefore[0].uuid)

        attachmentsAfter.size() == 1
        attachmentsAfter.uuid == attachmentsBefore.uuid
    }

    def "editing project description does not duplicate attachments"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        p1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateProject(p1)

        when:
        List<Attachment> attachmentsBefore = attachmentRepo.findAll()
        def projDescBefore = skillsService.getProjectDescription(p1.projectId)
        skillsService.updateProject(p1)
        skillsService.updateProject(p1)
        skillsService.updateProject(p1)
        def projDescAfter = skillsService.getProjectDescription(p1.projectId)
        List<Attachment> attachmentsAfter = attachmentRepo.findAll()

        then:
        projDescBefore.description == "Here is a [Link](${attachment1Href})"
        projDescAfter.description == "Here is a [Link](${attachment1Href})"

        attachmentsBefore.size() == 1
        attachment1Href.contains(attachmentsBefore[0].uuid)

        attachmentsAfter.size() == 1
        attachmentsAfter.uuid == attachmentsBefore.uuid
    }

    def "pasting a skill attachment into project description creates a copy"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachmentHref = attachFileAndReturnHref(p1.projectId)
        def skill = createSkill(1, 1, 1, 0, 1, 100)
        skill.description = "Here is a [Link](${attachmentHref})".toString()
        skillsService.createSkill(skill)

        when:
        p1.description = skill.description
        skillsService.updateProject(p1)

        def projectDescription = skillsService.getProjectDescription(p1.projectId)
        List<Attachment> attachments = attachmentRepo.findAll()

        then:
        attachments.size() == 2
        projectDescription.description != skill.description

        Attachment sourceAttachment = attachments.find { attachmentHref.contains(it.uuid) }
        sourceAttachment.projectId == p1.projectId
        sourceAttachment.skillId == skill.skillId
        !sourceAttachment.quizId

        Attachment copiedAttachment = attachments.find { projectDescription.description.contains(it.uuid) }
        copiedAttachment.uuid != sourceAttachment.uuid
        copiedAttachment.projectId == p1.projectId
        !projectDescription.description.contains(sourceAttachment.uuid)
        projectDescription.description.contains(copiedAttachment.uuid)
        !copiedAttachment.skillId
        !copiedAttachment.quizId
    }

    def "paste markdown with attachment to another project: to a new subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        skillsService.createProjectAndSubjectAndSkills(p2, null, null)

        when:
        def p2subj1 = createSubject(2, 1)
        p2subj1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSubject(p2subj1)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copySubj = skillsService.getSubject(p2subj1)

        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateSubject(copySubj)
        skillsService.updateSubject(copySubj)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        copySubj.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments[0].projectId == p2.projectId
        newAttachments[0].skillId == p2subj1.subjectId
        !newAttachments[0].quizId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "repeated attachment links copied to another project create only one destination attachment"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)
        def attachmentHref = attachFileAndReturnHref(p1.projectId)

        def p2 = createProject(2)
        skillsService.createProject(p2)
        String description = (1..3).collect { "[Link${it}](${attachmentHref})" }.join("\n")

        when:
        p2.description = description
        skillsService.updateProject(p2, p2.projectId)

        then:
        List<Attachment> attachments = attachmentRepo.findAll().toList()
        attachments.size() == 2

        Attachment originalAttachment = attachments.find { attachmentHref.contains(it.uuid) }
        originalAttachment.projectId == p1.projectId

        Attachment copiedAttachment = attachments.find { it.uuid != originalAttachment.uuid }
        copiedAttachment.projectId == p2.projectId
        !copiedAttachment.quizId
        !copiedAttachment.skillId

        def copiedProject = skillsService.getProjectDescription(p2.projectId)
        copiedProject.description == (1..3).collect {
            "[Link${it}](/api/download/${copiedAttachment.uuid})"
        }.join("\n")
    }

    def "paste markdown with attachment to another project: by editing a subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, null)

        when:
        p2subj1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateSubject(p2subj1)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copySubj = skillsService.getSubject(p2subj1)

        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateSubject(copySubj)
        skillsService.updateSubject(copySubj)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        copySubj.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments[0].projectId == p2.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to another project: by editing a subject - multiple links"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, null)

        when:
        p2subj1.description = "Here is a [Link](${attachment1Href})\n\nAnother a [Link](${attachment2Href})".toString()
        skillsService.updateSubject(p2subj1)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copySubj = skillsService.getSubject(p2subj1)

        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateSubject(copySubj)
        skillsService.updateSubject(copySubj)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 4
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 2
        newAttachments.each {
            assert copySubj.description.contains("[Link](/api/download/${it.uuid})".toString())
            assert it.projectId == p2.projectId
        }

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to another project: by editing a subject and changing subjectId at the same time"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, null)

        when:
        String origSubjId = p2subj1.subjectId
        p2subj1.subjectId = "newSubjectId"
        p2subj1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateSubject(p2subj1, origSubjId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copySubj = skillsService.getSubject(p2subj1)

        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateSubject(copySubj)
        skillsService.updateSubject(copySubj)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        copySubj.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments[0].projectId == p2.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to the same project: by editing a subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        when:
        p1subj1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateSubject(p1subj1)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copySubj = skillsService.getSubject(p1subj1)

        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateSubject(copySubj)
        skillsService.updateSubject(copySubj)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        copySubj.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments[0].projectId == p1.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "editing subject's id that has markdown with attachments must not duplicated attachments"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)

        def p1subj1 = createSubject(1, 1)
        p1subj1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSubject(p1subj1)

        when:
        def copySubj = skillsService.getSubject(p1subj1)
        List<Attachment> attachments = attachmentRepo.findAll()

        String origSubjId = p1subj1.subjectId
        p1subj1.subjectId = "newId"
        skillsService.updateSubject(p1subj1, origSubjId)

        def copySubjAfter = skillsService.getSubject(p1subj1)
        List<Attachment> attachmentsAfter = attachmentRepo.findAll()

        then:
        copySubj.description == "Here is a [Link](${attachment1Href})"
        copySubjAfter.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 1
        attachment1Href.contains(attachments[0].uuid)
        attachmentsAfter.uuid.sort() == attachments.uuid.sort()
    }

    def "editing subject's description does not duplicate attachments"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)

        def p1subj1 = createSubject(1, 1)
        p1subj1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSubject(p1subj1)

        when:
        def copySubj = skillsService.getSubject(p1subj1)
        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateSubject(p1subj1)
        skillsService.updateSubject(p1subj1)
        skillsService.updateSubject(p1subj1)

        def copySubjAfter = skillsService.getSubject(p1subj1)
        List<Attachment> attachmentsAfter = attachmentRepo.findAll()

        then:
        copySubj.description == "Here is a [Link](${attachment1Href})"
        copySubjAfter.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 1
        attachment1Href.contains(attachments[0].uuid)
        attachmentsAfter.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to another project: to a new skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, null)
        def p2Skills = createSkills(2, 2, 1, 100)
        p2Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p2Skills[1].description = "Here is a [Link](${attachment2Href})".toString()

        when:
        skillsService.createSkills(p2Skills)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: p2Skills[0].skillId])
        def copyProjSkill2 = skillsService.getSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: p2Skills[1].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill2, copyProjSkill2.skillId)
        skillsService.updateSkill(copyProjSkill2, copyProjSkill2.skillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 4
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        List<String> copiedDescriptions = newAttachments.collect( {"Here is a [Link](/api/download/${it.uuid})".toString() })
        copiedDescriptions.contains(copyProjSkill1.description)
        copiedDescriptions.contains(copyProjSkill2.description)

        newAttachments.each {
            assert it.projectId == p2.projectId
            assert it.skillId == p2Skills[0].skillId || it.skillId == p2Skills[1].skillId
            assert !it.quizId
        }

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to another project: by editing a skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(2, 2, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Skills)

        when:
        p2Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p2Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.updateSkill(p2Skills[0], p2Skills[0].skillId)
        skillsService.updateSkill(p2Skills[1], p2Skills[1].skillId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def copyProjSkill2 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill2, copyProjSkill2.skillId)
        skillsService.updateSkill(copyProjSkill2, copyProjSkill2.skillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 4
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }
        newAttachments.size() == 2

        List<String> copiedDescriptions = newAttachments.collect( {"Here is a [Link](/api/download/${it.uuid})".toString() })
        copiedDescriptions.contains(copyProjSkill1.description)
        copiedDescriptions.contains(copyProjSkill2.description)

        newAttachments.each {
            assert it.projectId == p2.projectId
            assert it.skillId == copyProjSkill1.skillId || it.skillId == copyProjSkill2.skillId
            assert !it.quizId
        }
        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to another project: by editing a skill and changing skillId at the same time"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Skills = createSkills(2, 2, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Skills)

        when:
        String originalSkillId = p2Skills[0].skillId
        p2Skills[0].skillId = "newId"
        p2Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateSkill(p2Skills[0], originalSkillId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: p2Skills[0].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        def copyProjSkill1SkillId = copyProjSkill1.skillId
        copyProjSkill1.skillId = "anotherSkillId"
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1SkillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        newAttachments.size() == 1
        copyProjSkill1.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments[0].projectId == p2.projectId
        newAttachments[0].skillId == p2Skills[0].skillId
        !newAttachments[0].quizId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to the same project: to a new skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills[0..1])


        when:
        p1Skills[2].description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSkill(p1Skills[2])

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[2].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        newAttachments.size() == 1
        copyProjSkill1.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments.each {
            assert it.projectId == p1.projectId
        }

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to the same project: by editing a skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        when:
        p1Skills[2].description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateSkill(p1Skills[2], p1Skills[2].skillId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[2].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }
        newAttachments.size() == 1
        copyProjSkill1.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments.each {
            assert it.projectId == p1.projectId
        }
        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to the same project: by editing a skill and changing skillId at the same time"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        when:
        String origSkillId = p1Skills[2].skillId
        p1Skills[2].skillId = "newSkillId"
        p1Skills[2].description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateSkill(p1Skills[2], origSkillId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[2].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }
        newAttachments.size() == 1
        copyProjSkill1.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments.each {
            assert it.projectId == p1.projectId
        }
        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "editing skill's id that has markdown with attachment must not accidentally duplicate attachments"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)
        when:
        List<Attachment> attachmentsBefore = attachmentRepo.findAll()
        def skillBefore = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def skill2Before = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        String originalSkillId = p1Skills[0].skillId
        p1Skills[0].skillId = "newId"
        skillsService.updateSkill(p1Skills[0], originalSkillId)

        List<Attachment> attachmentsAfter = attachmentRepo.findAll()
        def skillAfter = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def skill2After = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        then:
        skillBefore.description == "Here is a [Link](${attachment1Href})"
        skill2Before.description == "Here is a [Link](${attachment2Href})"

        skillAfter.description == "Here is a [Link](${attachment1Href})"
        skill2After.description == "Here is a [Link](${attachment2Href})"

        attachmentsBefore.size() == 2
        attachmentsBefore.find { attachment1Href.contains(it.uuid) }.skillId == skillBefore.skillId
        attachmentsBefore.find { attachment2Href.contains(it.uuid) }.skillId == skill2Before.skillId

        attachmentsAfter.size() == 2
        attachmentsAfter.find { attachment1Href.contains(it.uuid) }.skillId == skillAfter.skillId
        attachmentsAfter.find { attachment2Href.contains(it.uuid) }.skillId == skill2After.skillId

        attachmentsBefore.uuid.sort() == attachmentsAfter.uuid.sort()
    }

    def "editing skill's descdripton dose not duplicate attachments"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)
        when:
        List<Attachment> attachmentsBefore = attachmentRepo.findAll()
        def skillBefore = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def skill2Before = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        skillsService.updateSkill(p1Skills[0], p1Skills[0].skillId)
        skillsService.updateSkill(p1Skills[0], p1Skills[0].skillId)
        skillsService.updateSkill(p1Skills[0], p1Skills[0].skillId)
        skillsService.updateSkill(p1Skills[0], p1Skills[0].skillId)

        List<Attachment> attachmentsAfter = attachmentRepo.findAll()
        def skillAfter = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def skill2After = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        then:
        skillBefore.description == "Here is a [Link](${attachment1Href})"
        skill2Before.description == "Here is a [Link](${attachment2Href})"

        skillAfter.description == "Here is a [Link](${attachment1Href})"
        skill2After.description == "Here is a [Link](${attachment2Href})"

        attachmentsBefore.size() == 2
        attachmentsBefore.find { attachment1Href.contains(it.uuid) }.skillId == skillBefore.skillId
        attachmentsBefore.find { attachment2Href.contains(it.uuid) }.skillId == skill2Before.skillId

        attachmentsAfter.size() == 2
        attachmentsAfter.find { attachment1Href.contains(it.uuid) }.skillId == skillAfter.skillId
        attachmentsAfter.find { attachment2Href.contains(it.uuid) }.skillId == skill2After.skillId

        attachmentsBefore.uuid.sort() == attachmentsAfter.uuid.sort()
    }

    def "paste markdown with attachment to another project: to a new skills group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, null)
        def group = createSkillsGroup(2, 1, 11)

        when:
        group.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSkill(group)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: group.skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }
        newAttachments.size() == 1
        copyProjSkill1.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments.each {
            assert it.projectId == p2.projectId
        }

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to another project: by editing a skills group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def group = createSkillsGroup(2, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [group])

        when:
        group.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateSkill(group, group.skillId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: group.skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }
        newAttachments.size() == 1
        copyProjSkill1.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].projectId == p2.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to another project: by editing a skills group and changing groupId at the same time"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def group = createSkillsGroup(2, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [group])

        when:
        String originalSkillId = group.skillId
        group.skillId = "newId"
        group.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateSkill(group, originalSkillId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: group.skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        newAttachments.size() == 1
        copyProjSkill1.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].projectId == p2.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to the same project: to a new skills group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills[0..1])

        when:
        def group = createSkillsGroup(1, 1, 11)
        group.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSkill(group)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: group.skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        newAttachments.size() == 1
        copyProjSkill1.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].projectId == p1.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to the same project: by editing a skills group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()

        def group = createSkillsGroup(1, 1, 11)
        skillsService.createSkills([p1Skills, group].flatten())

        when:
        group.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateSkill(group, group.skillId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: group.skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }
        newAttachments.size() == 1
        copyProjSkill1.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].projectId == p1.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to the same project: by editing a skills group and changing group at the same time"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        def group = createSkillsGroup(1, 1, 11)
        skillsService.createSkills([p1Skills, group].flatten())

        when:
        String origSkillId = group.skillId
        group.skillId = "newSkillId"
        group.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateSkill(group, origSkillId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: group.skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }
        newAttachments.size() == 1
        copyProjSkill1.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].projectId == p1.projectId
        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "editing existing group's id with attachments must not duplicate attachments"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)

        def group = createSkillsGroup(1, 1, 11)
        group.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSkills([group].flatten())

        when:
        def groupRes = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: group.skillId])
        List<Attachment> attachments = attachmentRepo.findAll()

        String origGroupId = group.skillId
        group.skillId = "newId"
        skillsService.updateSkill(group, origGroupId)

        def groupResAfter = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: group.skillId])
        List<Attachment> attachmentsAfter = attachmentRepo.findAll()

        then:
        groupRes.description == "Here is a [Link](${attachment1Href})"
        groupResAfter.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 1
        attachments[0].projectId == p1.projectId
        attachments[0].skillId == origGroupId
        !attachments[0].quizId

        attachmentsAfter.uuid.sort() == attachments.uuid.sort()
        attachmentsAfter[0].skillId == group.skillId
    }

    def "editing existing group's description does not duplicate attachments"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)

        def group = createSkillsGroup(1, 1, 11)
        group.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSkills([group].flatten())

        when:
        def groupRes = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: group.skillId])
        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateSkill(group, group.skillId)
        skillsService.updateSkill(group, group.skillId)
        skillsService.updateSkill(group, group.skillId)

        def groupResAfter = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: group.skillId])
        List<Attachment> attachmentsAfter = attachmentRepo.findAll()

        then:
        groupRes.description == "Here is a [Link](${attachment1Href})"
        groupResAfter.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 1
        attachments[0].projectId == p1.projectId
        attachments[0].skillId == group.skillId
        !attachments[0].quizId

        attachmentsAfter.uuid.sort() == attachments.uuid.sort()
        attachmentsAfter[0].skillId == group.skillId
    }

    def "paste markdown with attachment to another project: to a new badge"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        skillsService.createProjectAndSubjectAndSkills(p2, null, null)

        when:
        def badge = createBadge(2, 1)
        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createBadge(badge)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyBadge = skillsService.getBadge(badge)

        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateBadge(copyBadge)
        skillsService.updateBadge(copyBadge)
        def copyBadgeId = copyBadge.badgeId
        copyBadge.badgeId = "anotherBadgeId"
        skillsService.updateBadge(copyBadge, copyBadgeId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        copyBadge.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments[0].projectId == p2.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to another project: by editing a badge"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, null)

        def badge = createBadge(2, 1)
        skillsService.createBadge(badge)

        when:
        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateBadge(badge)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyBadge = skillsService.getBadge(badge)

        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateBadge(copyBadge)
        skillsService.updateBadge(copyBadge)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        copyBadge.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments[0].projectId == p2.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to another project: by editing a badge - multiple links"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, null)

        def badge = createBadge(2, 1)
        skillsService.createBadge(badge)

        when:
        badge.description = "Here is a [Link](${attachment1Href})\n\nAnother a [Link](${attachment2Href})".toString()
        skillsService.updateBadge(badge)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyBadge = skillsService.getBadge(badge)

        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateBadge(copyBadge)
        skillsService.updateBadge(copyBadge)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 4
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 2
        newAttachments.each {
            assert copyBadge.description.contains("[Link](/api/download/${it.uuid})".toString())
            assert it.projectId == p2.projectId
        }

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to another project: by editing a badge and changing badgeId at the same time"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, null)

        def badge = createBadge(2, 1)
        skillsService.createBadge(badge)

        when:
        String origBadgeId = badge.badgeId
        badge.subjectId = "newBadgeId"
        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateBadge(badge, origBadgeId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyBadge = skillsService.getBadge(badge)

        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateBadge(copyBadge)
        skillsService.updateBadge(copyBadge)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        copyBadge.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments[0].projectId == p2.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to the same project: by editing a badge"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def badge = createBadge(1, 1)
        skillsService.createBadge(badge)

        when:
        badge.description = "Here is a [Link](${attachment1Href})".toString()
        String origBadgeId = badge.badgeId
        badge.badgeId = "newBadgeId"
        skillsService.updateBadge(badge, origBadgeId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyBadge = skillsService.getBadge(badge)

        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateBadge(copyBadge)
        skillsService.updateBadge(copyBadge)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        copyBadge.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments[0].projectId == p1.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to the same project: by editing a badge and changing badgeId at the same time"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def badge = createBadge(1, 1)
        skillsService.createBadge(badge)

        when:
        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateBadge(badge)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyBadge = skillsService.getBadge(badge)

        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateBadge(copyBadge)
        skillsService.updateBadge(copyBadge)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        copyBadge.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments[0].projectId == p1.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment to the same project: by creating a new badge"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        when:
        def badge = createBadge(1, 1)
        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateBadge(badge)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyBadge = skillsService.getBadge(badge)

        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateBadge(copyBadge)
        skillsService.updateBadge(copyBadge)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        copyBadge.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        newAttachments[0].projectId == p1.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "editing existing badge's id with attachments must not duplicate that attachment"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)

        def badge = createBadge(1, 1)
        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createBadge(badge)

        when:
        def badgeRes = skillsService.getBadge(badge)
        List<Attachment> attachments = attachmentRepo.findAll()

        String origBadgeId = badge.badgeId
        badge.badgeId = "newId"
        skillsService.updateBadge(badge, origBadgeId)

        def badgeResAfter = skillsService.getBadge(badge)
        List<Attachment> attachmentsAfter = attachmentRepo.findAll()

        then:
        badgeRes.description == "Here is a [Link](${attachment1Href})"
        badgeResAfter.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 1
        attachments[0].projectId == p1.projectId
        attachments[0].skillId == origBadgeId
        !attachments[0].quizId

        attachmentsAfter.uuid.sort() == attachments.uuid.sort()
        attachmentsAfter[0].skillId == badge.badgeId
        attachmentsAfter[0].projectId == p1.projectId
        !attachmentsAfter[0].quizId
    }

    def "editing existing badge's description does not duplicate that attachment"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)

        def badge = createBadge(1, 1)
        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createBadge(badge)

        when:
        def badgeRes = skillsService.getBadge(badge)
        List<Attachment> attachments = attachmentRepo.findAll()

        skillsService.updateBadge(badge, badge.badgeId)
        skillsService.updateBadge(badge, badge.badgeId)
        skillsService.updateBadge(badge, badge.badgeId)
        skillsService.updateBadge(badge, badge.badgeId)

        def badgeResAfter = skillsService.getBadge(badge)
        List<Attachment> attachmentsAfter = attachmentRepo.findAll()

        then:
        badgeRes.description == "Here is a [Link](${attachment1Href})"
        badgeResAfter.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 1
        attachments[0].projectId == p1.projectId
        attachments[0].skillId == badge.badgeId
        !attachments[0].quizId

        attachmentsAfter.uuid.sort() == attachments.uuid.sort()
        attachmentsAfter[0].skillId == badge.badgeId
        attachmentsAfter[0].projectId == p1.projectId
        !attachmentsAfter[0].quizId
    }

    def "paste markdown with attachment to another project: to approval justification"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skills = createSkills(2, 2, 1, 100)
        p2skills.each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2skills)

        when:
        String justification =  "Here is a [Link](${attachment1Href})".toString()
        skillsService.addSkill(p2skills[0], skillsService.userName, new Date(), justification)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def skillSummary = skillsService.getSingleSkillSummary(skillsService.userName, p2.projectId, p2skills[0].skillId)

        List<Attachment> attachments = attachmentRepo.findAll()

        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        skillSummary.approvalHistory.description == ["Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()]

        newAttachments[0].projectId == p2.projectId
    }

    def "paste markdown with attachment from project description to skill description"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        p1.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateProject(p1)

        when:
        def skill = createSkill(1, 1, 1)
        skill.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSkill(skill)

        def projDesc = skillsService.getProjectDescription(p1.projectId)
        def skillAfter = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill.skillId])

        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        skillsService.updateSkill(skillAfter)
        skillsService.updateSkill(skillAfter)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        projDesc.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {!attachment1Href.contains(it.uuid) }

        assert newAttachments.size() == 1
        skillAfter.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].projectId == p1.projectId
        newAttachments[0].skillId == skillAfter.skillId
        !newAttachments[0].quizId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment from quiz: quiz -> skill"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz.quizId)
        quiz.description =  "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz, quiz.quizId)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, null)
        def p2Skills = createSkills(2, 2, 1, 100)

        when:
        p2Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSkills(p2Skills)

        def quiz1Res = skillsService.getQuizDef(quiz.quizId)

        def copyProjSkill1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: p2Skills[0].skillId])
        def copyProjSkill2 = skillsService.getSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: p2Skills[1].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill2, copyProjSkill2.skillId)
        skillsService.updateSkill(copyProjSkill2, copyProjSkill2.skillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid)
        }
        newAttachments.size() == 1
        newAttachments[0].projectId ==  p2.projectId
        newAttachments[0].skillId ==  p2Skills[0].skillId
        !newAttachments[0].quizId

        copyProjSkill1.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment from quiz: quiz -> project"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz.quizId)
        quiz.description =  "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizDef(quiz, quiz.quizId)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, null)

        when:
        p2.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateProject(p2)

        def quiz1Res = skillsService.getQuizDef(quiz.quizId)

        def projRes = skillsService.getProjectDescription(p2.projectId)

        p2.description = projRes.description
        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateProject(p2)
        skillsService.updateProject(p2)
        skillsService.updateProject(p2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid)
        }
        newAttachments.size() == 1
        newAttachments[0].projectId ==  p2.projectId
        !newAttachments[0].skillId
        !newAttachments[0].quizId

        projRes.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment from quiz: question -> skill"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.question = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizQuestionDef(question1)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, null)
        def p2Skills = createSkills(2, 2, 1, 100)

        when:
        p2Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSkills(p2Skills)

        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)

        def copyProjSkill1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: p2Skills[0].skillId])
        def copyProjSkill2 = skillsService.getSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: p2Skills[1].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill2, copyProjSkill2.skillId)
        skillsService.updateSkill(copyProjSkill2, copyProjSkill2.skillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.questions[0].question == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid)
        }
        newAttachments.size() == 1
        newAttachments[0].projectId ==  p2.projectId
        newAttachments[0].skillId ==  p2Skills[0].skillId
        !newAttachments[0].quizId

        copyProjSkill1.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment from quiz: question -> project"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz1.quizId)

        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        question1.question = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createQuizQuestionDef(question1)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, null)

        when:
        p2.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateProject(p2)

        def quiz1Res = skillsService.getQuizQuestionDefs(quiz1.quizId)

        def projRes = skillsService.getProjectDescription(p2.projectId)

        p2.description = projRes.description
        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateProject(p2)
        skillsService.updateProject(p2)
        skillsService.updateProject(p2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        quiz1Res.questions[0].question == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        originalAttachment1.quizId == quiz1.quizId
        !originalAttachment1.projectId
        !originalAttachment1.skillId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid)
        }
        newAttachments.size() == 1
        newAttachments[0].projectId ==  p2.projectId
        !newAttachments[0].skillId
        !newAttachments[0].quizId

        projRes.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment from Global Badge: gb -> skill"() {
        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        def attachment1Href = attachFileForGlobalBadgeAndReturnHref(badge.badgeId)

        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateGlobalBadge(badge)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, null)
        def p2Skills = createSkills(2, 2, 1, 100)

        when:
        p2Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.createSkills(p2Skills)

        def badgeRes = skillsService.getGlobalBadge(badge.badgeId)

        def copyProjSkill1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: p2Skills[0].skillId])
        def copyProjSkill2 = skillsService.getSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: p2Skills[1].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill1, copyProjSkill1.skillId)
        skillsService.updateSkill(copyProjSkill2, copyProjSkill2.skillId)
        skillsService.updateSkill(copyProjSkill2, copyProjSkill2.skillId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        badgeRes.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        !originalAttachment1.quizId
        !originalAttachment1.projectId
        originalAttachment1.skillId == badge.badgeId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid)
        }
        newAttachments.size() == 1
        newAttachments[0].projectId ==  p2.projectId
        newAttachments[0].skillId ==  p2Skills[0].skillId
        !newAttachments[0].quizId

        copyProjSkill1.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment from Global Badge: gb -> project"() {
        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        def attachment1Href = attachFileForGlobalBadgeAndReturnHref(badge.badgeId)

        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateGlobalBadge(badge)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, null)

        when:
        p2.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateProject(p2)

        def badgeRes = skillsService.getGlobalBadge(badge.badgeId)

        def projRes = skillsService.getProjectDescription(p2.projectId)

        p2.description = projRes.description
        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.updateProject(p2)
        skillsService.updateProject(p2)
        skillsService.updateProject(p2)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        badgeRes.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        !originalAttachment1.quizId
        !originalAttachment1.projectId
        originalAttachment1.skillId == badge.badgeId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid)
        }
        newAttachments.size() == 1
        newAttachments[0].projectId ==  p2.projectId
        !newAttachments[0].skillId
        !newAttachments[0].quizId

        projRes.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "deleting the project removes associated attachments"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        List<String> p1AttachmentsHrefs = (1..7).collect { attachFileAndReturnHref(p1.projectId)}
        p1.description = "Here is a [Link](${p1AttachmentsHrefs[0]})".toString()
        skillsService.updateProject(p1)

        def p1subj1 = createSubject(1, 1)
        p1subj1.description = "Here is a [Link](${p1AttachmentsHrefs[1]})".toString()
        skillsService.createSubject(p1subj1)

        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        p1Skills[0].description = "Here is a [Link](${p1AttachmentsHrefs[2]})".toString()
        p1Skills[1].description = "Here is a [Link](${p1AttachmentsHrefs[3]})".toString()
        skillsService.createSkills(p1Skills)

        def badge = createBadge(1, 1)
        badge.description = "Here is a [Link](${p1AttachmentsHrefs[4]})".toString()
        skillsService.createBadge(badge)

        def group = createSkillsGroup(1, 1, 11)
        group.description = "Here is a [Link](${p1AttachmentsHrefs[5]})".toString()
        skillsService.createSkill(group)

        String justification =  "Here is a [Link](${p1AttachmentsHrefs[6]})".toString()
        skillsService.addSkill(p1Skills[0], skillsService.userName, new Date(), justification)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        List<String> p2AttachmentsHrefs = (1..7).collect { attachFileAndReturnHref(p2.projectId)}
        p2.description = "Here is a [Link](${p2AttachmentsHrefs[0]})".toString()
        skillsService.updateProject(p2)

        def p2subj1 = createSubject(2, 1)
        p2subj1.description = "Here is a [Link](${p2AttachmentsHrefs[1]})".toString()
        skillsService.createSubject(p2subj1)

        def p2Skills = createSkills(3, 2, 1, 100)
        p2Skills.each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        p2Skills[0].description = "Here is a [Link](${p2AttachmentsHrefs[2]})".toString()
        p2Skills[1].description = "Here is a [Link](${p2AttachmentsHrefs[3]})".toString()
        skillsService.createSkills(p2Skills)

        def p2Badge = createBadge(2, 1)
        p2Badge.description = "Here is a [Link](${p2AttachmentsHrefs[4]})".toString()
        skillsService.createBadge(p2Badge)

        def p2Group = createSkillsGroup(2, 1, 11)
        p2Group.description = "Here is a [Link](${p2AttachmentsHrefs[5]})".toString()
        skillsService.createSkill(p2Group)

        skillsService.addSkill(p2Skills[0], skillsService.userName, new Date(), "Here is a [Link](${p2AttachmentsHrefs[6]})".toString())

        when:
        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.deleteProject(p1.projectId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        attachments.collect { "/api/download/${it.uuid}".toString() }.sort() == [p1AttachmentsHrefs, p2AttachmentsHrefs].flatten().sort()
        attachments1.collect { "/api/download/${it.uuid}".toString() }.sort() == p2AttachmentsHrefs.sort()
    }

    def "deleting a skill removes associated attachments"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        List<String> p1AttachmentsHrefs = (1..7).collect { attachFileAndReturnHref(p1.projectId)}
        p1.description = "Here is a [Link](${p1AttachmentsHrefs[0]})".toString()
        skillsService.updateProject(p1)

        def p1subj1 = createSubject(1, 1)
        p1subj1.description = "Here is a [Link](${p1AttachmentsHrefs[1]})".toString()
        skillsService.createSubject(p1subj1)

        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        p1Skills[0].description = "Here is a [Link](${p1AttachmentsHrefs[2]})".toString()
        p1Skills[1].description = "Here is a [Link](${p1AttachmentsHrefs[3]})".toString()
        skillsService.createSkills(p1Skills)

        def badge = createBadge(1, 1)
        badge.description = "Here is a [Link](${p1AttachmentsHrefs[4]})".toString()
        skillsService.createBadge(badge)

        def group = createSkillsGroup(1, 1, 11)
        group.description = "Here is a [Link](${p1AttachmentsHrefs[5]})".toString()
        skillsService.createSkill(group)

        String justification =  "Here is a [Link](${p1AttachmentsHrefs[6]})".toString()
        skillsService.addSkill(p1Skills[0], skillsService.userName, new Date(), justification)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        List<String> p2AttachmentsHrefs = (1..7).collect { attachFileAndReturnHref(p2.projectId)}
        p2.description = "Here is a [Link](${p2AttachmentsHrefs[0]})".toString()
        skillsService.updateProject(p2)

        def p2subj1 = createSubject(2, 1)
        p2subj1.description = "Here is a [Link](${p2AttachmentsHrefs[1]})".toString()
        skillsService.createSubject(p2subj1)

        def p2Skills = createSkills(3, 2, 1, 100)
        p2Skills.each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        p2Skills[0].description = "Here is a [Link](${p2AttachmentsHrefs[2]})".toString()
        p2Skills[1].description = "Here is a [Link](${p2AttachmentsHrefs[3]})".toString()
        skillsService.createSkills(p2Skills)

        def p2Badge = createBadge(2, 1)
        p2Badge.description = "Here is a [Link](${p2AttachmentsHrefs[4]})".toString()
        skillsService.createBadge(p2Badge)

        def p2Group = createSkillsGroup(2, 1, 11)
        p2Group.description = "Here is a [Link](${p2AttachmentsHrefs[5]})".toString()
        skillsService.createSkill(p2Group)

        skillsService.addSkill(p2Skills[0], skillsService.userName, new Date(), "Here is a [Link](${p2AttachmentsHrefs[6]})".toString())

        when:
        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.deleteSkill(p1Skills[0])
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        attachments.collect { "/api/download/${it.uuid}".toString() }.sort() == [p1AttachmentsHrefs, p2AttachmentsHrefs].flatten().sort()
        // skill and its submitted approval request is removed
        List<String> withoutRemovedAttachments = p1AttachmentsHrefs.findAll { it != p1AttachmentsHrefs[2] && it != p1AttachmentsHrefs[6]}
        attachments1.collect { "/api/download/${it.uuid}".toString() }.sort() == [withoutRemovedAttachments, p2AttachmentsHrefs].flatten().sort()
    }

    def "deleting a subject removes associated attachments"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        List<String> p1AttachmentsHrefs = (1..7).collect { attachFileAndReturnHref(p1.projectId)}
        p1.description = "Here is a [Link](${p1AttachmentsHrefs[0]})".toString()
        skillsService.updateProject(p1)

        def p1subj1 = createSubject(1, 1)
        p1subj1.description = "Here is a [Link](${p1AttachmentsHrefs[1]})".toString()
        skillsService.createSubject(p1subj1)

        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        p1Skills[0].description = "Here is a [Link](${p1AttachmentsHrefs[2]})".toString()
        p1Skills[1].description = "Here is a [Link](${p1AttachmentsHrefs[3]})".toString()
        skillsService.createSkills(p1Skills)

        def badge = createBadge(1, 1)
        badge.description = "Here is a [Link](${p1AttachmentsHrefs[4]})".toString()
        skillsService.createBadge(badge)

        def group = createSkillsGroup(1, 1, 11)
        group.description = "Here is a [Link](${p1AttachmentsHrefs[5]})".toString()
        skillsService.createSkill(group)

        String justification =  "Here is a [Link](${p1AttachmentsHrefs[6]})".toString()
        skillsService.addSkill(p1Skills[0], skillsService.userName, new Date(), justification)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        List<String> p2AttachmentsHrefs = (1..7).collect { attachFileAndReturnHref(p2.projectId)}
        p2.description = "Here is a [Link](${p2AttachmentsHrefs[0]})".toString()
        skillsService.updateProject(p2)

        def p2subj1 = createSubject(2, 1)
        p2subj1.description = "Here is a [Link](${p2AttachmentsHrefs[1]})".toString()
        skillsService.createSubject(p2subj1)

        def p2Skills = createSkills(3, 2, 1, 100)
        p2Skills.each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        p2Skills[0].description = "Here is a [Link](${p2AttachmentsHrefs[2]})".toString()
        p2Skills[1].description = "Here is a [Link](${p2AttachmentsHrefs[3]})".toString()
        skillsService.createSkills(p2Skills)

        def p2Badge = createBadge(2, 1)
        p2Badge.description = "Here is a [Link](${p2AttachmentsHrefs[4]})".toString()
        skillsService.createBadge(p2Badge)

        def p2Group = createSkillsGroup(2, 1, 11)
        p2Group.description = "Here is a [Link](${p2AttachmentsHrefs[5]})".toString()
        skillsService.createSkill(p2Group)

        skillsService.addSkill(p2Skills[0], skillsService.userName, new Date(), "Here is a [Link](${p2AttachmentsHrefs[6]})".toString())

        when:
        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.deleteSubject(p1Skills[0])
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        attachments.collect { "/api/download/${it.uuid}".toString() }.sort() == [p1AttachmentsHrefs, p2AttachmentsHrefs].flatten().sort()
        // only project and badge is kept
        attachments1.collect { "/api/download/${it.uuid}".toString() }.sort() == [p1AttachmentsHrefs[0], p1AttachmentsHrefs[4], p2AttachmentsHrefs].flatten().sort()
    }

    def "deleting a badge removes associated attachments"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        List<String> p1AttachmentsHrefs = (1..7).collect { attachFileAndReturnHref(p1.projectId)}
        p1.description = "Here is a [Link](${p1AttachmentsHrefs[0]})".toString()
        skillsService.updateProject(p1)

        def p1subj1 = createSubject(1, 1)
        p1subj1.description = "Here is a [Link](${p1AttachmentsHrefs[1]})".toString()
        skillsService.createSubject(p1subj1)

        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        p1Skills[0].description = "Here is a [Link](${p1AttachmentsHrefs[2]})".toString()
        p1Skills[1].description = "Here is a [Link](${p1AttachmentsHrefs[3]})".toString()
        skillsService.createSkills(p1Skills)

        def badge = createBadge(1, 1)
        badge.description = "Here is a [Link](${p1AttachmentsHrefs[4]})".toString()
        skillsService.createBadge(badge)

        def group = createSkillsGroup(1, 1, 11)
        group.description = "Here is a [Link](${p1AttachmentsHrefs[5]})".toString()
        skillsService.createSkill(group)

        String justification =  "Here is a [Link](${p1AttachmentsHrefs[6]})".toString()
        skillsService.addSkill(p1Skills[0], skillsService.userName, new Date(), justification)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        List<String> p2AttachmentsHrefs = (1..7).collect { attachFileAndReturnHref(p2.projectId)}
        p2.description = "Here is a [Link](${p2AttachmentsHrefs[0]})".toString()
        skillsService.updateProject(p2)

        def p2subj1 = createSubject(2, 1)
        p2subj1.description = "Here is a [Link](${p2AttachmentsHrefs[1]})".toString()
        skillsService.createSubject(p2subj1)

        def p2Skills = createSkills(3, 2, 1, 100)
        p2Skills.each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        p2Skills[0].description = "Here is a [Link](${p2AttachmentsHrefs[2]})".toString()
        p2Skills[1].description = "Here is a [Link](${p2AttachmentsHrefs[3]})".toString()
        skillsService.createSkills(p2Skills)

        def p2Badge = createBadge(2, 1)
        p2Badge.description = "Here is a [Link](${p2AttachmentsHrefs[4]})".toString()
        skillsService.createBadge(p2Badge)

        def p2Group = createSkillsGroup(2, 1, 11)
        p2Group.description = "Here is a [Link](${p2AttachmentsHrefs[5]})".toString()
        skillsService.createSkill(p2Group)

        skillsService.addSkill(p2Skills[0], skillsService.userName, new Date(), "Here is a [Link](${p2AttachmentsHrefs[6]})".toString())

        when:
        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.removeBadge(badge)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        attachments.collect { "/api/download/${it.uuid}".toString() }.sort() == [p1AttachmentsHrefs, p2AttachmentsHrefs].flatten().sort()
        // skill and its submitted approval request is removed
        List<String> withoutRemovedAttachments = p1AttachmentsHrefs.findAll { it != p1AttachmentsHrefs[4]}
        attachments1.collect { "/api/download/${it.uuid}".toString() }.sort() == [withoutRemovedAttachments, p2AttachmentsHrefs].flatten().sort()
    }

    def "deleting a group removes associated attachments"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        List<String> p1AttachmentsHrefs = (1..7).collect { attachFileAndReturnHref(p1.projectId)}
        p1.description = "Here is a [Link](${p1AttachmentsHrefs[0]})".toString()
        skillsService.updateProject(p1)

        def p1subj1 = createSubject(1, 1)
        p1subj1.description = "Here is a [Link](${p1AttachmentsHrefs[1]})".toString()
        skillsService.createSubject(p1subj1)

        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        p1Skills[0].description = "Here is a [Link](${p1AttachmentsHrefs[2]})".toString()
        p1Skills[1].description = "Here is a [Link](${p1AttachmentsHrefs[3]})".toString()

        def badge = createBadge(1, 1)
        badge.description = "Here is a [Link](${p1AttachmentsHrefs[4]})".toString()
        skillsService.createBadge(badge)

        def group = createSkillsGroup(1, 1, 11)
        group.description = "Here is a [Link](${p1AttachmentsHrefs[5]})".toString()
        skillsService.createSkill(group)
        skillsService.assignSkillToSkillsGroup(group.skillId, p1Skills[0])
        skillsService.assignSkillToSkillsGroup(group.skillId, p1Skills[1])

        String justification =  "Here is a [Link](${p1AttachmentsHrefs[6]})".toString()
        skillsService.addSkill(p1Skills[0], skillsService.userName, new Date(), justification)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        List<String> p2AttachmentsHrefs = (1..7).collect { attachFileAndReturnHref(p2.projectId)}
        p2.description = "Here is a [Link](${p2AttachmentsHrefs[0]})".toString()
        skillsService.updateProject(p2)

        def p2subj1 = createSubject(2, 1)
        p2subj1.description = "Here is a [Link](${p2AttachmentsHrefs[1]})".toString()
        skillsService.createSubject(p2subj1)

        def p2Skills = createSkills(3, 2, 1, 100)
        p2Skills.each { it.selfReportingType = SkillDef.SelfReportingType.Approval }
        p2Skills[0].description = "Here is a [Link](${p2AttachmentsHrefs[2]})".toString()
        p2Skills[1].description = "Here is a [Link](${p2AttachmentsHrefs[3]})".toString()
        skillsService.createSkills(p2Skills)

        def p2Badge = createBadge(2, 1)
        p2Badge.description = "Here is a [Link](${p2AttachmentsHrefs[4]})".toString()
        skillsService.createBadge(p2Badge)

        def p2Group = createSkillsGroup(2, 1, 11)
        p2Group.description = "Here is a [Link](${p2AttachmentsHrefs[5]})".toString()
        skillsService.createSkill(p2Group)

        skillsService.addSkill(p2Skills[0], skillsService.userName, new Date(), "Here is a [Link](${p2AttachmentsHrefs[6]})".toString())

        when:
        List<Attachment> attachments = attachmentRepo.findAll()
        skillsService.deleteSkill(group)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        attachments.collect { "/api/download/${it.uuid}".toString() }.sort() == [p1AttachmentsHrefs, p2AttachmentsHrefs].flatten().sort()

        // only project, subject and badge is left after group is removed
        attachments1.collect { "/api/download/${it.uuid}".toString() }.sort() == [ p1AttachmentsHrefs[0],  p1AttachmentsHrefs[1], p1AttachmentsHrefs[4], p2AttachmentsHrefs].flatten().sort()
    }
}
