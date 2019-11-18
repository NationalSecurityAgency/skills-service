package skills.services.settings

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.exceptions.SkillException
import skills.controller.request.model.GlobalSettingsRequest
import skills.controller.request.model.ProjectSettingsRequest
import skills.controller.request.model.SettingsRequest
import skills.controller.request.model.UserProjectSettingsRequest
import skills.controller.request.model.UserSettingsRequest
import skills.storage.model.Setting
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

    Setting getGlobalSetting(String setting, String settingGroup){
        settingRepo.findByTypeAndUserRefIdAndProjectIdAndSettingGroupAndSetting(Setting.SettingType.Global, null, null, settingGroup, setting)
    }

    Setting getGlobalSetting(String setting){
        settingRepo.findByTypeAndUserRefIdAndProjectIdAndSettingGroupAndSetting(Setting.SettingType.Global, null, null, null, setting)
    }

    Setting getProjectSetting(String projectId, String setting){
        settingRepo.findByTypeAndUserRefIdAndProjectIdAndSettingGroupAndSetting(Setting.SettingType.Project, null, projectId, null, setting)
    }

    Setting getProjectSetting(String projectId, String setting, String settingGroup){
        settingRepo.findByTypeAndUserRefIdAndProjectIdAndSettingGroupAndSetting(Setting.SettingType.Project, null, projectId, settingGroup, setting)
    }

    Setting getUserProjectSetting(String userId, String projectId, String setting, String settingGroup){
        User user = userId ? userRepo.findByUserIdIgnoreCase(userId) : null
        settingRepo.findByTypeAndUserRefIdAndProjectIdAndSettingGroupAndSetting(Setting.SettingType.User, user?.id, projectId, settingGroup, setting)
    }

    Setting getUserSetting(String userId, String setting, String settingGroup){
        User user = userId ? userRepo.findByUserIdIgnoreCase(userId) : null
        settingRepo.findByTypeAndUserRefIdAndProjectIdAndSettingGroupAndSetting(Setting.SettingType.User, user?.id, null, settingGroup, setting)
    }

    List<Setting> getUserSettingsForGroup(String userId, String settingGroup) {
        User user = userId ? userRepo.findByUserIdIgnoreCase(userId) : null
        return getUserSettingsForGroup(user, settingGroup)
    }

    List<Setting> getUserSettingsForGroup(User user, String settingGroup) {
        settingRepo.findAllByTypeAndUserRefIdAndSettingGroup(Setting.SettingType.User, user?.id, settingGroup)
    }

    List<Setting> getUserProjectSettingsForGroup(String userId, String settingGroup) {
        User user = userId ? userRepo.findByUserIdIgnoreCase(userId) : null
        settingRepo.findAllByTypeAndUserRefIdAndSettingGroup(Setting.SettingType.UserProject, user?.id, settingGroup)
    }

    List<Setting> getProjectSettings(String projectId) {
        settingRepo.findAllByTypeAndProjectId(Setting.SettingType.Project, projectId)
    }

    List<Setting> getGlobalSettingsByGroup(String settingGroup){
        settingRepo.findAllByTypeAndSettingGroup(Setting.SettingType.Global, settingGroup)
    }

    void save(Setting setting){
        settingRepo.save(setting)
    }

    Setting loadSetting(SettingsRequest request){
        if(request instanceof UserProjectSettingsRequest){
            return getUserProjectSetting(request.userId, request.projectId, request.setting, request.settingGroup)
        } else if (request instanceof UserSettingsRequest) {
            return getUserSetting(request.userId, request.setting, request.settingGroup)
        } else if(request instanceof GlobalSettingsRequest){
            return getGlobalSetting(request.setting, request.settingGroup)
        }else if(request instanceof ProjectSettingsRequest){
            return getProjectSetting(request.projectId, request.setting, request.settingGroup)
        } else{
            log.error("unable SettingRequest [${request.getClass()}]")
            throw new SkillException("Unrecognized Setting type")
        }
    }
}
