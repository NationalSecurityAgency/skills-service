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
import skills.services.RuleSetDefGraphService
import skills.services.UserAchievementsAndPointsManagement
import skills.services.events.AchievedBadgeHandler
import skills.services.events.AchievedSkillsGroupHandler
import skills.services.events.SkillDate
import skills.services.events.SkillEventResult
import skills.storage.model.SkillDefMin
import skills.storage.model.UserPerformedSkill
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserPerformedSkillRepo

@Component
@Slf4j
@CompileStatic
class ImportedSkillsAchievementsHandler {

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    PointsAndAchievementsHandler pointsAndAchievementsHandler

    @Autowired
    UserAchievementsAndPointsManagement userAchievementsAndPointsManagement

    @Autowired
    AchievedBadgeHandler achievedBadgeHandler

    @Autowired
    AchievedSkillsGroupHandler achievedSkillsGroupHandler

    @Autowired
    UserPerformedSkillRepo performedSkillRepository

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
        if (!Boolean.valueOf(skillDefRepo.findSkillDefMinById(skill.id).enabled)) {
            log.debug('Skipping imported achievement for disabled skill: projectId=[{}], skillId=[{}], userId=[{}]', skill.projectId, skill.skillId, userId)
            return
        }
        if (userCommunityService.isUserCommunityOnlyProject(skill.projectId) && !userCommunityService.isUserCommunityMember(userId)) {
            log.debug('Skipping imported achievement for non-community member: projectId=[{}], skillId=[{}], userId=[{}]', skill.projectId, skill.skillId, userId)
            return
        }
        if (skill.copiedFromProjectId != skill.projectId) {
            if (!participationService.isEligible(skill.projectId, userId)) {
                log.debug('Skipping imported achievement for ineligible participant: projectId=[{}], skillId=[{}], userId=[{}]', skill.projectId, skill.skillId, userId)
                return
            }
            if (participationRepo.readStatus(skill.projectId, userId) == BackfillStatus.PENDING) {
                log.debug('Blocking imported achievement while participant initialization is pending: projectId=[{}], skillId=[{}], userId=[{}]', skill.projectId, skill.skillId, userId)
                throw new IllegalStateException('Catalog participant initialization is pending')
            }
            if (backfillService.hasApplicableFinalization(skill.projectId)) {
                log.debug('Blocking imported achievement while catalog finalization is unfinished: projectId=[{}], skillId=[{}], userId=[{}]', skill.projectId, skill.skillId, userId)
                throw new IllegalStateException('Catalog participant initialization is pending')
            }
        }

        if (isMotivationalSkill && thisRequestCompletedOriginalSkill) {
            // just need to update the oldest performed_on date
            UserPerformedSkill userPerformedSkill = performedSkillRepository.findTopBySkillRefIdAndUserIdOrderByPerformedOnAsc(skill.id, userId)
            if (userPerformedSkill.performedOn.before(incomingSkillDate.date)) {
                userPerformedSkill.performedOn = incomingSkillDate.date
                performedSkillRepository.save(userPerformedSkill)
            } else {
                // this should never happen as this same check is made when the original skill was reported and the dates in theory should all line up
                log.warn("incomingSkillDate [${incomingSkillDate}] is before oldest user_performed_skill [${userPerformedSkill}]")
            }
        } else {
            // handle user points and level achievements
            pointsAndAchievementsHandler.updatePointsAndAchievements(userId, skill, incomingSkillDate)

            if (thisRequestCompletedOriginalSkill) {
                SkillEventResult mockResForBadgeAndGroupCheck = new SkillEventResult()
                pointsAndAchievementsHandler.documentSkillAchieved(userId, skill, mockResForBadgeAndGroupCheck, incomingSkillDate)
                achievedBadgeHandler.checkForBadges(mockResForBadgeAndGroupCheck, userId, skill, incomingSkillDate)
                achievedSkillsGroupHandler.checkForSkillsGroup(mockResForBadgeAndGroupCheck, userId, skill, incomingSkillDate)
            }
        }
    }

    @Profile
    private void lockTransaction(String userId, String project) {
        log.debug("locking user [{}]-[{}]", userId, project)
        lockingService.lockForSkillReporting(userId, project)
    }
}
