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

import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.services.quiz.QuizQuestionType
import skills.storage.model.QuizDefParent

@Slf4j
class QuizDefSurveyManagementSpecs extends DefaultIntSpec {

    def "create quiz-survey definition"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)

        when:
        def newQuiz = skillsService.createQuizDef(quiz)

        def quizDefs = skillsService.getQuizDefs()

        then:
        newQuiz.body.quizId == quiz.quizId
        newQuiz.body.name == quiz.name
        newQuiz.body.type == QuizDefParent.QuizType.Survey.toString()

        quizDefs.quizId == [quiz.quizId]
        quizDefs.name == [quiz.name]
        quizDefs.type == [QuizDefParent.QuizType.Survey.toString()]
    }

    def "get quiz definition summary"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3, QuizDefParent.QuizType.Survey),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4, QuizDefParent.QuizType.Survey),
                QuizDefFactory.createTextInputQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        when:
        def res = skillsService.getQuizQuestionDefs(quiz.quizId)
        then:
        res.quizType == quiz.type
        res.questions.questionType == [QuizQuestionType.MultipleChoice.toString(), QuizQuestionType.SingleChoice.toString(), QuizQuestionType.TextInput.toString()]
        res.questions.question == questions.question

        res.questions[0].answers.isCorrect == [false, false, false]
        res.questions[0].answers.answer == questions[0].answers.answer

        res.questions[1].answers.isCorrect == [false, false, false, false]
        res.questions[1].answers.answer == questions[1].answers.answer

        res.questions[2].answers.isCorrect == [false]
        res.questions[2].answers.answer == [null]
    }

    def "add MultipleChoice question to survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        def newQuiz = skillsService.createQuizDef(quiz)

        def question = QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3)

        when:
        def newQuestion = skillsService.createQuizQuestionDef(question)

        then:
        newQuestion.body.id
        newQuestion.body.question == question.question
        newQuestion.body.questionType == QuizQuestionType.MultipleChoice.toString()
        newQuestion.body.answers.size() == 3
        newQuestion.body.answers[0].id
        newQuestion.body.answers[1].id
        newQuestion.body.answers[2].id
        newQuestion.body.answers.answer == question.answers.answer
        newQuestion.body.answers.isCorrect == [false, false, false]
    }

    def "add SingleChoice question to survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        def newQuiz = skillsService.createQuizDef(quiz)

        def question = QuizDefFactory.createSingleChoiceSurveyQuestion(1, 1, 2)

        when:
        def newQuestion = skillsService.createQuizQuestionDef(question)

        then:
        newQuestion.body.id
        newQuestion.body.question == question.question
        newQuestion.body.questionType == QuizQuestionType.SingleChoice.toString()
        newQuestion.body.answers.size() == 2
        newQuestion.body.answers[0].id
        newQuestion.body.answers[1].id
        newQuestion.body.answers.answer == question.answers.answer
        newQuestion.body.answers.isCorrect == [false, false]
    }

    def "add TextInput question to survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        def newQuiz = skillsService.createQuizDef(quiz)

        def question = QuizDefFactory.createTextInputQuestion(1, 1)

        when:
        def newQuestion = skillsService.createQuizQuestionDef(question)

        then:
        newQuestion.body.id
        newQuestion.body.question == question.question
        newQuestion.body.questionType == QuizQuestionType.TextInput.toString()
        newQuestion.body.answers.size() == 1
        newQuestion.body.answers[0].id
        newQuestion.body.answers.answer == [null]
        newQuestion.body.answers.isCorrect == [false]
    }

    def "update question definition - change answers text"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questionDef = QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4)
        skillsService.createQuizQuestionDef(QuizDefFactory.createSingleChoiceSurveyQuestion(1, 1, 3)).body
        def q2 = skillsService.createQuizQuestionDef(questionDef).body
        q2.question = "New Cool Questions?"
        q2.answers[0].answer = "New answer 1"
        q2.answers[2].answer = "New answer 3"
        q2.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)
        def updatedQuestions = skillsService.getQuizQuestionDefs(quiz.quizId)

        then:
        updatedQuestion.question == "New Cool Questions?"
        updatedQuestion.questionType == QuizQuestionType.SingleChoice.toString()
        updatedQuestion.answers.answer == ["New answer 1", "Answer #2", "New answer 3", "Answer #4"]
        updatedQuestion.answers.isCorrect == [false, false, false, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3, 4]

        updatedQuestions.questions.question == ["This is questions #1", "New Cool Questions?"]
        updatedQuestions.questions.questionType == [QuizQuestionType.SingleChoice.toString(), QuizQuestionType.SingleChoice.toString()]
        updatedQuestions.questions[1].answers.answer == ["New answer 1", "Answer #2", "New answer 3", "Answer #4"]
        updatedQuestions.questions[1].answers.isCorrect == [false, false, false, false]
        updatedQuestions.questions[1].answers.displayOrder == [1, 2, 3, 4]
    }

    def "update question definition - change question type - SingleChoice -> MultipleChoice"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questionDef = QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4)
        skillsService.createQuizQuestionDef(QuizDefFactory.createSingleChoiceSurveyQuestion(1, 1, 3)).body
        def q2 = skillsService.createQuizQuestionDef(questionDef).body
        q2.questionType = QuizQuestionType.MultipleChoice.toString()
        q2.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)
        def updatedQuestions = skillsService.getQuizQuestionDefs(quiz.quizId)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.questionType == QuizQuestionType.MultipleChoice.toString()
        updatedQuestion.answers.answer == ["Answer #1", "Answer #2", "Answer #3", "Answer #4"]
        updatedQuestion.answers.isCorrect == [false, false, false, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3, 4]

        updatedQuestions.questions.question == ["This is questions #1", "This is questions #2"]
        updatedQuestions.questions.questionType == [QuizQuestionType.SingleChoice.toString(), QuizQuestionType.MultipleChoice.toString()]
        updatedQuestions.questions[1].answers.answer == ["Answer #1", "Answer #2", "Answer #3", "Answer #4"]
        updatedQuestions.questions[1].answers.isCorrect == [false, false, false, false]
        updatedQuestions.questions[1].answers.displayOrder == [1, 2, 3, 4]
    }

    def "update question definition - change correct answers and question type - MultipleChoice -> SingleChoice"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questionDef = QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 2, 4)
        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3)).body
        def q2 = skillsService.createQuizQuestionDef(questionDef).body

        q2.questionType = QuizQuestionType.SingleChoice.toString()
        q2.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.questionType == QuizQuestionType.SingleChoice.toString()
        updatedQuestion.answers.answer == ["Answer #1", "Answer #2", "Answer #3", "Answer #4"]
        updatedQuestion.answers.isCorrect == [false, false, false, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3, 4]
    }

    def "update question definition - change question type - SingleChoice -> TextInput"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questionDef = QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4)
        skillsService.createQuizQuestionDef(QuizDefFactory.createSingleChoiceSurveyQuestion(1, 1, 3)).body
        def q2 = skillsService.createQuizQuestionDef(questionDef).body
        q2.questionType = QuizQuestionType.TextInput.toString()
        q2.quizId = quiz.quizId
        q2.answers = []
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)
        def updatedQuestions = skillsService.getQuizQuestionDefs(quiz.quizId)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.questionType == QuizQuestionType.TextInput.toString()
        updatedQuestion.answers.answer == [null]
        updatedQuestion.answers.isCorrect == [false]
        updatedQuestion.answers.displayOrder == [1]

        updatedQuestions.questions.question == ["This is questions #1", "This is questions #2"]
        updatedQuestions.questions.questionType == [QuizQuestionType.SingleChoice.toString(), QuizQuestionType.TextInput.toString()]
        updatedQuestions.questions[1].answers.answer == [null]
        updatedQuestions.questions[1].answers.isCorrect == [false]
        updatedQuestions.questions[1].answers.displayOrder == [1]
    }

    def "update question definition - change question type - TextInput -> SingleChoice"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createTextInputQuestion(1, 1)).body
        def questionDef = QuizDefFactory.createTextInputQuestion(1, 2)
        def q2 = skillsService.createQuizQuestionDef(questionDef).body
        q2.questionType = QuizQuestionType.SingleChoice.toString()
        q2.quizId = quiz.quizId
        q2.answers = [[
                              answer: "n1",
                              isCorrect: false,
                      ], [
                              answer: "n2",
                              isCorrect: false,
                      ], [
                              answer: "n3",
                              isCorrect: false,
                      ]]
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)

        then:
        updatedQuestion.questionType == QuizQuestionType.SingleChoice.toString()
        updatedQuestion.answers.answer == ["n1", "n2", "n3"]
        updatedQuestion.answers.isCorrect == [false, false, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3]
    }

    def "update question definition - one answer was removed"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createSingleChoiceSurveyQuestion(1, 1, 5)).body
        def questionDef = QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4)
        def q2 = skillsService.createQuizQuestionDef(questionDef).body
        q2.answers.remove(0)
        q2.quizId = quiz.quizId

        when:
        skillsService.updateQuizQuestionDef(q2)
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.questionType == QuizQuestionType.SingleChoice.toString()
        updatedQuestion.answers.answer == ["Answer #2", "Answer #3", "Answer #4"]
        updatedQuestion.answers.isCorrect == [false, false, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3]
    }

    def "update question definition - one answer was added at the end"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 5))
        def questionDef = QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4)
        def q2 = skillsService.createQuizQuestionDef(questionDef).body
        q2.answers.add([
                answer: "New",
                isCorrect: false,
        ])
        q2.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.questionType == QuizQuestionType.SingleChoice.toString()
        updatedQuestion.answers.answer == ["Answer #1", "Answer #2", "Answer #3", "Answer #4", "New"]
        updatedQuestion.answers.isCorrect == [false, false, false, false, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3, 4, 5]
    }

    def "update question definition - one answer was added in the middle"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 5))
        def questionDef = QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4)
        def q2 = skillsService.createQuizQuestionDef(questionDef).body

        q2.answers.add(1, [
                answer: "New",
                isCorrect: false,
        ])
        q2.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.questionType == QuizQuestionType.SingleChoice.toString()
        updatedQuestion.answers.answer == ["Answer #1", "New", "Answer #2", "Answer #3", "Answer #4"]
        updatedQuestion.answers.isCorrect == [false, false, false, false, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3, 4, 5]
    }

    def "update question definition - one answer was added, one question was removed and type was changed"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 5))
        def questionDef = QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4)
        def q2 = skillsService.createQuizQuestionDef(questionDef).body

        q2.answers.remove(2)
        q2.answers.add(0, [
                answer: "New",
                isCorrect: false,
        ])
        q2.quizId = quiz.quizId
        q2.questionType = QuizQuestionType.MultipleChoice.toString()
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.questionType == QuizQuestionType.MultipleChoice.toString()
        updatedQuestion.answers.answer == ["New", "Answer #1", "Answer #2", "Answer #4"]
        updatedQuestion.answers.isCorrect == [false, false, false, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3, 4]
    }

    def "update question definition - all questions replaced"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 5))
        def questionDef = QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4)
        def q2 = skillsService.createQuizQuestionDef(questionDef).body

        q2.answers = [[
                              answer: "n1",
                              isCorrect: false,
                      ], [
                              answer: "n2",
                              isCorrect: false,
                      ], [
                              answer: "n3",
                              isCorrect: false,
                      ]]
        q2.quizId = quiz.quizId
        q2.questionType = QuizQuestionType.MultipleChoice.toString()
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.questionType == QuizQuestionType.MultipleChoice.toString()
        updatedQuestion.answers.answer == ["n1", "n2", "n3"]
        updatedQuestion.answers.isCorrect == [false, false, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3]
    }
}

