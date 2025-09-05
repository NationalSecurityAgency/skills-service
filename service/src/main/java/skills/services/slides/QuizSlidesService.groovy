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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import skills.controller.exceptions.SkillQuizException
import skills.services.attributes.QuizAttrs
import skills.services.attributes.SlidesAttrs
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.Attachment
import skills.storage.repos.AttachmentRepo
import skills.storage.repos.QuizDefRepo

@Service
@Slf4j
class QuizSlidesService {

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    QuizAttrsStore quizAttrsStore

    @Autowired
    AttachmentRepo attachmentRepo

    @Autowired
    SlidesHelper slidesHelper

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    QuizAttrs getQuizAttrs(String quizId) {
        return quizAttrsStore.getQuizAttrs(quizId) ?:new QuizAttrs()
    }

    SlidesAttrs getSlidesAttrs(String quizId) {
        return quizAttrsStore.getSlidesAttrs(quizId) ?: new SlidesAttrs()
    }

    @Transactional
    SlidesAttrs saveSlides(String quizId, Boolean isAlreadyHosted, MultipartFile file, String slidesUrl, Double width) {
        QuizAttrs existingQuizAttrs = getQuizAttrs(quizId)
        SlidesAttrs existingSlidesAttrs = existingQuizAttrs?.slidesAttrs ?: null
        final boolean isEdit = existingSlidesAttrs?.url

        SlidesAttrs resAttributes = new SlidesAttrs()
        if (isAlreadyHosted) {
            slidesHelper.updateSlideAttrsWhenAlreadyHosted(resAttributes, existingSlidesAttrs)
        } else  {
            if (StringUtils.isBlank(slidesUrl) && !file) {
                throw new SkillQuizException("Either url or file must be supplied", quizId)
            }

            if (existingSlidesAttrs?.internallyHostedAttachmentUuid) {
                attachmentRepo.deleteByUuid(existingSlidesAttrs.internallyHostedAttachmentUuid)
            }

            resAttributes = validateAndSave(slidesUrl, quizId, file, resAttributes)
        }

        resAttributes.width = width

        existingQuizAttrs.slidesAttrs = resAttributes
        quizAttrsStore.saveQuizAttrs(quizId, existingQuizAttrs)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: isEdit ? DashboardAction.Edit : DashboardAction.Create,
                item: DashboardItem.SlidesSettings,
                itemId: quizId,
                quizId: quizId,
                actionAttributes: resAttributes
        ))

        return resAttributes
    }


    SlidesAttrs validateAndSave(String slidesUrl, String quizId, MultipartFile file, SlidesAttrs slidesAttrs) {
        if (file) {
            slidesHelper.validate(slidesUrl, file)
            Attachment attachment = slidesHelper.constructAttachment(file)
            attachment.quizId = quizId
            attachmentRepo.save(attachment)
            slidesHelper.updateSlideAttrsWithAttachment(attachment, slidesAttrs)
        } else {
            slidesAttrs.isInternallyHosted = false
            slidesAttrs.url = slidesUrl
        }

        return slidesAttrs
    }

    @Transactional
    boolean deleteSlidesAttrs(String quizId) {
        QuizAttrs quizAttrs = quizAttrsStore.getQuizAttrs(quizId)

        String internallyHostedUuid = quizAttrs?.slidesAttrs?.internallyHostedAttachmentUuid
        if (internallyHostedUuid) {
            attachmentRepo.deleteByUuid(internallyHostedUuid)
        }

        if (quizAttrs) {
            quizAttrs.slidesAttrs = null
            quizAttrsStore.saveQuizAttrs(quizId, quizAttrs)
        }

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete,
                item: DashboardItem.SlidesSettings,
                itemId: quizId,
                quizId: quizId,
        ))
    }
}
