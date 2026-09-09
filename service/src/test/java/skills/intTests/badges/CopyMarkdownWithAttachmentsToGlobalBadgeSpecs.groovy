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
package skills.intTests.badges

import org.springframework.http.HttpStatus
import skills.intTests.copyProject.CopyIntSpec
import skills.intTests.utils.SkillsClientException
import skills.storage.model.Attachment
import skills.storage.model.SkillDef
import spock.lang.IgnoreRest

import static skills.intTests.utils.SkillsFactory.*

class CopyMarkdownWithAttachmentsToGlobalBadgeSpecs extends CopyIntSpec {


    def "new global badge creation does not allow markdown with attachments"() {
        def p1 = createBadge(1)
        p1.description = "Here is a [Link](/api/download/8ab81f77-3484-4f5a-ae58-ae4e7143b449)"

        when:
        skillsService.createGlobalBadge(p1)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Attachments in the description are not allowed when creating a new global badge")
    }

    def "paste markdown with attachment from a project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def badge = createBadge(1)
        skillsService.createGlobalBadge(badge)

        when:
        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateGlobalBadge(badge)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def updatedGlobalBadge = skillsService.getGlobalBadge(badge.badgeId)

        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        badge.description = updatedGlobalBadge.description
        skillsService.updateGlobalBadge(badge)
        skillsService.updateGlobalBadge(badge)
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
        updatedGlobalBadge.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        !newAttachments[0].projectId
        newAttachments[0].skillId == updatedGlobalBadge.badgeId
        !newAttachments[0].quizId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "paste markdown with attachment from a project and changing badge id at the same time"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId)
        def attachment2Href = attachFileAndReturnHref(p1.projectId)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        skillsService.createSkills(p1Skills)

        def badge = createBadge(1)
        skillsService.createGlobalBadge(badge)

        when:
        badge.description = "Here is a [Link](${attachment1Href})".toString()
        String origBadgeId = badge.badgeId
        badge.badgeId = "newBadgeId"
        skillsService.updateGlobalBadge(badge, origBadgeId)

        def origProjSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def updatedGlobalBadge = skillsService.getGlobalBadge(badge.badgeId)

        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        badge.description = updatedGlobalBadge.description
        skillsService.updateGlobalBadge(badge)
        skillsService.updateGlobalBadge(badge)
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
        updatedGlobalBadge.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        !newAttachments[0].projectId
        newAttachments[0].skillId == updatedGlobalBadge.badgeId
        !newAttachments[0].quizId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }


    def "paste markdown with attachment from a another global badge"() {
        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        def attachment1Href = attachFileForGlobalBadgeAndReturnHref(badge.badgeId)

        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateGlobalBadge(badge)

        def badge2 = createBadge(1, 2)
        skillsService.createGlobalBadge(badge2)

        when:
        badge2.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateGlobalBadge(badge2)

        def origGlobalBadge = skillsService.getGlobalBadge(badge.badgeId)
        def updatedGlobalBadge = skillsService.getGlobalBadge(badge2.badgeId)
        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        skillsService.updateGlobalBadge(updatedGlobalBadge)
        skillsService.updateGlobalBadge(updatedGlobalBadge)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origGlobalBadge.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 2
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        !originalAttachment1.projectId
        originalAttachment1.skillId == origGlobalBadge.badgeId
        !originalAttachment1.quizId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        updatedGlobalBadge.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()

        !newAttachments[0].projectId
        newAttachments[0].skillId == updatedGlobalBadge.badgeId
        !newAttachments[0].quizId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }

    def "changing global badge id that has attachment does not duplicate attachments"() {
        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        def attachment1Href = attachFileForGlobalBadgeAndReturnHref(badge.badgeId)

        badge.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateGlobalBadge(badge)

        when:
        def badgeRes = skillsService.getGlobalBadge(badge.badgeId)
        List<Attachment> attachments = attachmentRepo.findAll()

        String origId = badge.badgeId
        badge.badgeId = "newId"
        skillsService.updateGlobalBadge(badge, origId)

        def badgeResAfter = skillsService.getGlobalBadge(badge.badgeId)
        List<Attachment> attachmentsAfter = attachmentRepo.findAll()

        then:
        badgeRes.description == "Here is a [Link](${attachment1Href})"
        badgeResAfter.description == "Here is a [Link](${attachment1Href})"

        attachments.size() == 1
        !attachments[0].projectId
        attachments[0].skillId == origId
        !attachments[0].quizId
        attachment1Href.contains(attachments[0].uuid)

        attachmentsAfter.size() == 1
        attachmentsAfter.uuid.sort() == attachments.uuid.sort()
        !attachmentsAfter[0].projectId
        attachmentsAfter[0].skillId == badgeResAfter.badgeId
        !attachmentsAfter[0].quizId
        attachment1Href.contains(attachmentsAfter[0].uuid)
    }
}
