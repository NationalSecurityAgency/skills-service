/**
 * Copyright 2021 SkillTree
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
package skills.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.controller.request.model.ProjectSettingsRequest
import skills.controller.result.model.SettingsResult
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.storage.model.ProjectLastTouched
import skills.storage.repos.ProjDefRepo

@Component
class ProjectExpirationService {

    private static final String SETTING_GROUP = "expiration"

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SettingsService settingService

    @Transactional
    public void flagProjectsOlderThan(Date expireAfter) {
        List<ProjectLastTouched> lastTouchedList = projDefRepo.findProjectsNotTouchedSince(expireAfter)
        List<ProjectLastTouched> toFlag = []

        // do we need both settings? what if you change expiring.unused to false if it's cancelled?
        //look up settings - confirm
        //that project is not already flagged for expiration
        //or that expiration was cancelled but the updated column value is older than expireAfter
        List<ProjectSettingsRequest> settings = []
        lastTouchedList.each {
            SettingsResult result = settingService.getProjectSetting(it.projectId, Settings.EXPIRING_UNUSED.getSettingName(), SETTING_GROUP)
            //extract date to configuration, etc
            if (!result || (result.getValue() == Boolean.FALSE.toString() && result.getUpdated().before(new Date().minus(180)))) {
                ProjectSettingsRequest psr = new ProjectSettingsRequest()
                psr.projectId = it.projectId
                psr.setting = Settings.EXPIRING_UNUSED.getSettingName()
                psr.settingGroup = "expiration"
                psr.value = Boolean.TRUE.toString()
                settings.add(psr)
            }
        }

        if (settings) {
            settingService.saveSettings(settings)
        }
    }

    @Transactional
    public void cancelExpiration(String projectId) {
        //remove expiring flag
        //add cancel expiration flag
        ProjectSettingsRequest psr = new ProjectSettingsRequest()
        psr.projectId = projectId
        psr.setting = Settings.EXPIRING_UNUSED.getSettingName()
        psr.settingGroup = SETTING_GROUP
        psr.value = Boolean.FALSE.toString()
        settingService.saveSetting(psr)
    }
}
