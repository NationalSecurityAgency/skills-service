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
import skills.services.quiz.QuizQuestionType

class QuizMetricsSpecs extends DefaultIntSpec {

    def "quiz metrics: number of attempts is more than distinct users"() {
        List<String> users = getRandomUsers(5, true)

        def quizInfo = createSimpleQuiz(1)
        runSimpleQuiz(users[0], quizInfo, true)
        runSimpleQuiz(users[1], quizInfo, false)
        runSimpleQuiz(users[1], quizInfo, false)
        runSimpleQuiz(users[1], quizInfo, true)
        runSimpleQuiz(users[2], quizInfo, true)
        runSimpleQuiz(users[3], quizInfo, false)
        runSimpleQuiz(users[3], quizInfo, true)
        runSimpleQuiz(users[4], quizInfo, true)

        def quizInfo2 = createSimpleQuiz(2)

        runSimpleQuiz(users[0], quizInfo2, true)
        runSimpleQuiz(users[1], quizInfo2, false)
        runSimpleQuiz(users[1], quizInfo2, false)
        runSimpleQuiz(users[2], quizInfo2, true)
        runSimpleQuiz(users[3], quizInfo2, false)
        runSimpleQuiz(users[4], quizInfo2, true, true) // do not include in progress

        when:
        def q1_metrics = skillsService.getQuizMetrics(quizInfo.quizId)
        def q2_metrics = skillsService.getQuizMetrics(quizInfo2.quizId)
        then:
        q1_metrics.numTaken == 8
        q1_metrics.numPassed == 5
        q1_metrics.numFailed == 3

        q1_metrics.numTakenDistinctUsers == 5
        q1_metrics.numPassedDistinctUsers == 5
        q1_metrics.numFailedDistinctUsers == 0

        q2_metrics.numTaken == 5
        q2_metrics.numPassed == 2
        q2_metrics.numFailed == 3

        q2_metrics.numTakenDistinctUsers == 4
        q2_metrics.numPassedDistinctUsers == 2
        q2_metrics.numFailedDistinctUsers == 2
    }

    def "quiz metrics: average runtime"() {
        List<String> users = getRandomUsers(5, true)

        def quizInfo = createSimpleQuiz(1)
        runSimpleQuiz(users[0], quizInfo, true, false, 600)
        runSimpleQuiz(users[1], quizInfo, false, false, 700)

        def quizInfo2 = createSimpleQuiz(2)
        runSimpleQuiz(users[0], quizInfo2, true, false, 800)
        runSimpleQuiz(users[1], quizInfo2, false, false, 820)

        when:
        def q1_metrics = skillsService.getQuizMetrics(quizInfo.quizId)
        def q2_metrics = skillsService.getQuizMetrics(quizInfo2.quizId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(q1_metrics))
        then:
        q1_metrics.numTaken == 2
        q1_metrics.avgAttemptRuntimeInMs >= 650
        q2_metrics.avgAttemptRuntimeInMs >= 810
    }

    def "quiz metrics - answers: single choice answer"() {
        List<String> users = getRandomUsers(5, true)

        def quizInfo = createSimpleQuiz(1)
        runSimpleQuiz(users[0], quizInfo, true, false, 600)
        runSimpleQuiz(users[1], quizInfo, false, false, 700)

        def quizInfo2 = createSimpleQuiz(2)
        runSimpleQuiz(users[0], quizInfo2, true, false, 800)
        runSimpleQuiz(users[1], quizInfo2, false, false, 820)

        when:
        def q1_metrics = skillsService.getQuizMetrics(quizInfo.quizId)
        def q2_metrics = skillsService.getQuizMetrics(quizInfo2.quizId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(q1_metrics))
        then:
        q1_metrics.numTaken == 2
        q1_metrics.avgAttemptRuntimeInMs >= 650
        q2_metrics.avgAttemptRuntimeInMs >= 810
    }

    def createSimpleQuiz(Integer num) {
        def quiz = QuizDefFactory.createQuiz(num, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(num, 1, 2, QuizQuestionType.SingleChoice),
                QuizDefFactory.createChoiceQuestion(num, 2, 4, QuizQuestionType.MultipleChoice)
        ]
        skillsService.createQuizQuestionDefs(questions)
        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        quizInfo.quizId = quiz.quizId
        return quizInfo
    }

    void runSimpleQuiz(String userId, def quizInfo, boolean pass, boolean inProgress = false, long sleepInMs = 0) {
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quizInfo.quizId, userId).body
        skillsService.reportQuizAnswerForUserId(quizInfo.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, userId)

        skillsService.reportQuizAnswerForUserId(quizInfo.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[0].id, userId)
        skillsService.reportQuizAnswerForUserId(quizInfo.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[2].id, userId)
        if (!pass) {
            skillsService.reportQuizAnswerForUserId(quizInfo.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[3].id, userId)
        }
        if (sleepInMs > 0) {
            Thread.sleep(sleepInMs)
        }
        if (!inProgress) {
            skillsService.completeQuizAttemptForUserId(quizInfo.quizId, quizAttempt.id, userId).body
        }
    }

    void runSimpleQuizByProvidingAnswers(String userId, def quizInfo, List<List<Integer>> answerOptions) {
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quizInfo.quizId, userId).body


        skillsService.reportQuizAnswerForUserId(quizInfo.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, userId)

        skillsService.reportQuizAnswerForUserId(quizInfo.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[0].id, userId)
        skillsService.reportQuizAnswerForUserId(quizInfo.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[2].id, userId)
        if (!pass) {
            skillsService.reportQuizAnswerForUserId(quizInfo.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[1].id, userId)
        }
        if (sleepInMs > 0) {
            Thread.sleep(sleepInMs)
        }
        if (!inProgress) {
            skillsService.completeQuizAttemptForUserId(quizInfo.quizId, quizAttempt.id, userId).body
        }
    }

}
