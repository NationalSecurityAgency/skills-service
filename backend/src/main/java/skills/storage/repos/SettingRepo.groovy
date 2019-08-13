package skills.storage.repos


import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.Setting

interface SettingRepo extends CrudRepository<Setting, Integer> {

    @Nullable
    Setting findByTypeAndUserIdAndProjectIdAndSettingGroupAndSetting(Setting.SettingType type, @Nullable Integer userId, @Nullable String projectId, @Nullable String settingGroup, String setting)

    @Nullable
    List<Setting> findAllByTypeAndUserIdAndSettingGroup(Setting.SettingType type, Integer userId, String settingGroup)

    @Nullable
    List<Setting> findAllByTypeAndProjectId(Setting.SettingType type, String projectId)

    @Nullable
    List<Setting> findAllByTypeAndSettingGroup(Setting.SettingType type, String settingGroup)
}
