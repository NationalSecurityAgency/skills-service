package skills.storage.repos

import org.springframework.data.repository.CrudRepository
import skills.storage.model.CustomIcon
import skills.storage.model.Setting

import javax.transaction.Transactional

interface SettingRepo extends CrudRepository<Setting, Integer> {

    List<Setting> findAllByProjectId(String projectId)

    List<Setting> findAllBySettingGroup(String settingGroup)

    List<Setting> findAllByProjectIdAndSettingGroup(String projectId, String settingGroup)

    void delete(Setting toDelete)

    List<Setting> findAllByProjectIdAndSetting(String projectid, String setting)

    List<Setting> findAllBySetting(String setting)

    @Transactional
    void deleteByProjectIdAndSetting(String projectId, String setting)
}
