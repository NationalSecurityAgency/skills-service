package skills.services.settings

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.request.model.GlobalSettingsRequest
import skills.controller.request.model.ProjectSettingsRequest
import skills.controller.request.model.SettingsRequest
import skills.controller.request.model.UserSettingsRequest
import skills.controller.result.model.SettingsResult
import skills.services.settings.listeners.ValidationRes
import skills.storage.model.Setting
import skills.storage.model.Setting.SettingType
import skills.storage.model.auth.User
import skills.storage.repos.SettingRepo
import skills.storage.repos.UserRepo
import skills.utils.Props

@Service
@Slf4j
class SettingsService {
    public static final String SYSTEM_SETTINGS = "SYSTEM"

    @Autowired
    SettingRepo settingRepo

    @Autowired
    List<SettingChangedListener> listeners = [];

    @Autowired
    UserRepo userRepo

    @Transactional
    void saveSettings(List<SettingsRequest> request) {
        request.each {
            saveSetting(it)
        }
    }

    @Transactional
    SettingsResult saveSetting(SettingsRequest request) {

        if(request instanceof UserSettingsRequest){
            return saveUserSetting(request)
        }

        Setting setting

        if (request.id) {
            Optional<Setting> existingSetting = settingRepo.findById(request.id)
            if (!existingSetting.present) {
                throw new SkillException("Requested setting update id [${request.id}] doesn't exist.")
            }

            log.info("Updating with [{}]", request)
            setting = existingSetting.get()
            applyListeners(setting, request)

            Props.copy(request, setting)

            log.info("Updating [{}]", setting)

            setting = settingRepo.save(setting)
            log.info("Saved [{}]", setting)
        } else {
            setting = new Setting()
            Props.copy(request, setting)
            setting.type = identifySettingType(request)

            applyListeners(null, request)
            settingRepo.save(setting)
            log.info("saved [{}]", setting)
        }

        SettingsResult result = new SettingsResult()
        Props.copy(setting, result)
        return result
    }

    private SettingsResult saveUserSetting(UserSettingsRequest userSetting){
        User user = userRepo.findByUserIdIgnoreCase(userSetting.userId)
        Setting setting
        if (userSetting.id) {
            Setting existingSetting = user.userProps.find { it.id == userSetting.id }
            applyListeners(existingSetting, userSetting)
            Props.copy(userSetting, existingSetting, "userId")
            setting = existingSetting
            userRepo.save(user)
        } else {
            applyListeners(null, userSetting)
            Setting newSetting = new Setting(type: SettingType.User)
            Props.copy(userSetting, newSetting, "userId")
            user.userProps.add(newSetting)
            user = userRepo.save(user)
            setting = user.userProps.find() { it.settingGroup == userSetting.settingGroup && it.setting == userSetting.setting && it.value == userSetting.value}
        }

        SettingsResult result = new SettingsResult()
        Props.copy(setting, result)
        return result
    }

    private static SettingType identifySettingType(SettingsRequest request){
        if(request instanceof GlobalSettingsRequest){
            return SettingType.Global
        }else if(request instanceof ProjectSettingsRequest){
            return SettingType.Project
        }else if(request instanceof UserSettingsRequest){
            return SettingType.User
        }else{
            log.error("unable SettingRequest [${request.getClass()}]")
            throw new SkillException("unrecognized Setting type")
        }
    }

    private void applyListeners(Setting previousValue, SettingsRequest incomingValue){
        listeners?.each{
            if(it.supports(incomingValue)){
                it.execute(previousValue, incomingValue)
            }
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

    @Transactional
    List<SettingsResult> loadSettingsForProject(String projectId){
        def result = []
        settingRepo.findAllByTypeAndProjectId(SettingType.Project, projectId)?.each{
            SettingsResult res = new SettingsResult()
            Props.copy(it, res)
            result.add(res)
        }

        return result
    }

    @Transactional
    List<SettingsResult> loadGlobalSettingsByType(String settingGroup){
        def result = []
        settingRepo.findAllByTypeAndSettingGroup(SettingType.Global, settingGroup)?.each{
            SettingsResult res = new SettingsResult()
            Props.copy(it, res)
            result.add(res)
        }

        return result
    }

    @Transactional(readOnly = true)
    SettingsResult getGlobalSetting(String setting, String settingGroup){
        List<Setting> settings = settingRepo.findAllByTypeAndSettingGroupAndSetting(SettingType.Global, settingGroup, setting)
        return convertToSingleResult(settings)
    }

    @Transactional(readOnly = true)
    SettingsResult getProjectSetting(String projectId, String setting, String settingGroup){
        List<Setting> settings = settingRepo.findAllByTypeAndProjectIdAndSettingGroupAndSetting(SettingType.Project, projectId, settingGroup, setting)
        return convertToSingleResult(settings)
    }

    @Transactional(readOnly = true)
    SettingsResult getUserSetting(String userId, String projectId, String setting, String settingGroup){
        User user = userRepo.findByUserIdIgnoreCase(userId)
        return convertToSingleResult(user.userProps.findAll { it.projectId == projectId && it.settingGroup == settingGroup && it.setting == setting })
    }

    @Transactional(readOnly = true)
    List<SettingsResult> getUserSettingsByType(String userId, String settingGroup){
        User user = userRepo.findByUserIdIgnoreCase(userId)
        return convert(user.userProps.findAll { it.settingGroup == settingGroup })
    }

    private static SettingsResult convertToSingleResult(List<Setting> settings){
        if(!settings){
            return null
        }

        assert settings.size() == 1
        SettingsResult result = new SettingsResult()
        Props.copy(settings.first(), result)
        return result
    }

    private static List<SettingsResult> convert(List<Setting> settings){
        if(!settings){
            return null
        }

        def res = []
        settings.each {
            SettingsResult result = new SettingsResult()
            Props.copy(it, result)
            res.add(result)
        }
        return res
    }

    @Transactional(readOnly = true)
    @Profile
    SettingsResult getProjectSetting(String projectId, String setting) {
        return getProjectSetting(projectId, setting, null)
    }

    @Transactional(readOnly = true)
    SettingsResult getGlobalSetting(String setting) {
        return getGlobalSetting(setting, null)
    }

    @Transactional
    void saveOrUpdateGlobalGroup(String settingsGroup, Map<String, String> settingsMap) {
        settingsMap.each { String setting, String value ->
            saveOrUpdateGlobalSetting(SettingType.Global, null, null, settingsGroup, setting, value)
        }
    }


    private SettingsResult saveOrUpdateGlobalSetting(SettingType settingType, String projectId, String userId, String settingsGroup, String setting, String value) {
        SettingsResult result = null

        SettingsRequest request = instantiateBasedOnType(settingType)
        if(request instanceof ProjectSettingsRequest){
            request.projectId = projectId
            result = getProjectSetting(projectId, setting)
        } else if (request instanceof UserSettingsRequest){
            request.projectId = projectId
            request.userId = userId
            return saveUserSetting(request)
        } else {
            result = getGlobalSetting(setting)
        }

        request.settingGroup = settingsGroup
        request.setting = setting
        request.value = value

        if (result?.id) {
            request.id = result.id
        }
        return saveSetting(request)
    }

    private static SettingsRequest instantiateBasedOnType(SettingType settingType){
        if(settingType == SettingType.Global){
            return new GlobalSettingsRequest()
        }else if(settingType == SettingType.Project){
            return new ProjectSettingsRequest()
        }else if(settingType == SettingType.User){
            return new UserSettingsRequest()
        }else{
            log.error("unknown SettingType [${settingType}], unable to map to SettingsRequest implementation")
            throw new SkillException("Unrecognized Setting Type")
        }
    }

    /**
     * Note that this method must only be called by users with the ADMIN role
     * @return
     */
    @Transactional
    List<SettingsResult> loadSystemSettings(){
        List<Setting> systemSettings = settingRepo.findAllByTypeAndSettingGroup(SettingType.Global, SYSTEM_SETTINGS)
        List<SettingsResult> resultList = []
        systemSettings?.each{
            SettingsResult res = new SettingsResult()
            Props.copy(it, res)
            resultList.add(res)
        }

        return resultList
    }
}
