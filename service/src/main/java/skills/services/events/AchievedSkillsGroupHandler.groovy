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
import skills.services.admin.SkillsGroupAdminService
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefMin
import skills.storage.model.SkillRelDef
import skills.storage.model.UserAchievement
import skills.storage.repos.SkillDefWithExtraRepo
import skills.storage.repos.SkillEventsSupportRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.UserAchievedLevelRepo

@Component
@CompileStatic
@Slf4j
class AchievedSkillsGroupHandler {

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    SkillsGroupAdminService skillsGroupAdminService

    @Profile
    void checkForSkillsGroup(SkillEventResult res, String userId, SkillDefMin currentSkillDef, SkillDate skillDate) {
        SkillDefMin skillsGroupSkillDef
        if (currentSkillDef.groupId) {
            skillsGroupSkillDef = skillEventsSupportRepo.findByProjectIdAndSkillIdAndType(currentSkillDef.projectId, currentSkillDef.groupId, SkillDef.ContainerType.SkillsGroup)
        } else  if (currentSkillDef.type == SkillDef.ContainerType.SkillsGroup) {
            skillsGroupSkillDef = currentSkillDef
        }
        if (skillsGroupSkillDef && Boolean.valueOf(skillsGroupSkillDef.enabled)) {
            Long achievedChildren = achievedLevelRepo.countAchievedChildren(userId, skillsGroupSkillDef.projectId, skillsGroupSkillDef.skillId, SkillRelDef.RelationshipType.SkillsGroupRequirement)
            int numSkillsRequired = skillsGroupAdminService.getActualNumSkillsRequred(skillsGroupSkillDef.numSkillsRequired, skillsGroupSkillDef.id)
            if (achievedChildren >= numSkillsRequired) {
                List<UserAchievement> achievements = achievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(userId, skillsGroupSkillDef.projectId, skillsGroupSkillDef.skillId)
                if (!achievements) {
                    Date achievedOn = getAchievedOnDate(userId, skillsGroupSkillDef, skillDate)
                    UserAchievement groupAchievement = new UserAchievement(userId: userId.toLowerCase(), projectId: skillsGroupSkillDef.projectId, skillId: skillsGroupSkillDef.skillId, skillRefId: skillsGroupSkillDef.id, pointsWhenAchieved: skillsGroupSkillDef.totalPoints, achievedOn: achievedOn)
                    achievedLevelRepo.save(groupAchievement)
                    res.completed.add(new CompletionItem(type: CompletionTypeUtil.getCompletionType(skillsGroupSkillDef.type), id: skillsGroupSkillDef.skillId, name: skillsGroupSkillDef.name))
                }
            }
        }
    }

    @Profile
    private Date getAchievedOnDate(String userId, SkillDefMin skillsGroup, SkillDate skillDate) {
        if (!skillDate.isProvided) {
            return skillDate.date
        }
        Date achievedOn = skillEventsSupportRepo.getUserPerformedSkillLatestDate(userId.toLowerCase(), skillsGroup.projectId, skillsGroup?.id, SkillRelDef.RelationshipType.SkillsGroupRequirement)
        if (!achievedOn || skillDate.date.after(achievedOn)) {
            achievedOn = skillDate.date
        }
        return achievedOn
    }
}
