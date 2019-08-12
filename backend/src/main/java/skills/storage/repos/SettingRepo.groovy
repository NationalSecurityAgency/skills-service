package skills.storage.repos

import org.hibernate.Transaction
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.CustomIcon
import skills.storage.model.Setting

import javax.transaction.Transactional

interface SettingRepo extends CrudRepository<Setting, Integer> {

    @Transactional
    List<Setting> findAllByType(Setting.SettingType type)

    @Transactional
    List<Setting> findAllByTypeAndProjectId(Setting.SettingType type, String projectId)

    Setting findByProjectIdAndSettingGroupAndSetting(String projectId, String settingGroup, String setting)

    @Transactional
    List<Setting> findAllByTypeAndUserId(Setting.SettingType type,Integer userId)

    @Transactional
    List<Setting> findAllByTypeAndUserIdAndProjectId(Setting.SettingType type,Integer userId, String projectId)

    @Transactional
    List<Setting> findAllByTypeAndUserIdAndProjectIdAndSettingGroup(Setting.SettingType type,Integer userId, String projectId, String settingGroup)

    @Transactional
    List<Setting> findAllByTypeAndUserIdAndProjectIdAndSettingGroupAndSetting(Setting.SettingType type,Integer userId, String projectId, String settingGroup, String setting)

    @Transactional
    List<Setting> findAllByTypeAndUserIdAndSettingGroup(Setting.SettingType type,Integer userId, String settingGroup)

    @Transactional
    List<Setting> findAllByTypeAndSettingGroup(Setting.SettingType type,String settingGroup)

    @Transactional
    List<Setting> findAllByTypeAndProjectIdAndSettingGroup(Setting.SettingType type, @Nullable String projectId, String settingGroup)

    @Transactional
    void delete(Setting toDelete)

    @Transactional
    List<Setting> findAllByTypeAndProjectIdAndSetting(Setting.SettingType type, @Nullable String projectid, String setting)

    @Transactional
    List<Setting> findAllByTypeAndProjectIdAndSettingGroupAndSetting(Setting.SettingType type, @Nullable String projectid, @Nullable String settingGroup, String setting)

    @Transactional
    List<Setting> findAllByTypeAndSettingGroupAndSetting(Setting.SettingType type, @Nullable String settingGroup, String setting)

    @Transactional
    List<Setting> findAllByTypeAndSetting(Setting.SettingType type, String setting)

    @Transactional
    void deleteByTypeAndProjectIdAndSetting(Setting.SettingType type, String projectId, String setting)
}
