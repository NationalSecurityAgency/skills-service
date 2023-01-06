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
import skills.quizLoading.model.QuizAttemptReq
import skills.quizLoading.model.QuizQuestionAttemptReq

class QuizMetricsSpecs extends DefaultIntSpec {

    def "load metrics"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        QuizAttemptReq quizAttempReq = new QuizAttemptReq(
                questionAnswers: [
                    new QuizQuestionAttemptReq(
                        questionId: quizInfo.questions[0].id,
                         selectedAnswerIds: [quizInfo.questions[0].answerOptions[0].id]
                    ), new QuizQuestionAttemptReq(
                        questionId: quizInfo.questions[1].id,
                        selectedAnswerIds: [quizInfo.questions[1].answerOptions[0].id]
                    )
                ]
        )

        QuizAttemptReq quizAttempReqFailed = new QuizAttemptReq(
                questionAnswers: [
                        new QuizQuestionAttemptReq(
                                questionId: quizInfo.questions[0].id,
                                selectedAnswerIds: [quizInfo.questions[0].answerOptions[0].id]
                        ), new QuizQuestionAttemptReq(
                        questionId: quizInfo.questions[1].id,
                        selectedAnswerIds: [quizInfo.questions[1].answerOptions[1].id]
                )
                ]
        )

        List<String> users = getRandomUsers(5, true)
        users.eachWithIndex { it, index ->
            if (index == 1) {
                skillsService.reportQuizAttemptForUser(it, quiz.quizId, quizAttempReqFailed)
            } else {
                skillsService.reportQuizAttemptForUser(it, quiz.quizId, quizAttempReq)
            }
        }

        when:
        def metrics = skillsService.getQuizMetrics(quiz.quizId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(metrics))
        then:
        metrics
    }

}
