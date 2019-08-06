package skills.storage.repos

import org.hibernate.Transaction
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.CustomIcon
import skills.storage.model.Setting

import javax.transaction.Transactional

interface SettingRepo extends CrudRepository<Setting, Integer> {

    @Transactional
    List<Setting> findAllByProjectId(String projectId)

    @Transactional
    List<Setting> findAllByUserId(String userId)

    @Transactional
    List<Setting> findAllByUserIdAndProjectId(String userId, String projectId)

    @Transactional
    List<Setting> findAllByUserIdAndProjectIdAndSettingGroup(String userId, String projectId, String settingGroup)

    @Transactional
    List<Setting> findAllBySettingGroup(String settingGroup)

    @Transactional
    List<Setting> findAllByProjectIdAndSettingGroup(@Nullable String projectId, String settingGroup)

    @Transactional
    void delete(Setting toDelete)

    @Transactional
    List<Setting> findAllByProjectIdAndSetting(@Nullable String projectid, String setting)

    @Transactional
    List<Setting> findAllByProjectIdAndSettingGroupAndSetting(@Nullable String projectid, @Nullable String settingGroup, String setting)

    @Transactional
    List<Setting> findAllBySetting(String setting)

    @Transactional
    void deleteByProjectIdAndSetting(String projectId, String setting)
}
