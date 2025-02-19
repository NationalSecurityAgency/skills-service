/**
 * Copyright 2024 SkillTree
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
package skills.dbupgrade

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import skills.storage.model.Setting
import skills.storage.repos.SettingRepo

@Service
@Slf4j
class DBCheckPointer implements CheckPointer {

    @Autowired
    SettingRepo settingRepo

    @PersistenceContext
    EntityManager entityManager;

    @Profile
    @Override
    @Transactional()
    int getRecordToStartOn(String filename) {
        List<Setting> settingList = settingRepo.findAllByTypeAndSetting(Setting.SettingType.UpgradeCheckpoint, filename)
        if (settingList && settingList.size() > 0) {
            return Integer.valueOf(settingList.get(0).value) + 1 // start at the next record
        }
        return 0
    }

    @Profile
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void recordRecord(String filename, int recordNum) {
        List<Setting> settingList = settingRepo.findAllByTypeAndSetting(Setting.SettingType.UpgradeCheckpoint, filename)
        if (settingList && settingList.size() > 0) {
            Setting setting = settingList.get(0)
            setting.value = recordNum.toString()
            settingRepo.save(setting)
        } else {
            Setting setting = new Setting(type: Setting.SettingType.UpgradeCheckpoint, setting: filename, value: recordNum.toString())
            settingRepo.save(setting)
        }
        entityManager.flush()
    }

    @Profile
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void cleanup(String filename) {
        settingRepo.deleteBySettingAndType(filename, Setting.SettingType.UpgradeCheckpoint)
        entityManager.flush()
    }

    @Override
    void close() throws Exception {

    }
}
