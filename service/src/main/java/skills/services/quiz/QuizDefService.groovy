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
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
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
import skills.controller.result.model.*
import skills.quizLoading.QuizSettings
import skills.services.*
import skills.services.admin.DataIntegrityExceptionHandlers
import skills.services.admin.ServiceValidatorHelper
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.*
import skills.storage.model.auth.RoleName
import skills.storage.repos.*
import skills.utils.InputSanitizer
import skills.utils.Props

import static org.springframework.data.domain.Sort.Direction.DESC

@Service
@Slf4j
class QuizDefService {

    @Value('${skills.config.ui.usersTableAdditionalUserTagKey:""}')
    String usersTableAdditionalUserTagKey

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

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    UserTagRepo userTagRepo

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    SkillEventAdminService skillEventAdminService

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    AttachmentService attachmentService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Transactional(readOnly = true)
    List<QuizDefResult> getCurrentUsersTestDefs() {
        UserInfo userInfo = userInfoService.currentUser
        String userId = userInfo.username?.toLowerCase()
        List<QuizDefResult> res = []

        List<QuizDefRepo.QuizDefBasicResult> fromDb = quizDefRepo.getQuizDefSummariesByUser(userId)
        if (fromDb) {
            fromDb = fromDb.sort { a, b -> b.created <=> a.created }
            res.addAll(fromDb.collect { convert(it) })
        }
        return res
    }

    @Transactional(readOnly = true)
    QuizDefResult getQuizDef(String quizId) {
        assert quizId
        QuizDefWithDescription quizDefWithDescription = quizDefWithDescRepo.findByQuizIdIgnoreCase(quizId)
        return convert(quizDefWithDescription)
    }

    @Profile
    private UserQuizAttempt findUserQuizAttempt(int quizAttemptId) {
        Optional<UserQuizAttempt> optionalUserQuizAttempt = userQuizAttemptRepo.findById(quizAttemptId)
        if (!optionalUserQuizAttempt.isPresent()) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] does not exist", ErrorCode.BadParam)
        }
        return optionalUserQuizAttempt.get()
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
    QuizDefResult copyQuiz(String originalQuizId, String newQuizId, QuizDefRequest quizDefRequest, String userIdParam = null) {
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

        quizDefWithDescription = new QuizDefWithDescription(quizId: newQuizId, name: quizDefRequest.name,
                description: quizDefRequest.description, type: QuizDefParent.QuizType.valueOf(quizDefRequest.type))
        log.debug("Created quiz [{}]", quizDefWithDescription)

        String userId = userIdParam ?: userInfoService.getCurrentUserId()

        if (!userInfoService.isCurrentUserASuperDuperUser()) {
            createdResourceLimitsValidator.validateNumQuizDefsCreated(userId)
        }

        DataIntegrityExceptionHandlers.dataIntegrityViolationExceptionHandler.handle(null, null, quizDefWithDescription.quizId) {
            quizDefWithDescription = quizDefWithDescRepo.save(quizDefWithDescription)
        }

        QuizQuestionsResult originalQuestions = getQuestionDefs(originalQuizId)
        originalQuestions.questions.forEach(question -> {
            List<QuizAnswerDefResult> answers = question.answers
            QuizQuestionDefRequest newQuestion = new QuizQuestionDefRequest()
            newQuestion.question = question.question
            newQuestion.questionType = question.questionType
            List<QuizAnswerDefRequest> newAnswers = answers.collect( it -> {
                return new QuizAnswerDefRequest(answer: it.answer, isCorrect: it.isCorrect)
            })
            newQuestion.answers = newAnswers
            saveQuestion(newQuizId, newQuestion)
        })

        log.debug("Saved [{}]", quizDefWithDescription)

        attachmentService.updateAttachmentsFoundInMarkdown(quizDefRequest.description, null, newQuizId, null)

        accessSettingsStorageService.addQuizDefUserRole(userId, newQuizId, RoleName.ROLE_QUIZ_ADMIN)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Create,
                item: DashboardItem.Quiz,
                actionAttributes: quizDefWithDescription,
                itemId: quizDefWithDescription.quizId,
                itemRefId: quizDefWithDescription.id,
                quizId: quizDefWithDescription.quizId,
        ))

        QuizDef updatedDef = quizDefRepo.findByQuizIdIgnoreCase(quizDefWithDescription.quizId)
        return convert(updatedDef)
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
        final boolean isEdit = quizDefWithDescription
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

            attachmentService.updateAttachmentsFoundInMarkdown(quizDefRequest.description, null, newQuizId, null)

            accessSettingsStorageService.addQuizDefUserRole(userId, newQuizId, RoleName.ROLE_QUIZ_ADMIN)
        }
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: isEdit ? DashboardAction.Edit : DashboardAction.Create,
                item: DashboardItem.Quiz,
                actionAttributes: quizDefWithDescription,
                itemId: quizDefWithDescription.quizId,
                itemRefId: quizDefWithDescription.id,
                quizId: quizDefWithDescription.quizId,
        ))

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

        updateQuizSetting(quizDef.id, quizDef.quizId, QuizSettings.MinNumQuestionsToPass.setting)
        updateQuizSetting(quizDef.id, quizDef.quizId, QuizSettings.QuizLength.setting)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete,
                item: DashboardItem.Question,
                itemId: quizDef.quizId,
                quizId: quizDef.quizId,
                actionAttributes: [
                        question: quizQuestionDef.question?.toString(),
                        questionType: quizQuestionDef.type?.toString()
                ],
        ))

        quizAnswerRepo.delete(quizQuestionDef)
    }

    void updateQuizSetting(Integer quizId, String quizDefQuizId, String setting) {
        QuizSetting retrievedSetting = quizSettingsRepo.findBySettingAndQuizRefId(setting, quizId)
        if (retrievedSetting) {
            Integer quizLength = Integer.valueOf(retrievedSetting.value)
            int numTotalQuestions = quizQuestionRepo.countByQuizId(quizDefQuizId) - 1
            if (numTotalQuestions == 0) {
                quizSettingsRepo.delete(retrievedSetting);
            } else if (numTotalQuestions < quizLength) {
                retrievedSetting.value = "${numTotalQuestions}"
                quizSettingsRepo.save(retrievedSetting)
            }
        }
    }

    @Transactional()
    QuizQuestionDefResult saveQuestion(String quizId, QuizQuestionDefRequest questionDefRequest, Integer existingQuestionId = null) {
        QuizDef quizDef = findQuizDef(quizId)
        validate(quizDef, questionDefRequest)

        lockingService.lockQuizDef(quizDef.quizId)

        QuizQuestionDef savedQuestion
        List<QuizAnswerDef> savedAnswers

        boolean isEdit = existingQuestionId != null
        if (isEdit) {
            savedQuestion = updateQuizQuestionDef(quizId, existingQuestionId, questionDefRequest)
            savedAnswers = updateQuizQuestionAnswerDefs(savedQuestion, questionDefRequest)
        } else {
            savedQuestion = createQuizQuestionDef(quizDef, questionDefRequest)
            savedAnswers = createQuizQuestionAnswerDefs(questionDefRequest, savedQuestion)
        }

        Map actionAttributes = [
                question: savedQuestion.question,
                questionType: savedQuestion.type,
        ]
        savedAnswers.sort (false, {it.displayOrder}).eachWithIndex { QuizAnswerDef q, Integer index ->
            actionAttributes["Answer${index+1}:text"] = q.answer
            actionAttributes["Answer${index+1}:isCorrectAnswer"] = q.isCorrectAnswer
        }
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: isEdit ? DashboardAction.Edit : DashboardAction.Create,
                item: DashboardItem.Question,
                itemId: quizDef.quizId,
                quizId: quizDef.quizId,
                actionAttributes: actionAttributes
        ))

        return convert(savedQuestion, savedAnswers)
    }

    @Profile
    private List<QuizAnswerDef> createQuizQuestionAnswerDefs(QuizQuestionDefRequest questionDefRequest, QuizQuestionDef savedQuestion) {
        List<QuizAnswerDef> answerDefs
        boolean isTextInputQuestion = questionDefRequest.questionType == QuizQuestionType.TextInput
        boolean isRatingsQuestion = questionDefRequest.questionType == QuizQuestionType.Rating
        if (isTextInputQuestion) {
            answerDefs = [
                    new QuizAnswerDef(
                            quizId: savedQuestion.quizId,
                            questionRefId: savedQuestion.id,
                            answer: null,
                            isCorrectAnswer: false,
                            displayOrder: 1,
                    )
            ]
        } else if (isRatingsQuestion) {
            answerDefs = (1..questionDefRequest.questionScale).collect { it ->
                new QuizAnswerDef(
                        quizId: savedQuestion.quizId,
                        questionRefId: savedQuestion.id,
                        answer: it,
                        isCorrectAnswer: false,
                        displayOrder: it
                )
            }
        } else {
            answerDefs = questionDefRequest.answers.withIndex().collect { QuizAnswerDefRequest answerDefRequest, int index ->
                new QuizAnswerDef(
                        quizId: savedQuestion.quizId,
                        questionRefId: savedQuestion.id,
                        answer: answerDefRequest.answer,
                        isCorrectAnswer: answerDefRequest.isCorrect,
                        displayOrder: index + 1,
                )
            }
        }
        List<QuizAnswerDef> savedAnswers = quizAnswerRepo.saveAllAndFlush(answerDefs)
        return savedAnswers
    }

    @Profile
    private  List<QuizAnswerDef> updateQuizQuestionAnswerDefs(QuizQuestionDef savedQuestion, QuizQuestionDefRequest questionDefRequest) {
        boolean isTextInputQuestion = questionDefRequest.questionType == QuizQuestionType.TextInput
        boolean isRating = questionDefRequest.questionType == QuizQuestionType.Rating
        List<QuizAnswerDef> savedAnswers
        List<QuizAnswerDef> existingAnswerDefs = quizAnswerRepo.findAllByQuestionRefId(savedQuestion.id).sort { it.displayOrder }
        if (isTextInputQuestion) {
            QuizAnswerDef qToUpdate = existingAnswerDefs[0]
            qToUpdate.answer = null
            qToUpdate.isCorrectAnswer = false
            savedAnswers = [quizAnswerRepo.saveAndFlush(qToUpdate)]
            if (existingAnswerDefs.size() > 1) {
                List<Integer> idsToRemove = existingAnswerDefs.subList(1, existingAnswerDefs.size()).collect { it.id }
                quizAnswerRepo.deleteAllById(idsToRemove)
            }
        } else if (isRating) {
            if (existingAnswerDefs.size() > 0) {
                List<Integer> idsToRemove = existingAnswerDefs.collect { it.id }
                quizAnswerRepo.deleteAllById(idsToRemove)
            }
            List<QuizAnswerDef> answerDefs = (1..questionDefRequest.questionScale).collect { it ->
                new QuizAnswerDef(
                        quizId: savedQuestion.quizId,
                        questionRefId: savedQuestion.id,
                        answer: it,
                        isCorrectAnswer: false,
                        displayOrder: it
                )
            }
            savedAnswers = quizAnswerRepo.saveAllAndFlush(answerDefs)
        } else {
            List<QuizAnswerDef> answerDefs = questionDefRequest.answers.withIndex().collect { QuizAnswerDefRequest answerDefRequest, Integer index ->
                QuizAnswerDef answerDef = existingAnswerDefs.find { it.id == answerDefRequest.id }
                if (answerDef) {
                    answerDef.answer = answerDefRequest.answer
                    answerDef.isCorrectAnswer = answerDefRequest.isCorrect
                    answerDef.displayOrder = index + 1
                } else {
                    answerDef = new QuizAnswerDef(
                            quizId: savedQuestion.quizId,
                            questionRefId: savedQuestion.id,
                            answer: answerDefRequest.answer,
                            isCorrectAnswer: answerDefRequest.isCorrect,
                            displayOrder: index + 1,
                    )
                }
                return answerDef
            }
            List<Integer> idsToRemove = existingAnswerDefs.findAll { QuizAnswerDef existingQ ->
                !answerDefs.find { QuizAnswerDef newQ -> existingQ.id == newQ.id }
            }.collect { it.id }
            if (idsToRemove) {
                quizAnswerRepo.deleteAllById(idsToRemove)
            }
            savedAnswers = quizAnswerRepo.saveAllAndFlush(answerDefs)
        }
        return savedAnswers
    }

    @Profile
    private QuizQuestionDef createQuizQuestionDef(QuizDef quizDef, QuizQuestionDefRequest questionDefRequest) {
        Integer maxExistingDisplayOrder = quizQuestionRepo.getMaxQuestionDisplayOrderByQuizId(quizDef.quizId)
        int displayOrder = maxExistingDisplayOrder != null ? maxExistingDisplayOrder + 1 : 0

        QuizQuestionDef questionDef = new QuizQuestionDef(
                quizId: quizDef.quizId,
                question: InputSanitizer.sanitize(questionDefRequest.question),
                type: questionDefRequest.questionType,
                displayOrder: displayOrder,
        )
        QuizQuestionDef savedQuestion = quizQuestionRepo.saveAndFlush(questionDef)
        return  savedQuestion
    }

    @Profile
    private QuizQuestionDef updateQuizQuestionDef(String quizId, int existingQuestionId, QuizQuestionDefRequest questionDefRequest) {
        QuizQuestionDef existing = getQuestingDef(quizId, existingQuestionId)
        existing.question = InputSanitizer.sanitize(questionDefRequest.question)
        existing.type = questionDefRequest.questionType
        QuizQuestionDef savedQuestion = quizQuestionRepo.saveAndFlush(existing)
        return savedQuestion
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
    @Profile
    QuizQuestionDefResult getQuestionDef(String quizId, Integer questionId) {
        QuizQuestionDef questionDef = getQuestingDef(quizId, questionId)
        List<QuizAnswerDef> answerDefs = quizAnswerRepo.findAllByQuestionRefId(questionDef.id)
        QuizQuestionDefResult res = convert(questionDef, answerDefs)
        return res
    }

    @Profile
    private QuizQuestionDef getQuestingDef(String quizId, int questionId) {
        QuizDef quizDef = findQuizDef(quizId)
        Optional<QuizQuestionDef> questionRes = quizQuestionRepo.findById(questionId)
        if (questionRes.isEmpty()) {
            throw new SkillQuizException("Provided question id [${questionId}] does not exist", quizId);
        }
        QuizQuestionDef questionDef = questionRes.get()
        if (quizDef.quizId != questionDef.quizId) {
            throw new SkillQuizException("Provided question id [${questionId}] does not exist in quiz [${quizId}]", quizId)
        }
        return questionDef
    }

    @Transactional
    TableResult getQuizRuns(String quizId, String query, PageRequest pageRequest) {
        long totalCount = userQuizAttemptRepo.countByQuizId(quizId)
        if (totalCount == 0) {
            return new TableResult(totalCount: totalCount, count: 0, data: [])
        }

        query = query ?: ''
        List<QuizRun> quizRuns = userQuizAttemptRepo.findQuizRuns(quizId, query, usersTableAdditionalUserTagKey, pageRequest)
        int count = totalCount > pageRequest.pageSize ? totalCount : quizRuns.size()
        if (totalCount > pageRequest.pageSize && query) {
            count = userQuizAttemptRepo.countQuizRuns(quizId, query)
        }

        return new TableResult(totalCount: totalCount, data: quizRuns, count: count)
    }

    @Transactional
    TableResult getUserQuestionAnswers(String quizId, Integer answerDefId, PageRequest pageRequest) {
        Optional<QuizAnswerDef> optionalQuizAnswerDef = quizAnswerRepo.findById(answerDefId)
        if (!optionalQuizAnswerDef.isPresent()) {
            throw new SkillQuizException("Provided answer id [${answerDefId}] does not exist", ErrorCode.BadParam)
        }
        QuizAnswerDef quizAnswerDef = optionalQuizAnswerDef.get()
        if (quizAnswerDef.quizId != quizId) {
            throw new SkillQuizException("Provided answer id [${answerDefId}] does not belong to quiz [${quizId}]", ErrorCode.BadParam)
        }

        List<UserQuizAnswer> answerAttempts = userQuizAnswerAttemptRepo.findUserAnswers(answerDefId, usersTableAdditionalUserTagKey, pageRequest)
        int count = answerAttempts.size()
        // pages are 0 based
        if (pageRequest.pageNumber > 0 || count >= pageRequest.pageSize) {
            count = userQuizAnswerAttemptRepo.countByQuizAnswerDefinitionRefId(answerDefId)
        }
        return new TableResult(totalCount: count, data: answerAttempts, count: count)
    }

    @Transactional
    void deleteQuizRun(String quizId, Integer attemptId) {
        Optional<UserQuizAttempt> optionalUserQuizAttempt = userQuizAttemptRepo.findById(attemptId)
        if (!optionalUserQuizAttempt.isPresent()) {
            throw new SkillQuizException("Quiz attempt with id [${attemptId}] does not exist", ErrorCode.BadParam)
        }
        UserQuizAttempt userQuizAttempt = optionalUserQuizAttempt.get()
        QuizDef quizDef1 = findQuizDef(quizId)
        if (quizDef1.id != userQuizAttempt.quizDefinitionRefId) {
            throw new SkillQuizException("Provided attempt id [${attemptId}] does not belong to quiz [${quizId}]", quizId, ErrorCode.BadParam)
        }

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete, item: DashboardItem.QuizAttempt,
                itemId: quizDef1.quizId, quizId: quizDef1.quizId,
                actionAttributes: [
                        attemptStarted: userQuizAttempt.started,
                        attemptCompleted: userQuizAttempt.completed,
                        status: userQuizAttempt.status,
                ]
        ))
        userQuizAttemptRepo.delete(userQuizAttempt)
        log.info("Removed [{}]", userQuizAttempt.toString())
        if (userQuizAttempt.status == UserQuizAttempt.QuizAttemptStatus.PASSED) {
            List<QuizToSkillDefRepo.ProjectIdAndSkillId> linkedSkills = quizToSkillDefRepo.getSkillsForQuiz(quizDef1.id)
            linkedSkills.each {
                List<UserPerformedSkill> userPerformedSkills = userPerformedSkillRepo.findAllBySkillRefIdAndUserId(it.skillRefId, userQuizAttempt.userId)
                if (userPerformedSkills) {
                    // since this is a quiz-based skill should never be more than 1
                    userPerformedSkills.each {
                        skillEventAdminService.removePerformedSkillEvent(it)
                    }
                }
            }
        }
    }

    @Transactional
    QuizMetrics getMetrics(String quizId) {
        QuizDef quiz = findQuizDef(quizId)
        boolean isSurvey = quiz.type == QuizDefParent.QuizType.Survey
        List<UserQuizAttemptRepo.QuizCounts> quizCounts = userQuizAttemptRepo.getUserQuizAttemptCounts(quizId)

        int totalNumAttempts = quizCounts ? quizCounts.collect { it.getNumAttempts() }.sum() : 0
        UserQuizAttemptRepo.QuizCounts passedQuizCounts = quizCounts.find{
            UserQuizAttempt.QuizAttemptStatus.PASSED == it.getStatus()
        }
        UserQuizAttemptRepo.QuizCounts failedQuizCounts = quizCounts.find {
            UserQuizAttempt.QuizAttemptStatus.FAILED == it.getStatus()
        }
        int numAttemptsPassed = passedQuizCounts ? passedQuizCounts.getNumAttempts() : 0
        int numAttemptsFailed = failedQuizCounts ? failedQuizCounts.getNumAttempts() : 0

        Integer totalNumDistinctUsers = isSurvey ? totalNumAttempts : userQuizAttemptRepo.getDistinctNumUsersByQuizId(quizId)
        int numDistinctUsersPassed = passedQuizCounts ? passedQuizCounts.getNumDistinctUsers() : 0
        int numDistinctUsersFailed = failedQuizCounts ? failedQuizCounts.getNumDistinctUsers() : 0

        Integer averageRuntimeInMs = userQuizAttemptRepo.getAverageMsRuntimeForQuiz(quizId)?.intValue()

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
                        int numAnswered = answerStatusCounts ? answerStatusCounts.collect{ it.getCount() }.sum() : 0
                        new QuizAnswerMetricsResult(
                                id: quizAnswerDefResult.id,
                                answer: quizAnswerDefResult.answer,
                                isCorrect: isSurvey ? true : quizAnswerDefResult.isCorrect,
                                numAnswered: numAnswered,
                                numAnsweredCorrect: isSurvey ? numAnswered : (answerCorrectCount?.getCount() ?: 0),
                                numAnsweredWrong: isSurvey ? 0 : (answerWrongCount?.getCount() ?: 0),
                        )
                    }
            )
        }

        return new QuizMetrics(
                quizType: quiz.type,
                numTaken: totalNumAttempts,
                numPassed: numAttemptsPassed,
                numFailed: numAttemptsFailed,
                numTakenDistinctUsers: totalNumDistinctUsers,
                numPassedDistinctUsers: numDistinctUsersPassed,
                numFailedDistinctUsers: numDistinctUsersFailed,
                avgAttemptRuntimeInMs: averageRuntimeInMs,
                questions: questions,
        )
    }

    private QuizQuestionDefResult convert(QuizQuestionDef savedQuestion, List<QuizAnswerDef> savedAnswers) {
        new QuizQuestionDefResult(
                id: savedQuestion.id,
                question: InputSanitizer.unsanitizeForMarkdown(savedQuestion.question),
                questionType: savedQuestion.type,
                answers: savedAnswers.collect { convert (it)}.sort { it.displayOrder},
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

    @Transactional
    UserGradedQuizQuestionsResult getUsersGradedResult(String quizId, Integer quizAttemptId) {
        QuizDef quizDef = findQuizDef(quizId)
        boolean isSurvey = quizDef.type == QuizDefParent.QuizType.Survey
        UserQuizAttempt userQuizAttempt = findUserQuizAttempt(quizAttemptId)
        if (userQuizAttempt.quizDefinitionRefId != quizDef.id) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] is not for [${quizId}] quiz", ErrorCode.BadParam)
        }
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userQuizAttempt.userId)
        List<UserQuizAnswerAttemptRepo.AnswerIdAndAnswerText> alreadySelected = userQuizAnswerAttemptRepo.getSelectedAnswerIdsAndText(userQuizAttempt.id)

        List<QuizQuestionDef> dbQuestionDefs = quizQuestionRepo.findAllByQuizIdIgnoreCase(quizId)
        List<QuizAnswerDef> dbAnswersDef = quizAnswerRepo.findAllByQuizIdIgnoreCase(quizId)
        List<UserQuizQuestionAttempt> questionAttempts = userQuizQuestionAttemptRepo.findAllByUserQuizAttemptRefId(userQuizAttempt.id)
        Map<Integer, List<QuizAnswerDef>> byQuestionId = dbAnswersDef.groupBy {it.questionRefId }

        List<QuizQuestionDefResult> questions = dbQuestionDefs
                .sort { it.displayOrder }
                .collect { QuizQuestionDef questionDef ->
                    List<QuizAnswerDef> quizAnswerDefs = byQuestionId[questionDef.id]

                    boolean isTextInput = questionDef.type == QuizQuestionType.TextInput
                    boolean isRating = questionDef.type == QuizQuestionType.Rating
                    List<UserGradedQuizAnswerResult> answers = quizAnswerDefs.collect { QuizAnswerDef answerDef ->
                        UserQuizAnswerAttemptRepo.AnswerIdAndAnswerText foundSelected = alreadySelected.find { it.answerId == answerDef.id }
                        return new UserGradedQuizAnswerResult(
                                id: answerDef.id,
                                answer: isTextInput ? foundSelected?.answerText : answerDef.answer,
                                isConfiguredCorrect: Boolean.valueOf(answerDef.isCorrectAnswer),
                                isSelected: foundSelected != null,
                        )
                    }

                    UserQuizQuestionAttempt userQuizQuestionAttempt = questionAttempts.find { it.quizQuestionDefinitionRefId == questionDef.id}
                    boolean isCorrect
                    if (userQuizQuestionAttempt?.status != null) {
                        isCorrect = userQuizQuestionAttempt.status == UserQuizQuestionAttempt.QuizQuestionStatus.CORRECT
                    } else {
                        isCorrect = isSurvey ? true : !answers.find { it.isConfiguredCorrect != it.isSelected}
                    }
                    return new UserGradedQuizQuestionResult(
                            id: questionDef.id,
                            question: InputSanitizer.unsanitizeForMarkdown(questionDef.question),
                            questionType: questionDef.type,
                            answers: answers,
                            isCorrect: isCorrect,
                    )
                }

        Integer numQuestionsToPass = userQuizAttempt.numQuestionsToPass
        if (userQuizAttempt.status == UserQuizAttempt.QuizAttemptStatus.INPROGRESS) {
            Integer confNumQuestionsPass = getMinNumQuestionsToPassSetting(quizDef.id)
            if (confNumQuestionsPass > 0) {
                numQuestionsToPass = confNumQuestionsPass
            }
        }

        String userTag
        if (StringUtils.isNotBlank(usersTableAdditionalUserTagKey)) {
            List<UserTag> userTags = userTagRepo.findAllByUserIdAndKeyIn(userAttrs.userId, [usersTableAdditionalUserTagKey.toLowerCase()].toSet())
            userTag = userTags ? userTags.first()?.value : null
        }

        return new UserGradedQuizQuestionsResult(quizType: quizDef.type,
                userId: userAttrs.userId,
                userIdForDisplay: userAttrs.userIdForDisplay,
                status: userQuizAttempt.status,
                questions: questions,
                numQuestionsToPass: numQuestionsToPass,
                started: userQuizAttempt.started,
                completed: userQuizAttempt.completed,
                userTag: userTag,
        )
    }

    @Profile
    private Integer getMinNumQuestionsToPassSetting(Integer quizRefId) {
        QuizSetting quizSetting = quizSettingsRepo.findBySettingAndQuizRefId(QuizSettings.MinNumQuestionsToPass.setting, quizRefId)
        return quizSetting ? Integer.valueOf(quizSetting.value) : -1
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

        propsBasedValidator.quizValidationMaxStrLength(PublicProps.UiProp.descriptionMaxLength, "Question", questionDefRequest.question, quizDef.quizId)
        int numQuestions = quizQuestionRepo.countByQuizId(quizDef.quizId)
        propsBasedValidator.quizValidationMaxIntValue(PublicProps.UiProp.maxQuestionsPerQuiz, "Number of Questions", numQuestions + 1, quizDef.quizId)
        CustomValidationResult customValidationResult = customValidator.validateDescription(questionDefRequest.question)
        if (!customValidationResult.valid) {
            throw new SkillQuizException("Question: ${customValidationResult.msg}", quizId, ErrorCode.BadParam)
        }

        if (questionDefRequest.questionType != QuizQuestionType.TextInput && questionDefRequest.questionType != QuizQuestionType.Rating) {
            QuizValidator.isNotNull(questionDefRequest.answers, "answers", quizId)
            QuizValidator.isTrue(questionDefRequest.answers.size() >= 2, "Must have at least 2 answers", quizId)
            questionDefRequest.answers.each {
                QuizValidator.isNotBlank(it.answer, "answers.answer", quizId, true)
                propsBasedValidator.quizValidationMaxStrLength(PublicProps.UiProp.maxQuizTextAnswerLength, "Answer", it.answer, quizDef.quizId)
            }
        }
        if (quizDef.type == QuizDefParent.QuizType.Quiz) {
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
            if (questionDefRequest.questionType == QuizQuestionType.TextInput || questionDefRequest.questionType == QuizQuestionType.Rating) {
                if (questionDefRequest.answers) {
                    throw new SkillQuizException("Questions with type of ${QuizQuestionType.TextInput} must not provide an answer]", quizId, ErrorCode.BadParam)
                }
            }
        }

    }

    @Transactional(readOnly = true)
    List<LabelCountItem> getUserTagCounts(String quizId, String userTag) {
        PageRequest pageRequest = PageRequest.of(0, 20, DESC, "tagCount")
        List<UserQuizAttemptRepo.TagValueCount> tagValueCounts = userQuizAttemptRepo.getUserTagCounts(quizId, userTag, pageRequest)
        return tagValueCounts?.collect { new LabelCountItem(value: it.tagValue, count: it.tagCount)}
    }

    @Transactional(readOnly = true)
    List<TimestampCountItem> getUsageOverTime(String quizId) {
        List<UserQuizAttemptRepo.DateCount> usageOverTime = userQuizAttemptRepo.getUsageOverTime(quizId)
        if (!usageOverTime) {
            return []
        }

        Date previousDate
        List<TimestampCountItem> res = usageOverTime.collect {
            List<TimestampCountItem> res = []
            Date currentDate = new Date(it.dateVal.getTime())
            if (previousDate) {
                List<DayCountItem> items = ZeroFillDayCountItemUtil.zeroFillDailyGaps(currentDate, previousDate, false)
                if (items) {
                    List<TimestampCountItem> timestampCountItems = items.collect {
                        new TimestampCountItem(value: it.day.time, count: it.count)
                    }.sort { it.value }
                    res.addAll(timestampCountItems)
                }
            }

            res.add(new TimestampCountItem(value: currentDate.getTime(), count: it.count))
            previousDate = currentDate
            return res
        }.flatten()

        Date now = StartDateUtil.computeStartDate(new Date(), EventType.DAILY)
        List<DayCountItem> items = ZeroFillDayCountItemUtil.zeroFillDailyGaps(now, previousDate, true)
        if (items) {
            res.addAll(items.collect {new TimestampCountItem( value: it.day.time, count: it.count)}.sort { it.value})
        }

        return res
    }

    @Transactional()
    void deleteQuiz(String quizId) {
        QuizDef quizDef = findQuizDef(quizId)
        if (quizToSkillDefRepo.countByQuizRefId(quizDef.id) > 0) {
            throw new SkillQuizException("Not allowed to remove quiz when assigned to at least 1 skill", quizDef.quizId, ErrorCode.InternalError)
        }
        int numRemoved = quizDefRepo.deleteByQuizIdIgnoreCase(quizDef.quizId)
        log.debug("Deleted project with id [{}]. Removed [{}] record", quizId, numRemoved)
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete,
                item: DashboardItem.Quiz,
                itemId: quizDef.quizId,
                quizId: quizDef.quizId,
        ))
    }

    @Transactional()
    Integer countNumSkillsQuizAssignedTo(String quizId) {
        QuizDef quizDef = findQuizDef(quizId)
        return quizToSkillDefRepo.countByQuizRefId(quizDef.id);
    }

    @Transactional()
    List<QuizSkillResult> getSkillsForQuiz(String quizId, userId) {
        QuizDef quizDef = findQuizDef(quizId)
        return quizToSkillDefRepo.getSkillsForQuizWithSubjects(quizDef.id, userId);
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
            propsBasedValidator.quizValidationMaxStrLength(PublicProps.UiProp.descriptionMaxLength, "Quiz Description", quizDefRequest.description, quizId)
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
        result.description = InputSanitizer.unsanitizeForMarkdown(result.description)
        result.displayOrder = 0 // todo
        return result
    }
}
