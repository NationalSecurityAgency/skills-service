package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.services.RuleSetDefGraphService
import skills.storage.model.SkillDef
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
        assert subjectId, "subjectId is required"
        nativeQueriesRepo.updatePointHistoryForSkill(projectId, subjectId, skillId, incrementDelta)
    }

    @Transactional
    void handlePointTotalsUpdate(String projectId, String subjectId, String skillId, int incrementDelta){
        assert subjectId, "subjectId is required"
        nativeQueriesRepo.updatePointTotalsForSkill(projectId, subjectId, skillId, incrementDelta)
    }



}
