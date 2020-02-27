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
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillExceptionBuilder
import skills.services.LockingService
import skills.services.events.pointsAndAchievements.PointsAndAchievementsHandler
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints
import skills.storage.repos.SkillEventsSupportRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo

import static skills.services.events.CompletionItem.CompletionItemType

@Service
@CompileStatic
@Slf4j
class SkillEventsService {

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

    @Transactional
    @Profile
    SkillEventResult reportSkill(String projectId, String skillId, String userId, Boolean notifyIfNotApplied, Date incomingSkillDate = new Date()) {
        SkillEventResult result = reportSkillInternal(projectId, skillId, userId, incomingSkillDate)
        if (notifyIfNotApplied || result.skillApplied) {
            skillEventPublisher.publishSkillUpdate(result, userId)
        }
        return result
    }

    private SkillEventResult reportSkillInternal(String projectId, String skillId, String userId, Date incomingSkillDate) {
        assert projectId
        assert skillId

        SkillEventsSupportRepo.SkillDefMin skillDefinition = getSkillDef(userId, projectId, skillId)

        SkillEventResult res = new SkillEventResult(projectId: projectId, skillId: skillId, name: skillDefinition.name)

        long numExistingSkills = getNumExistingSkills(userId, projectId, skillId)
        AppliedCheckRes checkRes = checkIfSkillApplied(userId, numExistingSkills, incomingSkillDate, skillDefinition)
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
        checkRes = checkIfSkillApplied(userId, numExistingSkills, incomingSkillDate, skillDefinition)
        if (!checkRes.skillApplied) {
            res.skillApplied = checkRes.skillApplied
            res.explanation = checkRes.explanation
            return res
        }

        UserPerformedSkill performedSkill = new UserPerformedSkill(userId: userId, skillId: skillId, projectId: projectId, performedOn: incomingSkillDate, skillRefId: skillDefinition.id)
        savePerformedSkill(performedSkill)

        res.pointsEarned = skillDefinition.pointIncrement

        List<CompletionItem> achievements = pointsAndAchievementsHandler.updatePointsAndAchievements(userId, skillDefinition, incomingSkillDate)
        if (achievements) {
            res.completed.addAll(achievements)
        }

        boolean requestedSkillCompleted = hasReachedMaxPoints(numExistingSkills + 1, skillDefinition)
        if (requestedSkillCompleted) {
            documentSkillAchieved(userId, numExistingSkills, skillDefinition, res)
            achievedBadgeHandler.checkForBadges(res, userId, skillDefinition)
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
    private void documentSkillAchieved(String userId, long numExistingSkills, SkillEventsSupportRepo.SkillDefMin skillDefinition, SkillEventResult res) {
        UserAchievement skillAchieved = new UserAchievement(userId: userId.toLowerCase(), projectId: skillDefinition.projectId, skillId: skillDefinition.skillId, skillRefId: skillDefinition?.id,
                pointsWhenAchieved: ((numExistingSkills.intValue() + 1) * skillDefinition.pointIncrement))
        achievedLevelRepo.save(skillAchieved)
        res.completed.add(new CompletionItem(type: CompletionItemType.Skill, id: skillDefinition.skillId, name: skillDefinition.name))
    }

    private boolean hasReachedMaxPoints(long numSkills, SkillEventsSupportRepo.SkillDefMin skillDefinition) {
        return numSkills * skillDefinition.pointIncrement >= skillDefinition.totalPoints
    }

}
