package skills.services.settings

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.request.model.*
import skills.controller.result.model.SettingsResult
import skills.services.LockingService
import skills.services.settings.listeners.ValidationRes
import skills.storage.model.Setting
import skills.storage.model.auth.User
import skills.storage.repos.UserRepo
import skills.utils.Props

@Service
@Slf4j
class SettingsService {

    @Autowired
    SettingsDataAccessor settingsDataAccessor

    @Autowired
    UserRepo userRepo

    @Autowired
    LockingService lockingService

    @Autowired
    List<SettingChangedListener> listeners = [];

    @Transactional
    void saveSettings(List<SettingsRequest> request) {
        request.each {
            saveSetting(it)
        }
    }

    @Transactional
    SettingsResult saveSetting(SettingsRequest request) {
        lockTransaction(request)
        Setting setting = settingsDataAccessor.loadSetting(request)
        if (setting) {
            applyListeners(setting, request)
            Props.copy(request, setting)
            log.debug("Updating [{}]", setting)
        } else {
            setting = new Setting()
            setting.type = SettingTypeUtil.getType(request)
            Props.copy(request, setting)
            handlerUserSettingsRequest(request, setting)
            applyListeners(null, request)
        }

        settingsDataAccessor.save(setting)
        log.debug("saved [{}]", setting)

        return convertToRes(setting)
    }

    private void lockTransaction(SettingsRequest request) {
        if(request instanceof UserProjectSettingsRequest){
            lockingService.lockUser(request.userId)
        } else if (request instanceof UserSettingsRequest) {
            lockingService.lockUser(request.userId)
        } else if(request instanceof GlobalSettingsRequest){
            lockingService.lockGlobally()
        }else if(request instanceof ProjectSettingsRequest){
            lockingService.lockProject(request.projectId)
        } else{
            log.error("unable SettingRequest [${request.getClass()}]")
            throw new SkillException("Unrecognized Setting type")
        }
    }


    private void handlerUserSettingsRequest(SettingsRequest request, Setting setting) {
        if (request instanceof UserSettingsRequest || request instanceof UserProjectSettingsRequest) {
            User user = userRepo.findByUserIdIgnoreCase(request.userId)
            if (!user) {
                throw new SkillException("Failed to find user with id [${request.userId}]")
            }
            setting.userId = user.id
        }
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
        return convertToResList(settings)
    }

    @Transactional(readOnly = true)
    List<SettingsResult> getGlobalSettingsByGroup(String settingGroup){
        List<Setting> settings = settingsDataAccessor.getGlobalSettingsByGroup(settingGroup)
        return convertToResList(settings)
    }

    @Transactional(readOnly = true)
    SettingsResult getGlobalSetting(String setting, String settingGroup){
        Setting settingDB = settingsDataAccessor.getGlobalSetting(setting, settingGroup)
        return convertToRes(settingDB)
    }

    @Transactional()
    SettingsResult getGlobalSetting(String setting){
        Setting settingDB = settingsDataAccessor.getGlobalSetting(setting)
        return convertToRes(settingDB)
    }

    @Transactional(readOnly = true)
    SettingsResult getProjectSetting(String projectId, String setting, String settingGroup){
        Setting settingDB = settingsDataAccessor.getProjectSetting(projectId, setting, settingGroup)
        return convertToRes(settingDB)
    }

    @Transactional()
    SettingsResult getProjectSetting(String projectId, String setting){
        Setting settingDB = settingsDataAccessor.getProjectSetting(projectId, setting)
        return convertToRes(settingDB)
    }

    @Transactional(readOnly = true)
    SettingsResult getUserSetting(String userId, String projectId, String setting, String settingGroup){
        Setting settingDB = settingsDataAccessor.getUserProjectSetting(userId, projectId, setting, settingGroup)
        return convertToRes(settingDB)
    }

    @Transactional(readOnly = true)
    List<SettingsResult> getUserSettingsForGroup(String userId, String settingGroup){
        List<Setting> settings = settingsDataAccessor.getUserSettingsForGroup(userId, settingGroup)
        return convertToResList(settings)
    }

    @Transactional(readOnly = true)
    List<SettingsResult> getUserSettingsForGroup(User user, String settingGroup){
        List<Setting> settings = settingsDataAccessor.getUserSettingsForGroup(user, settingGroup)
        return convertToResList(settings)
    }

    @Transactional(readOnly = true)
    List<SettingsResult> getUserProjectSettingsForGroup(String userId, String settingGroup){
        List<Setting> settings = settingsDataAccessor.getUserProjectSettingsForGroup(userId, settingGroup)
        return convertToResList(settings)
    }

    /**
     * Private helper methods
     */
    private SettingsResult convertToRes(Setting setting){
        if (!setting) {
            return null
        }
        SettingsResult res = new SettingsResult()
        Props.copy(setting, res)
        return res
    }

    private List<SettingsResult> convertToResList(List<Setting> settings) {
        if (!settings) {
            return []
        }
        return settings?.collect { convertToRes(it) }
    }

    private void applyListeners(Setting previousValue, SettingsRequest incomingValue){
        listeners?.each{
            if(it.supports(incomingValue)){
                it.execute(previousValue, incomingValue)
            }
        }
    }

}
