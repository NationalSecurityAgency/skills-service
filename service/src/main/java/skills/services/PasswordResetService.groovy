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
import org.springframework.security.access.method.P
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import skills.auth.SecurityMode
import skills.controller.exceptions.SkillException
import skills.controller.result.model.SettingsResult
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.settings.EmailSettingsService
import skills.storage.model.UserAttrs
import skills.storage.model.auth.PasswordResetToken
import skills.storage.model.auth.User
import skills.storage.repos.PasswordResetTokenRepo

import javax.annotation.PostConstruct
import java.time.LocalDateTime
import java.time.ZoneId

@Slf4j
@Component
@Conditional(SecurityMode.FormAuth)
class PasswordResetService {

    //TODO: extract this to configuration
    public static final int tokenExpirationTimeInHrs = 2

    @Autowired
    PasswordResetTokenRepo tokenRepo

    @Autowired
    UserAttrsService attrsService

    @Autowired
    EmailSendingService emailService

    @Autowired
    SettingsService settingsService


    PasswordResetTokenRepo loadToken(String token) {
        return tokenRepo.findByToken(token)
    }

    void createTokenAndNotifyUser(User user) {
        PasswordResetToken token = tokenRepo.findByUserId(user.userId)
        if (token) {
            log.info("found existing reset token for user [${user.userId}], generating new token with extended expiration time")
            token.setToken(UUID.randomUUID().toString())
            LocalDateTime expires = LocalDateTime.now()
            expires.plusHours(tokenExpirationTimeInHrs)
            token.setExpires(Date.from(expires.atZone(ZoneId.systemDefault()).toInstant()))
        } else {
            log.info("no reset token exists for user [${user.userId}], creating one now")
            token = new PasswordResetToken()
            token.user = user
            token.setToken(UUID.randomUUID().toString())
            LocalDateTime expires = LocalDateTime.now()
            expires.plusHours(tokenExpirationTimeInHrs)

            token.setExpires(Date.from(expires.atZone(ZoneId.systemDefault()).toInstant()))

            tokenRepo.save(token)
        }

        UserAttrs attrs = attrsService.findByUserId(user.userId)

        if (StringUtils.isEmpty(attrs.email)) {
            throw new SkillException("User [${user.userId}] has no email configured, unable to create reset token")
        }

        String email = attrs.email
        String name = "${attrs.firstName} ${attrs.lastName}"
        String validFor = "$tokenExpirationTimeInHrs hours"

        SettingsResult settingsResult = settingsService.getGlobalSetting(Settings.GLOBAL_PUBLIC_URL.settingName)

        if (!settingsResult) {
            throw new SkillException("No public URL is configured for the system, unable to send password reset email")
        }

        String publicUrl = settingsResult.value
        String join = ""
        if (!publicUrl.endsWith("/")){
            join = "/"
        }

        String url = "${publicUrl}${join}resetPassword/${token.token}"

        Context templateContext = new Context()
        templateContext.setVariable("recipientName", name)
        templateContext.setVariable("resetLink", url)
        templateContext.setVariable("senderName", "The team")
        templateContext.setVariable("validTime", validFor)

        //TODO: Error Handling
        emailService.sendEmailWithThymeleafTemplate("SkillTree Password Reset", email, "password_reset.html", templateContext)
    }

    boolean isTokenForUserId(String token, String userId) {
        tokenRepo.findByTokenAndUserId(token, userId) != null
    }

    void deleteToken(String token) {
        tokenRepo.deleteByToken(token)
    }
}
