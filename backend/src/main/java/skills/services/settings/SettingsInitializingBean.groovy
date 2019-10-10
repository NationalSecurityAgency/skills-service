package skills.services.settings

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import skills.controller.request.model.GlobalSettingsRequest
import skills.controller.request.model.SettingsRequest

/**
 * Auto load setting from properties by using the following naming convention:
 * skills.store.settings.<group_name>.<setting>=<value>
 *
 *  for example:
 *      skills.store.settings.public_groupName1.settingId1=valuea
 *  would equate to setting:
 *      group=public_groupName1, setting=settingId1, value=valuea
 */
@Component
@Slf4j
class SettingsInitializingBean {

    @Autowired
    SettingsService settingsService

    @Autowired
    DefaultSettingsToInit settingsToInit

    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationEvent() {
        loadSettings()
    }

    private void loadSettings() {
        List<SettingsRequest> toSave = []
        settingsToInit.settings?.each {
            String groupName = it?.key
            if (groupName) {
                it?.value?.each { def setting ->
                    String prop = setting.key
                    String value = setting.value
                    toSave.add(new GlobalSettingsRequest(settingGroup: groupName, setting: prop, value: value))
                    log.info("Adding [settingGroup: $groupName, setting: $prop, value: $value]")
                }
            }
        }
        if (toSave) {
            settingsService.saveSettings(toSave)
        }
    }
}
