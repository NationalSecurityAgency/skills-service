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
package skills.services

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.UserProjectSettingsRequest
import skills.controller.result.model.SettingsResult
import skills.services.admin.ProjAdminService
import skills.services.settings.SettingsDataAccessor
import skills.services.settings.SettingsService
import skills.storage.model.Setting
import skills.storage.model.auth.User
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SettingRepo
import skills.storage.repos.UserRepo

@Service
@Slf4j
class ProjectSortingService {

    enum Move {
        UP, DOWN
    }

    static final String PROJECT_SORT_GROUP = "project_sort_order"
    static final String PROJECT_SORT_SETTING = "project"

    @Autowired
    UserInfoService userInfoService

    @Autowired
    SettingsService settingsService

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    UserRepo userRepo

    @Autowired
    SettingsDataAccessor settingsDataAccessor

    @Autowired
    LockingService lockingService

    @Autowired
    SettingRepo settingRepo

    @Transactional(readOnly = true)
    Integer getProjectSortOrder(String projectId) {
        UserInfo userInfo = userInfoService.getCurrentUser()

        return getProjectSortOrder(projectId, userInfo.username)
    }

    @Transactional(readOnly = true)
    Integer getProjectSortOrder(String projectId, String userId) {

        SettingsResult setting = settingsService.getUserProjectSetting(userId, projectId, PROJECT_SORT_SETTING, PROJECT_SORT_GROUP)
        if(!setting){
            log.debug("no user sort setting exists for user [{}] for project [{}]", userId, projectId)
            return null
        }

        return setting.value.toInteger()
    }

    private Integer getHighestSortForUserProjects(String userId){
        List<SettingsResult> sortOrder = settingsService.getUserProjectSettingsForGroup(userId, PROJECT_SORT_GROUP)
        if (!sortOrder) {
            return 0
        }
        sortOrder?.sort() { SettingsResult one, SettingsResult two -> one.value.toInteger() <=> two.value.toInteger() }
        return sortOrder?.last()?.value?.toInteger()
    }

    @Transactional(readOnly = true)
    Map<String,Integer> getUserProjectsOrder(String userId){
        List<SettingsResult> sortOrder = settingsService.getUserProjectSettingsForGroup(userId, PROJECT_SORT_GROUP)
        if(!sortOrder){
            return [:]
        }
        sortOrder.sort() { SettingsResult one, SettingsResult two -> one.value.toInteger() <=> two.value.toInteger() }
        return sortOrder.collectEntries() { [it.projectId, it.value.toInteger()]}
    }

    @Transactional
    void setNewProjectDisplayOrder(String projectId, String userId){
        //check for existing sort order for this projectId, just to be sure
        if (getProjectSortOrder(projectId, userId) != null){
            return
        }
        Integer currentHighest = getHighestSortForUserProjects(userId)

        setProjectSortOrder(projectId, currentHighest == null ? 0 : currentHighest+1, userId)
    }

    @Transactional
    void deleteProjectDisplayOrder(String projectId, String userId) {
        settingsService.deleteUserProjectSetting(userId.toLowerCase(), PROJECT_SORT_GROUP, PROJECT_SORT_SETTING, projectId)
    }

    @Transactional
    void setProjectSortOrder(String projectId, Integer order, String userId){

        User user = userRepo.findByUserId(userId.toLowerCase())

        UserProjectSettingsRequest request = new UserProjectSettingsRequest()
        request.projectId = projectId
        request.value = order
        request.settingGroup = PROJECT_SORT_GROUP
        request.setting = PROJECT_SORT_SETTING

        settingsService.saveSetting(request, user)
    }


    @Transactional
    void updateDisplayOrderByUsingNewIndex(String userId, boolean isRootUser, String projectId, ActionPatchRequest patchRequest) {
        User user = userRepo.findByUserId(userId.toLowerCase())

        assert patchRequest.action == ActionPatchRequest.ActionType.NewDisplayOrderIndex
        SkillsValidator.isTrue(patchRequest.newDisplayOrderIndex >= 0, "[newDisplayOrderIndex] param must be >=0 but received [${patchRequest.newDisplayOrderIndex}]", projectId, null)

        List<String> expectedProjectIds = []
        if (isRootUser) {
            List<SettingsResult> pinnedProjectSettings = settingsService.getUserProjectSettingsForGroup(userId.toLowerCase(), ProjAdminService.rootUserPinnedProjectGroup)
            expectedProjectIds = pinnedProjectSettings.collect { it.projectId }
        } else {
            expectedProjectIds = projDefRepo.getProjectIdsByUser(userId.toLowerCase())
        }

        List<Setting> settings = settingsDataAccessor.getUserProjectSettingsForGroup(user.id, PROJECT_SORT_GROUP)
        List<Setting> settingsToDelete = settings.findAll { !expectedProjectIds.contains(it.projectId) }
        settings = settings.findAll { expectedProjectIds.contains(it.projectId) }
        Setting theItem = settings.find({ it.projectId == projectId })
        List<Setting> result = settings.findAll({ it.projectId != projectId }).sort({ it.value.toInteger() })

        int newIndex = Math.min(patchRequest.newDisplayOrderIndex, settings.size() - 1)
        result.add(newIndex, theItem)
        result.eachWithIndex { Setting entry, int i ->
            entry.value = i.toString()
        }

        settingsDataAccessor.saveAll(result)

        if (settingsToDelete) {
            settingsToDelete.each {
                log.warn("Removing the sort setting that does not have an associated project entry. This must have resulted from a bug when managing sort settings. [{}]", JsonOutput.toJson(it))
                settingRepo.delete(it)
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Updated display order {}", result.collect { "${it.projectId}=>${it.value}" })
        }
    }

}
