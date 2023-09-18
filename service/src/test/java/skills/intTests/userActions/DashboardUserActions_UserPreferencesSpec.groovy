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
package skills.intTests.userActions

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.storage.model.UserAttrs
import spock.lang.IgnoreIf

class DashboardUserActions_UserPreferencesSpec extends DefaultIntSpec {

    SkillsService rootService
    def setup() {
        rootService = createRootSkillService()
    }
    private String getDisplayName() {
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        return userAttrs.getUserIdForDisplay()
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "update user profile settings"() {
        def currentUser = skillsService.getCurrentUser()
        currentUser.first = "newFirst"
        currentUser.last = "newLast"
        currentUser.nickname = "newNickname"

        when:
        skillsService.updateUserInfo(currentUser)
        def res = rootService.getUserActionsForEverything()
        def updateAction = rootService.getUserActionAttributes(res.data[0].id)
        then:
        res.count == 1
        res.data[0].action == DashboardAction.Edit.toString()
        res.data[0].item == DashboardItem.UserPreference.toString()
        res.data[0].itemId == DashboardItem.UserPreference.toString()
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        !res.data[0].projectId
        !res.data[0].quizId

        updateAction.nickname == "newNickname"
        updateAction.firstName == "newFirst"
        updateAction.lastName == "newLast"
    }

    def "update user preferences"() {
        when:
        String setting = "mysetting1"
        skillsService.addOrUpdateUserSetting(setting, "one")
        def res = rootService.getUserActionsForEverything()
        def updateAction = rootService.getUserActionAttributes(res.data[0].id)
        then:
        res.count == 1
        res.data[0].action == DashboardAction.Edit.toString()
        res.data[0].item == DashboardItem.UserPreference.toString()
        res.data[0].itemId == DashboardItem.UserPreference.toString()
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        !res.data[0].projectId
        !res.data[0].quizId

        updateAction.setting == setting
        updateAction.value == "one"
        updateAction.settingGroup == "user.prefs"
    }
}
