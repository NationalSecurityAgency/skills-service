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
import org.apache.commons.lang3.time.DurationFormatUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.thymeleaf.context.Context
import skills.auth.SecurityMode
import skills.auth.UserAuthService
import skills.controller.exceptions.SkillException
import skills.controller.result.model.SettingsResult
import skills.notify.builders.Formatting
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.settings.EmailSettingsService
import skills.storage.model.UserAttrs
import skills.storage.model.auth.User
import skills.storage.model.auth.UserToken
import skills.storage.repos.PasswordResetTokenRepo

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

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
    EmailSendingService emailService

    @Autowired
    SettingsService settingsService

    @Transactional(readOnly = true)
    UserToken loadToken(String token) {
        return tokenRepo.findByToken(token)
    }

    @Transactional()
    void createResetPasswordTokenAndNotifyUser(User user) {
        createTokenAndNotifyUser(user, "SkillTree Password Reset", "password_reset.html", RESET_PW_TOKEN_TYPE)
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
        createTokenAndNotifyUser(user, "Please verify your email address", "verify_email.html", VERIFY_EMAIL_TOKEN_TYPE)
    }

    @Transactional()
    void createTokenAndNotifyUser(User user, String subject, String template, String type) {
        UserToken token = tokenRepo.findByUserId(user.userId)

        SettingsResult expirationSetting = settingsService.getGlobalSetting(Settings.GLOBAL_RESET_TOKEN_EXPIRATION.settingName)
        Duration expirationDuration = null
        if(expirationSetting) {
            expirationDuration = Duration.parse(expirationSetting.value)
        } else {
            expirationDuration = Duration.parse(DEFAULT_TOKEN_EXPIRATION)
        }

        String validFor = DurationFormatUtils.formatDurationWords(expirationDuration.toMillis(), true, true)
        LocalDateTime expires = LocalDateTime.now()
        expires = expirationDuration.addTo(expires)

        if (!token) {
            token = new UserToken()
            token.user = user
            token.type = type
        }

        token.setToken(UUID.randomUUID().toString())
        token.setExpires(Date.from(expires.atZone(ZoneId.systemDefault()).toInstant()))
        tokenRepo.save(token)
        UserAttrs attrs = attrsService.findByUserId(user.userId)

        if (StringUtils.isEmpty(attrs.email)) {
            throw new SkillException("User [${user.userId}] has no email configured, unable to create reset token")
        }

        String email = attrs.email
        String name = "${attrs.firstName} ${attrs.lastName}"

        SettingsResult settingsResult = settingsService.getGlobalSetting(Settings.GLOBAL_PUBLIC_URL.settingName)

        List<SettingsResult> emailSettings = settingsService.getGlobalSettingsByGroup(EmailSettingsService.settingsGroup);

        Formatting formatting = new Formatting(
                htmlHeader: emailSettings.find {it.setting == EmailSettingsService.htmlHeader }?.value ?: null,
                plaintextHeader: emailSettings.find { it.setting == EmailSettingsService.plaintextHeader }?.value ?: null,
                htmlFooter: emailSettings.find { it.setting == EmailSettingsService.htmlFooter }?.value ?: null,
                plaintextFooter: emailSettings.find { it.setting == EmailSettingsService.plaintextFooter }?.value ?:null
        )

        if (!settingsResult) {
            throw new SkillException("No public URL is configured for the system, unable to send ${type} email")
        }

        String publicUrl = settingsResult.value
        if (!publicUrl.endsWith("/")){
            publicUrl += "/"
        }

        String url = "${publicUrl}"

        Context templateContext = new Context()
        templateContext.setVariable("recipientName", name)
        templateContext.setVariable("email", email)
        templateContext.setVariable("senderName", "The team")
        templateContext.setVariable("validTime", validFor)
        templateContext.setVariable("publicUrl", url)
        templateContext.setVariable("token", token.token)
        templateContext.setVariable("htmlHeader", formatting.htmlHeader)
        templateContext.setVariable("htmlFooter", formatting.htmlFooter)

        emailService.sendEmailWithThymeleafTemplate(subject, email, template, templateContext)
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
}
