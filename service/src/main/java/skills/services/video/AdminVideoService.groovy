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
package skills.services.video

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.unit.DataSize
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import skills.auth.UserInfoService
import skills.controller.exceptions.AttachmentValidator
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.services.attributes.SkillAttributeService
import skills.services.attributes.SkillVideoAttrs
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.Attachment
import skills.storage.repos.AttachmentRepo
import skills.storage.repos.SkillDefRepo
import skills.utils.InputSanitizer

@Service
@Slf4j
class AdminVideoService {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    AttachmentRepo attachmentRepo

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    VideoHelperService videoHelperService

    @Value('#{"${skills.config.ui.maxVideoUploadSize:250MB}"}')
    DataSize maxAttachmentSize;

    @Value('#{"${skills.config.allowedVideoUploadMimeTypes}"}')
    List<MediaType> allowedAttachmentMimeTypes;

    @Transactional
    SkillVideoAttrs saveVideo(String projectId, String skillId, Boolean isAlreadyHosted,
                              MultipartFile file, String videoUrl, String captions, String transcript,
                              Double width, Double height) {

        SkillVideoAttrs existingVideoAttributes = skillAttributeService.getVideoAttrs(projectId, skillId)
        final boolean isEdit = existingVideoAttributes?.videoUrl

        SkillVideoAttrs videoAttrs = new SkillVideoAttrs()
        if (isAlreadyHosted) {
            SkillsValidator.isTrue(existingVideoAttributes?.isInternallyHosted, "Expected video to already be internally hosted but it was not present.", projectId, skillId)
            videoAttrs = videoHelperService.updateExistingVideo(videoAttrs, existingVideoAttributes)
        } else  {
            if (StringUtils.isBlank(videoUrl) && !file) {
                throw new SkillException("Either videoUrl or file must be supplied", projectId, skillId)
            }

            if (existingVideoAttributes?.internallyHostedAttachmentUuid) {
                attachmentRepo.deleteByUuid(existingVideoAttributes.internallyHostedAttachmentUuid)
            }

            videoAttrs = videoHelperService.validateAndSave(videoUrl, projectId, skillId, file, videoAttrs)

        }

        videoAttrs = videoHelperService.verifyCaptionsAndTranscript(videoAttrs, captions, transcript)
        videoAttrs = videoHelperService.setDimensions(videoAttrs, width, height)

        boolean isReadOnly = skillDefRepo.isImportedFromCatalog(projectId, skillId)
        SkillsValidator.isTrue(!isReadOnly, "Cannot set video attributes of read-only skill", projectId, skillId)

        skillAttributeService.saveVideoAttrs(projectId, skillId, videoAttrs)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: isEdit ? DashboardAction.Edit : DashboardAction.Create,
                item: DashboardItem.VideoSettings,
                itemId: skillId,
                projectId: projectId,
                actionAttributes: videoAttrs
        ))

        return videoAttrs
    }

    @Transactional
    boolean deleteVideoAttrs(String projectId, String skillId) {
        SkillVideoAttrs existingVideoAttributes = skillAttributeService.getVideoAttrs(projectId, skillId)

        if (existingVideoAttributes?.internallyHostedAttachmentUuid) {
            attachmentRepo.deleteByUuid(existingVideoAttributes.internallyHostedAttachmentUuid)
        }

        skillAttributeService.deleteVideoAttrs(projectId, skillId)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete,
                item: DashboardItem.VideoSettings,
                itemId: skillId,
                projectId: projectId,
        ))
    }
}
