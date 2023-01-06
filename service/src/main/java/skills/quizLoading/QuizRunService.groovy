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
package skills.quizLoading

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.apache.commons.collections4.ListUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillQuizException
import skills.quizLoading.model.QuizAnswerOptionsInfo
import skills.quizLoading.model.QuizAttemptReq
import skills.quizLoading.model.QuizAttemptStartResult
import skills.quizLoading.model.QuizGradedResult
import skills.quizLoading.model.QuizInfo
import skills.quizLoading.model.QuizQuestionAttemptReq
import skills.quizLoading.model.QuizQuestionGradedResult
import skills.quizLoading.model.QuizQuestionInfo
import skills.storage.model.QuizAnswerDef
import skills.storage.model.UserQuizAttempt
import skills.storage.model.UserQuizAnswerAttempt
import skills.storage.model.QuizDef
import skills.storage.model.QuizDefWithDescription
import skills.storage.model.QuizQuestionDef
import skills.storage.model.UserAttrs
import skills.storage.model.UserQuizQuestionAttempt
import skills.storage.repos.QuizAnswerDefRepo
import skills.storage.repos.UserQuizAnswerAttemptRepo
import skills.storage.repos.UserQuizAttemptRepo
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizDefWithDescRepo
import skills.storage.repos.QuizQuestionDefRepo
import skills.storage.repos.UserQuizQuestionAttemptRepo

@Service
@Slf4j
class QuizRunService {

    @Autowired
    QuizDefWithDescRepo quizDefWithDescRepo

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    QuizQuestionDefRepo quizQuestionRepo

    @Autowired
    QuizAnswerDefRepo quizAnswerRepo

    @Autowired
    UserQuizAttemptRepo quizAttemptRepo

    @Autowired
    UserQuizQuestionAttemptRepo quizQuestionAttemptRepo

    @Autowired
    UserQuizAnswerAttemptRepo quizAttemptAnswerRepo

    @Autowired
    UserInfoService userInfoService

    @Transactional
    QuizInfo loadQuizInfo(String quizId) {
        QuizDefWithDescription updatedDef = quizDefWithDescRepo.findByQuizIdIgnoreCase(quizId)
        if (!updatedDef) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }

        List<QuizQuestionInfo> questions = loadQuizQuestionInfo(quizId)

        return new QuizInfo(
                name: updatedDef.name,
                description: updatedDef.description,
                questions: questions,
        )
    }

    private List<QuizQuestionInfo> loadQuizQuestionInfo(String quizId) {
        List<QuizQuestionDef> dbQuestionDefs = quizQuestionRepo.findAllByQuizIdIgnoreCase(quizId)
        List<QuizAnswerDef> dbAnswersDef = quizAnswerRepo.findAllByQuizIdIgnoreCase(quizId)
        Map<Integer, List<QuizAnswerDef>> byQuizId = dbAnswersDef.groupBy { it.questionRefId }

        List<QuizQuestionInfo> questions = dbQuestionDefs.collect {
            List<QuizAnswerDef> quizAnswerDefs = byQuizId[it.id]
            new QuizQuestionInfo(
                    id: it.id,
                    question: it.question,
                    canSelectMoreThanOne: quizAnswerDefs.count({ Boolean.valueOf(it.isCorrectAnswer) }) > 1,
                    answerOptions: quizAnswerDefs.collect {
                        new QuizAnswerOptionsInfo(
                                id: it.id,
                                answerOption: it.answer
                        )
                    }
            )
        }
        return questions
    }

    static class GradedResultInfo {
        QuizGradedResult gradedResult
        QuizDef quizDef

        List<QuizQuestionDef> dbQuestionDefs
        List<QuizAnswerDef> dbAnswersDefs

        Map<Integer, List<QuizAnswerDef>> answerDefByQuestionId
        Map<Integer, List<QuizQuestionAttemptReq>> selectedAnswersByQuestion
    }


    @Transactional
    QuizAttemptStartResult startQuizAttempt(String quizId) {
        UserAttrs currentUserAttrs = userInfoService.getCurrentUserAttrs()
        return this.startQuizAttempt(currentUserAttrs.userId, quizId)
    }

    @Transactional
    QuizAttemptStartResult startQuizAttempt(String userId, String quizId) {
        UserQuizAttempt inProgressAttempt = quizAttemptRepo.getByUserIdAndQuizIdAndState(userId, quizId, UserQuizAttempt.QuizAttemptStatus.INPROGRESS)
        if (inProgressAttempt) {
            List<Integer> alreadySelected = quizAttemptAnswerRepo.getSelectedAnswerIds(userId, quizId)
            log.info("Continued existing quiz attempt {}", inProgressAttempt)
            return new QuizAttemptStartResult(
                    id: inProgressAttempt.id,
                    inProgressAlready: true,
                    selectedAnswerIds: alreadySelected ?: []
            )
        }
        QuizDef quizDef = getQuizDef(quizId)
        UserQuizAttempt userQuizAttempt = new UserQuizAttempt(
                userId: userId,
                quizDefinitionRefId: quizDef.id,
                status: UserQuizAttempt.QuizAttemptStatus.INPROGRESS,
                started: new Date())
        UserQuizAttempt savedAttempt = quizAttemptRepo.saveAndFlush(userQuizAttempt)
        log.info("Started new quiz attempt {}", savedAttempt)
        return new QuizAttemptStartResult(id: savedAttempt.id)
    }

    @Transactional
    void reportQuestionAnswer(String quizId, Integer attemptId, Integer answerDefId) {
        UserAttrs currentUserAttrs = userInfoService.getCurrentUserAttrs()
        this.reportQuestionAnswer(currentUserAttrs.userId, quizId, attemptId, answerDefId)
    }

    @Transactional
    void reportQuestionAnswer(String userId, String quizId, Integer attemptId, Integer answerDefId) {
        if (!quizAttemptRepo.existsByUserIdAndIdAndQuizId(userId, attemptId, quizId)) {
            throw new SkillQuizException("Provided attempt id [${attemptId}] does not exist for [${userId}] user and [${quizId}] quiz", ErrorCode.BadParam)
        }

        QuizAnswerDefRepo.AnswerDefPartialInfo answerDefPartialInfo = quizAnswerRepo.getPartialDefByAnswerDefId(answerDefId)
        if (!answerDefPartialInfo) {
            throw new SkillQuizException("Provided answer id [${answerDefId}] does not exist", ErrorCode.BadParam)
        }
        if (answerDefPartialInfo.getQuizId() != quizId) {
            throw new SkillQuizException("Supplied quizId of [${quizId}] does not match answer's quiz id  of [${answerDefPartialInfo.getQuizId()}]", ErrorCode.BadParam)
        }

        boolean isCorrectChoice = Boolean.valueOf(answerDefPartialInfo.getIsCorrectAnswer())
        boolean answerAttemptAlreadyDocumented = quizAttemptAnswerRepo.existsByUserIdAndQuizAnswerDefinitionRefId(userId, answerDefId)
        if (!answerAttemptAlreadyDocumented) {
            UserQuizAnswerAttempt answerAttempt = new UserQuizAnswerAttempt(
                    userQuizAttemptRefId: attemptId,
                    quizAnswerDefinitionRefId: answerDefId,
                    userId: userId,
                    status: isCorrectChoice ? UserQuizAnswerAttempt.QuizAnswerStatus.CORRECT : UserQuizAnswerAttempt.QuizAnswerStatus.WRONG,
            )
            quizAttemptAnswerRepo.save(answerAttempt)
        } else {
            log.warn("Answer was already persisted for user [{}] for answerDefId of [{}]", userId, answerDefId)
        }

        List<Integer> correctAnswerDefId = quizQuestionRepo.getAllCorrectAnswerDefIdsByAnswerDefId(answerDefId)
        assert correctAnswerDefId
        boolean isMultipleChoice = correctAnswerDefId.size() > 1
        if (!isMultipleChoice) {
            List<Integer> toRemove = correctAnswerDefId.findAll{ it != answerDefId }
            toRemove.each {
                quizAttemptAnswerRepo.deleteByQuizAnswerDefinitionRefId(it)
            }
        }
    }

    @Transactional
    QuizGradedResult completeQuizAttempt(String quizId, Integer quizAttemptId) {
        UserAttrs currentUserAttrs = userInfoService.getCurrentUserAttrs()
        return this.completeQuizAttempt(currentUserAttrs.userId, quizId, quizAttemptId)
    }

    @Transactional
    QuizGradedResult completeQuizAttempt(String userId, String quizId, Integer quizAttemptId) {
        QuizDef quizDef = getQuizDef(quizId)
        Optional<UserQuizAttempt> optionalUserQuizAttempt = quizAttemptRepo.findById(quizAttemptId)
        if (!optionalUserQuizAttempt.isPresent()) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] does not exist", ErrorCode.BadParam)
        }
        UserQuizAttempt userQuizAttempt = optionalUserQuizAttempt.get()
        if (userQuizAttempt.quizDefinitionRefId != quizDef.id) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] is not for [${quizId}] quiz", ErrorCode.BadParam)
        }
        if (userQuizAttempt.userId != userId) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] is not for [${userId}] user", ErrorCode.BadParam)
        }

        List<QuizQuestionDef> dbQuestionDefs = quizQuestionRepo.findAllByQuizIdIgnoreCase(quizId)
        List<QuizAnswerDef> dbAnswersDefs = quizAnswerRepo.findAllByQuizIdIgnoreCase(quizId)
        Map<Integer, List<QuizAnswerDef>> answerDefByQuestionId = dbAnswersDefs.groupBy {it.questionRefId }

        Set<Integer> selectedAnswerIds = quizAttemptAnswerRepo.getSelectedAnswerIds(userId, quizId).toSet()

        List<QuizQuestionGradedResult> gradedQuestions = dbQuestionDefs.collect { QuizQuestionDef quizQuestionDef ->
            List<QuizAnswerDef> quizAnswerDefs = answerDefByQuestionId[quizQuestionDef.id]

            List<Integer> correctIds = quizAnswerDefs.findAll({ Boolean.valueOf(it.isCorrectAnswer) }).collect { it.id }.sort()
            List<Integer> selectedIds = quizAnswerDefs.findAll { selectedAnswerIds.contains(it.id) }?.collect { it.id }
            if (!selectedIds) {
                throw new SkillQuizException("There is no answer provided for question with [${quizQuestionDef.id}] id", quizId, ErrorCode.BadParam)
            }

            boolean isCorrect = selectedIds.containsAll(correctIds)

            UserQuizQuestionAttempt userQuizQuestionAttempt = new UserQuizQuestionAttempt(
                    userQuizAttemptRefId: quizAttemptId,
                    quizQuestionDefinitionRefId: quizQuestionDef.id,
                    userId: userId,
                    status: isCorrect ? UserQuizQuestionAttempt.QuizQuestionStatus.CORRECT : UserQuizQuestionAttempt.QuizQuestionStatus.WRONG,
            )
            quizQuestionAttemptRepo.save(userQuizQuestionAttempt)

            return new QuizQuestionGradedResult(questionId: quizQuestionDef.id, isCorrect: isCorrect, selectedAnswerIds: selectedIds, correctAnswerIds: correctIds)
        }
        boolean quizPassed = !gradedQuestions.find { !it.isCorrect }
        QuizGradedResult gradedResult = new QuizGradedResult(passed: quizPassed, gradedQuestions: gradedQuestions)

        userQuizAttempt.status = quizPassed ? UserQuizAttempt.QuizAttemptStatus.PASSED : UserQuizAttempt.QuizAttemptStatus.FAILED
        userQuizAttempt.completed = new Date()
        quizAttemptRepo.save(userQuizAttempt)

        return gradedResult
    }

    @Transactional
    @Deprecated
    QuizGradedResult reportQuizAttempt(String quizId, QuizAttemptReq quizAttemptReq) {
        UserAttrs currentUserAttrs = userInfoService.getCurrentUserAttrs()
        return this.reportQuizAttempt(currentUserAttrs.userId, quizId, quizAttemptReq)
    }

    @Transactional
    @Profile
    @Deprecated
    QuizGradedResult reportQuizAttempt(String userId, String quizId, QuizAttemptReq quizAttemptReq) {
        GradedResultInfo gradedResultInfo = loadDataAndGradeAttempt(quizId, quizAttemptReq)
        persistUserQuizAttempt(gradedResultInfo, userId)

        return gradedResultInfo.gradedResult
    }

    @Profile
    @Deprecated
    private GradedResultInfo loadDataAndGradeAttempt(String quizId, QuizAttemptReq quizAttemptReq) {
        QuizDef quizDef = getQuizDef(quizId)

        List<QuizQuestionDef> dbQuestionDefs = quizQuestionRepo.findAllByQuizIdIgnoreCase(quizId)
        List<QuizAnswerDef> dbAnswersDefs = quizAnswerRepo.findAllByQuizIdIgnoreCase(quizId)
        Map<Integer, List<QuizAnswerDef>> answerDefByQuestionId = dbAnswersDefs.groupBy {it.questionRefId }
        Map<Integer, List<QuizQuestionAttemptReq>> selectedAnswersByQuestion = quizAttemptReq.questionAnswers.groupBy { it.questionId }

        List<QuizQuestionGradedResult> gradedQuestions = dbQuestionDefs.collect { QuizQuestionDef quizQuestionDef ->
            List<QuizAnswerDef> quizAnswerDefs = answerDefByQuestionId[quizQuestionDef.id]

            List<Integer> correctIds = quizAnswerDefs.findAll({ Boolean.valueOf(it.isCorrectAnswer) }).collect { it.id }.sort()

            List<QuizQuestionAttemptReq> selectedAnswers = selectedAnswersByQuestion[quizQuestionDef.id]
            if (!selectedAnswers || selectedAnswers.isEmpty() || !selectedAnswers[0].selectedAnswerIds) {
                throw new SkillQuizException("There is no answer provided for question with [${quizQuestionDef.id}] id", quizId, ErrorCode.BadParam)
            }
            List<Integer> selectedIds = selectedAnswers[0].selectedAnswerIds.sort()
            boolean isCorrect = ListUtils.isEqualList(correctIds, selectedIds)
            return new QuizQuestionGradedResult(questionId: quizQuestionDef.id, isCorrect: isCorrect, selectedAnswerIds: selectedIds, correctAnswerIds: correctIds)
        }
        boolean quizPassed = !gradedQuestions.find { !it.isCorrect }
        QuizGradedResult gradedResult = new QuizGradedResult(passed: quizPassed, gradedQuestions: gradedQuestions)

        return new GradedResultInfo(
                gradedResult: gradedResult,
                quizDef: quizDef,
                dbQuestionDefs: dbQuestionDefs,
                dbAnswersDefs: dbAnswersDefs,
                answerDefByQuestionId: answerDefByQuestionId,
                selectedAnswersByQuestion: selectedAnswersByQuestion,
        )
    }

    @Profile
    @Deprecated
    private void persistUserQuizAttempt(GradedResultInfo gradedResultInfo, String userId) {
        QuizGradedResult gradedResult = gradedResultInfo.gradedResult

        UserQuizAttempt quizAttempt = new UserQuizAttempt(
                quizDefinitionRefId: gradedResultInfo.quizDef.id,
                userId: userId,
                status: gradedResult.passed ? UserQuizAttempt.QuizAttemptStatus.PASSED : UserQuizAttempt.QuizAttemptStatus.FAILED)
        UserQuizAttempt savedQuizAttempt = quizAttemptRepo.saveAndFlush(quizAttempt)

        Map<Integer, List<QuizQuestionGradedResult>> gradedQuestionsByQuestionId = gradedResult.gradedQuestions.groupBy { it.questionId }
        gradedResultInfo.dbQuestionDefs.each { QuizQuestionDef quizQuestionDef ->
            QuizQuestionGradedResult quizQuestionGradedResult = gradedQuestionsByQuestionId[quizQuestionDef.id].first()
            UserQuizQuestionAttempt userQuizQuestionAttempt = new UserQuizQuestionAttempt(
                    userQuizAttemptRefId: savedQuizAttempt.id,
                    quizQuestionDefinitionRefId: quizQuestionDef.id,
                    userId: userId,
                    status: quizQuestionGradedResult.isCorrect ? UserQuizQuestionAttempt.QuizQuestionStatus.CORRECT : UserQuizQuestionAttempt.QuizQuestionStatus.WRONG,
            )
            UserQuizQuestionAttempt savedUserQuizQuestionAttempt = quizQuestionAttemptRepo.saveAndFlush(userQuizQuestionAttempt)

            List<QuizAnswerDef> answers = gradedResultInfo.answerDefByQuestionId[quizQuestionDef.id]
            List<Integer> selectedAnswerIds = gradedResultInfo.selectedAnswersByQuestion[quizQuestionDef.id].first().selectedAnswerIds
            List<UserQuizAnswerAttempt> userQuizAnswerAttempts = answers.findAll({ selectedAnswerIds.contains(it.id) }).collect { QuizAnswerDef quizAnswerDef ->
                boolean isCorrect = Boolean.valueOf(quizAnswerDef.isCorrectAnswer)
                new UserQuizAnswerAttempt(
                        userQuizQuestionAttemptRefId: savedUserQuizQuestionAttempt.id,
                        quizAnswerDefinitionRefId: quizAnswerDef.id,
                        userId: userId,
                        status: isCorrect ? UserQuizAnswerAttempt.QuizAnswerStatus.CORRECT : UserQuizAnswerAttempt.QuizAnswerStatus.WRONG,
                )
            }
            quizAttemptAnswerRepo.saveAll(userQuizAnswerAttempts)
        }
    }

    private QuizDef getQuizDef(String quizId) {
        QuizDef quizDef = quizDefRepo.findByQuizIdIgnoreCase(quizId)
        if (!quizDef) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }
        return quizDef
    }
}
