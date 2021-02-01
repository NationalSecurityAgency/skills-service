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
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillExceptionBuilder
import skills.services.LockingService
import skills.services.SelfReportingService
import skills.services.UserEventService
import skills.services.events.pointsAndAchievements.PointsAndAchievementsHandler
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints
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

    @Transactional
    @Profile
    SkillEventResult reportSkill(String projectId, String skillId, String userId, Boolean notifyIfNotApplied, Date incomingSkillDate, String approvalRequestedMsg) {
        SkillEventResult result = reportSkillInternal(projectId, skillId, userId, incomingSkillDate, approvalRequestedMsg)
        if (notifyIfNotApplied || result.skillApplied) {
            skillEventPublisher.publishSkillUpdate(result, userId)
        }
        metricsLogger.log("Reported Skills [${projectId}]", ['skillId': skillId]);
        return result
    }

    @Transactional
    protected void notifyUserOfAchievements(String userId){
        try {
            List<UserAchievement> pendingNotificationAchievements = achievedLevelRepo.findAllByUserIdAndNotifiedOrderByCreatedAsc(userId, Boolean.FALSE.toString())

            SkillEventResult ser

            pendingNotificationAchievements?.each {
                SkillEventsSupportRepo.SkillDefMin skill

                if(it.projectId) {
                    skill = skillEventsSupportRepo.findByProjectIdAndSkillId(it.projectId, it.skillId)
                } else {
                    skill = skillEventsSupportRepo.findBySkillIdWhereProjectIdIsNull(it.skillId)
                }

                if (!ser) {
                    ser = new SkillEventResult(projectId: it.projectId, skillId: it.skillId, name: skill.name)
                    ser.completed = []
                }

                CompletionItem completionItem
                if (it.level != null) {
                    Date day = it.created.clearTime()
                    UserPoints points = userPointsRepo.findByProjectIdAndUserIdAndSkillIdAndDay(it.projectId, userId, it.skillId, day)

                    if (points) {
                        completionItem = new CompletionItem(
                                level: it.level, name: skill.name,
                                id: points.skillId ?: "OVERALL",
                                type: points.skillId ? CompletionItemType.Subject : CompletionItemType.Overall)
                    }
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

    private SkillEventResult reportSkillInternal(String projectId, String skillId, String userId, Date incomingSkillDateParam, String approvalRequestedMsg) {
        assert projectId
        assert skillId

        SkillDate skillDate = new SkillDate(date: incomingSkillDateParam ?: new Date(), isProvided: incomingSkillDateParam != null)

        SkillEventsSupportRepo.SkillDefMin skillDefinition = getSkillDef(userId, projectId, skillId)

        SkillEventResult res = new SkillEventResult(projectId: projectId, skillId: skillId, name: skillDefinition.name)

        userEventService.recordEvent(projectId, skillDefinition.id, userId, skillDate.date)

        long numExistingSkills = getNumExistingSkills(userId, projectId, skillId)
        AppliedCheckRes checkRes = checkIfSkillApplied(userId, numExistingSkills, skillDate.date, skillDefinition)
        if (!checkRes.skillApplied) {
            res.skillApplied = checkRes.skillApplied
            res.explanation = checkRes.explanation
            return res
        }

        /**
         * Check if skill needs to be applied, if so then we'll need to db-lock to enforce cross-service lock;
         * once transaction is locked must redo all of the checks
         */
        lockTransaction(projectId, userId)
        numExistingSkills = getNumExistingSkills(userId, projectId, skillId)
        checkRes = checkIfSkillApplied(userId, numExistingSkills, skillDate.date, skillDefinition)
        if (!checkRes.skillApplied) {
            res.skillApplied = checkRes.skillApplied
            res.explanation = checkRes.explanation
            return res
        }

        if (skillDefinition.getSelfReportingType() == SkillDef.SelfReportingType.Approval) {
            selfReportingService.requestApproval(userId, skillDefinition, skillDate.date, approvalRequestedMsg)
            res.skillApplied = false
            res.explanation = "Skill was submitted for approval"
            return res
        }

        UserPerformedSkill performedSkill = new UserPerformedSkill(userId: userId, skillId: skillId, projectId: projectId, performedOn: skillDate.date, skillRefId: skillDefinition.id)
        savePerformedSkill(performedSkill)

        res.pointsEarned = skillDefinition.pointIncrement

        List<CompletionItem> achievements = pointsAndAchievementsHandler.updatePointsAndAchievements(userId, skillDefinition, skillDate)
        if (achievements) {
            res.completed.addAll(achievements)
        }

        boolean requestedSkillCompleted = hasReachedMaxPoints(numExistingSkills + 1, skillDefinition)
        if (requestedSkillCompleted) {
            documentSkillAchieved(userId, numExistingSkills, skillDefinition, res, skillDate)
            achievedBadgeHandler.checkForBadges(res, userId, skillDefinition, skillDate)
        }

        // if requestedSkillCompleted OR overall level achieved, then need to check for global badges
        boolean overallLevelAchieved = res.completed.find { it.level != null && it.type == CompletionItemType.Overall }
        if (requestedSkillCompleted || overallLevelAchieved) {
            achievedGlobalBadgeHandler.checkForGlobalBadges(res, userId, projectId, skillDefinition)
        }

        return res
    }

    @Profile
    private void lockTransaction(String projectId, String userId) {
        UserPoints userPoints = lockingService.lockUserPoints(projectId, userId)
        if (!userPoints) {
            lockingService.lockUser(userId)
        }
    }

    class AppliedCheckRes {
        boolean skillApplied = true
        String explanation
    }

    @Profile
    private AppliedCheckRes checkIfSkillApplied(String userId, long numExistingSkills, Date incomingSkillDate, SkillEventsSupportRepo.SkillDefMin skillDefinition) {
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
    private SkillEventsSupportRepo.SkillDefMin getSkillDef(String userId, String projectId, String skillId) {
        SkillEventsSupportRepo.SkillDefMin skillDefinition = skillEventsSupportRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, SkillDef.ContainerType.Skill)
        if (!skillDefinition) {
            throw new SkillExceptionBuilder()
                    .msg("Failed to report skill event because skill definition does not exist.")
                    .logLevel(SkillException.SkillExceptionLogLevel.WARN)
                    .printStackTrace(false)
                    .doNotRetry(true)
                    .projectId(projectId).skillId(skillId).userId(userId).build()
        }
        return skillDefinition
    }

    @Profile
    private void documentSkillAchieved(String userId, long numExistingSkills, SkillEventsSupportRepo.SkillDefMin skillDefinition, SkillEventResult res, SkillDate skillDate) {
        Date achievedOn = getAchievedOnDate(userId, skillDefinition, skillDate)
        UserAchievement skillAchieved = new UserAchievement(userId: userId.toLowerCase(), projectId: skillDefinition.projectId, skillId: skillDefinition.skillId, skillRefId: skillDefinition?.id,
                pointsWhenAchieved: ((numExistingSkills.intValue() + 1) * skillDefinition.pointIncrement), achievedOn: achievedOn)
        achievedLevelRepo.save(skillAchieved)
        res.completed.add(new CompletionItem(type: CompletionItemType.Skill, id: skillDefinition.skillId, name: skillDefinition.name))
    }

    @Profile
    private Date getAchievedOnDate(String userId, SkillEventsSupportRepo.SkillDefMin skillDefinition, SkillDate skillDate) {
        if (!skillDate.isProvided) {
            return skillDate.date
        }
        Date achievedOn = skillEventsSupportRepo.getUserPerformedSkillLatestDate(userId.toLowerCase(), skillDefinition.projectId, skillDefinition.skillId)
        if (!achievedOn || skillDate.date.after(achievedOn)) {
            achievedOn = skillDate.date
        }
        return achievedOn
    }

    private boolean hasReachedMaxPoints(long numSkills, SkillEventsSupportRepo.SkillDefMin skillDefinition) {
        return numSkills * skillDefinition.pointIncrement >= skillDefinition.totalPoints
    }

}
