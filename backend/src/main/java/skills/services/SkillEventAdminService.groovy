package skills.services


import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.services.events.CompletionItem
import skills.services.events.SkillEventResult
import skills.storage.model.*
import skills.storage.repos.*

@Component
@Slf4j
class SkillEventAdminService {

    @Autowired
    UserPerformedSkillRepo performedSkillRepository

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Transactional
    SkillEventResult deleteSkillEvent(String projectId, String skillId, String userId, Long timestamp) {
        List<UserPerformedSkill> performedSkills = performedSkillRepository.findAllByProjectIdAndSkillIdAndUserIdAndPerformedOn(projectId, skillId, userId, new Date(timestamp))
        if (!performedSkills) {
            throw new SkillException("This skill event does not exist", projectId, skillId, ErrorCode.BadParam)
        }
        // may have more than 1 event with the same exact timestamp, this happens when multiple events may fall
        // within configured time window and client send the same timestamp (example UI calendar control)
        UserPerformedSkill performedSkill = performedSkills.first()
        log.debug("Deleting skill [{}] for user [{}]", performedSkill, userId)

        SkillEventResult res = new SkillEventResult()

        SkillEventsSupportRepo.SkillDefMin skillDefinitionMin = getSkillDef(projectId, skillId)

        Long numExistingSkills = performedSkillRepository.countByUserIdAndProjectIdAndSkillId(userId, projectId, skillId)
        numExistingSkills = numExistingSkills ?: 0 // account for null

        List<SkillDef> performedDependencies = performedSkillRepository.findPerformedParentSkills(userId, projectId, skillId)
        if (performedDependencies) {
            res.skillApplied = false
            res.explanation = "You cannot delete a skill event when a parent skill dependency has already been performed. You must first delete " +
                    "the performed skills for the parent dependencies: ${performedDependencies.collect({ it.projectId + ":" + it.skillId })}."
            return res
        }

        updateUserPoints(userId, skillDefinitionMin, performedSkill.performedOn, skillId)
        boolean requestedSkillCompleted = hasReachedMaxPoints(numExistingSkills, skillDefinitionMin)
        if (requestedSkillCompleted) {
            checkForBadgesAchieved(userId, skillDefinitionMin)
            //this removes the skill achievements
            achievedLevelRepo.deleteByProjectIdAndSkillIdAndUserIdAndLevel(performedSkill.projectId, performedSkill.skillId, userId, null)
        }
        checkParentGraph(performedSkill.performedOn, res, userId, skillDefinitionMin)
        deleteProjectLevelIfNecessary(performedSkill.projectId, userId)
        performedSkillRepository.delete(performedSkill)

        return res
    }

    private void deleteProjectLevelIfNecessary(String projectId, String userId) {
        //get user points for Project, if less then UserAchievement.pointsWhenAchieved for highest project level, remove UserAchievement
        Integer userProjectPoints = userPointsRepo.getPointsByProjectIdAndUserId(projectId, userId)
        List<UserAchievement> achievements = achievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, null)
        def orderedLevels = achievements.findAll(){ it.level }.sort() { it.level }

        if (orderedLevels) {
            orderedLevels.reverse().each {
                if (it.pointsWhenAchieved > userProjectPoints) {
                    log.debug("deleting achievement ${it}, User no longer has enough points")
                    achievedLevelRepo.delete(it)
                }
            }
        }
    }

    private void checkForBadgesAchieved(String userId, SkillEventsSupportRepo.SkillDefMin currentSkillDef) {
        List<SkillRelDef> parentsRels = skillRelDefRepo.findAllByChildIdAndType(currentSkillDef.id, SkillRelDef.RelationshipType.BadgeRequirement)
        parentsRels.each {
            if (it.parent.type == SkillDef.ContainerType.Badge && withinActiveTimeframe(it.parent)) {
                SkillDef badge = it.parent
                List<SkillDef> nonAchievedChildren = achievedLevelRepo.findNonAchievedChildren(userId, badge.projectId, badge.skillId, SkillRelDef.RelationshipType.BadgeRequirement)
                if (!nonAchievedChildren) {
                    achievedLevelRepo.deleteByProjectIdAndSkillIdAndUserIdAndLevel(badge.projectId, badge.skillId, userId, null)
                }
            }
        }
    }

    private boolean withinActiveTimeframe(SkillDef skillDef) {
        boolean withinActiveTimeframe = true;
        if (skillDef.startDate && skillDef.endDate) {
            Date now = new Date()
            withinActiveTimeframe = skillDef.startDate.before(now) && skillDef.endDate.after(now)
        }
        return withinActiveTimeframe
    }

    private boolean hasReachedMaxPoints(long numSkills, SkillEventsSupportRepo.SkillDefMin skillDefinition) {
        return numSkills * skillDefinition.pointIncrement >= skillDefinition.totalPoints
    }

    private UserPoints updateUserPoints(String userId, SkillEventsSupportRepo.SkillDefMin requestedSkill, Date incomingSkillDate, String skillId = null) {
        doUpdateUserPoints(requestedSkill, userId, incomingSkillDate, skillId)
        return doUpdateUserPoints(requestedSkill, userId, null, skillId)
    }

    private UserPoints doUpdateUserPoints(SkillEventsSupportRepo.SkillDefMin requestedSkill, String userId, Date incomingSkillDate, String skillId) {
        Date day = incomingSkillDate ? new Date(incomingSkillDate.time).clearTime() : null
        UserPoints userPoints = userPointsRepo.findByProjectIdAndUserIdAndSkillIdAndDay(requestedSkill.projectId, userId, skillId, day)
        userPoints.points -= requestedSkill.pointIncrement

        if (userPoints.points <= 0) {
            userPointsRepo.delete(userPoints)
        } else {
            userPointsRepo.save(userPoints)
        }

        return userPoints
    }

    private void checkParentGraph(Date incomingSkillDate, SkillEventResult res, String userId, SkillEventsSupportRepo.SkillDefMin skillDef) {
        updateByTraversingUpSkillDefs(incomingSkillDate, res, skillDef, skillDef, userId)

        // updated project level
        updateUserPoints(userId, skillDef, incomingSkillDate, null)
    }

    private void updateByTraversingUpSkillDefs(Date incomingSkillDate, SkillEventResult res,
                                               SkillEventsSupportRepo.SkillDefMin currentDef,
                                               SkillEventsSupportRepo.SkillDefMin requesterDef,
                                               String userId) {
        if (currentDef.type == SkillDef.ContainerType.Subject) {
            UserPoints updatedPoints = updateUserPoints(userId, requesterDef, incomingSkillDate, currentDef.skillId)

            List<LevelDef> levelDefs = skillEventsSupportRepo.findLevelsBySkillId(currentDef.id)
            int currentScore = updatedPoints.points
            LevelDefinitionStorageService.LevelInfo levelInfo = levelDefService.getLevelInfo(currentDef.projectId, levelDefs, currentDef.totalPoints, currentScore)
            calculateLevels(levelInfo, updatedPoints, userId)
        }

        List<SkillEventsSupportRepo.SkillDefMin> parentsRels = skillEventsSupportRepo.findParentSkillsByChildIdAndType(currentDef.id, SkillRelDef.RelationshipType.RuleSetDefinition)
        parentsRels?.each {
            updateByTraversingUpSkillDefs(incomingSkillDate, res, it, requesterDef, userId)
        }
    }

    private CompletionItem calculateLevels(LevelDefinitionStorageService.LevelInfo levelInfo, UserPoints userPts, String userId) {
        CompletionItem res

        List<UserAchievement> userAchievedLevels = achievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(userId, userPts.projectId, userPts.skillId)

        // we are decrementing, so we need to remove any level that is greater than the current level
        List<UserAchievement> levelsToRemove = userAchievedLevels?.findAll { it.level > levelInfo.level }
        if (levelsToRemove) {
            achievedLevelRepo.deleteAll(levelsToRemove)
        }

        return res
    }

    private SkillEventsSupportRepo.SkillDefMin getSkillDef(String projectId, String skillId) {
        SkillEventsSupportRepo.SkillDefMin skillDefinition = skillEventsSupportRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, SkillDef.ContainerType.Skill)
        if (!skillDefinition) {
            throw new SkillException("Skill definition does not exist. Must create the skill definition first!", projectId, skillId)
        }
        return skillDefinition
    }

}
