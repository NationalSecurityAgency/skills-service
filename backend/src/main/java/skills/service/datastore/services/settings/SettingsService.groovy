package skills.service.datastore.services.settings

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.service.controller.exceptions.SkillException
import skills.service.controller.request.model.SettingsRequest
import skills.service.controller.result.model.SettingsResult
import skills.storage.model.Setting
import skills.storage.repos.SettingRepo
import skills.utils.Props

import java.security.Key

@Service
@Slf4j
class SettingsService {

    public static final String SYSTEM_SETTINGS = "SYSTEM"

    @Autowired
    SettingRepo settingRepo

    @Autowired
    List<SettingChangedListener> listeners = [];

    @Transactional
    public SettingsResult saveSetting(SettingsRequest request) {
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
            setting = new Setting(projectId: request.projectId,
                    settingGroup: request.settingGroup,
                    setting: request.setting,
                    value: request.value)

            applyListeners(null, request)
            settingRepo.save(setting)
            log.info("saved [{}]", setting)
        }

        SettingsResult result = new SettingsResult()
        Props.copy(setting, result)
        return result
    }

    private void applyListeners(Setting previousValue, SettingsRequest incomingValue){
        listeners?.each{
            if(it.supports(incomingValue)){
                it.execute(previousValue, incomingValue)
            }
        }
    }

    @Transactional
    List<SettingsResult> loadSettingsForProject(String projectId){
        def result = []
        settingRepo.findAllByProjectId(projectId)?.each{
            SettingsResult res = new SettingsResult()
            Props.copy(it, res)
            result.add(res)
        }

        return result
    }

    @Transactional
    List<SettingsResult> loadSettingsByType(String projectId, String settingGroup){
        def result = []
        settingRepo.findAllByProjectIdAndSettingGroup(projectId, settingGroup)?.each{
            SettingsResult res = new SettingsResult()
            Props.copy(it, res)
            result.add(res)
        }

        return result
    }

    @Transactional(readOnly = true)
    SettingsResult getSetting(String projectId, String setting){
        List<Setting> settings = settingRepo.findAllByProjectIdAndSetting(projectId, setting)
        if(!settings){
            return null
        }

        assert settings.size() == 1
        SettingsResult result = new SettingsResult()
        Props.copy(settings.first(), result)
        return result
    }

    @Transactional(readOnly = true)
    SettingsResult getGlobalSetting(String setting) {
        return getSetting(null, setting)
    }

    @Transactional
    void saveOrUpdateGroup(String projectId, String settingsGroup, Map<String, String> settingsMap) {
        settingsMap.each { String key, String value ->
            saveOrUpdateSettingInternal(projectId, settingsGroup, key, value)
        }
    }

    private SettingsResult saveOrUpdateSettingInternal(String projectId, String settingsGroup, String setting, String value) {
        SettingsResult result = getGlobalSetting(setting)
        SettingsRequest request = new SettingsRequest(
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
    List<SettingsResult> loadSystemSettings(){
        List<Setting> systemSettings = settingRepo.findAllBySettingGroup(SYSTEM_SETTINGS)
        List<SettingsResult> resultList = []
        systemSettings?.each{
            SettingsResult res = new SettingsResult()
            Props.copy(it, res)
            resultList.add(res)
        }

        return resultList
    }
}
