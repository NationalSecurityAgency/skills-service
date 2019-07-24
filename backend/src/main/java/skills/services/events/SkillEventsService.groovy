package skills.services.events

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
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
class SkillEventsService {

    @Autowired
    UserPerformedSkillRepo performedSkillRepository

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    TimeWindowHelper timeWindowHelper

    @Autowired
    CheckDependenciesHelper checkDependenciesHelper

    @Autowired
    CheckRecommendationHelper checkRecommendationHelper

    @Autowired
    HandleAchievementAndPointsHelper handleAchievementAndPointsHelper

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

        Long numExistingSkills = getNumExistingSkills(userId, projectId, skillId)
        numExistingSkills = numExistingSkills ?: 0 // account for null

        List<SkillDef> performedDependencies = performedSkillRepository.findPerformedParentSkills(userId, projectId, skillId)
        if (performedDependencies) {
            res.skillApplied = false
            res.explanation = "You cannot delete a skill event when a parent skill dependency has already been performed. You must first delete " +
                    "the performed skills for the parent dependencies: ${performedDependencies.collect({ it.projectId + ":" + it.skillId})}."
            return res
        }

        boolean decrement = true
        handleAchievementAndPointsHelper.updateUserPoints(userId, skillDefinition, performedSkill.performedOn, skillId, decrement)
        boolean requestedSkillCompleted = hasReachedMaxPoints(numExistingSkills, skillDefinition)
        if (requestedSkillCompleted) {
            checkForBadgesAchieved(res, userId, skillDefinition, decrement)
            achievedLevelRepo.deleteByProjectIdAndSkillIdAndUserIdAndLevel(performedSkill.projectId, performedSkill.skillId, userId, null)
        }
        handleAchievementAndPointsHelper.checkParentGraph(performedSkill.performedOn, res, userId, skillDefinition, decrement)
        performedSkillRepository.delete(performedSkill)

        return res
    }

    @Transactional
    @Profile
    SkillEventResult reportSkill(String projectId, String skillId, String userId, Date incomingSkillDate = new Date()) {
        assert projectId
        assert skillId

        // TODO: make a builder for the class
        SkillEventResult res = new SkillEventResult()

        SkillDef skillDefinition = getSkillDef(projectId, skillId)

        Long numExistingSkills = getNumExistingSkills(userId, projectId, skillId)
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

        CheckDependenciesHelper.DependencyCheckRes dependencyCheckRes = checkDependenciesHelper.check(userId, projectId, skillId)
        if (dependencyCheckRes.hasNotAchievedDependents) {
            res.skillApplied = false
            res.explanation = dependencyCheckRes.msg
            return res
        }

        boolean decrement = false
        UserPerformedSkill performedSkill = new UserPerformedSkill(userId: userId, skillId: skillId, projectId: projectId, performedOn: incomingSkillDate, skillRefId: skillDefinition.id )
        savePerformedSkill(performedSkill)
        handleAchievementAndPointsHelper.updateUserPoints(userId, skillDefinition, incomingSkillDate, skillId, decrement)

        boolean requestedSkillCompleted = hasReachedMaxPoints(numExistingSkills + 1, skillDefinition)
        if (requestedSkillCompleted) {
            documentSkillAchieved(userId, numExistingSkills, skillDefinition, res)
            checkForBadgesAchieved(res, userId, skillDefinition, decrement)
        }

        handleAchievementAndPointsHelper.checkParentGraph(incomingSkillDate, res, userId, skillDefinition, decrement)

        return res
    }

    @Profile
    private void savePerformedSkill(UserPerformedSkill performedSkill) {
        performedSkillRepository.save(performedSkill)
        log.debug("Saved skill [{}]", performedSkill)
    }

    @Profile
    private long getNumExistingSkills(String userId, String projectId, String skillId) {
        performedSkillRepository.countByUserIdAndProjectIdAndSkillId(userId, projectId, skillId)
    }

    @Profile
    private SkillDef getSkillDef(String projectId, String skillId) {
        SkillDef skillDefinition = skillDefRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, SkillDef.ContainerType.Skill)
        if (!skillDefinition) {
            throw new SkillException("Skill definition does not exist. Must create the skill definition first!", projectId, skillId)
        }
        return skillDefinition
    }

    @Profile
    private void documentSkillAchieved(String userId, long numExistingSkills, SkillDef skillDefinition, SkillEventResult res) {
        UserAchievement skillAchieved = new UserAchievement(userId: userId, projectId: skillDefinition.projectId, skillId: skillDefinition.skillId, skillDef: skillDefinition,
                pointsWhenAchieved: ((numExistingSkills.intValue() + 1) * skillDefinition.pointIncrement))
        achievedLevelRepo.save(skillAchieved)

        List<RecommendationItem> recommendationItems = checkRecommendationHelper.checkForRecommendations(userId, skillDefinition.projectId, skillDefinition.skillId)
        // only return first 10
        recommendationItems = recommendationItems?.take(10)
        res.completed.add(new CompletionItem(type: CompletionItem.CompletionItemType.Skill, id: skillDefinition.skillId, name: skillDefinition.name, recommendations: recommendationItems))
    }


    @Profile
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
                            res.completed.add(new CompletionItem(type: CompletionTypeUtil.getCompletionType(badge), id: badge.skillId, name: badge.name))
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


    private boolean hasReachedMaxPoints(long numSkills, SkillDef skillDefinition) {
        return numSkills * skillDefinition.pointIncrement >= skillDefinition.totalPoints
    }

}
