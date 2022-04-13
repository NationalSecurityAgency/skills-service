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
package skills.services.settings

import skills.controller.result.model.SettingsResult
import skills.services.settings.SettingsService
import skills.storage.model.Setting
import skills.storage.model.auth.User
import skills.storage.repos.UserRepo
import spock.lang.Specification

class SettingsServiceSpec extends Specification {

    def "failsafe when settings for user's project sort order contain null entry in the list"() {
        UserRepo userRepo = Mock()
        userRepo.findByUserId(_) >> new User()

        SettingsDataAccessor settingsDataAccessor = Mock()
        settingsDataAccessor.getUserProjectSettingsForGroup(_, _) >> [
                new Setting(projectId: "one", value: 1),
                null,
                new Setting(projectId: "two", value: 2),
        ]
        SettingsService settingsService = new SettingsService(settingsDataAccessor: settingsDataAccessor, userRepo: userRepo)

        String userId = "user1"
        String settingGroup = "group"
        when:
        List<SettingsResult> res = settingsService.getUserProjectSettingsForGroup(userId, settingGroup)
        then:
        res.size() == 2
        res[0].value == "1"
        res[1].value == "2"
    }

    def "return settings"() {
        UserRepo userRepo = Mock()
        userRepo.findByUserId(_) >> new User()

        SettingsDataAccessor settingsDataAccessor = Mock()
        settingsDataAccessor.getUserProjectSettingsForGroup(_, _) >> [
                new Setting(projectId: "one", value: 1),
                new Setting(projectId: "two", value: 2),
        ]
        SettingsService settingsService = new SettingsService(settingsDataAccessor: settingsDataAccessor, userRepo: userRepo)

        String userId = "user1"
        String settingGroup = "group"
        when:
        List<SettingsResult> res = settingsService.getUserProjectSettingsForGroup(userId, settingGroup)
        then:
        res.size() == 2
        res[0].value == "1"
        res[1].value == "2"
    }

    def "handle empty settings list"() {
        UserRepo userRepo = Mock()
        userRepo.findByUserId(_) >> new User()

        SettingsDataAccessor settingsDataAccessor = Mock()
        settingsDataAccessor.getUserProjectSettingsForGroup(_, _) >> [
        ]
        SettingsService settingsService = new SettingsService(settingsDataAccessor: settingsDataAccessor, userRepo: userRepo)

        String userId = "user1"
        String settingGroup = "group"
        when:
        List<SettingsResult> res = settingsService.getUserProjectSettingsForGroup(userId, settingGroup)
        then:
        res.size() == 0
    }

    def "handle null settings list"() {
        UserRepo userRepo = Mock()
        userRepo.findByUserId(_) >> new User()

        SettingsDataAccessor settingsDataAccessor = Mock()
        settingsDataAccessor.getUserProjectSettingsForGroup(_, _) >> null
        SettingsService settingsService = new SettingsService(settingsDataAccessor: settingsDataAccessor, userRepo: userRepo)

        String userId = "user1"
        String settingGroup = "group"
        when:
        List<SettingsResult> res = settingsService.getUserProjectSettingsForGroup(userId, settingGroup)
        then:
        res.size() == 0
    }

}
