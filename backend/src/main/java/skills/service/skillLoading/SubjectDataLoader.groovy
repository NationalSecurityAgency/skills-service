package skills.service.skillLoading

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.service.skillLoading.model.SkillDependencySummary
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.UserPoints
import skills.storage.repos.UserPointsRepo

@Component
@Slf4j
class SubjectDataLoader {

    @Autowired
    UserPointsRepo userPointsRepo

    static class SkillsAndPoints {
        SkillDef skillDef
        int points
        int todaysPoints

        SkillDependencySummary dependencyInfo
    }

    static class SkillsData {
        List<SkillsAndPoints> childrenWithPoints
    }

    SkillsData loadData(String userId, String projectId, String skillId, Integer version = 0) {
        return loadData(userId, projectId, skillId, version, SkillRelDef.RelationshipType.RuleSetDefinition)
    }

    SkillsData loadData(String userId, String projectId, String skillId, Integer version = 0, SkillRelDef.RelationshipType relationshipType) {
        List<SkillDefAndUserPoints> childrenWithUserPoints = loadChildren(userId, projectId, skillId, relationshipType, version)
        childrenWithUserPoints = childrenWithUserPoints?.sort({ it.skillDef.displayOrder })

        List<SkillDefAndUserPoints> todaysUserPoints = loadChildren(userId, projectId, skillId, relationshipType, version, new Date().clearTime())

        List<SkillsAndPoints> skillsAndPoints = childrenWithUserPoints.collect { SkillDefAndUserPoints skillDefAndUserPoints ->
            SkillDefAndUserPoints todaysPoints = todaysUserPoints.find({
                it.skillDef.id == skillDefAndUserPoints.skillDef.id
            })
            int todayPoints = todaysPoints.points ? todaysPoints.points?.points : 0
            int points = skillDefAndUserPoints.points ? skillDefAndUserPoints.points.points : 0

            List<SkillWithAchievementIndicator> dependents = loadDependentSkills(userId, projectId, skillDefAndUserPoints.skillDef.skillId)
            SkillDependencySummary dependencyInfo = dependents ? new SkillDependencySummary(
                    numDirectDependents: dependents.size(),
                    achieved: !dependents.find { !it.isAchieved }
            ) : null

            new SkillsAndPoints(skillDef: skillDefAndUserPoints.skillDef, points: points, todaysPoints: todayPoints, dependencyInfo: dependencyInfo)
        }
        new SkillsData(childrenWithPoints: skillsAndPoints)
    }

    private static class SkillWithAchievementIndicator {
        SkillDef skillDef
        boolean isAchieved
    }

    private List<SkillWithAchievementIndicator> loadDependentSkills(String userId, String projectId, String skillId) {
        List<Object []> dependentSkillsAndTheirAchievementStatus = userPointsRepo.findChildrenAndThierAchievements(userId, projectId, skillId, SkillRelDef.RelationshipType.Dependence)
        return dependentSkillsAndTheirAchievementStatus.collect {
            new SkillWithAchievementIndicator(skillDef: it[0], isAchieved: it[1] != null)
        }
    }

    private static class SkillDefAndUserPoints {
        SkillDef skillDef
        UserPoints points
    }

    private List<SkillDefAndUserPoints> loadChildren(String userId, String projectId, String skillId, SkillRelDef.RelationshipType relationshipType, Integer version = -1, Date day = null) {

        if (version == -1) {
            version = Integer.MAX_VALUE
        }
        List<Object[]> childrenWithUserPoints = day ?
                userPointsRepo.findChildrenAndTheirUserPoints(userId, projectId, skillId, relationshipType, version, day) :
                userPointsRepo.findChildrenAndTheirUserPoints(userId, projectId, skillId, relationshipType, version)

        List<SkillDefAndUserPoints> res = childrenWithUserPoints.collect {
            UserPoints userPoints = (it.length > 1 ? it[1] : null)
            return new SkillDefAndUserPoints(
                    skillDef: it[0], points: userPoints
            )
        }
        return res
    }

}
