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
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.request.model.GlobalSettingsRequest
import skills.controller.result.model.SettingsResult
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.settings.SystemSettings

import java.time.Duration
import java.time.format.DateTimeParseException
import java.util.regex.Pattern

@Slf4j
@Component
class SystemSettingsService {

    private static final int MAX_SETTING_VALUE = 3000

    public static final String CUSTOMIZATION = 'customization'

    private static final Pattern SCRIPT = ~/.*<[^>]*script.*/


    @Autowired
    SettingsService settingsService

    SystemSettings get() {
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

        List<SettingsResult> headerFooter = settingsService.getGlobalSettingsByGroup(CUSTOMIZATION)
        headerFooter?.each {
            if (Settings.GLOBAL_CUSTOM_HEADER.settingName == it.setting) {
                settings.customHeader = it.value
            } else if (Settings.GLOBAL_CUSTOM_FOOTER.settingName == it.setting) {
                settings.customFooter = it.value
            }
        }

        return settings
    }

    @Transactional
    void save(SystemSettings settings) {
        List<GlobalSettingsRequest> toSave = []
        saveButRemoveIfEmpty(Settings.GLOBAL_PUBLIC_URL, settings.publicUrl)

        saveButRemoveIfEmpty(Settings.GLOBAL_FROM_EMAIL, settings.fromEmail)
        saveFooterButRemoveIfEmpty(Settings.GLOBAL_CUSTOM_HEADER, settings.customHeader, "Custom Header")
        saveFooterButRemoveIfEmpty(Settings.GLOBAL_CUSTOM_FOOTER, settings.customFooter, "Custom Footer")

        if (StringUtils.isNotBlank(settings.resetTokenExpiration)) {
            try {
                Duration.parse(settings.resetTokenExpiration);
            } catch (DateTimeParseException dtpe) {
                throw new SkillException("${settings.resetTokenExpiration} is not a valid duration");
            }
            toSave << new GlobalSettingsRequest(setting: Settings.GLOBAL_RESET_TOKEN_EXPIRATION.settingName, value: settings.resetTokenExpiration)
        } else {
            settingsService.deleteGlobalSetting(Settings.GLOBAL_RESET_TOKEN_EXPIRATION.settingName)
        }

        settingsService.saveSettings(toSave)
    }

    private void saveButRemoveIfEmpty(Settings setting, String value) {
        if (StringUtils.isNotBlank(value)) {
            settingsService.saveSetting(new GlobalSettingsRequest(setting: setting.settingName, value: value))
        } else {
            settingsService.deleteGlobalSetting(setting.settingName)
        }
    }

    private void saveFooterButRemoveIfEmpty(Settings setting, String value, String settingNameForErrors) {
        if (StringUtils.isNotBlank(value)) {
            if (value ==~ SCRIPT) {
                throw new SkillException("Script tags are not allowed in ${settingNameForErrors}")
            }
            if (MAX_SETTING_VALUE < value?.length()) {
                throw new SkillException("${settingNameForErrors} may not be longer than [${MAX_SETTING_VALUE}]")
            }
            value = StringUtils.defaultString(value)
            settingsService.saveSetting(new GlobalSettingsRequest(setting: setting.settingName, value: value, settingGroup: CUSTOMIZATION))
        } else {
            settingsService.deleteGlobalSetting(setting.settingName)
        }
    }

}
