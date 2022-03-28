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
package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import skills.auth.SecurityMode

@Conditional(SecurityMode.FormAuth)
@Component
@Slf4j
class ScheduledDbLockCleanup {

    @Value('#{"${skills.config.databaseLockCleanupDays:14}"}')
    int databaseLockCleanupDays

    @Autowired
    LockingService lockingService

    @Scheduled(cron='#{"${skills.config.cleanupDatabaseLocksSchedule:0 0 1 * * *}"}')
    void cleanupExpiredLocks(){
        Date cleanupDate = new Date() - databaseLockCleanupDays
        log.info("removing all skills_db_locks older than [${cleanupDate}]")
        lockingService.deleteLocksOlderThan(cleanupDate)
    }
}
