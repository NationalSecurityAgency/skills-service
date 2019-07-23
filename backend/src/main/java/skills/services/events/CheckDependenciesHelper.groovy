package skills.services.events

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.storage.model.SkillRelDef
import skills.storage.repos.UserAchievedLevelRepo

@Component
@CompileStatic
class CheckDependenciesHelper {

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    static class DependencyCheckRes {
        boolean hasNotAchievedDependents = false
        String msg
    }

    @Profile
    DependencyCheckRes check(String userId, String projectId, String skillId) {
        List<UserAchievedLevelRepo.ChildWithAchievementsInfo> dependentsAndAchievements = achievedLevelRepo.findChildrenAndTheirAchievements(userId, projectId, skillId, SkillRelDef.RelationshipType.Dependence)
        List<UserAchievedLevelRepo.ChildWithAchievementsInfo> notAchievedDependents = dependentsAndAchievements.findAll({
            !it.childAchievedSkillId
        })
        DependencyCheckRes res
        if (notAchievedDependents) {
            List<UserAchievedLevelRepo.ChildWithAchievementsInfo> sorted = notAchievedDependents.sort({ a, b -> a.childProjectId <=> b.childProjectId ?: a.childSkillId <=> b.childSkillId })
            res = new DependencyCheckRes(
                    hasNotAchievedDependents: true,
                    msg: "Not all dependent skills have been achieved. Missing achievements for ${notAchievedDependents.size()} out of ${dependentsAndAchievements.size()}. " +
                            "Waiting on completion of ${sorted.collect({ it.childProjectId + ":" + it.childSkillId })}."
            )
        } else {
            res = new DependencyCheckRes()
        }
        return res
    }

}
