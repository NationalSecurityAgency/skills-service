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

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.storage.model.UserAttrs
import skills.storage.model.UserQuizAttempt
import skills.utils.WaitFor

@Slf4j
class TextInputAiGraderSpecs extends DefaultAiIntSpec {

    def setup() {
        startEmailServer()
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
        def quizRuns = skillsService.getQuizRuns(quiz.quizId)
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

        quizRuns.data.size() == 1
        quizRuns.data[0].status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING.toString()
        quizRuns.data[0].aiGradingStatus.questionId == [qRes.questions[0].id]
        quizRuns.data[0].aiGradingStatus.failed == [true]
        quizRuns.data[0].aiGradingStatus.hasFailedAttempts == [true]
        quizRuns.data[0].aiGradingStatus.attemptCount == [4]
        quizRuns.data[0].aiGradingStatus.attemptsLeft == [0]
    }

    def "AI grade text input - quiz runs with multiple states"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def q1 = QuizDefFactory.createTextInputQuestion(1, 1)
        def q2 = QuizDefFactory.createTextInputQuestion(1, 2)
        def q3 = QuizDefFactory.createChoiceQuestion(1, 3)
        def q4 = QuizDefFactory.createTextInputQuestion(1, 4)
        def q5 = QuizDefFactory.createTextInputQuestion(1, 5)
        skillsService.createQuizQuestionDefs([q1, q2, q3, q4, q5]).body
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)

        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer. no-confidenceLevel", 95, true)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[1].id, "This is the correct answer.", 95, true)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[4].id, "This is the correct answer. no-confidenceLevel", 95, true)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer. this answer should fail'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer. this answer should pass'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[3].answerOptions[0].id, [isSelected: true, answerText: 'Please grade me'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[4].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer. this answer should fail'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        SkillsService otherUser = createService(getRandomUsers(1)[0])
        def quizAttempt1 = otherUser.startQuizAttempt(quiz.quizId).body
        otherUser.reportQuizAnswer(quiz.quizId, quizAttempt1.id, quizAttempt1.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer. this answer should pass'])
        otherUser.reportQuizAnswer(quiz.quizId, quizAttempt1.id, quizAttempt1.questions[1].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer. this answer should pass'])
        otherUser.reportQuizAnswer(quiz.quizId, quizAttempt1.id, quizAttempt1.questions[2].answerOptions[0].id)
        otherUser.reportQuizAnswer(quiz.quizId, quizAttempt1.id, quizAttempt1.questions[3].answerOptions[0].id, [isSelected: true, answerText: 'Please grade me'])
        otherUser.reportQuizAnswer(quiz.quizId, quizAttempt1.id, quizAttempt1.questions[4].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer. this answer should pass'])
        def gradedQuizAttempt1 = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt1.id).body
        assert gradedQuizAttempt1.needsGrading == true

        when:
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def quizRuns = skillsService.getQuizRuns(quiz.quizId)

        println JsonOutput.prettyPrint(JsonOutput.toJson(quizRuns))
        then:
        quizRuns.data.size() == 2
        quizRuns.data[0].status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING.toString()
        quizRuns.data[0].aiGradingStatus.questionId == [qRes.questions[0].id, qRes.questions[4].id]
        quizRuns.data[0].aiGradingStatus.failed == [true, true]
        quizRuns.data[0].aiGradingStatus.hasFailedAttempts == [true, true]
        quizRuns.data[0].aiGradingStatus.attemptCount == [4, 4]
        quizRuns.data[0].aiGradingStatus.attemptsLeft == [0, 0]

        quizRuns.data[1].status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING.toString()
        !quizRuns.data[1].aiGradingStatus
    }


    def "Previously submitted answers will be sent for AI grading when AI grading gets enabled"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer. this answer should pass'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def quizAttemptResBefore = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)

        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer.", 90, true)

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

    def "Attempt to AI grade text input but was already graded manually"() {
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
        skillsService.gradeAnswer(skillsService.userName, quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, true, "Good answer")

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
        quizAttemptRes.questions[0].answers.gradingResult.feedback == ["Good answer"]
        quizAttemptRes.questions[0].answers.gradingResult.aiConfidenceLevel == [null]
        quizAttemptRes.questions[0].answers.gradingResult.graderUserId == [skillsService.userName]
        quizAttemptRes.questions[0].answers.gradingResult.gradedOn
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptCount == [4]
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptsLeft == [0]
        quizAttemptRes.questions[0].answers.aiGradingStatus.hasFailedAttempts == [true]
        quizAttemptRes.questions[0].answers.aiGradingStatus.failed == [true]
    }

    def "Attempt to manually grade text input but was already AI graded"() {
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

        skillsService.gradeAnswer(skillsService.userName, quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, true, "Good answer")
        then:

        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("was already completed")

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

    def "AI graded text input does not run custom validator"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer. add-jabberwocky", 90, true)

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
        quizAttemptRes.questions[0].answers.gradingResult.feedback == ["The student's answer demonstrates excellent understanding and closely matches the correct answer. Also, jabberwocky."]
        quizAttemptRes.questions[0].answers.gradingResult.aiConfidenceLevel == [90]
        quizAttemptRes.questions[0].answers.gradingResult.graderUserId == ['ai-grader']
        quizAttemptRes.questions[0].answers.gradingResult.gradedOn
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptCount == [1]
        quizAttemptRes.questions[0].answers.aiGradingStatus.attemptsLeft == [3]
        quizAttemptRes.questions[0].answers.aiGradingStatus.hasFailedAttempts == [false]
        quizAttemptRes.questions[0].answers.aiGradingStatus.failed == [false]
    }

    def "AI graded text input - quiz grade request is not sent for only AI graded questions, but graded notifications are still sent"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer.", 90, true)

        List<String> users = getRandomUsers(1, true)
        SkillsService testTaker = createService(users[0])

        def quizAttempt = testTaker.startQuizAttempt(quiz.quizId).body
        testTaker.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer. this answer should pass'])
        def gradedQuizAttempt = testTaker.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 1}
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail)

        then:
        emailRes
        emailRes.subj == "SkillTree Quiz Graded"
    }

    def "AI graded text input - quiz grade request and graded notifications are sent when at least one text input question is manually graded"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def aiGradedQuestion = QuizDefFactory.createTextInputQuestion(1, 1)
        def manuallyGradedQuestion = QuizDefFactory.createTextInputQuestion(1, 2)
        skillsService.createQuizQuestionDefs([aiGradedQuestion, manuallyGradedQuestion])
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer.", 90, true)

        List<String> users = getRandomUsers(1, true)
        SkillsService testTaker = createService(users[0])

        def quizAttempt = testTaker.startQuizAttempt(quiz.quizId).body
        testTaker.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer. this answer should pass'])
        testTaker.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer #2'])
        def gradedQuizAttempt = testTaker.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:

        skillsService.gradeAnswer(testTaker.userName, quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id, false, 'manual grader feedback')
        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 2 }
        List<EmailUtils.EmailRes> emailRes = EmailUtils.getEmails(greenMail)

        EmailUtils.EmailRes gradingRequest = emailRes.find {it.subj == "SkillTree Quiz Grading Requested" }
        EmailUtils.EmailRes gradedResponse = emailRes.find {it.subj == "SkillTree Quiz Graded" }

        then:
        gradingRequest
        gradedResponse
    }

    def "AI grade text input - failure notifications are sent on ERROR"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        skillsService.saveQuizTextInputAiGraderConfigs(quiz.quizId, qRes.questions[0].id, "This is the correct answer. no-gradingDecisionReason", 95, true)

        UserAttrs quizAdminUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)

        List<String> users = getRandomUsers(1, true)
        SkillsService testTaker = createService(users[0])
        def quizInfo = testTaker.getQuizInfo(quiz.quizId)

        def quizAttempt = testTaker.startQuizAttempt(quiz.quizId).body
        testTaker.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer. this answer should fail'])
        def gradedQuizAttempt = testTaker.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        def quizAttemptResBefore = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 1}
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail)

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

        emailRes
        emailRes.subj == "SkillTree Quiz AI Grading Failed!"
        emailRes.recipients == [quizAdminUserAttrs.email]
        emailRes.plainText.contains("SkillTree Quiz AI Grading Failed!")
        emailRes.plainText.contains("The automated AI grading system for the [${quizInfo.name}] quiz encountered an error and could not complete the evaluation. This quiz now requires manual grading intervention. As a quiz administrator, please review the submitted responses and determine whether the user should pass or fail this quiz attempt.")
        emailRes.plainText.contains("Grading URL: http://localhost:${localPort}/administrator/quizzes/${quiz.quizId}/grading")

        emailRes.html.contains("<h1>SkillTree Quiz AI Grading Failed!</h1>")
        emailRes.html.contains("<p>The automated AI grading system for the <b>${quizInfo.name}</b> quiz encountered an error and could not complete the evaluation. This quiz now requires manual grading intervention. As a quiz administrator, please review the submitted responses and determine whether the user should pass or fail this quiz attempt.</p>")
        emailRes.html.contains("<a href=\"http://localhost:${localPort}/administrator/quizzes/${quiz.quizId}/grading\" class=\"button\">Review Answers</a>")
    }
}
