package skills.service.controller.result.model

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

    boolean isEnabled(){
        return Boolean.valueOf(value) || value.toLowerCase() == "enabled" || value.toLowerCase() == "enable" || value.toLowerCase() == "on"
    }
}
