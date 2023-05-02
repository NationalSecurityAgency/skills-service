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
        q1_metrics.quizType == 'Quiz'
        q1_metrics.numTaken == 8
        q1_metrics.numPassed == 5
        q1_metrics.numFailed == 3

        q1_metrics.numTakenDistinctUsers == 5
        q1_metrics.numPassedDistinctUsers == 5
        q1_metrics.numFailedDistinctUsers == 2

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
        then:
        q1_metrics.numTaken == 2
        q1_metrics.avgAttemptRuntimeInMs >= 650
        q2_metrics.avgAttemptRuntimeInMs >= 810
    }

    def "quiz metrics - answers: single choice question"() {
        List<String> users = getRandomUsers(5, true)

        def quizInfo = createSimpleQuiz(1)
        runSimpleQuizByProvidingAnswers(users[0], quizInfo, [[0], [0, 2]]) // pass quiz example
        runSimpleQuizByProvidingAnswers(users[1], quizInfo, [[0], [0]]) // fail
        runSimpleQuizByProvidingAnswers(users[1], quizInfo, [[1], [0, 2]]) // fail
        runSimpleQuizByProvidingAnswers(users[1], quizInfo, [[1], [0, 2]]) // fail
        runSimpleQuizByProvidingAnswers(users[1], quizInfo, [[0], [0, 2]]) // pass
        runSimpleQuizByProvidingAnswers(users[2], quizInfo, [[0], [0, 2]]) // pass
        runSimpleQuizByProvidingAnswers(users[3], quizInfo, [[1], [0, 2]]) // pass
        runSimpleQuizByProvidingAnswers(users[4], quizInfo, [[1], [0, 2]], true) //  not complete

        def quizInfo2 = createSimpleQuiz(2)
        runSimpleQuizByProvidingAnswers(users[0], quizInfo2, [[0], [0, 2]]) // pass quiz example

        def quizInfo3 = createSimpleQuiz(3)
        runSimpleQuizByProvidingAnswers(users[0], quizInfo3, [[1], [0, 2]])

        when:
        def q1_metrics = skillsService.getQuizMetrics(quizInfo.quizId)
        def q2_metrics = skillsService.getQuizMetrics(quizInfo2.quizId)
        def q3_metrics = skillsService.getQuizMetrics(quizInfo3.quizId)
        then:
        q1_metrics.numTaken == 7
        def q1_question = q1_metrics.questions.find { it.questionType == QuizQuestionType.SingleChoice.toString() }
        q1_question.numAnsweredCorrect == 4
        q1_question.numAnsweredWrong == 3
        q1_question.answers[0].isCorrect == true
        q1_question.answers[0].numAnswered == 4
        q1_question.answers[0].numAnsweredCorrect == 4
        q1_question.answers[0].numAnsweredWrong == 0
        q1_question.answers[1].isCorrect == false
        q1_question.answers[1].numAnswered == 3
        q1_question.answers[1].numAnsweredCorrect == 0
        q1_question.answers[1].numAnsweredWrong == 3

        q2_metrics.numTaken == 1
        def q2_question = q2_metrics.questions.find { it.questionType == QuizQuestionType.SingleChoice.toString() }
        q2_question.numAnsweredCorrect == 1
        q2_question.numAnsweredWrong == 0
        q2_question.answers[0].isCorrect == true
        q2_question.answers[0].numAnswered == 1
        q2_question.answers[0].numAnsweredCorrect == 1
        q2_question.answers[0].numAnsweredWrong == 0
        q2_question.answers[1].isCorrect == false
        q2_question.answers[1].numAnswered == 0
        q2_question.answers[1].numAnsweredCorrect == 0
        q2_question.answers[1].numAnsweredWrong == 0

        q3_metrics.numTaken == 1
        def q3_question = q3_metrics.questions.find { it.questionType == QuizQuestionType.SingleChoice.toString() }
        q3_question.numAnsweredCorrect == 0
        q3_question.numAnsweredWrong == 1
        q3_question.answers[0].isCorrect == true
        q3_question.answers[0].numAnswered == 0
        q3_question.answers[0].numAnsweredCorrect == 0
        q3_question.answers[0].numAnsweredWrong == 0
        q3_question.answers[1].isCorrect == false
        q3_question.answers[1].numAnswered == 1
        q3_question.answers[1].numAnsweredCorrect == 0
        q3_question.answers[1].numAnsweredWrong == 1
    }

    def "quiz metrics - passed and failed are calculated correctly"() {
        List<String> users = getRandomUsers(5, true)

        def quizInfo = createSimpleQuiz(1)
        runSimpleQuizByProvidingAnswers(users[0], quizInfo, [[1], [1]])
        runSimpleQuizByProvidingAnswers(users[0], quizInfo, [[1], [1]])
        runSimpleQuizByProvidingAnswers(users[0], quizInfo, [[0], [0, 2]]) // pass quiz example

        when:
        def q1_metrics = skillsService.getQuizMetrics(quizInfo.quizId)

        then:
        q1_metrics.numTaken == 3
        q1_metrics.numPassed == 1
        q1_metrics.numFailed == 2
        q1_metrics.numPassedDistinctUsers == 1
        q1_metrics.numFailedDistinctUsers == 1
        q1_metrics.numTakenDistinctUsers == 1
    }

    def "quiz metrics - answers: multiple choice question"() {
        List<String> users = getRandomUsers(5, true)

        def quizInfo = createSimpleQuiz(1)
        runSimpleQuizByProvidingAnswers(users[0], quizInfo, [[0], [0, 2]]) // pass quiz example
        runSimpleQuizByProvidingAnswers(users[1], quizInfo, [[0], [0]])
        runSimpleQuizByProvidingAnswers(users[1], quizInfo, [[0], [0, 1]])
        runSimpleQuizByProvidingAnswers(users[1], quizInfo, [[0], [0, 3]])
        runSimpleQuizByProvidingAnswers(users[1], quizInfo, [[0], [0, 1, 2, 3]])
        runSimpleQuizByProvidingAnswers(users[1], quizInfo, [[0], [0, 2]])
        runSimpleQuizByProvidingAnswers(users[2], quizInfo, [[0], [0, 1, 2, 3]], true) // not counted
        runSimpleQuizByProvidingAnswers(users[3], quizInfo, [[0], [3]])
        runSimpleQuizByProvidingAnswers(users[4], quizInfo, [[0], [1, 3]])

        def quizInfo2 = createSimpleQuiz(2)
        runSimpleQuizByProvidingAnswers(users[0], quizInfo2, [[0], [0, 2]]) // pass quiz example

        def quizInfo3 = createSimpleQuiz(3)
        runSimpleQuizByProvidingAnswers(users[0], quizInfo3, [[0], [1, 3]])

        when:
        def q1_metrics = skillsService.getQuizMetrics(quizInfo.quizId)
        def q2_metrics = skillsService.getQuizMetrics(quizInfo2.quizId)
        def q3_metrics = skillsService.getQuizMetrics(quizInfo3.quizId)
        then:
        q1_metrics.numTaken == 8
        def q1_question = q1_metrics.questions.find { it.questionType == QuizQuestionType.MultipleChoice.toString() }
        q1_question.numAnsweredCorrect == 2
        q1_question.numAnsweredWrong == 6
        q1_question.answers[0].isCorrect == true
        q1_question.answers[0].numAnswered == 6
        q1_question.answers[0].numAnsweredCorrect == 6
        q1_question.answers[0].numAnsweredWrong == 0
        q1_question.answers[1].isCorrect == false
        q1_question.answers[1].numAnswered == 3
        q1_question.answers[1].numAnsweredCorrect == 0
        q1_question.answers[1].numAnsweredWrong == 3
        q1_question.answers[2].isCorrect == true
        q1_question.answers[2].numAnswered == 3
        q1_question.answers[2].numAnsweredCorrect == 3
        q1_question.answers[2].numAnsweredWrong == 0
        q1_question.answers[3].isCorrect == false
        q1_question.answers[3].numAnswered == 4
        q1_question.answers[3].numAnsweredCorrect == 0
        q1_question.answers[3].numAnsweredWrong == 4

        q2_metrics.numTaken == 1
        def q2_question = q2_metrics.questions.find { it.questionType == QuizQuestionType.MultipleChoice.toString() }
        q2_question.numAnsweredCorrect == 1
        q2_question.numAnsweredWrong == 0
        q2_question.answers[0].isCorrect == true
        q2_question.answers[0].numAnswered == 1
        q2_question.answers[0].numAnsweredCorrect == 1
        q2_question.answers[0].numAnsweredWrong == 0
        q2_question.answers[1].isCorrect == false
        q2_question.answers[1].numAnswered == 0
        q2_question.answers[1].numAnsweredCorrect == 0
        q2_question.answers[1].numAnsweredWrong == 0

        q3_metrics.numTaken == 1
        def q3_question = q3_metrics.questions.find { it.questionType == QuizQuestionType.MultipleChoice.toString() }
        q3_question.numAnsweredCorrect == 0
        q3_question.numAnsweredWrong == 1
        q3_question.answers[0].isCorrect == true
        q3_question.answers[0].numAnswered == 0
        q3_question.answers[0].numAnsweredCorrect == 0
        q3_question.answers[0].numAnsweredWrong == 0
        q3_question.answers[1].isCorrect == false
        q3_question.answers[1].numAnswered == 1
        q3_question.answers[1].numAnsweredCorrect == 0
        q3_question.answers[1].numAnsweredWrong == 1
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

    void runSimpleQuizByProvidingAnswers(String userId, def quizInfo, List<List<Integer>> answerOptions, boolean inProgress = false) {
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quizInfo.quizId, userId).body

        answerOptions.eachWithIndex{ List<Integer> answers, int questionNum ->
            answers.each {
                skillsService.reportQuizAnswerForUserId(quizInfo.quizId, quizAttempt.id, quizInfo.questions[questionNum].answerOptions[it].id, userId)
            }
        }

        if (!inProgress) {
            skillsService.completeQuizAttemptForUserId(quizInfo.quizId, quizAttempt.id, userId).body
        }
    }

}
