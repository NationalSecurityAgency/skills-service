package skills.service.datastore.services.settings;

import skills.service.controller.request.model.SettingsRequest;
import skills.storage.model.Setting;

public interface SettingChangedListener {
    public boolean supports(SettingsRequest setting);
    public void execute(Setting previousValue, SettingsRequest newValue);
}
