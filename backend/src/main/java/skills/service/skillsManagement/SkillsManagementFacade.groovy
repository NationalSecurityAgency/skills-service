package skills.service.skillsManagement

import groovy.time.TimeCategory
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.service.controller.exceptions.SkillException
import skills.service.datastore.services.LevelDefinitionStorageService
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

    static class AddSkillResult {
        boolean wasPerformed = true
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
    AddSkillResult addSkill(String projectId, String skillId, String userId, Date incomingSkillDate = new Date()) {
        assert projectId
        assert skillId
        // TODO: make a builder for the class
        AddSkillResult res = new AddSkillResult()

        SkillDef skillDefinition = skillDefRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, SkillDef.ContainerType.Skill)
        if (!skillDefinition) {
            throw new SkillException("Skill definition does not exist. Must create the skill definition first!", projectId, skillId)
        }
        Long numExistingSkills = performedSkillRepository.countByUserIdAndProjectIdAndSkillId(userId, projectId, skillId)
        numExistingSkills = numExistingSkills ?: 0 // account for null

        if (hasReachedMaxPoints(numExistingSkills, skillDefinition)) {
            res.wasPerformed = false
            res.explanation = "This skill reached its maximum points"
            return res
        }

        if (isInsideTimePeriod(skillDefinition, userId, incomingSkillDate)) {
            res.wasPerformed = false
            res.explanation = "This skill was already performed within the configured time period (within the last ${skillDefinition.pointIncrementInterval} hours)"
            return res
        }

        List<UserAchievedLevelRepo.ChildWithAchievementsInfo> dependentsAndAchievements = achievedLevelRepo.findChildrenAndTheirAchievements(userId, projectId, skillId, SkillRelDef.RelationshipType.Dependence)
        List<UserAchievedLevelRepo.ChildWithAchievementsInfo> notAchievedDependents = dependentsAndAchievements.findAll({
            !it.childAchievedSkillId
        })
        if (notAchievedDependents) {
            res.wasPerformed = false
            res.explanation = "Not all dependent skills have been achieved. Missing achievements for ${notAchievedDependents.size()} out of ${dependentsAndAchievements.size()}. " +
                    "Waiting on completion of ${notAchievedDependents.collect({ it.childProjectId + ":" + it.childSkillId})}."
            return res
        }

        UserPerformedSkill performedSkill = new UserPerformedSkill(userId: userId, skillId: skillId, projectId: projectId, performedOn: incomingSkillDate)
        performedSkillRepository.save(performedSkill)
        log.debug("Saved skill [{}]", performedSkill)
        updateUserPoints(userId, skillDefinition, incomingSkillDate, skillId)

        boolean requestedSkillCompleted = hasReachedMaxPoints(numExistingSkills + 1, skillDefinition)
        if (requestedSkillCompleted) {
            documentSkillAchieved(userId, numExistingSkills, skillDefinition, res)
            checkForBadgesAchieved(res, userId, skillDefinition)
        }

        checkParentGraph(incomingSkillDate, res, userId, skillDefinition)

        return res
    }

    private void documentSkillAchieved(String userId, long numExistingSkills, SkillDef skillDefinition, AddSkillResult res) {
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

    private void checkForBadgesAchieved(AddSkillResult res, String userId, SkillDef currentSkillDef) {
        List<SkillRelDef> parentsRels = skillRelDefRepo.findAllByChildAndType(currentSkillDef, SkillRelDef.RelationshipType.BadgeDependence)
        parentsRels.each {
            if (it.parent.type == SkillDef.ContainerType.Badge && withinActiveTimeframe(it.parent)) {
                SkillDef badge = it.parent
                List<SkillDef> nonAchievedChildren = achievedLevelRepo.findNonAchievedChildren(userId, badge.projectId, badge.skillId, SkillRelDef.RelationshipType.BadgeDependence)
                if (!nonAchievedChildren) {
                    UserAchievement groupAchievement = new UserAchievement(userId: userId, projectId: badge.projectId, skillId: badge.skillId, skillDef: badge)
                    achievedLevelRepo.save(groupAchievement)
                    res.completed.add(new CompletionItem(type: getCompletionType(badge), id: badge.skillId, name: badge.name))
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

    private void checkParentGraph(Date incomingSkillDate, AddSkillResult res, String userId, SkillDef skillDef) {
        updateByTraversingUpSkillDefs(incomingSkillDate, res, skillDef, skillDef, userId)

        // updated project level
        UserPoints totalPoints = updateUserPoints(userId, skillDef, incomingSkillDate)
        ProjDef projDef = projDefRepo.findByProjectId(skillDef.projectId)
        LevelDefinitionStorageService.LevelInfo levelInfo = levelDefService.getOverallLevelInfo(projDef, totalPoints.points)
        CompletionItem completionItem = calculateLevels(levelInfo, totalPoints, null, userId, "OVERALL")
        if (completionItem?.level && completionItem?.level > 0) {
            res.completed.add(completionItem)
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

    private void updateByTraversingUpSkillDefs(Date incomingSkillDate, AddSkillResult res, SkillDef currentDef, SkillDef requesterDef, String userId) {
        if (shouldEvaluateForAchievement(currentDef)) {
            UserPoints updatedPoints = updateUserPoints(userId, requesterDef, incomingSkillDate, currentDef.skillId)

            boolean hasLevelDefs = currentDef.levelDefinitions
            if (!hasLevelDefs & updatedPoints.points >= currentDef.totalPoints) {
                UserAchievement groupAchievement = new UserAchievement(userId: userId, projectId: currentDef.projectId, skillId: currentDef.skillId, skillDef: currentDef,
                        pointsWhenAchieved: updatedPoints.points)
                achievedLevelRepo.save(groupAchievement)

                res.completed.add(new CompletionItem(type: getCompletionType(currentDef), id: currentDef.skillId, name: currentDef.name))
            } else if (hasLevelDefs){
                LevelDefinitionStorageService.LevelInfo levelInfo = levelDefService.getLevelInfo(currentDef, updatedPoints.points)
                CompletionItem completionItem = calculateLevels(levelInfo, updatedPoints, currentDef,  userId, currentDef.name)
                if (completionItem?.level && completionItem?.level > 0) {
                    res.completed.add(completionItem)
                }
            }
        }

        List<SkillRelDef> parentsRels = skillRelDefRepo.findAllByChildAndType(currentDef, SkillRelDef.RelationshipType.RuleSetDefinition)
        parentsRels?.each {
            updateByTraversingUpSkillDefs(incomingSkillDate, res, it.parent, requesterDef, userId)
        }
    }

    /**
     * @param skillId if null then will document it at overall project level
     */
    private UserPoints updateUserPoints(String userId, SkillDef requestedSkill, Date incomingSkillDate, String skillId = null) {
        doUpdateUserPoints(requestedSkill, userId, incomingSkillDate, skillId)
        UserPoints res = doUpdateUserPoints(requestedSkill, userId, null, skillId)
        return res
    }

    private UserPoints doUpdateUserPoints(SkillDef requestedSkill, String userId, Date incomingSkillDate, String skillId) {
        Date day = incomingSkillDate ? new Date(incomingSkillDate.time).clearTime() : null
        UserPoints subjectPoints = userSubjectPointsRepository.findByProjectIdAndUserIdAndSkillIdAndDay(requestedSkill.projectId, userId, skillId, day)
        if (!subjectPoints) {
            subjectPoints = new UserPoints(userId: userId, projectId: requestedSkill.projectId, skillId: skillId, points: requestedSkill.pointIncrement, day: day)
        } else {
            subjectPoints.points += requestedSkill.pointIncrement
        }
        UserPoints res = userSubjectPointsRepository.save(subjectPoints)
        log.debug("Updated points [{}]", res)
        res
    }

    private CompletionItem calculateLevels(LevelDefinitionStorageService.LevelInfo levelInfo, UserPoints userPts, SkillDef skillDef, String userId, String name) {
        CompletionItem res

        List<UserAchievement> userAchievedLevels = achievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(userId, userPts.projectId, userPts.skillId)
        if (!userAchievedLevels?.find { it.level == levelInfo.level }) {
            UserAchievement newLevel = new UserAchievement(userId: userId, projectId: userPts.projectId, skillId: userPts.skillId, skillDef: skillDef,
                    level: levelInfo.level, pointsWhenAchieved: userPts.points)
            achievedLevelRepo.save(newLevel)
            log.info("Achieved new level [{}]", newLevel)

            res = new CompletionItem(
                    level: newLevel.level, name: name,
                    id: userPts.skillId ?: "OVERALL",
                    type: userPts.skillId ? CompletionItem.CompletionItemType.Subject : CompletionItem.CompletionItemType.Overall)
        }

        return res
    }

    private boolean hasReachedMaxPoints(long numSkills, SkillDef skillDefinition) {
        return numSkills * skillDefinition.pointIncrement >= skillDefinition.totalPoints
    }

    @CompileDynamic
    private boolean isInsideTimePeriod(SkillDef skillDefinition, String userId, Date incomingSkillDate) {

        // because incoming date may be provided in the past (ex. automated job) then we simply have to make sure that
        // there is no other skill before or after skillDefinition.pointIncrementInterval
        Date checkStartDate
        Date checkEndDate
        use(TimeCategory) {
            checkStartDate = incomingSkillDate - skillDefinition.pointIncrementInterval.hours
            checkEndDate = incomingSkillDate + skillDefinition.pointIncrementInterval.hours
        }
        if (log.isDebugEnabled()) {
            log.debug("Looking for [$skillDefinition.skillId] between [$checkStartDate] and [$checkEndDate]")
        }

        Long count = performedSkillRepository.countByUserIdAndProjectIdAndSkillIdAndPerformedOnGreaterThanAndPerformedOnLessThan(
                userId,
                skillDefinition.projectId,
                skillDefinition.skillId,
                checkStartDate,
                checkEndDate
        )
        return count > 0
    }
}


