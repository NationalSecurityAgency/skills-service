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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillQuizException
import skills.services.RuleSetDefGraphService
import skills.services.admin.BatchOperationsTransactionalAccessor
import skills.storage.model.QuizDef
import skills.storage.model.QuizToSkillDef
import skills.storage.model.SkillDef
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizToSkillDefRepo

@Service
@Slf4j
class QuizToSkillService {

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    BatchOperationsTransactionalAccessor batchOperationsTransactionalAccessor

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Transactional
    void saveQuizToSkillAssignment(SkillDef savedSkill, String quizId) {
        assert quizId
        Integer skillRef = savedSkill.id
        QuizDef quizDef = getQuizDef(quizId)

        QuizToSkillDefRepo.QuizNameAndId quizNameAndId = quizToSkillDefRepo.getQuizIdBySkillIdRef(skillRef)
        boolean quizIdUpdated = quizNameAndId && quizNameAndId.getQuizId() != quizDef.quizId
        if (quizIdUpdated) {
            quizToSkillDefRepo.deleteBySkillRefId(skillRef)
            quizToSkillDefRepo.flush()
        }

        if (!quizNameAndId || quizIdUpdated) {
            quizToSkillDefRepo.save(new QuizToSkillDef(quizRefId: quizDef.id, skillRefId: skillRef))
            updateAffectedUserPointsAndAchievements(quizDef, savedSkill)
        }
    }

    @Profile
    private void updateAffectedUserPointsAndAchievements(QuizDef quizDef, SkillDef savedSkill) {
        Integer skillRef = savedSkill.id

        batchOperationsTransactionalAccessor.createUserPerformedEntriesFromPassedQuizzes(quizDef.id, skillRef)
        batchOperationsTransactionalAccessor.createSkillUserPointsFromPassedQuizzes(quizDef.id, skillRef)
        batchOperationsTransactionalAccessor.createUserAchievementsFromPassedQuizzes(quizDef.id, skillRef)

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
