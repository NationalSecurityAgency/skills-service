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
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.unit.DataSize
import skills.intTests.utils.DefaultIntSpec
import skills.storage.model.Attachment

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.Query

@Slf4j
class AttachmentSpecs extends DefaultIntSpec {

    @Value('#{"${skills.config.maxAttachmentSize}"}')
    DataSize maxAttachmentSize;

    @Value('#{"${skills.config.allowedAttachmentFileTypes}"}')
    List<String> allowedAttachmentFileTypes;

    @PersistenceContext
    EntityManager entityManager;

    def "upload attachment"() {
        String filename = 'test-pdf.pdf'
        byte[] bytes = 'Test is a test'.getBytes('UTF-8')
        Resource resource = new ByteArrayResource(bytes) {
            @Override
            String getFilename() {
                return filename
            }
        }

        when:
        def result = skillsService.uploadAttachment(resource)

        then:
        result
        result.success
        result.contentType == "application/pdf"
        result.size == bytes.length
        result.filename == filename
        result.href ==~ /^\/api\/download\/[^\/]+\/test-pdf.pdf$/
    }

    def "upload and then download attachment"() {
        String filename = 'test-pdf.pdf'
        byte[] bytes = 'Test is a test'.getBytes('UTF-8')
        Resource resource = new ByteArrayResource(bytes) {
            @Override
            String getFilename() {
                return filename
            }
        }

        when:
        def uploadResult = skillsService.uploadAttachment(resource)
        File file = skillsService.downloadAttachment(uploadResult.href)

        then:
        uploadResult
        uploadResult.success
        uploadResult.contentType == "application/pdf"
        uploadResult.size == bytes.length
        uploadResult.filename == filename
        uploadResult.href ==~ /^\/api\/download\/[^\/]+\/test-pdf.pdf$/

        file
        file.bytes == bytes
    }

    def "upload all valid attachment mime types"() {
        List<Resource> resources = []
        List results = []
        allowedAttachmentFileTypes.each { fileType ->
            String filename = "test${fileType}"
            byte[] bytes = 'Test is a test'.getBytes('UTF-8')
            Resource resource = new ByteArrayResource(bytes) {
                @Override
                String getFilename() {
                    return filename
                }
            }
            resources.add(resource)
        }

        when:
        resources.each { resource ->
            results.add(skillsService.uploadAttachment(resource))
        }

        then:
        results
        results.size() == resources.size()
        results.every { it.success }
    }

    def "attempt to upload attachment with invalid mime-type"() {
        String filename = 'test-zip.zip'
        byte[] bytes = 'Test is a test'.getBytes('UTF-8')
        Resource resource = new ByteArrayResource(bytes) {
            @Override
            String getFilename() {
                return filename
            }
        }

        when:
        def result = skillsService.uploadAttachment(resource)

        then:

        result
        !result.success
        result.errorCode == 'BadParam'
        result.explanation.contains('Invalid media type [application/zip]')
    }

    def "attempt to upload attachment with size greater than max allowed"() {
        String filename = 'test-zip.zip'
        Integer fileSize = maxAttachmentSize.toBytes()//+1
        byte[] bytes = new byte[fileSize]
        new Random().nextBytes(bytes)
        Resource resource = new ByteArrayResource(bytes) {
            @Override
            String getFilename() {
                return filename
            }
        }

        when:
        def result = skillsService.uploadAttachment(resource)

        then:

        result
        !result.success
        result.errorCode == 'InternalError'
        result.explanation.contains('Unexpected Error')
    }

    @Transactional
    def "do not allow uploading multiple files at once"() {
        setup:
        int deleteCount = this.deleteAll()
        log.info("Delete [${deleteCount}] existing attachments")

        List<Resource> resources = []
        allowedAttachmentFileTypes.each { fileType ->
            String filename = "test${fileType}"
            byte[] bytes = 'Test is a test'.getBytes('UTF-8')
            Resource resource = new ByteArrayResource(bytes) {
                @Override
                String getFilename() {
                    return filename
                }
            }
            resources.add(resource)
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

    int deleteAll() {
        return entityManager.createQuery("DELETE from Attachment").executeUpdate()
    }
}
