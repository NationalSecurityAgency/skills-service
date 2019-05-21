package skills.storage.repos

import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.CustomIcon
import skills.storage.model.Setting

import javax.transaction.Transactional

interface SettingRepo extends CrudRepository<Setting, Integer> {

    @Transactional
    List<Setting> findAllByProjectId(String projectId)

    @Transactional
    List<Setting> findAllBySettingGroup(String settingGroup)

    @Transactional
    List<Setting> findAllByProjectIdAndSettingGroup(String projectId, String settingGroup)

    @Transactional
    void delete(Setting toDelete)

    @Transactional
    List<Setting> findAllByProjectIdAndSetting(@Nullable String projectid, String setting)

    @Transactional
    List<Setting> findAllBySetting(String setting)

    @Transactional
    void deleteByProjectIdAndSetting(String projectId, String setting)
}
