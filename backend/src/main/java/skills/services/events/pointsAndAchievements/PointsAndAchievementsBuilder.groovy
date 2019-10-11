package skills.services.events.pointsAndAchievements

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import skills.services.LevelDefinitionStorageService
import skills.services.events.CompletionItem
import skills.storage.model.LevelDefInterface
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints
import skills.storage.repos.SkillEventsSupportRepo

@Slf4j
@CompileStatic
class PointsAndAchievementsBuilder {
    String userId
    String projectId
    String skillId
    Integer skillRefId
    LoadedData loadedData
    int pointIncrement
    Date incomingSkillDate
    LevelDefinitionStorageService levelDefService


    class PointsAndAchievementsResult {
        DataToSave dataToSave
        List<CompletionItem> completionItems
    }

    DataToSave dataToSave
    List<CompletionItem> completionItems = []

    @Profile
    PointsAndAchievementsResult build() {
        dataToSave = new DataToSave(pointIncrement: pointIncrement)

        // any parent that exist must get points added
        dataToSave.toAddPointsTo.addAll(loadedData.tinyUserPoints)

        // create user points for this skill if it doesn't already exist
        dataToSave.toSave.addAll(createUserPointsIfNeeded(skillRefId, skillId))

        loadedData.parentDefs.each { SkillEventsSupportRepo.TinySkillDef parentSkillDef ->
            // if needed, create user points for parents
            dataToSave.toSave.addAll(createUserPointsIfNeeded(parentSkillDef.id, parentSkillDef.skillId))
            handleSubjectAchievement(parentSkillDef)
        }

        // if needed, created user points for project
        dataToSave.toSave.addAll(createUserPointsIfNeeded(null, null))

        handleOverallAchievement()

        PointsAndAchievementsResult res = new PointsAndAchievementsResult(dataToSave: dataToSave, completionItems: completionItems)
        return res
    }


    private void handleOverallAchievement() {
        List<UserAchievement> achievements = checkForAchievements(null, null, loadedData.tinyProjectDef.totalPoints)
        if (achievements) {
            dataToSave.userAchievements.addAll(achievements)
            completionItems.addAll(achievements.collect {
                new CompletionItem(
                        level: it.level,
                        name: "OVERALL",
                        id: "OVERALL",
                        type: CompletionItem.CompletionItemType.Overall)
            })
        }
    }

    private void handleSubjectAchievement(SkillEventsSupportRepo.TinySkillDef skillDef) {
        if (skillDef.type == SkillDef.ContainerType.Subject) {
            List<UserAchievement> achievements = checkForAchievements(skillDef.skillId, skillDef.id, skillDef.totalPoints)
            if (achievements) {
                dataToSave.userAchievements.addAll(achievements)
                completionItems.addAll(achievements.collect {
                    new CompletionItem(
                            level: it.level, name: skillDef.name,
                            id: skillDef.skillId,
                            type: CompletionItem.CompletionItemType.Subject)
                })
            }
        }
    }

    @Profile
    private  List<UserAchievement> checkForAchievements(String skillId, Integer skillRefId, Integer totalPoints) {
        List<UserAchievement> res

        SkillEventsSupportRepo.TinyUserPoints existingUserPoints = loadedData.getTotalUserPoints(skillRefId)

        List<LevelDefInterface> levelDefs = loadedData.levels.findAll({ it.skillRefId == skillRefId })
        int currentScore = existingUserPoints ? existingUserPoints.points + pointIncrement : pointIncrement
        LevelDefinitionStorageService.LevelInfo levelInfo = levelDefService.getLevelInfo(projectId, levelDefs, totalPoints, currentScore)

        // first achieved level is 1, level 0 should not be documented
        if (levelInfo.level > 0) {
            List<SkillEventsSupportRepo.TinyUserAchievement> userAchievedLevels = loadedData.getUserAchievements(skillRefId)
            Integer maxAchieved = userAchievedLevels.collect({it.level}).max()
            maxAchieved = maxAchieved ?: 0
            // handle an edge case where user achieves multiple levels via one event
            if (levelInfo.level > maxAchieved) {
                res = (maxAchieved+1..levelInfo.level).collect {
                    UserAchievement achievement = new UserAchievement(userId: userId.toLowerCase(), projectId: projectId, skillId: skillId, skillRefId: skillRefId,
                            level: it, pointsWhenAchieved: currentScore)
                    log.debug("Achieved new level [{}]", achievement)
                    return achievement
                }
            }
        }

        return res
    }


    private List<UserPoints> createUserPointsIfNeeded(Integer skillRefId, String skillId) {
        List<UserPoints> toSave = []
        List<SkillEventsSupportRepo.TinyUserPoints> myExistingPoints = loadedData.getUserPoints(skillRefId)

        // add overall user points if it's the first time
        if (!myExistingPoints) {
            toSave << constructUserPoints(userId, projectId, skillRefId, skillId, null, pointIncrement)
        }

        // add user points if a record doesn't exist for that day already
        Date incomingDay = new Date(incomingSkillDate.time).clearTime()
        if (!myExistingPoints?.find { it.getDay() == incomingDay }) {
            toSave << constructUserPoints(userId, projectId, skillRefId, skillId, incomingDay, pointIncrement)
        }

        return toSave
    }


    private UserPoints constructUserPoints(String userId, String projectId, Integer skillRefId, String skillId, Date day, Integer pointIncrement) {
        return new UserPoints(
                userId: userId.toLowerCase(),
                projectId: projectId,
                skillId: skillId,
                skillRefId: skillRefId,
                points: pointIncrement,
                day: day)
    }

}


