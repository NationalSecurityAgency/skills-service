package skills.services.events.pointsAndAchievements

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.controller.exceptions.SkillException
import skills.services.LevelDefinitionStorageService
import skills.services.events.CompletionItem
import skills.services.events.CompletionTypeUtil
import skills.services.events.SkillEventResult
import skills.storage.model.LevelDef
import skills.storage.model.LevelDefInterface
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillEventsSupportRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo

@Component
@Slf4j
@CompileStatic
class PointsAndAchievementsHandler {

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    UserAchievedLevelRepo userAchievedLevelRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Autowired
    PointsAndAchievementsSaver saver

    @Autowired
    PointsAndAchievementsDataLoader dataLoader

    @Profile
    List<CompletionItem> updatePointsAndAchievements(String userId, SkillEventsSupportRepo.SkillDefMin skillDef, Date incomingSkillDate){
        LoadedData loadedData = dataLoader.loadData(skillDef.projectId, userId, incomingSkillDate, skillDef)

        PointsAndAchievementsBuilder builder = new PointsAndAchievementsBuilder(
                userId: userId,
                projectId: skillDef.projectId,
                skillId: skillDef.skillId,
                skillRefId: skillDef.id,
                loadedData: loadedData,
                pointIncrement: skillDef.pointIncrement,
                incomingSkillDate: incomingSkillDate,
                levelDefService: levelDefService,
        )
        PointsAndAchievementsBuilder.PointsAndAchievementsResult result = builder.build()
        saver.save(result.dataToSave)
        return result.completionItems
    }



    @Profile
    void checkParentGraph(Date incomingSkillDate, SkillEventResult res, String userId, SkillEventsSupportRepo.SkillDefMin skillDef, boolean decrement) {
        updateByTraversingUpSkillDefs(incomingSkillDate, res, skillDef, skillDef, userId, decrement)

        // updated project level
        UserPoints totalPoints = updateUserPoints(userId, skillDef, incomingSkillDate, null, decrement)
        if (!decrement) {
            List<LevelDef> levelDefs = skillEventsSupportRepo.findLevelsByProjectId(skillDef.projectId)
            SkillEventsSupportRepo.TinyProjectDef totalProjectPoints = skillEventsSupportRepo.getTinyProjectDef(skillDef.projectId)
            LevelDefinitionStorageService.LevelInfo levelInfo = levelDefService.getLevelInfo(skillDef.projectId, levelDefs, totalProjectPoints.totalPoints, totalPoints.points)
            CompletionItem completionItem = calculateLevels(levelInfo, totalPoints, null, userId, "OVERALL", decrement)
            if (completionItem?.level && completionItem?.level > 0) {
                res.completed.add(completionItem)
            }
        }
    }

    /**
     * @param skillId if null then will document it at overall project level
     */
    UserPoints updateUserPoints(String userId, SkillEventsSupportRepo.SkillDefMin requestedSkill, Date incomingSkillDate, String skillId = null, boolean decrement) {
        doUpdateUserPoints(requestedSkill, userId, incomingSkillDate, skillId, decrement)
        UserPoints res = doUpdateUserPoints(requestedSkill, userId, null, skillId, decrement)
        return res
    }

    @Profile
    private UserPoints doUpdateUserPoints(SkillEventsSupportRepo.SkillDefMin requestedSkill, String userId, Date incomingSkillDate, String skillId, boolean decrement) {
        Date day = incomingSkillDate ? new Date(incomingSkillDate.time).clearTime() : null
        UserPoints userPoints = getUserPoints(requestedSkill, userId, skillId, day)
        if (!userPoints) {
            assert !decrement
            userPoints = new UserPoints(userId: userId?.toLowerCase(), projectId: requestedSkill.projectId,
                    skillId: skillId,
                    skillRefId: skillId ? requestedSkill.id : null,
                    points: requestedSkill.pointIncrement, day: day)
        } else {
            if (decrement) {
                userPoints.points -= requestedSkill.pointIncrement
            } else {
                userPoints.points += requestedSkill.pointIncrement
            }
        }

        UserPoints res
        if (decrement && userPoints.points <= 0) {
            userPointsRepo.delete(userPoints)
            res = new UserPoints(userId: userId?.toLowerCase(), projectId: requestedSkill.projectId,
                    skillId: skillId,
                    skillRefId: skillId ? requestedSkill.id : null,
                    points: 0, day: day)
        } else {
            res = saveUserPoints(userPoints)
            log.debug("Updated points [{}]", res)
        }
        res
    }

    @Profile
    private UserPoints saveUserPoints(UserPoints subjectPoints) {
        userPointsRepo.save(subjectPoints)
    }

    @Profile
    private UserPoints getUserPoints(SkillEventsSupportRepo.SkillDefMin requestedSkill, String userId, String skillId, Date day) {
        userPointsRepo.findByProjectIdAndUserIdAndSkillIdAndDay(requestedSkill.projectId, userId, skillId, day)
    }

    @Profile
    private void updateByTraversingUpSkillDefs(Date incomingSkillDate, SkillEventResult res,
                                               SkillEventsSupportRepo.SkillDefMin currentDef,
                                               SkillEventsSupportRepo.SkillDefMin requesterDef,
                                               String userId, boolean decrement) {
        if (shouldEvaluateForAchievement(currentDef)) {
            UserPoints updatedPoints = updateUserPoints(userId, requesterDef, incomingSkillDate, currentDef.skillId, decrement)

            List<LevelDef> levelDefs = skillEventsSupportRepo.findLevelsBySkillId(currentDef.id)
            if (!levelDefs) {
                if (!decrement && updatedPoints.points >= currentDef.totalPoints) {
                    UserAchievement groupAchievement = new UserAchievement(userId: userId.toLowerCase(), projectId: currentDef.projectId, skillId: currentDef.skillId, skillRefId: currentDef?.id,
                            pointsWhenAchieved: updatedPoints.points)
                    achievedLevelRepo.save(groupAchievement)

                    res.completed.add(new CompletionItem(type: CompletionTypeUtil.getCompletionType(currentDef.type), id: currentDef.skillId, name: currentDef.name))
                } else if (decrement && updatedPoints.points <= currentDef.totalPoints) {
                    // we are decrementing, there are no levels defined and points are less that total points so we need
                    // to delete previously added achievement if it exists
                    achievedLevelRepo.deleteByProjectIdAndSkillIdAndUserIdAndLevel(currentDef.projectId, currentDef.skillId, userId, null)
                }
            } else {
                int currentScore = decrement ? updatedPoints.points + requesterDef.pointIncrement : updatedPoints.points
                LevelDefinitionStorageService.LevelInfo levelInfo = levelDefService.getLevelInfo(currentDef.projectId, levelDefs, currentDef.totalPoints, currentScore)
                CompletionItem completionItem = calculateLevels(levelInfo, updatedPoints, currentDef,  userId, currentDef.name, decrement)
                if (!decrement && completionItem?.level && completionItem?.level > 0) {
                    res.completed.add(completionItem)
                }
            }
        }

        List<SkillEventsSupportRepo.SkillDefMin> parentsRels = skillEventsSupportRepo.findParentSkillsByChildIdAndType(currentDef.id, SkillRelDef.RelationshipType.RuleSetDefinition)
        parentsRels?.each {
            updateByTraversingUpSkillDefs(incomingSkillDate, res, it, requesterDef, userId, decrement)
        }
    }

    private boolean shouldEvaluateForAchievement(SkillEventsSupportRepo.SkillDefMin skillDef) {
        skillDef.type == SkillDef.ContainerType.Subject
    }

    @Profile
    private CompletionItem calculateLevels(LevelDefinitionStorageService.LevelInfo levelInfo, UserPoints userPts, SkillEventsSupportRepo.SkillDefMin skillDef, String userId, String name, boolean decrement) {
        CompletionItem res

        List<UserAchievement> userAchievedLevels = achievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(userId, userPts.projectId, userPts.skillId)
        boolean levelAlreadyAchieved = userAchievedLevels?.find { it.level == levelInfo.level }
        if (!levelAlreadyAchieved && !decrement) {
            UserAchievement newLevel = new UserAchievement(userId: userId.toLowerCase(), projectId: userPts.projectId, skillId: userPts.skillId, skillRefId: skillDef?.id,
                    level: levelInfo.level, pointsWhenAchieved: userPts.points)
            achievedLevelRepo.save(newLevel)
            log.debug("Achieved new level [{}]", newLevel)

            res = new CompletionItem(
                    level: newLevel.level, name: name,
                    id: userPts.skillId ?: "OVERALL",
                    type: userPts.skillId ? CompletionItem.CompletionItemType.Subject : CompletionItem.CompletionItemType.Overall)
        } else if (decrement) {
            // we are decrementing, so we need to remove any level that is greater than the current level (there should only be one)
            List<UserAchievement> levelsToRemove = userAchievedLevels?.findAll { it.level >= levelInfo.level }
            if (levelsToRemove) {
                assert levelsToRemove.size() == 1, "we are decrementing a single skill so we should not be remove multiple (${levelsToRemove.size()} levels)"
                achievedLevelRepo.delete(levelsToRemove.first())
            }
        }

        return res
    }


}
