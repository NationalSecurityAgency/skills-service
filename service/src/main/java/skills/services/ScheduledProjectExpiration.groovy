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

    @Autowired
    FeatureService featureService

    @Value('#{"${skills.config.unusedProjectDeletionEnabled:true}"}')
    Boolean unusedProjectDeletionEnabled = true

    @Value('#{"${skills.config.expireUnusedProjectsOlderThan:180}"}')
    int unusedProjectExpirationInDays

    @Value('#{"${skills.config.expirationGracePeriod:7}"}')
    int unusedProjectExpirationGracePeriodInDays

    @Scheduled(cron='#{"${skills.config.projectExpirationSchedule:* 4 0 * * *}"}')
    public void flagUnusedProjectsForDeletion(){
        if (!unusedProjectDeletionEnabled) {
            log.debug("skills.config.unusedProjectDeletionEnabled is set to false, unused project deletion will not occur")
            return
        }
        if (!featureService.isEmailServiceFeatureEnabled()) {
            log.debug("Email Settings have not configured for this instance, unused project deletion will not occur")
            return
        }
        log.info("identifying projects that haven't been used in [${unusedProjectExpirationInDays}] days")
        Date expireOlderThan = new Date().minus(unusedProjectExpirationInDays)
        projectExpirationService.flagOldProjects(expireOlderThan)
        log.info("deleting projects whose grace period has expired")
        Date cutoff = new Date().minus(unusedProjectExpirationGracePeriodInDays)
        projectExpirationService.deleteUnusedProjects(cutoff)
        log.info("sending pending deletion notifications to Project Administrators")
        projectExpirationService.notifyGracePeriodProjectAdmins(cutoff)
    }

}
