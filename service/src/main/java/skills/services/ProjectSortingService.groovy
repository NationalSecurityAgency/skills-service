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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.request.model.UserProjectSettingsRequest
import skills.controller.result.model.SettingsResult
import skills.services.settings.SettingsDataAccessor
import skills.services.settings.SettingsService
import skills.storage.model.Setting
import skills.storage.model.auth.User
import skills.storage.repos.UserRepo

@Service
@Slf4j
class ProjectSortingService {

    enum Move { UP, DOWN }

    static final String PROJECT_SORT_GROUP = "project_sort_order"
    static final String PROJECT_SORT_SETTING = "project"

    @Autowired
    UserInfoService userInfoService

    @Autowired
    SettingsService settingsService

    @Autowired
    UserRepo userRepo

    @Autowired
    SettingsDataAccessor settingsDataAccessor

    @Autowired
    LockingService lockingService

    @Transactional(readOnly = true)
    Integer getProjectSortOrder(String projectId){
        UserInfo userInfo = userInfoService.getCurrentUser()

        return getProjectSortOrder(projectId, userInfo.username)
    }

    @Transactional(readOnly = true)
    Integer getProjectSortOrder(String projectId, String userId){

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
    Map<String,Integer> getUserProjectsOrder(){
        UserInfo userInfo = userInfoService.getCurrentUser()
        return getUserProjectsOrder(userInfo.username)
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
    void setProjectSortOrder(String projectId, Integer order, String userId){

        User user = userRepo.findByUserId(userId.toLowerCase())

        UserProjectSettingsRequest request = new UserProjectSettingsRequest()
        request.projectId = projectId
        request.value = order
        request.settingGroup = PROJECT_SORT_GROUP
        request.setting = PROJECT_SORT_SETTING

        settingsService.saveSetting(request, user)
    }

    /**
     * Changes moveMeProjectId to be above or below the adjacentProjectId's sort order based on the direction parameter.
     *
     * Note that these projectIds must be adjacent to one another to perform this operation
     *
     * @param moveMeProjectId
     * @param adjacentProjectId
     * @param direction
     */
    @Transactional
    void changeProjectOrder(String moveMeProjectId, Move direction){
        UserInfo userInfo = userInfoService.getCurrentUser()
        lockingService.lockUser(userInfo.username)

        List<Setting> sortOrder = settingsDataAccessor.getUserProjectSettingsForGroup(userInfo.username, PROJECT_SORT_GROUP)

        sortOrder.sort() { Setting one, Setting two -> one.value.toInteger() <=> two.value.toInteger() }

        int idx = sortOrder.findIndexOf { it.projectId == moveMeProjectId }

        assert idx > -1, "failed to find sort setting for projectId [${moveMeProjectId}] for user ${userInfo.username}"

        Setting moveMe = sortOrder.get(idx)
        Setting swapWith

        if(direction == Move.UP){
            assert idx-1 >= 0
            swapWith = sortOrder.get(idx-1)
            assert swapWith.projectId != moveMeProjectId
        }else{
            assert idx+1 < sortOrder.size()
            swapWith = sortOrder.get(idx+1)
            assert swapWith.projectId != moveMeProjectId
        }

        Integer swapWithOrder = swapWith.value.toInteger()
        Integer moveMeOrder = moveMe.value.toInteger()

        moveMe.value = swapWithOrder
        swapWith.value = moveMeOrder

        settingsDataAccessor.saveAll([moveMe, swapWith])
    }

}
