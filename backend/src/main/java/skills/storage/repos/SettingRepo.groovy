package skills.storage.repos


import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.Setting

interface SettingRepo extends CrudRepository<Setting, Integer> {

    @Nullable
    Setting findByTypeAndUserRefIdAndProjectIdAndSettingGroupAndSetting(Setting.SettingType type, @Nullable Integer userRefId, @Nullable String projectId, @Nullable String settingGroup, String setting)

    @Nullable
    List<Setting> findAllByTypeAndUserRefIdAndSettingGroup(Setting.SettingType type, Integer userRefId, String settingGroup)

    @Nullable
    List<Setting> findAllByTypeAndProjectId(Setting.SettingType type, String projectId)

    @Nullable
    List<Setting> findAllByTypeAndSettingGroup(Setting.SettingType type, String settingGroup)
}
