package skills.services.settings

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.services.settings.listeners.ValidationRes
import skills.storage.model.Setting
import skills.storage.repos.SettingRepo
import skills.utils.Props

@Service
@Slf4j
class SettingsService {

    public static final String SYSTEM_SETTINGS = "SYSTEM"

    @Autowired
    SettingRepo settingRepo

    @Autowired
    List<SettingChangedListener> listeners = [];

    @Transactional
    void saveSettings(List<skills.controller.request.model.SettingsRequest> request) {
        request.each {
            saveSetting(it)
        }
    }

    @Transactional
    skills.controller.result.model.SettingsResult saveSetting(skills.controller.request.model.SettingsRequest request) {
        Setting setting

        if (request.id) {
            Optional<Setting> existingSetting = settingRepo.findById(request.id)
            if (!existingSetting.present) {
                throw new skills.controller.exceptions.SkillException("Requested setting update id [${request.id}] doesn't exist.")
            }

            log.info("Updating with [{}]", request)
            setting = existingSetting.get()
            applyListeners(setting, request)

            Props.copy(request, setting)

            log.info("Updating [{}]", setting)

            setting = settingRepo.save(setting)
            log.info("Saved [{}]", setting)
        } else {
            setting = new Setting(projectId: request.projectId,
                    settingGroup: request.settingGroup,
                    setting: request.setting,
                    value: request.value)

            applyListeners(null, request)
            settingRepo.save(setting)
            log.info("saved [{}]", setting)
        }

        skills.controller.result.model.SettingsResult result = new skills.controller.result.model.SettingsResult()
        Props.copy(setting, result)
        return result
    }

    private void applyListeners(Setting previousValue, skills.controller.request.model.SettingsRequest incomingValue){
        listeners?.each{
            if(it.supports(incomingValue)){
                it.execute(previousValue, incomingValue)
            }
        }
    }

    @Transactional(readOnly = true)
    ValidationRes isValid(List<skills.controller.request.model.SettingsRequest> settings) {
        ValidationRes foundInvalid = settings.collect({ isValid(it) }).find({!it.isValid})
        return foundInvalid ?: new ValidationRes(isValid: true)
    }

    @Transactional(readOnly = true)
    ValidationRes isValid(skills.controller.request.model.SettingsRequest setting) {
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
    List<skills.controller.result.model.SettingsResult> loadSettingsForProject(String projectId){
        def result = []
        settingRepo.findAllByProjectId(projectId)?.each{
            skills.controller.result.model.SettingsResult res = new skills.controller.result.model.SettingsResult()
            Props.copy(it, res)
            result.add(res)
        }

        return result
    }

    @Transactional
    List<skills.controller.result.model.SettingsResult> loadSettingsByType(String projectId, String settingGroup){
        def result = []
        settingRepo.findAllByProjectIdAndSettingGroup(projectId, settingGroup)?.each{
            skills.controller.result.model.SettingsResult res = new skills.controller.result.model.SettingsResult()
            Props.copy(it, res)
            result.add(res)
        }

        return result
    }

    @Transactional(readOnly = true)
    skills.controller.result.model.SettingsResult getSetting(String projectId, String setting, String settingGroup){
        List<Setting> settings = settingRepo.findAllByProjectIdAndSettingGroupAndSetting(projectId, settingGroup, setting)
        if(!settings){
            return null
        }

        assert settings.size() == 1
        skills.controller.result.model.SettingsResult result = new skills.controller.result.model.SettingsResult()
        Props.copy(settings.first(), result)
        return result
    }

    @Transactional(readOnly = true)
    @Profile
    skills.controller.result.model.SettingsResult getSetting(String projectId, String setting) {
        return getSetting(projectId, setting, null)
    }

    @Transactional(readOnly = true)
    skills.controller.result.model.SettingsResult getGlobalSetting(String setting) {
        return getSetting(null, setting)
    }

    @Transactional
    void saveOrUpdateGroup(String projectId, String settingsGroup, Map<String, String> settingsMap) {
        settingsMap.each { String key, String value ->
            saveOrUpdateSettingInternal(projectId, settingsGroup, key, value)
        }
    }

    private skills.controller.result.model.SettingsResult saveOrUpdateSettingInternal(String projectId, String settingsGroup, String setting, String value) {
        skills.controller.result.model.SettingsResult result = getGlobalSetting(setting)
        skills.controller.request.model.SettingsRequest request = new skills.controller.request.model.SettingsRequest(
                projectId: projectId,
                settingGroup: settingsGroup,
                setting: setting,
                value: value
        )
        if (result?.id) {
            request.id = result.id
        }
        return saveSetting(request)
    }

    /**
     * Note that this method must only be called by users with the ADMIN role
     * @return
     */
    @Transactional
    List<skills.controller.result.model.SettingsResult> loadSystemSettings(){
        List<Setting> systemSettings = settingRepo.findAllBySettingGroup(SYSTEM_SETTINGS)
        List<skills.controller.result.model.SettingsResult> resultList = []
        systemSettings?.each{
            skills.controller.result.model.SettingsResult res = new skills.controller.result.model.SettingsResult()
            Props.copy(it, res)
            resultList.add(res)
        }

        return resultList
    }
}
