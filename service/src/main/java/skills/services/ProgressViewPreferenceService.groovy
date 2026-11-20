/**
 * Copyright 2026 SkillTree
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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.auth.UserInfoService
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.UserSettingsRequest
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.storage.repos.UserRepo

@Service
class ProgressViewPreferenceService {
    static final String PREFERENCE_GROUP = 'user.prefs'
    static final List<String> ALLOWED_VALUES = ['project-default', 'compact', 'expanded'].asImmutable()

    @Autowired
    SettingsService settingsService

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserRepo userRepo

    void validate(List<UserSettingsRequest> requests) {
        List<UserSettingsRequest> preferences = requests.findAll { UserSettingsRequest request ->
            request.setting == Settings.USER_PROGRESS_VIEW.settingName
        }
        preferences.each { UserSettingsRequest request ->
            SkillsValidator.isTrue(request.settingGroup == PREFERENCE_GROUP,
                    "Progress view preference must use settingGroup [${PREFERENCE_GROUP}].")
            SkillsValidator.isTrue(request.value in ALLOWED_VALUES,
                    "Progress view preference must be one of [${ALLOWED_VALUES.join(', ')}].")
        }
    }

    Boolean getCompactViewPreference() {
        String userId = userInfoService.currentUserId
        // Anonymous and new embedded learners have no saved preference; reads never create users.
        if (!userId || !userRepo.existsByUserIdIgnoreCase(userId)) {
            return null
        }
        String value = settingsService.getUserSetting(userId, Settings.USER_PROGRESS_VIEW.settingName, PREFERENCE_GROUP, false)?.value
        return value == 'compact' ? true : value == 'expanded' ? false : null
    }
}
