package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillsValidator
import skills.storage.model.SkillDef
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo
import skills.storage.repos.UserPointsRepo
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

    @Transactional
    void handleSkillRemoval(SkillDef skillDef) {
        SkillDef subject = ruleSetDefGraphService.getParentSkill(skillDef)
        nativeQueriesRepo.decrementPointsForDeletedSkill(skillDef.projectId, skillDef.skillId, subject.skillId)
        userPointsRepo.deleteByProjectIdAndSkillId(skillDef.projectId, skillDef.skillId)

        userPerformedSkillRepo.deleteByProjectIdAndSkillId(skillDef.projectId, skillDef.skillId)
        userAchievedLevelRepo.deleteByProjectIdAndSkillId(skillDef.projectId, skillDef.skillId)
    }

    @Transactional
    void handleSubjectRemoval(SkillDef subject) {
        nativeQueriesRepo.updateOverallScoresBySummingUpAllChildSubjects(subject.projectId, SkillDef.ContainerType.Subject)
    }

    @Transactional
    void handlePointHistoryUpdate(String projectId, String subjectId, String skillId, int incrementDelta){
        SkillsValidator.isTrue(
                skillDefRepo.existsByProjectIdAndSkillIdAndTypeAllIgnoreCase(projectId, skillId, SkillDef.ContainerType.Skill),
                "Skill does not exist",
                projectId,
                skillId
        )
        SkillsValidator.isTrue(
            skillDefRepo.existsByProjectIdAndSkillIdAndTypeAllIgnoreCase(projectId, subjectId, SkillDef.ContainerType.Subject),
                "Subject does not exist",
                projectId,
                subjectId,
        )

        nativeQueriesRepo.updatePointHistoryForSkill(projectId, subjectId, skillId, incrementDelta)
    }

    @Transactional
    void handlePointTotalsUpdate(String projectId, String subjectId, String skillId, int incrementDelta){
        SkillsValidator.isTrue(
                skillDefRepo.existsByProjectIdAndSkillIdAndTypeAllIgnoreCase(projectId, skillId, SkillDef.ContainerType.Skill),
                "Skill does not exist",
                projectId,
                skillId
        )
        SkillsValidator.isTrue(
                skillDefRepo.existsByProjectIdAndSkillIdAndTypeAllIgnoreCase(projectId, subjectId, SkillDef.ContainerType.Subject),
                "Subject does not exist",
                projectId,
                subjectId,
        )

        nativeQueriesRepo.updatePointTotalsForSkill(projectId, subjectId, skillId, incrementDelta)
    }

    @Transactional
    void updatePointsWhenOccurrencesAreDecreased(String projectId, String subjectId, String skillId, int pointIncrement, int numOccurrences){
        nativeQueriesRepo.updatePointTotalWhenOccurrencesAreDecreased(projectId, subjectId, skillId, pointIncrement, numOccurrences)
        nativeQueriesRepo.updatePointHistoryWhenOccurrencesAreDecreased(projectId, subjectId, skillId, pointIncrement, numOccurrences)
    }

    @Transactional
    void removeExtraEntriesOfUserPerformedSkillByUser(String projectId, String skillId, int numEventsToKeep){
        assert numEventsToKeep > 0
        nativeQueriesRepo.removeExtraEntriesOfUserPerformedSkillByUser(projectId, skillId, numEventsToKeep)
    }

    @Transactional
    void insertUserAchievementWhenDecreaseOfOccurrencesCausesUsersToAchieve(String projectId, String skillId, Integer skillRefId, int numOfOccurrences) {
        assert numOfOccurrences > 0
        userAchievedLevelRepo.insertUserAchievementWhenDecreaseOfOccurrencesCausesUsersToAchieve(projectId, skillId, skillRefId, numOfOccurrences)
    }

    @Transactional
    void removeUserAchievementsThatDoNotMeetNewNumberOfOccurrences(String projectId, String skillId, int numOfOccurrences) {
        assert numOfOccurrences > 0
        nativeQueriesRepo.removeUserAchievementsThatDoNotMeetNewNumberOfOccurrences(projectId, skillId, numOfOccurrences)
    }

}
