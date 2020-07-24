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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Component
import org.thymeleaf.util.StringUtils
import skills.controller.result.model.SettingsResult
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.settings.EmailSettingsService

@Slf4j
@Component
class FeatureService {
    @Autowired
    SettingsService settingsService

    @Autowired
    EmailSettingsService emailSettingsService

    boolean isPasswordResetFeatureEnabled() {
        JavaMailSender mailSender = emailSettingsService.getMailSender()
        SettingsResult publicUrl = settingsService.getGlobalSetting(Settings.GLOBAL_PUBLIC_URL.settingName)
        boolean publicUrlConfigured = true
        boolean mailSenderConfigured = true

        if (mailSender == null) {
            log.warn("Email Settings are not configured or are invalid, please configure through Dashboard for Password Reset feature to function")
            mailSenderConfigured = false;
        } else {
            if (mailSender instanceof JavaMailSenderImpl){
                try {
                    mailSender.testConnection()
                } catch (Exception e) {
                    mailSenderConfigured = false
                    log.warn('Email Settings are invalid, please configure valid settings through the Dashboard for Password Reset feature to function')
                }
            }
        }

        if (StringUtils.isEmpty(publicUrl?.value)) {
            log.warn("Public URL setting is not configured, please configure through Dashboard for Password Reset feature to function")
            publicUrlConfigured = false
        }

        return mailSenderConfigured && publicUrlConfigured
    }
}
