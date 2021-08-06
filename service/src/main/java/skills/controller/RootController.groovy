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
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*
import skills.auth.AuthMode
import skills.auth.UserInfoService
import skills.auth.pki.PkiUserLookup
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.ContactUsersRequest
import skills.controller.request.model.GlobalSettingsRequest
import skills.controller.request.model.SuggestRequest
import skills.controller.result.model.ProjectResult
import skills.controller.result.model.RequestResult
import skills.controller.result.model.SettingsResult
import skills.controller.result.model.UserInfoRes
import skills.controller.result.model.UserRoleRes
import skills.profile.EnableCallStackProf
import skills.services.AccessSettingsStorageService
import skills.services.ContactUsersService
import skills.services.SystemSettingsService
import skills.services.admin.ProjAdminService
import skills.services.settings.SettingsService
import skills.settings.EmailConfigurationResult
import skills.settings.EmailConnectionInfo
import skills.settings.EmailSettingsService
import skills.settings.SystemSettings
import skills.storage.model.auth.RoleName

import javax.xml.bind.DatatypeConverter
import java.security.MessageDigest
import java.security.Principal

@RestController
@RequestMapping('/root')
@Slf4j
@EnableCallStackProf
class RootController {

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    EmailSettingsService emailSettingsService

    @Value('#{securityConfig.authMode}}')
    AuthMode authMode = AuthMode.DEFAULT_AUTH_MODE

    @Autowired(required = false)
    PkiUserLookup pkiUserLookup

    @Autowired
    UserDetailsService userDetailsService

    @Autowired
    SettingsService settingsService

    @Autowired
    SystemSettingsService systemSettingsService

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    UserInfoService userInfoService

    @Autowired
    ContactUsersService contactUsersService

    @GetMapping('/rootUsers')
    @ResponseBody
    List<UserRoleRes> getRootUsers() {
        return accessSettingsStorageService.getRootUsers()
    }

    @PostMapping('/users/')
    @ResponseBody
    List<UserInfoRes> getNonRootUsers(@RequestBody SuggestRequest suggestRequest,
                                      @RequestParam(required = false, value = "userSuggestOption") String userSuggestOption) {
        String query = suggestRequest.suggestQuery.toLowerCase()
        if (authMode == AuthMode.FORM) {
            boolean emptyQuery = StringUtils.isBlank(query)
            return accessSettingsStorageService.getNonRootUsers().findAll {
                !emptyQuery ? it.userId.toLowerCase().contains(query) : true
            }.collect {
                accessSettingsStorageService.loadUserInfo(it.userId)
            }.unique()
        } else {
            List<String> rootUsers = accessSettingsStorageService.rootUsers.collect { it.userId.toLowerCase() }
            return pkiUserLookup?.suggestUsers(query, userSuggestOption)?.findAll {
                !rootUsers.contains(it.username.toLowerCase())
            }?.unique()?.take(5)?.collect { new UserInfoRes(it) }
        }
    }

    @PostMapping('/users/without/role/{roleName}')
    @ResponseBody
    List<UserInfoRes> suggestUsersWithoutRole(@PathVariable("roleName") RoleName roleName,
                                              @RequestBody SuggestRequest suggestRequest,
                                              @RequestParam(required = false, value = "userSuggestOption") String userSuggestOption) {
       String query = suggestRequest.suggestQuery.toLowerCase()
        if (authMode == AuthMode.FORM) {
            boolean emptyQuery = StringUtils.isBlank(query)
            return accessSettingsStorageService.getUserRolesWithoutRole(roleName).findAll {
                !emptyQuery ? it.userId.toLowerCase().contains(query) : true
            }.collect {
                accessSettingsStorageService.loadUserInfo(it.userId)
            }.unique()
        } else {
            if (StringUtils.isBlank(query)) {
                query = "a"
            }
            List<String> usersWithRole = accessSettingsStorageService.getUserRolesWithRole(roleName).collect { it.userId.toLowerCase() }
            return pkiUserLookup?.suggestUsers(query, userSuggestOption)?.findAll {
                !usersWithRole.contains(it.username.toLowerCase())
            }?.unique()?.take(5)?.collect { new UserInfoRes(it) }
        }
    }


    @GetMapping('/isRoot')
    boolean isRoot(Principal principal) {
        return accessSettingsStorageService.isRoot(principal.name)
    }

    @PutMapping('/addRoot/{userKey}')
    RequestResult addRoot(@PathVariable('userKey') String userKey) {
        String userId = getUserId(userKey)
        accessSettingsStorageService.addRoot(userId)
        return new RequestResult(success: true)
    }

    @DeleteMapping('/deleteRoot/{userId}')
    void deleteRoot(@PathVariable('userId') String userId) {
        SkillsValidator.isTrue(accessSettingsStorageService.getRootAdminCount() > 1, 'At least one root user must exist at all times! Deleting another user will cause no root users to exist!')
        accessSettingsStorageService.deleteRoot(userId?.toLowerCase())
    }

    @GetMapping('/users/roles/{roleName}')
    @ResponseBody
    List<UserRoleRes> getUserRolesWithRole(@PathVariable("roleName") RoleName roleName) {
        return accessSettingsStorageService.getUserRolesWithRole(roleName)
    }

    @PutMapping('/users/{userKey}/roles/{roleName}')
    RequestResult addRole(@PathVariable('userKey') String userKey,
                          @PathVariable("roleName") RoleName roleName) {
        if (roleName == RoleName.ROLE_SUPER_DUPER_USER) {
            addRoot(userKey)
        } else {
            String userId = getUserId(userKey)
            accessSettingsStorageService.addUserRole(userId, null, roleName)
        }
        return new RequestResult(success: true)
    }

    @DeleteMapping('/users/{userId}/roles/{roleName}')
    void deleteRole(@PathVariable('userId') String userId,
                    @PathVariable("roleName") RoleName roleName) {
        userId = userId?.toLowerCase()
        if (roleName == RoleName.ROLE_SUPER_DUPER_USER) {
            deleteRoot(userId)
        } else {
            userId = getUserId(userId)
            accessSettingsStorageService.deleteUserRole(userId, null, roleName)
        }
    }

    @PostMapping('/testEmailSettings')
    boolean testEmailSettings(@RequestBody EmailConnectionInfo emailConnectionInfo) {
        return emailSettingsService.testConnection(emailConnectionInfo)
    }

    @PostMapping('/saveEmailSettings')
    RequestResult saveEmailSettings(@RequestBody EmailConnectionInfo emailConnectionInfo) {
        EmailConfigurationResult success = emailSettingsService.updateConnectionInfo(emailConnectionInfo)
        return new RequestResult(success: success?.configurationSuccessful, explanation: success?.explanation)
    }

    @GetMapping('/getEmailSettings')
    EmailConnectionInfo fetchEmailSettings(){
        emailSettingsService.fetchEmailSettings()
    }

    @PostMapping('/saveSystemSettings')
    RequestResult saveSystemSettings(@RequestBody SystemSettings settings){
        if (settings.userAgreement) {
            MessageDigest md = MessageDigest.getInstance("MD5")
            md.update(settings.userAgreement.getBytes())
            byte[] digest = md.digest()
            String agreementVersion = DatatypeConverter
                    .printHexBinary(digest).toUpperCase()
            settings.userAgreemmentVersion = agreementVersion
        }
        systemSettingsService.save(settings)
        return RequestResult.success()
    }

    @GetMapping('/getSystemSettings')
    SystemSettings getSystemSettings(){
        return systemSettingsService.get()
    }

    @RequestMapping(value = "/global/settings/{setting}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult saveGlobalSetting(@PathVariable("setting") String setting, @RequestBody GlobalSettingsRequest settingRequest) {
        SkillsValidator.isNotBlank(setting, "Setting Id")
        SkillsValidator.isTrue(setting == settingRequest.setting, "Setting Id must equal")

        settingsService.saveSetting(settingRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/global/settings", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult saveGlobalSettings(@RequestBody List<GlobalSettingsRequest> values){
        SkillsValidator.isNotNull(values, "Settings")

        List<GlobalSettingsRequest> toDelete = values.findAll { StringUtils.isBlank(it.value)}
        if (toDelete) {
            settingsService.deleteGlobalSettings(toDelete)
        }

        List<GlobalSettingsRequest> toSave = values.findAll { !StringUtils.isBlank(it.value)}
        if (toSave) {
            settingsService.saveSettings(toSave)
        }

        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/global/settings/{settingGroup}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SettingsResult> getGlobalSettings(@PathVariable("settingGroup") String settingsGroup) {
        SkillsValidator.isNotBlank(settingsGroup, "Settings Group")
        return settingsService.getGlobalSettingsByGroup(settingsGroup)
    }

    @GetMapping('/projects')
    List<ProjectResult> getAllProjects() {
        return projAdminService.getAllProjects()
    }

    @GetMapping('/searchProjects')
    List<ProjectResult> searchProjects(@RequestParam(required = true, name = "name") String nameSearch) {
        return projAdminService.searchByProjectName(nameSearch)
    }

    @PostMapping('/pin/{projectId}')
    RequestResult pinProject(@PathVariable("projectId") String projectId) {
        //create project order setting for this user
        projAdminService.pinProjectForRootUser(projectId)
        return RequestResult.success()
    }

    @DeleteMapping('/pin/{projectId}')
    RequestResult unpinProject(@PathVariable("projectId") String projectId) {
        //remove project order setting for this user
        projAdminService.unpinProjectForRootUser(projectId)
        return RequestResult.success()
    }

    @GetMapping('/users/countAllProjectAdmins')
    Long countProjectAdministrators() {
        Long res = contactUsersService.countAllProjectAdminsWithEmail()
        res == null ? 0 : res
    }

    @PostMapping('/users/contactAllProjectAdmins')
    RequestResult contactProjectAdministrators(@RequestBody ContactUsersRequest cur) {
        SkillsValidator.isNotBlank(cur?.emailSubject, "emailSubject")
        SkillsValidator.isNotBlank(cur?.emailBody, "emailBody")
        //intentionally ignore queryCriteria as that doesn't apply to this use case
        contactUsersService.contactAllProjectAdmins(cur.emailSubject, cur.emailBody)
        return RequestResult.success()
    }

    @RequestMapping(value="/users/previewEmail", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    RequestResult testEmail(@RequestBody ContactUsersRequest contactUsersRequest) {
        SkillsValidator.isNotBlank(contactUsersRequest?.emailSubject, "emailSubject")
        SkillsValidator.isNotBlank(contactUsersRequest?.emailBody, "emailBody")
        String userId = userInfoService.getCurrentUserId()
        contactUsersService.previewEmail(contactUsersRequest.emailSubject, contactUsersRequest.emailBody, userId)
        return RequestResult.success()
    }





    private String getUserId(String userKey) {
        // userKey will be the userId when in FORM authMode, or the DN when in PKI auth mode.
        // When in PKI auth mode, the userDetailsService implementation will create the user
        // account if the user is not already a portal user (PkiUserDetailsService).
        // In the case of FORM authMode, the userKey is the userId and the user is expected
        // to already have a portal user account in the database
        if (authMode == AuthMode.PKI) {
            try {
                return userDetailsService.loadUserByUsername(userKey).username
            } catch (UsernameNotFoundException e) {
                throw new SkillException("User [$userKey] does not exist")
            }
        } else {
            return userKey
        }
    }
}
