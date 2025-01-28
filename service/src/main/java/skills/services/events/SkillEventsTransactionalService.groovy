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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillExceptionBuilder
import skills.quizLoading.QuizRunService
import skills.services.LockingService
import skills.services.SelfReportingService
import skills.services.UserEventService
import skills.services.admin.SkillCatalogService
import skills.services.admin.SkillsGroupAdminService
import skills.services.attributes.SkillAttributeService
import skills.services.events.pointsAndAchievements.PointsAndAchievementsHandler
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.*
import skills.storage.repos.*
import skills.tasks.TaskSchedulerService
import skills.utils.MetricsLogger

import static skills.services.events.CompletionItem.CompletionItemType
import static skills.services.events.SkillEventsService.AppliedCheckRes
import static skills.services.events.SkillEventsService.SkillApprovalParams

@Service
@CompileStatic
@Slf4j
class SkillEventsTransactionalService {

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
    TaskSchedulerService taskSchedulerService

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    QuizRunService quizRunService

    @Transactional
    void notifyUserOfAchievements(String userId){
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
                    UserPoints points = userPointsRepo.findByProjectIdAndUserIdAndSkillId(it.projectId, userId, it.skillId)

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

    @Profile
    @Transactional
    SkillEventResult reportSkillInternal(String projectId, String skillId, String userId, Date incomingSkillDateParam, SkillApprovalParams approvalParams = SkillEventsService.defaultSkillApprovalParams) {
        assert projectId
        assert skillId

        SkillDate skillDate = new SkillDate(date: incomingSkillDateParam ?: new Date(), isProvided: incomingSkillDateParam != null)

        SkillDefMin skillDefinition = getSkillDef(userId, projectId, skillId)
        if (Boolean.valueOf(skillDefinition.readOnly) && !skillDefinition.selfReportingType) {
            throw new SkillException("Skills imported from the catalog can only be reported if the original skill is configured for Self Reporting", projectId, skillId, ErrorCode.ReadOnlySkill)
        }
        if (!approvalParams.isFromPassingQuiz && skillDefinition.selfReportingType == SkillDef.SelfReportingType.Quiz) {
            SkillException e = new SkillException("Cannot report skill events directly to a quiz-based skill. Can only achieve by completing quiz/survey.", projectId, skillId, ErrorCode.SkillEventForQuizSkillIsNotAllowed)
            e.doNotRetry = true
            throw e;
        }
        if (skillDefinition.selfReportingType && skillDefinition.copiedFromProjectId) {
            projectId = skillDefinition.copiedFromProjectId
            skillDefinition = getCopiedFromSkillDef(skillDefinition, userId)
            // override it as reused skill's id will not match
            skillId = skillDefinition.skillId
        }
        Boolean isMotivationalSkill = skillAttributeService.isMotivationalSkill(projectId, skillId)
        SkillEventResult res = new SkillEventResult(projectId: projectId, skillId: skillId, name: skillDefinition.name, selfReportType: skillDefinition.getSelfReportingType()?.toString())

        long numExistingSkills = getNumExistingSkills(userId, projectId, skillId)
        AppliedCheckRes checkRes = checkIfSkillApplied(userId, numExistingSkills, skillDate.date, skillDefinition, isMotivationalSkill)
        if (!checkRes.skillApplied) {
            // record event should happen AFTER the lock OR if it does not need the lock;
            // otherwise there is a chance of a deadlock (although unlikely); this can happen because record event
            // mutates the row - so that row is locked in addition to the explicit lock
            recordEvent(skillDefinition, userId, skillDate)

            res.skillApplied = checkRes.skillApplied
            res.explanation = checkRes.explanation
            return res
        }

        /**
         * Check if skill needs to be applied, if so then we'll need to db-lock to enforce cross-service lock;
         * once transaction is locked must redo all of the checks
         */
        lockTransaction(userId, projectId)

        final boolean isApprovalRequest = approvalParams && !approvalParams.disableChecks &&
                skillDefinition.getSelfReportingType() == SkillDef.SelfReportingType.Approval

        if (!isApprovalRequest) {
            // record event should happen AFTER the lock OR if it does not need the lock;
            // otherwise there is a chance of a deadlock (although unlikely); this can happen because record event
            // mutates the row - so that row is locked in addition to the explicit lock
            recordEvent(skillDefinition, userId, skillDate)
        }
        numExistingSkills = getNumExistingSkills(userId, projectId, skillId)
        checkRes = checkIfSkillApplied(userId, numExistingSkills, skillDate.date, skillDefinition, isMotivationalSkill)
        if (!checkRes.skillApplied) {
            res.skillApplied = checkRes.skillApplied
            res.explanation = checkRes.explanation
            return res
        }

        if (isApprovalRequest) {
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

        if (isMotivationalSkill && hasReachedMaxPoints(numExistingSkills, skillDefinition)) {
            UserPerformedSkill userPerformedSkill = performedSkillRepository.findTopBySkillRefIdAndUserIdOrderByPerformedOnAsc(skillDefinition.id, userId)
            if (userPerformedSkill.performedOn.before(skillDate.date)) {
                userPerformedSkill.performedOn = skillDate.date
                performedSkillRepository.save(userPerformedSkill)
                scheduleImportedSkills(skillDefinition, userId, skillDate, hasReachedMaxPoints(numExistingSkills, skillDefinition), true)
                res.skillApplied = true
                res.explanation = 'Skill Achievement retained'
                return res
            } else {
                log.warn("incomingSkillDate [${skillDate.date}] is before oldest user_performed_skill [${userPerformedSkill}]")
                res.skillApplied = false
                res.explanation = "This skill reached its maximum points"
                return res
            }
        }

        UserPerformedSkill performedSkill = new UserPerformedSkill(userId: userId,
                skillId: skillId, projectId: skillDefinition.projectId,
                performedOn: skillDate.date, skillRefId: skillDefinition.id)
        savePerformedSkill(performedSkill)

        res.pointsEarned = skillDefinition.pointIncrement
        res.totalPointsEarned = ((int)numExistingSkills + 1) * skillDefinition.pointIncrement
        res.totalPoints = skillDefinition.totalPoints
        res.numOccurrencesToCompletion = (int)(skillDefinition.totalPoints / skillDefinition.pointIncrement)

        List<CompletionItem> achievements = pointsAndAchievementsHandler.updatePointsAndAchievements(userId, skillDefinition, skillDate)
        if (achievements) {
            res.completed.addAll(achievements)
        }

        boolean requestedSkillCompleted = hasReachedMaxPoints(numExistingSkills + 1, skillDefinition)
        if (requestedSkillCompleted) {
            pointsAndAchievementsHandler.documentSkillAchieved(userId, skillDefinition, res, skillDate)
            achievedBadgeHandler.checkForBadges(res, userId, skillDefinition, skillDate)
            achievedSkillsGroupHandler.checkForSkillsGroup(res, userId, skillDefinition, skillDate)
            quizRunService.checkForDependentQuizzes(res, userId, skillDefinition)
        }

        // if requestedSkillCompleted OR overall level achieved, then need to check for global badges
        boolean overallLevelAchieved = res.completed.find { it.level != null && it.type == CompletionItemType.Overall }
        if (requestedSkillCompleted || overallLevelAchieved) {
            achievedGlobalBadgeHandler.checkForGlobalBadges(res, userId, skillDefinition.projectId, skillDefinition)
        }

        scheduleImportedSkills(skillDefinition, userId, skillDate, requestedSkillCompleted, false)

        return res
    }

    @Profile
    private void scheduleImportedSkills(SkillDefMin skillDefinition, String userId, SkillDate skillDate, boolean requestedSkillCompleted, boolean isMotivationalSkill) {
        List<Integer> importedSkillIds = skillDefRepo.findSkillDefIdsByCopiedFrom(skillDefinition.id)
        if (importedSkillIds) {
            importedSkillIds.each { Integer importedSkillId ->
                taskSchedulerService.scheduleImportedSkillAchievement(userId, importedSkillId, skillDate, requestedSkillCompleted, isMotivationalSkill)
            }
        }
    }

    @Profile
    private void recordEvent(SkillDefMin skillDefinition, String userId, SkillDate skillDate) {
        userEventService.recordEvent(skillDefinition.projectId, skillDefinition.id, userId, skillDate.date)
    }

    @Profile
    private void lockTransaction(String userId, String projectId) {
        log.debug("locking user-project [{}-{}]", userId, projectId)
        lockingService.lockForSkillReporting(userId, projectId)
    }

    @Profile
    private AppliedCheckRes checkIfSkillApplied(String userId, long numExistingSkills, Date incomingSkillDate, SkillDefMin skillDefinition, Boolean isMotivationalSkill) {
        AppliedCheckRes res = new AppliedCheckRes()
        if (!isMotivationalSkill && hasReachedMaxPoints(numExistingSkills, skillDefinition)) {
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
    private Long getNumExistingSkills(String userId, String projectId, String skillId) {
        Long numExistingSkills = performedSkillRepository.countByUserIdAndProjectIdAndSkillId(userId, projectId, skillId)
        return numExistingSkills ?: 0l // account for null
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
    private SkillDefMin getCopiedFromSkillDef(SkillDefMin skillDef, String userId) {
        SkillDefMin res = skillEventsSupportRepo.findBySkillRefId(skillDef.copiedFrom)
        if (!res) {
            throw new SkillExceptionBuilder()
                    .msg("Failed to report skill event because copied from skill definition does not exist.")
                    .logLevel(SkillException.SkillExceptionLogLevel.WARN)
                    .printStackTrace(false)
                    .doNotRetry(true)
                    .errorCode(ErrorCode.SkillNotFound)
                    .projectId(skillDef.projectId).skillId(skillDef.skillId).userId(userId).build()
        }
        return res
    }

    private boolean hasReachedMaxPoints(long numSkills, SkillDefMin skillDefinition) {
        return numSkills * skillDefinition.pointIncrement >= skillDefinition.totalPoints
    }
}
