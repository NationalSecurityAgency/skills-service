package skills.services.settings;

public enum Settings {
    LEVEL_AS_POINTS("level.points.enabled");

    private String settingName;

    private Settings(String settingName){
        this.settingName = settingName;
    }

    public String getSettingName(){
        return this.settingName;
    }
}
