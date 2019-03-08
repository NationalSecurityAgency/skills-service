package skills.service.skillLoading

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.service.skillLoading.model.SkillDependencySummary
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.UserPointsRepo

@Component
@Slf4j
class DependencySummaryLoader {

    @Autowired
    UserPointsRepo userPointsRepo

    private static class SkillWithAchievementIndicator {
        SkillDef skillDef
        boolean isAchieved
    }

    @Transactional(readOnly = true)
    SkillDependencySummary loadDependencySummary(String userId, String projectId, String skillId){
        List<SkillWithAchievementIndicator> dependents = loadDependentSkills(userId, projectId, skillId)
        SkillDependencySummary dependencySummary = dependents ? new SkillDependencySummary(
                numDirectDependents: dependents.size(),
                achieved: !dependents.find { !it.isAchieved }
        ) : null

        return dependencySummary
    }

    private List<SkillWithAchievementIndicator> loadDependentSkills(String userId, String projectId, String skillId) {
        List<Object []> dependentSkillsAndTheirAchievementStatus = userPointsRepo.findChildrenAndThierAchievements(userId, projectId, skillId, SkillRelDef.RelationshipType.Dependence)
        return dependentSkillsAndTheirAchievementStatus.collect {
            new SkillWithAchievementIndicator(skillDef: it[0], isAchieved: it[1] != null)
        }
    }
}
