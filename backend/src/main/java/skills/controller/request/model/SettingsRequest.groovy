package skills.controller.request.model

import groovy.transform.Canonical

@Canonical
abstract class SettingsRequest {

    //nullable
    String settingGroup
    //non-null
    String setting
    //non-null
    String value

    //convenience method for settings that are in an either on or off state as opposed to containing a meaningful value
    boolean isEnabled(){
        return Boolean.valueOf(value) || value.toLowerCase() == "enabled" || value.toLowerCase() == "enable" || value.toLowerCase() == "on"
    }
}
