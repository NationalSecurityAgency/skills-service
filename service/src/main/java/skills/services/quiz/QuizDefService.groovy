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
import skills.controller.result.model.QuizAnswerDefResult
import skills.controller.result.model.QuizDefResult
import skills.controller.result.model.QuizMetrics
import skills.controller.result.model.QuizQuestionDefResult
import skills.services.AccessSettingsStorageService
import skills.services.CreatedResourceLimitsValidator
import skills.services.CustomValidationResult
import skills.services.CustomValidator
import skills.services.IdFormatValidator
import skills.services.LockingService
import skills.services.admin.DataIntegrityExceptionHandlers
import skills.services.admin.ServiceValidatorHelper
import skills.storage.model.LabeledCount
import skills.storage.model.QuizAnswerDef
import skills.storage.model.QuizDef
import skills.storage.model.QuizDefWithDescription
import skills.storage.model.QuizQuestionDef
import skills.storage.model.UserQuizAttempt
import skills.storage.model.auth.RoleName
import skills.storage.repos.QuizAnswerDefRepo
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizDefWithDescRepo
import skills.storage.repos.QuizQuestionDefRepo
import skills.storage.repos.UserQuizAnswerAttemptRepo
import skills.storage.repos.UserQuizAttemptRepo
import skills.storage.repos.UserQuizQuestionAttemptRepo
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

        List<QuizDefRepo.QuizDefSummaryResult> fromDb = quizDefRepo.getQuizDefSummariesByUser(userId)
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
            Props.copy(quizDefRequest, quizDefWithDescription)
            log.debug("Updating [{}]", quizDefWithDescription)

            DataIntegrityExceptionHandlers.dataIntegrityViolationExceptionHandler.handle(null, null, quizDefWithDescription.quizId) {
                quizDefWithDescription = quizDefWithDescRepo.save(quizDefWithDescription)
            }
            log.debug("Saved [{}]", quizDefWithDescription)
        } else {
            quizDefWithDescription = new QuizDefWithDescription(quizId: newQuizId, name: quizDefRequest.name,
                    description: quizDefRequest.description)
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
    QuizQuestionDefResult saveQuestion(String quizId, QuizQuestionDefRequest questionDefRequest) {
        validate(quizId, questionDefRequest)
        QuizDef quizDef = findQuizDef(quizId)

        lockingService.lockQuizDef(quizDef.quizId)

        Integer maxExistingDisplayOrder = quizQuestionRepo.getMaxQuestionDisplayOrderByQuizId(quizDef.quizId)
        int displayOrder = maxExistingDisplayOrder != null ? maxExistingDisplayOrder + 1 : 0

        QuizQuestionDef questionDef = new QuizQuestionDef(
                quizId: quizDef.quizId,
                question: questionDefRequest.question,
                type: questionDefRequest.questionType,
                displayOrder: displayOrder,
        )
        QuizQuestionDef savedQuestion = quizQuestionRepo.saveAndFlush(questionDef)

        List<QuizAnswerDef> answerDefs = questionDefRequest.answers.withIndex().collect { QuizAnswerDefRequest answerDefRequest, int index ->
            new QuizAnswerDef(
                    quizId: quizDef.quizId,
                    questionRefId: savedQuestion.id,
                    answer: answerDefRequest.answer,
                    isCorrectAnswer: answerDefRequest.isCorrect,
                    displayOrder: index + 1,
            )
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
    List<QuizQuestionDefResult> getQuestionDefs(String quizId) {
        List<QuizQuestionDef> dbQuestionDefs = quizQuestionRepo.findAllByQuizIdIgnoreCase(quizId)
        if (!dbQuestionDefs){
            return []
        }

        List<QuizAnswerDef> dbAnswersDef = quizAnswerRepo.findAllByQuizIdIgnoreCase(quizId)
        Map<Integer, List<QuizAnswerDef>> byQuizId = dbAnswersDef.groupBy {it.questionRefId }

        return dbQuestionDefs.collect { QuizQuestionDef quizQuestionDef ->
            List<QuizAnswerDef> quizAnswerDefs = byQuizId[quizQuestionDef.id]
            convert(quizQuestionDef, quizAnswerDefs)
        }.sort({ it.displayOrder })
    }

    @Transactional
    QuizMetrics getMetrics(String quizId) {
        List<LabeledCount> quizCounts = userQuizAttemptRepo.getUserQuizAttemptCounts(quizId)

        int total = quizCounts.collect { it.getCount() }.sum()
        LabeledCount numPassedCount = quizCounts.find{
            UserQuizAttempt.QuizAttemptStatus.PASSED.toString().equalsIgnoreCase(it.getLabel())
        }
        int numPassed = numPassedCount ? numPassedCount.getCount() : 0

        List<QuizQuestionDefResult> questionDefResults = getQuestionDefs(quizId)
        List<UserQuizQuestionAttemptRepo.QuestionIdAndStatusCount> questionIdAndStatusCounts = userQuizQuestionAttemptRepo.getUserQuizQuestionAttemptCounts(quizId)

        return new QuizMetrics(
                numTaken: total,
                numPassed: numPassed,
                numFailed: total - numPassed,
                questionDefResults: questionDefResults,
        )
    }

    private QuizQuestionDefResult convert(QuizQuestionDef savedQuestion, List<QuizAnswerDef> savedAnswers) {
        new QuizQuestionDefResult(
                id: savedQuestion.id,
                question: savedQuestion.question,
                questionType: QuizQuestionType.valueOf(savedQuestion.type),
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
    private void validate(String quizId, QuizQuestionDefRequest questionDefRequest) {
        QuizValidator.isNotBlank(questionDefRequest.question, "question", quizId)
        QuizValidator.isNotNull(questionDefRequest.questionType, "questionType", quizId)
        QuizValidator.isNotNull(questionDefRequest.answers, "questions", quizId)
        QuizValidator.isTrue(questionDefRequest.answers.size() >= 2, "Must have at least answer", quizId)
        questionDefRequest.answers.each {
            QuizValidator.isNotBlank(it.answer, "questions.answer", quizId)
        }
        QuizValidator.isTrue(questionDefRequest.answers.find({ it.isCorrect}) != null, "Must set isCorrect=true on at least 1 question", quizId)

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


    private void validateQuizDefRequest(String quizId, QuizDefRequest quizDefRequest) {
        IdFormatValidator.validate(quizId)
        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "QuizId Id", quizId)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "QuizId Id", quizId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxQuizNameLength, "Quiz Name", quizDefRequest.name)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minNameLength, "Quiz Name", quizDefRequest.name)

        if (!quizDefRequest?.name) {
            throw new SkillQuizException("Quiz name was not provided.", quizId, ErrorCode.BadParam)
        }

        CustomValidationResult customValidationResult = customValidator.validate(quizDefRequest)
        if (!customValidationResult.valid) {
            throw new SkillQuizException(customValidationResult.msg, quizId, ErrorCode.BadParam)
        }
    }

    private QuizDefResult convert(QuizDefRepo.QuizDefSummaryResult quizDefSummaryResult) {
        QuizDefResult result = Props.copy(quizDefSummaryResult, new QuizDefResult())
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
