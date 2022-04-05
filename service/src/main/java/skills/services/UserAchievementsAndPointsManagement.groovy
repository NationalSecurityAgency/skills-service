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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillsValidator
import skills.controller.result.model.SettingsResult
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.*
import skills.storage.repos.nativeSql.NativeQueriesRepo

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
    NativeQueriesRepo nativeQueriesRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    SettingsService settingsService

    @Transactional
    void handleSkillRemoval(SkillDef skillDef, SkillDef subject) {
        assert subject.type == SkillDef.ContainerType.Subject
        nativeQueriesRepo.decrementPointsForDeletedSkill(skillDef.projectId, skillDef.skillId, subject.skillId)
        userPointsRepo.deleteByProjectIdAndSkillId(skillDef.projectId, skillDef.skillId)

        userPerformedSkillRepo.deleteByProjectIdAndSkillId(skillDef.projectId, skillDef.skillId)
        userAchievedLevelRepo.deleteByProjectIdAndSkillId(skillDef.projectId, skillDef.skillId)
    }

    @Transactional
    void handleSubjectRemoval(SkillDef subject) {
        nativeQueriesRepo.updateOverallScoresBySummingUpAllChildSubjects(subject.projectId, SkillDef.ContainerType.Subject)
    }

    void adjustUserPointsAfterModification(SkillDef skill) {
        log.info("Updating all UserPoints for [{}]-[{}]", skill.projectId, skill.skillId)
        nativeQueriesRepo.updateUserPointsForASkill(skill.projectId, skill.skillId)

        List<SkillDef> parents = skillRelDefRepo.findParentByChildIdAndTypes(skill.id, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
        while (parents) {
            assert parents.size() == 1
            SkillDef parent = parents.first()
            log.info("Updating parent's UserPoints for [{}]-[{}]", parent.projectId, parent.skillId)
            nativeQueriesRepo.updateUserPointsForSubjectOrGroup(parent.projectId, parent.skillId)
            parents = skillRelDefRepo.findParentByChildIdAndTypes(parent.id, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
        }

        log.info("Updating project's UserPoints for [{}]", skill.projectId)
        nativeQueriesRepo.updateUserPointsForProject(skill.projectId)
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
        nativeQueriesRepo.updatePointTotalsForSkill(projectId, subjectId, skillId, incrementDelta)
    }

    @Transactional
    void updatePointsWhenOccurrencesAreDecreased(String projectId, String subjectId, String skillId, int pointIncrement, int newOccurrences, int previousOccurrences){
        if (log.isDebugEnabled()){
            log.debug("Update points as occurrences were decreased. projectId=[${projectId}], subjectId=[${subjectId}], skillId=[${skillId}], pointIncrement=[${pointIncrement}], newOccurrences=[$numOccurrences], previousOccurrences=[${previousOccurrences}]")
        }
        nativeQueriesRepo.updatePointTotalWhenOccurrencesAreDecreased(projectId, subjectId, skillId, pointIncrement, newOccurrences, previousOccurrences)
    }

    @Transactional
    void removeExtraEntriesOfUserPerformedSkillByUser(String projectId, String skillId, int numEventsToKeep){
        assert numEventsToKeep > 0
        if (log.isDebugEnabled()){
            log.debug("Remove extra entries from UserPerformedSkill. projectId=[${projectId}], numEventsToKeep=[${numEventsToKeep}], skillId=[${skillId}]")
        }
        nativeQueriesRepo.removeExtraEntriesOfUserPerformedSkillByUser(projectId, skillId, numEventsToKeep)
    }

    @Transactional
    void insertUserAchievementWhenDecreaseOfOccurrencesCausesUsersToAchieve(String projectId, String skillId, Integer skillRefId, int numOfOccurrences) {
        assert numOfOccurrences > 0
        if (log.isDebugEnabled()){
            log.debug("Insert User Achievements. projectId=[${projectId}], skillId=[${skillId}], skillRefId=[${skillRefId}], numOfOccurrences=[$numOfOccurrences]")
        }
        userAchievedLevelRepo.insertUserAchievementWhenDecreaseOfOccurrencesCausesUsersToAchieve(projectId, skillId, skillRefId, numOfOccurrences, Boolean.FALSE.toString())
    }

    @Transactional
    void identifyAndAddLevelAchievements(SkillDef subject) {
        assert subject.type == SkillDef.ContainerType.Subject
        SettingsResult settingsResult = settingsService.getProjectSetting(subject.projectId, Settings.LEVEL_AS_POINTS.settingName)
        boolean pointsBased = settingsResult ? settingsResult.isEnabled() : false

        nativeQueriesRepo.identifyAndAddProjectLevelAchievements(subject.projectId, pointsBased)
        nativeQueriesRepo.identifyAndAddSubjectLevelAchievements(subject.projectId, subject.skillId, pointsBased)
    }

    @Transactional
    void insertUserAchievementWhenDecreaseOfSkillsRequiredCausesUsersToAchieve(String projectId, String groupSkillId, Integer groupSkillRefId, List<String> childSkillIds, int numSkillsRequired) {
        assert numSkillsRequired > 0
        if (log.isDebugEnabled()){
            log.debug("Insert User Achievements. projectId=[${projectId}], skillId=[${groupSkillId}], skillRefId=[${groupSkillRefId}], childSkillIds=[${childSkillIds}] numSkillsRequired=[$numSkillsRequired]")
        }
        userAchievedLevelRepo.insertUserAchievementWhenDecreaseOfNumSkillsRequiredCausesUsersToAchieve(projectId, groupSkillId, groupSkillRefId, childSkillIds, numSkillsRequired, Boolean.FALSE.toString())
    }

    @Transactional
    void removeUserAchievementsThatDoNotMeetNewNumberOfOccurrences(String projectId, String skillId, int numOfOccurrences) {
        assert numOfOccurrences > 0
        if (log.isDebugEnabled()){
            log.debug("Remove User achievements that do not meet number of occurences. projectId=[${projectId}], skillId=[${skillId}], numOfOccurrences=[$numOfOccurrences]")
        }
        nativeQueriesRepo.removeUserAchievementsThatDoNotMeetNewNumberOfOccurrences(projectId, skillId, numOfOccurrences)
    }

}
