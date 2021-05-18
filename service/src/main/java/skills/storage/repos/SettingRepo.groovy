/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.storage.repos

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
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

    @Nullable
    Setting findAllByTypeAndSettingGroupAndSettingAndProjectId(Setting.SettingType type, String settingGroup, String setting, String projectId)


    @Nullable
    @Query('''select s.value from Setting s, User u 
            where 
                s.userRefId = u.id and
                u.userId=?1 and
                s.setting=?2 and 
                s.projectId is null and 
                s.type='User' ''')
    String findUserSettingValueByUserIdAndSettingAndProjectIdIsNull(String userId, String setting)

    @Modifying
    @Query("delete from Setting s where s.setting = ?1 AND s.type = 'Global'")
    void deleteGlobalSetting(String setting)

    @Modifying
    @Query("delete from Setting  s where s.setting = ?1 AND s.value = ?2 AND s.type = 'RootUser' ")
    void deleteRootUserSetting(String setting, String value)

    @Modifying
    void deleteBySettingAndType(String setting, Setting.SettingType type)

    @Modifying
    void deleteBySettingAndTypeAndUserRefId(String setting, Setting.SettingType type, Integer userRefId)

}
