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
import skills.services.admin.SkillsGroupAdminService
import skills.services.events.SkillDate
import skills.storage.model.LevelDefInterface
import skills.storage.model.SkillDefMin
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.SkillEventsSupportRepo

@Component
@Slf4j
@CompileStatic
class PointsAndAchievementsDataLoader {

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Autowired
    LoadedDataValidator validator

    @Autowired
    SkillsGroupAdminService skillsGroupAdminService

    @Profile
    LoadedData loadData(String projectId, String userId, SkillDate incomingSkillDate, SkillDefMin skillDef){
        List<SkillEventsSupportRepo.TinySkillDef> parentDefs = loadParents(skillDef)

        // handle skills group with less than all skills required
        Integer skillsGroupDefId
        Integer numChildSkillsRequired
        Integer subjectDefId
        if (skillDef.groupId) {
            // skills group child, check parent and if numSkillsRequired then need to load siblings to determine which contribute to points
            assert parentDefs && parentDefs.size() == 1 && parentDefs.first().type == SkillDef.ContainerType.SkillsGroup && parentDefs.first().skillId == skillDef.groupId
            parentDefs.addAll(loadParents(parentDefs.first().id))
            if (parentDefs.first().numSkillsRequired > 0 && Boolean.valueOf(parentDefs.first().enabled)) {
                skillsGroupDefId = parentDefs.first().id
                numChildSkillsRequired = parentDefs.first().numSkillsRequired
            }
        } else if (skillDef.type == SkillDef.ContainerType.SkillsGroup) {
            if (skillDef.numSkillsRequired > 0 && Boolean.valueOf(skillDef.enabled)) {
                skillsGroupDefId = skillDef.id
                numChildSkillsRequired = skillDef.numSkillsRequired
            }
        }
        subjectDefId = parentDefs.find { it.type == SkillDef.ContainerType.Subject }?.id

        List<Integer> skillRefIds = [skillDef.id]
        skillRefIds.addAll(parentDefs.collect { it.id })
        List<SkillEventsSupportRepo.TinyUserPoints> tinyUserPoints = loadPoints(projectId, userId, skillRefIds, incomingSkillDate.date)

        SkillEventsSupportRepo.TinyProjectDef tinyProjectDef = loadProject(projectId)
        List<Integer> parentIds = parentDefs.collect { it.id }
        List<LevelDefInterface> tinyLevels = loadLevels(parentIds, tinyProjectDef)

        List<SkillEventsSupportRepo.TinyUserAchievement> tinyUserAchievements = loadAchievements(userId, projectId, skillRefIds)

        LoadedData res = new LoadedData(userId: userId, projectId: projectId, parentDefs: parentDefs, tinyUserPoints: tinyUserPoints,
                levels: tinyLevels, tinyUserAchievements: tinyUserAchievements, tinyProjectDef:tinyProjectDef, skillsRefId: skillDef.id,
                subjectDefId: subjectDefId, numChildSkillsRequired: numChildSkillsRequired, skillsGroupDefId: skillsGroupDefId)
        validator.validate(res)

        return res
    }

    @Profile
    private List<SkillEventsSupportRepo.TinyUserAchievement> loadAchievements(String userId, String projectId, List<Integer> skillRefIds) {
        skillEventsSupportRepo.findTinyUserAchievementsByUserIdAndProjectIdAndSkillIds(userId, projectId, skillRefIds)
    }

    @Profile
    private List<LevelDefInterface> loadLevels(List<Integer> parentIds, SkillEventsSupportRepo.TinyProjectDef tinyProjectDef) {
        skillEventsSupportRepo.findLevelsBySkillIdsOrByProjectId(parentIds, tinyProjectDef.id)
    }

    @Profile
    private SkillEventsSupportRepo.TinyProjectDef loadProject(String projectId) {
        skillEventsSupportRepo.getTinyProjectDef(projectId)
    }

    @Profile
    private List<SkillEventsSupportRepo.TinyUserPoints> loadPoints(String projectId, String userId, List<Integer> skillRefIds, Date incomingSkillDate) {
        skillEventsSupportRepo.findTinyUserPointsProjectIdAndUserIdAndSkillsAndDay(projectId, userId, skillRefIds, incomingSkillDate)
    }

    @Profile
    private List<SkillEventsSupportRepo.TinySkillDef> loadParents(SkillDefMin skillDef) {
        loadParents(skillDef.id)
    }

    @Profile
    private List<SkillEventsSupportRepo.TinySkillDef> loadParents(Integer skillRefId) {
        skillEventsSupportRepo.findTinySkillDefsParentsByChildIdAndTypeIn(skillRefId, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
    }
}
