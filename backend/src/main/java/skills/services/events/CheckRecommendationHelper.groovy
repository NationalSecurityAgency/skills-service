package skills.services.events

import callStack.profiler.Profile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.UserAchievedLevelRepo

@Component
class CheckRecommendationHelper {

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Profile
    List<RecommendationItem> checkForRecommendations(String userId, String projectId, String skillId) {
        List<RecommendationItem> res = []

        // 1. find all of the neighbors that don't have achievements
        // 2. check to see that all of those have all of their dependencies full-filled, if so recommend otherwise keep walking down the tree to find the next best recommendation
        List<SkillDef> nonAchievedParents = achievedLevelRepo.findNonAchievedParents(userId, projectId, skillId, SkillRelDef.RelationshipType.Dependence)
        List<SkillDef> myRecommendations = achievedLevelRepo.findNonAchievedChildren(userId, projectId, skillId, SkillRelDef.RelationshipType.Recommendation)
        List<SkillDef> recommendationsToConsider = []
        if(nonAchievedParents){
            recommendationsToConsider.addAll(nonAchievedParents)
        }
        if(myRecommendations){
            recommendationsToConsider.addAll(myRecommendations)
        }
        recommendationsToConsider = recommendationsToConsider.unique { it.skillId }
        if (recommendationsToConsider) {
            recommendationsToConsider.collect({
                res.addAll(checkGraphForRecommendations(userId, it))
            })
        }

        return res
    }

    @Profile
    private List<RecommendationItem> checkGraphForRecommendations(String userId, SkillDef skillDef) {
        List<RecommendationItem> res = []
        List<SkillDef> nonAchievedChildren = achievedLevelRepo.findNonAchievedChildren(userId, skillDef.projectId, skillDef.skillId, SkillRelDef.RelationshipType.Dependence)
        if (!nonAchievedChildren) {
            res << new RecommendationItem(id: skillDef.skillId, name: skillDef.name)
        } else {
            nonAchievedChildren.each {
                res.addAll(checkGraphForRecommendations(userId, it))
            }
        }

        return res
    }
}
