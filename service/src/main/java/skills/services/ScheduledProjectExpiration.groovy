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
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Slf4j
class ScheduledProjectExpiration {

    @Autowired
    ProjectExpirationService projectExpirationService

    @Value('#{"${skills.config.expireUnusedProjectsOlderThan:180}"}')
    int unusedProjectExpirationInDays

    @Value('#{"${skills.config.expirationGracePeriod:7}"}')
    int unusedProjectExpirationGracePeriodInDays

    @Scheduled(cron='#{"${skills.config.projectExpirationSchedule:* 4 0 * * *}"}')
    public void flagUnusedProjectsForDeletion(){
        log.info("running scheduled project expiration")
        Date expireOlderThan = new Date().minus(unusedProjectExpirationInDays)
        projectExpirationService.flagOldProjects(expireOlderThan)
    }

    @Scheduled(cron='#{"${skills.config.deleteUnusedProjectSchedule:* 55 23 * * *}"}')
    public void deleteUnusedProjects() {
        log.info("deleting projects that have been flagged for expiration where the grace period has expired")
        Date cutoff = new Date().minus(unusedProjectExpirationGracePeriodInDays)
        projectExpirationService.deleteUnusedProjects(cutoff)
    }

    @Scheduled(cron='#{"${skills.config.expiringProjectNotificationSchedule:* 15 0 * * *}"}')
    public void sendNofifications() {
        Date cutoff = new Date().minus(unusedProjectExpirationGracePeriodInDays)
        projectExpirationService.notifyGracePeriodProjectAdmins(cutoff)
    }

}
