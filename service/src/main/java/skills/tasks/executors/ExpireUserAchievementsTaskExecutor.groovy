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
import skills.services.admin.UserAchievementExpirationService
import skills.storage.model.SkillAttributesDef
import skills.tasks.data.ExpireUserAchievements

@Slf4j
@Component
class ExpireUserAchievementsTaskExecutor implements VoidExecutionHandler<ExpireUserAchievements> {

    private static final long LOGGING_THRESHOLD = 5000

    @Autowired
    UserAchievementExpirationService userAchievementExpirationService

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

    void removeExpiredUserAchievements() {
        log.info("Checking for expiring user achievements.")
        List<SkillAttributesDef> skillAttributesDefList = userAchievementExpirationService.getSkillAttributesForExpirationCheck()
        for (SkillAttributesDef skillAttributesDef: skillAttributesDefList) {
            try {
                userAchievementExpirationService.checkAndExpireIfNecessary(skillAttributesDef)
            } catch (Exception ex) {
                log.error("Unexpected error expiring skill - ${skillAttributesDef}", ex)
            }
        }
    }
}
