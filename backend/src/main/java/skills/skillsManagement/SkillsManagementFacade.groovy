package skills.skillsManagement


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.services.LevelDefinitionStorageService
import skills.storage.model.ProjDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.UserPerformedSkillRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo
import skills.storage.model.UserPerformedSkill
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints

@Service
@CompileStatic
@Slf4j
class SkillsManagementFacade {

    @Value('#{"${skills.subjects.minimumPoints:20}"}')
    int minimumSubjectPoints

    @Value('#{"${skills.project.minimumPoints:20}"}')
    int minimumProjectPoints

    @Autowired
    UserPerformedSkillRepo performedSkillRepository

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    UserPointsRepo userSubjectPointsRepository

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    TimeWindowHelper timeWindowHelper

    static class SkillEventResult {
        boolean success = true
        boolean skillApplied = true
        // only really applicable if it wasn't performed
        String explanation = "Skill event was applied"
        List<CompletionItem> completed = []
    }

    static class CompletionItem {
        static enum CompletionItemType {
            Overall, Subject, Skill, Badge
        };
        CompletionItemType type
        Integer level // optional
        String id
        String name
        List<RecommendationItem> recommendations = []
    }

    static class RecommendationItem {
        String id
        String name
    }
    @Transactional
    SkillEventResult deleteSkillEvent(Integer skillPkId) {
        assert skillPkId
        Optional<UserPerformedSkill> existing = performedSkillRepository.findById(skillPkId)
        assert existing.present, "Skill [${skillPkId}] with id [${skillPkId}] does not exist"
        UserPerformedSkill performedSkill = existing.get()
        String projectId = performedSkill.projectId
        String skillId = performedSkill.skillId
        String userId = performedSkill.userId
        log.debug("Deleting skill [{}] for user [{}]", performedSkill, userId)

        SkillEventResult res = new SkillEventResult()

        SkillDef skillDefinition = skillDefRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, SkillDef.ContainerType.Skill)
        if (!skillDefinition) {
            throw new skills.controller.exceptions.SkillException("Skill definition does not exist!", projectId, skillId)
        }

        Long numExistingSkills = performedSkillRepository.countByUserIdAndProjectIdAndSkillId(userId, projectId, skillId)
        numExistingSkills = numExistingSkills ?: 0 // account for null

        List<SkillDef> performedDependencies = performedSkillRepository.findPerformedParentSkills(userId, projectId, skillId)
        if (performedDependencies) {
            res.skillApplied = false
            res.explanation = "You cannot delete a skill event when a parent skill dependency has already been performed. You must first delete " +
                    "the performed skills for the parent dependencies: ${performedDependencies.collect({ it.projectId + ":" + it.skillId})}."
            return res
        }

        boolean decrement = true
        updateUserPoints(userId, skillDefinition, performedSkill.performedOn, skillId, decrement)
        boolean requestedSkillCompleted = hasReachedMaxPoints(numExistingSkills, skillDefinition)
        if (requestedSkillCompleted) {
            checkForBadgesAchieved(res, userId, skillDefinition, decrement)
            achievedLevelRepo.deleteByProjectIdAndSkillIdAndUserIdAndLevel(performedSkill.projectId, performedSkill.skillId, userId, null)
        }
        checkParentGraph(performedSkill.performedOn, res, userId, skillDefinition, decrement)
        performedSkillRepository.delete(performedSkill)

        return res
    }

    @Transactional
    SkillEventResult addSkill(String projectId, String skillId, String userId, Date incomingSkillDate = new Date()) {
        assert projectId
        assert skillId

        // TODO: make a builder for the class
        SkillEventResult res = new SkillEventResult()

        SkillDef skillDefinition = skillDefRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, SkillDef.ContainerType.Skill)
        if (!skillDefinition) {
            throw new skills.controller.exceptions.SkillException("Skill definition does not exist. Must create the skill definition first!", projectId, skillId)
        }

        Long numExistingSkills = performedSkillRepository.countByUserIdAndProjectIdAndSkillId(userId, projectId, skillId)
        numExistingSkills = numExistingSkills ?: 0 // account for null

        if (hasReachedMaxPoints(numExistingSkills, skillDefinition)) {
            res.skillApplied = false
            res.explanation = "This skill reached its maximum points"
            return res
        }

        TimeWindowHelper.TimeWindowRes timeWindowRes = timeWindowHelper.checkTimeWindow(skillDefinition, userId, incomingSkillDate)
        if (timeWindowRes.isFull()) {
            res.skillApplied = false
            res.explanation = timeWindowRes.msg
            return res
        }

        List<UserAchievedLevelRepo.ChildWithAchievementsInfo> dependentsAndAchievements = achievedLevelRepo.findChildrenAndTheirAchievements(userId, projectId, skillId, SkillRelDef.RelationshipType.Dependence)
        List<UserAchievedLevelRepo.ChildWithAchievementsInfo> notAchievedDependents = dependentsAndAchievements.findAll({
            !it.childAchievedSkillId
        })
        if (notAchievedDependents) {
            res.skillApplied = false
            List<UserAchievedLevelRepo.ChildWithAchievementsInfo> sorted = notAchievedDependents.sort({ a, b -> a.childProjectId <=> b.childProjectId ?: a.childSkillId <=> b.childSkillId })
            res.explanation = "Not all dependent skills have been achieved. Missing achievements for ${notAchievedDependents.size()} out of ${dependentsAndAchievements.size()}. " +
                    "Waiting on completion of ${sorted.collect({ it.childProjectId + ":" + it.childSkillId})}."
            return res
        }

        boolean decrement = false
        UserPerformedSkill performedSkill = new UserPerformedSkill(userId: userId, skillId: skillId, projectId: projectId, performedOn: incomingSkillDate)
        performedSkillRepository.save(performedSkill)
        log.debug("Saved skill [{}]", performedSkill)
        updateUserPoints(userId, skillDefinition, incomingSkillDate, skillId, decrement)

        boolean requestedSkillCompleted = hasReachedMaxPoints(numExistingSkills + 1, skillDefinition)
        if (requestedSkillCompleted) {
            documentSkillAchieved(userId, numExistingSkills, skillDefinition, res)
            checkForBadgesAchieved(res, userId, skillDefinition, decrement)
        }

        checkParentGraph(incomingSkillDate, res, userId, skillDefinition, decrement)

        return res
    }

    private void documentSkillAchieved(String userId, long numExistingSkills, SkillDef skillDefinition, SkillEventResult res) {
        UserAchievement skillAchieved = new UserAchievement(userId: userId, projectId: skillDefinition.projectId, skillId: skillDefinition.skillId, skillDef: skillDefinition,
                pointsWhenAchieved: ((numExistingSkills.intValue() + 1) * skillDefinition.pointIncrement))
        achievedLevelRepo.save(skillAchieved)

        List<RecommendationItem> recommendationItems = checkForRecommendations(userId, skillDefinition.projectId, skillDefinition.skillId)
        // only return first 10
        recommendationItems = recommendationItems?.take(10)
        res.completed.add(new CompletionItem(type: CompletionItem.CompletionItemType.Skill, id: skillDefinition.skillId, name: skillDefinition.name, recommendations: recommendationItems))
    }

    private List<RecommendationItem> checkForRecommendations(String userId, String projectId, String skillId) {
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

    private void checkForBadgesAchieved(SkillEventResult res, String userId, SkillDef currentSkillDef, boolean decrement) {
        List<SkillRelDef> parentsRels = skillRelDefRepo.findAllByChildAndType(currentSkillDef, SkillRelDef.RelationshipType.BadgeDependence)
        parentsRels.each {
            if (it.parent.type == SkillDef.ContainerType.Badge && withinActiveTimeframe(it.parent)) {
                SkillDef badge = it.parent
                List<SkillDef> nonAchievedChildren = achievedLevelRepo.findNonAchievedChildren(userId, badge.projectId, badge.skillId, SkillRelDef.RelationshipType.BadgeDependence)
                if (!nonAchievedChildren) {
                    if (!decrement) {
                        List<UserAchievement> badges = achievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(userId, badge.projectId, badge.skillId)
                        if (!badges) {
                            UserAchievement groupAchievement = new UserAchievement(userId: userId, projectId: badge.projectId, skillId: badge.skillId, skillDef: badge)
                            achievedLevelRepo.save(groupAchievement)
                            res.completed.add(new CompletionItem(type: getCompletionType(badge), id: badge.skillId, name: badge.name))
                        }
                    } else {
                        achievedLevelRepo.deleteByProjectIdAndSkillIdAndUserIdAndLevel(badge.projectId, badge.skillId, userId, null)
                    }
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

    private void checkParentGraph(Date incomingSkillDate, SkillEventResult res, String userId, SkillDef skillDef, boolean decrement) {
        updateByTraversingUpSkillDefs(incomingSkillDate, res, skillDef, skillDef, userId, decrement)

        // updated project level
        UserPoints totalPoints = updateUserPoints(userId, skillDef, incomingSkillDate, null, decrement)
        ProjDef projDef = projDefRepo.findByProjectId(skillDef.projectId)
        if(projDef.totalPoints < minimumProjectPoints){
            throw new skills.controller.exceptions.SkillException("Insufficient project points, skill achievement is disallowed", projDef.projectId)
        }
        if (!decrement) {
            LevelDefinitionStorageService.LevelInfo levelInfo = levelDefService.getOverallLevelInfo(projDef, totalPoints.points)
            CompletionItem completionItem = calculateLevels(levelInfo, totalPoints, null, userId, "OVERALL", decrement)
            if (completionItem?.level && completionItem?.level > 0) {
                res.completed.add(completionItem)
            }
        }
    }

    private boolean shouldEvaluateForAchievement(SkillDef skillDef) {
        skillDef.type == SkillDef.ContainerType.Subject
    }

    private CompletionItem.CompletionItemType getCompletionType(SkillDef skillDef){
        switch (skillDef.type) {
            case SkillDef.ContainerType.Subject:
                return CompletionItem.CompletionItemType.Subject
            case SkillDef.ContainerType.Badge:
                return CompletionItem.CompletionItemType.Badge
            default:
                throw new IllegalStateException("this method doesn't support type $skillDef.type")
        }
    }

    private void updateByTraversingUpSkillDefs(Date incomingSkillDate, SkillEventResult res, SkillDef currentDef, SkillDef requesterDef, String userId, boolean decrement) {
        if (shouldEvaluateForAchievement(currentDef)) {
            UserPoints updatedPoints = updateUserPoints(userId, requesterDef, incomingSkillDate, currentDef.skillId, decrement)

            boolean hasLevelDefs = currentDef.levelDefinitions
            if (!hasLevelDefs) {
                if (!decrement && updatedPoints.points >= currentDef.totalPoints) {
                    UserAchievement groupAchievement = new UserAchievement(userId: userId, projectId: currentDef.projectId, skillId: currentDef.skillId, skillDef: currentDef,
                            pointsWhenAchieved: updatedPoints.points)
                    achievedLevelRepo.save(groupAchievement)

                    res.completed.add(new CompletionItem(type: getCompletionType(currentDef), id: currentDef.skillId, name: currentDef.name))
                } else if (decrement && updatedPoints.points <= currentDef.totalPoints) {
                    // we are decrementing, there are no levels defined and points are less that total points so we need
                    // to delete previously added achievement if it exists
                    achievedLevelRepo.deleteByProjectIdAndSkillIdAndUserIdAndLevel(currentDef.projectId, currentDef.skillId, userId, null)
                }
            } else if (hasLevelDefs) {
                if (currentDef.totalPoints < minimumSubjectPoints) {
                    throw new skills.controller.exceptions.SkillException("Insufficient Subject points, skill achievement is disallowed", currentDef.skillId)
                }
                LevelDefinitionStorageService.LevelInfo levelInfo = levelDefService.getLevelInfo(currentDef, decrement ? updatedPoints.points + requesterDef.pointIncrement : updatedPoints.points)
                CompletionItem completionItem = calculateLevels(levelInfo, updatedPoints, currentDef,  userId, currentDef.name, decrement)
                if (!decrement && completionItem?.level && completionItem?.level > 0) {
                    res.completed.add(completionItem)
                }
            }
        }

        List<SkillRelDef> parentsRels = skillRelDefRepo.findAllByChildAndType(currentDef, SkillRelDef.RelationshipType.RuleSetDefinition)
        parentsRels?.each {
            updateByTraversingUpSkillDefs(incomingSkillDate, res, it.parent, requesterDef, userId, decrement)
        }
    }

    /**
     * @param skillId if null then will document it at overall project level
     */
    private UserPoints updateUserPoints(String userId, SkillDef requestedSkill, Date incomingSkillDate, String skillId = null, boolean decrement) {
        doUpdateUserPoints(requestedSkill, userId, incomingSkillDate, skillId, decrement)
        UserPoints res = doUpdateUserPoints(requestedSkill, userId, null, skillId, decrement)
        return res
    }

    private UserPoints doUpdateUserPoints(SkillDef requestedSkill, String userId, Date incomingSkillDate, String skillId, boolean decrement) {
        Date day = incomingSkillDate ? new Date(incomingSkillDate.time).clearTime() : null
        UserPoints subjectPoints = userSubjectPointsRepository.findByProjectIdAndUserIdAndSkillIdAndDay(requestedSkill.projectId, userId, skillId, day)
        if (!subjectPoints) {
            assert !decrement
            subjectPoints = new UserPoints(userId: userId, projectId: requestedSkill.projectId, skillId: skillId, points: requestedSkill.pointIncrement, day: day)
        } else {
            if (decrement) {
                subjectPoints.points -= requestedSkill.pointIncrement
            } else {
                subjectPoints.points += requestedSkill.pointIncrement
            }
        }

        UserPoints res
        if (decrement && subjectPoints.points <= 0) {
            userSubjectPointsRepository.delete(subjectPoints)
            res = new UserPoints(userId: userId, projectId: requestedSkill.projectId, skillId: skillId, points: 0, day: day)
        } else {
            res = userSubjectPointsRepository.save(subjectPoints)
            log.debug("Updated points [{}]", res)
        }
        res
    }

    private CompletionItem calculateLevels(LevelDefinitionStorageService.LevelInfo levelInfo, UserPoints userPts, SkillDef skillDef, String userId, String name, boolean decrement) {
        CompletionItem res

        List<UserAchievement> userAchievedLevels = achievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(userId, userPts.projectId, userPts.skillId)
        boolean levelAlreadyAchieved = userAchievedLevels?.find { it.level == levelInfo.level }
        if (!levelAlreadyAchieved && !decrement) {
            UserAchievement newLevel = new UserAchievement(userId: userId, projectId: userPts.projectId, skillId: userPts.skillId, skillDef: skillDef,
                    level: levelInfo.level, pointsWhenAchieved: userPts.points)
            achievedLevelRepo.save(newLevel)
            log.info("Achieved new level [{}]", newLevel)

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

    private boolean hasReachedMaxPoints(long numSkills, SkillDef skillDefinition) {
        return numSkills * skillDefinition.pointIncrement >= skillDefinition.totalPoints
    }


}


