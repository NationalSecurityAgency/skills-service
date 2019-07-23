package skills.services.settings;

import skills.controller.request.model.SettingsRequest;
import skills.services.settings.listeners.ValidationRes;
import skills.storage.model.Setting;

public interface SettingChangedListener {
    boolean supports(SettingsRequest setting);
    void execute(Setting previousValue, SettingsRequest newValue);
    ValidationRes isValid(SettingsRequest newValue);
}
