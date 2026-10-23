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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.services.admin.SkillCatalogFinalizationService
import skills.services.admin.SkillCatalogFinalizationService.FinalizeState
import skills.services.admin.UserCommunityService
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.SkillDef
import skills.storage.repos.ProjectParticipationRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserPerformedSkillRepo
import skills.tasks.TaskSchedulerService

@Service
@Slf4j
class CatalogParticipantBackfillService {
    @Autowired ProjectParticipationRepo participationRepo
    @Autowired SkillDefRepo skillDefRepo
    @Autowired UserPerformedSkillRepo performedSkillRepo
    @Autowired LockingService lockingService
    @Autowired UserCommunityService userCommunityService
    @Autowired TaskSchedulerService taskSchedulerService
    @Autowired CatalogImportedCreditService importedCreditService
    @Autowired @Lazy SkillCatalogFinalizationService finalizationService

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
        // Order eligibility/status commits with finalization start: bulk-visible first, or PENDING for catch-up.
        lockingService.lockForSkillsCatalogFinalizationInit(projectId)
        ProjectParticipation participant = participationRepo.findByProjectIdAndUserId(projectId, userId)
        if (participant && participant.backfillStatus != BackfillStatus.NOT_REQUESTED) {
            log.trace('Retaining catalog participation status: projectId=[{}], userId=[{}], status=[{}]', projectId, userId, participant.backfillStatus)
            return participant.backfillStatus
        }
        if (!visit && !participant && !performedSkillRepo.existsLocalEventByProjectIdAndUserId(projectId, userId)) {
            log.debug('Skipping catalog qualification without participation or local events: projectId=[{}], userId=[{}]', projectId, userId)
            return null
        }
        // Serialize pending claims with activation and its catch-up handoff.
        lockingService.lockCatalogActivation(projectId)
        boolean finalizing = hasApplicableFinalization(projectId)
        boolean imports = skillDefRepo.existsEnabledCatalogImport(projectId)
        if (!participant && !visit && !imports && !finalizing) {
            log.debug('Skipping catalog qualification without enabled imports or finalization: projectId=[{}], userId=[{}]', projectId, userId)
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
        log.debug('Evaluated catalog qualification: projectId=[{}], userId=[{}], visit=[{}], enabledImports=[{}], finalizing=[{}], sourceCredit=[{}], status=[{}]',
                projectId, userId, visit, imports, finalizing, sourceCredit, participant.backfillStatus)
        if (participant.backfillStatus == BackfillStatus.PENDING && finalizing) {
            log.debug('Deferring catalog participant backfill to finalization sweep: projectId=[{}], userId=[{}]', projectId, userId)
        }
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
            log.debug('Skipping catalog participation reset without participation or prior local events: projectId=[{}], userId=[{}]', projectId, userId)
            return
        }
        if (hasApplicableFinalization(projectId)) {
            log.debug('Deferring catalog participation reset credit restoration to finalization: projectId=[{}], userId=[{}]', projectId, userId)
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
        log.debug('Evaluated catalog participation reset: projectId=[{}], userId=[{}], status=[{}]', projectId, userId, participant?.backfillStatus)
    }

    @Transactional(timeoutString = '${skills.catalog.backfillTimeoutSeconds:300}')
    void backfill(String projectId, String userId) {
        lockingService.lockForSkillReporting(userId, projectId)
        ProjectParticipation participant = participationRepo.findByProjectIdAndUserId(projectId, userId)
        if (!participant || participant.backfillStatus != BackfillStatus.PENDING) {
            log.debug('Skipping catalog participant backfill: projectId=[{}], userId=[{}], status=[{}]', projectId, userId, participant?.backfillStatus)
            return
        }
        if (hasApplicableFinalization(projectId)) {
            log.debug('Blocking catalog participant backfill until finalization completes: projectId=[{}], userId=[{}]', projectId, userId)
            throw new IllegalStateException("Catalog finalization is not complete for [${projectId}]")
        }
        // Keep the IDs evaluated by this attempt to detect a newer batch before committing completion.
        List<SkillDef> imports = skillDefRepo.findAllByProjectIdAndTypeAndEnabledAndCopiedFromIsNotNull(
                projectId, SkillDef.ContainerType.Skill, 'true')
        Set<Integer> processedImportIds = imports.collect { SkillDef skill -> skill.id }.toSet()
        if (!userCommunityService.isUserCommunityOnlyProject(projectId) || userCommunityService.isUserCommunityMember(userId)) {
            copyCredit(projectId, userId, imports)
        } else {
            log.debug('Excluding catalog credit for non-community member: projectId=[{}], userId=[{}]', projectId, userId)
        }
        validateCompletion(projectId, processedImportIds)
        participant.backfillStatus = BackfillStatus.COMPLETED
        participationRepo.save(participant)
        log.debug('Marked catalog participant COMPLETED in current transaction: projectId=[{}], userId=[{}]', projectId, userId)
    }

    private void validateCompletion(String projectId, Set<Integer> processedImportIds) {
        // Hold through commit so a new finalization cannot start after this completion check.
        lockingService.lockForSkillsCatalogFinalizationInit(projectId)
        boolean finalizing = hasApplicableFinalization(projectId)
        // A run may already be COMPLETED; changed IDs still invalidate the older enabled-only calculation.
        Set<Integer> enabledImportIds = skillDefRepo.findAllByProjectIdAndTypeAndEnabledAndCopiedFromIsNotNull(
                projectId, SkillDef.ContainerType.Skill, 'true').collect { SkillDef skill -> skill.id }.toSet()
        if (finalizing || processedImportIds != enabledImportIds) {
            // Roll back credit/awards and retain PENDING for durable retry.
            throw new IllegalStateException("Catalog finalization changed during backfill for [${projectId}]")
        }
    }

    private void copyCredit(String projectId, String userId, List<SkillDef> imports = null) {
        long started = System.nanoTime()
        if (imports == null) {
            imports = skillDefRepo.findAllByProjectIdAndTypeAndEnabledAndCopiedFromIsNotNull(
                    projectId, SkillDef.ContainerType.Skill, 'true')
        }
        if (!imports) {
            log.debug('Skipping catalog credit copy without enabled imports: projectId=[{}], userId=[{}]', projectId, userId)
            return
        }
        // Restore external imports before same-project reuse, which can reference those rows.
        List<SkillDef> orderedImports = imports.findAll { SkillDef skill -> skill.copiedFromProjectId != projectId } +
                imports.findAll { SkillDef skill -> skill.copiedFromProjectId == projectId }
        orderedImports.each { SkillDef skill ->
            importedCreditService.reconcileSkillCredit(userId, skillDefRepo.findSkillDefMinById(skill.id))
        }
        importedCreditService.reconcileTotalsAndAchievements(projectId, userId, imports)
        log.debug('Finished catalog credit reconciliation in current transaction: projectId=[{}], userId=[{}], imports=[{}], elapsedMs=[{}]',
                projectId, userId, imports.size(), (System.nanoTime() - started) / 1_000_000L)
    }
}
