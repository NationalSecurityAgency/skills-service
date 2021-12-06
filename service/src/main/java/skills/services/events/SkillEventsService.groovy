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
package skills.services.events

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillExceptionBuilder
import skills.services.LockingService
import skills.services.SelfReportingService
import skills.services.UserEventService
import skills.services.admin.SkillCatalogService
import skills.services.admin.SkillsGroupAdminService
import skills.services.events.pointsAndAchievements.PointsAndAchievementsHandler
import skills.storage.model.QueuedSkillUpdate
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefMin
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints
import skills.storage.repos.QueuedSkillUpdateRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillEventsSupportRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo
import skills.storage.repos.UserPointsRepo
import skills.utils.MetricsLogger

import static skills.services.events.CompletionItem.CompletionItemType

@Service
@CompileStatic
@Slf4j
class SkillEventsService {

    private static final String PENDING_NOTIFICATION_EXPLANATION = "Achieved due to a modification " +
            "in the training profile (such as: skill deleted, occurrences modified, badge published, etc..)"

    @Autowired
    UserPerformedSkillRepo performedSkillRepository

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    TimeWindowHelper timeWindowHelper

    @Autowired
    CheckDependenciesHelper checkDependenciesHelper

    @Autowired
    PointsAndAchievementsHandler pointsAndAchievementsHandler

    @Autowired
    AchievedBadgeHandler achievedBadgeHandler

    @Autowired
    AchievedGlobalBadgeHandler achievedGlobalBadgeHandler

    @Autowired
    AchievedSkillsGroupHandler achievedSkillsGroupHandler

    @Autowired
    LockingService lockingService

    @Autowired
    SkillEventPublisher skillEventPublisher

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    MetricsLogger metricsLogger;

    @Autowired
    UserEventService userEventService

    @Autowired
    SelfReportingService selfReportingService

    @Autowired
    SkillsGroupAdminService skillsGroupAdminService

    @Autowired
    SkillCatalogService skillCatalogService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    QueuedSkillUpdateRepo queuedSkillUpdateRepo

    static class SkillApprovalParams {
        boolean disableChecks = false
        String approvalRequestedMsg

        SkillApprovalParams(){}

        SkillApprovalParams(String approvalRequestedMsg) {
            this.approvalRequestedMsg = approvalRequestedMsg
        }
    }
    static SkillApprovalParams defaultSkillApprovalParams = new SkillApprovalParams()

    @Transactional
    @Profile
    SkillEventResult reportSkill(String projectId, String skillId, String userId, Boolean notifyIfNotApplied, Date incomingSkillDate, SkillApprovalParams skillApprovalParams = defaultSkillApprovalParams) {
        SkillEventResult result = reportSkillInternal(projectId, skillId, userId, incomingSkillDate, skillApprovalParams)
        if (notifyIfNotApplied || result.skillApplied) {
            skillEventPublisher.publishSkillUpdate(result, userId)
        }
        return result
    }

    @Transactional
    protected void notifyUserOfAchievements(String userId){
        try {
            List<UserAchievement> pendingNotificationAchievements = achievedLevelRepo.findAllByUserIdAndNotifiedOrderByCreatedAsc(userId, Boolean.FALSE.toString())

            SkillEventResult ser

            pendingNotificationAchievements?.each {
                SkillDefMin skill

                if(it.projectId && it.skillId) {
                    skill = skillEventsSupportRepo.findByProjectIdAndSkillId(it.projectId, it.skillId)
                } else if (it.skillId) {
                    skill = skillEventsSupportRepo.findBySkillIdWhereProjectIdIsNull(it.skillId)
                }

                if (!ser) {
                    ser = new SkillEventResult(projectId: it.projectId, skillId: it.skillId, name: skill? skill.name : 'OVERALL')
                    ser.completed = []
                }

                CompletionItem completionItem
                if (it.level != null) {
                    Date day = it.created.clearTime()
                    UserPoints points = userPointsRepo.findByProjectIdAndUserIdAndSkillIdAndDay(it.projectId, userId, it.skillId, day)

                    completionItem = new CompletionItem(
                            level: it.level, name: skill?.name ?: "OVERALL",
                            id: points?.skillId ?: "OVERALL",
                            type: points?.skillId ? CompletionItemType.Subject : CompletionItemType.Overall)
                } else {
                    if(SkillDef.ContainerType.Skill == skill.type) {
                        completionItem = new CompletionItem(type: CompletionItemType.Skill, id: skill.skillId, name: skill.name)
                    } else {
                        //why doesn't CompletionTypeUtil support Skill?
                        completionItem = new CompletionItem(type: CompletionTypeUtil.getCompletionType(skill.type), id: skill.skillId, name: skill.name)
                    }
                }

                if (completionItem) {
                    ser.completed.add(completionItem)
                }

                it.notified = Boolean.TRUE.toString()
            }

            if (ser) {
                ser.explanation = PENDING_NOTIFICATION_EXPLANATION
                skillEventPublisher.publishSkillUpdate(ser, userId)
                achievedLevelRepo.saveAll(pendingNotificationAchievements)
            }
        } catch (Exception e) {
            log.error("unable to notify user [${userId}] of pending achievements", e)
            throw e
        }
    }

    @Async
    public void identifyPendingNotifications(String userId) {
        notifyUserOfAchievements(userId)
    }

    private SkillEventResult reportSkillInternal(String projectId, String skillId, String userId, Date incomingSkillDateParam, SkillApprovalParams approvalParams) {
        assert projectId
        assert skillId

        SkillDate skillDate = new SkillDate(date: incomingSkillDateParam ?: new Date(), isProvided: incomingSkillDateParam != null)

        SkillDefMin skillDefinition = getSkillDef(userId, projectId, skillId)
        skillDefinition
        final boolean isCatalogSkill = skillCatalogService.isAvailableInCatalog(skillDefinition.projectId, skillDefinition.skillId)
        if (Boolean.valueOf(skillDefinition.readOnly) && !skillDefinition.selfReportingType) {
            throw new SkillException("Skills imported from the catalog can only be reported if the original skill is configured for Self Reporting", projectId, skillId, ErrorCode.ReadOnlySkill)
        }

        SkillEventResult res = new SkillEventResult(projectId: projectId, skillId: skillId, name: skillDefinition.name)

        metricsLogger.log([
                'skillId': skillId,
                'projectId': projectId,
                'requestedUserId': userId,
                'selfReported': (skillDefinition.getSelfReportingType() !== null).toString(),
                'selfReportType': skillDefinition.getSelfReportingType()?.toString(),
        ]);

        long numExistingSkills = getNumExistingSkills(userId, projectId, skillId)
        AppliedCheckRes checkRes = checkIfSkillApplied(userId, numExistingSkills, skillDate.date, skillDefinition)
        if (!checkRes.skillApplied) {
            // record event should happen AFTER the lock OR if it does not need the lock;
            // otherwise there is a chance of a deadlock (although unlikely); this can happen because record event
            // mutates the row - so that row is locked in addition to the explicit lock
            recordEvent(skillDefinition, userId, skillDate, isCatalogSkill)

            res.skillApplied = checkRes.skillApplied
            res.explanation = checkRes.explanation
            return res
        }

        /**
         * Check if skill needs to be applied, if so then we'll need to db-lock to enforce cross-service lock;
         * once transaction is locked must redo all of the checks
         */
        lockTransaction(userId)

        // record event should happen AFTER the lock OR if it does not need the lock;
        // otherwise there is a chance of a deadlock (although unlikely); this can happen because record event
        // mutates the row - so that row is locked in addition to the explicit lock
        recordEvent(skillDefinition, userId, skillDate, isCatalogSkill)
        numExistingSkills = getNumExistingSkills(userId, projectId, skillId)
        checkRes = checkIfSkillApplied(userId, numExistingSkills, skillDate.date, skillDefinition)
        if (!checkRes.skillApplied) {
            res.skillApplied = checkRes.skillApplied
            res.explanation = checkRes.explanation
            return res
        }

        if (approvalParams && !approvalParams.disableChecks &&
            skillDefinition.getSelfReportingType() == SkillDef.SelfReportingType.Approval) {
            // if this skill was imported from the catalog, request approval using the original OG
            // skill id to prevent duplicated approval requests for what is effectively the same skill
            if (skillDefinition.copiedFrom) {
                skillDefinition = skillDefRepo.findSkillDefMinById(skillDefinition.copiedFrom)
            }
            checkRes = selfReportingService.requestApproval(userId, skillDefinition, skillDate.date, approvalParams?.approvalRequestedMsg)
            res.skillApplied = checkRes.skillApplied
            res.explanation = checkRes.explanation
            return res
        }

        // capture res for the reported skillDefinition but perform this loop
        // for each copy including og if in catalog
        Closure<SkillEventResult> recordSkillOccurrence = { SkillDefMin sd,  SkillEventResult r ->
            UserPerformedSkill performedSkill = new UserPerformedSkill(userId: userId, skillId: skillId, projectId: sd.projectId, performedOn: skillDate.date, skillRefId: sd.id)
            savePerformedSkill(performedSkill)

            r.pointsEarned = skillDefinition.pointIncrement

            List<CompletionItem> achievements = pointsAndAchievementsHandler.updatePointsAndAchievements(userId, sd, skillDate)
            if (achievements) {
                r.completed.addAll(achievements)
            }

            boolean requestedSkillCompleted = hasReachedMaxPoints(numExistingSkills + 1, sd)
            if (requestedSkillCompleted) {
                documentSkillAchieved(userId, numExistingSkills, sd, res, skillDate)
                achievedBadgeHandler.checkForBadges(res, userId, sd, skillDate)
                achievedSkillsGroupHandler.checkForSkillsGroup(res, userId, sd, skillDate)
            }

            // if requestedSkillCompleted OR overall level achieved, then need to check for global badges
            boolean overallLevelAchieved = r.completed.find { it.level != null && it.type == CompletionItemType.Overall }
            if (requestedSkillCompleted || overallLevelAchieved) {
                achievedGlobalBadgeHandler.checkForGlobalBadges(r, userId, sd.projectId, sd)
            }
            return r
        }

        res = recordSkillOccurrence(skillDefinition, res)
        //now do it for each of the skills if in catalog or from catalog
        if (isCatalogSkill || skillDefinition.copiedFrom != null) {
            def relatedSkills = skillCatalogService.getRelatedSkills(skillDefinition)
            //TODO: make async
            relatedSkills?.each {
                recordSkillOccurrence(it, new SkillEventResult())
            }
        }

        return res
    }

    @Profile
    private void recordEvent(SkillDefMin skillDefinition, String userId, SkillDate skillDate, boolean isCatalogSkill=false) {
        if (skillDefinition.getCopiedFrom() != null || isCatalogSkill) {
            List<SkillDefMin> toBeRecorded = []
            if (isCatalogSkill) {
                List<SkillDefMin> copies = skillCatalogService.getSkillsCopiedFrom(skillDefinition.id)
                if (copies) {
                    toBeRecorded.addAll(copies)
                }
                toBeRecorded.add(skillDefinition)
            } else {
                //get og skill, record event for og and all copies except for skillDefinition
                SkillDefMin og = skillDefRepo.findSkillDefMinById(skillDefinition.copiedFrom)
                List<SkillDefMin> copies = skillCatalogService.getSkillsCopiedFrom(og.id)
                toBeRecorded.add(og)
                //not working
                if (copies) {
                    toBeRecorded.addAll(copies)
                    toBeRecorded.removeIf { it.id == skillDefinition.id }
                }
            }
            toBeRecorded?.each {
                userEventService.recordEvent(it.projectId, it.id, userId, skillDate.date)
            }
        }
        userEventService.recordEvent(skillDefinition.projectId, skillDefinition.id, userId, skillDate.date)
    }

    @Profile
    private void recordEvent(String projectId, Integer rawSkillDefId, String userId, SkillDate skillDate) {
        userEventService.recordEvent(projectId, rawSkillDefId, userId, skillDate.date)
    }

    @Profile
    private void lockTransaction(String userId) {
        log.debug("locking user [{}]", userId)
        lockingService.lockUser(userId)
    }

    static class AppliedCheckRes {
        boolean skillApplied = true
        String explanation
    }

    @Profile
    private AppliedCheckRes checkIfSkillApplied(String userId, long numExistingSkills, Date incomingSkillDate, SkillDefMin skillDefinition) {
        AppliedCheckRes res = new AppliedCheckRes()
        if (hasReachedMaxPoints(numExistingSkills, skillDefinition)) {
            res.skillApplied = false
            res.explanation = "This skill reached its maximum points"
            return res
        }

        TimeWindowHelper.TimeWindowRes timeWindowRes = timeWindowHelper.checkTimeWindow(skillDefinition, userId, incomingSkillDate)
        if (timeWindowRes.isFull()) {
            res.skillApplied = false
            res.explanation = timeWindowRes.msg
            return res
        }

        CheckDependenciesHelper.DependencyCheckRes dependencyCheckRes = checkDependenciesHelper.check(userId, skillDefinition.projectId, skillDefinition.skillId)
        if (dependencyCheckRes.hasNotAchievedDependents) {
            res.skillApplied = false
            res.explanation = dependencyCheckRes.msg
            return res
        }

        if (skillDefinition.groupId && (!Boolean.valueOf(skillDefinition.enabled) || !skillsGroupAdminService.isParentSkillsGroupEnabled(skillDefinition.projectId, skillDefinition.groupId))) {
            res.skillApplied = false
            res.explanation = "This skill belongs to a Skill Group that is not yet enabled"
            return res
        }
        return  res
    }

    @Profile
    private void savePerformedSkill(UserPerformedSkill performedSkill) {
        performedSkillRepository.save(performedSkill)
        log.debug("Saved skill [{}]", performedSkill)
    }

    @Profile
    private long getNumExistingSkills(String userId, String projectId, String skillId) {
        Long numExistingSkills = performedSkillRepository.countByUserIdAndProjectIdAndSkillId(userId, projectId, skillId)
        return numExistingSkills ?: 0 // account for null
    }

    @Profile
    private SkillDefMin getSkillDef(String userId, String projectId, String skillId) {
        SkillDefMin skillDefinition = skillEventsSupportRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, SkillDef.ContainerType.Skill)
        if (!skillDefinition) {
            throw new SkillExceptionBuilder()
                    .msg("Failed to report skill event because skill definition does not exist.")
                    .logLevel(SkillException.SkillExceptionLogLevel.WARN)
                    .printStackTrace(false)
                    .doNotRetry(true)
                    .errorCode(ErrorCode.SkillNotFound)
                    .projectId(projectId).skillId(skillId).userId(userId).build()
        }
        return skillDefinition
    }

    @Profile
    private void documentSkillAchieved(String userId, long numExistingSkills, SkillDefMin skillDefinition, SkillEventResult res, SkillDate skillDate) {
        Date achievedOn = getAchievedOnDate(userId, skillDefinition, skillDate)
        UserAchievement skillAchieved = new UserAchievement(userId: userId.toLowerCase(), projectId: skillDefinition.projectId, skillId: skillDefinition.skillId, skillRefId: skillDefinition?.id,
                pointsWhenAchieved: ((numExistingSkills.intValue() + 1) * skillDefinition.pointIncrement), achievedOn: achievedOn)
        achievedLevelRepo.save(skillAchieved)
        res.completed.add(new CompletionItem(type: CompletionItemType.Skill, id: skillDefinition.skillId, name: skillDefinition.name))
    }

    @Profile
    private Date getAchievedOnDate(String userId, SkillDefMin skillDefinition, SkillDate skillDate) {
        if (!skillDate.isProvided) {
            return skillDate.date
        }
        Date achievedOn = skillEventsSupportRepo.getUserPerformedSkillLatestDate(userId.toLowerCase(), skillDefinition.projectId, skillDefinition.skillId)
        if (!achievedOn || skillDate.date.after(achievedOn)) {
            achievedOn = skillDate.date
        }
        return achievedOn
    }

    private boolean hasReachedMaxPoints(long numSkills, SkillDefMin skillDefinition) {
        return numSkills * skillDefinition.pointIncrement >= skillDefinition.totalPoints
    }

}
