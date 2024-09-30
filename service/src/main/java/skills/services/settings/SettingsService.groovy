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

import callStack.profiler.Profile
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.PublicProps
import skills.auth.AuthMode
import skills.auth.SkillsAuthorizationException
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.PublicPropsBasedValidator
import skills.controller.exceptions.SkillException
import skills.controller.request.model.*
import skills.controller.result.model.SettingsResult
import skills.services.LockingService
import skills.services.admin.UserCommunityService
import skills.services.settings.listeners.ValidationRes
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.Setting
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.User
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserRepo
import skills.utils.Props

@Service
@Slf4j
class SettingsService {

    @Value('#{securityConfig.authMode}}')
    AuthMode authMode = AuthMode.DEFAULT_AUTH_MODE

    @Autowired
    UserInfoService userInfoService

    @Autowired
    SettingsDataAccessor settingsDataAccessor

    @Autowired
    UserRepo userRepo

    @Autowired
    LockingService lockingService

    @Autowired
    List<SettingChangedListener> listeners = [];

    @Autowired
    PublicPropsBasedValidator propsBasedValidator

    @Autowired
    UserCommunityService userCommunityService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Transactional
    void saveSettings(List<SettingsRequest> request, User user=null, boolean saveUserAction = false) {
        request.each {
            saveSetting(it, user, saveUserAction)
        }
    }

    @Transactional
    void deleteProjectSettings(String projectId, List<SettingsRequest> request) {
        request.each {
            deleteProjectSetting(projectId, it.setting)
        }
    }

    @Transactional
    void deleteUserSettings(List<UserSettingsRequest> request) {
        request.each {
            deleteUserProjectSetting(it.setting, getUserRefId(request))
        }
    }

    @Transactional
    void deleteUserProjectSetting(String userId, String settingGroup, String setting, String projectId = null) {
        Integer userRefId = getUserRefId(userId)
        settingsDataAccessor.deleteUserProjectSetting(userRefId, setting, settingGroup, projectId)
    }

    @Transactional
    SettingsResult saveSetting(SettingsRequest request, User user=null, boolean performSaveUserAction = false) {
        validateSettingRequest(request)
        Integer userRefId = user ? user.id : getUserRefId(request)
        String userId = user ? user.userId : loadCurrentUser(isUserSettingRequest(request))?.username
        lockTransaction(request, userId)
        Setting setting = settingsDataAccessor.loadSetting(request, userRefId)
        if (setting) {
            applyListeners(setting, request)
            Props.copy(request, setting)
            log.debug("Updating [{}]", setting)
        } else {
            setting = new Setting()
            setting.type = SettingTypeUtil.getType(request)
            Props.copy(request, setting)
            handlerUserSettingsRequest(request, setting, userRefId)
            applyListeners(null, request)
        }

        if (performSaveUserAction) {
            saveUserAction(setting)
        }
        settingsDataAccessor.save(setting)
        log.debug("saved [{}]", setting)

        return convertToRes(setting)
    }

    @Profile
    private saveUserAction(Setting setting) {
        Setting.SettingType settingType = setting.type

        Map actionAttributes = [
                value: setting.value,
                setting: setting.setting,
        ]

        if (setting.userRefId) {
            UserAttrs userAttrs = userAttrsRepo.findUserAttrsByUserTableRefId(setting.userRefId)
            if (userAttrs) {
                actionAttributes.userId = userAttrs.userIdForDisplay
            }
        }

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Create,
                item: DashboardItem.Settings,
                itemId: settingType,
                projectId: setting.projectId,
                actionAttributes: actionAttributes
        ))
    }


    private void validateSettingRequest(SettingsRequest request) {
        if (request instanceof ProjectSettingsRequest && request.setting.endsWith('.displayName')) {
            String fieldName = StringUtils.capitalize(StringUtils.split(request.setting, '.')[0])
            propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxCustomLabelLength, "${fieldName} Display Text", request.value)
        }
    }

    private void lockTransaction(SettingsRequest request, String userId) {
        if(request instanceof UserProjectSettingsRequest){
            lockingService.lockUser(userId)
        } else if (request instanceof UserSettingsRequest) {
            lockingService.lockUser(userId)
        } else if(request instanceof GlobalSettingsRequest){
            lockingService.lockGlobalSettings()
        }else if(request instanceof ProjectSettingsRequest){
            lockingService.lockProject(request.projectId)
        } else{
            log.error("unable SettingRequest [${request.getClass()}]")
            throw new SkillException("Unrecognized Setting type")
        }
    }

    private void handlerUserSettingsRequest(SettingsRequest request, Setting setting, Integer userRefId) {
        if (isUserSettingRequest(request)) {
            setting.userRefId = userRefId
        }
    }

    private Integer getUserRefId(SettingsRequest request) {
        Integer userRefId = null
        if (isUserSettingRequest(request)) {
            UserInfo currentUser = loadCurrentUser()
            userRefId = getUserRefId(currentUser?.username)
        }
        return userRefId
    }

    private boolean isUserSettingRequest(SettingsRequest request) {
        return request instanceof UserSettingsRequest || request instanceof UserProjectSettingsRequest
    }

    private Integer getUserRefId(String userId, boolean failIfNotFound = true) {
        User user = userRepo.findByUserId(userId.toLowerCase())
        if (failIfNotFound && !user) {
            throw new SkillException("Failed to find user with id [${userId.toLowerCase()}]")
        }
        return user?.id
    }

    private UserInfo loadCurrentUser(boolean failIfNoCurrentUser=true) {
        UserInfo currentUser = userInfoService.getCurrentUser()
        if (!currentUser && failIfNoCurrentUser) {
            throw new SkillsAuthorizationException('No current user found')
        }
        return currentUser
    }

    @Transactional(readOnly = true)
    ValidationRes isValid(List<SettingsRequest> settings) {
        ValidationRes foundInvalid = settings.collect({ isValid(it) }).find({!it.isValid})
        return foundInvalid ?: new ValidationRes(isValid: true)
    }

    @Transactional(readOnly = true)
    ValidationRes isValid(SettingsRequest setting) {
        ValidationRes res = new ValidationRes(isValid: true);
        listeners?.each {
            if (it.supports(setting)) {
                res = it.isValid(setting)
                if(!res.isValid){
                    return res;
                }
            }
        }
        return res
    }

    @Transactional(readOnly = true)
    List<SettingsResult> loadSettingsForProject(String projectId) {
        List<Setting> settings = settingsDataAccessor.getProjectSettings(projectId)
        List<SettingsResult> res = convertToResList(settings)

        UserInfo currentUser = userInfoService.getCurrentUser()
        List<String> usrRoles = currentUser.authorities.collect { it.authority.toUpperCase() }
        RoleName usersRole = usrRoles.contains(RoleName.ROLE_PROJECT_APPROVER.toString()) ? RoleName.ROLE_PROJECT_APPROVER  : RoleName.ROLE_PROJECT_ADMIN
        res.add(new SettingsResult(
                setting: Settings.USER_PROJECT_ROLE.settingName,
                projectId: projectId,
                value: usersRole.toString(),
                userId: currentUser.username?.toLowerCase()
        ))
        if (userCommunityService.isUserCommunityConfigured()) {
            boolean isUserCommunityProtectedProject = settings.find { it.setting == Settings.USER_COMMUNITY_ONLY_PROJECT.settingName}?.isEnabled()
            res.add(new SettingsResult(
                    setting: Settings.PROJECT_COMMUNITY_VALUE.settingName,
                    projectId: projectId,
                    value: userCommunityService.getCommunityNameBasedProjConfStatus(isUserCommunityProtectedProject),
                    userId: currentUser.username?.toLowerCase()
            ))
        }
        return res
    }

    @Transactional(readOnly = true)
    List<SettingsResult> getGlobalSettingsByGroup(String settingGroup){
        List<Setting> settings = settingsDataAccessor.getGlobalSettingsByGroup(settingGroup)
        return convertToResList(settings)
    }

    @Transactional(readOnly = true)
    SettingsResult getGlobalSetting(String setting, String settingGroup){
        Setting settingDB = settingsDataAccessor.getGlobalSetting(setting, settingGroup)
        if (settingDB != null) {
            return convertToRes(settingDB)
        } else {
            log.debug("Global Setting is null for setting [{}], settingGroup [{}], settingDB [{}]", setting, settingGroup, settingDB)
        }
    }

    @Transactional()
    SettingsResult getGlobalSetting(String setting){
        Setting settingDB = settingsDataAccessor.getGlobalSetting(setting)
        if (settingDB != null) {
            return convertToRes(settingDB)
        } else {
            log.debug("Global Setting is null for setting [{}], settingDB [{}]", setting, settingDB)
        }
    }

    @Transactional(readOnly = true)
    SettingsResult getProjectSetting(String projectId, String setting, String settingGroup){
        Setting settingDB = settingsDataAccessor.getProjectSetting(projectId, setting, settingGroup)
        return convertToRes(settingDB)
    }

    @Transactional()
    SettingsResult getProjectSetting(String projectId, String setting){
        Setting settingDB = settingsDataAccessor.getProjectSetting(projectId, setting)
        if (settingDB != null) {
            return convertToRes(settingDB)
        } else {
            log.debug("Project Setting is null for projectId [{}], setting [{}], settingDB [{}]", projectId, setting, settingDB)
        }
    }

    @Transactional(readOnly = true)
    List<SettingsResult> getProjectSettings(String projectId, List<String> settings) {
        List<Setting> s = settingsDataAccessor.getProjectSettings(projectId, settings)
        List<SettingsResult> res = []
        if (s) {
            s?.each {
                res << convertToRes(it)
            }
        } else {
            log.debug("No Project Settings found for projectId [{}], requested settings [{}]", projectId, settings)
        }
        return res
    }

    @Transactional()
    List<SettingsResult> getProjectSettingsForAllProjects(String setting){
        List<Setting> settings = settingsDataAccessor.getProjectSettingsForAllProjectsBySettings(setting)
        return convertToResList(settings)
    }

    @Transactional(readOnly = true)
    SettingsResult getUserProjectSetting(String userId, String projectId, String setting, String settingGroup){
        Setting settingDB = settingsDataAccessor.getUserProjectSetting(getUserRefId(userId), projectId, setting, settingGroup)
        if (settingDB != null) {
            return convertToRes(settingDB, userId)
        } else {
            log.debug("User Project Setting is null for userId [{}], projectId [{}], setting [{}], settingGroup [{}], settingDB [{}]", userId, projectId, setting, settingGroup, settingDB)
            return null
        }
    }

    @Transactional(readOnly = true)
    SettingsResult getUserSetting(String userId, String setting, String settingGroup, boolean failIfNotFound = true){
        Setting settingDB = settingsDataAccessor.getUserSetting(getUserRefId(userId, failIfNotFound), setting, settingGroup)
        if (settingDB != null) {
            return convertToRes(settingDB, userId)
        }
        log.debug("User Setting is null for userId [{}], setting [{}], settingGroup [{}], settingDB [{}]", userId, setting, settingGroup, settingDB)
        return null
    }

    @Transactional(readOnly = true)
    List<SettingsResult> getUserSettingsForGroup(String userId, String settingGroup){
        List<Setting> settings = settingsDataAccessor.getUserSettingsForGroup(getUserRefId(userId), settingGroup)
        return convertToResList(settings, userId)
    }

    @Transactional(readOnly = true)
    List<SettingsResult> getUserProjectSettingsForGroup(String userId, String settingGroup){
        List<Setting> settings = settingsDataAccessor.getUserProjectSettingsForGroup(getUserRefId(userId), settingGroup)
        return convertToResList(settings)
    }

    @Transactional(readOnly = true)
    List<SettingsResult> getUserProjectSettingsForAllProjectsForGroup(String userId, String settingGroup){
        List<Setting> settings = settingsDataAccessor.getUserProjectSettingsForAllProjectsBySettingsGroup(userId, settingGroup)
        return convertToResList(settings)
    }


    @Transactional(readOnly = true)
    List<SettingsResult> getRootUserSettingsForGroup(String settingGroup) {
        List<Setting> settings = settingsDataAccessor.getRootUserSettingsByGroup(settingGroup)
        return convertToResList(settings)
    }

    @Transactional()
    void deleteProjectSetting(String projectId, String setting) {
        settingsDataAccessor.deleteProjectSetting(projectId, setting)
    }

    @Transactional()
    void deleteUserProjectSetting(String setting, Integer userRefId) {
        settingsDataAccessor.deleteUserProjectSetting(setting, userRefId)
    }

    @Transactional()
    void deleteGlobalSetting(String setting) {
        settingsDataAccessor.deleteGlobalSetting(setting)
    }

    @Transactional
    void deleteGlobalSettings(List<GlobalSettingsRequest> request) {
        request.each {
            deleteGlobalSetting(it.setting)
        }
    }

    @Transactional()
    void deleteRootUserSetting(String setting, String value) {
        settingsDataAccessor.deleteRootUserSetting(setting, value)
    }

    /**
     * Private helper methods
     */
    private SettingsResult convertToRes(Setting setting, String userId=null){
        if (!setting) {
            log.error("Code thinks this setting object is null [{}], groovyTruth [{}], nullCheck [{}]", JsonOutput.toJson(setting), !setting, setting == null)
            return null
        }
        SettingsResult res = new SettingsResult()
        Props.copy(setting, res)
        res.userId = userId
        return res
    }

    private List<SettingsResult> convertToResList(List<Setting> settings, String userId=null) {
        if (!settings) {
            return []
        }

        List<SettingsResult> res = settings?.collect { convertToRes(it, userId) }
        // surprisingly nulls sporadically appear in the list above (although the data looks right)
        // remove any nulls and warn if nulls are found
        // once the issue is found this code can be removed but for now we need a failsafe
        Boolean hasNulls = res.findAll({ !it }).size() > 0
        if (hasNulls) {
            log.error("Found null values in the settings list for [{}] user. fromDB=[{}], converted=[{}]", userId, JsonOutput.toJson(settings), JsonOutput.toJson(res))
            res = res.findAll({ it })
        }

        return res
    }

    private void applyListeners(Setting previousValue, SettingsRequest incomingValue){
        listeners?.each{
            if(it.supports(incomingValue)){
                it.execute(previousValue, incomingValue)
            }
        }
    }

}
