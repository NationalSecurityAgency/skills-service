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
        return isEmailServiceFeatureEnabled('Password Reset')
    }

    boolean isEmailServiceFeatureEnabled(String featureName = 'Email Service') {
        List<Settings> expected = [Settings.GLOBAL_PUBLIC_URL, Settings.GLOBAL_FROM_EMAIL]
        for (Settings setting : expected) {
            SettingsResult settingRes = settingsService.getGlobalSetting(setting.settingName)
            if (StringUtils.isEmpty(settingRes?.value)) {
                log.warn("[${setting.settingName}] setting is not configured, please configure through Dashboard for ${featureName} feature to function")
                return false
            }
        }

        if (!emailSettingsService.fetchEmailSettings()?.host) {
            log.warn("Email Settings are not configured or are invalid, please configure through Dashboard for ${featureName} feature to function")
            return false;
        }

        return true
    }

    String getPublicUrl() {
        SettingsResult publicUrlSetting = settingsService.getGlobalSetting(Settings.GLOBAL_PUBLIC_URL.settingName)
        if (!publicUrlSetting) {
            log.warn("Skill approval notifications are disabled since global setting [${Settings.GLOBAL_PUBLIC_URL}] is NOT set")
            return null
        }

        String publicUrl = publicUrlSetting.value
        if (!publicUrl.endsWith("/")){
            publicUrl += "/"
        }
        return publicUrl
    }
}
