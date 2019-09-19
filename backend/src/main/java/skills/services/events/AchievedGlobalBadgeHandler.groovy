package skills.services.events

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.result.model.GlobalBadgeLevelRes
import skills.services.GlobalBadgesService
import skills.skillLoading.SkillsLoader
import skills.storage.model.SkillRelDef
import skills.storage.model.UserAchievement
import skills.storage.repos.SkillEventsSupportRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.UserAchievedLevelRepo

import static skills.storage.repos.SkillEventsSupportRepo.*

@Component
@CompileStatic
@Slf4j
class AchievedGlobalBadgeHandler {

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Autowired
    GlobalBadgesService globalBadgesService

    @Autowired
    SkillsLoader skillsLoader


    @Profile
    void checkForGlobalBadges(SkillEventResult res, String userId, String projectId, SkillDefMin currentSkillDef) {
        List<SkillDefMin> globalBadges = skillEventsSupportRepo.findGlobalBadgesForProjectIdAndSkillId(projectId, currentSkillDef.skillId)
        if (globalBadges) {
            List<TinyProjectDef> projectsForUser = skillEventsSupportRepo.getTinyProjectDefForUserId(userId)
            Map<String, Integer> projectLevels = [:]
            badgeCheckLoop: for (SkillDefMin globalBadge : globalBadges) {
                // first check required project levels
                List<GlobalBadgeLevelRes> requiredLevels = globalBadgesService.getGlobalBadgeLevels(globalBadge.skillId)
                for (GlobalBadgeLevelRes requiredLevel : requiredLevels) {
                    if (projectsForUser.find { it.projectId == requiredLevel.projectId }) {
                        if (!projectLevels.containsKey(requiredLevel.projectId)) {
                            projectLevels[requiredLevel.projectId] = skillsLoader.getUserLevel(requiredLevel.projectId, userId)
                        }
                        Integer achievedProjectLevel = projectLevels.get(requiredLevel.projectId)
                        if (!(achievedProjectLevel >= requiredLevel.level)) {
                            break badgeCheckLoop
                        }
                    } else {
                        break badgeCheckLoop
                    }
                }

                // all project level requirements met, check required skills
                Long nonAchievedChildren = achievedLevelRepo.countNonAchievedGlobalSkills(userId, globalBadge.skillId, SkillRelDef.RelationshipType.BadgeRequirement)
                if (nonAchievedChildren == 0) {
                    List<UserAchievement> badges = achievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(userId, globalBadge.projectId, globalBadge.skillId)
                    if (!badges) {
                        UserAchievement groupAchievement = new UserAchievement(userId: userId, projectId: globalBadge.projectId, skillId: globalBadge.skillId, skillRefId: globalBadge?.id)
                        achievedLevelRepo.save(groupAchievement)
                        res.completed.add(new CompletionItem(type: CompletionTypeUtil.getCompletionType(globalBadge.type), id: globalBadge.skillId, name: globalBadge.name))
                    }
                }
            }
        }
    }

    @Profile
    private List<TinyUserAchievement> loadOverallLevelAchievements(String userId, String projectId, List<Integer> skillRefIds) {
        skillEventsSupportRepo.findTinyUserAchievementsByUserIdAndProjectId(userId, projectId)
    }
}
