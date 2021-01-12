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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import skills.auth.AuthMode
import skills.auth.SkillsAuthorizationException
import skills.auth.UserAuthService
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.pki.PkiUserLookup
import skills.controller.request.model.SuggestRequest
import skills.controller.result.model.RequestResult
import skills.controller.result.model.UserInfoRes
import skills.services.AccessSettingsStorageService
import skills.services.UserAdminService
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserRepo

@RestController
@RequestMapping("/app")
@Slf4j
@skills.profile.EnableCallStackProf
class UserInfoController {

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


    @RequestMapping(value = "/userInfo", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CrossOrigin(allowCredentials = 'true')
    def getUserInfo() {
        def res = 'null'
        UserInfo currentUser = userInfoService.getCurrentUser()
        if (currentUser) {
            res = new UserInfoRes(currentUser)
        } else if (authMode == AuthMode.PKI) {
            throw new SkillsAuthorizationException('Unauthenticated user while using PKI Authorization Mode')
        }
        return res
    }

    @RequestMapping(value = "/userInfo", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult updateUserInfo(@RequestBody UserInfoRes userInfoReq) {
        UserInfo currentUser = userInfoService.getCurrentUser()
        if (currentUser) {
            if (userInfoReq.first) {
                currentUser.firstName = userInfoReq.first
            }
            if (userInfoReq.last) {
                currentUser.lastName = userInfoReq.last
            }
            currentUser.nickname = userInfoReq.nickname
            userAuthService.createOrUpdateUser(currentUser)
        } else if (authMode == AuthMode.PKI) {
            throw new SkillsAuthorizationException('Unauthenticated user while using PKI Authorization Mode')
        }
        return new RequestResult(success: true)
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

}

