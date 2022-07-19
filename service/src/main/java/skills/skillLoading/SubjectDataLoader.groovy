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
import skills.skillLoading.model.SkillDependencySummary
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefParent
import skills.storage.model.SkillDefWithExtra
import skills.storage.model.SkillRelDef
import skills.storage.model.UserPoints
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


    static class SkillsAndPoints {
        SkillDef skillDef
        int points
        int todaysPoints
        String copiedFromProjectName

        SkillDependencySummary dependencyInfo
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
                    copiedFromProjectName: skillDefAndUserPoints.copiedFromProjectName)
        }
        new SkillsData(childrenWithPoints: skillsAndPoints)
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
    }

    @Profile
    private List<SkillDefAndUserPoints> loadChildren(String userId, String projectId, String skillId, List<SkillRelDef.RelationshipType> relationshipTypes, Integer version = Integer.MAX_VALUE) {

        List<Object[]> childrenWithUserPoints = findChildrenPoints(userId, projectId, skillId, relationshipTypes, version)

        List<SkillDefAndUserPoints> res = childrenWithUserPoints.collect {
            UserPoints userPoints = (it.length > 1 ? it[1] : null) as UserPoints
            return new SkillDefAndUserPoints(
                    skillDef: it[0] as SkillDef, points: userPoints, copiedFromProjectName: it.length > 2 ? (String)it[2] : null
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
