package skills.controller.result.model

import groovy.transform.Canonical

@Canonical
class SettingsResult {
    Integer id
    //nullable
    String settingGroup
    //nullable
    String projectId
    //non-null
    String setting
    //non-null
    String value
    //nullable
    String userId

    boolean isEnabled(){
        return Boolean.valueOf(value) || value.toLowerCase() == "enabled" || value.toLowerCase() == "enable" || value.toLowerCase() == "on"
    }
}
