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
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.PublicProps
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.PublicPropsBasedValidator
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.QuizValidator
import skills.controller.exceptions.SkillQuizException
import skills.controller.result.model.MyQuizAttempt
import skills.controller.result.model.QuizSkillResult
import skills.controller.result.model.TableResult
import skills.quizLoading.model.*
import skills.services.CustomValidationResult
import skills.services.CustomValidator
import skills.services.LockingService
import skills.services.admin.UserCommunityService
import skills.services.attributes.ExpirationAttrs
import skills.services.attributes.QuestionAttrs
import skills.services.attributes.SkillAttributeService
import skills.services.attributes.SlidesAttrs
import skills.services.attributes.TextInputAiGradingAttrs
import skills.services.events.SkillEventResult
import skills.services.events.SkillEventsService
import skills.services.quiz.QuizQuestionType
import skills.services.slides.QuizAttrsStore
import skills.skillLoading.model.SlidesSummary
import skills.storage.model.*
import skills.storage.repos.*
import skills.tasks.TaskSchedulerService
import skills.tasks.data.TextInputAiGradingRequest
import skills.utils.InputSanitizer

import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
@Slf4j
class QuizRunService {

    @Autowired
    QuizDefWithDescRepo quizDefWithDescRepo

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    QuizAttrsStore quizAttrsStore

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
    PublicPropsBasedValidator propsBasedValidator

    @Autowired
    QuizSettingsRepo quizSettingsRepo

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    SkillEventsService skillEventsService

    @Autowired
    UserQuizQuestionAttemptRepo quizQuestionAttemptRepo

    @Autowired
    UserQuizAnswerGradedRepo userQuizAnswerGradedRepo

    @Autowired
    UserQuizAnswerAttemptRepo quizAttemptAnswerRepo

    @Autowired
    UserQuizQuestionAttemptRepo quizAttemptQuestionRepo

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    QuizNotificationService quizNotificationService

    @Autowired
    UserCommunityService userCommunityService

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    @Autowired
    TaskSchedulerService taskSchedulerService

    @Value('#{"${skills.config.ui.minimumSubjectPoints}"}')
    int minimumSubjectPoints

    @Value('#{"${skills.config.ui.minimumProjectPoints}"}')
    int minimumProjectPoints

    JsonSlurper jsonSlurper = new JsonSlurper()
    static final ObjectMapper mapper = new ObjectMapper()
    static final String AI_GRADER_USERID = 'ai-grader'

    private boolean skillExpiringSoon(String skillId, String projectId) {
        ExpirationAttrs attrs = skillAttributeService.getExpirationAttrs( projectId, skillId )
        return attrs && attrs?.expirationType == ExpirationAttrs.DAILY
    }

    @Transactional
    QuizInfo loadQuizInfo(String userId, String quizId, String skillId = null, String projectId = null) {
        QuizDefWithDescription quizDefWithDesc = quizDefWithDescRepo.findByQuizIdIgnoreCase(quizId)
        if (!quizDefWithDesc) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }

        List<QuizSetting> quizSettings = loadQuizSettings(quizDefWithDesc.id)
        boolean multipleTakes = quizSettings?.find( { it.setting == QuizSettings.MultipleTakes.setting })?.value?.toBoolean()

        UserQuizAttemptRepo.UserQuizAttemptStats userAttemptsStats =
                quizAttemptRepo.getUserAttemptsStats(userId, quizDefWithDesc.id,
                        UserQuizAttempt.QuizAttemptStatus.INPROGRESS, UserQuizAttempt.QuizAttemptStatus.PASSED)

        Integer numberOfQuestions = quizQuestionRepo.countByQuizId(quizId)

        QuizSetting maxNumAttemptsSetting = quizSettings?.find( { it.setting == QuizSettings.MaxNumAttempts.setting })
        QuizSetting minNumQuestionsToPassSetting = quizSettings?.find( { it.setting == QuizSettings.MinNumQuestionsToPass.setting })
        QuizSetting quizLength = quizSettings?.find( { it.setting == QuizSettings.QuizLength.setting })
        QuizSetting quizTimeLimit = quizSettings?.find( { it.setting == QuizSettings.QuizTimeLimit.setting })
        QuizSetting onlyIncorrect = quizSettings?.find({it.setting == QuizSettings.RetakeIncorrectQuestionsOnly.setting})
        QuizSetting showDescriptionOnQuizPage = quizSettings?.find({it.setting == QuizSettings.ShowDescriptionOnQuizPage.setting})
        boolean showDescription = showDescriptionOnQuizPage?.value?.toBoolean()

        Integer quizLengthAsInteger = quizLength ? Integer.valueOf(quizLength.value) : 0
        Integer lengthSetting = quizLengthAsInteger > 0 ? quizLengthAsInteger : numberOfQuestions
        boolean onlyIncorrectQuestions = onlyIncorrect?.value?.toBoolean()

        Integer numIncorrectQuestions = 0
        QuizToSkillDefRepo.QuizAttemptInfo latestQuizAttempt = getLatestQuizAttempt(quizDefWithDesc.id, userId)
        if(onlyIncorrectQuestions && latestQuizAttempt?.status == UserQuizAttempt.QuizAttemptStatus.FAILED) {
            numIncorrectQuestions = quizAttemptQuestionRepo.countWrongQuestionIdsForAttempt(latestQuizAttempt.attemptId)
        }

        if(!multipleTakes && skillId && projectId) {
            multipleTakes = skillExpiringSoon(skillId, projectId)
        }

        boolean canStartQuiz = true
        String errorMessage = null
        List<QuizSkillResult> skills = quizToSkillDefRepo.getSkillsForQuizWithSubjects(quizDefWithDesc.id, userId)
        skills.forEach(skill -> {
          if(skill.subjectPoints < minimumSubjectPoints) {
              canStartQuiz = false
              errorMessage = "This ${quizDefWithDesc.getType().toString()} is assigned to a Skill (${skill.skillId}) that does not have enough points to be completed. The Subject (${skill.subjectId}) that contains this skill must have at least ${ minimumSubjectPoints } points."
          }
            if(skill.projectPoints < minimumProjectPoints) {
                canStartQuiz = false
                errorMessage = "This ${quizDefWithDesc.getType().toString()} is assigned to a Skill (${skill.skillId}) that does not have enough points to be completed. The Project (${skill.projectId}) that contains this skill must have at least ${ minimumProjectPoints } points."
            }
        })

        UserQuizAttempt needsGradingAttempt = quizAttemptRepo.getByUserIdAndQuizIdAndState(userId, quizId, UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING)
        boolean needsGrading = false
        Date needsGradingAttemptDate = null
        if (needsGradingAttempt) {
            canStartQuiz = false
            needsGrading = true
            needsGradingAttemptDate = needsGradingAttempt.updated
        }

        String userCommunity = userCommunityService.getQuizUserCommunity(quizDefWithDesc.quizId)
        return new QuizInfo(
                name: quizDefWithDesc.name,
                description: InputSanitizer.unsanitizeForMarkdown(quizDefWithDesc.description),
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
                canStartQuiz: canStartQuiz,
                errorMessage: errorMessage,
                needsGrading: needsGrading,
                needsGradingAttemptDate: needsGradingAttemptDate,
                onlyIncorrectQuestions: onlyIncorrectQuestions,
                numIncorrectQuestions: numIncorrectQuestions,
                showDescriptionOnQuizPage: showDescription,
                slidesSummary: getSlidesSummary(quizDefWithDesc.quizId),
                userCommunity: userCommunity
        )
    }

    @Profile
    private SlidesSummary getSlidesSummary(String quizId) {
        SlidesAttrs slidesAttrs = quizAttrsStore.getSlidesAttrs(quizId)

        SlidesSummary res = null
        if (slidesAttrs) {
            res = new SlidesSummary(
                    url: slidesAttrs.url,
                    type: slidesAttrs.type,
                    width: slidesAttrs.width
            )
        }
        return res
    }

    private List<QuizQuestionDef> selectOnlyIncorrectQuestions(Integer quizId, String userId, List<QuizQuestionDef> questions) {
        QuizToSkillDefRepo.QuizAttemptInfo latestQuizAttempt = getLatestQuizAttempt(quizId, userId)
        if(latestQuizAttempt?.status == UserQuizAttempt.QuizAttemptStatus.FAILED) {
            Integer latestQuizAttemptId = latestQuizAttempt?.attemptId
            if(latestQuizAttemptId) {
                List<Integer> questionIds = quizAttemptQuestionRepo.getWrongQuestionIdsForAttempt(latestQuizAttemptId)
                questions = questions?.findAll { question -> questionIds.contains(question.id) }
            }
        }
        return questions
    }

    private List<QuizQuestionInfo> loadQuizQuestionInfo(QuizDef quizDef, List<QuizSetting> quizSettings, String userId) {
        List<QuizQuestionDef> dbQuestionDefs = quizQuestionRepo.findAllByQuizIdIgnoreCase(quizDef.quizId)
        List<QuizAnswerDef> dbAnswersDef = quizAnswerRepo.findAllByQuizIdIgnoreCase(quizDef.quizId)
        Map<Integer, List<QuizAnswerDef>> byQuizId = dbAnswersDef.groupBy { it.questionRefId }

        QuizSetting quizLength = quizSettings?.find( { it.setting == QuizSettings.QuizLength.setting })
        boolean randomizeQuestions = quizSettings?.find( { it.setting == QuizSettings.RandomizeQuestions.setting })?.value?.toBoolean()
        boolean randomizeAnswers = quizSettings?.find( { it.setting == QuizSettings.RandomizeAnswers.setting })?.value?.toBoolean()
        boolean forceRandomizationOfQuestions = quizLength?.value != null && Integer.valueOf(quizLength.value) < dbQuestionDefs.size()
        QuizSetting onlyIncorrect = quizSettings?.find({it.setting == QuizSettings.RetakeIncorrectQuestionsOnly.setting})
        boolean onlyIncorrectQuestions = onlyIncorrect?.value?.toBoolean()
        Boolean showAnswerHintsOnRetakesOnly = quizSettings?.find( { it.setting == QuizSettings.ShowAnswerHintsOnRetakeAttemptsOnly.setting })?.value?.toBoolean()
        Boolean includeAnswerHints = !showAnswerHintsOnRetakesOnly || isRetakeAttempt(quizDef, userId)

        if(onlyIncorrectQuestions) {
            dbQuestionDefs = selectOnlyIncorrectQuestions(quizDef.id, userId, dbQuestionDefs)
        }

        if(randomizeQuestions || forceRandomizationOfQuestions) {
            dbQuestionDefs?.shuffle()
        } else {
            dbQuestionDefs?.sort{it.getDisplayOrder()}
        }
        List<QuizQuestionInfo> questions = dbQuestionDefs?.collect {
            List<QuizAnswerDef> quizAnswerDefs = byQuizId[it.id]
            if(randomizeAnswers) {
                quizAnswerDefs?.shuffle()
            }

            def answerOptions
            List<String> matchingTerms = []

            if(it.type == QuizQuestionType.Matching) {
                JsonSlurper slurper = new JsonSlurper()
                def values = []
                quizAnswerDefs.collect{ answer ->
                    def parsedAnswer = slurper.parseText(answer.multiPartAnswer)
                    parsedAnswer.id = answer.id
                    matchingTerms.push(parsedAnswer.value)
                    values.push([value: parsedAnswer.term, id: parsedAnswer.id])
                }
                matchingTerms.shuffle()

                answerOptions = values.collect { answer ->
                    new QuizAnswerOptionsInfo(
                            id: answer.id,
                            answerOption: answer.value
                    )
                }.sort{ it.id }
            } else {
                answerOptions = quizAnswerDefs.collect {
                    new QuizAnswerOptionsInfo(
                            id: it.id,
                            answerOption: it.answer ? it.answer : it.multiPartAnswer
                    )
                }
            }
            new QuizQuestionInfo(
                    id: it.id,
                    question: InputSanitizer.unsanitizeForMarkdown(it.question),
                    questionType: it.type.toString(),
                    canSelectMoreThanOne: quizAnswerDefs?.count({ Boolean.valueOf(it.isCorrectAnswer) }) > 1,
                    answerOptions: answerOptions,
                    answerHint: includeAnswerHints ? it.answerHint : null,
                    mediaAttributes: it.attributes,
                    matchingTerms: matchingTerms
            )
        }
        return questions
    }
    private Boolean isRetakeAttempt(QuizDef quizDef, String userId) {
        Boolean isRetakeAttempt = quizDef.type == QuizDef.QuizType.Quiz && quizAttemptRepo.getUserAttemptsStats(userId, quizDef.id,
                UserQuizAttempt.QuizAttemptStatus.INPROGRESS, UserQuizAttempt.QuizAttemptStatus.PASSED).userNumPreviousQuizAttempts > 0
        return isRetakeAttempt
    }

    LocalDateTime calculateDeadline(LocalDateTime started, Integer timeLimitInSeconds) {
        LocalDateTime deadlineLocal = started.plusSeconds(timeLimitInSeconds);
        return deadlineLocal
    }

    Boolean hasQuizExpired(LocalDateTime deadline) {
        LocalDateTime currentDate = LocalDateTime.now(ZoneOffset.UTC)
        return currentDate.isAfter(deadline)
    }

    QuizToSkillDefRepo.QuizAttemptInfo getLatestQuizAttempt(Integer id, String userId) {
        return quizAttemptRepo.getLatestQuizAttemptForUserByQuizId(id, userId)
    }

    @Transactional
    QuizAttemptStartResult startQuizAttempt(String userId, String quizId, String skillId = null, String projectId = null) {
        QuizDef quizDef = getQuizDef(quizId)
        List<QuizSetting> quizSettings = loadQuizSettings(quizDef.id)
        Integer quizTimeLimit = quizSettings?.find( { it.setting == QuizSettings.QuizTimeLimit.setting })?.value?.toInteger()
        QuizSetting quizLength = quizSettings?.find( { it.setting == QuizSettings.QuizLength.setting })

        List<QuizQuestionInfo> questions = loadQuizQuestionInfo(quizDef, quizSettings, userId)
        List<QuizQuestionInfo> questionsForQuiz = []
        Integer quizLengthAsInteger = quizLength ? Integer.valueOf(quizLength.value) : 0
        Integer lengthSetting = quizLengthAsInteger > 0 ? quizLengthAsInteger : questions.size()

        validateNoPendingGradingAttempt(userId, quizId)

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

            List<Integer> selectedAnswerIds = alreadySelected?.findAll({!it.getAnswerText()})?.collect { it.getAnswerId()}
            List<QuizAttemptStartResult.AnswerIdAndEnteredText> enteredText = alreadySelected?.findAll({it.getAnswerText()})?.collect {
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


        validateQuizAttempts(quizDef, userId, quizId, skillId, projectId)
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

    private void validateNoPendingGradingAttempt(String userId, String quizId) {
        // do not allow to start a new attempt if there is a pending attempt that needs grading
        UserQuizAttempt needsGradingAttempt = quizAttemptRepo.getByUserIdAndQuizIdAndState(userId, quizId, UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING)
        if (needsGradingAttempt) {
            throw new SkillQuizException("There is currently a pending attempt for  [${userId}] that awaiting grading. Cannot start a new attempt", quizId, ErrorCode.BadParam)
        }
    }

    @Profile
    private void validateQuizAttempts(QuizDef quizDef, String userId, String quizId, String skillId = null, String projectId = null) {
        UserQuizAttemptRepo.UserQuizAttemptStats userAttemptsStats = quizAttemptRepo.getUserAttemptsStats(userId, quizDef.id,
                UserQuizAttempt.QuizAttemptStatus.INPROGRESS, UserQuizAttempt.QuizAttemptStatus.PASSED)
        Integer numCurrentAttempts = userAttemptsStats?.getUserNumPreviousQuizAttempts() ?: 0
        boolean allowMultipleTakes = allowMultipleTakes(quizDef.id)
        boolean aboutToExpire = false

        if (skillId && projectId) {
            aboutToExpire = skillExpiringSoon(skillId, projectId)
        }
        if (quizDef.type == QuizDefParent.QuizType.Survey  && !allowMultipleTakes && !aboutToExpire) {
            if (numCurrentAttempts > 0) {
                throw new SkillQuizException("User [${userId}] has already taken this survey", quizId, ErrorCode.BadParam)
            }
        } else {
            if (userAttemptsStats?.getUserQuizPassed() && !allowMultipleTakes && !aboutToExpire) {
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
        return quizSettingsRepo.findAllByQuizRefIdAndSettingIn(quizRefId, [
                QuizSettings.MaxNumAttempts.setting,
                QuizSettings.MinNumQuestionsToPass.setting,
                QuizSettings.RandomizeQuestions.setting,
                QuizSettings.RandomizeAnswers.setting,
                QuizSettings.QuizLength.setting,
                QuizSettings.QuizTimeLimit.setting,
                QuizSettings.MultipleTakes.setting,
                QuizSettings.RetakeIncorrectQuestionsOnly.setting,
                QuizSettings.ShowAnswerHintsOnRetakeAttemptsOnly.setting,
                QuizSettings.ShowDescriptionOnQuizPage.setting,
        ])
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
    private boolean alwaysShowCorrectAnswers(Integer quizRefId) {
        return getQuizSettingAsBoolean(quizRefId, QuizSettings.AlwaysShowCorrectAnswers.setting)
    }
    @Profile
    private boolean onlyIncorrectAnswers(Integer quizRefId) {
        return getQuizSettingAsBoolean(quizRefId, QuizSettings.RetakeIncorrectQuestionsOnly.setting)
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

        if (!quizAnswerRepo.doesAnswerBelongToQuizAttempt(quizAttemptId, answerDefId)) {
            throw new SkillQuizException("Provided answer id [${answerDefId}] does not exist for [${quizAttemptId}] quiz attempt", ErrorCode.BadParam)
        }

        UserQuizAttempt inProgressAttempt = quizAttemptRepo.getByUserIdAndQuizIdAndState(userId, quizId, UserQuizAttempt.QuizAttemptStatus.INPROGRESS)
        if (inProgressAttempt) {
            if(shouldQuizBeFailed(inProgressAttempt)) {
                failQuizAttempt(userId, quizId, quizAttemptId)
                throw new SkillQuizException("Deadline for [${quizAttemptId}] has expired", ErrorCode.BadParam)
            }
        }

        QuizAnswerDefRepo.AnswerDefPartialInfo answerDefPartialInfo = getAnswerDefPartialInfo(quizId, answerDefId)

        if (answerDefPartialInfo.getQuestionType() == QuizQuestionType.TextInput) {
            if (quizReportAnswerReq.isSelected) {
                propsBasedValidator.quizValidationMaxStrLength(PublicProps.UiProp.maxTakeQuizInputTextAnswerLength,
                        "Answer", quizReportAnswerReq.answerText, quizId)
                QuizValidator.isNotBlank(quizReportAnswerReq.getAnswerText(), "answerText", quizId)
                CustomValidationResult customValidationResult = validator.validateDescription(quizReportAnswerReq.getAnswerText(), null, null, quizId)
                if (!customValidationResult.valid) {
                    throw new SkillQuizException("answerText is invalid: ${customValidationResult.msg}", quizId, ErrorCode.BadParam)
                }
            } else {
                QuizValidator.isTrue(StringUtils.isBlank(quizReportAnswerReq.getAnswerText()), "For TextInput type, if isSelected=false then the answer must be null or blank", quizId)
            }
            QuizDef quizDef = getQuizDef(quizId)
            handleReportingTextInputQuestion(quizDef, userId, quizAttemptId, answerDefId, quizReportAnswerReq)
        } else if (answerDefPartialInfo.getQuestionType() == QuizQuestionType.Matching) {
            handleReportingMatchingQuestion(userId, quizAttemptId, answerDefId, quizReportAnswerReq, answerDefPartialInfo)
        } else {
            handleReportingAChoiceBasedQuestion(userId, quizAttemptId, answerDefId, quizReportAnswerReq, answerDefPartialInfo)
        }
    }

    private void handleReportingMatchingQuestion(String userId, Integer quizAttemptId, Integer answerDefId, QuizReportAnswerReq quizReportAnswerReq, QuizAnswerDefRepo.AnswerDefPartialInfo answerDefPartialInfo) {
        UserQuizAnswerAttempt existingAnswerAttempt = quizAttemptAnswerRepo.findByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefId(quizAttemptId, answerDefId)
        def parsed = jsonSlurper.parseText(answerDefPartialInfo.multiPartAnswer)
        if (existingAnswerAttempt) {
            existingAnswerAttempt.answer = quizReportAnswerReq.getAnswerText()
            existingAnswerAttempt.status = quizReportAnswerReq.getAnswerText() == parsed.value ? UserQuizAnswerAttempt.QuizAnswerStatus.CORRECT : UserQuizAnswerAttempt.QuizAnswerStatus.WRONG
            quizAttemptAnswerRepo.save(existingAnswerAttempt)
        } else if (quizReportAnswerReq.isSelected) {
            UserQuizAnswerAttempt newAnswerAttempt = new UserQuizAnswerAttempt(
                    userQuizAttemptRefId: quizAttemptId,
                    quizAnswerDefinitionRefId: answerDefId,
                    userId: userId,
                    status: quizReportAnswerReq.getAnswerText() == parsed.value ? UserQuizAnswerAttempt.QuizAnswerStatus.CORRECT : UserQuizAnswerAttempt.QuizAnswerStatus.WRONG,
                    answer: quizReportAnswerReq.getAnswerText(),
            )
            quizAttemptAnswerRepo.save(newAnswerAttempt)
        }
    }

    private void handleReportingTextInputQuestion(QuizDef quizDef, String userId, Integer quizAttemptId, Integer answerDefId, QuizReportAnswerReq quizReportAnswerReq) {
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
                    status: quizDef.type == QuizDefParent.QuizType.Quiz ? UserQuizAnswerAttempt.QuizAnswerStatus.NEEDS_GRADING : UserQuizAnswerAttempt.QuizAnswerStatus.CORRECT,
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

    @Transactional
    QuizAnswerGradingResult gradeQuestionAnswer(String userId, String quizId, Integer quizAttemptId, Integer answerDefId, QuizGradeAnswerReq gradeAnswerReq, Boolean aiAssistantGraded = false, Integer aiConfidenceLevel = null) {
        String graderUserId = aiAssistantGraded ? AI_GRADER_USERID : userInfoService.getCurrentUser().username
        UserAttrs graderUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(graderUserId)

        QuizDef quizDef = getQuizDef(quizId)
        if (quizDef.type != QuizDefParent.QuizType.Quiz) {
            throw new SkillQuizException("Provided quizId [${quizId}] is not a quiz", ErrorCode.BadParam)
        }
        UserQuizAttempt userQuizAttempt = getQuizAttempt(quizAttemptId)
        validateAttempt(userQuizAttempt, quizDef, quizAttemptId, quizId, userId)

        propsBasedValidator.quizValidationMaxStrLength(PublicProps.UiProp.maxGraderFeedbackMessageLength, "Feedback", gradeAnswerReq.feedback, quizId)
        CustomValidationResult customValidationResult = validator.validateDescription(gradeAnswerReq.feedback, null, null, quizDef.quizId)
        if (!customValidationResult.valid) {
            throw new SkillQuizException("Feedback is invalid: ${customValidationResult.msg}", quizId, ErrorCode.BadParam)
        }

        Optional<QuizAnswerDef> quizAnswerDefOptional = quizAnswerRepo.findById(answerDefId)
        if (!quizAnswerDefOptional.isPresent()) {
            throw new SkillQuizException("Provided answer definition id [${quizAttemptId}] does not exist", ErrorCode.BadParam)
        }
        QuizAnswerDef answerDef = quizAnswerDefOptional.get()

        UserQuizAnswerAttempt userQuizAnswerAttempt = quizAttemptAnswerRepo.findByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefId(quizAttemptId, answerDefId)
        if (!userQuizAnswerAttempt) {
            throw new SkillQuizException("Could not find answer attempt for quizAttemptId=[${quizAttemptId}] and answerDefId=[${answerDefId}]", ErrorCode.BadParam)
        }
        if (userQuizAnswerAttempt.userQuizAttemptRefId != userQuizAttempt.id) {
            throw new SkillQuizException("Supplied quiz answer attempt id  [${userQuizAnswerAttempt.userQuizAttemptRefId}] does not match user quiz attempt id [${userQuizAttempt.id}]", ErrorCode.BadParam)
        }
        if (aiAssistantGraded && !aiConfidenceLevel) {
            throw new SkillQuizException("AI grader confidence level must be provided for AI assistant graded answer attempt", ErrorCode.BadParam)
        }

        UserQuizAnswerGraded alreadyGraded = userQuizAnswerGradedRepo.findByUserQuizAnswerAttemptRefId(userQuizAnswerAttempt.id)
        if (alreadyGraded) {
            throw new SkillQuizException("Question for quiz [${quizId}] attemptId [${quizAttemptId}] answerDefId [${answerDefId}] has already been graded", ErrorCode.BadParam)
        }

        List<UserQuizQuestionAttempt> questionAttempts = quizQuestionAttemptRepo.findAllByUserQuizAttemptRefId(userQuizAttempt.id)

        userQuizAnswerAttempt.status = gradeAnswerReq.isCorrect ? UserQuizAnswerAttempt.QuizAnswerStatus.CORRECT : UserQuizAnswerAttempt.QuizAnswerStatus.WRONG
        quizAttemptAnswerRepo.save(userQuizAnswerAttempt)

        UserQuizQuestionAttempt questionAttempt = questionAttempts.find { it.quizQuestionDefinitionRefId == answerDef.questionRefId }
        questionAttempt.status = gradeAnswerReq.isCorrect ? UserQuizQuestionAttempt.QuizQuestionStatus.CORRECT : UserQuizQuestionAttempt.QuizQuestionStatus.WRONG
        quizQuestionAttemptRepo.save(questionAttempt)

        boolean doneGradingAttempt = questionAttempts.find({ it.status == UserQuizQuestionAttempt.QuizQuestionStatus.NEEDS_GRADING }) == null
        if (doneGradingAttempt) {
            int numCorrect = (int)questionAttempts.count {it.status == UserQuizQuestionAttempt.QuizQuestionStatus.CORRECT}
            Integer minNumQuestionsToPassConf = getMinNumQuestionsToPassSetting(quizDef.id)
            Integer minNumQuestionsToPass = minNumQuestionsToPassConf > 0 ? minNumQuestionsToPassConf : questionAttempts.size();
            boolean isQuizPassed = numCorrect >= minNumQuestionsToPass

            userQuizAttempt.status = isQuizPassed ? UserQuizAttempt.QuizAttemptStatus.PASSED : UserQuizAttempt.QuizAttemptStatus.FAILED
            quizAttemptRepo.save(userQuizAttempt)

            quizNotificationService.sendGradedRequestNotification(quizDef, userQuizAttempt)
        }

        UserQuizAnswerGraded userQuizAnswerGraded = new UserQuizAnswerGraded(
                graderUserAttrsRefId: graderUserAttrs.id,
                userQuizAnswerAttemptRefId: userQuizAnswerAttempt.id,
                feedback: gradeAnswerReq.feedback,
                aiConfidenceLevel: aiConfidenceLevel)
        userQuizAnswerGradedRepo.save(userQuizAnswerGraded)

        if (doneGradingAttempt) {
            reportAnyAssociatedSkills(userQuizAttempt, quizDef)
        }

        return new QuizAnswerGradingResult(doneGradingAttempt: doneGradingAttempt)
    }

    QuizGradedResult failQuizAttempt(String userId, String quizId, Integer quizAttemptId) {
        QuizDef quizDef = getQuizDef(quizId)
        UserQuizAttempt userQuizAttempt = getQuizAttempt(quizAttemptId)
        validateAttempt(userQuizAttempt, quizDef, quizAttemptId, quizId, userId)

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

    private static void validateAttempt(UserQuizAttempt userQuizAttempt, QuizDef quizDef, int quizAttemptId, String quizId, String userId) {
        if (userQuizAttempt.quizDefinitionRefId != quizDef.id) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] is not for [${quizId}] quiz", ErrorCode.BadParam)
        }
        if (userQuizAttempt.userId != userId) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] is not for [${userId}] user", ErrorCode.BadParam)
        }
        if (userQuizAttempt.status == UserQuizAttempt.QuizAttemptStatus.PASSED || userQuizAttempt.status == UserQuizAttempt.QuizAttemptStatus.FAILED) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] was already completed", ErrorCode.BadParam)
        }
    }

    @Transactional
    QuizGradedResult completeQuizAttempt(String userId, String quizId, Integer quizAttemptId) {
        QuizDef quizDef = getQuizDef(quizId)
        boolean isSurvey = quizDef.type == QuizDefParent.QuizType.Survey

        if (quizAttemptRepo.checkQuizStatus(userId, quizAttemptId, quizId, UserQuizAttempt.QuizAttemptStatus.FAILED)) {
            throw new SkillQuizException("Provided attempt id [${quizAttemptId}], which corresponds to a failed quiz", ErrorCode.BadParam)
        }

        UserQuizAttempt userQuizAttempt = getQuizAttempt(quizAttemptId)
        validateAttempt(userQuizAttempt, quizDef, quizAttemptId, quizId, userId)

        if(shouldQuizBeFailed(userQuizAttempt)) {
            failQuizAttempt(userId, quizId, quizAttemptId)
            throw new SkillQuizException("Deadline for [${quizAttemptId}] has expired", ErrorCode.BadParam)
        }

        List<QuizQuestionDef> dbQuestionDefs = quizQuestionRepo.findQuestionDefsForSpecificQuizAttempt(userQuizAttempt.id)?.sort { it.displayOrder }
        List<QuizAnswerDef> dbAnswersDefs = quizAnswerRepo.findAllByQuestionRefIdIn(dbQuestionDefs.collect({it.id}))
        Map<Integer, List<QuizAnswerDef>> answerDefByQuestionId = dbAnswersDefs.groupBy {it.questionRefId }

        Set<Integer> selectedAnswerIds = quizAttemptAnswerRepo.getSelectedAnswerIds(quizAttemptId).toSet()
        List<UserQuizQuestionAttempt> existingAttempt = quizQuestionAttemptRepo.findAllByUserQuizAttemptRefId(quizAttemptId)

        List<QuizQuestionGradedResult> gradedQuestions = dbQuestionDefs.collect { QuizQuestionDef quizQuestionDef ->
            List<QuizAnswerDef> quizAnswerDefs = answerDefByQuestionId[quizQuestionDef.id]

            List<Integer> correctIds = quizAnswerDefs.findAll({ Boolean.valueOf(it.isCorrectAnswer) }).collect { it.id }.sort()
            List<Integer> selectedIds = quizAnswerDefs.findAll { selectedAnswerIds.contains(it.id) }?.collect { it.id }
            UserQuizQuestionAttempt.QuizQuestionStatus status
            boolean isCorrect
            if (!isSurvey && quizQuestionDef.type == QuizQuestionType.TextInput) {
                status = UserQuizQuestionAttempt.QuizQuestionStatus.NEEDS_GRADING
                isCorrect = false
                if (quizQuestionDef.attributes) {
                    QuestionAttrs questionAttrs = mapper.readValue(quizQuestionDef.attributes, QuestionAttrs.class)
                    TextInputAiGradingAttrs textInputAiGradingAttrs = questionAttrs.textInputAiGradingConf
                    if (textInputAiGradingAttrs?.enabled) {
                        assert quizAnswerDefs.size() == 1, "Unexpected number of quizAnswerDefs for TextInput question [${quizAnswerDefs.size()}]"
                        Integer answerDefId = quizAnswerDefs.first().id
                        UserQuizAnswerAttempt userQuizAnswerAttempt = quizAttemptAnswerRepo.findByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefId(quizAttemptId, answerDefId)
                        taskSchedulerService.gradeTextInputUsingAi(new TextInputAiGradingRequest(userId: userId, quizId: quizId, quizAttemptId: quizAttemptId, answerDefId: answerDefId, textInputAiGradingAttrs: textInputAiGradingAttrs, studentAnswer: userQuizAnswerAttempt.answer, question: quizQuestionDef.question))
                    }
                }
            } else if (quizQuestionDef.type == QuizQuestionType.Matching) {
                List<UserQuizAnswerAttempt> attempt = quizAttemptAnswerRepo.findAllByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefIdIn(quizAttemptId, selectedIds.toSet())
                if(attempt) {
                    status = attempt.find{answer -> answer.status == UserQuizAnswerAttempt.QuizAnswerStatus.WRONG } ? UserQuizQuestionAttempt.QuizQuestionStatus.WRONG : UserQuizQuestionAttempt.QuizQuestionStatus.CORRECT
                    correctIds = attempt.findAll{answer -> answer.status == UserQuizAnswerAttempt.QuizAnswerStatus.CORRECT }.collect { it.quizAnswerDefinitionRefId }
                } else {
                    status = UserQuizQuestionAttempt.QuizQuestionStatus.WRONG
                }
                isCorrect = status == UserQuizQuestionAttempt.QuizQuestionStatus.CORRECT
            } else {
                if (!selectedIds) {
                    status = UserQuizQuestionAttempt.QuizQuestionStatus.INCOMPLETE
                }
                isCorrect = isSurvey ?: selectedIds.containsAll(correctIds) && correctIds.containsAll(selectedIds)
                if (!status || status != UserQuizQuestionAttempt.QuizQuestionStatus.INCOMPLETE) {
                    status = isCorrect ? UserQuizQuestionAttempt.QuizQuestionStatus.CORRECT : UserQuizQuestionAttempt.QuizQuestionStatus.WRONG
                }
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

            return new QuizQuestionGradedResult(questionId: quizQuestionDef.id, isCorrect: isCorrect, selectedAnswerIds: selectedIds, correctAnswerIds: correctIds, status: status)
        }
        Integer quizLength = getQuizLength(quizDef.id)
        if(quizLength == -1) {
            quizLength = quizQuestionRepo.countByQuizId(quizId)
        }
        int numCorrect = (int)gradedQuestions.count { it.isCorrect }
        int numQuestionsNeedGrading = (int)gradedQuestions.count { it.status == UserQuizQuestionAttempt.QuizQuestionStatus.NEEDS_GRADING }
        Integer minNumQuestionsToPassConf = getMinNumQuestionsToPassSetting(quizDef.id)
        Integer minNumQuestionsToPass = minNumQuestionsToPassConf > 0 ? minNumQuestionsToPassConf : quizLength;
        boolean onlyIncorrectQuestions = onlyIncorrectAnswers(quizDef.id)
        if(onlyIncorrectQuestions) {
            Integer previouslyCorrect = quizLength - gradedQuestions.size()
            numCorrect = numCorrect + previouslyCorrect
        }

        boolean showCorrectAnswers = alwaysShowCorrectAnswers(quizDef.id)
        boolean quizPassed = numCorrect >= minNumQuestionsToPass

        boolean shouldReturnGradedRes = (quizPassed || showCorrectAnswers) && quizDef.type == QuizDefParent.QuizType.Quiz;
        QuizGradedResult gradedResult = new QuizGradedResult(passed: quizPassed, numQuestionsGotWrong: quizLength - numCorrect - numQuestionsNeedGrading,
                gradedQuestions: shouldReturnGradedRes ? gradedQuestions : [])

        if (numQuestionsNeedGrading > 0) {
            userQuizAttempt.status = UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING
            gradedResult.numQuestionsNeedGrading = numQuestionsNeedGrading
            gradedResult.needsGrading = true
        } else {
            userQuizAttempt.status = quizPassed ? UserQuizAttempt.QuizAttemptStatus.PASSED : UserQuizAttempt.QuizAttemptStatus.FAILED
        }
        userQuizAttempt.completed = new Date()
        userQuizAttempt.numQuestionsToPass = minNumQuestionsToPass
        quizAttemptRepo.save(userQuizAttempt)

        if(userQuizAttempt.status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING) {
            quizNotificationService.sendGradingRequestNotifications(quizDef, userId)
        }

        gradedResult.associatedSkillResults = reportAnyAssociatedSkills(userQuizAttempt, quizDef)
        gradedResult.started = userQuizAttempt.started
        gradedResult.completed = userQuizAttempt.completed
        return gradedResult
    }

    private Integer getMinNumQuestionsRequiredToPass(QuizDef quizDef, int numTotalQuestions) {
        Integer minNumQuestionsToPassConf = getMinNumQuestionsToPassSetting(quizDef.id)
        Integer minNumQuestionsToPass = minNumQuestionsToPassConf > 0 ? minNumQuestionsToPassConf : numTotalQuestions;
        return minNumQuestionsToPass
    }

    @Profile
    List<SkillEventResult> reportAnyAssociatedSkills(UserQuizAttempt userQuizAttempt, QuizDef quizDef) {
        return reportAnyAssociatedSkills(userQuizAttempt, quizDef.id)
    }

    @Profile
    List<SkillEventResult> reportAnyAssociatedSkills(UserQuizAttempt userQuizAttempt, Integer quizDefId) {
        List<SkillEventResult> res = []
        if (userQuizAttempt.status == UserQuizAttempt.QuizAttemptStatus.PASSED) {
            List<QuizToSkillDefRepo.ProjectIdAndSkillId> skills = quizToSkillDefRepo.getSkillsForQuiz(quizDefId)
            if (skills) {
                skills.each {
                    if (Boolean.valueOf(it.enabled)) {
                        SkillEventResult skillEventResult = reportSkill(it.projectId, it.skillId, userQuizAttempt.userId, userQuizAttempt.completed)
                        if (skillEventResult) {
                            res.add(skillEventResult)
                        }
                    }
                }
            }
        }
        return res
    }

    @Profile
    void checkForDependentQuizzes(SkillEventResult res, String userId, SkillDefMin currentSkillDef) {
        List<SkillDefMin> parents = skillEventsSupportRepo.findParentSkillsByChildIdAndType(currentSkillDef.id, [SkillRelDef.RelationshipType.Dependence])
        Set<ProjectAndSkillId> dependantQuizSkillIds = []
        // check for dependent quiz skills or badges that contain quiz skills that are passed, if so then report those skill(s)
        parents.each { SkillDefMin skillDefMin ->
            if (skillDefMin.selfReportingType == SkillDef.SelfReportingType.Quiz) {
                dependantQuizSkillIds.add(new ProjectAndSkillId(skillDefMin.projectId, skillDefMin.skillId, skillDefMin.id))
            } else if (skillDefMin.type == SkillDef.ContainerType.Badge) {
                List<SkillDef> badgeSkills = skillDefRepo.findChildSkillsByIdAndRelationshipType(skillDefMin.id, SkillRelDef.RelationshipType.BadgeRequirement)
                dependantQuizSkillIds.addAll(badgeSkills.findAll { it.selfReportingType == SkillDef.SelfReportingType.Quiz }.collect {new ProjectAndSkillId(it.projectId, it.skillId, it.id)})
            }
        }
        dependantQuizSkillIds.each { quizSkill ->
            UserQuizAttempt passedQuizAttempt = findPassedUserQuizAttemptForSkillAndUser(quizSkill.skillRefId, userId)
            if (passedQuizAttempt) {
                SkillEventResult skillEventResult = reportSkill(quizSkill.projectId, quizSkill.skillId, passedQuizAttempt.userId, passedQuizAttempt.completed)
                if (skillEventResult) {
                    res.completed.addAll(skillEventResult.completed)
                }
            }
        }
    }

    private SkillEventResult reportSkill(String projectId, String skillId, String userId, Date incomingSkillDate) {
        SkillEventResult skillEventResult = null
        if (userCanReportSkill(userId, projectId)) {
            skillEventResult = skillEventsService.reportSkill(projectId, skillId, userId, false, incomingSkillDate,
                    new SkillEventsService.SkillApprovalParams(disableChecks: true, isFromPassingQuiz: true))
        }
        return skillEventResult
    }

    private Boolean userCanReportSkill(String userId, String projectId) {
        return !userCommunityService.isUserCommunityOnlyProject(projectId) || userCommunityService.isUserCommunityMember(userId)
    }

    @Transactional
    TableResult getCurrentUserQuizRuns(String quizNameQuery, PageRequest pageRequest) {
        UserInfo currentUser = userInfoService.currentUser
        Boolean isUCMember = userCommunityService.isUserCommunityMember(currentUser.username) ?: false
        Page<MyQuizAttempt> quizAttemptsPage = quizAttemptRepo.findUserQuizAttempts(currentUser.username, quizNameQuery ?: "", isUCMember, pageRequest)
        long count = quizAttemptsPage.getTotalElements()
        List<MyQuizAttempt> quizAttempts = quizAttemptsPage.getContent()
        return new TableResult(totalCount: count, data: quizAttempts, count: count)
    }

    private QuizDef getQuizDef(String quizId) {
        QuizDef quizDef = quizDefRepo.findByQuizIdIgnoreCase(quizId)
        if (!quizDef) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }
        return quizDef
    }
    private UserQuizAttempt getQuizAttempt(Long quizAttemptId) {
        Optional<UserQuizAttempt> optionalUserQuizAttempt = quizAttemptRepo.findById(quizAttemptId)
        if (!optionalUserQuizAttempt.isPresent()) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] does not exist", ErrorCode.BadParam)
        }
        return optionalUserQuizAttempt.get()
    }
    private QuizAnswerDefRepo.AnswerDefPartialInfo getAnswerDefPartialInfo(String quizId, Integer answerDefId) {
        QuizAnswerDefRepo.AnswerDefPartialInfo answerDefPartialInfo = quizAnswerRepo.getPartialDefByAnswerDefId(answerDefId)
        if (!answerDefPartialInfo) {
            throw new SkillQuizException("Provided answer id [${answerDefId}] does not exist", ErrorCode.BadParam)
        }
        if (answerDefPartialInfo.getQuizId() != quizId) {
            throw new SkillQuizException("Supplied quizId of [${quizId}] does not match answer's quiz id  of [${answerDefPartialInfo.getQuizId()}]", ErrorCode.BadParam)
        }
        return answerDefPartialInfo
    }

    private UserQuizAttempt findPassedUserQuizAttemptForSkillAndUser(Integer skillRefId, String userId) {
        PageRequest onePlease = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "updated"))
        List<UserQuizAttempt> passedQuizAttempts = userQuizAttemptRepo.findBySkillRefIdAndUserIdAndByStatus(
                skillRefId,
                userId,
                UserQuizAttempt.QuizAttemptStatus.PASSED, onePlease)
        return passedQuizAttempts ? passedQuizAttempts.first() : null
    }

    @Canonical
    private static class ProjectAndSkillId {
        String projectId
        String skillId
        Integer skillRefId
    }
}
