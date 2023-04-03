/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.services

import callStack.profiler.Profile
import callStack.utils.CachedThreadPool
import callStack.utils.ThreadPoolUtils
import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.result.model.RequestResult
import skills.services.admin.SkillCatalogService
import skills.services.events.*
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.*
import skills.storage.repos.*
import skills.tasks.TaskSchedulerService
import skills.utils.MetricsLogger

import java.util.concurrent.Callable

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
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Autowired
    UserEventService userEventService

    @Autowired
    private SkillEventsService skillsManagementFacade

    @Autowired
    private UserInfoService userInfoService

    @Autowired
    SkillEventPublisher skillEventPublisher

    @Autowired
    MetricsLogger metricsLogger

    @Autowired
    SkillEventsTransactionalService skillEventsTransactionalService

    @Autowired
    SkillCatalogService skillCatalogService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    @Autowired
    GlobalBadgesService globalBadgesService

    @Autowired
    TaskSchedulerService taskSchedulerService

    @Value('#{"${skills.bulkUserLookup.minNumOfThreads:1}"}')
    Integer minNumOfThreads

    @Value('#{"${skills.bulkUserLookup.maxNumOfThreads:10}"}')
    Integer maxNumOfThreads

    private CachedThreadPool pool

    @PostConstruct
    void init() {
        pool = new CachedThreadPool('user-lookup', minNumOfThreads, maxNumOfThreads)
    }

    @Profile
    BulkSkillEventResult bulkReportSkills(String projectId, String skillId, List<String> userIds, Date incomingSkillDate) {
        // collect userIds outside of the DB transaction
        BulkUserLookupResult res = bulkLookupUserNames(userIds, projectId, skillId)

        if (!res) {
            log.warn("No user names for found for userIds [${userIds}]")
            return new BulkSkillEventResult(
                    projectId: projectId,
                    skillId: skillId,
                    userIdsErrored: userIds,
            )
        }

        // report all skills as a single transaction
        Map<String, SkillEventResult> results = bulkReportSkillsInternal(projectId, skillId, res.requestedUserIds, incomingSkillDate)
        if (!results) {
            log.warn("No skills were report for projectId [${projectId}], skillId [${skillId}], userIds [${userIds}]")
            return new BulkSkillEventResult(
                    projectId: projectId,
                    skillId: skillId,
                    userIdsErrored: userIds,
            )
        }

        // perform notification and metrics logging
        performBulkReportSkillNotifications(results)

        BulkSkillEventResult bulkResult = new BulkSkillEventResult(
                projectId: projectId,
                skillId: skillId,
                name: results.values().first().name,
                userIdsAppliedCount: results.values().count { it.skillApplied },
                userIdsNotAppliedCount: results.values().count { !it.skillApplied },
                userIdsErrored: res.userIdsErrored,
        )

        log.debug("Completed bulk skill report [${bulkResult}]")
        return bulkResult
    }

    @Profile
    BulkUserLookupResult bulkLookupUserNames(List<String> userIds, String projectId, String skillId) {
        BulkUserLookupResult res = new BulkUserLookupResult()
        List<Callable<String>> listToSubmit = userIds.collect { final requestedUserId ->
            ThreadPoolUtils.callable {
                String userId
                try {
                    userId = userInfoService.getUserName(requestedUserId, false)
                } catch (Exception e) {
                    log.warn("Error reporting skillId [${projectId}], [${skillId}] for user [${requestedUserId}]: [${e.message}]")
                    res.userIdsErrored.add(requestedUserId)
                }
                return userId
            }
        }
        res.requestedUserIds = pool.submitAndGetResults(listToSubmit)

        return res
    }

    @Profile
    @Transactional
    Map<String, SkillEventResult> bulkReportSkillsInternal(String projectId, String skillId, List<String> userIds, Date incomingSkillDate) {
        Map<String, SkillEventResult> results = [:]
        for (String userId : userIds) {
            SkillEventResult result = skillEventsTransactionalService.reportSkillInternal(projectId, skillId, userId, incomingSkillDate)
            results.put(userId, result)
        }
        return results
    }

    @Profile
    @Transactional
    RequestResult bulkDeleteSkillEventsForUser(String projectId, String userId) {
        performedSkillRepository.deleteAllByUserIdAndProjectId(userId, projectId)
        userEventService.removeAllEvents(projectId, userId)
        achievedLevelRepo.deleteAllByProjectIdAndUserId(projectId, userId)
        List<Integer> globalBadges = achievedLevelRepo.getAchievedGlobalBadgeForUserIntersectingProjectId(userId, projectId)
        achievedLevelRepo.deleteAllById(globalBadges)
        userPointsRepo.deleteAllByProjectIdAndUserId(projectId, userId)
        skillApprovalRepo.deleteAllByProjectIdAndUserId(projectId, userId)
        userQuizAttemptRepo.deleteAllAttemptsForQuizzesAssociatedToProjectAndByUserId(projectId, userId)

        propagateUpdatesToQuizSkillsAndImportedSkills(projectId, userId)

        return RequestResult.success()
    }

    @Profile
    private void propagateUpdatesToQuizSkillsAndImportedSkills(String projectId, String userId) {
        List<QuizToSkillDefRepo.ProjectIdAndSkillId> skillsToUpdateInOtherProjects = quizToSkillDefRepo.getOtherProjectsSkillRefIdsWithQuizzesInThisProject(projectId)
        Map<String, List<Integer>> projAndSkillIds = [:]
        Closure addToProjAndSkillIds = { String proj, List<Integer> toAdd ->
            List<Integer> skillRefIds = projAndSkillIds[proj]
            if (!skillRefIds) {
                projAndSkillIds[proj] = toAdd.toList()
            } else {
                skillRefIds.add(toAdd)
                skillRefIds = skillRefIds.unique()
                projAndSkillIds[proj] = skillRefIds
            }
        }

        // collect all of the skills that utilize the same quizzes of this project in another project(s)
        Map<String, List<QuizToSkillDefRepo.ProjectIdAndSkillId>> byProjId = skillsToUpdateInOtherProjects.groupBy { it.getProjectId() }
        byProjId.each {
            List<Integer> skillRefIds = it.value.collect { it.skillRefId }
            addToProjAndSkillIds(it.key, skillRefIds)

            // follow the projects where quizzes are used to see if those skills were imported elsewhere
            List<SkillDefRepo.ProjectIdAndSkill> importedSkillsFromQuizFollowedProjects = skillDefRepo.getAllImportedCopiesByOriginalSkillRefIds(skillRefIds)
            if (importedSkillsFromQuizFollowedProjects) {
                Map<String, List<SkillDefRepo.ProjectIdAndSkill>> importedByProjectId = importedSkillsFromQuizFollowedProjects.groupBy { it.projectId }
                importedByProjectId.each {
                    addToProjAndSkillIds(it.key, it.value.collect { it.skillRefId })
                }
            }
        }

        // collect catalog imported skills
        List<SkillDefRepo.ProjectIdAndSkill> imported = skillDefRepo.getAllImportedByOriginalProject(projectId)
        if (imported) {
            Map<String, List<SkillDefRepo.ProjectIdAndSkill>> importedByProjectId = imported.groupBy { it.projectId }
            importedByProjectId.each {
                addToProjAndSkillIds(it.key, it.value.collect { it.skillRefId })
            }
        }

        // notify other projects
        projAndSkillIds.each {
            taskSchedulerService.removeSkillEventsForAUser(userId, it.key, it.value)
        }
    }

    @Transactional
    @Profile
    RequestResult deleteSkillEvent(String projectId, String skillId, String userId, Long timestamp) {
        if (skillCatalogService.isSkillImportedFromCatalog(projectId, skillId)) {
            throw new SkillException("Cannot delete skill events on skills imported from the catalog", projectId, skillId)
        }

        List<UserPerformedSkill> performedSkills = performedSkillRepository.findAllByProjectIdAndSkillIdAndUserIdAndPerformedOn(projectId, skillId, userId, new Date(timestamp))
        if (!performedSkills) {
            throw new SkillException("This skill event does not exist", projectId, skillId, ErrorCode.BadParam)
        }
        // may have more than 1 event with the same exact timestamp, this happens when multiple events may fall
        // within configured time window and client send the same timestamp (example UI calendar control)
        UserPerformedSkill performedSkill = performedSkills.first()
        log.debug("Deleting skill [{}] for user [{}]", performedSkill, userId)

        RequestResult res = new RequestResult()

        List<SkillDef> performedDependencies = performedSkillRepository.findPerformedParentSkills(userId, projectId, skillId)
        if (performedDependencies) {
            res.success = false
            res.explanation = "You cannot delete a skill event when a parent skill dependency has already been performed. You must first delete " +
                    "the performed skills for the parent dependencies: ${performedDependencies.collect({ it.projectId + ":" + it.skillId })}."
            return res
        }

        SkillEventResult skillEventResult = removePerformedSkillEvent(performedSkill)
        removeAssociatedQuizAttempts(performedSkill)

        res.success = skillEventResult.skillApplied
        res.explanation = skillEventResult.explanation
        return res
    }

    @Profile
    void removeAssociatedQuizAttempts(UserPerformedSkill performedSkill) {
        List<Integer> quizRefIds = quizToSkillDefRepo.getQuizRefIdsBySkillRefId(performedSkill.skillRefId)
        if (quizRefIds) {
            quizRefIds.each {Integer quizRefId ->
                List<UserQuizAttempt> attempts = userQuizAttemptRepo.findByUserIdAndQuizDefinitionRefIdAndStatus(performedSkill.userId, quizRefId, UserQuizAttempt.QuizAttemptStatus.PASSED)
                if (attempts) {
                    attempts.each { UserQuizAttempt userQuizAttempt ->
                        userQuizAttemptRepo.delete(userQuizAttempt)
                        log.info("Removed [{}]", userQuizAttempt.toString())

                        List<QuizToSkillDefRepo.ProjectIdAndSkillId> linkedSkills = quizToSkillDefRepo.getSkillsForQuiz(quizRefId)
                        linkedSkills.each {
                            List<UserPerformedSkill> userPerformedSkills = performedSkillRepository.findAllBySkillRefIdAndUserId(it.skillRefId, userQuizAttempt.userId)
                            if (userPerformedSkills) {
                                List<UserQuizAttempt> attemptsWithInititalRecordRemoved = userPerformedSkills.findAll { it.id != performedSkill.id}
                                // since this is a quiz-based skill should never be more than 1
                                attemptsWithInititalRecordRemoved.each {
                                    removePerformedSkillEvent(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Profile
    SkillEventResult removePerformedSkillEvent(UserPerformedSkill performedSkill) {
        log.info("Removing UserPerformedSkill {}", performedSkill)
        String projectId = performedSkill.projectId;
        String skillId = performedSkill.skillId
        String userId = performedSkill.userId

        SkillDefMin skillDefinitionMin = getSkillDef(projectId, skillId)
        Long numExistingSkills = performedSkillRepository.countByUserIdAndProjectIdAndSkillId(userId, projectId, skillId) ?: 0
        // account for null

        // handle catalog
        List<SkillDefWithExtra> related = skillDefRepo.findSkillDefMinCopiedFrom(performedSkill.skillRefId)
        if (related) {
            log.info("Propagating event deletion to the catalog skills - [{}] copies imported", related?.size())
            related?.each {
                updateUserPointsAndAchievementsWhenPerformedSkillRemoved(userId, it, numExistingSkills)
            }
            log.info("Completed propagating event deletion to the catalog skills - [{}] copies imported", related?.size())
        }

        List<Integer> badgesSkillIsUsedIn = globalBadgesService.globalBadgesSkillIsUsedIn(projectId, skillId)
        if (badgesSkillIsUsedIn) {
            // do a delete
            badgesSkillIsUsedIn.forEach{ it ->
                achievedLevelRepo.deleteAllBySkillRefIdAndUserId(it, userId)
            }
        }

        SkillEventResult skillEventResult = updateUserPointsAndAchievementsWhenPerformedSkillRemoved(userId, skillDefinitionMin, numExistingSkills)
        performedSkillRepository.delete(performedSkill)
        userEventService.removeEvent(performedSkill.performedOn, performedSkill.userId, performedSkill.skillRefId)

        return skillEventResult
    }

//    @Profile
//    void batchRemovePerformedSkillsForUserAndSpecificSkills(String userId, String projectId, List<Integer> skillRefIds) {
//        performedSkillRepository.deleteAllByUserIdAndSkillRefIdIn(userId, skillRefIds)
//        userEventService.removeAllEvents(userId, skillRefIds)
//        userPointsRepo.deleteAllByUserIdAndSkillRefIdIn(userId, skillRefIds)
//
//        removeGroupAchievementsForSkillsForASpecificUser(skillRefIds, userId)
//        updatePointsAndRemoveAchievementsForSkillsAndSpecificUser(skillRefIds, userId, projectId)
//        removeBadgeAchievementsForSkillsAndSpecificUser(skillRefIds, userId)
//        removeGlobalBadgeAchievementsForSkillsAndSpecificUser(skillRefIds, userId)
//
//        userPointsRepo.updateUserPointsForProjectAndUser(projectId, userId)
//        userPointsRepo.removeOrphanedProjectPointsForUser(projectId, userId)
////        userAchievementsAndPointsManagement.removeProjectLevelAchievementsIfUserDoesNotQualify(projectId, userId)
//    }

//    @Profile
//    private void removeGlobalBadgeAchievementsForSkillsAndSpecificUser(List<Integer> skillRefIds, String userId) {
//        List<Integer> badgesSkillIsUsedIn = skillRelDefRepo.getGlobalBadgeIdsForSkills(skillRefIds)
//        if (badgesSkillIsUsedIn) {
//            // do a delete
//            badgesSkillIsUsedIn.forEach { it ->
//                achievedLevelRepo.deleteAllBySkillRefIdAndUserId(it, userId)
//            }
//        }
//    }
//
//    @Profile
//    private void removeBadgeAchievementsForSkillsAndSpecificUser(List<Integer> skillRefIds, String userId) {
//        List<SkillDef> badges = skillRelDefRepo.findParentByChildIdInAndTypes(skillRefIds, [SkillRelDef.RelationshipType.BadgeRequirement])
//        badges.unique { it.id }.each { SkillDef badge ->
//            if (BadgeUtils.withinActiveTimeframe(badge)) {
//                achievedLevelRepo.deleteByProjectIdAndSkillIdAndUserIdAndLevel(badge.projectId, badge.skillId, userId, null)
//            }
//        }
//    }
//
//    @Profile
//    private void updatePointsAndRemoveAchievementsForSkillsAndSpecificUser(List<Integer> skillRefIds, String userId, String projectId) {
//        List<SkillDef> subjects = skillRefIds.collect {
//            ruleSetDefGraphService.getMySubjectParent(it)
//        }.unique { it.id }
//        subjects.each { SkillDef subject ->
//            userPointsRepo.updateSubjectUserPointsForUser(userId, projectId, subject.skillId, true)
//            userPointsRepo.removeSubjectUserPointsForNonExistentSkillDef(projectId, subject.skillId)
////            userAchievementsAndPointsManagement.removeSubjectLevelAchievementsIfThisUserDoesNotQualify(userId, subject)
//        }
//    }
//
//    @Profile
//    private void removeGroupAchievementsForSkillsForASpecificUser(List<Integer> skillRefIds, String userId) {
//        List<SkillDef> groups = skillRefIds.collect {
//            ruleSetDefGraphService.getMyGroupParent(it)
//        }.findAll { it != null }.unique { it.id }
//        if (groups) {
//            groups.each { SkillDef group ->
//                long userAchievementNumRemoved = achievedLevelRepo.deleteAllBySkillRefIdAndUserId(group.id, userId)
//                log.info("Removed [{}] UserAchievement records for user=[{}], group.id=[{}({})]", userAchievementNumRemoved, userId, group.skillId, group.id)
//            }
//        }
//    }

    private SkillEventResult updateUserPointsAndAchievementsWhenPerformedSkillRemoved(String userId, SkillDefMin skillDefinitionMin, Long numExistingPerformedSkills) {
        log.info("Updating points and achievements after skill was removed for userId=[{}], projectId=[{}], skillId=[{}], numExistingPerformedSkills=[{}]",
                userId, skillDefinitionMin.projectId, skillDefinitionMin.skillId, numExistingPerformedSkills)
        updateUserPoints(userId, skillDefinitionMin, skillDefinitionMin.skillId)
        boolean requestedSkillCompleted = hasReachedMaxPoints(numExistingPerformedSkills, skillDefinitionMin)
        if (requestedSkillCompleted) {
            checkForBadgesAchieved(userId, skillDefinitionMin)
            //this removes the skill achievements
            achievedLevelRepo.deleteByProjectIdAndSkillIdAndUserIdAndLevel(skillDefinitionMin.projectId, skillDefinitionMin.skillId, userId, null)
        }
        removeGroupAchievementIfNeeded(skillDefinitionMin)
        SkillEventResult skillEventResult = new SkillEventResult(projectId: skillDefinitionMin.projectId, skillId: skillDefinitionMin.skillId, name: skillDefinitionMin.name)
        checkParentGraph(skillEventResult, userId, skillDefinitionMin)
        deleteProjectLevelIfNecessary(skillDefinitionMin.projectId, userId, numExistingPerformedSkills.toInteger())
        return skillEventResult
    }

    @Profile
    private void removeGroupAchievementIfNeeded(SkillDefMin skillDefinitionMin) {
        log.info("removeGroupAchievementIfNeeded: ${skillDefinitionMin.projectId} - ${skillDefinitionMin.skillId} - ${skillDefinitionMin.groupId}")
        if (skillDefinitionMin.groupId) {
            SkillDef group = ruleSetDefGraphService.getMyGroupParent(skillDefinitionMin.id)
            if (group) {
                long userAchievementNumRemoved = achievedLevelRepo.deleteAllBySkillRefId(group.id)
                log.info("Removed [{}] UserAchievement records for group.id=[{}({})]", userAchievementNumRemoved, group.skillId, group.id)
            }
        }
    }

    @Profile
    private void performBulkReportSkillNotifications(Map<String, SkillEventResult> results) {
        results.each { String userId, SkillEventResult result ->
            if (result.skillApplied) {
                skillEventPublisher.publishSkillUpdate(result, userId)
            }
            metricsLogger.logSkillReported(userId, result)
        }
    }


    @Profile
    private void deleteProjectLevelIfNecessary(String projectId, String userId, int numberOfExistingEvents) {
        List<UserAchievement> projAchievements = achievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, null)
        Integer userProjectPoints = userPointsRepo.getPointsByProjectIdAndUserId(projectId, userId)
        if (userProjectPoints == null && (numberOfExistingEvents - 1) <= 0 ){
            log.info("There are no skill events for user [{}] proj [{}]. Will remove all of them", userId, projectId)
            deleteAchievements(projAchievements)
        } else {
            if (projAchievements && userProjectPoints != null) {
                ProjDef projDef = projDefAccessor.getProjDef(projectId)
                LevelDefinitionStorageService.LevelInfo userCurrentLevelShouldBe = levelDefService.getOverallLevelInfo(projDef, userProjectPoints)
                List<UserAchievement> toDelete = projAchievements.findAll { it.level > userCurrentLevelShouldBe.level }
                deleteAchievements(toDelete)
            }
        }
    }

    private void deleteAchievements(List<UserAchievement> toDelete) {
        for (UserAchievement achievement in toDelete) {
            log.debug("deleting achievement ${achievement}, User no longer has enough points")
            achievedLevelRepo.delete(achievement)
        }
    }

    private void checkForBadgesAchieved(String userId, SkillDefMin currentSkillDef) {
        List<SkillRelDef> parentsRels = skillRelDefRepo.findAllByChildIdAndType(currentSkillDef.id, SkillRelDef.RelationshipType.BadgeRequirement)
        parentsRels.each {
            if (it.parent.type == SkillDef.ContainerType.Badge && BadgeUtils.withinActiveTimeframe(it.parent)) {
                SkillDef badge = it.parent
                List<SkillDef> nonAchievedChildren = achievedLevelRepo.findNonAchievedChildren(userId, badge.projectId, badge.skillId, SkillRelDef.RelationshipType.BadgeRequirement)
                if (!nonAchievedChildren) {
                    achievedLevelRepo.deleteByProjectIdAndSkillIdAndUserIdAndLevel(badge.projectId, badge.skillId, userId, null)
                }
            }
        }
    }

    private boolean hasReachedMaxPoints(long numSkills, SkillDefMin skillDefinition) {
        return numSkills * skillDefinition.pointIncrement >= skillDefinition.totalPoints
    }

    private UserPoints updateUserPoints(String userId, SkillDefMin requestedSkill, String skillId = null) {
        return doUpdateUserPoints(requestedSkill, userId, skillId)
    }

    private UserPoints doUpdateUserPoints(SkillDefMin requestedSkill, String userId, String skillId) {
        UserPoints userPoints = userPointsRepo.findByProjectIdAndUserIdAndSkillId(requestedSkill.projectId, userId, skillId)
        if (!userPoints) {
            throw new SkillException("Failed to find UserPoints for projectId=[${requestedSkill.projectId}], skillId=[${skillId}], userId=[${userId}]", requestedSkill.projectId, skillId, ErrorCode.InternalError)
        }
        userPoints.points -= requestedSkill.pointIncrement

        if (userPoints.points <= 0) {
            userPointsRepo.delete(userPoints)
        } else {
            userPointsRepo.save(userPoints)
        }

        return userPoints
    }

    private void checkParentGraph(SkillEventResult res, String userId, SkillDefMin skillDef) {
        updateByTraversingUpSkillDefs(res, skillDef, skillDef, userId)

        // updated project level
        updateUserPoints(userId, skillDef, null)
    }

    private void updateByTraversingUpSkillDefs(SkillEventResult res,
                                               SkillDefMin currentDef,
                                               SkillDefMin requesterDef,
                                               String userId) {
        if (currentDef.type == SkillDef.ContainerType.Subject) {
            UserPoints updatedPoints = updateUserPoints(userId, requesterDef, currentDef.skillId)

            List<LevelDef> levelDefs = skillEventsSupportRepo.findLevelsBySkillId(currentDef.id)
            int currentScore = updatedPoints.points
            LevelDefinitionStorageService.LevelInfo levelInfo = levelDefService.getLevelInfo(currentDef.projectId, levelDefs, currentDef.totalPoints, currentScore)
            calculateLevels(levelInfo, updatedPoints, userId)
        }

        List<SkillDefMin> parentsRels = skillEventsSupportRepo
                .findParentSkillsByChildIdAndType(currentDef.id, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
        parentsRels?.each {
            updateByTraversingUpSkillDefs(res, it, requesterDef, userId)
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

    private SkillDefMin getSkillDef(String projectId, String skillId) {
        SkillDefMin skillDefinition = skillEventsSupportRepo.findByProjectIdAndSkillIdAndType(projectId, skillId, SkillDef.ContainerType.Skill)
        if (!skillDefinition) {
            throw new SkillException("Skill definition does not exist. Must create the skill definition first!", projectId, skillId)
        }
        return skillDefinition
    }

    static class BulkUserLookupResult {
        List<String> requestedUserIds
        List<String> userIdsErrored = []
    }
}
