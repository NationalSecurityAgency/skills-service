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
package skills.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.services.admin.BatchOperationsTransactionalAccessor
import skills.services.admin.SkillCatalogFinalizationService
import skills.services.admin.SkillCatalogFinalizationService.FinalizeState
import skills.services.admin.UserCommunityService
import skills.services.events.AchievedBadgeHandler
import skills.services.events.AchievedGlobalBadgeHandler
import skills.services.events.SkillDate
import skills.services.events.SkillEventResult
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.repos.ProjectParticipationRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo
import skills.storage.repos.UserPointsRepo
import skills.tasks.TaskSchedulerService

@Service
class CatalogParticipantBackfillService {
    @Autowired ProjectParticipationRepo participationRepo
    @Autowired SkillDefRepo skillDefRepo
    @Autowired UserPerformedSkillRepo performedSkillRepo
    @Autowired UserPointsRepo userPointsRepo
    @Autowired UserAchievedLevelRepo achievementRepo
    @Autowired LockingService lockingService
    @Autowired UserCommunityService userCommunityService
    @Autowired TaskSchedulerService taskSchedulerService
    @Autowired @Lazy SkillCatalogFinalizationService finalizationService
    @Autowired BatchOperationsTransactionalAccessor batchAccessor
    @Autowired RuleSetDefGraphService graphService
    @Autowired @Lazy AchievedBadgeHandler badgeHandler
    @Autowired @Lazy AchievedGlobalBadgeHandler globalBadgeHandler

    boolean hasApplicableFinalization(String projectId) {
        skillDefRepo.existsCatalogImport(projectId) &&
                finalizationService.getCurrentState(projectId) in [FinalizeState.RUNNING, FinalizeState.FAILED]
    }

    @Transactional
    BackfillStatus touchEvaluatedVisit(String projectId, String userId) {
        participationRepo.touchActivity(projectId, userId, new Date())
        participationRepo.readStatus(projectId, userId)
    }

    // Joins the successful award transaction; source-only events never call this for a destination.
    @Transactional
    BackfillStatus qualify(String projectId, String userId, boolean visit = false) {
        lockingService.lockForSkillReporting(userId, projectId)
        ProjectParticipation participant = participationRepo.findByProjectIdAndUserId(projectId, userId)
        if (participant && participant.backfillStatus != BackfillStatus.NOT_REQUESTED) {
            return participant.backfillStatus
        }
        if (!visit && !participant && !performedSkillRepo.existsLocalEventByProjectIdAndUserId(projectId, userId)) {
            return null
        }
        // Retain the foundation's reporting-before-activation lock convention. This
        // does not define the Step 3 start/completion or copy/source-event boundary.
        lockingService.lockCatalogActivation(projectId)
        boolean finalizing = hasApplicableFinalization(projectId)
        boolean imports = skillDefRepo.existsEnabledCatalogImport(projectId)
        if (!participant && !visit && !imports && !finalizing) {
            return null
        }
        if (!participant) {
            participant = new ProjectParticipation(projectId: projectId, userId: userId)
        }
        boolean sourceCredit = !finalizing && imports && skillDefRepo.existsSourceCreditForEnabledCatalogImports(projectId, userId,
                userCommunityService.userCommunityUserTagKey ?: '', userCommunityService.userCommunityUserTagValue ?: '')
        participant.backfillStatus = finalizing || sourceCredit ? BackfillStatus.PENDING :
                (imports ? BackfillStatus.COMPLETED : BackfillStatus.NOT_NEEDED)
        participationRepo.save(participant)
        if (participant.backfillStatus == BackfillStatus.PENDING && !finalizing) {
            taskSchedulerService.scheduleCatalogParticipantBackfill(projectId, userId)
        }
        return participant.backfillStatus
    }

    @Transactional
    void reset(String projectId, String userId, boolean hadLocalEvents = false) {
        lockingService.lockForSkillReporting(userId, projectId)
        lockingService.lockCatalogActivation(projectId)
        ProjectParticipation participant = participationRepo.findByProjectIdAndUserId(projectId, userId)
        if (!participant && !hadLocalEvents) {
            return
        }
        if (hasApplicableFinalization(projectId)) {
            // Retain eligibility after deletion so finalization can restore all enabled import batches.
            if (!participant && hadLocalEvents) {
                participant = new ProjectParticipation(projectId: projectId, userId: userId)
            }
            if (participant) {
                participant.backfillStatus = BackfillStatus.PENDING
                participationRepo.save(participant)
            }
            return
        }
        boolean imports = skillDefRepo.existsEnabledCatalogImport(projectId)
        if (!participant && hadLocalEvents && imports) {
            participant = new ProjectParticipation(projectId: projectId, userId: userId)
        }
        if (participant) {
            // Nested awards during restoration must not schedule initial backfill; failures roll this status back too.
            participant.backfillStatus = imports ? BackfillStatus.COMPLETED : BackfillStatus.NOT_NEEDED
            participationRepo.save(participant)
        }
        if (imports && skillDefRepo.existsSourceCreditForEnabledCatalogImports(projectId, userId,
                userCommunityService.userCommunityUserTagKey ?: '', userCommunityService.userCommunityUserTagValue ?: '')) {
            // Joins the deletion transaction: callers never observe cleared catalog credit after a normal reset.
            copyCredit(projectId, userId)
        }
    }

    @Transactional(timeoutString = '${skills.catalog.backfillTimeoutSeconds:300}')
    void backfill(String projectId, String userId) {
        lockingService.lockForSkillReporting(userId, projectId)
        ProjectParticipation participant = participationRepo.findByProjectIdAndUserId(projectId, userId)
        if (!participant || participant.backfillStatus != BackfillStatus.PENDING) {
            return
        }
        if (hasApplicableFinalization(projectId)) {
            throw new IllegalStateException("Catalog finalization is not complete for [${projectId}]")
        }
        if (!userCommunityService.isUserCommunityOnlyProject(projectId) || userCommunityService.isUserCommunityMember(userId)) {
            copyCredit(projectId, userId)
        }
        participant.backfillStatus = BackfillStatus.COMPLETED
        participationRepo.save(participant)
    }

    private void copyCredit(String projectId, String userId) {
        List<SkillDef> imports = skillDefRepo.findAllByProjectIdAndTypeAndEnabledAndCopiedFromIsNotNull(
                projectId, SkillDef.ContainerType.Skill, 'true')
        if (!imports) {
            return
        }
        List<Integer> sourceIds = imports.collect { SkillDef skill -> skill.copiedFrom }
        batchAccessor.copySingleUserSkillUserPointsToTheImportedProjects(userId, projectId, sourceIds)
        batchAccessor.copyForSingleUserSkillAchievementsToTheImportedProjects(userId, projectId, sourceIds)
        List<Integer> reuseSourceIds = imports.findAll { SkillDef skill -> skill.copiedFromProjectId == projectId }
                .collect { SkillDef skill -> skill.copiedFrom }
        if (reuseSourceIds) {
            // Reused catalog skills reference destination rows restored by the first copy pass.
            batchAccessor.copySingleUserSkillUserPointsToTheImportedProjects(userId, projectId, reuseSourceIds)
            batchAccessor.copyForSingleUserSkillAchievementsToTheImportedProjects(userId, projectId, reuseSourceIds)
        }
        List<SkillDef> groups = imports.findAll { SkillDef skill -> skill.groupId != null }
                .collect { SkillDef skill -> graphService.getMyGroupParent(skill.id) }.unique { SkillDef group -> group.id }
        batchAccessor.identifyAndAddGroupAchievementsForSingleUser(userId, groups)
        List<SkillDef> subjects = imports.collect { SkillDef skill -> graphService.getMySubjectParent(skill.id) }.unique { SkillDef subject -> subject.id }
        subjects.each { SkillDef subject ->
            batchAccessor.createSubjectUserPointsForSingleNewUser(userId, projectId, subject.skillId)
            userPointsRepo.updateSubjectUserPointsForUser(userId, projectId, subject.skillId, true)
            batchAccessor.identifyAndAddSubjectLevelAchievementsForSingleUser(userId, projectId, subject.skillId)
            batchAccessor.addMissingCatalogLevels(projectId, subject.skillId, userId)
        }
        batchAccessor.createProjectUserPointsForSingleNewUser(userId, projectId)
        userPointsRepo.updateUserPointsForProjectAndUser(projectId, userId)
        batchAccessor.identifyAndAddProjectLevelAchievements(userId, projectId)
        batchAccessor.addMissingCatalogLevels(projectId, null, userId)
        imports.each { SkillDef skill ->
            List<UserAchievement> achievements = achievementRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skill.skillId)
            if (achievements) {
                SkillDate date = new SkillDate(date: achievements.first().achievedOn, isProvided: true)
                badgeHandler.checkForBadges(new SkillEventResult(), userId, skillDefRepo.findSkillDefMinById(skill.id), date)
            }
            globalBadgeHandler.checkForGlobalBadges(new SkillEventResult(), userId, projectId, skillDefRepo.findSkillDefMinById(skill.id))
        }
    }
}
