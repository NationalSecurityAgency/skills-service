/**
 * Copyright 2021 SkillTree
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
import skills.controller.result.model.SettingsResult
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.settings.SystemSettings


@Component
@Slf4j
class UserAgreementService {

    @Autowired
    SystemSettingsService systemSettingsService

    @Autowired
    SettingsService settingsService

    public UserAgreementResult getUserAgreementStatus(String userId) {
        UserAgreementResult uar = new UserAgreementResult()
        SystemSettings systemSettings = systemSettingsService.get()
        uar.currentVersion = systemSettings.userAgreemmentVersion
        uar.userAgreement = systemSettings.userAgreement
        if (systemSettings.userAgreement) {
            SettingsResult settingsResult = settingsService.getUserSetting(userId, Settings.USER_VIEWED_USER_AGREEMENT.settingName, "user")
            uar.lastViewedVersion = settingsResult?.value
        }
        return uar
    }

}
