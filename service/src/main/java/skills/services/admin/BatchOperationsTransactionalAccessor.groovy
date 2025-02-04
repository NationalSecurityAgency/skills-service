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
package skills.services.admin

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.controller.result.model.SettingsResult
import skills.services.BadgeUtils
import skills.services.RuleSetDefGraphService
import skills.services.RuleSetDefinitionScoreUpdater
import skills.services.UserAchievementsAndPointsManagement
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserEventsRepo
import skills.storage.repos.UserPerformedSkillRepo
import skills.storage.repos.UserPointsRepo
import skills.storage.repos.nativeSql.PostgresQlNativeRepo

@Service
@Slf4j
class BatchOperationsTransactionalAccessor {

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    UserAchievedLevelRepo userAchievedLevelRepo

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    PostgresQlNativeRepo PostgresQlNativeRepo

    @Autowired
    UserAchievementsAndPointsManagement userAchievementsAndPointsManagement

    @Autowired
    RuleSetDefinitionScoreUpdater ruleSetDefinitionScoreUpdater

    @Autowired
    SkillsGroupAdminService skillsGroupAdminService

    @Autowired
    SettingsService settingsService

    @Autowired
    UserEventsRepo userEventsRepo

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    UserCommunityService userCommunityService

    @Transactional
    @Profile
    void enableSkills(List<SkillDef> disabledImportedSkills) {
        disabledImportedSkills.each {
            it.enabled = Boolean.TRUE.toString()
        }
        skillDefRepo.saveAll(disabledImportedSkills)
    }

    /**
     * Handle subject after there has been user points change in one of its skills
     * (1) create user points for new users
     * (2) update existing
     * (3) calculate achievements
     */
    @Transactional
    void handlePointsAndAchievementsForSubject(SkillDef subject) {
        String projectId = subject.projectId
        String subjectId = subject.skillId

        createSubjectUserPointsForTheNewUsers(projectId, subjectId)

        log.info("Updating UserPoints for the existing users for [{}-{}] subject", projectId, subjectId)
        updateUserPointsForSubject(projectId, subjectId)


        identifyAndAddSubjectLevelAchievements(subject.projectId, subjectId)
    }

    /**
     * Handle project after there has been user points change in one of its subjects or skills
     * (1) create user points for new users
     * (2) update existing
     * (3) calculate achievements
     * @param projectId
     */
    @Transactional
    void handlePointsAndAchievementsForProject(String projectId) {
        SettingsResult settingsResult = settingsService.getProjectSetting(projectId, Settings.LEVEL_AS_POINTS.settingName)
        boolean pointsBased = settingsResult ? settingsResult.isEnabled() : false

        createProjectUserPointsForTheNewUsers(projectId)
        updateUserPointsForProject(projectId)
        identifyAndAddProjectLevelAchievements(projectId, pointsBased)
    }

    @Transactional
    @Profile
    void updateSubjectTotalPoints(String projectId, String subjectId) {
        ruleSetDefinitionScoreUpdater.updateSubjectTotalPoints(projectId, subjectId, false)
    }


    @Transactional
    @Profile
    void updateGroupTotalPoints(String projectId, String groupId) {
        ruleSetDefinitionScoreUpdater.updateGroupTotalPoints(projectId, groupId, false)
    }

    @Transactional
    @Profile
    void updateProjectsTotalPoints(String projectId) {
        skillDefRepo.updateProjectsTotalPoints(projectId, false)
    }

    @Transactional
    @Profile
    void copySkillUserPointsToTheImportedProjects(String toProjectId, List<Integer> fromSkillRefIds) {
        log.info("Copying [{}] skills UserPoints to the imported project [{}]", fromSkillRefIds.size(), toProjectId)
        userPointsRepo.copySkillUserPointsToTheImportedProjects(toProjectId, fromSkillRefIds, userCommunityService.userCommunityUserTagKey, userCommunityService.userCommunityUserTagValue)
        log.info("Done copying [{}] skills UserPoints to the imported project [{}]", fromSkillRefIds.size(), toProjectId)
    }

    @Transactional
    @Profile
    void copySingleUserSkillUserPointsToTheImportedProjects(String userId, String toProjectId, List<Integer> fromSkillRefIds) {
        log.info("Copying [{}] skills UserPoints to the imported project [{}] for user=[{}]", fromSkillRefIds.size(), toProjectId, userId)
        userPointsRepo.copySingleUserSkillUserPointsToTheImportedProjects(userId, toProjectId, fromSkillRefIds)
        log.info("Done copying [{}] skills UserPoints to the imported project [{}] for user=[{}]", fromSkillRefIds.size(), toProjectId, userId)
    }

    @Transactional
    @Profile
    void copySkillAchievementsToTheImportedProjects(String projectId, List<Integer> fromSkillRefIds) {
        log.info("Copying [{}] skills achievements to the imported project [{}]", fromSkillRefIds.size(), projectId)
        userAchievedLevelRepo.copySkillAchievementsToTheImportedProjects(fromSkillRefIds, userCommunityService.userCommunityUserTagKey, userCommunityService.userCommunityUserTagValue)
        log.info("Done copying [{}] skills achievements to the imported project [{}]", fromSkillRefIds.size(), projectId)
    }

    @Transactional
    @Profile
    void copyForSingleUserSkillAchievementsToTheImportedProjects(String userId, String projectId, List<Integer> fromSkillRefIds) {
        log.info("Copying [{}] skills achievements to the imported project [{}] for user=[{}]", fromSkillRefIds.size(), projectId, userId)
        userAchievedLevelRepo.copyForSingleUserSkillAchievementsToTheImportedProjects(userId, fromSkillRefIds)
        log.info("Done copying [{}] skills achievements to the imported project [{}] for user=[{}]", fromSkillRefIds.size(), projectId, userId)
    }

    @Transactional
    @Profile
    Integer createSubjectUserPointsForTheNewUsers(String toProjectId, String toSubjectId) {
        log.info("Creating UserPoints for the new users for [{}-{}] subject", toProjectId, toSubjectId)
        int numRows = userPointsRepo.createSubjectUserPointsForTheNewUsers(toProjectId, toSubjectId)
        log.info("Created [{}] UserPoints for the new users for [{}-{}] subject", numRows, toProjectId, toSubjectId)

        return numRows
    }


    @Transactional
    @Profile
    Integer createSubjectUserPointsForSingleNewUser(String userId, String toProjectId, String toSubjectId) {
        log.info("Creating UserPoints for the new users for [{}-{}] subject", toProjectId, toSubjectId)
        int numRows = userPointsRepo.createSubjectUserPointsForSingleNewUser(userId, toProjectId, toSubjectId)
        log.info("Created [{}] UserPoints for the new users for [{}-{}] subject", numRows, toProjectId, toSubjectId)

        return numRows
    }

    @Transactional
    @Profile
    void createSkillUserPointsFromPassedQuizzes(Integer quizRefId, Integer skillRefId) {
        log.info("Creating UserPoints for users that passed the quiz: quizRefId=[{}], skillId=[{}]", quizRefId, skillRefId)
        userPointsRepo.createSkillUserPointsFromPassedQuizzes(quizRefId, skillRefId, userCommunityService.userCommunityUserTagKey, userCommunityService.userCommunityUserTagValue)
        log.info("Completed creating UserPoints for users that passed the quiz: quizRefId=[{}], skillId=[{}]", quizRefId, skillRefId)
    }

    @Transactional
    @Profile
    void createSkillUserPointsFromPassedQuizzesForProject(String projectId) {
        log.info("Creating UserPoints for users that passed the quiz: projectId=[{}]", projectId)
        userPointsRepo.createSkillUserPointsFromPassedQuizzesForProject(projectId, userCommunityService.userCommunityUserTagKey, userCommunityService.userCommunityUserTagValue)
        log.info("Completed creating UserPoints for users that passed the quiz: projectId=[{}]", projectId)
    }


    @Transactional
    @Profile
    void createUserPerformedEntriesFromPassedQuizzes(Integer quizRefId, Integer skillRefId) {
        log.info("Creating UserPerformedSkills for users that passed the quiz: quizRefId=[{}], skillId=[{}]", quizRefId, skillRefId)
        userPointsRepo.createUserPerformedEntriesFromPassedQuizzes(quizRefId, skillRefId, userCommunityService.userCommunityUserTagKey, userCommunityService.userCommunityUserTagValue)
        log.info("Completed creating UserPerformedSkills for users that passed the quiz: quizRefId=[{}], skillId=[{}]", quizRefId, skillRefId)
    }


    @Transactional
    @Profile
    void createUserEventEntriesFromPassedQuizzes(Integer quizRefId, Integer skillRefId) {
        log.info("Creating UserEvents for users that passed the quiz: quizRefId=[{}], skillId=[{}]", quizRefId, skillRefId)
        userEventsRepo.createUserEventEntriesFromPassedQuizzes(quizRefId, skillRefId, userCommunityService.userCommunityUserTagKey, userCommunityService.userCommunityUserTagValue)
        log.info("Completed creating UserEvents for users that passed the quiz: quizRefId=[{}], skillId=[{}]", quizRefId, skillRefId)
    }

    @Transactional
    @Profile
    void createUserPerformedEntriesFromPassedQuizzesForProject(String projectId) {
        log.info("Creating UserPerformedSkills for users that passed the quiz: projectId=[{}]", projectId)
        userPointsRepo.createUserPerformedEntriesFromPassedQuizzesForProject(projectId, userCommunityService.userCommunityUserTagKey, userCommunityService.userCommunityUserTagValue)
        log.info("Completed creating UserPerformedSkills for users that passed the quiz: projectId=[{}]", projectId)
    }

    @Transactional
    @Profile
    void createUserAchievementsFromPassedQuizzes(Integer quizRefId, Integer skillRefId) {
        log.info("Creating UserAchievements for users that passed the quiz: quizRefId=[{}], skillRefId=[{}]", quizRefId, skillRefId)
        userPointsRepo.createUserAchievementsFromPassedQuizzes(quizRefId, skillRefId, userCommunityService.userCommunityUserTagKey, userCommunityService.userCommunityUserTagValue)
        log.info("Completed creating UserAchievements for users that passed the quiz: quizRefId=[{}], skillRefId=[{}]", quizRefId, skillRefId)
    }

    @Transactional
    @Profile
    void createUserAchievementsFromPassedQuizzes(String projectId) {
        log.info("Creating UserAchievements for users that passed the quiz: projectId=[{}]", projectId)
        userPointsRepo.createUserAchievementsFromPassedQuizzesForProject(projectId, userCommunityService.userCommunityUserTagKey, userCommunityService.userCommunityUserTagValue)
        log.info("Completed creating UserAchievements for users that passed the quiz: projectId=[{}]", projectId)
    }

    @Transactional
    @Profile
    void updateUserPointsForSubject(String projectId, String subjectId) {
        log.info("Updating UserPoints for subject: projectId=[{}], subjectId=[{}]", projectId, subjectId)
        PostgresQlNativeRepo.updateUserPointsForSubject(projectId, subjectId, false)
        log.info("Completed updating UserPoints for subject: projectId=[{}], subjectId=[{}]", projectId, subjectId)
    }

    @Transactional
    @Profile
    void removeSubjectUserPointsForNonExistentSkillDef(String projectId, String subjectId) {
        log.info("Removing UserPoints for subject that doesn't have any child points: projectId=[{}], subjectId=[{}]", projectId, subjectId)
        userPointsRepo.removeSubjectUserPointsForNonExistentSkillDef(projectId, subjectId)
        log.info("Completed removing UserPoints for subject that doesn't have any child points: projectId=[{}], subjectId=[{}]", projectId, subjectId)
    }

    @Transactional
    @Profile
    void identifyAndAddGroupAchievements(List<SkillDef> groups) {
        groups.each { SkillDef skillsGroupSkillDef ->
            int numSkillsRequired = skillsGroupAdminService.getActualNumSkillsRequred(skillsGroupSkillDef.numSkillsRequired, skillsGroupSkillDef.id)
            log.info("Identifying group achievements groupRefId=[{}], groupId=[{}.{}], numSkillsRequired=[{}]",
                    skillsGroupSkillDef.id, skillsGroupSkillDef.projectId, skillsGroupSkillDef.skillId, numSkillsRequired)
            userAchievedLevelRepo.identifyAndAddGroupAchievements(
                    skillsGroupSkillDef.projectId,
                    skillsGroupSkillDef.skillId,
                    skillsGroupSkillDef.id,
                    numSkillsRequired,
                    Boolean.FALSE.toString(),
            )
            log.info("Finished identifying group achievements groupRefId=[{}], groupId=[{}.{}], numSkillsRequired=[{}]",
                    skillsGroupSkillDef.id, skillsGroupSkillDef.projectId, skillsGroupSkillDef.skillId, numSkillsRequired)
        }
    }

    @Transactional
    @Profile
    void identifyAndAddGroupAchievementsForSingleUser(String userId, List<SkillDef> groups) {
        groups.each { SkillDef skillsGroupSkillDef ->
            int numSkillsRequired = skillsGroupAdminService.getActualNumSkillsRequred(skillsGroupSkillDef.numSkillsRequired, skillsGroupSkillDef.id)
            log.info("Identifying group achievements userId=[{}], groupRefId=[{}], groupId=[{}.{}], numSkillsRequired=[{}]",
                    userId, skillsGroupSkillDef.id, skillsGroupSkillDef.projectId, skillsGroupSkillDef.skillId, numSkillsRequired)
            userAchievedLevelRepo.identifyAndAddGroupAchievementsForSingleUser(
                    userId,
                    skillsGroupSkillDef.projectId,
                    skillsGroupSkillDef.skillId,
                    skillsGroupSkillDef.id,
                    numSkillsRequired,
                    Boolean.FALSE.toString(),
            )
            log.info("Finished identifying group achievements userId=[{}], groupRefId=[{}], groupId=[{}.{}], numSkillsRequired=[{}]",
                    userId, skillsGroupSkillDef.id, skillsGroupSkillDef.projectId, skillsGroupSkillDef.skillId, numSkillsRequired)
        }
    }

    @Transactional
    @Profile
    void identifyAndAddSubjectLevelAchievements(String projectId, String subjectId) {
        log.info("Identifying subject level achievements for [{}-{}] subject", projectId, subjectId)
        SkillDef subject = skillDefRepo.findByProjectIdAndSkillId(projectId, subjectId)
        userAchievementsAndPointsManagement.identifyAndAddSubjectLevelAchievements(subject)
        log.info("Completed import for subject. projectIdTo=[{}], subjectIdTo=[{}]", projectId, subjectId)
    }

    @Transactional
    @Profile
    void identifyAndAddSubjectLevelAchievementsForSingleUser(String userId, String projectId, String subjectId) {
        log.info("Identifying subject level achievements for [{}-{}] subject for user=[{}]", projectId, subjectId, userId, )
        SkillDef subject = skillDefRepo.findByProjectIdAndSkillId(projectId, subjectId)
        userAchievementsAndPointsManagement.identifyAndAddSubjectLevelAchievementsForSingleUser(userId, subject)
        log.info("Completed import for subject. projectIdTo=[{}], subjectIdTo=[{}] for user=[{}]", projectId, subjectId, userId)
    }

    @Transactional
    @Profile
    void createProjectUserPointsForTheNewUsers(String toProjectId) {
        log.info("Creating UserPoints for the new users for [{}] project", toProjectId)
        userPointsRepo.createProjectUserPointsForTheNewUsers(toProjectId)
        log.info("Competed creating UserPoints for the new users for [{}] project", toProjectId)
    }

    @Transactional
    @Profile
    void createProjectUserPointsForSingleNewUser(String userId, String toProjectId) {
        log.info("Creating UserPoints for the new users for [{}] project for user [{}]", toProjectId, userId)
        userPointsRepo.createProjectUserPointsForSingleNewUser(userId, toProjectId)
        log.info("Competed creating UserPoints for the new users for [{}] project for user [{}]", toProjectId, userId)
    }

    @Transactional
    @Profile
    void updateUserPointsForProject(String projectId) {
        log.info("Updating UserPoints for the existing users for [{}] project", projectId)
        PostgresQlNativeRepo.updateUserPointsForProject(projectId)
        log.info("Completed updating UserPoints for the existing users for [{}] project", projectId)
    }

    @Transactional
    @Profile
    void identifyAndAddProjectLevelAchievements(String projectId, boolean pointsBasedLevels){
        log.info("Identifying and adding project level achievements for [{}] project, pointsBased=[{}]", projectId, pointsBasedLevels)
        userAchievementsAndPointsManagement.identifyAndAddProjectLevelAchievements(projectId)
        log.info("Completed identifying and adding project level achievements for [{}] project, pointsBased=[{}]", projectId, pointsBasedLevels)
    }

    @Transactional
    @Profile
    void identifyAndAddProjectLevelAchievements(String userId, String projectId){
        log.info("Identifying and adding project level achievements for [{}] project, userId=[{}]", projectId, userId)
        userAchievementsAndPointsManagement.identifyAndAddProjectLevelAchievementsForSingleUser(userId, projectId)
        log.info("Completed identifying and adding project level achievements for [{}] project, userId=[{}]", projectId, userId)
    }

    @Transactional
    @Profile
    void batchRemovePerformedSkillsForUserAndSpecificSkills(String userId, String projectId, List<Integer> skillRefIds) {
        userPerformedSkillRepo.deleteAllByUserIdAndSkillRefIdIn(userId, skillRefIds)
        userEventsRepo.deleteAllByUserIdAndSkillRefIdIn(userId, skillRefIds)
        userPointsRepo.deleteAllByUserIdAndSkillRefIdIn(userId, skillRefIds)
        userAchievedLevelRepo.deleteAllBySkillRefIdInAndUserId(skillRefIds, userId)

        removeGroupAchievementsForSkillsForASpecificUser(skillRefIds, userId)
        updateSubjectPointsAndRemoveAchievementsForSkillsAndSpecificUser(skillRefIds, userId, projectId)
        removeBadgeAchievementsForSkillsAndSpecificUser(skillRefIds, userId)
        removeGlobalBadgeAchievementsForSkillsAndSpecificUser(skillRefIds, userId)

        userPointsRepo.updateUserPointsForProjectAndUser(projectId, userId)
        userPointsRepo.removeOrphanedProjectPointsForUser(projectId, userId)
        userAchievementsAndPointsManagement.removeProjectLevelAchievementsIfUserDoesNotQualify(userId, projectId)
    }

    @Profile
    private void removeGlobalBadgeAchievementsForSkillsAndSpecificUser(List<Integer> skillRefIds, String userId) {
        List<Integer> badgesSkillIsUsedIn = skillRelDefRepo.getGlobalBadgeIdsForSkills(skillRefIds)
        if (badgesSkillIsUsedIn) {
            // do a delete
            badgesSkillIsUsedIn.forEach { it ->
                userAchievedLevelRepo.deleteAllBySkillRefIdAndUserId(it, userId)
            }
        }
    }

    @Profile
    private void removeBadgeAchievementsForSkillsAndSpecificUser(List<Integer> skillRefIds, String userId) {
        List<SkillDef> badges = skillRelDefRepo.findParentByChildIdInAndTypes(skillRefIds, SkillDef.ContainerType.Badge, [SkillRelDef.RelationshipType.BadgeRequirement])
        badges.unique { it.id }.each { SkillDef badge ->
            if (BadgeUtils.withinActiveTimeframe(badge)) {
                userAchievedLevelRepo.deleteByProjectIdAndSkillIdAndUserIdAndLevel(badge.projectId, badge.skillId, userId, null)
            }
        }
    }

    @Profile
    private void updateSubjectPointsAndRemoveAchievementsForSkillsAndSpecificUser(List<Integer> skillRefIds, String userId, String projectId) {
        List<SkillDef> subjects = skillRefIds.collect {
            ruleSetDefGraphService.getMySubjectParent(it)
        }.unique { it.id }
        subjects.each { SkillDef subject ->
            int numUpdated = userPointsRepo.updateSubjectUserPointsForUser(userId, projectId, subject.skillId, true)
            log.info("Updated [{}] Subject UserPoints For User userId=[{}], subject=[{}-{}]", numUpdated, userId, projectId, subject.skillId)
            int numRemoved = userPointsRepo.removeSubjectUserPointsForNonExistentSkillDef(projectId, subject.skillId)
            log.info("Removed [{}] Subject UserPoints For User userId=[{}], subject=[{}-{}]", numRemoved, userId, projectId, subject.skillId)
            userAchievementsAndPointsManagement.removeSubjectLevelAchievementsIfThisUserDoesNotQualify(userId, subject)
        }
    }

    @Profile
    private void removeGroupAchievementsForSkillsForASpecificUser(List<Integer> skillRefIds, String userId) {
        List<SkillDef> groups = skillRefIds.collect {
            ruleSetDefGraphService.getMyGroupParent(it)
        }.findAll { it != null }.unique { it.id }
        if (groups) {
            groups.each { SkillDef group ->
                long userAchievementNumRemoved = userAchievedLevelRepo.deleteAllBySkillRefIdAndUserId(group.id, userId)
                log.info("Removed [{}] UserAchievement records for user=[{}], group.id=[{}({})]", userAchievementNumRemoved, userId, group.skillId, group.id)
            }
        }
    }

}
