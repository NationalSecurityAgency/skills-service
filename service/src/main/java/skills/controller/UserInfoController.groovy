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
package skills.controller

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import skills.auth.*
import skills.auth.pki.PkiUserLookup
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.SuggestRequest
import skills.controller.request.model.UserSettingsRequest
import skills.controller.result.model.RequestResult
import skills.controller.result.model.SettingsResult
import skills.controller.result.model.UserInfoRes
import skills.services.AccessSettingsStorageService
import skills.services.UserAdminService
import skills.services.settings.SettingsService
import skills.services.settings.listeners.ValidationRes
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserRepo

@RestController
@RequestMapping("/app")
@Slf4j
@skills.profile.EnableCallStackProf
class UserInfoController {

    static final String USER_PREFS_GROUP = 'user.prefs'
    static final String HOME_PAGE_PREF = 'home_page'

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserAuthService userAuthService

    @Autowired
    UserRepo userRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Value('#{securityConfig.authMode}}')
    AuthMode authMode = AuthMode.DEFAULT_AUTH_MODE

    @Autowired
    UserAdminService userAdminService

    @Autowired(required = false)
    PkiUserLookup pkiUserLookup

    @Autowired
    SettingsService settingsService

    @RequestMapping(value = "/userInfo", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CrossOrigin(allowCredentials = 'true')
    def getUserInfo() {
        def res = 'null'
        UserInfo currentUser = loadCurrentUser()
        if (currentUser) {
            res = new UserInfoRes(currentUser)
            res.landingPage = settingsService.getUserSetting(currentUser.username, HOME_PAGE_PREF, USER_PREFS_GROUP)?.value
        }
        return res
    }

    @RequestMapping(value = "/userInfo", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult updateUserInfo(@RequestBody UserInfoRes userInfoReq) {
        UserInfo currentUser = loadCurrentUser(true)
        if (currentUser) {
            if (userInfoReq.first) {
                currentUser.firstName = userInfoReq.first
            }
            if (userInfoReq.last) {
                currentUser.lastName = userInfoReq.last
            }
            currentUser.nickname = userInfoReq.nickname
            userAuthService.createOrUpdateUser(currentUser)
        }
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/userInfo/settings", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SettingsResult> getUserSettings() {
        UserInfo currentUser = loadCurrentUser(true)
        return settingsService.getUserSettingsForGroup(currentUser.username, USER_PREFS_GROUP)
    }

    @RequestMapping(value = "/userInfo/settings", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult saveUserSettings(@RequestBody List<UserSettingsRequest> values) {
        SkillsValidator.isNotNull(values, "Settings")

        List<UserSettingsRequest> toDelete = values.findAll { StringUtils.isBlank(it.value)}
        if (toDelete) {
            settingsService.deleteUserSettings(toDelete)
        }

        List<UserSettingsRequest> toSave = values.findAll { !StringUtils.isBlank(it.value)}
        if (toSave) {
            settingsService.saveSettings(toSave)
        }

        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/userInfo/settings/checkValidity", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    def checkSettingsValidity(@RequestBody List<UserSettingsRequest> values) {
        SkillsValidator.isNotNull(values, "Settings")
        UserSettingsRequest invalid = values.find {it.settingGroup != USER_PREFS_GROUP }
        SkillsValidator.isTrue(invalid == null, "settingGroup [${invalid?.settingGroup}] does not match expected group [${USER_PREFS_GROUP}]")
        ValidationRes validationRes = settingsService.isValid(values)
        return [
                success: true,
                valid: validationRes.isValid,
                explanation: validationRes.explanation
        ]
    }

    @RequestMapping(value = "/userInfo/hasRole/{role}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    boolean hasRole(@PathVariable("role") String role) {
        role = role.toLowerCase()
        UserInfo currentUser = userInfoService.getCurrentUser()
        return currentUser.authorities.find { it.authority.toLowerCase() == role }
    }

    @RequestMapping(value = "/userInfo/hasAnyRole/{roles}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    boolean hasAnyRole(@PathVariable("roles") List<String> roles) {
        boolean foundRole = false
        roleCheckLoop:
        for (String role : roles) {
            if (hasRole(role)) {
                foundRole = true
                break roleCheckLoop
            }
        }
        return foundRole
    }

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @RequestMapping(value = "/users/suggestDashboardUsers", method = RequestMethod.POST, produces = "application/json")
    List<UserInfoRes> suggestExistingDashboardUsers(@RequestBody SuggestRequest suggestRequest) {
        return userAdminService.suggestDashboardUsers(suggestRequest.suggestQuery, suggestRequest.includeSelf)
    }

    @RequestMapping(value = "/users/validExistingDashboardUserId/{userId}", method = RequestMethod.GET, produces = "application/json")
    Boolean isValidExistingDashboardUserId(@PathVariable("userId") String userId) {
        return userRepo.findByUserId(userId?.toLowerCase()) != null
    }

    @RequestMapping(value = "/users/projects/{projectId}/suggestClientUsers", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    List<UserInfoRes> suggestExistingClientUsersForProject(@PathVariable("projectId") String projectId, @RequestBody SuggestRequest suggestRequest) {
        return userAdminService.suggestUsersForProject(projectId, suggestRequest.suggestQuery, PageRequest.of(0, 5)).collect { new UserInfoRes(userId: it) }
    }

    @RequestMapping(value = "/users/suggestClientUsers/", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    List<UserInfoRes> suggestExistingClientUsers(@RequestBody SuggestRequest suggestRequest) {
        return userAdminService.suggestUsers(suggestRequest.suggestQuery, PageRequest.of(0, 5)).collect { new UserInfoRes(userId: it) }
    }

    @RequestMapping(value = "/users/projects/{projectId}/validExistingClientUserId/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    Boolean isValidExistingClientUserIdForProject(@PathVariable("projectId") String projectId, @PathVariable("userId") String userId) {
        return userAdminService.isValidExistingUserIdForProject(projectId, userId?.toLowerCase())
    }

    @RequestMapping(value = "/users/validExistingClientUserId/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    Boolean isValidExistingClientUserId(@PathVariable("userId") String userId) {
        return userAdminService.isValidExistingUserId(userId?.toLowerCase())
    }

    @RequestMapping(value = "/users/suggestPkiUsers", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    List<UserInfoRes> suggestExistingPkiUsers(@RequestBody SuggestRequest suggestRequest, @RequestParam(value="userSuggestOption", required = false, defaultValue = '') String userSuggestOption ) {
        String query = suggestRequest.suggestQuery
        if (!query) {
            query = "a"
        }
        return pkiUserLookup?.suggestUsers(query, userSuggestOption)?.take(5)?.collect { new UserInfoRes(it) }
    }

    private UserInfo loadCurrentUser(boolean failIfNoCurrentUser=false) {
        UserInfo currentUser = userInfoService.getCurrentUser()
        if (currentUser) {
        } else if (failIfNoCurrentUser || authMode == AuthMode.PKI) {
            throw new SkillsAuthorizationException('No current user found')
        }
        return currentUser
    }

}

