/**
 * Copyright 2024 SkillTree
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
package skills.quizLoading

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.UIConfigProperties
import skills.controller.result.model.UserRoleRes
import skills.notify.EmailNotifier
import skills.notify.Notifier
import skills.services.AccessSettingsStorageService
import skills.services.FeatureService
import skills.storage.model.Notification
import skills.storage.model.QuizDef
import skills.storage.model.QuizSetting
import skills.storage.model.UserAttrs
import skills.storage.model.UserQuizAttempt
import skills.storage.model.auth.RoleName
import skills.storage.repos.QuizSettingsRepo
import skills.storage.repos.UserAttrsRepo

@Service
@Slf4j
class QuizNotificationService {

    @Autowired
    FeatureService featureService

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    QuizSettingsRepo quizSettingsRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    EmailNotifier notifier

    @Autowired
    UIConfigProperties uiConfigProperties

    void sendGradingRequestNotifications(QuizDef quizDef, String userId) {
        String publicUrl = featureService.getPublicUrl()
        if(!publicUrl) {
            return
        }

        List<UserRoleRes> quizRoles = accessSettingsStorageService.findAllQuizRoles(quizDef.quizId)
        List<UserRoleRes> quizAdminRoles = quizRoles.findAll { it.roleName == RoleName.ROLE_QUIZ_ADMIN }
        List<UserRoleRes> subscribedUsers = quizAdminRoles.findAll { isUserSubscribed(quizDef, it.userId) }

        if (subscribedUsers) {
            UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)
            Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                    userIds: subscribedUsers.collect { it.userId },
                    type: Notification.Type.QuizGradingRequested.toString(),
                    keyValParams: [
                            userRequesting: userAttrs.userIdForDisplay,
                            quizName     : quizDef.name,
                            quizId     : quizDef.quizId,
                            gradingUrl    : "${publicUrl}administrator/quizzes/${quizDef.quizId}/grading",
                            publicUrl     : publicUrl,
                            replyTo       : userAttrs?.email,
                            communityHeaderDescriptor : uiConfigProperties.ui.defaultCommunityDescriptor
                    ],
            )
            notifier.sendNotification(request)
        } else {
            log.debug("No grading notifications subscribers found for quiz [{}]", quizDef.quizId)
        }
    }
    private Boolean isUserSubscribed(QuizDef quizDef, String userId) {
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)
        QuizSetting disableGradingSetting = quizSettingsRepo.findBySettingAndQuizRefIdAndUserRefId(QuizUserPreferences.DisableGradingRequestNotification.preference, quizDef.id, userAttrs.id)
        //default to true if setting doesn't exist
        return disableGradingSetting ? !Boolean.valueOf(disableGradingSetting.value) : true
    }


    void sendGradedRequestNotification(QuizDef quizDef, UserQuizAttempt userQuizAttempt) {
        String publicUrl = featureService.getPublicUrl()
        if (!publicUrl) {
            return
        }

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userQuizAttempt.userId)
        Boolean passed = userQuizAttempt.status == UserQuizAttempt.QuizAttemptStatus.PASSED
        Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                userIds: [userAttrs.userId],
                type: Notification.Type.QuizGradedResponse.toString(),
                keyValParams: [
                        quizName                 : quizDef.name,
                        passed                   : passed,
                        quizUrl                  : "${publicUrl}progress-and-rankings/my-quiz-attempts/${userQuizAttempt.id}",
                        publicUrl                : publicUrl,
                        replyTo                  : userAttrs?.email,
                        communityHeaderDescriptor: uiConfigProperties.ui.defaultCommunityDescriptor
                ],
        )
        notifier.sendNotification(request)
    }

}
