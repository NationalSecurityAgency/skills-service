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
package skills.services.events.pointsAndAchievements

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.services.LockingService
import skills.services.ProjectParticipationService
import skills.services.CatalogParticipantBackfillService
import skills.services.admin.UserCommunityService
import skills.storage.repos.ProjectParticipationRepo
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.services.CatalogImportedCreditService
import skills.services.events.SkillDate
import skills.storage.model.SkillDefMin
import skills.storage.repos.SkillDefRepo

@Component
@Slf4j
@CompileStatic
class ImportedSkillsAchievementsHandler {

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired CatalogImportedCreditService importedCreditService

    @Autowired
    LockingService lockingService

    @Autowired ProjectParticipationService participationService
    @Autowired CatalogParticipantBackfillService backfillService
    @Autowired ProjectParticipationRepo participationRepo
    @Autowired UserCommunityService userCommunityService

    void handleAchievementsForImportedSkills(String userId, SkillDefMin skill, SkillDate incomingSkillDate, boolean thisRequestCompletedOriginalSkill, boolean isMotivationalSkill) {
        if (log.isDebugEnabled()) {
            log.debug("userId=[${userId}], skill=[${skill.skillId}], incomingSkillDate=[${incomingSkillDate}], thisRequestCompletedOriginalSkill=[${thisRequestCompletedOriginalSkill}]")
        }
        lockTransaction(userId, skill.projectId)
        boolean enabled = Boolean.valueOf(skillDefRepo.findSkillDefMinById(skill.id).enabled)
        boolean finalizing = backfillService.hasApplicableFinalization(skill.projectId)
        if (!enabled || finalizing) {
            // Order deferral with activation's durable handoff, without serializing
            // unrelated learners' ordinary reconciliation on a project-wide lock.
            lockingService.lockCatalogActivation(skill.projectId)
            enabled = Boolean.valueOf(skillDefRepo.findSkillDefMinById(skill.id).enabled)
            finalizing = backfillService.hasApplicableFinalization(skill.projectId)
        }
        if (!enabled) {
            log.debug('Skipping imported achievement for disabled skill: projectId=[{}], skillId=[{}], userId=[{}]', skill.projectId, skill.skillId, userId)
            return
        }
        if (userCommunityService.isUserCommunityOnlyProject(skill.projectId) && !userCommunityService.isUserCommunityMember(userId)) {
            log.debug('Skipping imported achievement for non-community member: projectId=[{}], skillId=[{}], userId=[{}]', skill.projectId, skill.skillId, userId)
            return
        }
        if (finalizing) {
            // Activation schedules discrepancies for all enabled imports, not just the new batch.
            log.debug('Deferring imported credit to finalization: projectId=[{}], skillId=[{}], userId=[{}]', skill.projectId, skill.skillId, userId)
            return
        }
        if (skill.copiedFromProjectId != skill.projectId) {
            if (!participationService.isEligible(skill.projectId, userId)) {
                log.debug('Skipping imported achievement for ineligible participant: projectId=[{}], skillId=[{}], userId=[{}]', skill.projectId, skill.skillId, userId)
                return
            }
            if (participationRepo.readStatus(skill.projectId, userId) == BackfillStatus.PENDING) {
                // Any synchronization request can finish initialization. This does not
                // depend on acknowledgment of an older project-wide sweep task.
                backfillService.backfill(skill.projectId, userId)
            }
        }
        importedCreditService.reconcile(userId, skill)
    }

    @Profile
    private void lockTransaction(String userId, String project) {
        log.debug("locking user [{}]-[{}]", userId, project)
        lockingService.lockForSkillReporting(userId, project)
    }
}
