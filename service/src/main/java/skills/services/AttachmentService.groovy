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
package skills.services

import groovy.util.logging.Slf4j
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import skills.auth.UserInfoService
import skills.controller.result.model.UploadAttachmentResult
import skills.storage.model.Attachment
import skills.storage.repos.AttachmentRepo

import java.util.regex.Pattern

@Slf4j
@Service
class AttachmentService {

    private static final Pattern UUID_PATTERN = ~/\[.+\]\(\/api\/download|playVideo\/([^\)]*)/

    @Autowired
    AttachmentRepo attachmentRepo

    @Autowired
    UserInfoService userInfoService

    @Transactional
    UploadAttachmentResult saveAttachment(MultipartFile file,
                                          String projectId,
                                          String quizId,
                                          String skillId) {
        String userId = userInfoService.getCurrentUserId();
        String uuid = UUID.randomUUID().toString()

        Attachment attachment = new Attachment(
                filename: file.originalFilename,
                contentType: file.contentType,
                uuid: uuid,
                size: file.size,
                userId: userId,
                projectId: projectId,
                quizId: quizId,
                skillId: skillId)
        attachment.setContent(BlobProxy.generateProxy(file.inputStream, file.size))
        attachmentRepo.save(attachment);
        return new UploadAttachmentResult(
                filename: file.originalFilename,
                contentType: file.contentType,
                href: "/api/download/${uuid}",
                uuid: uuid,
                size: file.size,
                userId: userId,
                projectId: projectId,
                quizId: quizId,
                skillId: skillId,
        )
    }

    @Transactional
    Attachment copyAttachmentWithNewUuid(Attachment attachment, String newProjectId = null) {
        String uuid = UUID.randomUUID().toString()
        Attachment res = new Attachment(
                filename: attachment.filename,
                contentType: attachment.contentType,
                uuid: uuid,
                size: attachment.size,
                userId: attachment.userId,
                projectId: newProjectId ?: attachment.projectId,
                quizId: newProjectId ? null : attachment.quizId, // then now a quiz for sure
                skillId: newProjectId ? null : attachment.skillId, // if a new project then skillId may not exist
                content: attachment.content
        )
        attachmentRepo.save(res)
        return res
    }

    @Transactional(readOnly = true)
    Attachment getAttachment(String uuid) {
        return attachmentRepo.findByUuid(uuid)
    }

    @Transactional
    Integer deleteGlobalBadgeAttachments(String globalBadgeId) {
        return attachmentRepo.deleteBySkillIdAndProjectIdIsNull(globalBadgeId)
    }

    @Transactional
    void updateAttachmentsFoundInMarkdown(String description, String projectId, String quizId, String skillId) {
        if (description) {
            UUID_PATTERN.matcher(description).findAll().collect { it[1] }.each { uuid ->
                Attachment attachment = attachmentRepo.findByUuid(uuid)
                if (!attachment) {
                    throw new IllegalStateException("Failed to find attachment with uuid: [${uuid}]. method params are projectId: [${projectId}], quizId: [${quizId}], skillId: [${skillId}]")
                }
                boolean changed = false
                if (attachment.projectId != projectId) {
                    attachment.setProjectId(projectId)
                    changed = true
                }
                if (attachment.quizId != quizId) {
                    attachment.setQuizId(quizId)
                    changed = true
                }

                // only override if skill is not already set
                // this can happen if user copy-and-pasted description
                if (!attachment.skillId && skillId) {
                    attachment.setSkillId(skillId)
                    changed = true
                }
                if (changed) {
                    attachmentRepo.save(attachment)
                }
            }
        }
    }

    List<String> findAttachmentUuids(String description) {
        if (description) {
            return UUID_PATTERN.matcher(description).findAll().collect { it[1] }
        }
        return []
    }

}
