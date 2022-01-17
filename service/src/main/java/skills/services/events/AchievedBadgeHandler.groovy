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
import skills.services.BadgeUtils
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefMin
import skills.storage.model.SkillRelDef
import skills.storage.model.UserAchievement
import skills.storage.repos.SkillEventsSupportRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.UserAchievedLevelRepo

@Component
@CompileStatic
@Slf4j
class AchievedBadgeHandler {

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Profile
    void checkForBadges(SkillEventResult res, String userId, SkillDefMin currentSkillDef, SkillDate skillDate) {
        List<SkillDefMin> parents = skillEventsSupportRepo.findParentSkillsByChildIdAndType(currentSkillDef.id, SkillRelDef.RelationshipType.BadgeRequirement)

        parents.each { SkillDefMin skillDefMin ->
            if (skillDefMin.type == SkillDef.ContainerType.Badge && BadgeUtils.withinActiveTimeframe(skillDefMin) &&
                    (skillDefMin.enabled == null || Boolean.valueOf(skillDefMin.enabled)) ) {
                SkillDefMin badge = skillDefMin
                Long nonAchievedChildren = achievedLevelRepo.countNonAchievedChildren(userId, badge.projectId, badge.skillId, SkillRelDef.RelationshipType.BadgeRequirement)
                if (nonAchievedChildren == 0) {
                    List<UserAchievement> badges = achievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(userId, badge.projectId, badge.skillId)
                    if (!badges) {
                        Date achievedOn = getAchievedOnDate(userId, badge, skillDate)
                        UserAchievement groupAchievement = new UserAchievement(userId: userId.toLowerCase(), projectId: badge.projectId, skillId: badge.skillId, skillRefId: badge?.id, achievedOn: achievedOn)
                        achievedLevelRepo.save(groupAchievement)
                        res.completed.add(new CompletionItem(type: CompletionTypeUtil.getCompletionType(badge.type), id: badge.skillId, name: badge.name))
                    }
                }
            }
        }
    }

    @Profile
    private Date getAchievedOnDate(String userId, SkillDefMin badge, SkillDate skillDate) {
        if (!skillDate.isProvided) {
            return skillDate.date
        }
        Date achievedOn = skillEventsSupportRepo.getUserPerformedSkillLatestDate(userId.toLowerCase(), badge.projectId, badge?.id, SkillRelDef.RelationshipType.BadgeRequirement)
        if (!achievedOn || skillDate.date.after(achievedOn)) {
            achievedOn = skillDate.date
        }
        return achievedOn
    }

}
