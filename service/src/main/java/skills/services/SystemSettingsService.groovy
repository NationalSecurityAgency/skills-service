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
import skills.controller.exceptions.SkillException
import skills.controller.request.model.GlobalSettingsRequest
import skills.controller.result.model.SettingsResult
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.settings.SystemSettings

import java.time.Duration
import java.time.format.DateTimeParseException

@Slf4j
@Component
class SystemSettingsService {

    @Autowired
    SettingsService settingsService

    SystemSettings get(){
        SystemSettings settings = new SystemSettings()
        SettingsResult result = settingsService.getGlobalSetting(Settings.GLOBAL_PUBLIC_URL.settingName)
        if (result) {
            settings.publicUrl = result.value
        }
        result = settingsService.getGlobalSetting(Settings.GLOBAL_RESET_TOKEN_EXPIRATION.settingName)
        if (result) {
            settings.resetTokenExpiration = result.value
        }
        result = settingsService.getGlobalSetting(Settings.GLOBAL_FROM_EMAIL.settingName)
        if (result) {
            settings.fromEmail = result.value
        }

        return settings
    }

    void save(SystemSettings settings) {
        List<GlobalSettingsRequest> toSave = []
        toSave << new GlobalSettingsRequest(setting: Settings.GLOBAL_PUBLIC_URL.settingName, value: settings.publicUrl)

        if (settings.resetTokenExpiration) {
            try {
                Duration.parse(settings.resetTokenExpiration);
            } catch (DateTimeParseException dtpe) {
                throw new SkillException("[${settings.resetTokenExpiration} is not a valid duration");
            }
            toSave << new GlobalSettingsRequest(setting: Settings.GLOBAL_RESET_TOKEN_EXPIRATION.settingName, value: settings.resetTokenExpiration)
        }
        if (settings.fromEmail) {
            toSave << new GlobalSettingsRequest(setting: Settings.GLOBAL_FROM_EMAIL.settingName, value: settings.fromEmail)
        }

        settingsService.saveSettings(toSave)
    }

}
