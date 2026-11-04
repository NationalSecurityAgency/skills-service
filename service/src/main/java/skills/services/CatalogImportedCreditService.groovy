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
import skills.services.events.AchievedBadgeHandler
import skills.services.events.AchievedGlobalBadgeHandler
import skills.services.events.SkillDate
import skills.services.events.SkillEventResult
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefMin
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.CatalogParticipationCleanupRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo
import skills.storage.repos.UserPointsRepo.ImportedCreditSnapshot
import skills.tasks.TaskSchedulerService

/** Synchronization requests use current source credit, never the increment in an old task. */
@Service
class CatalogImportedCreditService {
    @Autowired UserPointsRepo userPointsRepo
    @Autowired UserAchievedLevelRepo achievementRepo
    @Autowired SkillDefRepo skillDefRepo
    @Autowired CatalogParticipationCleanupRepo cleanupRepo
    @Autowired LockingService lockingService
    @Autowired RuleSetDefGraphService graphService
    @Autowired BatchOperationsTransactionalAccessor batchAccessor
    @Autowired UserAchievementsAndPointsManagement pointsManagement
    @Autowired @Lazy AchievedBadgeHandler badgeHandler
    @Autowired AchievedGlobalBadgeHandler globalBadgeHandler
    @Autowired @Lazy TaskSchedulerService taskSchedulerService

    // The caller checks enabled/participation/community/finalization state under the same lock.
    @Transactional
    void reconcile(String userId, SkillDefMin skill) {
        lockingService.lockForSkillReporting(userId, skill.projectId)
        if (reconcileSkillCredit(userId, skill)) {
            reconcileTotalsAndAchievements(skill.projectId, userId, [skillDefRepo.findById(skill.id).get()])
        }
    }

    // Backfill also uses this source snapshot/absolute-credit path, then recalculates totals once.
    // Both callers hold the destination user/project lock for the enclosing transaction.
    // explicitRepair is temporary pre-5.1 root Fix mode; retire its branches/argument with those endpoints.
    @Transactional
    boolean reconcileSkillCredit(String userId, SkillDefMin skill, boolean explicitRepair = false) {
        ImportedCreditSnapshot source = userPointsRepo.readImportedCredit(userId, skill.id)
        if (!source) {
            return false
        }
        UserPoints destination = userPointsRepo.findByProjectIdAndUserIdAndSkillId(skill.projectId, userId, skill.skillId)
        List<UserAchievement> awards = achievementRepo.findAllByUserIdAndProjectIdAndSkillId(userId, skill.projectId, skill.skillId)
        boolean creditChanged = (destination?.points ?: 0) != source.points ||
                (source.achievedOn != null) != !awards.isEmpty() ||
                awards.any { UserAchievement award -> award.achievedOn != source.achievedOn || award.pointsWhenAchieved != source.pointsWhenAchieved }
        boolean clearedByBatch = source.points == 0 && destination != null && source.achievedOn == null
        if (explicitRepair && !source.achievedOn && awards) {
            achievementRepo.deleteAll(awards)
        } else if (!source.achievedOn && awards && !clearedByBatch) {
            batchAccessor.removeCatalogSkillAchievements(userId, skill.projectId, [skill.id])
        } else if (source.achievedOn && !awards) {
            achievementRepo.save(new UserAchievement(userId: userId, projectId: skill.projectId,
                    skillId: skill.skillId, skillRefId: skill.id, achievedOn: source.achievedOn,
                    pointsWhenAchieved: source.pointsWhenAchieved))
        } else if (source.achievedOn) {
            awards.each { UserAchievement award ->
                if (award.achievedOn != source.achievedOn || award.pointsWhenAchieved != source.pointsWhenAchieved) {
                    award.achievedOn = source.achievedOn
                    award.pointsWhenAchieved = source.pointsWhenAchieved
                    achievementRepo.save(award)
                }
            }
        }
        if (source.points > 0) {
            userPointsRepo.reconcileImportedPoints(userId, skill.projectId, skill.skillId, skill.id,
                    source.points, source.performedOn ?: source.achievedOn ?: new Date())
        } else if (explicitRepair && destination) {
            // Repair must not enter ordinary deletion's global-badge or downstream scheduling paths.
            userPointsRepo.delete(destination)
        } else if (destination && !source.achievedOn) {
            batchAccessor.batchRemovePerformedSkillsForUserAndSpecificSkills(userId, skill.projectId, [skill.id])
        } else if (destination) {
            userPointsRepo.delete(destination)
        }
        if (creditChanged && !clearedByBatch && !explicitRepair) {
            // Same-project reuse can reference this imported row. Notify downstream copies
            // only when source state changed, so duplicate delivery does not fan out again.
            // Include disabled copies to cover changes committed after their activation snapshot.
            skillDefRepo.findSkillDefIdsByCopiedFromIncludingDisabled(skill.id).each { Integer copyId ->
                taskSchedulerService.scheduleImportedSkillAchievement(userId, copyId,
                        new SkillDate(date: new Date(), isProvided: false), false, false)
            }
        }
        return source.points > 0 || destination != null || source.achievedOn != null || !awards.isEmpty()
    }

    @Transactional
    void reconcileTotalsAndAchievements(String projectId, String userId, List<SkillDef> imports, boolean explicitRepair = false) {
        List<SkillDef> groups = explicitRepair ? skillDefRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.SkillsGroup) :
                imports.findAll { SkillDef skill -> skill.groupId != null }
                .collect { SkillDef skill -> graphService.getMyGroupParent(skill.id) }.unique { SkillDef group -> group.id }
        batchAccessor.identifyAndAddGroupAchievementsForSingleUser(userId, groups)
        List<SkillDef> subjects = explicitRepair ? skillDefRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.Subject) :
                imports.collect { SkillDef skill -> graphService.getMySubjectParent(skill.id) }.unique { SkillDef subject -> subject.id }
        if (explicitRepair) {
            subjects.each { SkillDef subject -> batchAccessor.createSubjectUserPointsForSingleNewUser(userId, projectId, subject.skillId) }
            cleanupRepo.repairContainerTotals(projectId, userId)
        }
        subjects.each { SkillDef subject ->
            if (!explicitRepair) {
                batchAccessor.createSubjectUserPointsForSingleNewUser(userId, projectId, subject.skillId)
                userPointsRepo.updateSubjectUserPointsForUser(userId, projectId, subject.skillId, true)
            }
            pointsManagement.removeSubjectLevelAchievementsIfThisUserDoesNotQualify(userId, subject)
            batchAccessor.identifyAndAddSubjectLevelAchievementsForSingleUser(userId, projectId, subject.skillId)
            batchAccessor.addMissingCatalogLevels(projectId, subject.skillId, userId)
        }
        batchAccessor.createProjectUserPointsForSingleNewUser(userId, projectId)
        if (explicitRepair) {
            cleanupRepo.repairProjectTotal(projectId, userId)
        } else {
            userPointsRepo.updateUserPointsForProjectAndUser(projectId, userId)
        }
        pointsManagement.removeProjectLevelAchievementsIfUserDoesNotQualify(userId, projectId)
        batchAccessor.identifyAndAddProjectLevelAchievements(userId, projectId)
        batchAccessor.addMissingCatalogLevels(projectId, null, userId)
        imports.each { SkillDef skill ->
            List<UserAchievement> achievements = achievementRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skill.skillId)
            SkillDefMin min = skillDefRepo.findSkillDefMinById(skill.id)
            if (achievements && !explicitRepair) {
                badgeHandler.checkForBadges(new SkillEventResult(), userId, min,
                        new SkillDate(date: achievements.first().achievedOn, isProvided: true))
            }
        }
        if (!explicitRepair) {
            globalBadgeHandler.reconcileGlobalBadgesForProject(userId, projectId)
        }
    }
}
