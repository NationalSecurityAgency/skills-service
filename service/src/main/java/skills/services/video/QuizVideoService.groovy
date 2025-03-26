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

    @Value('#{"${skills.config.ui.maxVideoUploadSize:250MB}"}')
    DataSize maxAttachmentSize;

    @Value('#{"${skills.config.allowedVideoUploadMimeTypes}"}')
    List<MediaType> allowedAttachmentMimeTypes;

    SkillVideoAttrs getVideoAttrs(String quizId, Integer questionId) {
        return quizDefService.getVideoAttributesForQuestion(quizId, questionId)
    }

    @Transactional
    SkillVideoAttrs saveVideo(String quizId, Integer questionId, Boolean isAlreadyHosted,
                              MultipartFile file, String videoUrl, String captions, String transcript,
                              Double width, Double height) {

        SkillVideoAttrs existingVideoAttributes = quizDefService.getVideoAttributesForQuestion(quizId, questionId)
        final boolean isEdit = existingVideoAttributes?.videoUrl

        SkillVideoAttrs videoAttrs = new SkillVideoAttrs()
        if (isAlreadyHosted) {
            SkillsValidator.isTrue(existingVideoAttributes?.isInternallyHosted, "Expected video to already be internally hosted but it was not present.", quizId, questionId.toString())
            videoAttrs.videoUrl = existingVideoAttributes.videoUrl
            videoAttrs.videoType = existingVideoAttributes.videoType
            videoAttrs.isInternallyHosted = existingVideoAttributes.isInternallyHosted
            videoAttrs.internallyHostedFileName = existingVideoAttributes.internallyHostedFileName
            videoAttrs.internallyHostedAttachmentUuid = existingVideoAttributes.internallyHostedAttachmentUuid
            videoAttrs.width = existingVideoAttributes.width
            videoAttrs.height = existingVideoAttributes.height
        } else  {
            if (StringUtils.isBlank(videoUrl) && !file) {
                throw new SkillException("Either videoUrl or file must be supplied", quizId, questionId.toString())
            }

            if (existingVideoAttributes?.internallyHostedAttachmentUuid) {
                attachmentRepo.deleteByUuid(existingVideoAttributes.internallyHostedAttachmentUuid)
            }

            if (file) {
                SkillsValidator.isTrue(StringUtils.isBlank(videoUrl), "If file param is provided then videoUrl must be null/blank. Provided url=[${videoUrl}]", quizId)
                AttachmentValidator.isWithinMaxAttachmentSize(file.getSize(), maxAttachmentSize);
                AttachmentValidator.isAllowedAttachmentMimeType(file.getContentType(), allowedAttachmentMimeTypes);
                saveVideoFileAndUpdateAttributes(quizId, questionId, file, videoAttrs)
            } else {
                videoAttrs.isInternallyHosted = false
                videoAttrs.videoUrl = videoUrl
            }

        }

        if (StringUtils.isNotBlank(captions)){
            videoAttrs.captions = InputSanitizer.sanitize(captions)?.trim()
        }
        if (StringUtils.isNotBlank(transcript)){
            videoAttrs.transcript = InputSanitizer.sanitize(transcript)?.trim()
        }
        if(width && height) {
            videoAttrs.width = width
            videoAttrs.height = height
        }
//        boolean isReadOnly = skillDefRepo.isImportedFromCatalog(quizId, questionId)
//        SkillsValidator.isTrue(!isReadOnly, "Cannot set video attributes of read-only skill", quizId, questionId)

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

    void saveVideoFileAndUpdateAttributes(String quizId, Integer questionId, MultipartFile file, SkillVideoAttrs videoAttrs) {
        String userId = userInfoService.getCurrentUserId();
        String uuid = UUID.randomUUID().toString()

        Attachment attachment = new Attachment(
                filename: file.originalFilename,
                contentType: file.contentType,
                uuid: uuid,
                size: file.size,
                userId: userId,
                quizId: quizId)
//                skillId: questionId)
        attachment.setContent(BlobProxy.generateProxy(file.inputStream, file.size))
        attachmentRepo.save(attachment)

        videoAttrs.videoType = attachment.contentType
        videoAttrs.videoUrl = "/api/download/${uuid}"
        videoAttrs.isInternallyHosted = true
        videoAttrs.internallyHostedFileName = attachment.filename
        videoAttrs.internallyHostedAttachmentUuid = attachment.uuid
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
