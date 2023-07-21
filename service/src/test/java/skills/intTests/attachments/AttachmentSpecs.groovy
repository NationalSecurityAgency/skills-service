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

    SkillsService supervisorService

    def setup() {
        supervisorService = createSupervisor()
    }

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
        File file = skillsService.downloadAttachment(uploadResult.href)

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
        result.errorCode == 'InternalError'
        result.explanation.contains('Unexpected Error')
    }

    def "attempt to upload attachment without providing associated projectId, quizId, or skillId"() {
        Map proj = SkillsFactory.createProject()
        skillsService.createProject(proj)
        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        when:
        def result = skillsService.uploadAttachment(resource)

        then:
        result
        !result.success
        result.errorCode == 'BadParam'
        result.explanation.equals('At least one of projectId, quizId, or skillId must be supplied.')
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
        supervisorService.createGlobalBadge(badge)
        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        when:
        def result = supervisorService.uploadAttachment(resource, null, badge.badgeId)

        then:
        result
        result.success
        result.contentType == "application/pdf"
        result.size == contents.getBytes().length
        result.filename == filename
        result.href ==~ /^\/api\/download\/[^\/]+$/
        result.userId == supervisorService.userName
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

    private List<Attachment> findAll() {
        String query = "SELECT a from Attachment a"
        Query getAttachments = entityManager.createQuery(query, Attachment)
        return getAttachments.getResultList()
    }
}
