package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.request.model.UserProjectSettingsRequest
import skills.controller.request.model.UserSettingsRequest
import skills.controller.result.model.SettingsResult
import skills.services.settings.SettingsDataAccessor
import skills.services.settings.SettingsService
import skills.storage.model.Setting
import skills.storage.model.auth.User
import skills.storage.repos.UserRepo
import org.springframework.transaction.annotation.Transactional

@Service
@Slf4j
class SortingService {

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

    @Transactional(readOnly = true)
    Integer getProjectSortOrder(String projectId){
        UserInfo userInfo = userInfoService.getCurrentUser()

        SettingsResult setting = settingsService.getUserSetting(userInfo.username, projectId, PROJECT_SORT_SETTING, PROJECT_SORT_GROUP)
        if(!setting){
            log.debug("no user sort setting exists for user [{}] for project [{}]", userInfo.username, projectId)
            return null
        }

        return setting.value.toInteger()
    }

    private Integer getHighestSortForUserProjects(){
        UserInfo userInfo = userInfoService.getCurrentUser()
        List<SettingsResult> sortOrder = settingsService.getUserProjectSettingsForGroup(userInfo.username, PROJECT_SORT_GROUP)
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
    void setNewProjectDisplayOrder(String projectId){
        //check for existing sort order for this projectId, just to be sure
        assert getProjectSortOrder(projectId) == null
        Integer currentHighest = getHighestSortForUserProjects()

        setProjectSortOrder(projectId, currentHighest == null ? 0 : currentHighest+1)
    }


    @Transactional
    void setProjectSortOrder(String projectId, Integer order){
        UserInfo userInfo = userInfoService.getCurrentUser()

        UserProjectSettingsRequest request = new UserProjectSettingsRequest()
        request.userId = userInfo.username
        request.projectId = projectId
        request.value = order
        request.settingGroup = PROJECT_SORT_GROUP
        request.setting = PROJECT_SORT_SETTING

        settingsService.saveSetting(request)
    }

    /**
     * Changes moveMeProjectId to be above or below the adajcentProjectId's sort order based on the direction parameter.
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
        User user = userRepo.findByUserIdIgnoreCase(userInfo.username)
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

        userRepo.save(user)
    }

}
