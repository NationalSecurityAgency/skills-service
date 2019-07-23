package skills.skillLoading

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.skillLoading.model.SkillDependencySummary
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.UserPoints
import skills.storage.repos.UserPointsRepo

@Component
@Slf4j
class SubjectDataLoader {

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    DependencySummaryLoader dependencySummaryLoader


    static class SkillsAndPoints {
        SkillDef skillDef
        int points
        int todaysPoints

        SkillDependencySummary dependencyInfo
    }

    static class SkillsData {
        List<SkillsAndPoints> childrenWithPoints
    }

    SkillsData loadData(String userId, String projectId, String skillId, Integer version = Integer.MAX_VALUE) {
        return loadData(userId, projectId, skillId, version, SkillRelDef.RelationshipType.RuleSetDefinition)
    }

    SkillsData loadData(String userId, String projectId, String skillId, Integer version = Integer.MAX_VALUE, SkillRelDef.RelationshipType relationshipType) {
        List<SkillDefAndUserPoints> childrenWithUserPoints = loadChildren(userId, projectId, skillId, relationshipType, version)
        childrenWithUserPoints = childrenWithUserPoints?.sort({ it.skillDef.displayOrder })

        List<SkillDefAndUserPoints> todaysUserPoints = loadChildren(userId, projectId, skillId, relationshipType, version, new Date().clearTime())

        List<SkillsAndPoints> skillsAndPoints = childrenWithUserPoints.collect { SkillDefAndUserPoints skillDefAndUserPoints ->
            SkillDefAndUserPoints todaysPoints = todaysUserPoints.find({
                it.skillDef.id == skillDefAndUserPoints.skillDef.id
            })
            int todayPoints = todaysPoints.points ? todaysPoints.points?.points : 0
            int points = skillDefAndUserPoints.points ? skillDefAndUserPoints.points.points : 0

            SkillDependencySummary dependencyInfo = dependencySummaryLoader.loadDependencySummary(userId,
                    skillDefAndUserPoints.skillDef.projectId,
                    skillDefAndUserPoints.skillDef.skillId)

            new SkillsAndPoints(skillDef: skillDefAndUserPoints.skillDef, points: points, todaysPoints: todayPoints, dependencyInfo: dependencyInfo)
        }
        new SkillsData(childrenWithPoints: skillsAndPoints)
    }

    private static class SkillDefAndUserPoints {
        SkillDef skillDef
        UserPoints points
    }

    private List<SkillDefAndUserPoints> loadChildren(String userId, String projectId, String skillId, SkillRelDef.RelationshipType relationshipType, Integer version = Integer.MAX_VALUE, Date day = null) {

        List<Object[]> childrenWithUserPoints = day ?
                userPointsRepo.findChildrenAndTheirUserPoints(userId, projectId, skillId, relationshipType, version, day) :
                userPointsRepo.findChildrenAndTheirUserPoints(userId, projectId, skillId, relationshipType, version)

        List<SkillDefAndUserPoints> res = childrenWithUserPoints.collect {
            UserPoints userPoints = (it.length > 1 ? it[1] : null)
            return new SkillDefAndUserPoints(
                    skillDef: it[0], points: userPoints
            )
        }
        return res?.sort { it.skillDef.displayOrder }
    }

}
