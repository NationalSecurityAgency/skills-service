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
import skills.services.RuleSetDefGraphService
import skills.services.UserAchievementsAndPointsManagement
import skills.services.events.AchievedBadgeHandler
import skills.services.events.SkillDate
import skills.services.events.SkillEventResult
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefMin
import skills.storage.repos.SkillDefRepo

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
    LockingService lockingService

    void handleAchievementsForImportedSkills(String userId, SkillDefMin skill, SkillDate incomingSkillDate, boolean thisRequestCompletedOriginalSkill) {
        if (log.isDebugEnabled()) {
            log.debug("userId=[${userId}], skill=[${skill.skillId}], incomingSkillDate=[${incomingSkillDate}], thisRequestCompletedOriginalSkill=[${thisRequestCompletedOriginalSkill}]")
        }
        lockTransaction(userId, skill.projectId)
        // handle user points and level achievements
        pointsAndAchievementsHandler.updatePointsAndAchievements(userId, skill, incomingSkillDate)

        if (thisRequestCompletedOriginalSkill) {
            SkillEventResult mockResForBadgeCheck = new SkillEventResult()
            pointsAndAchievementsHandler.documentSkillAchieved(userId, skill, mockResForBadgeCheck, incomingSkillDate)
            achievedBadgeHandler.checkForBadges(mockResForBadgeCheck, userId, skill, incomingSkillDate)
        }
    }

    @Profile
    private void lockTransaction(String userId, String project) {
        log.debug("locking user [{}]-[{}]", userId, project)
        lockingService.lockForSkillReporting(userId, project)
    }
}
