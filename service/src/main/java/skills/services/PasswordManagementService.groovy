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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.thymeleaf.context.Context
import skills.UIConfigProperties
import skills.auth.SecurityMode
import skills.auth.UserAuthService
import skills.controller.exceptions.SkillException
import skills.controller.result.model.SettingsResult
import skills.notify.EmailNotifier
import skills.notify.Notifier
import skills.notify.builders.PasswordResetNotificationBuilder
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.storage.model.Notification
import skills.storage.model.UserAttrs
import skills.storage.model.auth.User
import skills.storage.model.auth.UserToken
import skills.storage.repos.PasswordResetTokenRepo
import skills.utils.Expiration
import skills.utils.ExpirationUtils

import java.time.format.DateTimeFormatter

@Slf4j
@Component
@Conditional(SecurityMode.FormAuth)
class PasswordManagementService {

    public static final String DEFAULT_TOKEN_EXPIRATION = "PT2H"

    public static final String RESET_PW_TOKEN_TYPE = 'reset-password'
    public static final String VERIFY_EMAIL_TOKEN_TYPE = 'verify-email'

    @Autowired
    PasswordResetTokenRepo tokenRepo

    @Autowired
    UserAuthService userAuthService

    @Autowired
    UserAttrsService attrsService

    @Autowired
    FeatureService featureService

    @Autowired
    SettingsService settingsService

    @Autowired
    EmailNotifier notifier

    @Autowired
    UIConfigProperties uiConfigProperties

    @Transactional(readOnly = true)
    UserToken loadToken(String token) {
        return tokenRepo.findByToken(token)
    }

    @Transactional()
    void createResetPasswordTokenAndNotifyUser(User user) {
        createTokenAndNotifyUser(user, Notification.Type.PasswordReset.toString(), RESET_PW_TOKEN_TYPE)
    }

    @Transactional()
    void createEmailVerificationTokenAndNotifyUser(String userId) {
        log.info("requesting email verification for [${userId}]")
        User user = userAuthService.getUserRepository().findByUserId(userId)
        if (!user) {
            log.error("no user found for email verification")
            throw new SkillException("No user found for id [${userId}]")
        }
        // remove any existing tokens and then issue a new one
        deleteTokensForUser(user.id, VERIFY_EMAIL_TOKEN_TYPE)
        createTokenAndNotifyUser(user, Notification.Type.VerifyEmail.toString(), VERIFY_EMAIL_TOKEN_TYPE)
    }

    @Transactional()
    void createTokenAndNotifyUser(User user, String notificationType, String tokenType) {
        UserToken token = tokenRepo.findByUserIdAndType(user.userId, tokenType)

        SettingsResult expirationSetting = settingsService.getGlobalSetting(Settings.GLOBAL_RESET_TOKEN_EXPIRATION.settingName)
        String duration = DEFAULT_TOKEN_EXPIRATION
        if(expirationSetting) {
            duration = expirationSetting.value
        }

        Expiration expiration = ExpirationUtils.getExpiration(duration)

        if (!token) {
            token = new UserToken()
            token.user = user
            token.type = tokenType
        }

        token.setToken(UUID.randomUUID().toString())
        token.setExpires(expiration.expiresOn)
        tokenRepo.save(token)
        UserAttrs attrs = attrsService.findByUserId(user.userId)

        if (StringUtils.isEmpty(attrs.email)) {
            throw new SkillException("User [${user.userId}] has no email configured, unable to create reset token")
        }

        String email = attrs.email
        String name = "${attrs.firstName} ${attrs.lastName}"
        String publicUrl = featureService.getPublicUrl()
        if (!publicUrl) {
            throw new SkillException("No public URL is configured for the system, unable to send [${tokenType}] email")
        }

        Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                userIds: [email],
                type: notificationType,
                keyValParams: [
                    recipientName            : name,
                    email                    : email,
                    senderName               : "The team",
                    validTime                : expiration.validFor,
                    publicUrl                : publicUrl,
                    token                    : token.token,
                    communityHeaderDescriptor: uiConfigProperties.ui.defaultCommunityDescriptor
                ],
        )
        notifier.sendNotification(request)
    }

    @Transactional(readOnly = true)
    boolean isTokenForUserId(String token, String userId) {
        tokenRepo.findByTokenAndUserId(token, userId) != null
    }

    @Transactional
    void deleteToken(String token) {
        tokenRepo.deleteByToken(token)
    }

    @Transactional
    void deleteTokensForUser(Integer userId, String tokenType) {
        tokenRepo.deleteByUserIdAndType(userId, tokenType)
    }

    @Transactional
    void deleteTokensOlderThan(Date date) {
        tokenRepo.deleteByExpiresBefore(date)
    }
}
