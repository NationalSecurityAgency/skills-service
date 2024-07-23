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
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillsValidator
import skills.controller.result.model.LevelDefinitionRes
import skills.services.settings.SettingsService
import skills.storage.model.SkillDef
import skills.storage.repos.*
import skills.storage.repos.nativeSql.PostgresQlNativeRepo

@Service
@Slf4j
class UserAchievementsAndPointsManagement {

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    UserAchievedLevelRepo userAchievedLevelRepo

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    PostgresQlNativeRepo PostgresQlNativeRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    SettingsService settingsService

    @Autowired
    LevelDefinitionStorageService levelDefinitionStorageService

    @Transactional
    void handleSkillRemoval(SkillDef skillDef, SkillDef subject) {
        assert subject.type == SkillDef.ContainerType.Subject
        PostgresQlNativeRepo.decrementPointsForDeletedSkill(skillDef.projectId, skillDef.skillId, subject.skillId)
        userPointsRepo.deleteByProjectIdAndSkillId(skillDef.projectId, skillDef.skillId)
        //decrement points in the step above can result in zero point entries if a user has only achieved the skill being deleted
        //cleanup those entries so as not to pollute user metrics
        userPointsRepo.deleteZeroPointEntries(skillDef.projectId)

        userPerformedSkillRepo.deleteByProjectIdAndSkillId(skillDef.projectId, skillDef.skillId)
        userAchievedLevelRepo.deleteByProjectIdAndSkillId(skillDef.projectId, skillDef.skillId)
        //don't leave any achievements if a user no longer has any entries in user_points
        userAchievedLevelRepo.deleteAchievementsWithNoPoints(skillDef.projectId)
    }

    @Transactional
    void handleSubjectRemoval(SkillDef subject) {
        PostgresQlNativeRepo.updateOverallScoresBySummingUpAllChildSubjects(subject.projectId, SkillDef.ContainerType.Subject)
        userPointsRepo.removeOrphanedProjectPoints(subject.projectId)
        userAchievedLevelRepo.deleteAchievementsWithNoPoints(subject.projectId)
    }

    @Profile
    void adjustUserPointsAfterModification(SkillDef skill) {
        log.info("Updating all UserPoints for [{}]-[{}]", skill.projectId, skill.skillId)
        PostgresQlNativeRepo.updateUserPointsForASkill(skill.projectId, skill.skillId)

        SkillDef subject = ruleSetDefGraphService.getMySubjectParent(skill.id)
        log.info("Updating subject's UserPoints for [{}]-[{}]", subject.projectId, subject.skillId)
        PostgresQlNativeRepo.updateUserPointsForSubject(subject.projectId, subject.skillId, true)

        log.info("Updating project's UserPoints for [{}]", skill.projectId)
        PostgresQlNativeRepo.updateUserPointsForProject(skill.projectId)
    }

    @Transactional
    void handlePointIncrementUpdate(String projectId, String subjectId, String skillId, int incrementDelta){
        SkillsValidator.isTrue(
                skillDefRepo.existsByProjectIdAndSkillIdAndTypeInAllIgnoreCase(projectId, skillId, [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup]),
                "Skill does not exist",
                projectId,
                skillId
        )
        SkillsValidator.isTrue(
            skillDefRepo.existsByProjectIdAndSkillIdAndTypeInAllIgnoreCase(projectId, subjectId, [SkillDef.ContainerType.Subject]),
                "Subject does not exist",
                projectId,
                subjectId,
        )

        if (log.isDebugEnabled()){
            log.debug("Updating existing UserPoints. projectId=[${projectId}], subjectId=[${subjectId}], skillId=[${skillId}], incrementDelta=[${incrementDelta}], ")
        }
        PostgresQlNativeRepo.updatePointTotalsForSkill(projectId, subjectId, skillId, incrementDelta)
    }

    @Transactional
    void updatePointsWhenOccurrencesAreDecreased(String projectId, String subjectId, String skillId, int pointIncrement, int newOccurrences, int previousOccurrences){
        if (log.isDebugEnabled()){
            log.debug("Update points as occurrences were decreased. projectId=[${projectId}], subjectId=[${subjectId}], skillId=[${skillId}], pointIncrement=[${pointIncrement}], newOccurrences=[$numOccurrences], previousOccurrences=[${previousOccurrences}]")
        }
        PostgresQlNativeRepo.updatePointTotalWhenOccurrencesAreDecreased(projectId, subjectId, skillId, pointIncrement, newOccurrences, previousOccurrences)
    }

    @Profile
    @Transactional
    void removeExtraEntriesOfUserPerformedSkillByUser(String projectId, String skillId, int numEventsToKeep){
        assert numEventsToKeep > 0
        if (log.isDebugEnabled()){
            log.debug("Remove extra entries from UserPerformedSkill. projectId=[${projectId}], numEventsToKeep=[${numEventsToKeep}], skillId=[${skillId}]")
        }
        PostgresQlNativeRepo.removeExtraEntriesOfUserPerformedSkillByUser(projectId, skillId, numEventsToKeep)
    }

    @Profile
    @Transactional
    void insertUserAchievementWhenDecreaseOfOccurrencesCausesUsersToAchieve(String projectId, String skillId, Integer skillRefId, int numOfOccurrences) {
        assert numOfOccurrences > 0
        if (log.isDebugEnabled()){
            log.debug("Insert User Achievements. projectId=[${projectId}], skillId=[${skillId}], skillRefId=[${skillRefId}], numOfOccurrences=[$numOfOccurrences]")
        }
        userAchievedLevelRepo.insertUserAchievementWhenDecreaseOfOccurrencesCausesUsersToAchieve(projectId, skillId, skillRefId, numOfOccurrences, Boolean.FALSE.toString())
    }

    @Profile
    @Transactional
    void identifyAndAddLevelAchievements(SkillDef subject) {
        assert subject.type == SkillDef.ContainerType.Subject
        this.identifyAndAddProjectLevelAchievements(subject.projectId)
        this.identifyAndAddSubjectLevelAchievements(subject)
    }

    @Transactional
    @Profile
    void identifyAndAddProjectLevelAchievements(String projectId) {
        List<LevelDefinitionRes> levels = levelDefinitionStorageService.getLevels(projectId)
        boolean skillsDefined = levels[0].pointsFrom != null
        if (skillsDefined) {
            levels.each {
                int numUpdated = userAchievedLevelRepo.identifyAndAddProjectLevelAchievementsForALevel(projectId, it.level, it.pointsFrom)
                log.info("Calculate project's level achievements for projectId=[{}], level=[{}], pointsFromExclusive=[{}]. Num rows updated = [{}]",
                        projectId, it.level, it.pointsFrom, numUpdated)
            }
        } else {
            log.info("Project achievement calculations will not be performed aa there are no skills defined for projectId=[{}]", projectId)
        }
    }

    @Transactional
    @Profile
    void identifyAndAddProjectLevelAchievementsForSingleUser(String userId, String projectId) {
        List<LevelDefinitionRes> levels = levelDefinitionStorageService.getLevels(projectId)
        boolean skillsDefined = levels[0].pointsFrom != null
        if (skillsDefined) {
            levels.each {
                int numUpdated = userAchievedLevelRepo.identifyAndAddProjectLevelAchievementsForALevelAndSingleUser(userId, projectId, it.level, it.pointsFrom)
                log.info("Calculate project's level achievements for userId=[{}], projectId=[{}], level=[{}], pointsFromExclusive=[{}]. Num rows updated = [{}]",
                        userId, projectId, it.level, it.pointsFrom, numUpdated)
            }
        } else {
            log.info("Project achievement calculations will not be performed aa there are no skills defined for projectId=[{}]", projectId)
        }
    }

    @Transactional
    @Profile
    void identifyAndAddSubjectLevelAchievements(SkillDef subject) {
        List<LevelDefinitionRes> levels = levelDefinitionStorageService.getLevels(subject.projectId, subject.skillId)
        boolean skillsDefined = levels[0].pointsFrom != null
        if (skillsDefined) {
            levels.each {
                int numUpdated = userAchievedLevelRepo.identifyAndAddSubjectLevelAchievementsForALevel(subject.projectId, subject.skillId, subject.id, it.level, it.pointsFrom)
                log.info("Calculate subject's level achievements for projectId=[{}], subjectId=[{}({})], level=[{}], pointsFromExclusive=[{}]. Num rows updated = [{}]",
                        subject.projectId, subject.skillId, subject.id, it.level, it.pointsFrom, numUpdated)
            }
        } else {
            log.info("Subject achievement calculations will not be performed as there are no skills defined for projectId=[{}], subjectId=[{}]", subject.projectId, subject.skillId,)
        }
    }

    @Transactional
    @Profile
    void identifyAndAddSubjectLevelAchievementsForSingleUser(String userId, SkillDef subject) {
        List<LevelDefinitionRes> levels = levelDefinitionStorageService.getLevels(subject.projectId, subject.skillId)
        boolean skillsDefined = levels[0].pointsFrom != null
        if (skillsDefined) {
            levels.each {
                int numUpdated = userAchievedLevelRepo.identifyAndAddSubjectLevelAchievementsForALevelForASingleUser(userId, subject.projectId, subject.skillId, subject.id, it.level, it.pointsFrom)
                log.info("Calculate subject's level achievements for userId=[{}], projectId=[{}], subjectId=[{}({})], level=[{}], pointsFromExclusive=[{}]. Num rows updated = [{}]",
                        userId, subject.projectId, subject.skillId, subject.id, it.level, it.pointsFrom, numUpdated)
            }
        } else {
            log.info("Subject achievement calculations will not be performed as there are no skills defined for projectId=[{}], subjectId=[{}]", subject.projectId, subject.skillId,)
        }
    }

    @Transactional
    @Profile
    void removeSubjectLevelAchievementsIfUsersDoNotQualify(SkillDef subject) {
        List<LevelDefinitionRes> levels = levelDefinitionStorageService.getLevels(subject.projectId, subject.skillId)
        boolean skillsDefined = levels[0].pointsFrom != null
        if (skillsDefined) {
            levels.each {
                int numUpdated = userAchievedLevelRepo.removeSubjectLevelAchievementsIfUsersDoNotQualify(subject.id, it.level, it.pointsFrom)
                log.info("Remove subject's level achievements for projectId=[{}], subjectId=[{}({})], level=[{}], pointsFromExclusive=[{}]. Num rows updated = [{}]",
                        subject.projectId, subject.skillId, subject.id, it.level, it.pointsFrom, numUpdated)
            }
        } else {
            int numDeleted = userAchievedLevelRepo.deleteAllBySkillRefId(subject.id)
            log.info("There are no skills defined for projectId=[{}], subjectId=[{}({})]. Removed [{}] subject achievements",
                    subject.projectId, subject.skillId, subject.id, numDeleted)
        }
    }

    @Profile
    void removeSubjectLevelAchievementsIfThisUserDoesNotQualify(String userId, SkillDef subject) {
        List<LevelDefinitionRes> levels = levelDefinitionStorageService.getLevels(subject.projectId, subject.skillId)
        boolean skillsDefined = levels[0].pointsFrom != null
        if (skillsDefined) {
            levels.each {
                int numUpdated = userAchievedLevelRepo.removeSubjectLevelAchievementsIfThisUserDoesNotQualify(userId, subject.id, it.level, it.pointsFrom)
                log.info("Remove subject's level achievements for user=[{}], projectId=[{}], subjectId=[{}({})], level=[{}], pointsFromExclusive=[{}]. Num rows updated = [{}]",
                        userId, subject.projectId, subject.skillId, subject.id, it.level, it.pointsFrom, numUpdated)
            }
        } else {
            int numDeleted = userAchievedLevelRepo.deleteAllBySkillRefIdAndUserId(subject.id, userId)
            log.info("There are no skills defined for user=[{}], projectId=[{}], subjectId=[{}({})]. Removed [{}] subject achievements",
                    userId, subject.projectId, subject.skillId, subject.id, numDeleted)
        }
    }

    @Transactional
    @Profile
    void removeProjectLevelAchievementsIfUsersDoNotQualify(String projectId) {
        List<LevelDefinitionRes> levels = levelDefinitionStorageService.getLevels(projectId)
        assert levels
        if (levels) {
            levels.each {
                if(it.pointsFrom) {
                    int numUpdated = userAchievedLevelRepo.removeProjectLevelAchievementsIfUsersDoNotQualify(projectId, it.level, it.pointsFrom)
                    log.info("Remove project's level achievements for projectId=[{}], level=[{}], pointsFromExclusive=[{}]. Num rows removed = [{}]",
                            projectId, it.level, it.pointsFrom, numUpdated)
                }
            }
        }
    }

    @Transactional
    @Profile
    void removeProjectLevelAchievementsIfUserDoesNotQualify(String userId, String projectId) {
        List<LevelDefinitionRes> levels = levelDefinitionStorageService.getLevels(projectId)
        assert levels
        if (levels) {
            levels.each {
                int numUpdated = userAchievedLevelRepo.removeProjectLevelAchievementsIfUserDoesNotQualify(userId, projectId, it.level, it.pointsFrom)
                log.info("Remove project's level achievements for user=[{}], projectId=[{}], level=[{}], pointsFromExclusive=[{}]. Num rows removed = [{}]",
                        userId, projectId, it.level, it.pointsFrom, numUpdated)
            }
        }
    }

    @Profile
    @Transactional
    void insertUserAchievementWhenDecreaseOfSkillsRequiredCausesUsersToAchieve(String projectId, String groupSkillId, Integer groupSkillRefId, List<String> childSkillIds, int numSkillsRequired) {
        assert numSkillsRequired > 0
        if (log.isDebugEnabled()) {
            log.debug("Insert User Achievements. projectId=[${projectId}], skillId=[${groupSkillId}], skillRefId=[${groupSkillRefId}], childSkillIds=[${childSkillIds}] numSkillsRequired=[$numSkillsRequired]")
        }
        userAchievedLevelRepo.insertUserAchievementWhenDecreaseOfNumSkillsRequiredCausesUsersToAchieve(projectId, groupSkillId, groupSkillRefId, childSkillIds, numSkillsRequired, Boolean.FALSE.toString())
    }

    @Profile
    @Transactional
    void removeUserAchievementsThatDoNotMeetNewNumberOfOccurrences(String projectId, String skillId, int numOfOccurrences) {
        assert numOfOccurrences > 0
        if (log.isDebugEnabled()){
            log.debug("Remove User achievements that do not meet number of occurences. projectId=[${projectId}], skillId=[${skillId}], numOfOccurrences=[$numOfOccurrences]")
        }
        PostgresQlNativeRepo.removeUserAchievementsThatDoNotMeetNewNumberOfOccurrences(projectId, skillId, numOfOccurrences)
    }

}
