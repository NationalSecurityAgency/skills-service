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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import skills.auth.UserInfoService
import skills.controller.result.model.UploadAttachmentResult
import skills.storage.model.Attachment
import skills.storage.repos.AttachmentRepo

@Slf4j
@Service
class AttachmentService {

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
        attachmentRepo.saveAttachment(attachment, file.inputStream);
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

    @Transactional(readOnly = true)
    Attachment getAttachment(String uuid) {
        return attachmentRepo.getAttachmentByUuid(uuid)
    }

    Integer deleteGlobalBadgeAttachments(String globalBadgeId) {
        return attachmentRepo.deleteBySkillIdAndProjectIdIsNull(globalBadgeId)
    }

}
