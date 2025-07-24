/**
 * Copyright 2025 SkillTree
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
package skills.services.slides

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.unit.DataSize
import org.springframework.web.multipart.MultipartFile
import skills.auth.UserInfoService
import skills.controller.exceptions.AttachmentValidator
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.services.attributes.SkillAttributeService
import skills.services.attributes.SkillVideoAttrs
import skills.services.attributes.SlidesAttrs
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.Attachment
import skills.storage.repos.AttachmentRepo
import skills.storage.repos.SkillDefRepo

@Service
@Slf4j
class AdminSlidesService {

    @Value('#{"${skills.config.ui.maxVideoUploadSize:250MB}"}')
    DataSize maxAttachmentSize;

    @Value('#{"${skills.config.allowedSlidesUploadMimeTypes}"}')
    List<MediaType> allowedSlidesUploadMimeTypes;

    @Autowired
    AttachmentRepo attachmentRepo

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    UserInfoService userInfoService

    @Autowired
    SkillDefRepo skillDefRepo

    @Transactional
    SlidesAttrs saveSlides(String projectId, String skillId, Boolean isAlreadyHosted,
                              MultipartFile file, String slidesUrl, Double scale) {

        SlidesAttrs existingAttributes = skillAttributeService.getSlidesAttrs(projectId, skillId)
        final boolean isEdit = existingAttributes?.url

        SlidesAttrs resAttributes = new SlidesAttrs()
        if (isAlreadyHosted) {
            SkillsValidator.isTrue(existingAttributes?.isInternallyHosted, "Expected slides to already be internally hosted but it was not present.", projectId, skillId)
            resAttributes.url = existingAttributes.url
            resAttributes.type = existingAttributes.type
            resAttributes.isInternallyHosted = existingAttributes.isInternallyHosted
            resAttributes.internallyHostedFileName = existingAttributes.internallyHostedFileName
            resAttributes.internallyHostedAttachmentUuid = existingAttributes.internallyHostedAttachmentUuid
        } else  {
            if (StringUtils.isBlank(slidesUrl) && !file) {
                throw new SkillException("Either url or file must be supplied", projectId, skillId)
            }

            if (existingAttributes?.internallyHostedAttachmentUuid) {
                attachmentRepo.deleteByUuid(existingAttributes.internallyHostedAttachmentUuid)
            }

            resAttributes = validateAndSave(slidesUrl, projectId, skillId, file, resAttributes)
        }

        resAttributes.scale = scale

        boolean isReadOnly = skillDefRepo.isImportedFromCatalog(projectId, skillId)
        SkillsValidator.isTrue(!isReadOnly, "Cannot set video attributes of read-only skill", projectId, skillId)

        skillAttributeService.saveSlidesAttrs(projectId, skillId, resAttributes)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: isEdit ? DashboardAction.Edit : DashboardAction.Create,
                item: DashboardItem.SlidesSettings,
                itemId: skillId,
                projectId: projectId,
                actionAttributes: resAttributes
        ))

        return resAttributes
    }

    SlidesAttrs validateAndSave(String slidesUrl, String projectOrQuizId, String skillId, MultipartFile file, SlidesAttrs slidesAttrs, boolean isQuiz = false) {
        if (file) {
            SkillsValidator.isTrue(StringUtils.isBlank(slidesUrl), "If file param is provided then slidesUrl must be null/blank. Provided url=[${slidesUrl}]", projectOrQuizId, skillId)
            AttachmentValidator.isWithinMaxAttachmentSize(file.getSize(), maxAttachmentSize);
            AttachmentValidator.isAllowedAttachmentMimeType(file.getContentType(), allowedSlidesUploadMimeTypes);
            saveVideoFileAndUpdateAttributes(projectOrQuizId, skillId, file, slidesAttrs, isQuiz)
        } else {
            slidesAttrs.isInternallyHosted = false
            slidesAttrs.url = slidesUrl
        }

        return slidesAttrs
    }

    void saveVideoFileAndUpdateAttributes(String projectOrQuizId, String skillId, MultipartFile file, SlidesAttrs slidesAttrs, boolean isQuiz = false) {
        String userId = userInfoService.getCurrentUserId();
        String uuid = UUID.randomUUID().toString()

        Attachment attachment = new Attachment(
                filename: file.originalFilename,
                contentType: file.contentType,
                uuid: uuid,
                size: file.size,
                userId: userId)
        if(isQuiz) {
            attachment.quizId = projectOrQuizId
        } else {
            attachment.projectId = projectOrQuizId
            attachment.skillId = skillId
        }
        attachment.setContent(BlobProxy.generateProxy(file.inputStream, file.size))
        attachmentRepo.save(attachment)

        slidesAttrs.type = attachment.contentType
        slidesAttrs.url = "/api/download/${uuid}"
        slidesAttrs.isInternallyHosted = true
        slidesAttrs.internallyHostedFileName = attachment.filename
        slidesAttrs.internallyHostedAttachmentUuid = attachment.uuid
    }

}
