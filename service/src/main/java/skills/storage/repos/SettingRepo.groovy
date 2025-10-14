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

import java.util.stream.Stream

interface SettingRepo extends CrudRepository<Setting, Integer> {

    @Nullable
    Setting findByTypeAndUserRefIdAndProjectIdIgnoreCaseAndSettingGroupAndSetting(Setting.SettingType type, @Nullable Integer userRefId, @Nullable String projectId, @Nullable String settingGroup, String setting)

    @Nullable
    List<Setting> findByTypeAndUserRefIdAndProjectIdAndSettingGroupAndSettingIn(Setting.SettingType type, @Nullable Integer userRefId, @Nullable String projectId, @Nullable String settingGroup, List<String> settings)

    @Nullable
    List<Setting> findAllByTypeAndUserRefIdAndSettingGroup(Setting.SettingType type, Integer userRefId, String settingGroup)

    @Nullable
    List<Setting> findAllByTypeAndProjectId(Setting.SettingType type, String projectId)

    @Nullable
    List<Setting> findAllByTypeAndSettingGroup(Setting.SettingType type, String settingGroup)

    @Nullable
    List<Setting> findAllByTypeAndSetting(Setting.SettingType type, String setting)

    @Nullable
    Setting findAllByTypeAndSettingGroupAndSettingAndProjectId(Setting.SettingType type, String settingGroup, String setting, String projectId)

    @Nullable
    Setting findByTypeAndSkillRefIdAndSettingGroupAndSetting(Setting.SettingType type, Integer skillRefId, @Nullable String settingGroup, String setting)

    @Nullable
    @Query('''SELECT s 
            FROM Setting s 
            JOIN SkillDef sd ON s.skillRefId = sd.id
            WHERE (sd.projectId = ?1 OR (?1 IS NULL AND sd.projectId IS NULL)) 
              AND sd.skillId = ?2
              AND (s.settingGroup = ?3 OR (?3 IS NULL AND s.settingGroup IS NULL))
              AND s.setting = ?4
              AND (s.projectId = ?1 OR (?1 IS NULL AND s.projectId IS NULL))
              AND s.type = 'Skill' ''')
    Setting findSkillSettingByProjectIdAndSkillId(@Nullable String projectId, String skillId, @Nullable String settingGroup, String setting)

    @Nullable
    @Query('''select s from Setting s 
            where 
                s.setting=?1 and 
                s.projectId in ?2 and 
                s.type='Project' ''')
    List<Setting> findSettingsInProjectList(String setting, List<String> projectIds)

    @Nullable
    @Query('''select s.value from Setting s, User u 
            where 
                s.userRefId = u.id and
                u.userId=?1 and
                s.setting=?2 and 
                s.projectId is null and 
                s.type='User' ''')
    String findUserSettingValueByUserIdAndSettingAndProjectIdIsNull(String userId, String setting)

    @Nullable
    @Query('''select s.value from Setting s, User u 
            where 
                s.userRefId = u.id and
                u.userId=?1 and
                s.setting=?2 and 
                s.projectId=?3 and 
                s.type='UserProject' ''')
    String findUserSettingValueByUserIdAndSettingAndProjectId(String userId, String setting, String projectId)

    @Nullable
    @Query('''select s from Setting s, User u 
            where 
                s.userRefId = u.id and
                u.userId=?1 and
                s.settingGroup=?2 and
                s.type = 'UserProject' ''')
    List<Setting> findUserSettingsForAllProjectsByUserIdAndAndGroupId(String userId, String settingsGroup)

    @Modifying
    @Query("delete from Setting s where s.setting = ?1 AND s.type = 'Global'")
    void deleteGlobalSetting(String setting)

    @Modifying
    @Query("delete from Setting  s where s.setting = ?1 AND s.value = ?2 AND s.type = 'RootUser' ")
    void deleteRootUserSetting(String setting, String value)

    @Modifying
    void deleteBySettingAndType(String setting, Setting.SettingType type)

    @Modifying
    @Query("delete from Setting  s where lower(s.projectId) = lower(?1) and lower(s.setting) = lower(?2) and s.type = 'Project' and s.userRefId is null")
    void deleteProjectSetting(String projectId, String setting)

    @Modifying
    void deleteBySettingAndTypeAndUserRefId(String setting, Setting.SettingType type, Integer userRefId)

    @Modifying
    void deleteBySettingAndSettingGroupAndProjectIdAndTypeAndUserRefId(String setting, String settingGroup, String projectId, Setting.SettingType type, Integer userRefId)

    @Query('''select s from Setting s where s.settingGroup=?1''')
    Stream<Setting> scanSettingsByGroup(String settingsGroup)
}
