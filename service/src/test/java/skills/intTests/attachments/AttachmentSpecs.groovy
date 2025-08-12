/**
 * Copyright 2021 SkillTree
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
package skills.intTests.attachments

import groovy.util.logging.Slf4j
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Query
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.util.unit.DataSize
import skills.intTests.utils.*
import skills.services.AttachmentService
import skills.storage.model.Attachment
import skills.utils.GroovyToJavaByteUtils

@Slf4j
class AttachmentSpecs extends DefaultIntSpec {

    @Value('#{"${skills.config.maxAttachmentSize}"}')
    DataSize maxAttachmentSize

    @Value('#{"${skills.config.allowedAttachmentFileTypes}"}')
    List<String> allowedAttachmentFileTypes

    @PersistenceContext
    EntityManager entityManager

    @Autowired
    TransactionHelper transactionHelper

    @Autowired
    AttachmentService attachmentService

    def "upload attachment"() {
        Map proj = SkillsFactory.createProject()
        skillsService.createProject(proj)
        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        when:
        def result = skillsService.uploadAttachment(resource, proj.projectId)

        then:
        result
        result.success
        result.contentType == "application/pdf"
        result.size == contents.getBytes().length
        result.filename == filename
        result.href ==~ /^\/api\/download\/[^\/]+$/
        result.userId == skillsService.userName
    }

    def "upload and then download attachment"() {
        Map proj = SkillsFactory.createProject()
        skillsService.createProject(proj)
        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        when:
        def uploadResult = skillsService.uploadAttachment(resource, proj.projectId)
        SkillsService.FileAndHeaders fileAndHeaders = skillsService.downloadAttachment(uploadResult.href)
        File file = fileAndHeaders.file

        then:
        uploadResult
        uploadResult.success
        uploadResult.contentType == "application/pdf"
        uploadResult.size == contents.getBytes().length
        uploadResult.filename == filename
        uploadResult.href ==~ /^\/api\/download\/[^\/]+$/
        uploadResult.userId == skillsService.userName

        file
        file.bytes == contents.getBytes()
    }

    def "upload all valid attachment mime types"() {
        Map proj = SkillsFactory.createProject()
        skillsService.createProject(proj)
        List<Resource> resources = []
        List results = []
        allowedAttachmentFileTypes.each { fileType ->
            String filename = "test${fileType}"
            resources.add(GroovyToJavaByteUtils.toByteArrayResource('Test is a test', filename))
        }

        when:
        resources.each { resource ->
            results.add(skillsService.uploadAttachment(resource, proj.projectId))
        }

        then:
        results
        results.size() == resources.size()
        results.every { it.success }
    }

    def "attempt to upload attachment with invalid mime-type"() {
        Map proj = SkillsFactory.createProject()
        skillsService.createProject(proj)
        String filename = 'test-zip.zip'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource('Test is a test', filename)

        when:
        def result = skillsService.uploadAttachment(resource, proj.projectId)

        then:

        result
        !result.success
        result.errorCode == 'BadParam'
        result.explanation.contains('Invalid media type [application/zip]')
    }

    def "attempt to upload attachment with size greater than max allowed"() {
        Map proj = SkillsFactory.createProject()
        skillsService.createProject(proj)
        String filename = 'test-zip.pdf'
        Integer fileSize = maxAttachmentSize.toBytes()+1
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(fileSize, filename)

        when:
        def result = skillsService.uploadAttachment(resource, proj.projectId)

        then:

        result
        !result.success
        result.errorCode == 'BadParam'
        result.explanation.contains('File size [1 MB] exceeds maximum file size [1 MB]')
    }

    def "attempt to upload attachment that is a associated to both a projectId and a quizId"() {
        Map proj = SkillsFactory.createProject()
        skillsService.createProject(proj)
        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        when:
        def result = skillsService.uploadAttachment(resource, "projectId", null, "quizId")

        then:
        result
        !result.success
        result.errorCode == 'BadParam'
        result.explanation.equals('Attachment cannot be associated to both a projectId and a quizId')
    }

    def "upload attachment with just projectId"() {
        Map proj = SkillsFactory.createProject()
        skillsService.createProject(proj)
        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        when:
        def result = skillsService.uploadAttachment(resource, proj.projectId)

        then:
        result
        result.success
        result.contentType == "application/pdf"
        result.size == contents.getBytes().length
        result.filename == filename
        result.href ==~ /^\/api\/download\/[^\/]+$/
        result.userId == skillsService.userName
        result.projectId == proj.projectId
        result.quizId == null
        result.skillId == null
    }

    def "upload attachment with just quizId"() {
        def quiz = QuizDefFactory.createQuizSurvey()
        skillsService.createQuizDef(quiz)
        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        when:
        def result = skillsService.uploadAttachment(resource, null, null, quiz.quizId)

        then:
        result
        result.success
        result.contentType == "application/pdf"
        result.size == contents.getBytes().length
        result.filename == filename
        result.href ==~ /^\/api\/download\/[^\/]+$/
        result.userId == skillsService.userName
        result.projectId == null
        result.quizId == quiz.quizId
        result.skillId == null
    }

    def "upload attachment with just skillId"() {
        def badge = SkillsFactory.createBadge()
        badge.enabled = false
        skillsService.createGlobalBadge(badge)
        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        when:
        def result = skillsService.uploadAttachment(resource, null, badge.badgeId)

        then:
        result
        result.success
        result.contentType == "application/pdf"
        result.size == contents.getBytes().length
        result.filename == filename
        result.href ==~ /^\/api\/download\/[^\/]+$/
        result.userId == skillsService.userName
        result.projectId == null
        result.quizId == null
        result.skillId == badge.badgeId
    }

    def "upload attachment with projectId and skillId"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        when:
        def result = skillsService.uploadAttachment(resource, proj.projectId, subject.subjectId)

        then:
        result
        result.success
        result.contentType == "application/pdf"
        result.size == contents.getBytes().length
        result.filename == filename
        result.href ==~ /^\/api\/download\/[^\/]+$/
        result.userId == skillsService.userName
        result.projectId == proj.projectId
        result.quizId == null
        result.skillId == subject.subjectId
    }

    def "do not allow uploading multiple files at once"() {
        setup:
        Map proj = SkillsFactory.createProject()
        skillsService.createProject(proj)
        Integer deleteCount = transactionHelper.doInTransaction {
            entityManager.createQuery("DELETE from Attachment").executeUpdate()
        }
        log.info("Delete [${deleteCount}] existing attachments")

        List<Resource> resources = []
        allowedAttachmentFileTypes.each { fileType ->
            String filename = "test${fileType}"
            resources.add(GroovyToJavaByteUtils.toByteArrayResource('Test is a test', filename))
        }

        when:
        // only one will be uploaded - last one wins
        def result = skillsService.uploadAttachments(resources)
        List<Attachment> attachments = this.findAll()

        then:
        result
        result.success
        result.filename == resources.last().filename
        attachments
        attachments.size() == 1
    }

    def "attachment gets proper id's assigned on creation when present description markdown"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map skill = SkillsFactory.createSkill()
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 2)
        def badge = SkillsFactory.createBadge()
        def quiz = QuizDefFactory.createQuizSurvey()
        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        when:
        def projAttachRes = skillsService.uploadAttachment(resource)
        proj.description = "description with [${filename}](/api/download/${projAttachRes.uuid})".toString()
        skillsService.createProject(proj)

        def subjAttachRes = skillsService.uploadAttachment(resource)
        subject.description = "description with [${filename}](/api/download/${subjAttachRes.uuid})".toString()
        skillsService.createSubject(subject)

        def skillAttachRes = skillsService.uploadAttachment(resource)
        skill.description = "description with [${filename}](/api/download/${skillAttachRes.uuid})".toString()
        skillsService.createSkill(skill)

        def skillsGroupAttachRes = skillsService.uploadAttachment(resource)
        skillsGroup.description = "description with [${filename}](/api/download/${skillsGroupAttachRes.uuid})".toString()
        skillsService.createSkill(skillsGroup)

        def badgeAttachRes = skillsService.uploadAttachment(resource)
        badge.description = "description with [${filename}](/api/download/${badgeAttachRes.uuid})".toString()
        skillsService.createBadge(badge)

        def quizAttachRes = skillsService.uploadAttachment(resource)
        quiz.description = "description with [${filename}](/api/download/${quizAttachRes.uuid})".toString()
        skillsService.createQuizDef(quiz)

        Attachment projAttach = attachmentService.getAttachment(projAttachRes.uuid)
        Attachment subjAttach = attachmentService.getAttachment(subjAttachRes.uuid)
        Attachment skillAttach = attachmentService.getAttachment(skillAttachRes.uuid)
        Attachment skillsGroupAttach = attachmentService.getAttachment(skillsGroupAttachRes.uuid)
        Attachment badgeAttach = attachmentService.getAttachment(badgeAttachRes.uuid)
        Attachment quizAttach = attachmentService.getAttachment(quizAttachRes.uuid)

        then:
        projAttach.projectId == proj.projectId
        projAttach.quizId == null
        projAttach.skillId == null

        subjAttach.projectId == subject.projectId
        subjAttach.quizId == null
        subjAttach.skillId == subject.subjectId

        skillAttach.projectId == skill.projectId
        skillAttach.quizId == null
        skillAttach.skillId == skill.skillId

        skillsGroupAttach.projectId == skillsGroup.projectId
        skillsGroupAttach.quizId == null
        skillsGroupAttach.skillId == skillsGroup.skillId

        badgeAttach.projectId == badge.projectId
        badgeAttach.quizId == null
        badgeAttach.skillId == badge.badgeId

        quizAttach.projectId == null
        quizAttach.quizId == quiz.quizId
        quizAttach.skillId == null
    }

    private List<Attachment> findAll() {
        String query = "SELECT a from Attachment a"
        Query getAttachments = entityManager.createQuery(query, Attachment)
        return getAttachments.getResultList()
    }
}
