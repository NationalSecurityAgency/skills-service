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
import org.apache.commons.lang3.StringUtils
import org.hibernate.engine.jdbc.proxy.BlobProxy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.unit.DataSize
import org.springframework.web.multipart.MultipartFile
import skills.auth.UserInfoService
import skills.controller.exceptions.AttachmentValidator
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.result.model.UploadAttachmentResult
import skills.services.admin.UserCommunityService
import skills.storage.model.Attachment
import skills.storage.repos.AttachmentRepo
import skills.storage.repos.SkillDefWithExtraRepo

import java.util.regex.Pattern

@Slf4j
@Service
class AttachmentService {

    private static final Pattern UUID_PATTERN = ~/\[.+\]\(\/api\/download\/([^\)]*)/

    @Autowired
    AttachmentRepo attachmentRepo

    @Autowired
    UserInfoService userInfoService

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    UserCommunityService userCommunityService

    @Value('${skills.config.allowedAttachmentMimeTypes}')
    List<MediaType> allowedAttachmentMimeTypes;

    @Value('${skills.config.maxAttachmentSize:10MB}')
    DataSize maxAttachmentSize;

    @Transactional
    UploadAttachmentResult saveAttachment(MultipartFile file,
                                          String projectId,
                                          String quizId,
                                          String skillId) {
        boolean isAtLeastOneIdPresent = !StringUtils.isBlank(projectId) || !StringUtils.isBlank(quizId) || !StringUtils.isBlank(skillId);
        SkillsValidator.isTrue(isAtLeastOneIdPresent,
                "Attachment must be associated to either a projectId or a quizId or a skillId");

        AttachmentValidator.isWithinMaxAttachmentSize(file.getSize(), maxAttachmentSize);
        AttachmentValidator.isAllowedAttachmentMimeType(file.getContentType(), allowedAttachmentMimeTypes);

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
        persistAttachment(attachment);
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

    void validateUCRules(Attachment attachment, String newProjectId, String newQuizId, String newSkillId) {
        if (userCommunityService.isUserCommunityConfigured()) {
            boolean isAttachmentFromUC = false
            if (attachment.projectId) {
                isAttachmentFromUC = userCommunityService.isUserCommunityOnlyProject(attachment.projectId)
            }
            if (!isAttachmentFromUC && attachment.quizId) {
                isAttachmentFromUC = userCommunityService.isUserCommunityOnlyQuiz(attachment.quizId)
            }

            if (isAttachmentFromUC && !newProjectId && !newQuizId && !newSkillId) {
                log.warn("Cannot copy attachment with uuid=[${attachment.uuid}], projectId=[${attachment.projectId}] quizId=[${attachment.quizId}] to a non-UC destination")
                throw new SkillException("Not authorized to copy the attachment", ErrorCode.AccessDenied)
            }

            boolean toUC = false
            if (newProjectId) {
                toUC = userCommunityService.isUserCommunityOnlyProject(newProjectId)

                if (newProjectId && attachment.projectId && attachment.projectId != newProjectId) {
                    if (isAttachmentFromUC && !toUC) {
                        throw new SkillException("Not allowed to copy attachments to non-UC project[${newProjectId}]", newProjectId, null, ErrorCode.AccessDenied)
                    }
                }
            }
            if (newQuizId) {
                toUC = userCommunityService.isUserCommunityOnlyQuiz(newQuizId)

                if (isAttachmentFromUC && !toUC) {
                    throw new SkillException("Not allowed to copy attachments to non-UC quiz:[${newQuizId}]", ErrorCode.AccessDenied)
                }
            }

            if (toUC || isAttachmentFromUC) {
                String userId = userInfoService.getCurrentUserId();
                if (!userCommunityService.isUserCommunityMember(userId)) {
                    log.warn("User attempted to copy attachment with uuid=[${attachment.uuid}] from projectId=[${attachment.projectId}] to projectId=[${newProjectId}] but user is not UC member")
                    throw new SkillException("Not authorized to copy the attachment", ErrorCode.AccessDenied)
                }
            }
        }
    }

    @Transactional
    Attachment copyAttachmentWithNewUuid(Attachment attachment, String newProjectId = null, String newQuizId = null, String skillId = null) {
        validateUCRules(attachment, newProjectId, newQuizId, skillId)
        Attachment res = constructNewAttachmentWithNewUuid(attachment, newProjectId, newQuizId, skillId)
        persistAttachment(res)
        return res
    }

    static Attachment constructNewAttachmentWithNewUuid(Attachment attachment, String newProjectId = null, String newQuizId = null, String newSkillId = null) {
        boolean isAtLeastOneIdPresent = !StringUtils.isBlank(newProjectId) || !StringUtils.isBlank(newQuizId) || !StringUtils.isBlank(newSkillId);
        if (!isAtLeastOneIdPresent) {
            throw new SkillException("Must provide projectId, quizId or skillId when creating a new attachment from another attachment uuid=[${attachment?.uuid}]", ErrorCode.BadParam)
        }

        String uuid = UUID.randomUUID().toString()
        Attachment res = new Attachment(
                filename: attachment.filename,
                contentType: attachment.contentType,
                uuid: uuid,
                size: attachment.size,
                userId: attachment.userId,
                projectId: newProjectId,
                quizId: newQuizId,
                skillId: newSkillId,
                content: attachment.content
        )
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
    String copyAttachmentsForIncomingDescription(String description, String projectId, String skillId, String quizId, boolean doNotSaveSkillId = false) {
        String res = description
        if (description) {
            List<String> uuidsToHandle = findAttachmentUuids(description)
            uuidsToHandle?.each { String uuid ->
                Attachment attachment = attachmentRepo.findByUuid(uuid)
                if (attachment) {
                    boolean isProjDifferent = projectId && attachment.projectId && projectId != attachment.projectId
                    boolean isQuizDifferent = quizId && attachment.quizId && quizId != attachment.quizId
                    boolean isSkillDifferent = projectId && attachment.skillId != skillId
                    boolean onlyDestSkillId = skillId && (!projectId && !quizId)
                    boolean isOnlyDestSkillIdDifferent = onlyDestSkillId && (attachment.quizId || attachment.projectId || (attachment.skillId && attachment.skillId?.equalsIgnoreCase(skillId)))

                    if (isProjDifferent || isQuizDifferent || isSkillDifferent || isOnlyDestSkillIdDifferent) {
                        // skill id will be updated later in the stack
                        // cannot set it here as skill was not saved yet
                        Attachment newAttachment = copyAttachmentWithNewUuid(attachment, projectId, quizId, doNotSaveSkillId ? null : skillId)
                        res = res.replace("(/api/download/${uuid})", "(/api/download/${newAttachment.uuid})")
                    }
                } else {
                    log.warn("updateAttachmentsInIncomingDescription: failed to find attachment with uuid: [${uuid}]. method params are projectId: [${projectId}], skillId: [${skillId}]")
                }
            }
        }

        return res
    }

    static class CopyAttachmentRes {
        boolean updated = false
        String markdown = ""
    }
    static class CopyAttachmentReq {
        String markdown

        String projectId
        String originalSkillId
        String newSkillId

        String quizId
        String originalQuizId
        Integer questionId = -1
        Integer attemptId = -1
        Integer answerAttemptId = -1
    }

    @Transactional
    CopyAttachmentRes updateAttachmentsAttrsBasedOnUuidsInMarkdown(String markdown, String projectId, String quizId, String originalSkillId, String newSkillId = null) {
        return updateAttachmentsAttrsBasedOnUuidsInMarkdown(new CopyAttachmentReq(
                markdown: markdown,
                projectId: projectId,
                quizId: quizId,
                originalSkillId: originalSkillId,
                newSkillId: newSkillId
        ))
    }

    @Transactional
    CopyAttachmentRes updateAttachmentsAttrsBasedOnUuidsInMarkdown(CopyAttachmentReq attachmentReq) {
        String newSkillId = attachmentReq.newSkillId ?: attachmentReq.originalSkillId
        CopyAttachmentRes res = new CopyAttachmentRes(markdown: attachmentReq.markdown)
        if (res.markdown) {
            List<String> uuids = findAttachmentUuids(res.markdown)
            uuids?.each { uuid ->
                Attachment attachment = attachmentRepo.findByUuid(uuid)

                if (attachment) {
                    boolean isProjDifferent = attachmentReq.projectId && attachment.projectId && attachmentReq.projectId != attachment.projectId
                    boolean isSkillIdMissing = !attachment.quizId && attachment.projectId && attachment.projectId == attachmentReq.projectId && !attachment.skillId && attachmentReq.originalSkillId
                    if (isSkillIdMissing) {
                        // check if the attachment is used by another non-skill description
                        if (attachmentRepo.isAttachmentUsedInProjDesc(attachment.uuid)) {
                            isSkillIdMissing = false
                        }
                    }

                    boolean isSkillDifferent = !isSkillIdMissing && attachmentReq.projectId && attachment.skillId != attachmentReq.originalSkillId
                    boolean onlyDestSkillId = !isSkillIdMissing && attachmentReq.originalSkillId && (!attachmentReq.projectId && !attachmentReq.quizId)
                    boolean isOnlyDestSkillIdDifferent = onlyDestSkillId && (attachment.quizId || attachment.projectId || (attachment.skillId && !attachment.skillId?.equalsIgnoreCase(attachmentReq.originalSkillId)))

                    String quizIdToCompare = attachmentReq.originalQuizId ?: attachmentReq.quizId
                    boolean isQuizDifferent = attachmentReq.quizId && attachment.quizId && quizIdToCompare != attachment.quizId
                    if (!isQuizDifferent && attachmentReq.quizId) {
                        isQuizDifferent = attachmentRepo.isAttachmentUsedInAnotherQuestion(attachment.uuid, attachmentReq.questionId)
                           || ((attachmentReq.questionId > -1 || attachmentReq.attemptId > -1) && attachmentRepo.isAttachmentInQuizDescription(attachment.uuid))
                            || attachmentRepo.isAttachmentInAnotherQuizTextInputAnswer(attachment.uuid, attachmentReq.answerAttemptId)
                    }

                    if (isProjDifferent || isQuizDifferent || isSkillDifferent || isOnlyDestSkillIdDifferent) {
                        // skill id will be updated later in the stack
                        // cannot set it here as skill was not saved yet
                        Attachment newAttachment = copyAttachmentWithNewUuid(attachment, attachmentReq.projectId, attachmentReq.quizId, newSkillId)
                        res.markdown = res.markdown.replace("(/api/download/${uuid})", "(/api/download/${newAttachment.uuid})")
                        res.updated = true
                    }

                    // only override if skill is not already set
                    // this can happen if user copy-and-pasted description
                    if (isSkillIdMissing) {
                        attachment.setSkillId(newSkillId)
                        persistAttachment(attachment)
                    }
                }
            }
        }

        return res
    }

    List<String> findAttachmentUuids(String description) {
        if (description) {
            return UUID_PATTERN.matcher(description).findAll().collect { it[1] }
        }
        return []
    }

    CustomValidationResult validateIfAttachmentsAreAllowedToBeCopied(String description, String projectId, String quizId) {
        if (projectId && quizId) {
            throw new IllegalStateException("must not supply both projectId[${projectId}] and quizId[${quizId}]")
        }

        if (userCommunityService.isUserCommunityConfigured() && description && (projectId || quizId)) {
            def matcher = UUID_PATTERN.matcher(description)

            boolean foundMatch = matcher.find()
            if (foundMatch) {
                Boolean isDestProjNotUC = projectId ? !userCommunityService.isUserCommunityOnlyProject(projectId) : false
                Boolean isDestQuizNotUC = quizId ? !userCommunityService.isUserCommunityOnlyQuiz(quizId) : false
                boolean destinationIsNonUC = (isDestProjNotUC || isDestQuizNotUC)

                if (destinationIsNonUC) {
                    do {
                        String fullMatch = matcher.group(0)
                        String uuid = matcher.group(1)

                        Attachment attachment = attachmentRepo.findByUuid(uuid)
                        if (attachment?.projectId && userCommunityService.isUserCommunityOnlyProject(attachment.projectId)) {
                            String linkName = extractNameFromDownloadLink(fullMatch)
                            return new CustomValidationResult(valid: false, msg: "Attachment [$linkName] is not allowed to be copied to this project")
                        }
                        if (attachment?.quizId && userCommunityService.isUserCommunityOnlyQuiz(attachment.quizId)) {
                            String linkName = extractNameFromDownloadLink(fullMatch)
                            return new CustomValidationResult(valid: false, msg: "Attachment [$linkName] is not allowed to be copied to this quiz")
                        }
                        foundMatch = matcher.find()
                    } while( foundMatch)
                }
            }
        }
        // Return valid result as placeholder - implement your actual validation
        return CustomValidationResult.valid()
    }

    private String extractNameFromDownloadLink(String downloadLink) {
        // Extract the link name by removing the markdown link syntax
        // Format: [<name>](/api/download/<uuid>)
        String linkName = downloadLink.replaceFirst(/\[/, "").replaceFirst(/\](.*)/, "")
        return linkName
    }

    void persistAttachment(Attachment attachment) {
        attachmentRepo.save(attachment)
    }

    boolean doesAttachmentExistInProjectAndLinkedToASkillId(String attachmentUuid, String projectId) {
        return attachmentRepo.existsByUuidAndProjectIdIgnoreCaseAndSkillIdNotNull(attachmentUuid, projectId)
    }

    int updateGlobalBadgeId(String oldGlobalBadgeId, String newGlobalBadgeId) {
        return attachmentRepo.updateAttachmentsSkillIdWhereProjectIsNull(oldGlobalBadgeId, newGlobalBadgeId)
    }
}
