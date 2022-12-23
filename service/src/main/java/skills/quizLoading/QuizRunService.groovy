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
    QuizGradedResult reportQuizAttempt(String quizId, QuizAttemptReq quizAttemptReq) {
        UserAttrs currentUserAttrs = userInfoService.getCurrentUserAttrs()
        return this.reportQuizAttempt(currentUserAttrs.userId, quizId, quizAttemptReq)
    }

    @Transactional
    @Profile
    QuizGradedResult reportQuizAttempt(String userId, String quizId, QuizAttemptReq quizAttemptReq) {
        GradedResultInfo gradedResultInfo = loadDataAndGradeAttempt(quizId, quizAttemptReq)
        persistUserQuizAttempt(gradedResultInfo, userId)

        return gradedResultInfo.gradedResult
    }

    @Profile
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
            List<UserQuizAnswerAttempt> userQuizAnswerAttempts = answers.collect { QuizAnswerDef quizAnswerDef ->
                boolean isSelected = selectedAnswerIds.contains(quizAnswerDef.id)
                boolean isCorrect = (quizAnswerDef.isCorrectAnswer && isSelected) || (!quizAnswerDef.isCorrectAnswer && !isSelected)
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
