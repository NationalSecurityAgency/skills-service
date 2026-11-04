/**
 * Copyright 2026 SkillTree
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
package skills.services.admin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillsValidator
import skills.services.BadgeUtils
import skills.services.CatalogImportedCreditService
import skills.services.CatalogParticipantBackfillService
import skills.services.LockingService
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints
import skills.storage.repos.ProjectParticipationRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillEventsSupportRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo
import skills.storage.repos.UserPointsRepo

/** Temporary pre-5.1 explicit recovery, not learner initialization. Retire with cleanup tooling. */
@Service
class CatalogParticipationFixService {
    @Autowired LockingService lockingService
    @Autowired CatalogParticipantBackfillService backfillService
    @Autowired CatalogImportedCreditService creditService
    @Autowired ProjectParticipationRepo participationRepo
    @Autowired UserPerformedSkillRepo performedRepo
    @Autowired SkillDefRepo skillRepo
    @Autowired SkillEventsSupportRepo eventsSupportRepo
    @Autowired UserAchievedLevelRepo achievementRepo
    @Autowired UserPointsRepo pointsRepo
    @Autowired UserCommunityService communityService
    @Autowired SkillsGroupAdminService groupService

    @Transactional(timeoutString = '${skills.catalog.backfillTimeoutSeconds:300}')
    void fix(String projectId, String userId) {
        lockingService.lockForSkillReporting(userId, projectId)
        SkillsValidator.isTrue(participationRepo.existsByProjectIdAndUserId(projectId, userId) ||
                performedRepo.existsLocalEventByProjectIdAndUserId(projectId, userId),
                'Fix requires existing destination participation or local performed events; it does not enroll users', projectId)
        SkillsValidator.isTrue(!backfillService.hasApplicableFinalization(projectId),
                'Catalog finalization is RUNNING or FAILED; retry Fix after finalization/recovery', projectId)
        List<SkillDef> imports = skillRepo.findAllByProjectIdAndTypeAndEnabledAndCopiedFromIsNotNull(projectId, SkillDef.ContainerType.Skill, 'true')
        Set<Integer> processedIds = imports.collect { SkillDef skill -> skill.id }.toSet()
        if (!communityService.isUserCommunityOnlyProject(projectId) || communityService.isUserCommunityMember(userId)) {
            List<SkillDef> ordered = imports.findAll { SkillDef skill -> skill.copiedFromProjectId != projectId } +
                    imports.findAll { SkillDef skill -> skill.copiedFromProjectId == projectId }
            ordered.each { SkillDef skill -> creditService.reconcileSkillCredit(userId, skillRepo.findSkillDefMinById(skill.id), true) }
        } else {
            // An explicit repair must not retain catalog credit that community policy disallows.
            imports.each { SkillDef skill ->
                if (skill.copiedFromProjectId != projectId) {
                    UserPoints credit = pointsRepo.findByProjectIdAndUserIdAndSkillId(projectId, userId, skill.skillId)
                    if (credit) { pointsRepo.delete(credit) }
                    achievementRepo.deleteAll(achievementRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skill.skillId))
                }
            }
            // Same-project reuse may reference a now-cleared catalog row. Reconcile it from
            // that committed-in-this-transaction source state, retaining reuse of native credit.
            imports.findAll { SkillDef skill -> skill.copiedFromProjectId == projectId }.each { SkillDef skill ->
                creditService.reconcileSkillCredit(userId, skillRepo.findSkillDefMinById(skill.id), true)
            }
        }
        // Remove only invalid destination-derived awards. Valid awards retain IDs/dates/audits.
        List<SkillDef> groups = skillRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.SkillsGroup)
        groups.each { SkillDef group ->
            int required = groupService.getActualNumSkillsRequred(group.numSkillsRequired, group.id)
            if (achievementRepo.countAchievedChildren(userId, projectId, group.skillId, SkillRelDef.RelationshipType.SkillsGroupRequirement) < required) {
                achievementRepo.deleteAll(achievementRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, group.skillId))
            }
        }
        List<SkillDef> badges = skillRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.Badge)
        badges.each { SkillDef badge ->
            if (achievementRepo.countNonAchievedChildren(userId, projectId, badge.skillId, SkillRelDef.RelationshipType.BadgeRequirement) > 0) {
                achievementRepo.deleteAll(achievementRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, badge.skillId))
            }
        }
        creditService.reconcileTotalsAndAchievements(projectId, userId,
                skillRepo.findAllByProjectIdAndTypeAndEnabled(projectId, SkillDef.ContainerType.Skill, 'true'), true)
        // No ordinary badge handler: quiz awards could modify membership or global achievements.
        badges.each { SkillDef badge ->
            if (badge.enabled == 'true' && BadgeUtils.withinActiveTimeframe(badge) &&
                    achievementRepo.countNonAchievedChildren(userId, projectId, badge.skillId, SkillRelDef.RelationshipType.BadgeRequirement) == 0 &&
                    achievementRepo.countAchievedChildren(userId, projectId, badge.skillId, SkillRelDef.RelationshipType.BadgeRequirement) > 0 &&
                    !achievementRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, badge.skillId)) {
                Date latestEvent = eventsSupportRepo.getUserPerformedSkillLatestDate(userId, projectId, badge.id, SkillRelDef.RelationshipType.BadgeRequirement)
                Date latestAchievement = eventsSupportRepo.getLatestChildAchievementDate(userId, projectId, badge.id, SkillRelDef.RelationshipType.BadgeRequirement)
                Date achievedOn = [latestEvent, latestAchievement].findAll { Date date -> date != null }.max() ?: new Date()
                achievementRepo.save(new UserAchievement(userId: userId, projectId: projectId, skillId: badge.skillId,
                        skillRefId: badge.id, achievedOn: achievedOn))
            }
        }
        // Reporting first; acquire finalization-init only at completion and retain through commit.
        try {
            backfillService.validateCreditCompletion(projectId, processedIds)
        } catch (IllegalStateException overlap) {
            SkillsValidator.isTrue(false, 'Catalog finalization overlap overtook Fix; repair rolled back. Retry after finalization/recovery.', projectId)
        }
    }
}
