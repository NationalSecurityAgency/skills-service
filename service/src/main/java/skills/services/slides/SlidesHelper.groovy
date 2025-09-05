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
import org.springframework.util.unit.DataSize
import org.springframework.web.multipart.MultipartFile
import skills.auth.UserInfoService
import skills.controller.exceptions.AttachmentValidator
import skills.controller.exceptions.SkillsValidator
import skills.services.attributes.SlidesAttrs
import skills.storage.model.Attachment
import skills.storage.repos.AttachmentRepo

@Service
@Slf4j
class SlidesHelper {

    @Value('#{"${skills.config.ui.maxSlidesUploadSize:250MB}"}')
    DataSize maxAttachmentSize;

    @Value('#{"${skills.config.allowedSlidesUploadMimeTypes}"}')
    List<MediaType> allowedSlidesUploadMimeTypes;

    @Autowired
    AttachmentRepo attachmentRepo

    @Autowired
    UserInfoService userInfoService

    void validate(String slidesUrl, MultipartFile file) {
        SkillsValidator.isTrue(StringUtils.isBlank(slidesUrl), "If file param is provided then slidesUrl must be null/blank. Provided url=[${slidesUrl}]")
        AttachmentValidator.isWithinMaxAttachmentSize(file.getSize(), maxAttachmentSize);
        AttachmentValidator.isAllowedAttachmentMimeType(file.getContentType(), allowedSlidesUploadMimeTypes);
    }

    Attachment constructAttachment(MultipartFile file) {
        String userId = userInfoService.getCurrentUserId();
        String uuid = UUID.randomUUID().toString()

        Attachment attachment = new Attachment(
                filename: file.originalFilename,
                contentType: file.contentType,
                uuid: uuid,
                size: file.size,
                userId: userId)
        attachment.setContent(BlobProxy.generateProxy(file.inputStream, file.size))
        return attachment
    }


    static void updateSlideAttrsWithAttachment(Attachment attachment, SlidesAttrs slidesAttrs) {
        slidesAttrs.type = attachment.contentType
        slidesAttrs.url = "/api/download/${attachment.uuid}"
        slidesAttrs.isInternallyHosted = true
        slidesAttrs.internallyHostedFileName = attachment.filename
        slidesAttrs.internallyHostedAttachmentUuid = attachment.uuid
    }

    static void updateSlideAttrsWhenAlreadyHosted(SlidesAttrs resAttributes, SlidesAttrs existingAttributes) {
        SkillsValidator.isTrue(existingAttributes?.isInternallyHosted, "Expected slides to already be internally hosted but it was not present.")
        resAttributes.url = existingAttributes.url
        resAttributes.type = existingAttributes.type
        resAttributes.isInternallyHosted = existingAttributes.isInternallyHosted
        resAttributes.internallyHostedFileName = existingAttributes.internallyHostedFileName
        resAttributes.internallyHostedAttachmentUuid = existingAttributes.internallyHostedAttachmentUuid
    }

}
