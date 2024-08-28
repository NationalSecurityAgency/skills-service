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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*
import skills.UIConfigProperties
import skills.auth.AuthMode
import skills.auth.UserInfoService
import skills.auth.pki.PkiUserLookup
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.ContactUsersRequest
import skills.controller.request.model.GlobalSettingsRequest
import skills.controller.request.model.SuggestRequest
import skills.controller.request.model.UserTagRequest
import skills.controller.result.model.*
import skills.dbupgrade.ReportedSkillEventQueue
import skills.profile.EnableCallStackProf
import skills.services.AccessSettingsStorageService
import skills.services.ContactUsersService
import skills.services.CustomValidationResult
import skills.services.CustomValidator
import skills.services.FeatureService
import skills.services.SystemSettingsService
import skills.services.admin.ProjAdminService
import skills.services.settings.SettingsService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.settings.EmailConfigurationResult
import skills.settings.EmailConnectionInfo
import skills.settings.EmailSettingsService
import skills.settings.SystemSettings
import skills.storage.model.UserTag
import skills.storage.model.auth.RoleName
import skills.storage.repos.UserTagRepo
import skills.tasks.executors.ExpireUserAchievementsTaskExecutor

import javax.xml.bind.DatatypeConverter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.Principal

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

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

    @Autowired
    CustomValidator customValidator

    @Autowired
    UserTagRepo userTagRepo

    @Autowired
    FeatureService featureService

    @Autowired
    UIConfigProperties uiConfigProperties

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    ExpireUserAchievementsTaskExecutor expireUserAchievementsTaskExecutor

    @Autowired
    ReportedSkillEventQueue reportedSkillEventQueue

    @GetMapping('/rootUsers')
    @ResponseBody
    List<UserRoleRes> getRootUsers() {
        return accessSettingsStorageService.getRootUsers()
    }

    @PostMapping('/users')
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

    @PostMapping('/runSkillExpiration')
    void runSkillExpiration() {
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()
    }

    @PostMapping('/runReplayEventsAfterUpgrade')
    void runReplayEventsAfterUpgrade() {
        reportedSkillEventQueue.replayEvents()
    }

    @GetMapping('/isRoot')
    boolean isRoot(Principal principal) {
        return accessSettingsStorageService.isRoot(principal.name)
    }

    @PutMapping('/addRoot/{userKey}')
    RequestResult addRoot(@PathVariable('userKey') String userKey) {
        String userId = getUserId(userKey)
        accessSettingsStorageService.addRoot(userId)
        String publicUrl = featureService.getPublicUrl()

        def emailBody = "Congratulations! You've been just added as a Root Administrator for the [SkillTree Dashboard](${publicUrl}administrator).\n\n" +
                "The Root role is meant for administering the dashboard itself and not any specific project. Users with the Root role can view the Inception project." +
                "Users with the Root role can also assign Supervisor and Root roles to other dashboard users. Thank you for being part of the SkillTree Community!\n\n" +
                "Always yours,\n\n" +
                "-SkillTree Bot"
        contactUsersService.sendEmail("SkillTree - You've been added as root", emailBody, userId, null, uiConfigProperties.ui.defaultCommunityDescriptor)
        projAdminService.pinAllExistingProjectsWhereUserIsAdminExceptInception(userId)
        return new RequestResult(success: true)
    }

    @DeleteMapping('/deleteRoot/{userId}')
    void deleteRoot(@PathVariable('userId') String userId) {
        SkillsValidator.isTrue(accessSettingsStorageService.getRootAdminCount() > 1, 'At least one root user must exist at all times! Deleting another user will cause no root users to exist!')
        accessSettingsStorageService.deleteRoot(userId?.toLowerCase())
        projAdminService.unpinAllProjectsForRootUser(userId)
    }

    @GetMapping('/users/roles/{roleName}')
    @ResponseBody
    TableResult getUserRolesWithRole(@PathVariable("roleName") RoleName roleName,
                                     @RequestParam int limit,
                                     @RequestParam int page,
                                     @RequestParam String orderBy,
                                     @RequestParam Boolean ascending) {
        PageRequest pagingRequest = createPagingRequest(limit, page, orderBy, ascending)
        return accessSettingsStorageService.getUserRolesWithRole(roleName, pagingRequest)
    }

    @PutMapping('/users/{userKey}/roles/{roleName}')
    RequestResult addRole(@PathVariable('userKey') String userKey,
                          @PathVariable("roleName") RoleName roleName) {
        if (roleName == RoleName.ROLE_SUPER_DUPER_USER) {
            addRoot(userKey)
        } else {
            String userId = getUserId(userKey)
            accessSettingsStorageService.addUserRole(userId, null, roleName, true)
        }
        return new RequestResult(success: true)
    }

    @DeleteMapping('/users/{userId}/roles/{roleName}')
    void deleteRole(@PathVariable('userId') String userId,
                    @PathVariable("roleName") RoleName roleName) {
        userId = userId?.toLowerCase()
        if (roleName == RoleName.ROLE_SUPER_DUPER_USER) {
            deleteRoot(userId)
            projAdminService.unpinAllProjectsForRootUser(userId)
        } else {
            accessSettingsStorageService.deleteUserRole(userId, null, roleName, true)
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
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Create, item: DashboardItem.Settings,
                actionAttributes: settings,
                itemId: "SystemSettings",
        ))
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

        settingsService.saveSetting(settingRequest, null, true)
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
            settingsService.saveSettings(toSave, null, true)
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
        validateEmailBodyAndSubject(cur)
        //intentionally ignore queryCriteria as that doesn't apply to this use case
        contactUsersService.contactAllProjectAdmins(cur.emailSubject, cur.emailBody)
        return RequestResult.success()
    }

    @RequestMapping(value="/users/previewEmail", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    RequestResult testEmail(@RequestBody ContactUsersRequest contactUsersRequest) {
        validateEmailBodyAndSubject(contactUsersRequest)
        String userId = userInfoService.getCurrentUserId()
        contactUsersService.sendEmail(contactUsersRequest.emailSubject, contactUsersRequest.emailBody, userId)
        return RequestResult.success()
    }

    private void validateEmailBodyAndSubject(ContactUsersRequest contactUsersRequest) {
        SkillsValidator.isNotBlank(contactUsersRequest?.emailSubject, "emailSubject")
        SkillsValidator.isNotBlank(contactUsersRequest?.emailBody, "emailBody")
        CustomValidationResult customValidationResult = customValidator.validateEmailBodyAndSubject(contactUsersRequest)
        if (!customValidationResult.valid) {
            throw new SkillException(customValidationResult.msg)
        }
    }

    @RequestMapping(value="/users/{userId}/tags/{tagKey}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    RequestResult saveTag(@PathVariable("userId") String userId, @PathVariable("tagKey") String tagKey, @RequestBody UserTagRequest userTagRequest) {
        SkillsValidator.isNotEmpty(userTagRequest?.tags, "tags")

        List<UserTag> userTags = userTagRequest.tags.collect { new UserTag(userId: userId?.toLowerCase(), key: tagKey, value: it)}
        userTagRepo.saveAll(userTags)

        return RequestResult.success()
    }

    @PostMapping('/rebuildUserAndProjectPoints/{projectId}')
    RequestResult rebuildUserAndProjectPoints(@PathVariable("projectId") String projectId) {
        projAdminService.rebuildUserAndProjectPoints(projectId)
        return RequestResult.success()
    }


    @RequestMapping(value = "/dashboardActions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @CompileStatic
    TableResult getDashboardActions(@RequestParam int limit,
                                    @RequestParam int page,
                                    @RequestParam String orderBy,
                                    @RequestParam Boolean ascending,
                                    @RequestParam(required=false) String projectIdFilter,
                                    @RequestParam(required=false) String itemFilter,
                                    @RequestParam(required=false) String userFilter,
                                    @RequestParam(required=false) String quizFilter,
                                    @RequestParam(required=false) String itemIdFilter,
                                    @RequestParam(required=false) String actionFilter) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        return userActionsHistoryService.getUsersActions(pageRequest,
                null,
                null,
                projectIdFilter ? URLDecoder.decode(projectIdFilter, StandardCharsets.UTF_8) : null,
                itemFilter? DashboardItem.valueOf(itemFilter) : null,
                userFilter ? URLDecoder.decode(userFilter, StandardCharsets.UTF_8) : null,
                quizFilter ? URLDecoder.decode(quizFilter, StandardCharsets.UTF_8) : null,
                itemIdFilter ? URLDecoder.decode(itemIdFilter, StandardCharsets.UTF_8) : null,
                actionFilter ? DashboardAction.valueOf(actionFilter) : null)
    }

    @RequestMapping(value = "/dashboardActions/filterOptions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @CompileStatic
    DashboardUserActionsFilterOptions getActionFilterOptions() {
        return userActionsHistoryService.getUserActionsFilterOptions()
    }

    @RequestMapping(value = "/dashboardActions/{actionId}/attributes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @CompileStatic
    Map getDashboardActionAttributes(@PathVariable("actionId") Long actionId) {
        return userActionsHistoryService.getActionAttributes(actionId)
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

    private static PageRequest createPagingRequest(int limit, int page, String orderBy, Boolean ascending) {
        SkillsValidator.isTrue(limit <= 200, "Cannot ask for more than 200 items, provided=[${limit}]")
        SkillsValidator.isTrue(page >= 0, "Cannot provide negative page. provided =[${page}]")
        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)

        return pageRequest
    }
}
