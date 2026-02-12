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
import org.springframework.web.multipart.MultipartFile
import skills.auth.UserInfoService
import skills.controller.exceptions.AttachmentValidator
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.services.attributes.SkillAttributeService
import skills.services.attributes.SkillVideoAttrs
import skills.services.quiz.QuizDefService
import skills.services.quiz.QuizValidatorService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.Attachment
import skills.storage.repos.AttachmentRepo
import skills.utils.InputSanitizer

@Service
@Slf4j
class QuizVideoService {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    AttachmentRepo attachmentRepo

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    QuizDefService quizDefService

    @Autowired
    QuizValidatorService quizValidatorService

    @Autowired
    VideoHelperService videoHelperService

    @Value('#{"${skills.config.ui.maxVideoUploadSize:250MB}"}')
    DataSize maxAttachmentSize;

    @Value('#{"${skills.config.allowedVideoUploadMimeTypes}"}')
    List<MediaType> allowedAttachmentMimeTypes;

    SkillVideoAttrs getVideoAttrs(String quizId, Integer questionId) {
        quizValidatorService.validateQuestion(quizId, questionId)
        return quizDefService.getVideoAttributesForQuestion(quizId, questionId)
    }

    @Transactional
    SkillVideoAttrs saveVideo(String quizId, Integer questionId, Boolean isAlreadyHosted,
                              MultipartFile file, String videoUrl, String captions, String transcript,
                              Double width, Double height) {
        quizValidatorService.validateQuestion(quizId, questionId)
        SkillVideoAttrs existingVideoAttributes = quizDefService.getVideoAttributesForQuestion(quizId, questionId)
        final boolean isEdit = existingVideoAttributes?.videoUrl

        SkillVideoAttrs videoAttrs = new SkillVideoAttrs()
        if (isAlreadyHosted) {
            SkillsValidator.isTrue(existingVideoAttributes?.isInternallyHosted, "Expected video to already be internally hosted but it was not present.", quizId, questionId.toString())
            videoAttrs = videoHelperService.updateExistingVideo(videoAttrs, existingVideoAttributes)
        } else  {
            if (StringUtils.isBlank(videoUrl) && !file) {
                throw new SkillException("Either videoUrl or file must be supplied", quizId, questionId.toString())
            }

            if (existingVideoAttributes?.internallyHostedAttachmentUuid) {
                attachmentRepo.deleteByUuid(existingVideoAttributes.internallyHostedAttachmentUuid)
            }

            videoAttrs = videoHelperService.validateAndSave(videoUrl, quizId, null, file, videoAttrs, true)

        }

        videoAttrs = videoHelperService.verifyCaptionsAndTranscript(videoAttrs, captions, transcript)
        videoAttrs = videoHelperService.setDimensions(videoAttrs, width, height)

        quizDefService.saveVideoAttributesForQuestion(quizId, questionId, videoAttrs)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: isEdit ? DashboardAction.Edit : DashboardAction.Create,
                item: DashboardItem.VideoSettings,
                itemId: questionId,
                quizId: quizId,
                actionAttributes: videoAttrs
        ))

        return videoAttrs
    }

    @Transactional
    boolean deleteVideoAttrs(String quizId, Integer questionId) {
        SkillVideoAttrs existingVideoAttributes = quizDefService.getVideoAttributesForQuestion(quizId, questionId)

        if (existingVideoAttributes?.internallyHostedAttachmentUuid) {
            attachmentRepo.deleteByUuid(existingVideoAttributes.internallyHostedAttachmentUuid)
        }

        quizDefService.deleteVideoAttrs(quizId, questionId)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete,
                item: DashboardItem.VideoSettings,
                itemId: questionId,
                quizId: quizId,
        ))
    }
}
