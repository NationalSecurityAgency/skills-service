/**
 * Copyright 2024 SkillTree
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

import groovy.json.JsonOutput
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.storage.model.UserQuizAttempt
import skills.storage.model.UserQuizQuestionAttempt

class QuizGradingSpecs extends DefaultIntSpec {

    def "grading information is returned from complete quiz endpoint"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])

        // quiz without text input
        def quiz1 = QuizDefFactory.createQuiz(2, "Fancy Description")
        skillsService.createQuizDef(quiz1)
        skillsService.createQuizQuestionDefs([QuizDefFactory.createChoiceQuestion(2, 1)])

        def quizInfo1 = skillsService.getQuizInfo(quiz1.quizId)
        def quizAttempt1 = skillsService.startQuizAttempt(quiz1.quizId).body
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt1.id, quizInfo1.questions[0].answerOptions[1].id)

        // quiz with multiple questions to grade
        def quiz2 = QuizDefFactory.createQuiz(3, "Fancy Description")
        skillsService.createQuizDef(quiz2)
        skillsService.createQuizQuestionDefs([
                QuizDefFactory.createTextInputQuestion(3, 1),
                QuizDefFactory.createChoiceQuestion(3, 1),
                QuizDefFactory.createTextInputQuestion(3, 1),
        ])
        skillsService.saveQuizSettings(quiz2.quizId, [
                [setting: QuizSettings.AlwaysShowCorrectAnswers.setting, value: '1'],
        ])

        def quizInfo2 = skillsService.getQuizInfo(quiz2.quizId)
        def quizAttempt2 = skillsService.startQuizAttempt(quiz2.quizId).body
        skillsService.reportQuizAnswer(quiz2.quizId, quizAttempt2.id, quizInfo2.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        skillsService.reportQuizAnswer(quiz2.quizId, quizAttempt2.id, quizInfo2.questions[1].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz2.quizId, quizAttempt2.id, quizInfo2.questions[2].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])

        when:
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        def gradedQuizAttempt1 = skillsService.completeQuizAttempt(quiz1.quizId, quizAttempt1.id).body
        def gradedQuizAttempt2 = skillsService.completeQuizAttempt(quiz2.quizId, quizAttempt2.id).body

        then:
        gradedQuizAttempt.needsGrading == true
        gradedQuizAttempt.numQuestionsNeedGrading == 1
        !gradedQuizAttempt.gradedQuestions

        gradedQuizAttempt1.needsGrading == false
        gradedQuizAttempt1.numQuestionsNeedGrading == 0
        !gradedQuizAttempt1.gradedQuestions

        gradedQuizAttempt2.needsGrading == true
        gradedQuizAttempt2.numQuestionsNeedGrading == 2
        gradedQuizAttempt2.gradedQuestions.status == [
                UserQuizQuestionAttempt.QuizQuestionStatus.NEEDS_GRADING.toString(),
                UserQuizQuestionAttempt.QuizQuestionStatus.CORRECT.toString(),
                UserQuizQuestionAttempt.QuizQuestionStatus.NEEDS_GRADING.toString(),
        ]
    }

    def "cannot start a quiz which is currently waiting grading"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MultipleTakes.setting, value: true],
        ])

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        skillsService.startQuizAttempt(quiz.quizId).body
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("There is currently a pending attempt")
        e.message.contains("that awaiting grading")
    }

    def "grade and fail a single question quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        def quizAttemptResBefore = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        skillsService.gradeAnswer(skillsService.userName, quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, false, "Good answer")
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        println JsonOutput.prettyPrint(JsonOutput.toJson(quizAttemptRes))
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
        quizAttemptRes.questions[0].answers.gradingResult.feedback == ["Good answer"]
        quizAttemptRes.questions[0].answers.gradingResult.graderUserId == [skillsService.userName]
        quizAttemptRes.questions[0].answers.gradingResult.gradedOn
    }

    def "grade and pass a single question quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        def quizAttemptResBefore = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        skillsService.gradeAnswer(skillsService.userName, quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, true, "Good answer")
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        println JsonOutput.prettyPrint(JsonOutput.toJson(quizAttemptRes))
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
        quizAttemptRes.questions[0].answers.gradingResult.graderUserId == [skillsService.userName]
        quizAttemptRes.questions[0].answers.gradingResult.gradedOn
    }

    def "grade and pass with partial requirement"() {
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

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, [isSelected: true, answerText: '3rd question ansewr'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        when:
        def quizAttemptResBefore = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        skillsService.gradeAnswer(skillsService.userName, quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, true, "Good answer")
        skillsService.gradeAnswer(skillsService.userName, quiz.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, false, "Good answer 3")
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        println JsonOutput.prettyPrint(JsonOutput.toJson(quizAttemptResBefore))
        println JsonOutput.prettyPrint(JsonOutput.toJson(quizAttemptRes))
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
        quizAttemptRes.questions[0].answers.gradingResult.feedback == ["Good answer"]
        quizAttemptRes.questions[0].answers.gradingResult.graderUserId == [skillsService.userName]
        quizAttemptRes.questions[0].answers.gradingResult.gradedOn

        quizAttemptRes.questions[1].answers.needsGrading == [false, false]
        quizAttemptRes.questions[1].answers.gradingResult == [null, null]

        quizAttemptRes.questions[2].answers.needsGrading == [false]
        quizAttemptRes.questions[2].answers.gradingResult.feedback == ["Good answer 3"]
        quizAttemptRes.questions[2].answers.gradingResult.graderUserId == [skillsService.userName]
        quizAttemptRes.questions[2].answers.gradingResult.gradedOn
    }

    def "cannot grade quiz that was already graded"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.AlwaysShowCorrectAnswers.setting, value: '1'],
        ])

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        skillsService.gradeAnswer(skillsService.userName, quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, false, "Good answer")
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        assert quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.FAILED.toString()

        when:
        skillsService.gradeAnswer(skillsService.userName, quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, false, "Good answer")
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Provided quiz attempt id [${quizAttempt.id}] was already completed")
    }

    def "grading attempt where user does not match"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.AlwaysShowCorrectAnswers.setting, value: '1'],
        ])

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        SkillsService anotherUser = createService("anotherUser")
        def quizAttempt1 = anotherUser.startQuizAttempt(quiz.quizId).body
        anotherUser.reportQuizAnswer(quiz.quizId, quizAttempt1.id, quizInfo.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        def gradedQuizAttempt1 = anotherUser.completeQuizAttempt(quiz.quizId, quizAttempt1.id).body
        assert gradedQuizAttempt1.needsGrading == true

        when:
        skillsService.gradeAnswer(anotherUser.userName, quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, false, "Good answer")
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Provided quiz attempt id [${quizAttempt.id}] is not for [${anotherUser.userName}] user")
    }

    def "grading attempt where attempt id is for another quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.AlwaysShowCorrectAnswers.setting, value: '1'],
        ])

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        def quiz2 = QuizDefFactory.createQuiz(2, "Fancy Description")
        skillsService.createQuizDef(quiz2)
        skillsService.createQuizQuestionDefs([ QuizDefFactory.createTextInputQuestion(2, 1)])
        def quizAttempt2 = skillsService.startQuizAttempt(quiz2.quizId).body

        when:
        skillsService.gradeAnswer(skillsService.userName, quiz.quizId, quizAttempt2.id, quizInfo.questions[0].answerOptions[0].id, false, "Good answer")
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Provided quiz attempt id [${quizAttempt2.id}] is not for [${quiz.quizId}] quiz")
    }

    def "grading attempt where question definition id is not for this quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.AlwaysShowCorrectAnswers.setting, value: '1'],
        ])

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.needsGrading == true

        def quiz2 = QuizDefFactory.createQuiz(2, "Fancy Description")
        skillsService.createQuizDef(quiz2)
        skillsService.createQuizQuestionDefs([ QuizDefFactory.createTextInputQuestion(2, 1)])
        def quizInfo2 = skillsService.getQuizInfo(quiz2.quizId)
        def quizAttempt2 = skillsService.startQuizAttempt(quiz2.quizId).body
        skillsService.reportQuizAnswer(quiz2.quizId, quizAttempt2.id, quizInfo2.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        def gradedQuizAttempt2 = skillsService.completeQuizAttempt(quiz2.quizId, quizAttempt2.id).body
        assert gradedQuizAttempt2.needsGrading == true

        when:
        skillsService.gradeAnswer(skillsService.userName, quiz.quizId, quizAttempt.id, quizInfo2.questions[0].answerOptions[0].id, false, "Good answer")
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Could not find answer attempt for quizAttemptId=[${quizAttempt.id}] and answerDefId=[${quizInfo2.questions[0].answerOptions[0].id}]")
    }
}