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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.ProjectSettingsRequest
import skills.controller.result.model.SettingsResult
import skills.notify.EmailNotifier
import skills.notify.Notifier
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.storage.model.Notification
import skills.storage.model.ProjDef
import skills.storage.model.ProjectLastTouched
import skills.storage.model.auth.RoleName
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.UserRoleRepo

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Slf4j
@Component
class ProjectExpirationService {

    private static final String SETTING_GROUP = "expiration"

    @Value('#{"${skills.config.expireUnusedProjectsOlderThan:180}"}')
    int unusedProjectExpirationInDays

    @Value('#{"${skills.config.expirationGracePeriod:7}"}')
    int unusedProjectExpirationGracePeriodInDays

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SettingsService settingService

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    FeatureService featureService

    @Autowired
    EmailNotifier notifier

    @Transactional
    public void flagOldProjects(Date expireOlderThan) {
        List<ProjectLastTouched> lastTouchedList = projDefRepo.findProjectsNotTouchedSince(expireOlderThan)

        List<ProjectSettingsRequest> settings = []
        lastTouchedList.each {
            SettingsResult result = settingService.getProjectSetting(it.projectId, Settings.EXPIRING_UNUSED.getSettingName(), SETTING_GROUP)
            if (!result || (result.getValue() == Boolean.FALSE.toString() && result.getUpdated().before(expireOlderThan))) {
                ProjectSettingsRequest psr = new ProjectSettingsRequest()
                psr.projectId = it.getProjectId()
                psr.setting = Settings.EXPIRING_UNUSED.getSettingName()
                psr.settingGroup = SETTING_GROUP
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
        //validate that the projectId exists
        boolean exists = projDefRepo.existsByProjectIdIgnoreCase(projectId)
        if (!exists) {
            throw new SkillException("Cannot cancel expiration for Project ID that does not exist", projectId, "", ErrorCode.ProjectNotFound)
        }
        ProjectSettingsRequest psr = new ProjectSettingsRequest()
        psr.projectId = projectId
        psr.setting = Settings.EXPIRING_UNUSED.getSettingName()
        psr.settingGroup = SETTING_GROUP
        psr.value = Boolean.FALSE.toString()
        settingService.saveSetting(psr)
    }

    @Transactional
    public void deleteUnusedProjects(Date cutoff) {
        List<ProjDef> expiringProjects = projDefRepo.getExpiringProjects(cutoff)
        log.info("identified [${expiringProjects?.size()}] Projects flagged for expiration on or before [${cutoff}], deleting now")
        expiringProjects.each {
            projDefRepo.delete(it)
        }
        log.info("removed [${expiringProjects.size()}] expired projects")
    }

    @Transactional(readOnly = true)
    public List<ProjDef> getFlaggedProjectsInGracePeriod(Date cutoff) {
        List<ProjDef> expiringProjectsInGracePeriod = projDefRepo.getProjectsWithinGracePeriod(cutoff)
        log.info("identifed [${expiringProjectsInGracePeriod.size()}] Projects that are flagged for expiration but still within the grace period of [${unusedProjectExpirationInDays} days]")
        return expiringProjectsInGracePeriod
    }

    @Transactional
    public void notifyGracePeriodProjectAdmins(Date gracePeriodCutoff) {
        String publicUrl = featureService.getPublicUrl()
        LocalDateTime now = LocalDateTime.now()

        List<ProjDef> inGracePeriod = getFlaggedProjectsInGracePeriod(gracePeriodCutoff)
        inGracePeriod.each {
            List<String> adminUsers = userRoleRepo.findUserIdsByProjectIdAndRoleName(it.getProjectId(), RoleName.ROLE_PROJECT_ADMIN)
            SettingsResult expiring = settingService.getProjectSetting(it.projectId, Settings.EXPIRING_UNUSED.getSettingName(), SETTING_GROUP)
            if (adminUsers) {
                Date updated = expiring.getUpdated()
                Date expirationDate = updated.plus(unusedProjectExpirationGracePeriodInDays)
                int days = Math.abs(ChronoUnit.DAYS.between(now, expirationDate.toLocalDateTime()))
                Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                        userIds: adminUsers,
                        type: Notification.Type.ProjectExpiration.toString(),
                        keyValParams: [
                                projectName     : it.name,
                                projectId       : it.projectId,
                                publicUrl       : publicUrl,
                                expiresOn       : expirationDate.toLocalDateTime().format(DateTimeFormatter.ISO_DATE),
                                expiringIn      : "${days} day${days == 1 ? '' : 's'}",
                                createdOn       : it.created.toLocalDateTime().format(DateTimeFormatter.ISO_DATE),
                                unusedProjectExpirationInDays : unusedProjectExpirationInDays
                        ],
                )
                notifier.sendNotification(request)
            }
        }
    }
}
