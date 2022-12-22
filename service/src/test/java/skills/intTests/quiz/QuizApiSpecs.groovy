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

import groovy.json.JsonOutput
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory

class QuizApiSpecs extends DefaultIntSpec {

    def "change quiz questions display order"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 5, 2)
        questions[1].answers[0].isCorrect = true
        questions[1].answers[1].isCorrect = true
        skillsService.createQuizQuestionDefs(questions)

        when:
        def qRes = skillsService.getQuizInfo(quiz.quizId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(qRes))
        then:
        qRes.name == quiz.name
    }

    def "report quiz attempt - pass quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(quizInfo))
        when:
        def qRes = skillsService.reportQuizAttempt(quiz.quizId, [
                questionAnswers: [[
                        questionId: quizInfo.questions[0].id,
                        selectedAnswerIds: [quizInfo.questions[0].answerOptions[0].id]
                ], [
                        questionId: quizInfo.questions[1].id,
                        selectedAnswerIds: [quizInfo.questions[1].answerOptions[0].id]
                ]]
        ])
        println JsonOutput.prettyPrint(JsonOutput.toJson(qRes))
        then:
        qRes.body.passed == true
        qRes.body.gradedQuestions.questionId == quizInfo.questions.id
        qRes.body.gradedQuestions.isCorrect == [true, true]
        qRes.body.gradedQuestions[0].selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id]
        qRes.body.gradedQuestions[1].selectedAnswerIds == [quizInfo.questions[1].answerOptions[0].id]
    }

    def "report quiz attempt - fail quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(quizInfo))
        when:
        def qRes = skillsService.reportQuizAttempt(quiz.quizId, [
                questionAnswers: [[
                                          questionId: quizInfo.questions[0].id,
                                          selectedAnswerIds: [quizInfo.questions[0].answerOptions[0].id]
                                  ], [
                                          questionId: quizInfo.questions[1].id,
                                          selectedAnswerIds: [quizInfo.questions[1].answerOptions[1].id]
                                  ]]
        ])
        println JsonOutput.prettyPrint(JsonOutput.toJson(qRes))
        then:
        qRes.body.passed == false
        qRes.body.gradedQuestions.questionId == quizInfo.questions.id
        qRes.body.gradedQuestions.isCorrect == [true, false]
        qRes.body.gradedQuestions[0].selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id]
        qRes.body.gradedQuestions[1].selectedAnswerIds == [quizInfo.questions[1].answerOptions[1].id]
    }
}
