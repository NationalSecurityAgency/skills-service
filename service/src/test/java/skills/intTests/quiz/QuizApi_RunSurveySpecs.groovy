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

class QuizApi_RunSurveySpecs extends DefaultIntSpec {

    def "report survey attempt"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4),
                QuizDefFactory.createTextInputSurveyQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[2].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'This is user provided answer'])

        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        println JsonOutput.prettyPrint(JsonOutput.toJson(gradedQuizAttempt))
        then:
        gradedQuizAttempt.passed == true
        gradedQuizAttempt.gradedQuestions.questionId == quizInfo.questions.id
        gradedQuizAttempt.gradedQuestions.isCorrect == [true, true, true]
        gradedQuizAttempt.gradedQuestions[0].selectedAnswerIds == [quizInfo.questions[0].answerOptions[1].id]
        gradedQuizAttempt.gradedQuestions[1].selectedAnswerIds == [quizInfo.questions[1].answerOptions[2].id]
        gradedQuizAttempt.gradedQuestions[2].selectedAnswerIds == [quizInfo.questions[2].answerOptions[0].id]
    }

    def "restart survey attempt"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4),
                QuizDefFactory.createTextInputSurveyQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'This is user provided answer'])

        def restartedQuizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        println JsonOutput.prettyPrint(JsonOutput.toJson(restartedQuizAttempt))
        then:
        restartedQuizAttempt.selectedAnswerIds == [quizInfo.questions[0].answerOptions[1].id]
        restartedQuizAttempt.enteredText.size() == 1
        restartedQuizAttempt.enteredText.find { it.answerId == quizInfo.questions[2].answerOptions[0].id }.answerText == 'This is user provided answer'
    }
}
