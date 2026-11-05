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
import org.springframework.stereotype.Component
import skills.storage.model.SkillDefMin
import skills.storage.model.SkillRelDef
import skills.storage.model.UserAchievement
import skills.storage.repos.SkillEventsSupportRepo
import skills.storage.repos.UserAchievedLevelRepo

@Component
@CompileStatic
@Slf4j
class AchievedGlobalBadgeHandler {

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Profile
    void checkForGlobalBadges(SkillEventResult res, String userId, String projectId, SkillDefMin currentSkillDef) {
        reconcileGlobalBadges(res, userId,
                skillEventsSupportRepo.findGlobalBadgesForProjectIdAndSkillId(projectId, currentSkillDef.skillId))
    }

    // Credit reductions can invalidate a project-level requirement even when no
    // directly required skill was completed or lost. Run after totals and levels settle.
    @Profile
    void reconcileGlobalBadgesForProject(String userId, String projectId) {
        reconcileGlobalBadges(new SkillEventResult(), userId,
                skillEventsSupportRepo.findGlobalBadgesForProjectIdAndSkillId(projectId, null))
    }

    private void reconcileGlobalBadges(SkillEventResult res, String userId, List<SkillDefMin> globalBadges) {
        if (globalBadges) {
            // make the processing order predictable
            globalBadges = globalBadges.sort { it.skillId }

            for (SkillDefMin globalBadge : globalBadges) {
                if(globalBadge.enabled != null && !Boolean.valueOf(globalBadge.enabled)) {
                    log.debug("global badge [{}] isn't enabled yet, cannot be checked for achievement", globalBadge.skillId)
                    continue;
                }
                boolean qualifies = achievedLevelRepo.didUserAchieveAllGlobalBadgeLevels(globalBadge.skillId, userId) &&
                        achievedLevelRepo.countNonAchievedGlobalSkills(userId, globalBadge.skillId, SkillRelDef.RelationshipType.BadgeRequirement) == 0
                if (qualifies) {
                    if (!achievedLevelRepo.existsByUserIdAndSkillIdAndProjectIdIsNull(userId, globalBadge.skillId)) {
                        UserAchievement groupAchievement = new UserAchievement(userId: userId.toLowerCase(), projectId: null,
                                skillId: globalBadge.skillId, skillRefId: globalBadge?.id, achievedOn: new Date())
                        achievedLevelRepo.save(groupAchievement)
                        res.completed.add(new CompletionItem(type: CompletionTypeUtil.getCompletionType(globalBadge.type), id: globalBadge.skillId, name: globalBadge.name))
                    }
                } else {
                    achievedLevelRepo.deleteAllBySkillRefIdAndUserId(globalBadge.id, userId)
                }
            }
        }
    }
}
