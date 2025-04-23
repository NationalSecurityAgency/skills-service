package skills.services.video

import org.apache.commons.lang3.StringUtils
import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.stereotype.Service
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
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
import skills.storage.repos.SkillDefRepo
import skills.utils.InputSanitizer

@Service
@Slf4j
class VideoHelperService {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    AttachmentRepo attachmentRepo

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    QuizDefService quizDefService

    @Value('#{"${skills.config.ui.maxVideoUploadSize:250MB}"}')
    DataSize maxAttachmentSize;

    @Value('#{"${skills.config.allowedVideoUploadMimeTypes}"}')
    List<MediaType> allowedAttachmentMimeTypes;

    static SkillVideoAttrs updateExistingVideo(SkillVideoAttrs videoAttrs, SkillVideoAttrs existingVideoAttributes) {
        videoAttrs.videoUrl = existingVideoAttributes.videoUrl
        videoAttrs.videoType = existingVideoAttributes.videoType
        videoAttrs.isInternallyHosted = existingVideoAttributes.isInternallyHosted
        videoAttrs.internallyHostedFileName = existingVideoAttributes.internallyHostedFileName
        videoAttrs.internallyHostedAttachmentUuid = existingVideoAttributes.internallyHostedAttachmentUuid
        videoAttrs.width = existingVideoAttributes.width
        videoAttrs.height = existingVideoAttributes.height

        return videoAttrs
    }

    static SkillVideoAttrs verifyCaptionsAndTranscript(SkillVideoAttrs videoAttrs, String captions, String transcript) {
        if (StringUtils.isNotBlank(captions)){
            videoAttrs.captions = InputSanitizer.sanitize(captions)?.trim()
        }
        if (StringUtils.isNotBlank(transcript)){
            videoAttrs.transcript = InputSanitizer.sanitize(transcript)?.trim()
        }

        return videoAttrs
    }

    static SkillVideoAttrs setDimensions(SkillVideoAttrs videoAttrs, Double width, Double height) {
        if(width && height) {
            videoAttrs.width = width
            videoAttrs.height = height
        }
        return videoAttrs
    }

    SkillVideoAttrs validateAndSave(String videoUrl, String projectOrQuizId, String skillId, MultipartFile file, SkillVideoAttrs videoAttrs, boolean isQuiz = false) {
        if (file) {
            SkillsValidator.isTrue(StringUtils.isBlank(videoUrl), "If file param is provided then videoUrl must be null/blank. Provided url=[${videoUrl}]", projectOrQuizId, skillId)
            AttachmentValidator.isWithinMaxAttachmentSize(file.getSize(), maxAttachmentSize);
            AttachmentValidator.isAllowedAttachmentMimeType(file.getContentType(), allowedAttachmentMimeTypes);
            saveVideoFileAndUpdateAttributes(projectOrQuizId, skillId, file, videoAttrs, isQuiz)
        } else {
            videoAttrs.isInternallyHosted = false
            videoAttrs.videoUrl = videoUrl
        }

        return videoAttrs
    }

    void saveVideoFileAndUpdateAttributes(String projectOrQuizId, String skillId, MultipartFile file, SkillVideoAttrs videoAttrs, boolean isQuiz = false) {
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

        videoAttrs.videoType = attachment.contentType
        videoAttrs.videoUrl = "/api/download/${uuid}"
        videoAttrs.isInternallyHosted = true
        videoAttrs.internallyHostedFileName = attachment.filename
        videoAttrs.internallyHostedAttachmentUuid = attachment.uuid
    }

}
