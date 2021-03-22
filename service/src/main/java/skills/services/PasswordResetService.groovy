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
import org.springframework.security.access.method.P
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import skills.auth.SecurityMode
import skills.controller.exceptions.SkillException
import skills.controller.result.model.SettingsResult
import skills.notify.builders.Formatting
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.settings.EmailSettingsService
import skills.storage.model.UserAttrs
import skills.storage.model.auth.PasswordResetToken
import skills.storage.model.auth.User
import skills.storage.repos.PasswordResetTokenRepo

import javax.annotation.PostConstruct
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

@Slf4j
@Component
@Conditional(SecurityMode.FormAuth)
class PasswordResetService {

    public static final String DEFAULT_TOKEN_EXPIRATION = "PT2H"

    @Autowired
    PasswordResetTokenRepo tokenRepo

    @Autowired
    UserAttrsService attrsService

    @Autowired
    EmailSendingService emailService

    @Autowired
    SettingsService settingsService

    @Transactional(readOnly = true)
    PasswordResetToken loadToken(String token) {
        return tokenRepo.findByToken(token)
    }

    @Transactional()
    void createTokenAndNotifyUser(User user) {
        PasswordResetToken token = tokenRepo.findByUserId(user.userId)

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
            token = new PasswordResetToken()
            token.user = user
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
            throw new SkillException("No public URL is configured for the system, unable to send password reset email")
        }

        String publicUrl = settingsResult.value
        if (!publicUrl.endsWith("/")){
            publicUrl += "/"
        }

        String url = "${publicUrl}"

        Context templateContext = new Context()
        templateContext.setVariable("recipientName", name)
        templateContext.setVariable("senderName", "The team")
        templateContext.setVariable("validTime", validFor)
        templateContext.setVariable("publicUrl", url)
        templateContext.setVariable("resetToken", token.token)
        templateContext.setVariable("htmlHeader", formatting.htmlHeader)
        templateContext.setVariable("htmlFooter", formatting.htmlFooter)

        emailService.sendEmailWithThymeleafTemplate("SkillTree Password Reset", email, "password_reset.html", templateContext)
    }

    @Transactional(readOnly = true)
    boolean isTokenForUserId(String token, String userId) {
        tokenRepo.findByTokenAndUserId(token, userId) != null
    }

    @Transactional
    void deleteToken(String token) {
        tokenRepo.deleteByToken(token)
    }
}
