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
package skills.tasks.executors

import callStack.profiler.CProf
import callStack.profiler.Profile
import callStack.profiler.ProfileEvent
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.VoidExecutionHandler
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.controller.request.model.GlobalSettingsRequest
import skills.controller.result.model.SettingsResult
import skills.services.LockingService
import skills.services.admin.UserAchievementExpirationService
import skills.services.settings.SettingsService
import skills.storage.model.SkillAttributesDef
import skills.tasks.data.ExpireUserAchievements

import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Slf4j
@Component
class ExpireUserAchievementsTaskExecutor implements VoidExecutionHandler<ExpireUserAchievements> {

    private static final long LOGGING_THRESHOLD = 5000
    public static final String SCHEDULED_SETTING_GROUP = "scheduled"
    public static final String SKILL_EXPIRATION_LAST_RUN_DATE = "skill_expiration_last_run"

    @Autowired
    UserAchievementExpirationService userAchievementExpirationService

    @Autowired
    SettingsService settingService

    @Autowired
    LockingService lockingService

    @Override
    @Profile
    void execute(TaskInstance<ExpireUserAchievements> taskInstance, ExecutionContext executionContext) {
        CProf.clear()
        String profName = "expireUserAchievementsReport".toString()
        CProf.start(profName)
        try {
            removeExpiredUserAchievements()
        } finally {
            ProfileEvent resProfEvent = CProf.stop(profName)
            if (resProfEvent.getRuntimeInMillis() > LOGGING_THRESHOLD) {
                log.info("Expiring user achievements took > [${LOGGING_THRESHOLD}]ms", CProf.prettyPrint())
            }
        }
    }

    @Transactional
    void removeExpiredUserAchievements() {
        log.info("Checking for expiring user achievements.")
        lockingService.lockForSkillExpiration()

        final LocalDate todayLd = LocalDate.now()
        final String today = todayLd.format(DateTimeFormatter.BASIC_ISO_DATE)

        SettingsResult expirationLastRan = settingService.getGlobalSetting(SKILL_EXPIRATION_LAST_RUN_DATE, SCHEDULED_SETTING_GROUP)

        if (expirationLastRan && !LocalDate.parse(expirationLastRan.getValue(), DateTimeFormatter.BASIC_ISO_DATE).isBefore(todayLd)) {
            log.info("skill expiration was already run today (potentially by another node), will not run again today")
            return
        }

        try {
            List<SkillAttributesDef> skillAttributesDefList = userAchievementExpirationService.getSkillAttributesForExpirationCheck()
            for (SkillAttributesDef skillAttributesDef : skillAttributesDefList) {
                try {
                    userAchievementExpirationService.checkAndExpireIfNecessary(skillAttributesDef)
                } catch (Exception ex) {
                    log.error("Unexpected error expiring skill - ${skillAttributesDef}", ex)
                }
            }
        } finally {
            GlobalSettingsRequest lastRunSettingRequest = new GlobalSettingsRequest()
            lastRunSettingRequest.value = today
            lastRunSettingRequest.setting = SKILL_EXPIRATION_LAST_RUN_DATE
            lastRunSettingRequest.settingGroup = SCHEDULED_SETTING_GROUP
            settingService.saveSetting(lastRunSettingRequest)
        }
    }
}
