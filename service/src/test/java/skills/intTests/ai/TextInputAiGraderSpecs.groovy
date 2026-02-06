/**
 * Copyright 2025 SkillTree
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
package skills.intTests.ai

import groovy.util.logging.Slf4j
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.*
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import skills.controller.request.model.AiChatRequest
import skills.intTests.utils.CertificateRegistry
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.MockLlmServer
import skills.intTests.utils.QuizDefFactory
import skills.quizLoading.QuizSettings
import skills.storage.model.UserAttrs
import skills.storage.model.UserQuizAttempt
import spock.lang.IgnoreIf

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.time.Duration

@Slf4j
class TextInputAiGraderSpecs extends DefaultAiIntSpec {

    def setup() {
        if (!userAttrsRepo.findByUserIdIgnoreCase('ai-grader')) {
            UserAttrs userAttrs = new UserAttrs(userId: 'ai-grader',
                    userIdForDisplay: 'AI Assistant',
                    firstName: 'AI',
                    lastName: 'Grader',
                    userTagsLastUpdated: new Date())
            userAttrsRepo.save(userAttrs)
        }
    }

    def "AI grade text input and PASS - single question quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer.", 90, true)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer. this answer should pass'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        def quizAttemptResBefore = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptResBefore.status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING.toString()
        quizAttemptResBefore.questions.isCorrect == [false]
        quizAttemptResBefore.questions.needsGrading == [true]
        quizAttemptResBefore.questions[0].answers.needsGrading == [true]
        quizAttemptResBefore.questions[0].answers.gradingResult == [null]
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        quizAttemptRes.questions.isCorrect == [true]
        quizAttemptRes.questions.needsGrading == [false]
        quizAttemptRes.questions[0].answers.needsGrading == [false]
        quizAttemptRes.questions[0].answers.gradingResult.feedback == ["The student's answer demonstrates excellent understanding and closely matches the correct answer."]
        quizAttemptRes.questions[0].answers.gradingResult.aiConfidenceLevel == [90]
        quizAttemptRes.questions[0].answers.gradingResult.graderUserId == ['ai-grader']
        quizAttemptRes.questions[0].answers.gradingResult.gradedOn
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptCount == [1]
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptsLeft == [3]
        quizAttemptRes.questions[0].answers.aiGradingStatus.hasFailedAttempts == [false]
        quizAttemptRes.questions[0].answers.aiGradingStatus.failed == [false]
    }

    def "AI grade text input and PASS - multiple questions"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs([
                QuizDefFactory.createTextInputQuestion(1, 1),
                QuizDefFactory.createChoiceQuestion(1, 2),
        ])
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer 1", 90, true)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer 1. this answer should pass'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        def quizAttemptResBefore = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptResBefore.status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING.toString()
        quizAttemptResBefore.questions.isCorrect == [false, true]
        quizAttemptResBefore.questions.needsGrading == [true, false]
        quizAttemptResBefore.questions[0].answers.needsGrading == [true]
        quizAttemptResBefore.questions[0].answers.gradingResult == [null]
        quizAttemptResBefore.questions[1].answers.needsGrading == [false, false]
        quizAttemptResBefore.questions[1].answers.gradingResult == [null,  null]
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        quizAttemptRes.questions.isCorrect == [true, true]
        quizAttemptRes.questions.needsGrading == [false, false]
        quizAttemptRes.questions[0].answers.needsGrading == [false]
        quizAttemptRes.questions[0].answers.gradingResult.feedback == ["The student's answer demonstrates excellent understanding and closely matches the correct answer."]
        quizAttemptRes.questions[0].answers.gradingResult.graderUserId == ['ai-grader']
        quizAttemptRes.questions[0].answers.gradingResult.gradedOn

        quizAttemptRes.questions[1].answers.needsGrading == [false, false]
        quizAttemptRes.questions[1].answers.gradingResult == [null, null]
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptCount == [1]
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptsLeft == [3]
        quizAttemptRes.questions[0].answers.aiGradingStatus.hasFailedAttempts == [false]
        quizAttemptRes.questions[0].answers.aiGradingStatus.failed == [false]
    }

    def "AI grade text input and PASS with partial requirement"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs([
                QuizDefFactory.createTextInputQuestion(1, 1),
                QuizDefFactory.createChoiceQuestion(1, 2),
                QuizDefFactory.createTextInputQuestion(1, 3)
        ])
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '2'],
        ])
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer 1", 90, true)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[2].id, "This is the correct answer 3", 90, true)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer 1. this answer should pass'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer 1. this answer should fail'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        def quizAttemptResBefore = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptResBefore.status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING.toString()
        quizAttemptResBefore.questions.isCorrect == [false, true, false]
        quizAttemptResBefore.questions.needsGrading == [true, false, true]
        quizAttemptResBefore.questions[0].answers.needsGrading == [true]
        quizAttemptResBefore.questions[0].answers.gradingResult == [null]
        quizAttemptResBefore.questions[1].answers.needsGrading == [false, false]
        quizAttemptResBefore.questions[1].answers.gradingResult == [null,  null]
        quizAttemptResBefore.questions[2].answers.needsGrading == [true]
        quizAttemptResBefore.questions[2].answers.gradingResult == [null]
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        quizAttemptRes.questions.isCorrect == [true, true, false]
        quizAttemptRes.questions.needsGrading == [false, false, false]
        quizAttemptRes.questions[0].answers.needsGrading == [false]
        quizAttemptRes.questions[0].answers.gradingResult.feedback == ["The student's answer demonstrates excellent understanding and closely matches the correct answer."]
        quizAttemptRes.questions[0].answers.gradingResult.graderUserId == ['ai-grader']
        quizAttemptRes.questions[0].answers.gradingResult.gradedOn

        quizAttemptRes.questions[1].answers.needsGrading == [false, false]
        quizAttemptRes.questions[1].answers.gradingResult == [null, null]

        quizAttemptRes.questions[2].answers.needsGrading == [false]
        quizAttemptRes.questions[2].answers.gradingResult.feedback == ["The student's answer shows significant misunderstandings and does not match the correct answer."]
        quizAttemptRes.questions[2].answers.gradingResult.graderUserId == ['ai-grader']
        quizAttemptRes.questions[2].answers.gradingResult.gradedOn
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptCount == [1]
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptsLeft == [3]
        quizAttemptRes.questions[0].answers.aiGradingStatus.hasFailedAttempts == [false]
        quizAttemptRes.questions[0].answers.aiGradingStatus.failed == [false]
    }

    def "AI grade text input and PASS with partial requirement - multiple ai graded questions"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs([
                QuizDefFactory.createTextInputQuestion(1, 1),
                QuizDefFactory.createChoiceQuestion(1, 2),
                QuizDefFactory.createTextInputQuestion(1, 3)
        ])
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '2'],
        ])
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer 1", 90, true)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[2].id, "This is the correct answer 3", 90, true)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer 1. this answer should pass'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer 1. this answer should pass'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        def quizAttemptResBefore = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptResBefore.status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING.toString()
        quizAttemptResBefore.questions.isCorrect == [false, false, false]
        quizAttemptResBefore.questions.needsGrading == [true, false, true]
        quizAttemptResBefore.questions[0].answers.needsGrading == [true]
        quizAttemptResBefore.questions[0].answers.gradingResult == [null]
        quizAttemptResBefore.questions[1].answers.needsGrading == [false, false]
        quizAttemptResBefore.questions[1].answers.gradingResult == [null,  null]
        quizAttemptResBefore.questions[2].answers.needsGrading == [true]
        quizAttemptResBefore.questions[2].answers.gradingResult == [null]
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        quizAttemptRes.questions.isCorrect == [true, false, true]
        quizAttemptRes.questions.needsGrading == [false, false, false]
        quizAttemptRes.questions[0].answers.needsGrading == [false]
        quizAttemptRes.questions[0].answers.gradingResult.feedback == ["The student's answer demonstrates excellent understanding and closely matches the correct answer."]
        quizAttemptRes.questions[0].answers.gradingResult.graderUserId == ['ai-grader']
        quizAttemptRes.questions[0].answers.gradingResult.gradedOn

        quizAttemptRes.questions[1].answers.needsGrading == [false, false]
        quizAttemptRes.questions[1].answers.gradingResult == [null, null]

        quizAttemptRes.questions[2].answers.needsGrading == [false]
        quizAttemptRes.questions[2].answers.gradingResult.feedback == ["The student's answer demonstrates excellent understanding and closely matches the correct answer."]
        quizAttemptRes.questions[2].answers.gradingResult.graderUserId == ['ai-grader']
        quizAttemptRes.questions[2].answers.gradingResult.gradedOn
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptCount == [1]
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptsLeft == [3]
        quizAttemptRes.questions[0].answers.aiGradingStatus.hasFailedAttempts == [false]
        quizAttemptRes.questions[0].answers.aiGradingStatus.failed == [false]
    }

    def "AI grade text input and FAIL - single question quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer.", 90, true)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer. this answer should fail'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        def quizAttemptResBefore = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptResBefore.status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING.toString()
        quizAttemptResBefore.questions.isCorrect == [false]
        quizAttemptResBefore.questions.needsGrading == [true]
        quizAttemptResBefore.questions[0].answers.needsGrading == [true]
        quizAttemptResBefore.questions[0].answers.gradingResult == [null]
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.FAILED.toString()
        quizAttemptRes.questions.isCorrect == [false]
        quizAttemptRes.questions.needsGrading == [false]
        quizAttemptRes.questions[0].answers.needsGrading == [false]
        quizAttemptRes.questions[0].answers.gradingResult.feedback == ["The student's answer shows significant misunderstandings and does not match the correct answer."]
        quizAttemptRes.questions[0].answers.gradingResult.aiConfidenceLevel == [10]
        quizAttemptRes.questions[0].answers.gradingResult.graderUserId == ['ai-grader']
        quizAttemptRes.questions[0].answers.gradingResult.gradedOn
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptCount == [1]
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptsLeft == [3]
        quizAttemptRes.questions[0].answers.aiGradingStatus.hasFailedAttempts == [false]
        quizAttemptRes.questions[0].answers.aiGradingStatus.failed == [false]
    }

    def "AI grade text input and FAIL - multiple questions"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs([
                QuizDefFactory.createTextInputQuestion(1, 1),
                QuizDefFactory.createChoiceQuestion(1, 2),
        ])
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer 1", 90, true)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer 1. this answer should fail'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        def quizAttemptResBefore = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptResBefore.status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING.toString()
        quizAttemptResBefore.questions.isCorrect == [false, true]
        quizAttemptResBefore.questions.needsGrading == [true, false]
        quizAttemptResBefore.questions[0].answers.needsGrading == [true]
        quizAttemptResBefore.questions[0].answers.gradingResult == [null]
        quizAttemptResBefore.questions[1].answers.needsGrading == [false, false]
        quizAttemptResBefore.questions[1].answers.gradingResult == [null,  null]
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.FAILED.toString()
        quizAttemptRes.questions.isCorrect == [false, true]
        quizAttemptRes.questions.needsGrading == [false, false]
        quizAttemptRes.questions[0].answers.needsGrading == [false]
        quizAttemptRes.questions[0].answers.gradingResult.feedback == ["The student's answer shows significant misunderstandings and does not match the correct answer."]
        quizAttemptRes.questions[0].answers.gradingResult.graderUserId == ['ai-grader']
        quizAttemptRes.questions[0].answers.gradingResult.gradedOn

        quizAttemptRes.questions[1].answers.needsGrading == [false, false]
        quizAttemptRes.questions[1].answers.gradingResult == [null, null]
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptCount == [1]
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptsLeft == [3]
        quizAttemptRes.questions[0].answers.aiGradingStatus.hasFailedAttempts == [false]
        quizAttemptRes.questions[0].answers.aiGradingStatus.failed == [false]
    }

    def "AI grade text input and FAIL with partial requirement"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs([
                QuizDefFactory.createTextInputQuestion(1, 1),
                QuizDefFactory.createChoiceQuestion(1, 2),
                QuizDefFactory.createTextInputQuestion(1, 3)
        ])
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '2'],
        ])
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer 1", 90, true)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[2].id, "This is the correct answer 3", 90, true)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer 1. this answer should fail'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer 1. this answer should fail'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        def quizAttemptResBefore = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptResBefore.status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING.toString()
        quizAttemptResBefore.questions.isCorrect == [false, true, false]
        quizAttemptResBefore.questions.needsGrading == [true, false, true]
        quizAttemptResBefore.questions[0].answers.needsGrading == [true]
        quizAttemptResBefore.questions[0].answers.gradingResult == [null]
        quizAttemptResBefore.questions[1].answers.needsGrading == [false, false]
        quizAttemptResBefore.questions[1].answers.gradingResult == [null,  null]
        quizAttemptResBefore.questions[2].answers.needsGrading == [true]
        quizAttemptResBefore.questions[2].answers.gradingResult == [null]
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.FAILED.toString()
        quizAttemptRes.questions.isCorrect == [false, true, false]
        quizAttemptRes.questions.needsGrading == [false, false, false]
        quizAttemptRes.questions[0].answers.needsGrading == [false]
        quizAttemptRes.questions[0].answers.gradingResult.feedback == ["The student's answer shows significant misunderstandings and does not match the correct answer."]
        quizAttemptRes.questions[0].answers.gradingResult.graderUserId == ['ai-grader']
        quizAttemptRes.questions[0].answers.gradingResult.gradedOn

        quizAttemptRes.questions[1].answers.needsGrading == [false, false]
        quizAttemptRes.questions[1].answers.gradingResult == [null, null]

        quizAttemptRes.questions[2].answers.needsGrading == [false]
        quizAttemptRes.questions[2].answers.gradingResult.feedback == ["The student's answer shows significant misunderstandings and does not match the correct answer."]
        quizAttemptRes.questions[2].answers.gradingResult.graderUserId == ['ai-grader']
        quizAttemptRes.questions[2].answers.gradingResult.gradedOn
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptCount == [1]
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptsLeft == [3]
        quizAttemptRes.questions[0].answers.aiGradingStatus.hasFailedAttempts == [false]
        quizAttemptRes.questions[0].answers.aiGradingStatus.failed == [false]
    }
    
    def "AI grade text input and FAIL because confidence is too low"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer.", 95, true)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer. this answer should pass'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        def quizAttemptResBefore = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptResBefore.status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING.toString()
        quizAttemptResBefore.questions.isCorrect == [false]
        quizAttemptResBefore.questions.needsGrading == [true]
        quizAttemptResBefore.questions[0].answers.needsGrading == [true]
        quizAttemptResBefore.questions[0].answers.gradingResult == [null]
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.FAILED.toString()
        quizAttemptRes.questions.isCorrect == [false]
        quizAttemptRes.questions.needsGrading == [false]
        quizAttemptRes.questions[0].answers.needsGrading == [false]
        quizAttemptRes.questions[0].answers.gradingResult.feedback == ["The student's answer demonstrates excellent understanding and closely matches the correct answer."]
        quizAttemptRes.questions[0].answers.gradingResult.aiConfidenceLevel == [90]
        quizAttemptRes.questions[0].answers.gradingResult.graderUserId == ['ai-grader']
        quizAttemptRes.questions[0].answers.gradingResult.gradedOn
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptCount == [1]
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptsLeft == [3]
        quizAttemptRes.questions[0].answers.aiGradingStatus.hasFailedAttempts == [false]
        quizAttemptRes.questions[0].answers.aiGradingStatus.failed == [false]
    }
    
    def "AI grade text input and ERROR because confidence not returned from LLM"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer. no-confidenceLevel", 95, true)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer. this answer should fail'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        def quizAttemptResBefore = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptResBefore.status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING.toString()
        quizAttemptResBefore.questions.isCorrect == [false]
        quizAttemptResBefore.questions.needsGrading == [true]
        quizAttemptResBefore.questions[0].answers.needsGrading == [true]
        quizAttemptResBefore.questions[0].answers.gradingResult == [null]
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING.toString()
        quizAttemptRes.questions.isCorrect == [false]
        quizAttemptRes.questions.needsGrading == [true]
        quizAttemptRes.questions[0].answers.needsGrading == [true]
        quizAttemptRes.questions[0].answers.gradingResult == [null]
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptCount == [4]
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptsLeft == [0]
        quizAttemptRes.questions[0].answers.aiGradingStatus.hasFailedAttempts == [true]
        quizAttemptRes.questions[0].answers.aiGradingStatus.failed == [true]
    }
}
