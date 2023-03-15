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
package skills.services.quiz

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillQuizException
import skills.controller.request.model.SkillImportRequest
import skills.controller.request.model.SkillProjectCopyRequest
import skills.controller.request.model.SkillRequest
import skills.services.RuleSetDefGraphService
import skills.services.admin.BatchOperationsTransactionalAccessor
import skills.storage.model.QuizDef
import skills.storage.model.QuizToSkillDef
import skills.storage.model.SkillDef
import skills.storage.model.UserQuizAttempt
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizToSkillDefRepo
import skills.storage.repos.UserQuizAttemptRepo

@Service
@Slf4j
class QuizToSkillService {

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    UserQuizAttemptRepo quizAttemptRepo

    @Autowired
    BatchOperationsTransactionalAccessor batchOperationsTransactionalAccessor

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Transactional
    void handleQuizToSkillRelationship(SkillDef savedSkill, SkillRequest skillRequest){
        QuizDef quizDef = getQuizDef(skillRequest.quizId)

        boolean isSkillCatalogImport = skillRequest instanceof SkillImportRequest
        if (!isSkillCatalogImport) {
            saveQuizToSkillAssignment(savedSkill, quizDef)
            boolean isProjectCopyRequest = skillRequest instanceof SkillProjectCopyRequest
            if (!isProjectCopyRequest && doesQuizHasAtLeastOneRun(quizDef)) {
                updateAffectedUserPointsAndAchievements(quizDef, savedSkill)
            }
        } else if (doesQuizHasAtLeastOneRun(quizDef)) {
            List<Integer> fromSkillRefIds = [savedSkill.copiedFrom]
            batchOperationsTransactionalAccessor.copySkillUserPointsToTheImportedProjects(savedSkill.projectId, fromSkillRefIds)
            batchOperationsTransactionalAccessor.copySkillAchievementsToTheImportedProjects(savedSkill.projectId, fromSkillRefIds)
            updateGroupSubjectAndProjectPointsAndAchievements(savedSkill)
        }
    }

    private boolean saveQuizToSkillAssignment(SkillDef savedSkill, QuizDef quizDef) {
        Integer skillRef = savedSkill.id

        QuizToSkillDefRepo.QuizNameAndId quizNameAndId = quizToSkillDefRepo.getQuizIdBySkillIdRef(skillRef)
        boolean quizIdUpdated = quizNameAndId && quizNameAndId.getQuizId() != quizDef.quizId
        if (quizIdUpdated) {
            quizToSkillDefRepo.deleteBySkillRefId(skillRef)
            quizToSkillDefRepo.flush()
        }

        if (!quizNameAndId || quizIdUpdated) {
            quizToSkillDefRepo.save(new QuizToSkillDef(quizRefId: quizDef.id, skillRefId: skillRef))
            return true;
        }

        return false
    }

    @Profile
    private void updateAffectedUserPointsAndAchievements(QuizDef quizDef, SkillDef savedSkill) {
        Integer skillRef = savedSkill.id

        batchOperationsTransactionalAccessor.createUserPerformedEntriesFromPassedQuizzes(quizDef.id, skillRef)
        batchOperationsTransactionalAccessor.createSkillUserPointsFromPassedQuizzes(quizDef.id, skillRef)
        batchOperationsTransactionalAccessor.createUserAchievementsFromPassedQuizzes(quizDef.id, skillRef)

        updateGroupSubjectAndProjectPointsAndAchievements(savedSkill)
    }

    private boolean doesQuizHasAtLeastOneRun(QuizDef quizDef) {
        List<UserQuizAttempt> firstPassedRun = quizAttemptRepo.findByQuizRefIdByStatus(quizDef.id, UserQuizAttempt.QuizAttemptStatus.PASSED, PageRequest.of(0, 1))
        return firstPassedRun
    }

    private void updateGroupSubjectAndProjectPointsAndAchievements(SkillDef savedSkill) {
        Integer skillRef = savedSkill.id

        if (savedSkill.groupId) {
            SkillDef group = ruleSetDefGraphService.getMyGroupParent(skillRef)
            if (group) {
                batchOperationsTransactionalAccessor.identifyAndAddGroupAchievements([group])
            }
        }

        SkillDef subject = ruleSetDefGraphService.getMySubjectParent(skillRef)
        batchOperationsTransactionalAccessor.handlePointsAndAchievementsForSubject(subject)
        batchOperationsTransactionalAccessor.handlePointsAndAchievementsForProject(savedSkill.projectId)
    }

    @Transactional
    void removeQuizToSkillAssignment(Integer skillRef) {
        quizToSkillDefRepo.deleteBySkillRefId(skillRef)
        quizToSkillDefRepo.flush()
    }

    QuizToSkillDefRepo.QuizNameAndId getQuizIdForSkillRefId(Integer skillRefId) {
        return quizToSkillDefRepo.getQuizIdBySkillIdRef(skillRefId)
    }

    private QuizDef getQuizDef(String quizId) {
        QuizDef quizDef = quizDefRepo.findByQuizIdIgnoreCase(quizId)
        if (!quizDef) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }
        return quizDef
    }
}
