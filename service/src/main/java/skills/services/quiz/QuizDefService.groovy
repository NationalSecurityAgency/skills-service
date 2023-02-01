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

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.PublicProps
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.PublicPropsBasedValidator
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.QuizValidator
import skills.controller.exceptions.SkillQuizException
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.QuizAnswerDefRequest
import skills.controller.request.model.QuizDefRequest
import skills.controller.request.model.QuizQuestionDefRequest
import skills.controller.request.model.QuizSettingsRequest
import skills.controller.result.model.*
import skills.quizLoading.QuizSettings
import skills.services.*
import skills.services.admin.DataIntegrityExceptionHandlers
import skills.services.admin.ServiceValidatorHelper
import skills.storage.model.*
import skills.storage.model.auth.RoleName
import skills.storage.repos.*
import skills.utils.InputSanitizer
import skills.utils.Props

@Service
@Slf4j
class QuizDefService {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    QuizQuestionDefRepo quizQuestionRepo

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    @Autowired
    UserQuizQuestionAttemptRepo userQuizQuestionAttemptRepo

    @Autowired
    UserQuizAnswerAttemptRepo userQuizAnswerAttemptRepo

    @Autowired
    QuizAnswerDefRepo quizAnswerRepo

    @Autowired
    QuizSettingsRepo quizSettingsRepo

    @Autowired
    LockingService lockingService

    @Autowired
    CustomValidator customValidator

    @Autowired
    QuizDefWithDescRepo quizDefWithDescRepo

    @Autowired
    ServiceValidatorHelper serviceValidatorHelper

    @Autowired
    CreatedResourceLimitsValidator createdResourceLimitsValidator

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    PublicPropsBasedValidator propsBasedValidator

    @Transactional(readOnly = true)
    List<QuizDefResult> getCurrentUsersTestDefs() {
        boolean isRoot = userInfoService.isCurrentUserASuperDuperUser()

        UserInfo userInfo = userInfoService.currentUser
        String userId = userInfo.username?.toLowerCase()
        List<QuizDefResult> res = []

        List<QuizDefRepo.QuizDefBasicResult> fromDb = quizDefRepo.getQuizDefSummariesByUser(userId)
        if (fromDb) {
            res.addAll(fromDb.collect {convert(it) })
        }


//        Map<String, Integer> projectIdSortOrder = sortingService.getUserProjectsOrder(userId)
//        List<ProjectResult> finalRes
//        if (isRoot) {
//            finalRes = loadProjectsForRoot(projectIdSortOrder, userId)
//        } else {
//            // sql join with UserRoles and there is 1-many relationship that needs to be normalized
//            List<ProjSummaryResult> projects = projDefRepo.getProjectSummariesByUser(userId)
//            finalRes = projects?.unique({ it.projectId })?.collect({
//                ProjectResult res = convert(it, projectIdSortOrder)
//                return res
//            })
//        }
//
//        finalRes.sort() { it.displayOrder }
//
//        if (finalRes) {
//            finalRes.first().isFirst = true
//            finalRes.last().isLast = true

        return res
    }

    @Transactional(readOnly = true)
    QuizDefResult getQuizDef(String quizId) {
        assert quizId
        QuizDefWithDescription quizDefWithDescription = quizDefWithDescRepo.findByQuizIdIgnoreCase(quizId)
        return convert(quizDefWithDescription)
    }

    @Transactional(readOnly = true)
    QuizDefSummaryResult getQuizDefSummary(String quizId) {
        assert quizId
        QuizDefRepo.QuizDefSummaryRes dbRes = quizDefRepo.getQuizDefSummary(quizId)
        return new QuizDefSummaryResult(
                quizId: quizId,
                name: dbRes.getName(),
                created: dbRes.getCreated(),
                type: QuizDefParent.QuizType.valueOf(dbRes.getQuizType()),
                numQuestions: dbRes.getNumQuestions()
        )
    }

    @Transactional()
    QuizDefResult saveQuizDef(String originalQuizId, String newQuizId, QuizDefRequest quizDefRequest, String userIdParam = null) {
        validateQuizDefRequest(newQuizId, quizDefRequest)

        newQuizId = InputSanitizer.sanitize(newQuizId)
        quizDefRequest.name = InputSanitizer.sanitize(quizDefRequest.name)?.trim()
        quizDefRequest.description = StringUtils.trimToNull(InputSanitizer.sanitize(quizDefRequest.description))

        lockingService.lockQuizDefs()

        QuizDefWithDescription quizDefWithDescription = originalQuizId ? quizDefWithDescRepo.findByQuizIdIgnoreCase(originalQuizId) : null
        if (!quizDefWithDescription || !quizDefWithDescription.quizId.equalsIgnoreCase(originalQuizId)) {
            serviceValidatorHelper.validateQuizIdDoesNotExist(newQuizId)
        }
        if (!quizDefWithDescription || !quizDefWithDescription.name.equalsIgnoreCase(quizDefWithDescription.name)) {
            serviceValidatorHelper.validateQuizNameDoesNotExist(quizDefRequest.name, newQuizId)
        }
        if (quizDefWithDescription) {
            QuizDefParent.QuizType incomingType = QuizDefParent.QuizType.valueOf(quizDefRequest.type)
            QuizValidator.isTrue(quizDefWithDescription.type == incomingType, "Existing quiz type cannot be changed", quizDefWithDescription.quizId)
            Props.copy(quizDefRequest, quizDefWithDescription)
            log.debug("Updating [{}]", quizDefWithDescription)

            DataIntegrityExceptionHandlers.dataIntegrityViolationExceptionHandler.handle(null, null, quizDefWithDescription.quizId) {
                quizDefWithDescription = quizDefWithDescRepo.save(quizDefWithDescription)
            }
            log.debug("Saved [{}]", quizDefWithDescription)
        } else {
            quizDefWithDescription = new QuizDefWithDescription(quizId: newQuizId, name: quizDefRequest.name,
                    description: quizDefRequest.description, type: QuizDefParent.QuizType.valueOf(quizDefRequest.type))
            log.debug("Created project [{}]", quizDefWithDescription)

            String userId = userIdParam ?: userInfoService.getCurrentUserId()

            if (!userInfoService.isCurrentUserASuperDuperUser()) {
                createdResourceLimitsValidator.validateNumQuizDefsCreated(userId)
            }

            DataIntegrityExceptionHandlers.dataIntegrityViolationExceptionHandler.handle(null, null, quizDefWithDescription.quizId) {
                quizDefWithDescription = quizDefWithDescRepo.save(quizDefWithDescription)
            }

            log.debug("Saved [{}]", quizDefWithDescription)

            accessSettingsStorageService.addQuizDefUserRole(userId, newQuizId, RoleName.ROLE_QUIZ_ADMIN)
        }

        QuizDef updatedDef = quizDefRepo.findByQuizIdIgnoreCase(quizDefWithDescription.quizId)
        return convert(updatedDef)
    }

    @Transactional()
    void deleteQuestion(String quizId, Integer quizQuestionDefId) {
        QuizDef quizDef = findQuizDef(quizId)

        lockingService.lockQuizDef(quizDef.quizId)

        QuizQuestionDef quizQuestionDef = quizQuestionRepo.getById(quizQuestionDefId)
        QuizValidator.isNotNull(quizQuestionDef, "Question Definition ID", quizDef.quizId)
        if (quizQuestionDef.quizId != quizDef.quizId) {
            throw new SkillQuizException("Provided Question Definition ID [${quizQuestionDefId}] does not belong to the quiz [${quizDef.quizId}]", quizDef.quizId, ErrorCode.BadParam)
        }


        QuizSetting quizSetting = quizSettingsRepo.findBySettingAndQuizRefId(QuizSettings.MinNumQuestionsToPass.setting, quizDef.id)
        if (quizSetting) {
            Integer minNumQuestionsToPass = Integer.valueOf(quizSetting.value)
            int numTotalQuestions = quizQuestionRepo.countByQuizId(quizDef.quizId) -1
            if (numTotalQuestions == 0) {
                quizSettingsRepo.delete(quizSetting)
            } else if (numTotalQuestions < minNumQuestionsToPass) {
                quizSetting.value =  "${numTotalQuestions}"
                quizSettingsRepo.save(quizSetting)
            }
        }

        quizAnswerRepo.delete(quizQuestionDef)
    }

    @Transactional()
    QuizQuestionDefResult saveQuestion(String quizId, QuizQuestionDefRequest questionDefRequest) {
        QuizDef quizDef = findQuizDef(quizId)
        validate(quizDef, questionDefRequest)

        lockingService.lockQuizDef(quizDef.quizId)

        Integer maxExistingDisplayOrder = quizQuestionRepo.getMaxQuestionDisplayOrderByQuizId(quizDef.quizId)
        int displayOrder = maxExistingDisplayOrder != null ? maxExistingDisplayOrder + 1 : 0

        QuizQuestionType questionType = questionDefRequest.questionType
        if (!questionType || (questionType != QuizQuestionType.TextInput && quizDef.type != QuizDefParent.QuizType.Survey)) {
            questionType = questionDefRequest.answers.count { it.isCorrect } > 1 ?
                    QuizQuestionType.MultipleChoice : QuizQuestionType.SingleChoice
        }

        QuizQuestionDef questionDef = new QuizQuestionDef(
                quizId: quizDef.quizId,
                question: questionDefRequest.question,
                type: questionType,
                displayOrder: displayOrder,
        )
        QuizQuestionDef savedQuestion = quizQuestionRepo.saveAndFlush(questionDef)

        boolean isTextInputQuestion = questionDefRequest.questionType == QuizQuestionType.TextInput

        List<QuizAnswerDef> answerDefs
        if (isTextInputQuestion) {
            answerDefs = [
                    new QuizAnswerDef(
                            quizId: quizDef.quizId,
                            questionRefId: savedQuestion.id,
                            answer: null,
                            isCorrectAnswer: false,
                            displayOrder: 1,
                    )
            ]
        } else {
            answerDefs = questionDefRequest.answers.withIndex().collect { QuizAnswerDefRequest answerDefRequest, int index ->
                new QuizAnswerDef(
                        quizId: quizDef.quizId,
                        questionRefId: savedQuestion.id,
                        answer: answerDefRequest.answer,
                        isCorrectAnswer: answerDefRequest.isCorrect,
                        displayOrder: index + 1,
                )
            }
        }
        List<QuizAnswerDef> savedAnswers = quizAnswerRepo.saveAllAndFlush(answerDefs)

        return convert(savedQuestion, savedAnswers)
    }

    @Transactional
    void setDisplayOrder(String quizId, Integer questionId,  ActionPatchRequest patchRequest) {
        QuizDef quizDef = findQuizDef(quizId)
        lockingService.lockQuizDef(quizDef.quizId)
        if(ActionPatchRequest.ActionType.NewDisplayOrderIndex == patchRequest.action) {
            List<QuizQuestionDefRepo.DisplayOrder> currentDisplayOrder = quizQuestionRepo.getQuestionsDisplayOrder(quizDef.quizId)
            QuizQuestionDefRepo.DisplayOrder theItemToMove = currentDisplayOrder.find { it.getId() == questionId }
            QuizValidator.isTrue(theItemToMove != null, "Provide question id [${questionId}] is not valid", quizId)
            List<QuizQuestionDefRepo.DisplayOrder> mutableDisplayOrder = currentDisplayOrder.findAll { it.getId() != questionId }.sort { it.getDisplayOrder() }

            int newIndex = Math.min(patchRequest.newDisplayOrderIndex, currentDisplayOrder.size() - 1)
            mutableDisplayOrder.add(newIndex, theItemToMove)

            mutableDisplayOrder.eachWithIndex{ QuizQuestionDefRepo.DisplayOrder entry, int i ->
                quizQuestionRepo.updateDisplayOrder(entry.getId(), i)
            }
        }
    }

    @Transactional
    QuizQuestionsResult getQuestionDefs(String quizId) {
        QuizDef quizDef = findQuizDef(quizId)
        List<QuizQuestionDef> dbQuestionDefs = quizQuestionRepo.findAllByQuizIdIgnoreCase(quizId)
        if (!dbQuestionDefs){
            return new QuizQuestionsResult(quizType: quizDef.type, questions: [])
        }

        List<QuizAnswerDef> dbAnswersDef = quizAnswerRepo.findAllByQuizIdIgnoreCase(quizId)
        Map<Integer, List<QuizAnswerDef>> byQuizId = dbAnswersDef.groupBy {it.questionRefId }

        List<QuizQuestionDefResult> questions = dbQuestionDefs.collect { QuizQuestionDef quizQuestionDef ->
            List<QuizAnswerDef> quizAnswerDefs = byQuizId[quizQuestionDef.id]
            convert(quizQuestionDef, quizAnswerDefs)
        }.sort({ it.displayOrder })

        return new QuizQuestionsResult(quizType: quizDef.type, questions: questions)
    }

    @Transactional
    QuizMetrics getMetrics(String quizId) {
        List<LabeledCount> quizCounts = userQuizAttemptRepo.getUserQuizAttemptCounts(quizId)

        int total = quizCounts.collect { it.getCount() }.sum()
        LabeledCount numPassedCount = quizCounts.find{
            UserQuizAttempt.QuizAttemptStatus.PASSED.toString().equalsIgnoreCase(it.getLabel())
        }
        int numPassed = numPassedCount ? numPassedCount.getCount() : 0

        QuizQuestionsResult quizQuestionsResult = getQuestionDefs(quizId)
        List<QuizQuestionDefResult> questionDefResults = quizQuestionsResult?.questions
        List<UserQuizQuestionAttemptRepo.IdAndStatusCount> questionIdAndStatusCounts = userQuizQuestionAttemptRepo.getUserQuizQuestionAttemptCounts(quizId)
        List<UserQuizQuestionAttemptRepo.IdAndStatusCount> answerIdAndStatusCounts = userQuizQuestionAttemptRepo.getUserQuizAnswerAttemptCounts(quizId)

        Map<Integer, List<UserQuizQuestionAttemptRepo.IdAndStatusCount>> byQuestionId = questionIdAndStatusCounts.groupBy { it.getId()}
        Map<Integer, List<UserQuizQuestionAttemptRepo.IdAndStatusCount>> byAnswerId = answerIdAndStatusCounts.groupBy { it.getId()}

        List<QuizQuestionMetricsResult> questions = questionDefResults.collect { QuizQuestionDefResult questionDefResult ->
            List<UserQuizQuestionAttemptRepo.IdAndStatusCount> questionStatusCounts = byQuestionId[questionDefResult.id]
            UserQuizQuestionAttemptRepo.IdAndStatusCount questionCorrectCounts = questionStatusCounts?.find { it.getStatus() == UserQuizQuestionAttempt.QuizQuestionStatus.CORRECT.toString() }
            UserQuizQuestionAttemptRepo.IdAndStatusCount questionWrongCounts = questionStatusCounts?.find { it.getStatus() == UserQuizQuestionAttempt.QuizQuestionStatus.WRONG.toString() }
            return new QuizQuestionMetricsResult(
                    id: questionDefResult.id,
                    question: questionDefResult.question,
                    questionType: questionDefResult.questionType,
                    numAnsweredCorrect: questionCorrectCounts?.getCount() ?: 0,
                    numAnsweredWrong: questionWrongCounts?.getCount() ?: 0,

                    answers: questionDefResult.answers.collect { QuizAnswerDefResult quizAnswerDefResult ->
                        List<UserQuizQuestionAttemptRepo.IdAndStatusCount> answerStatusCounts = byAnswerId[quizAnswerDefResult.id]
                        UserQuizQuestionAttemptRepo.IdAndStatusCount answerCorrectCount = answerStatusCounts?.find { it.getStatus() == UserQuizAnswerAttempt.QuizAnswerStatus.CORRECT.toString() }
                        UserQuizQuestionAttemptRepo.IdAndStatusCount answerWrongCount = answerStatusCounts?.find { it.getStatus() == UserQuizAnswerAttempt.QuizAnswerStatus.WRONG.toString() }
                        new QuizAnswerMetricsResult(
                                id: quizAnswerDefResult.id,
                                answer: quizAnswerDefResult.answer,
                                isCorrect: quizAnswerDefResult.isCorrect,
                                numAnswered: answerStatusCounts ? answerStatusCounts.collect{ it.getCount() }.sum() : 0,
                                numAnsweredCorrect: answerCorrectCount?.getCount() ?: 0,
                                numAnsweredWrong: answerWrongCount?.getCount() ?: 0,
                        )
                    }
            )
        }

        return new QuizMetrics(
                numTaken: total,
                numPassed: numPassed,
                numFailed: total - numPassed,
                questions: questions,
        )
    }

    private QuizQuestionDefResult convert(QuizQuestionDef savedQuestion, List<QuizAnswerDef> savedAnswers) {
        new QuizQuestionDefResult(
                id: savedQuestion.id,
                question: savedQuestion.question,
                questionType: savedQuestion.type,
                answers: savedAnswers.collect { convert (it)},
                displayOrder: savedQuestion.displayOrder,
        )
    }

    private QuizAnswerDefResult convert(QuizAnswerDef savedAnswer) {
        new QuizAnswerDefResult(
                id: savedAnswer.id,
                answer: savedAnswer.answer,
                isCorrect: Boolean.valueOf(savedAnswer.isCorrectAnswer),
                displayOrder: savedAnswer.displayOrder,
        )
    }

    private QuizDef findQuizDef(String quizId) {
        QuizDef updatedDef = quizDefRepo.findByQuizIdIgnoreCase(quizId)
        if (!updatedDef) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }
        return updatedDef
    }

    private void validate(QuizDef quizDef, QuizQuestionDefRequest questionDefRequest) {
        String quizId = quizDef.getQuizId()
        QuizValidator.isNotBlank(questionDefRequest.question, "question", quizId)
        QuizValidator.isNotNull(questionDefRequest.questionType, "questionType", quizId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.descriptionMaxLength, "Question", questionDefRequest.question)
        CustomValidationResult customValidationResult = customValidator.validateDescription(questionDefRequest.question)
        if (!customValidationResult.valid) {
            throw new SkillQuizException("Question: ${customValidationResult.msg}", quizId, ErrorCode.BadParam)
        }

        if (questionDefRequest.questionType != QuizQuestionType.TextInput) {
            QuizValidator.isNotNull(questionDefRequest.answers, "answers", quizId)
            questionDefRequest.answers.each {
                QuizValidator.isNotBlank(it.answer, "answers.answer", quizId)
            }
        }
        if (quizDef.type == QuizDefParent.QuizType.Quiz) {
            QuizValidator.isTrue(questionDefRequest.answers.size() >= 2, "Must have at least 2 answers", quizId)
            QuizValidator.isTrue(questionDefRequest.answers.find({ it.isCorrect }) != null, "For quiz.type of Quiz must set isCorrect=true on at least 1 question", quizId)

            if (questionDefRequest.questionType == QuizQuestionType.MultipleChoice) {
                QuizValidator.isTrue(questionDefRequest.answers.count({ it.isCorrect }) >= 2, "For questionType=[${QuizQuestionType.MultipleChoice}] must provide >= 2 correct answers", quizId)
            } else if (questionDefRequest.questionType == QuizQuestionType.SingleChoice) {
                QuizValidator.isTrue(questionDefRequest.answers.count({ it.isCorrect }) == 1, "For questionType=[${QuizQuestionType.SingleChoice}] must provide exactly 1 correct answer", quizId)
            } else {
                QuizValidator.isTrue(false, "questionType=[${questionDefRequest.questionType}] is not supported for quiz.type of Quiz", quizId)
            }
        } else {
            QuizValidator.isTrue(questionDefRequest.answers.find({ it.isCorrect }) == null, "All answers for a survey questions must set to isCorrect=false", quizId)
            if (questionDefRequest.questionType == QuizQuestionType.TextInput) {
                if (questionDefRequest.answers) {
                    throw new SkillQuizException("Questions with type of ${QuizQuestionType.TextInput} must not provide an answer]", quizId, ErrorCode.BadParam)
                }
            }
        }

    }


    @Transactional()
    void deleteQuiz(String quizId) {
        int numRemoved = quizDefRepo.deleteByQuizIdIgnoreCase(quizId)
        log.debug("Deleted project with id [{}]. Removed [{}] record", quizId, numRemoved)
    }

    @Transactional(readOnly = true)
    boolean existsByQuizId(String quizId) {
        return quizDefRepo.existsByQuizIdIgnoreCase(quizId)
    }

    @Transactional(readOnly = true)
    boolean existsByQuizName(String quizName) {
        return quizDefRepo.existsByNameIgnoreCase(quizName)
    }


    Set<String> availableQuizTypes = QuizDefParent.QuizType.values().collect({ it.toString() }).toSet()
    private void validateQuizDefRequest(String quizId, QuizDefRequest quizDefRequest) {
        IdFormatValidator.validate(quizId)
        propsBasedValidator.quizValidationMaxStrLength(PublicProps.UiProp.maxIdLength, "QuizId", quizId, quizId)
        propsBasedValidator.quizValidationMinStrLength(PublicProps.UiProp.minIdLength, "QuizId", quizId, quizId)

        propsBasedValidator.quizValidationMaxStrLength(PublicProps.UiProp.maxQuizNameLength, "Quiz Name", quizDefRequest.name, quizId)
        propsBasedValidator.quizValidationMinStrLength(PublicProps.UiProp.minNameLength, "Quiz Name", quizDefRequest.name, quizId)

        if (quizDefRequest.description) {
            propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.descriptionMaxLength, "Quiz Description", quizDefRequest.description)
        }

        QuizValidator.isNotBlank(quizDefRequest.type, "Type")
        if (!availableQuizTypes.contains(quizDefRequest.type)){
            throw new SkillQuizException("Not supported quiz type [${quizDefRequest.type}] please select one from ${availableQuizTypes}", quizId, ErrorCode.BadParam)
        }

        if (!quizDefRequest?.name) {
            throw new SkillQuizException("Quiz name was not provided.", quizId, ErrorCode.BadParam)
        }

        CustomValidationResult customValidationResult = customValidator.validate(quizDefRequest)
        if (!customValidationResult.valid) {
            throw new SkillQuizException(customValidationResult.msg, quizId, ErrorCode.BadParam)
        }
    }

    private QuizDefResult convert(QuizDefRepo.QuizDefBasicResult quizDefSummaryResult) {
        QuizDefResult result = Props.copy(quizDefSummaryResult, new QuizDefResult())
        result.type = QuizDefParent.QuizType.valueOf(quizDefSummaryResult.getQuizType())
        result.displayOrder = 0 // todo
        return result
    }

    private QuizDefResult convert(QuizDef updatedDef) {
        QuizDefResult result = Props.copy(updatedDef, new QuizDefResult())
        result.displayOrder = 0 // todo
        return result
    }

    private QuizDefResult convert(QuizDefWithDescription updatedDef) {
        QuizDefResult result = Props.copy(updatedDef, new QuizDefResult())
        result.displayOrder = 0 // todo
        return result
    }
}
