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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.exceptions.SkillException
import skills.controller.request.model.*
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.Setting
import skills.storage.model.Setting.SettingType
import skills.storage.model.auth.User
import skills.storage.repos.SettingRepo
import skills.storage.repos.UserRepo

/**
 * Abstract access to settings storage but utilizing Setting.SettingType to reliably differentiate between different setting types
 */
@Component
@Slf4j
@CompileStatic
class SettingsDataAccessor {

    @Autowired
    SettingRepo settingRepo

    @Autowired
    UserRepo userRepo

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    Setting getGlobalSetting(String setting, String settingGroup){
        settingRepo.findByTypeAndUserRefIdAndProjectIdIgnoreCaseAndSettingGroupAndSetting(Setting.SettingType.Global, null, null, settingGroup, setting)
    }

    Setting getGlobalSetting(String setting){
        settingRepo.findByTypeAndUserRefIdAndProjectIdIgnoreCaseAndSettingGroupAndSetting(Setting.SettingType.Global, null, null, null, setting)
    }

    Setting getProjectSetting(String projectId, String setting){
        settingRepo.findByTypeAndUserRefIdAndProjectIdIgnoreCaseAndSettingGroupAndSetting(Setting.SettingType.Project, null, projectId, null, setting)
    }

    List<Setting> getProjectSettings(String projectId, List<String> settings) {
        settingRepo.findByTypeAndUserRefIdAndProjectIdAndSettingGroupAndSettingIn(Setting.SettingType.Project, null, projectId, null, settings)
    }

    Setting getProjectSetting(String projectId, String setting, String settingGroup){
        settingRepo.findByTypeAndUserRefIdAndProjectIdIgnoreCaseAndSettingGroupAndSetting(Setting.SettingType.Project, null, projectId, settingGroup, setting)
    }

    Setting getUserProjectSetting(Integer userRefId, String projectId, String setting, String settingGroup){
        settingRepo.findByTypeAndUserRefIdAndProjectIdIgnoreCaseAndSettingGroupAndSetting(Setting.SettingType.UserProject, userRefId, projectId, settingGroup, setting)
    }

    List<Setting> getUserProjectSettingsForAllProjectsBySettingsGroup(String userId, String settingGroup){
        settingRepo.findUserSettingsForAllProjectsByUserIdAndAndGroupId(userId, settingGroup)
    }

    List<Setting> getProjectSettingsForAllProjectsBySettings(String setting){
        settingRepo.findAllByTypeAndSetting(Setting.SettingType.Project, setting)
    }

    List<Setting> getProjectSettingForAllProjectsInList(String setting, List<String> projectIds) {
        settingRepo.findSettingsInProjectList(setting, projectIds)
    }

    Setting getUserSetting(Integer userRefId, String setting, String settingGroup){
        settingRepo.findByTypeAndUserRefIdAndProjectIdIgnoreCaseAndSettingGroupAndSetting(Setting.SettingType.User, userRefId, null, settingGroup, setting)
    }

    List<Setting> getUserSettingsForGroup(User user, String settingGroup) {
        settingRepo.findAllByTypeAndUserRefIdAndSettingGroup(Setting.SettingType.User, user?.id, settingGroup)
    }

    List<Setting> getUserSettingsForGroup(Integer userRefId, String settingGroup) {
        settingRepo.findAllByTypeAndUserRefIdAndSettingGroup(Setting.SettingType.User, userRefId, settingGroup)
    }

    List<Setting> getUserProjectSettingsForGroup(String userId, String settingGroup) {
        User user = userId ? userRepo.findByUserId(userId?.toLowerCase()) : null
        settingRepo.findAllByTypeAndUserRefIdAndSettingGroup(Setting.SettingType.UserProject, user?.id, settingGroup)
    }

    List<Setting> getUserProjectSettingsForGroup(Integer userRefId, String settingGroup) {
        settingRepo.findAllByTypeAndUserRefIdAndSettingGroup(Setting.SettingType.UserProject, userRefId, settingGroup)
    }

    List<Setting> getProjectSettings(String projectId) {
        settingRepo.findAllByTypeAndProjectId(Setting.SettingType.Project, projectId)
    }

    List<Setting> getGlobalSettingsByGroup(String settingGroup){
        settingRepo.findAllByTypeAndSettingGroup(Setting.SettingType.Global, settingGroup)
    }

    List<Setting> getRootUserSettingsByGroup(String settingGroup) {
        settingRepo.findAllByTypeAndSettingGroup(Setting.SettingType.RootUser, settingGroup)
    }

    Setting getRootUserProjectSetting(String settingGroup, String setting, String projectId) {
        settingRepo.findAllByTypeAndSettingGroupAndSettingAndProjectId(Setting.SettingType.RootUser, settingGroup, setting, projectId)
    }

    String getUserSettingValue(String userId, String setting) {
        return settingRepo.findUserSettingValueByUserIdAndSettingAndProjectIdIsNull(userId, setting)
    }

    Setting getSkillSetting(Integer skillRefId, String setting, String settingGroup){
        settingRepo.findByTypeAndSkillRefIdAndSettingGroupAndSetting(SettingType.Skill, skillRefId, settingGroup, setting)
    }

    void save(Setting setting){
        settingRepo.save(setting)
    }

    void saveAll(Iterable<Setting> settings){
        settingRepo.saveAll(settings)
    }

    void deleteProjectSetting(String projectId, String setting) {
        SettingType type = Setting.SettingType.Project
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete,
                item: DashboardItem.Settings,
                itemId: type,
                projectId: projectId,
                actionAttributes: [
                        setting: setting
                ]
        ))
        settingRepo.deleteProjectSetting(projectId, setting)
    }

    void deleteUserProjectSetting(String setting, Integer userRefId) {
        settingRepo.deleteBySettingAndTypeAndUserRefId(setting, SettingType.User, userRefId)
    }

    void deleteUserProjectSetting(Integer userRefId, String setting, String settingGroup, String projectId=null) {
        settingRepo.deleteBySettingAndSettingGroupAndProjectIdAndTypeAndUserRefId(setting, settingGroup, projectId, SettingType.UserProject, userRefId)
    }

    void deleteGlobalSetting(String setting) {
        settingRepo.deleteGlobalSetting(setting)
    }

    void deleteRootUserSetting(String setting, String value) {
        settingRepo.deleteRootUserSetting(setting, value)
    }

    Setting loadSetting(SettingsRequest request, Integer userRefId=null){
        if(request instanceof UserProjectSettingsRequest){
            return getUserProjectSetting(userRefId, request.projectId, request.setting, request.settingGroup)
        } else if (request instanceof UserSettingsRequest) {
            return getUserSetting(userRefId, request.setting, request.settingGroup)
        } else if(request instanceof GlobalSettingsRequest){
            return getGlobalSetting(request.setting, request.settingGroup)
        } else if(request instanceof RootUserProjectSettingsRequest) {
            return getRootUserProjectSetting(request.settingGroup, request.setting, request.projectId)
        } else if(request instanceof ProjectSettingsRequest){
            return getProjectSetting(request.projectId, request.setting, request.settingGroup)
        } else if(request instanceof SkillSettingsRequest){
            return getSkillSetting(request.skillRefId, request.setting, request.settingGroup)
        } else {
            log.error("unable SettingRequest [${request.getClass()}]")
            throw new SkillException("Unrecognized Setting type")
        }
    }
}
