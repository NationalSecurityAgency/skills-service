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


import org.springframework.beans.factory.annotation.Value
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.services.quiz.QuizQuestionType
import skills.storage.model.QuizDefParent
import skills.storage.model.UserAttrs
import skills.storage.model.UserQuizAttempt

class QuizGradedResSpecs extends DefaultIntSpec {

    @Value('${skills.config.ui.usersTableAdditionalUserTagKey}')
    String usersTableAdditionalUserTagKey

    def "quiz: get completed graded quiz - passed"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptRes.userId == userAttrs.userId
        quizAttemptRes.userIdForDisplay == userAttrs.userIdForDisplay
        quizAttemptRes.quizType == QuizDefParent.QuizType.Quiz.toString()
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        quizAttemptRes.started
        quizAttemptRes.completed
        !quizAttemptRes.userTag
        quizAttemptRes.questions.id == quizAttempt.questions.id
        quizAttemptRes.questions.question == questions.question
        quizAttemptRes.questions.questionType == questions.questionType
        quizAttemptRes.questions.isCorrect == [true, true]

        quizAttemptRes.questions[0].answers.answer == questions[0].answers.answer
        quizAttemptRes.questions[0].answers.isConfiguredCorrect == [true, false]
        quizAttemptRes.questions[0].answers.isSelected == [true, false]

        quizAttemptRes.questions[1].answers.answer == questions[1].answers.answer
        quizAttemptRes.questions[1].answers.isConfiguredCorrect == [true, false]
        quizAttemptRes.questions[1].answers.isSelected == [true, false]
    }

    def "quiz: get completed graded quiz - passed - multiple types of questions"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 2, 2, QuizQuestionType.SingleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.MultipleChoice)
                ]
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[2].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptRes.userId == userAttrs.userId
        quizAttemptRes.userIdForDisplay == userAttrs.userIdForDisplay
        quizAttemptRes.quizType == QuizDefParent.QuizType.Quiz.toString()
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        quizAttemptRes.questions.id == quizAttempt.questions.id
        quizAttemptRes.questions.question == questions.question
        quizAttemptRes.questions.questionType == [QuizQuestionType.SingleChoice.toString(), QuizQuestionType.MultipleChoice.toString()]
        quizAttemptRes.questions.isCorrect == [true, true]
        quizAttemptRes.questions[0].answers.answer == questions[0].answers.answer
        quizAttemptRes.questions[0].answers.isConfiguredCorrect == [true, false]
        quizAttemptRes.questions[0].answers.isSelected == [true, false]

        quizAttemptRes.questions[1].answers.answer == questions[1].answers.answer
        quizAttemptRes.questions[1].answers.isConfiguredCorrect == [true, false, true, false]
        quizAttemptRes.questions[1].answers.isSelected == [true, false, true, false]

    }

    def "quiz: get completed graded quiz - failed - multiple types of questions - SingleChoice is wrong"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 2, 2, QuizQuestionType.SingleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.MultipleChoice)
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[2].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptRes.userId == userAttrs.userId
        quizAttemptRes.userIdForDisplay == userAttrs.userIdForDisplay
        quizAttemptRes.quizType == QuizDefParent.QuizType.Quiz.toString()
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.FAILED.toString()
        quizAttemptRes.questions.id == quizAttempt.questions.id
        quizAttemptRes.questions.question == questions.question
        quizAttemptRes.questions.questionType == [QuizQuestionType.SingleChoice.toString(), QuizQuestionType.MultipleChoice.toString()]
        quizAttemptRes.questions.isCorrect == [false, true]
        quizAttemptRes.questions[0].answers.answer == questions[0].answers.answer
        quizAttemptRes.questions[0].answers.isConfiguredCorrect == [true, false]
        quizAttemptRes.questions[0].answers.isSelected == [false, true]

        quizAttemptRes.questions[1].answers.answer == questions[1].answers.answer
        quizAttemptRes.questions[1].answers.isConfiguredCorrect == [true, false, true, false]
        quizAttemptRes.questions[1].answers.isSelected == [true, false, true, false]

    }
    def "quiz: get completed graded quiz - failed - multiple types of questions - MultipleChoice is wrong"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 2, 2, QuizQuestionType.SingleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.MultipleChoice)
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[2].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptRes.userId == userAttrs.userId
        quizAttemptRes.userIdForDisplay == userAttrs.userIdForDisplay
        quizAttemptRes.quizType == QuizDefParent.QuizType.Quiz.toString()
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.FAILED.toString()
        quizAttemptRes.questions.id == quizAttempt.questions.id
        quizAttemptRes.questions.question == questions.question
        quizAttemptRes.questions.questionType == [QuizQuestionType.SingleChoice.toString(), QuizQuestionType.MultipleChoice.toString()]
        quizAttemptRes.questions.isCorrect == [true, false]
        quizAttemptRes.questions[0].answers.answer == questions[0].answers.answer
        quizAttemptRes.questions[0].answers.isConfiguredCorrect == [true, false]
        quizAttemptRes.questions[0].answers.isSelected == [true, false]

        quizAttemptRes.questions[1].answers.answer == questions[1].answers.answer
        quizAttemptRes.questions[1].answers.isConfiguredCorrect == [true, false, true, false]
        quizAttemptRes.questions[1].answers.isSelected == [false, true, true, false]
    }

    def "quiz: get completed graded quiz - failed - multiple types of questions - MultipleChoice is wrong via extra selection"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 2, 2, QuizQuestionType.SingleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.MultipleChoice)
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[2].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[3].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptRes.userId == userAttrs.userId
        quizAttemptRes.userIdForDisplay == userAttrs.userIdForDisplay
        quizAttemptRes.quizType == QuizDefParent.QuizType.Quiz.toString()
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.FAILED.toString()
        quizAttemptRes.questions.id == quizAttempt.questions.id
        quizAttemptRes.questions.question == questions.question
        quizAttemptRes.questions.questionType == [QuizQuestionType.SingleChoice.toString(), QuizQuestionType.MultipleChoice.toString()]
        quizAttemptRes.questions.isCorrect == [true, false]
        quizAttemptRes.questions[0].answers.answer == questions[0].answers.answer
        quizAttemptRes.questions[0].answers.isConfiguredCorrect == [true, false]
        quizAttemptRes.questions[0].answers.isSelected == [true, false]

        quizAttemptRes.questions[1].answers.answer == questions[1].answers.answer
        quizAttemptRes.questions[1].answers.isConfiguredCorrect == [true, false, true, false]
        quizAttemptRes.questions[1].answers.isSelected == [true, false, true, true]
    }

    def "quiz: get completed graded quiz - in progress - multiple types of questions - all questions right"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 2, 2, QuizQuestionType.SingleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.MultipleChoice)
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[2].id)

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptRes.userId == userAttrs.userId
        quizAttemptRes.userIdForDisplay == userAttrs.userIdForDisplay
        quizAttemptRes.quizType == QuizDefParent.QuizType.Quiz.toString()
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.INPROGRESS.toString()
        quizAttemptRes.questions.id == quizAttempt.questions.id
        quizAttemptRes.questions.question == questions.question
        quizAttemptRes.questions.questionType == [QuizQuestionType.SingleChoice.toString(), QuizQuestionType.MultipleChoice.toString()]
        quizAttemptRes.questions.isCorrect == [true, true]
        quizAttemptRes.questions[0].answers.answer == questions[0].answers.answer
        quizAttemptRes.questions[0].answers.isConfiguredCorrect == [true, false]
        quizAttemptRes.questions[0].answers.isSelected == [true, false]

        quizAttemptRes.questions[1].answers.answer == questions[1].answers.answer
        quizAttemptRes.questions[1].answers.isConfiguredCorrect == [true, false, true, false]
        quizAttemptRes.questions[1].answers.isSelected == [true, false, true, false]
    }

    def "quiz: get completed graded quiz - in progress - multiple types of questions - all questions wrong"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 2, 2, QuizQuestionType.SingleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.MultipleChoice)
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptRes.userId == userAttrs.userId
        quizAttemptRes.userIdForDisplay == userAttrs.userIdForDisplay
        quizAttemptRes.quizType == QuizDefParent.QuizType.Quiz.toString()
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.INPROGRESS.toString()
        quizAttemptRes.questions.id == quizAttempt.questions.id
        quizAttemptRes.questions.question == questions.question
        quizAttemptRes.questions.questionType == [QuizQuestionType.SingleChoice.toString(), QuizQuestionType.MultipleChoice.toString()]
        quizAttemptRes.questions.isCorrect == [false, false]
        quizAttemptRes.questions[0].answers.answer == questions[0].answers.answer
        quizAttemptRes.questions[0].answers.isConfiguredCorrect == [true, false]
        quizAttemptRes.questions[0].answers.isSelected == [false, true]

        quizAttemptRes.questions[1].answers.answer == questions[1].answers.answer
        quizAttemptRes.questions[1].answers.isConfiguredCorrect == [true, false, true, false]
        quizAttemptRes.questions[1].answers.isSelected == [true, false, false, false]
    }

    def "quiz: get completed graded quiz - in progress - multiple types of questions - one question not selected"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 2, 2, QuizQuestionType.SingleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.MultipleChoice)
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptRes.userId == userAttrs.userId
        quizAttemptRes.userIdForDisplay == userAttrs.userIdForDisplay
        quizAttemptRes.quizType == QuizDefParent.QuizType.Quiz.toString()
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.INPROGRESS.toString()
        quizAttemptRes.questions.id == quizAttempt.questions.id
        quizAttemptRes.questions.question == questions.question
        quizAttemptRes.questions.questionType == [QuizQuestionType.SingleChoice.toString(), QuizQuestionType.MultipleChoice.toString()]
        quizAttemptRes.questions.isCorrect == [false, false]
        quizAttemptRes.questions[0].answers.answer == questions[0].answers.answer
        quizAttemptRes.questions[0].answers.isConfiguredCorrect == [true, false]
        quizAttemptRes.questions[0].answers.isSelected == [false, true]

        quizAttemptRes.questions[1].answers.answer == questions[1].answers.answer
        quizAttemptRes.questions[1].answers.isConfiguredCorrect == [true, false, true, false]
        quizAttemptRes.questions[1].answers.isSelected == [false, false, false, false]
    }

    def "survey: get completed"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4),
                QuizDefFactory.createTextInputSurveyQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[2].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[3].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'This is user provided answer'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptRes.userId == userAttrs.userId
        quizAttemptRes.userIdForDisplay == userAttrs.userIdForDisplay
        quizAttemptRes.quizType == QuizDefParent.QuizType.Survey.toString()
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        quizAttemptRes.questions.id == quizAttempt.questions.id
        quizAttemptRes.questions.question == questions.question
        quizAttemptRes.questions.questionType == [ QuizQuestionType.MultipleChoice.toString(), QuizQuestionType.SingleChoice.toString(), QuizQuestionType.TextInput.toString()]
        quizAttemptRes.questions.isCorrect == [true, true, true]

        quizAttemptRes.questions[0].answers.answer == questions[0].answers.answer
        quizAttemptRes.questions[0].answers.isConfiguredCorrect == [false, false, false]
        quizAttemptRes.questions[0].answers.isSelected == [false, true, true]

        quizAttemptRes.questions[1].answers.answer == questions[1].answers.answer
        quizAttemptRes.questions[1].answers.isConfiguredCorrect == [false, false, false, false]
        quizAttemptRes.questions[1].answers.isSelected == [false, false, false, true]

        quizAttemptRes.questions[2].answers.answer == ['This is user provided answer']
        quizAttemptRes.questions[2].answers.isConfiguredCorrect == [false]
        quizAttemptRes.questions[2].answers.isSelected == [true]
    }

    def "survey: get in progress - some questions answered"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4),
                QuizDefFactory.createTextInputSurveyQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[2].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'This is user provided answer'])

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptRes.userId == userAttrs.userId
        quizAttemptRes.userIdForDisplay == userAttrs.userIdForDisplay
        quizAttemptRes.quizType == QuizDefParent.QuizType.Survey.toString()
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.INPROGRESS.toString()
        quizAttemptRes.questions.id == quizAttempt.questions.id
        quizAttemptRes.questions.question == questions.question
        quizAttemptRes.questions.questionType == [ QuizQuestionType.MultipleChoice.toString(), QuizQuestionType.SingleChoice.toString(), QuizQuestionType.TextInput.toString()]
        quizAttemptRes.questions.isCorrect == [true, true, true]

        quizAttemptRes.questions[0].answers.answer == questions[0].answers.answer
        quizAttemptRes.questions[0].answers.isConfiguredCorrect == [false, false, false]
        quizAttemptRes.questions[0].answers.isSelected == [false, true, true]

        quizAttemptRes.questions[1].answers.answer == questions[1].answers.answer
        quizAttemptRes.questions[1].answers.isConfiguredCorrect == [false, false, false, false]
        quizAttemptRes.questions[1].answers.isSelected == [false, false, false, false]

        quizAttemptRes.questions[2].answers.answer == ['This is user provided answer']
        quizAttemptRes.questions[2].answers.isConfiguredCorrect == [false]
        quizAttemptRes.questions[2].answers.isSelected == [true]
    }

    def "survey: get in progress - no questions answered"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4),
                QuizDefFactory.createTextInputSurveyQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptRes.userId == userAttrs.userId
        quizAttemptRes.userIdForDisplay == userAttrs.userIdForDisplay
        quizAttemptRes.quizType == QuizDefParent.QuizType.Survey.toString()
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.INPROGRESS.toString()
        quizAttemptRes.questions.id == quizAttempt.questions.id
        quizAttemptRes.questions.question == questions.question
        quizAttemptRes.questions.questionType == [ QuizQuestionType.MultipleChoice.toString(), QuizQuestionType.SingleChoice.toString(), QuizQuestionType.TextInput.toString()]
        quizAttemptRes.questions.isCorrect == [true, true, true]

        quizAttemptRes.questions[0].answers.answer == questions[0].answers.answer
        quizAttemptRes.questions[0].answers.isConfiguredCorrect == [false, false, false]
        quizAttemptRes.questions[0].answers.isSelected == [false, false, false]

        quizAttemptRes.questions[1].answers.answer == questions[1].answers.answer
        quizAttemptRes.questions[1].answers.isConfiguredCorrect == [false, false, false, false]
        quizAttemptRes.questions[1].answers.isSelected == [false, false, false, false]

        quizAttemptRes.questions[2].answers.answer == [null]
        quizAttemptRes.questions[2].answers.isConfiguredCorrect == [false]
        quizAttemptRes.questions[2].answers.isSelected == [false]
    }

    def "number of required questions to pass changed in the middle of the quiz run"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)


        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '1'],
        ])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '2'],
        ])
        def quizAttemptRes1 = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptRes.numQuestionsToPass == 2
        quizAttemptRes1.numQuestionsToPass == 1
    }

    def "quiz: get completed graded quiz with user tag"() {
        List<String> users = getRandomUsers(2, true)
        SkillsService rootSkillsService = createRootSkillService(users[0])
        SkillsService regularUser = createService(users[1])

        rootSkillsService.saveUserTag(regularUser.userName, usersTableAdditionalUserTagKey, ["ABC"])

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = regularUser.getQuizInfo(quiz.quizId)
        def quizAttempt =  regularUser.startQuizAttempt(quiz.quizId).body
        regularUser.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        regularUser.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        regularUser.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(regularUser.userName)
        when:
        def quizAttemptRes = skillsService.getQuizAttemptResult(quiz.quizId, quizAttempt.id)
        then:
        quizAttemptRes.userId == userAttrs.userId
        quizAttemptRes.userIdForDisplay == userAttrs.userIdForDisplay
        quizAttemptRes.quizType == QuizDefParent.QuizType.Quiz.toString()
        quizAttemptRes.status == UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        quizAttemptRes.started
        quizAttemptRes.completed
        quizAttemptRes.userTag == "ABC"
    }
}

