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


import skills.auth.UserInfo
import skills.controller.request.model.UserSettingsRequest
import skills.services.settings.SettingsService
import skills.storage.repos.UserRepo
import spock.lang.Specification

class AccessSettingsStorageServiceSpec extends Specification {

    def "CreateAppUser"() {
        UserInfo userInfo = new UserInfo(
                firstName: 'Joe',
                lastName: 'Schmo',
                email: 'joe@schmo.com',
                username: 'jschmo',
                password: 'password',
        )

        UserInfoValidator userInfoValidator = Mock()
        UserAttrsService userAttrsService = Mock()
        UserRepo userRepo = Mock()
        SettingsService settingsService = Mock()
        String landingPage = 'home'
        AccessSettingsStorageService accessSettingsStorageService = new AccessSettingsStorageService(
                userInfoValidator: userInfoValidator,
                userAttrsService: userAttrsService,
                userRepository: userRepo,
                settingsService: settingsService,
                defaultLandingPage: landingPage,
        )
        accessSettingsStorageService.settingsService = settingsService

        when:
        AccessSettingsStorageService.UserAndUserAttrsHolder user = accessSettingsStorageService.createAppUser(userInfo, false)

        then:
        user
        1 * settingsService.saveSetting(new UserSettingsRequest(
                settingGroup: AccessSettingsStorageService.USER_PREFS_GROUP,
                setting: AccessSettingsStorageService.HOME_PAGE_PREF,
                value: landingPage
        ), _)
    }
}
