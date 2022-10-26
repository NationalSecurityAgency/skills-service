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
package skills.skillLoading

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.services.settings.ClientPrefKey
import skills.services.settings.ClientPrefService
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.skillLoading.model.SkillDependencySummary
import skills.storage.model.ClientPref
import skills.storage.model.SimpleBadgeRes
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefParent
import skills.storage.model.SkillRelDef
import skills.storage.model.UserPoints
import skills.storage.repos.SettingRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillDefWithExtraRepo
import skills.storage.repos.UserPerformedSkillRepo
import skills.storage.repos.UserPointsRepo

@Component
@Slf4j
@CompileStatic
class SubjectDataLoader {

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    DependencySummaryLoader dependencySummaryLoader

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    SettingsService settingsService

    @Autowired
    ClientPrefService clientPrefService

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    SkillDefRepo skillDefRepo

    static class SkillsAndPoints {
        SkillDef skillDef
        int points
        int todaysPoints
        String copiedFromProjectName
        String description
        Boolean isLastViewed

        SkillDependencySummary dependencyInfo

        List<SkillsAndPoints> children = []
        SkillApproval approval
        List<SimpleBadgeRes> badges = []
    }

    static class SkillsData {
        List<SkillsAndPoints> childrenWithPoints
    }

    @Profile
    SkillsData loadData(String userId,  String projectId, SkillDefParent skillDefWithExtra, Integer version = Integer.MAX_VALUE, List<SkillRelDef.RelationshipType> relationshipTypes) {
        String skillId = skillDefWithExtra.skillId
        List<SkillDefAndUserPoints> childrenWithUserPoints = loadChildren(userId, projectId, skillId, relationshipTypes, version)
        childrenWithUserPoints = childrenWithUserPoints?.sort({ it.skillDef.displayOrder })

        List<UserPointsRepo.SkillRefIdWithPoints> todaysUserPoints = loadChildrenDayPoints(userId, skillDefWithExtra.id, relationshipTypes, new Date().clearTime())

        List<UserPointsRepo.SkillWithChildAndAchievementIndicator> allProjectDepsAndAchievements = loadAllDepsWithAchievementIndicator(userId, projectId, version)
        Map<Integer, List<UserPointsRepo.SkillWithChildAndAchievementIndicator>> byParentId = allProjectDepsAndAchievements.groupBy { it.parentId }

        List<SkillsAndPoints> skillsAndPoints = childrenWithUserPoints.collect { SkillDefAndUserPoints skillDefAndUserPoints ->
            UserPointsRepo.SkillRefIdWithPoints todaysPoints = todaysUserPoints.find({
                it.skillRefId == skillDefAndUserPoints.skillDef.id
            })

            int todayPoints = todaysPoints?.points ? todaysPoints.points : 0
            int points = skillDefAndUserPoints?.points ? skillDefAndUserPoints.points.points : 0

            if (skillDefAndUserPoints.skillDef.copiedFrom != null && skillDefAndUserPoints.skillDef.selfReportingType) {
                // because of the catalog's async nature when self-approval honor skill is submitted todaysPoints and points are not consistent on the imported side
                // this is because todaysPoints are calculated from UserPerformedSkill but points come from UserPoints; UserPerformedSkill
                // is shared in the catalog exported/imported skills but UserPoints are duplicated and asynchronously synced
                if (todayPoints > points) {
                    // this will at least account for 1 event that have not been propagated and make it a bit more consistent
                    // it mostly likely will account for the first event only unless multiple skill events are submitted in the same day
                    points = points + skillDefAndUserPoints.skillDef.pointIncrement
                }
            }

            List<UserPointsRepo.SkillWithChildAndAchievementIndicator> dependents = byParentId[skillDefAndUserPoints.skillDef.id]
            SkillDependencySummary dependencyInfo
            if (dependents) {
                dependencyInfo = dependents ? new SkillDependencySummary(
                        numDirectDependents: dependents.size(),
                        achieved: !dependents.find { it.getAchievementId() == null }
                ) : null
            }
            new SkillsAndPoints(skillDef: skillDefAndUserPoints.skillDef, points: points, todaysPoints: todayPoints, dependencyInfo: dependencyInfo,
                    copiedFromProjectName: skillDefAndUserPoints.copiedFromProjectName, approval: skillDefAndUserPoints.approval)
        }

        updateLastViewedSkill(skillsAndPoints, userId, projectId)

        skillsAndPoints = handleGroupSkills(skillsAndPoints, relationshipTypes)
        skillsAndPoints = handleGroupDescriptions(projectId, skillsAndPoints, relationshipTypes)
        skillsAndPoints = handleBadges(projectId, skillsAndPoints)

        new SkillsData(childrenWithPoints: skillsAndPoints)
    }

    @Profile
    private void updateLastViewedSkill(List<SkillsAndPoints> skillsAndPoints, String userId, String projectId) {
        String lastViewedSkillId = null
        if (projectId) {
            ClientPref clientPref = clientPrefService.findPref(ClientPrefKey.LastViewedSkill, userId, projectId)
            lastViewedSkillId = clientPref?.value
        }
        skillsAndPoints.each {
            it.isLastViewed = it.skillDef.skillId == lastViewedSkillId
        }
    }

    @Profile
    private List<SkillsAndPoints> handleBadges(String projectId, List<SkillsAndPoints> skillsAndPoints) {
        if(projectId) {
            List<String> skillIds = skillsAndPoints.collect{ it -> it.skillDef.skillId }
            def badges = skillDefRepo.findAllBadgesForSkill(skillIds, projectId);
            def badgesById = badges.groupBy{ it.skillId }
            skillsAndPoints.forEach{ it ->
                it.badges = badgesById[it.skillDef.skillId]
            }
        }
        return skillsAndPoints;
    }

    private List<SkillsAndPoints> handleGroupDescriptions(String projectId, List<SkillsAndPoints> skillsAndPoints, List<SkillRelDef.RelationshipType> relationshipTypes) {
        if (relationshipTypes.containsAll([SkillRelDef.RelationshipType.SkillsGroupRequirement, SkillRelDef.RelationshipType.GroupSkillToSubject])) {
            List<SkillsAndPoints> groups = skillsAndPoints.findAll({ it.skillDef.type == SkillDef.ContainerType.SkillsGroup })
            List<String> groupIds = groups.collect { it.skillDef.skillId }
            if (groupIds) {
                Boolean groupDescriptionsOn = settingsService.getProjectSetting(projectId, Settings.GROUP_DESCRIPTIONS.settingName)?.value?.toBoolean()
                if (groupDescriptionsOn) {
                    Map<String, List<SkillsAndPoints>> bySkillId = groups.groupBy { it.skillDef.skillId}
                    List<SkillDefWithExtraRepo.SkillIdAndDesc> loadedDescriptions = skillDefWithExtraRepo.findDescriptionBySkillIdIn(projectId, groupIds)
                    loadedDescriptions.each {
                        SkillsAndPoints group = bySkillId[it.getSkillId()]?.first()
                        if (!group) {
                            throw new IllegalStateException("Failed locate the group. This is a logic error. lookedFor=[${it.getSkillId()}]")
                        }
                        group.description = it.description
                    }
                }
            }
        }
        return skillsAndPoints;
    }

    private List<SkillsAndPoints> handleGroupSkills(List<SkillsAndPoints> skillsAndPoints, List<SkillRelDef.RelationshipType> relationshipTypes) {
        if (relationshipTypes.containsAll([SkillRelDef.RelationshipType.SkillsGroupRequirement, SkillRelDef.RelationshipType.GroupSkillToSubject])) {
            List<SkillsAndPoints> res = skillsAndPoints
            List<SkillsAndPoints> childrenOfGroups = skillsAndPoints.findAll({ it.skillDef.groupId })
            if (childrenOfGroups) {
                res = skillsAndPoints.findAll({ !it.skillDef.groupId })
                Map<String, List<SkillsAndPoints>> bySkillId = res.groupBy { it.skillDef.skillId }
                childrenOfGroups.each {
                    SkillsAndPoints parent = bySkillId[it.skillDef.groupId]?.first()
                    if (!parent) {
                        throw new IllegalStateException("Failed to find group for a skill under that group. groupSkillId=[${it.skillDef.groupId}], childSkillId=[${it.skillDef.skillId}]")
                    }
                    parent.children.add(it)
                }
            }

            return res
        }
        return skillsAndPoints
    }

    @Profile
    private List<UserPointsRepo.SkillWithChildAndAchievementIndicator> loadAllDepsWithAchievementIndicator(String userId, String projectId, int version) {
        if (projectId) {
            userPointsRepo.findAllChildrenAndTheirAchievementsForProject(userId, projectId, SkillRelDef.RelationshipType.Dependence, version)
        } else {
            userPointsRepo.findAllChildrenAndTheirAchievementsForGlobal(userId, SkillRelDef.RelationshipType.Dependence, version)
        }
    }

    private static class SkillDefAndUserPoints {
        SkillDef skillDef
        UserPoints points
        String copiedFromProjectName
        SkillApproval approval
    }

    @Profile
    private List<SkillDefAndUserPoints> loadChildren(String userId, String projectId, String skillId, List<SkillRelDef.RelationshipType> relationshipTypes, Integer version = Integer.MAX_VALUE) {

        List<Object[]> childrenWithUserPoints = findChildrenPoints(userId, projectId, skillId, relationshipTypes, version)

        List<SkillDefAndUserPoints> res = childrenWithUserPoints.collect {
            UserPoints userPoints = (it.length > 1 ? it[1] : null) as UserPoints
            SkillApproval skillApproval = (projectId ? (it.length > 3 ? it[3] : null) : (it.length > 2 ? it[2] : null)) as SkillApproval
            return new SkillDefAndUserPoints(
                    skillDef: it[0] as SkillDef, points: userPoints, copiedFromProjectName: it.length > 2 ? (String)it[2] : null, approval: skillApproval
            )
        }
        return res?.findAll {it.skillDef.type != SkillDef.ContainerType.SkillsGroup || it.skillDef.totalPoints > 0 }.sort { it.skillDef.displayOrder }
    }

    @Profile
    private List<UserPointsRepo.SkillRefIdWithPoints> loadChildrenDayPoints(String userId, Integer skillRefId, List<SkillRelDef.RelationshipType> relationshipTypes, Date day) {
        List<UserPointsRepo.SkillRefIdWithPoints> res = userPointsRepo.calculatePointsForChildSkillsForADay(userId, skillRefId, relationshipTypes.collect { it.toString()}, day)
        return res
    }

    @Profile
    private List<Object[]> findChildrenPoints(String userId, String projectId, String skillId, List<SkillRelDef.RelationshipType> relationshipTypes, int version) {
        if (projectId) {
            return userPointsRepo.findChildrenAndTheirUserPoints(userId, projectId, skillId, relationshipTypes, version)
        } else {
            return userPointsRepo.findGlobalChildrenAndTheirUserPoints(userId, skillId, relationshipTypes, version)
        }
    }

}
