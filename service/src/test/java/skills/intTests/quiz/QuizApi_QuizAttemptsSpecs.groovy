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
package skills.intTests.quiz


import org.springframework.beans.factory.annotation.Autowired
import skills.controller.exceptions.ErrorCode
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.storage.repos.*

class QuizApi_QuizAttemptsSpecs extends DefaultIntSpec {

    @Autowired
    QuizQuestionDefRepo quizQuestionDefRepo

    @Autowired
    QuizAnswerDefRepo quizAnswerDefRepo

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    @Autowired
    UserQuizQuestionAttemptRepo userQuizQuestionAttemptRepo

    @Autowired
    UserQuizAnswerAttemptRepo userQuizAnswerAttemptRepo

    def "run quiz 2 attempts - failed attempt followed by a pass"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        def quizAttempt1 =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt1.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt1.id, quizAttempt.questions[1].answerOptions[0].id)
        def gradedQuizAttempt1 = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt1.id).body

        then:
        gradedQuizAttempt.passed == false
        gradedQuizAttempt.numQuestionsGotWrong == 1
        !gradedQuizAttempt.gradedQuestions

        gradedQuizAttempt1.passed == true
        gradedQuizAttempt1.numQuestionsGotWrong == 0
        gradedQuizAttempt1.gradedQuestions.questionId == quizAttempt.questions.id
        gradedQuizAttempt1.gradedQuestions.isCorrect == [true, true]
        gradedQuizAttempt1.gradedQuestions[0].selectedAnswerIds == [quizAttempt.questions[0].answerOptions[0].id]
        gradedQuizAttempt1.gradedQuestions[1].selectedAnswerIds == [quizAttempt.questions[1].answerOptions[0].id]
    }

    def "previous quiz attempts do not affect follow-on attempts"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        when:
        def quizAttempt1_t0 =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt1_t0.id, quizAttempt.questions[0].answerOptions[1].id)
        def quizAttempt1_t1 =  skillsService.startQuizAttempt(quiz.quizId).body
        then:
        quizAttempt1_t0.id != quizAttempt.id
        !quizAttempt1_t0.selectedAnswerIds
        quizAttempt1_t1.selectedAnswerIds == [quizAttempt.questions[0].answerOptions[1].id]
        quizAttempt1_t0.id == quizAttempt1_t1.id
        // make sure old runs answer were not removed
        userQuizAnswerAttemptRepo.findAll().findAll({ it.userQuizAttemptRefId == quizAttempt.id }).collect { it.quizAnswerDefinitionRefId } == [quizAttempt.questions[0].answerOptions[0].id, quizAttempt.questions[1].answerOptions[1].id]
    }

    def "only 1 attempt is allowed"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MaxNumAttempts.setting, value: '1'],
        ])

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.passed == false
        when:
        skillsService.startQuizAttempt(quiz.quizId).body

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("exhausted [1] available attempt")
        e.message.contains("quizId:${quiz.quizId}")
        e.message.contains("errorCode:${ErrorCode.UserQuizAttemptsExhausted}")
    }

    def "each user has their own attempt count"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MaxNumAttempts.setting, value: '1'],
        ])

        when:
        // user 1
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        SkillsService user2Service = createService(getRandomUsers(1)[0])
        def u2QuizAttempt =  user2Service.startQuizAttempt(quiz.quizId).body
        user2Service.reportQuizAnswer(quiz.quizId, u2QuizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        def u2GradedQuizAttempt = user2Service.completeQuizAttempt(quiz.quizId, u2QuizAttempt.id).body

        then:
        gradedQuizAttempt.passed == false
        u2GradedQuizAttempt.passed == false
    }

    def "only 2 attempts are allowed"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MaxNumAttempts.setting, value: '2'],
        ])

        // attempt 1
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.passed == false

        // attempt 2
        def quizAttempt2 =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt2.id, quizAttempt.questions[0].answerOptions[1].id)
        def gradedQuizAttempt2 = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt2.id).body
        assert gradedQuizAttempt2.passed == false
        when:
        skillsService.startQuizAttempt(quiz.quizId).body

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("exhausted [2] available attempts")
        e.message.contains("quizId:${quiz.quizId}")
        e.message.contains("errorCode:${ErrorCode.UserQuizAttemptsExhausted}")
    }

    def "MaxNumAttempts=-1 means unlimited attempts"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MaxNumAttempts.setting, value: '-1'],
        ])

        when:
        List passedResults = (1..5).collect {
            def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
            skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
            def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
            return gradedQuizAttempt.passed
        }

        def finalAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, finalAttempt.id, finalAttempt.questions[0].answerOptions[0].id)
        def gradedFinalAttempt = skillsService.completeQuizAttempt(quiz.quizId, finalAttempt.id).body

        then:
        passedResults == [false, false, false, false, false]
        gradedFinalAttempt.passed == true
    }

    def "by default there is unlimited # of attempts"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        List passedResults = (1..5).collect {
            def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
            skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
            def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
            return gradedQuizAttempt.passed
        }

        def finalAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, finalAttempt.id, finalAttempt.questions[0].answerOptions[0].id)
        def gradedFinalAttempt = skillsService.completeQuizAttempt(quiz.quizId, finalAttempt.id).body

        then:
        passedResults == [false, false, false, false, false]
        gradedFinalAttempt.passed == true
    }

    def "surveys only allow 1 attempt"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 2)]
        skillsService.createQuizQuestionDefs(questions)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        when:
        skillsService.startQuizAttempt(quiz.quizId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("has already taken this survey")
        e.message.contains("quizId:${quiz.quizId}")
    }

    def "passed quiz cannot be run again"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.passed == true
        when:
        skillsService.startQuizAttempt(quiz.quizId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("already took and passed this quiz")
        e.message.contains("quizId:${quiz.quizId}")
    }

    def "quiz info returns attempt information"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        // attempt 1
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        def quizInfo_t1 = skillsService.getQuizInfo(quiz.quizId)

        // attempt 2
        def quizAttempt2 =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt2.id, quizAttempt.questions[0].answerOptions[1].id)
        def gradedQuizAttempt2 = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt2.id).body

        def quizInfo_t2 = skillsService.getQuizInfo(quiz.quizId)

        // attempt 2
        def quizAttempt3 =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt3.id, quizAttempt.questions[0].answerOptions[0].id)

        def quizInfo_t3 = skillsService.getQuizInfo(quiz.quizId)
        def gradedQuizAttempt3 = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt3.id).body
        def quizInfo_t4 = skillsService.getQuizInfo(quiz.quizId)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MaxNumAttempts.setting, value: '3'],
        ])
        def quizInfo_t5 = skillsService.getQuizInfo(quiz.quizId)

        then:
        gradedQuizAttempt.passed == false
        gradedQuizAttempt2.passed == false
        gradedQuizAttempt3.passed == true

        quizInfo.isAttemptAlreadyInProgress == false
        quizInfo.userNumPreviousQuizAttempts == 0
        quizInfo.userQuizPassed == false
        quizInfo.userLastQuizAttemptDate == null
        quizInfo.maxAttemptsAllowed == -1

        quizInfo_t1.isAttemptAlreadyInProgress == false
        quizInfo_t1.userNumPreviousQuizAttempts == 1
        quizInfo_t1.userQuizPassed == false
        quizInfo_t1.userLastQuizAttemptDate
        quizInfo_t1.maxAttemptsAllowed == -1

        quizInfo_t2.isAttemptAlreadyInProgress == false
        quizInfo_t2.userNumPreviousQuizAttempts == 2
        quizInfo_t2.userQuizPassed == false
        quizInfo_t2.userLastQuizAttemptDate
        quizInfo_t2.maxAttemptsAllowed == -1

        quizInfo_t3.isAttemptAlreadyInProgress == true
        quizInfo_t3.userNumPreviousQuizAttempts == 2
        quizInfo_t3.userQuizPassed == false
        quizInfo_t3.userLastQuizAttemptDate
        quizInfo_t3.maxAttemptsAllowed == -1

        quizInfo_t4.isAttemptAlreadyInProgress == false
        quizInfo_t4.userNumPreviousQuizAttempts == 3
        quizInfo_t4.userQuizPassed == true
        quizInfo_t4.userLastQuizAttemptDate
        quizInfo_t4.maxAttemptsAllowed == -1

        quizInfo_t5.isAttemptAlreadyInProgress == false
        quizInfo_t5.userNumPreviousQuizAttempts == 3
        quizInfo_t5.userQuizPassed == true
        quizInfo_t5.userLastQuizAttemptDate
        quizInfo_t5.maxAttemptsAllowed == 3
    }

}
