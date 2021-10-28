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
import skills.controller.result.model.GlobalBadgeLevelRes
import skills.services.GlobalBadgesService
import skills.skillLoading.SkillsLoader
import skills.storage.model.SkillDefMin
import skills.storage.model.SkillRelDef
import skills.storage.model.UserAchievement
import skills.storage.repos.SkillEventsSupportRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.UserAchievedLevelRepo

import static skills.storage.repos.SkillEventsSupportRepo.*

@Component
@CompileStatic
@Slf4j
class AchievedGlobalBadgeHandler {

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Autowired
    GlobalBadgesService globalBadgesService

    @Autowired
    SkillsLoader skillsLoader


    @Profile
    void checkForGlobalBadges(SkillEventResult res, String userId, String projectId, SkillDefMin currentSkillDef) {
        List<SkillDefMin> globalBadges = skillEventsSupportRepo.findGlobalBadgesForProjectIdAndSkillId(projectId, currentSkillDef.skillId)
        if (globalBadges) {

            List<UserAchievement> achievements = achievedLevelRepo.findAllLevelsByUserId(userId)
            Map<String, Integer> userProjectLevels = (Map<String, Integer>)achievements?.findAll {!it.skillId}?.groupBy { it.projectId }
                    ?.collectEntries {String key, List<UserAchievement> val -> [key,val.collect{it.level}.max()]}

            badgeCheckLoop: for (SkillDefMin globalBadge : globalBadges) {
                if(globalBadge.enabled != null && !Boolean.valueOf(globalBadge.enabled)) {
                    log.debug("global badge [{}] isn't enabled yet, cannot be checked for achievement", globalBadge.skillId)
                    continue;
                }
                // first check required project levels
                List<GlobalBadgeLevelRes> requiredLevels = globalBadgesService.getGlobalBadgeLevels(globalBadge.skillId)
                for (GlobalBadgeLevelRes requiredLevel : requiredLevels) {
                    if (userProjectLevels.containsKey(requiredLevel.projectId)) {
                        Integer achievedProjectLevel = userProjectLevels.get(requiredLevel.projectId)
                        if (!(achievedProjectLevel >= requiredLevel.level)) {
                            break badgeCheckLoop
                        }
                    } else {
                        break badgeCheckLoop
                    }
                }

                // all project level requirements met, check required skills
                Long nonAchievedChildren = achievedLevelRepo.countNonAchievedGlobalSkills(userId, globalBadge.skillId, SkillRelDef.RelationshipType.BadgeRequirement)
                if (nonAchievedChildren == 0) {
                    List<UserAchievement> badges = achievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(userId, globalBadge.projectId, globalBadge.skillId)
                    if (!badges) {
                        UserAchievement groupAchievement = new UserAchievement(userId: userId.toLowerCase(), projectId: globalBadge.projectId,
                                skillId: globalBadge.skillId, skillRefId: globalBadge?.id, achievedOn: new Date())
                        achievedLevelRepo.save(groupAchievement)
                        res.completed.add(new CompletionItem(type: CompletionTypeUtil.getCompletionType(globalBadge.type), id: globalBadge.skillId, name: globalBadge.name))
                    }
                }
            }
        }
    }

    @Profile
    private List<TinyUserAchievement> loadOverallLevelAchievements(String userId, String projectId, List<Integer> skillRefIds) {
        skillEventsSupportRepo.findTinyUserAchievementsByUserIdAndProjectId(userId, projectId)
    }
}
