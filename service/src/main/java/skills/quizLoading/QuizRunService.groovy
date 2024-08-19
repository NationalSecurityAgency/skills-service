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
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.QuizValidator
import skills.controller.exceptions.SkillQuizException
import skills.quizLoading.model.*
import skills.services.CustomValidationResult
import skills.services.CustomValidator
import skills.services.LockingService
import skills.services.events.SkillEventResult
import skills.services.events.SkillEventsService
import skills.services.quiz.QuizQuestionType
import skills.storage.model.*
import skills.storage.repos.*
import skills.utils.InputSanitizer

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

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
    LockingService lockingService

    @Autowired
    CustomValidator validator

    @Autowired
    QuizSettingsRepo quizSettingsRepo

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    SkillEventsService skillEventsService

    @Autowired
    UserQuizQuestionAttemptRepo quizQuestionAttemptRepo

    @Autowired
    UserQuizAnswerAttemptRepo quizAttemptAnswerRepo

    @Autowired
    UserInfoService userInfoService

    @Transactional
    QuizInfo loadQuizInfo(String userId, String quizId) {
        QuizDefWithDescription quizDefWithDesc = quizDefWithDescRepo.findByQuizIdIgnoreCase(quizId)
        if (!quizDefWithDesc) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }

        List<QuizSetting> quizSettings = loadQuizSettings(quizDefWithDesc.id)
        boolean randomizeQuestionsSetting = quizSettings?.find( { it.setting == QuizSettings.RandomizeQuestions.setting })?.value?.toBoolean()
        boolean randomizeAnswersSetting = quizSettings?.find( { it.setting == QuizSettings.RandomizeAnswers.setting })?.value?.toBoolean()
        boolean multipleTakes = quizSettings?.find( { it.setting == QuizSettings.MultipleTakes.setting })?.value?.toBoolean()
        List<QuizQuestionInfo> questions = loadQuizQuestionInfo(quizId, randomizeQuestionsSetting, randomizeAnswersSetting)

        UserQuizAttemptRepo.UserQuizAttemptStats userAttemptsStats =
                quizAttemptRepo.getUserAttemptsStats(userId, quizDefWithDesc.id,
                        UserQuizAttempt.QuizAttemptStatus.INPROGRESS, UserQuizAttempt.QuizAttemptStatus.PASSED)

        Integer numberOfQuestions = quizQuestionRepo.countByQuizId(quizId)

        QuizSetting maxNumAttemptsSetting = quizSettings?.find( { it.setting == QuizSettings.MaxNumAttempts.setting })
        QuizSetting minNumQuestionsToPassSetting = quizSettings?.find( { it.setting == QuizSettings.MinNumQuestionsToPass.setting })
        QuizSetting quizLength = quizSettings?.find( { it.setting == QuizSettings.QuizLength.setting })
        QuizSetting quizTimeLimit = quizSettings?.find( { it.setting == QuizSettings.QuizTimeLimit.setting })
        Integer quizLengthAsInteger = quizLength ? Integer.valueOf(quizLength.value) : 0
        Integer lengthSetting = quizLengthAsInteger > 0 ? quizLengthAsInteger : numberOfQuestions

        return new QuizInfo(
                name: quizDefWithDesc.name,
                description: InputSanitizer.unsanitizeForMarkdown(quizDefWithDesc.description),
                questions: questions.take(lengthSetting),
                quizType: quizDefWithDesc.getType().toString(),
                isAttemptAlreadyInProgress: userAttemptsStats?.getIsAttemptAlreadyInProgress() ?: false,
                userNumPreviousQuizAttempts: userAttemptsStats?.getUserNumPreviousQuizAttempts() ?: 0,
                userQuizPassed: userAttemptsStats?.getUserQuizPassed() ?: false,
                userLastQuizAttemptDate: userAttemptsStats?.getUserLastQuizAttemptCompleted() ?: null,
                maxAttemptsAllowed: maxNumAttemptsSetting ? Integer.valueOf(maxNumAttemptsSetting.value) : -1,
                minNumQuestionsToPass: minNumQuestionsToPassSetting ? Integer.valueOf(minNumQuestionsToPassSetting.value) : -1,
                quizLength: lengthSetting,
                quizTimeLimit: quizTimeLimit ? Integer.valueOf(quizTimeLimit.value) : -1,
                multipleTakes: multipleTakes,
        )
    }

    private List<QuizQuestionInfo> loadQuizQuestionInfo(String quizId, boolean randomizeQuestions = false, boolean randomizeAnswers = false) {
        List<QuizQuestionDef> dbQuestionDefs = quizQuestionRepo.findAllByQuizIdIgnoreCase(quizId)
        List<QuizAnswerDef> dbAnswersDef = quizAnswerRepo.findAllByQuizIdIgnoreCase(quizId)
        Map<Integer, List<QuizAnswerDef>> byQuizId = dbAnswersDef.groupBy { it.questionRefId }

        if(randomizeQuestions) {
            dbQuestionDefs?.shuffle()
        } else {
            dbQuestionDefs?.sort{it.getDisplayOrder()}
        }
        List<QuizQuestionInfo> questions = dbQuestionDefs?.collect {
            List<QuizAnswerDef> quizAnswerDefs = byQuizId[it.id]
            if(randomizeAnswers) {
                quizAnswerDefs?.shuffle()
            }
            new QuizQuestionInfo(
                    id: it.id,
                    question: InputSanitizer.unsanitizeForMarkdown(it.question),
                    questionType: it.type.toString(),
                    canSelectMoreThanOne: quizAnswerDefs?.count({ Boolean.valueOf(it.isCorrectAnswer) }) > 1,
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

    LocalDateTime calculateDeadline(LocalDateTime started, Integer timeLimitInSeconds) {
        LocalDateTime deadlineLocal = started.plusSeconds(timeLimitInSeconds);
        return deadlineLocal
    }

    Boolean hasQuizExpired(LocalDateTime deadline) {
        LocalDateTime currentDate = LocalDateTime.now(ZoneOffset.UTC)
        return currentDate.isAfter(deadline)
    }

    @Transactional
    QuizAttemptStartResult startQuizAttempt(String userId, String quizId) {
        QuizDefWithDescription quizDefWithDesc = quizDefWithDescRepo.findByQuizIdIgnoreCase(quizId)
        List<QuizSetting> quizSettings = loadQuizSettings(quizDefWithDesc.id)
        boolean randomizeQuestionsSetting = quizSettings?.find( { it.setting == QuizSettings.RandomizeQuestions.setting })?.value?.toBoolean()
        boolean randomizeAnswersSetting = quizSettings?.find( { it.setting == QuizSettings.RandomizeAnswers.setting })?.value?.toBoolean()
        Integer quizTimeLimit = quizSettings?.find( { it.setting == QuizSettings.QuizTimeLimit.setting })?.value?.toInteger()
        QuizSetting quizLength = quizSettings?.find( { it.setting == QuizSettings.QuizLength.setting })
        List<QuizQuestionInfo> questions = loadQuizQuestionInfo(quizId, randomizeQuestionsSetting, randomizeAnswersSetting)
        List<QuizQuestionInfo> questionsForQuiz = []
        Integer quizLengthAsInteger = quizLength ? Integer.valueOf(quizLength.value) : 0
        Integer lengthSetting = quizLengthAsInteger > 0 ? quizLengthAsInteger : questions.size()

        log.info("Starting [{}] quiz attempt for [{}] user", quizId, userId)
        UserQuizAttempt inProgressAttempt = quizAttemptRepo.getByUserIdAndQuizIdAndState(userId, quizId, UserQuizAttempt.QuizAttemptStatus.INPROGRESS)

        if (inProgressAttempt) {
            Date deadline = null
            if (quizTimeLimit > 0) {
                LocalDateTime deadlineLocal = calculateDeadline(inProgressAttempt.started.toLocalDateTime(), quizTimeLimit)
                deadline = deadlineLocal.toDate()
                if (hasQuizExpired(deadlineLocal)) {
                    log.info("Deadline has passed for user [{}] on quiz [{}], failing quiz attempt", userId, quizId)
                    failQuizAttempt(userId, quizId, inProgressAttempt.id)
                    return new QuizAttemptStartResult(
                            id: inProgressAttempt.id,
                            inProgressAlready: true,
                            selectedAnswerIds: [],
                            enteredText: null,
                            questions: [],
                            existingAttemptFailed: true,
                            deadline: deadline
                    )
                }
            }

            List<UserQuizAnswerAttemptRepo.AnswerIdAndAnswerText> alreadySelected = quizAttemptAnswerRepo.getSelectedAnswerIdsAndText(inProgressAttempt.id)

            List<Integer> selectedAnswerIds = alreadySelected?.findAll({!it.getAnswerText()}).collect { it.getAnswerId()}
            List<QuizAttemptStartResult.AnswerIdAndEnteredText> enteredText = alreadySelected?.findAll({it.getAnswerText()}).collect {
                new QuizAttemptStartResult.AnswerIdAndEnteredText(answerId: it.getAnswerId(), answerText: it.getAnswerText())
            }

            List<UserQuizQuestionAttempt> attemptQuestion = quizQuestionAttemptRepo.findAllByUserQuizAttemptRefId(inProgressAttempt.id)
            attemptQuestion.each { attempt ->
                def selectedQuestion = questions.find { it ->
                    it.id == attempt.quizQuestionDefinitionRefId && attempt.userId == userId
                }
                if (selectedQuestion) {
                    selectedQuestion.displayOrder = attempt.displayOrder
                    questionsForQuiz.push(selectedQuestion)
                }
            }

            log.info("Continued executing quiz attempt {}", inProgressAttempt)
            return new QuizAttemptStartResult(
                    id: inProgressAttempt.id,
                    inProgressAlready: true,
                    selectedAnswerIds: selectedAnswerIds ?: [],
                    enteredText: enteredText,
                    questions: questionsForQuiz.sort{ it.displayOrder },
                    deadline: deadline
            )
        }

        questionsForQuiz = questions.take(lengthSetting).eachWithIndex { it, index ->
            it.displayOrder = index + 1
        }

        QuizDef quizDef = getQuizDef(quizId)
        validateQuizAttempts(quizDef, userId, quizId)
        int numQuestions = quizQuestionRepo.countByQuizId(quizDef.quizId)
        QuizValidator.isTrue(numQuestions > 0, "Must have at least 1 question declared in order to start.", quizDef.quizId)

        Integer minNumQuestionsToPassConf = getMinNumQuestionsToPassSetting(quizDef.id)
        Integer minNumQuestionsToPass = minNumQuestionsToPassConf > 0 ? minNumQuestionsToPassConf : numQuestions;

        LocalDateTime deadline = null
        LocalDateTime start = LocalDateTime.now(ZoneOffset.UTC)
        if(quizTimeLimit > 0) {
            deadline = start.plusSeconds(quizTimeLimit)
        }

        UserQuizAttempt userQuizAttempt = new UserQuizAttempt(
                userId: userId,
                quizDefinitionRefId: quizDef.id,
                status: UserQuizAttempt.QuizAttemptStatus.INPROGRESS,
                numQuestionsToPass: minNumQuestionsToPass,
                started: start.toDate())
        UserQuizAttempt savedAttempt = quizAttemptRepo.saveAndFlush(userQuizAttempt)
        log.info("Started new quiz attempt {}", savedAttempt)

        questionsForQuiz.each{ question ->
            UserQuizQuestionAttempt userQuizQuestionAttempt = new UserQuizQuestionAttempt(
                    userQuizAttemptRefId: savedAttempt.id,
                    quizQuestionDefinitionRefId: question.id,
                    userId: userId,
                    status: UserQuizQuestionAttempt.QuizQuestionStatus.INCOMPLETE,
                    displayOrder: question.displayOrder
            )
            quizQuestionAttemptRepo.save(userQuizQuestionAttempt)
        }

        return new QuizAttemptStartResult(id: savedAttempt.id, questions: questionsForQuiz.sort{ it.displayOrder }, deadline: deadline?.toDate())
    }

    @Profile
    private void validateQuizAttempts(QuizDef quizDef, String userId, String quizId) {
        UserQuizAttemptRepo.UserQuizAttemptStats userAttemptsStats = quizAttemptRepo.getUserAttemptsStats(userId, quizDef.id,
                UserQuizAttempt.QuizAttemptStatus.INPROGRESS, UserQuizAttempt.QuizAttemptStatus.PASSED)
        Integer numCurrentAttempts = userAttemptsStats?.getUserNumPreviousQuizAttempts() ?: 0
        boolean allowMultipleTakes = allowMultipleTakes(quizDef.id)
        if (quizDef.type == QuizDefParent.QuizType.Survey) {
            if (numCurrentAttempts > 0) {
                throw new SkillQuizException("User [${userId}] has already taken this survey", quizId, ErrorCode.BadParam)
            }
        } else {
            if (userAttemptsStats?.getUserQuizPassed() && !allowMultipleTakes) {
                throw new SkillQuizException("User [${userId}] already took and passed this quiz.", quizId, ErrorCode.UserQuizAttemptsExhausted)
            }

            int numConfiguredAttempts = getMaxQuizAttemptsSetting(quizDef.id)
            // anything 0 or below is considered to be unlimited attempts
            if (numConfiguredAttempts > 0 && numCurrentAttempts >= numConfiguredAttempts) {
                throw new SkillQuizException("User [${userId}] exhausted [${numConfiguredAttempts}] available attempts for this quiz.", quizId, ErrorCode.UserQuizAttemptsExhausted)
            }
        }
    }

    @Profile
    private List<QuizSetting> loadQuizSettings(Integer quizRefId) {
        return quizSettingsRepo.findAllByQuizRefIdAndSettingIn(quizRefId, [QuizSettings.MaxNumAttempts.setting, QuizSettings.MinNumQuestionsToPass.setting, QuizSettings.RandomizeQuestions.setting, QuizSettings.RandomizeAnswers.setting, QuizSettings.QuizLength.setting, QuizSettings.QuizTimeLimit.setting, QuizSettings.MultipleTakes.setting])
    }

    @Profile
    private Integer getMaxQuizAttemptsSetting(Integer quizRefId) {
        return getQuizSettingAsInteger(quizRefId, QuizSettings.MaxNumAttempts.setting)
    }
    @Profile
    private Integer getMinNumQuestionsToPassSetting(Integer quizRefId) {
        return getQuizSettingAsInteger(quizRefId, QuizSettings.MinNumQuestionsToPass.setting)
    }
    @Profile
    private Integer getQuizLength(Integer quizRefId) {
        return getQuizSettingAsInteger(quizRefId, QuizSettings.QuizLength.setting)
    }
    @Profile
    private boolean allowMultipleTakes(Integer quizRefId) {
        return getQuizSettingAsBoolean(quizRefId, QuizSettings.MultipleTakes.setting)
    }
    @Profile
    private Integer getQuizSettingAsInteger(Integer quizRefId, String setting) {
        QuizSetting quizSetting = quizSettingsRepo.findBySettingAndQuizRefId(setting, quizRefId)
        return quizSetting ? Integer.valueOf(quizSetting.value) : -1
    }
    @Profile
    private boolean getQuizSettingAsBoolean(Integer quizRefId, String setting) {
        QuizSetting quizSetting = quizSettingsRepo.findBySettingAndQuizRefId(setting, quizRefId)
        return quizSetting?.value?.toBoolean()
    }

    boolean shouldQuizBeFailed(UserQuizAttempt attempt) {
        List<QuizSetting> quizSettings = loadQuizSettings(attempt.quizDefinitionRefId)
        Integer quizTimeLimit = quizSettings?.find( { it.setting == QuizSettings.QuizTimeLimit.setting })?.value?.toInteger()

        if (quizTimeLimit > 0) {
            LocalDateTime deadlineLocal = calculateDeadline(attempt.started.toLocalDateTime(), quizTimeLimit)
            if (hasQuizExpired(deadlineLocal)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    void reportQuestionAnswer(String userId, String quizId, Integer quizAttemptId, Integer answerDefId, QuizReportAnswerReq quizReportAnswerReq) {
        lockingService.lockUserQuizAttempt(quizAttemptId)

        if (!quizAttemptRepo.existsByUserIdAndIdAndQuizId(userId, quizAttemptId, quizId)) {
            throw new SkillQuizException("Provided attempt id [${quizAttemptId}] does not exist for [${userId}] user and [${quizId}] quiz", ErrorCode.BadParam)
        }

        if (quizAttemptRepo.checkQuizStatus(userId, quizAttemptId, quizId, UserQuizAttempt.QuizAttemptStatus.FAILED)) {
            throw new SkillQuizException("Provided attempt id [${quizAttemptId}], which corresponds to a failed quiz", ErrorCode.BadParam)
        }

        UserQuizAttempt inProgressAttempt = quizAttemptRepo.getByUserIdAndQuizIdAndState(userId, quizId, UserQuizAttempt.QuizAttemptStatus.INPROGRESS)
        if (inProgressAttempt) {
            if(shouldQuizBeFailed(inProgressAttempt)) {
                failQuizAttempt(userId, quizId, quizAttemptId)
                throw new SkillQuizException("Deadline for [${quizAttemptId}] has expired", ErrorCode.BadParam)
            }
        }

        QuizAnswerDefRepo.AnswerDefPartialInfo answerDefPartialInfo = quizAnswerRepo.getPartialDefByAnswerDefId(answerDefId)
        if (!answerDefPartialInfo) {
            throw new SkillQuizException("Provided answer id [${answerDefId}] does not exist", ErrorCode.BadParam)
        }
        if (answerDefPartialInfo.getQuizId() != quizId) {
            throw new SkillQuizException("Supplied quizId of [${quizId}] does not match answer's quiz id  of [${answerDefPartialInfo.getQuizId()}]", ErrorCode.BadParam)
        }

        if (answerDefPartialInfo.getQuestionType() == QuizQuestionType.TextInput) {
            if (quizReportAnswerReq.isSelected) {
                QuizValidator.isNotBlank(quizReportAnswerReq.getAnswerText(), "answerText", quizId)
                CustomValidationResult customValidationResult = validator.validateDescription(quizReportAnswerReq.getAnswerText())
                if (!customValidationResult.valid) {
                    throw new SkillQuizException("answerText is invalid: ${customValidationResult.msg}", quizId, ErrorCode.BadParam)
                }
            } else {
                QuizValidator.isTrue(StringUtils.isBlank(quizReportAnswerReq.getAnswerText()), "For TextInput type, if isSelected=false then the answer must be null or blank", quizId)
            }
            handleReportingTextInputQuestion(userId, quizAttemptId, answerDefId, quizReportAnswerReq)
        } else {
            handleReportingAChoiceBasedQuestion(userId, quizAttemptId, answerDefId, quizReportAnswerReq, answerDefPartialInfo)
        }
    }

    private void handleReportingTextInputQuestion(String userId, Integer quizAttemptId, Integer answerDefId, QuizReportAnswerReq quizReportAnswerReq) {
        UserQuizAnswerAttempt existingAnswerAttempt = quizAttemptAnswerRepo.findByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefId(quizAttemptId, answerDefId)
        if (existingAnswerAttempt) {
            if (quizReportAnswerReq.isSelected) {
                existingAnswerAttempt.answer = quizReportAnswerReq.getAnswerText()
                quizAttemptAnswerRepo.save(existingAnswerAttempt)
            } else {
                quizAttemptAnswerRepo.delete(existingAnswerAttempt)
            }
        } else if (quizReportAnswerReq.isSelected) {
            UserQuizAnswerAttempt newAnswerAttempt = new UserQuizAnswerAttempt(
                    userQuizAttemptRefId: quizAttemptId,
                    quizAnswerDefinitionRefId: answerDefId,
                    userId: userId,
                    status: UserQuizAnswerAttempt.QuizAnswerStatus.CORRECT,
                    answer: quizReportAnswerReq.getAnswerText(),
            )
            quizAttemptAnswerRepo.save(newAnswerAttempt)
        }
    }

    private void handleReportingAChoiceBasedQuestion(String userId, Integer attemptId, Integer answerDefId, QuizReportAnswerReq quizReportAnswerReq, QuizAnswerDefRepo.AnswerDefPartialInfo answerDefPartialInfo) {
        if (!quizReportAnswerReq.isSelected) {
            quizAttemptAnswerRepo.deleteByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefId(attemptId, answerDefId)
        } else {
            boolean isCorrectChoice = Boolean.valueOf(answerDefPartialInfo.getIsCorrectAnswer())
            boolean answerAttemptAlreadyDocumented = quizAttemptAnswerRepo.existsByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefId(attemptId, answerDefId)
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

            List<QuizAnswerDefRepo.AnswerIdAndCorrectness> questions = quizAnswerRepo.getAnswerIdsAndCorrectnessIndicator(answerDefPartialInfo.getQuestionRefId())
            assert questions
            if (answerDefPartialInfo.getQuestionType() == QuizQuestionType.SingleChoice || answerDefPartialInfo.getQuestionType() == QuizQuestionType.Rating) {
                List<Integer> toRemove = questions.findAll { it.getAnswerRefId() != answerDefId }.collect { it.getAnswerRefId() }
                toRemove.each {
                    quizAttemptAnswerRepo.deleteByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefId(attemptId, it)
                }
            }
        }
    }

    QuizGradedResult failQuizAttempt(String userId, String quizId, Integer quizAttemptId) {
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

        QuizGradedResult gradedResult = new QuizGradedResult(passed: false, numQuestionsGotWrong: 0,
                gradedQuestions: [])

        userQuizAttempt.status = UserQuizAttempt.QuizAttemptStatus.FAILED
        userQuizAttempt.completed = new Date()
        quizAttemptRepo.save(userQuizAttempt)

        gradedResult.associatedSkillResults = reportAnyAssociatedSkills(userQuizAttempt, quizDef)
        gradedResult.started = userQuizAttempt.started
        gradedResult.completed = userQuizAttempt.completed
        return gradedResult
    }

    @Transactional
    QuizGradedResult completeQuizAttempt(String userId, String quizId, Integer quizAttemptId) {
        QuizDef quizDef = getQuizDef(quizId)
        boolean isSurvey = quizDef.type == QuizDefParent.QuizType.Survey
        Optional<UserQuizAttempt> optionalUserQuizAttempt = quizAttemptRepo.findById(quizAttemptId)
        if (!optionalUserQuizAttempt.isPresent()) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] does not exist", ErrorCode.BadParam)
        }

        if (quizAttemptRepo.checkQuizStatus(userId, quizAttemptId, quizId, UserQuizAttempt.QuizAttemptStatus.FAILED)) {
            throw new SkillQuizException("Provided attempt id [${quizAttemptId}], which corresponds to a failed quiz", ErrorCode.BadParam)
        }

        UserQuizAttempt userQuizAttempt = optionalUserQuizAttempt.get()
        if (userQuizAttempt.quizDefinitionRefId != quizDef.id) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] is not for [${quizId}] quiz", ErrorCode.BadParam)
        }
        if (userQuizAttempt.userId != userId) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] is not for [${userId}] user", ErrorCode.BadParam)
        }

        if(shouldQuizBeFailed(userQuizAttempt)) {
            failQuizAttempt(userId, quizId, quizAttemptId)
            throw new SkillQuizException("Deadline for [${quizAttemptId}] has expired", ErrorCode.BadParam)
        }

        List<QuizQuestionDef> dbQuestionDefs = quizQuestionRepo.findAllByQuizIdIgnoreCase(quizId)?.sort { it.displayOrder }
        List<QuizAnswerDef> dbAnswersDefs = quizAnswerRepo.findAllByQuizIdIgnoreCase(quizId)
        Map<Integer, List<QuizAnswerDef>> answerDefByQuestionId = dbAnswersDefs.groupBy {it.questionRefId }

        Set<Integer> selectedAnswerIds = quizAttemptAnswerRepo.getSelectedAnswerIds(quizAttemptId).toSet()
        List<UserQuizQuestionAttempt> existingAttempt = quizQuestionAttemptRepo.findAllByUserQuizAttemptRefId(quizAttemptId)

        List<QuizQuestionGradedResult> gradedQuestions = dbQuestionDefs.collect { QuizQuestionDef quizQuestionDef ->
            List<QuizAnswerDef> quizAnswerDefs = answerDefByQuestionId[quizQuestionDef.id]

            List<Integer> correctIds = quizAnswerDefs.findAll({ Boolean.valueOf(it.isCorrectAnswer) }).collect { it.id }.sort()
            List<Integer> selectedIds = quizAnswerDefs.findAll { selectedAnswerIds.contains(it.id) }?.collect { it.id }
            UserQuizQuestionAttempt.QuizQuestionStatus status
            if (!selectedIds) {
                status = UserQuizQuestionAttempt.QuizQuestionStatus.INCOMPLETE
            }

            boolean isCorrect = isSurvey ?: selectedIds.containsAll(correctIds) && correctIds.containsAll(selectedIds)
            if (status != UserQuizQuestionAttempt.QuizQuestionStatus.INCOMPLETE) {
                status = isCorrect ? UserQuizQuestionAttempt.QuizQuestionStatus.CORRECT : UserQuizQuestionAttempt.QuizQuestionStatus.WRONG
            }

            UserQuizQuestionAttempt attemptedQuestion = existingAttempt.find { it.quizQuestionDefinitionRefId == quizQuestionDef.id && it.userId == userId && it.userQuizAttemptRefId == quizAttemptId }

            if (attemptedQuestion) {
                attemptedQuestion.status = status
                quizQuestionAttemptRepo.save(attemptedQuestion)
            }
            else {
                UserQuizQuestionAttempt userQuizQuestionAttempt = new UserQuizQuestionAttempt(
                        userQuizAttemptRefId: quizAttemptId,
                        quizQuestionDefinitionRefId: quizQuestionDef.id,
                        userId: userId,
                        status: status
                )

                quizQuestionAttemptRepo.save(userQuizQuestionAttempt)
            }

            return new QuizQuestionGradedResult(questionId: quizQuestionDef.id, isCorrect: isCorrect, selectedAnswerIds: selectedIds, correctAnswerIds: correctIds)
        }
        Integer quizLength = getQuizLength(quizDef.id)
        if(quizLength == -1) {
            quizLength = gradedQuestions.size()
        }
        int numCorrect = gradedQuestions.count { it.isCorrect }
        Integer minNumQuestionsToPassConf = getMinNumQuestionsToPassSetting(quizDef.id)
        Integer minNumQuestionsToPass = minNumQuestionsToPassConf > 0 ? minNumQuestionsToPassConf : quizLength;
        boolean quizPassed = numCorrect >= minNumQuestionsToPass

        boolean shouldReturnGradedRes = quizPassed && quizDef.type == QuizDefParent.QuizType.Quiz;
        if (!quizPassed && quizDef.type == QuizDefParent.QuizType.Quiz) {
            UserQuizAttemptRepo.UserQuizAttemptStats userAttemptsStats = quizAttemptRepo.getUserAttemptsStats(userId, quizDef.id,
                    UserQuizAttempt.QuizAttemptStatus.INPROGRESS, UserQuizAttempt.QuizAttemptStatus.PASSED)
            Integer numCurrentAttempts = (userAttemptsStats?.getUserNumPreviousQuizAttempts() ?: 0) + 1
            int numConfiguredAttempts = getMaxQuizAttemptsSetting(quizDef.id)
            // anything 0 or below is considered to be unlimited attempts
            // only return graded results if there are no more attempts available
            shouldReturnGradedRes = numConfiguredAttempts > 0 && numCurrentAttempts >= numConfiguredAttempts;
        }

        QuizGradedResult gradedResult = new QuizGradedResult(passed: quizPassed, numQuestionsGotWrong: quizLength - numCorrect,
                gradedQuestions: shouldReturnGradedRes ? gradedQuestions : [])

        userQuizAttempt.status = quizPassed ? UserQuizAttempt.QuizAttemptStatus.PASSED : UserQuizAttempt.QuizAttemptStatus.FAILED
        userQuizAttempt.completed = new Date()
        userQuizAttempt.numQuestionsToPass = minNumQuestionsToPass
        quizAttemptRepo.save(userQuizAttempt)

        gradedResult.associatedSkillResults = reportAnyAssociatedSkills(userQuizAttempt, quizDef)
        gradedResult.started = userQuizAttempt.started
        gradedResult.completed = userQuizAttempt.completed
        return gradedResult
    }

    @Profile
    List<SkillEventResult> reportAnyAssociatedSkills(UserQuizAttempt userQuizAttempt, QuizDef quizDef) {
        List<SkillEventResult> res = []
        if (userQuizAttempt.status == UserQuizAttempt.QuizAttemptStatus.PASSED) {
            List<QuizToSkillDefRepo.ProjectIdAndSkillId> skills = quizToSkillDefRepo.getSkillsForQuiz(quizDef.id)
            if (skills){
                skills.each {
                    SkillEventResult skillEventResult = skillEventsService.reportSkill(it.projectId, it.skillId, userQuizAttempt.userId, false, userQuizAttempt.completed,
                            new SkillEventsService.SkillApprovalParams(disableChecks: true, isFromPassingQuiz: true))
                    res.add(skillEventResult)
                }
            }
        }
        return res
    }

    private QuizDef getQuizDef(String quizId) {
        QuizDef quizDef = quizDefRepo.findByQuizIdIgnoreCase(quizId)
        if (!quizDef) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }
        return quizDef
    }
}
