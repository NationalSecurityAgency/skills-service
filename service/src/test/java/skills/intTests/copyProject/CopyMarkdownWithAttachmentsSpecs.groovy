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
import skills.intTests.utils.SkillsClientException
import skills.storage.model.Attachment

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

        def copyProjSkill1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def copyProjSkill2 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        List<Attachment> attachments = attachmentRepo.findAll()
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
        }
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
        }
    }

}
