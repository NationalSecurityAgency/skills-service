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
import skills.intTests.utils.SkillsService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName

class DashboardUserActions_RootRoleSpec extends DefaultIntSpec {

    String  displayName
    SkillsService rootService
    def setup() {
        rootService = createRootSkillService()
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(rootService.userName)
        displayName = userAttrs.getUserIdForDisplay()
    }

    def "add/remove root user role"() {
        SkillsService otherUser = createService(getRandomUsers(1))

        when:
        rootService.addRootRole(otherUser.userName)
        rootService.removeRootRole(otherUser.userName)
        def res = rootService.getUserActionsForEverything()
        def createAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        String otherUserDisplay = userAttrsRepo.findByUserIdIgnoreCase(otherUser.userName).userIdForDisplay
        then:
        res.count == 2
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.UserRole.toString()
        res.data[0].itemId == otherUserDisplay
        res.data[0].userId == rootService.userName.toLowerCase()
        res.data[0].userIdForDisplay == displayName
        !res.data[0].projectId
        !res.data[0].quizId

        res.data[1].action == DashboardAction.Create.toString()
        res.data[1].item == DashboardItem.UserRole.toString()
        res.data[1].itemId == otherUserDisplay
        res.data[1].userId == rootService.userName.toLowerCase()
        res.data[1].userIdForDisplay == displayName
        !res.data[1].projectId
        !res.data[1].quizId

        createAction.userId == otherUserDisplay
        createAction.userRole == RoleName.ROLE_SUPER_DUPER_USER.toString()

        deleteAction.userId == otherUserDisplay
        deleteAction.userRole == RoleName.ROLE_SUPER_DUPER_USER.toString()
    }

    def "save email settings"() {
        when:
        rootService.saveEmailSettings("somehost", "smtp", 1026, false, true, "fakeuser", "fakepassword")
        def res = rootService.getUserActionsForEverything()
        def saveAction = rootService.getUserActionAttributes(res.data[0].id)
        then:
        res.count == 1
        res.data[0].action == DashboardAction.Create.toString()
        res.data[0].item == DashboardItem.Settings.toString()
        res.data[0].itemId == "EmailSettings"
        res.data[0].userId == rootService.userName.toLowerCase()
        res.data[0].userIdForDisplay == displayName
        !res.data[0].projectId
        !res.data[0].quizId

        saveAction.host == "somehost"
        saveAction.protocol == "smtp"
        saveAction.port == 1026
        saveAction.username == "fakeuser"
        !saveAction.password
    }

    def "save global settings"() {
        when:
        rootService.addOrUpdateGlobalSetting("globalKey", ["setting": "globalKey", "value": "val1"])
        def res = rootService.getUserActionsForEverything()
        def saveAction = rootService.getUserActionAttributes(res.data[0].id)
        then:
        res.count == 1
        res.data[0].action == DashboardAction.Create.toString()
        res.data[0].item == DashboardItem.Settings.toString()
        res.data[0].itemId == "Global"
        res.data[0].userId == rootService.userName.toLowerCase()
        res.data[0].userIdForDisplay == displayName
        !res.data[0].projectId
        !res.data[0].quizId

        saveAction.setting == "globalKey"
        saveAction.value == "val1"
    }

    def "save system settings"() {
        when:
        rootService.saveSystemSettings("PT1H", "<div>header</div>", "<div>footer</div>")
        def res = rootService.getUserActionsForEverything()
        def saveAction = rootService.getUserActionAttributes(res.data[0].id)
        then:
        res.count == 1
        res.data[0].action == DashboardAction.Create.toString()
        res.data[0].item == DashboardItem.Settings.toString()
        res.data[0].itemId == "SystemSettings"
        res.data[0].userId == rootService.userName.toLowerCase()
        res.data[0].userIdForDisplay == displayName
        !res.data[0].projectId
        !res.data[0].quizId

        saveAction.customHeader == "<div>header</div>"
        saveAction.customFooter == "<div>footer</div>"
        saveAction.resetTokenExpiration == "PT1H"
    }
}
