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

class SurveyMetricsSpecs extends DefaultIntSpec {

    def "survey metrics counts"() {
        List<String> users = getRandomUsers(9, true)

        def surveyInfo1 = createSimpleSurvey(1)
        reportSurvey(users[0], surveyInfo1, [[0], [0, 2]], "Cool Answer")
        reportSurvey(users[1], surveyInfo1, [[1], [1, 2]], "Cool Answer")
        reportSurvey(users[2], surveyInfo1, [[0], [0]], "Cool Answer")
        reportSurvey(users[3], surveyInfo1, [[1], [0, 2]], "Cool Answer")
        reportSurvey(users[4], surveyInfo1, [[0], [0, 1, 2]], "Cool Answer")
        reportSurvey(users[5], surveyInfo1, [[1], [0, 2]], "Cool Answer")
        reportSurvey(users[6], surveyInfo1, [[0], [0, 2]], "Cool Answer")
        reportSurvey(users[7], surveyInfo1, [[0], [0, 2]], "Cool Answer")
        reportSurvey(users[8], surveyInfo1, [[0], [0, 2]], "Cool Answer", true) // doesn't count

        def surveyInfo2 = createSimpleSurvey(2)
        reportSurvey(users[0], surveyInfo2, [[0], [0, 2]], "Cool Answer")
        reportSurvey(users[1], surveyInfo2, [[1], [1, 2]], "Cool Answer")

        def surveyInfo3 = createSimpleSurvey(3)

        when:
        def q1_metrics = skillsService.getQuizMetrics(surveyInfo1.quizId)
        def q2_metrics = skillsService.getQuizMetrics(surveyInfo2.quizId)
        def q3_metrics = skillsService.getQuizMetrics(surveyInfo3.quizId)
        then:
        q1_metrics.numTaken == 8
        q1_metrics.numPassed == 8
        q1_metrics.numFailed == 0

        q1_metrics.numTakenDistinctUsers == 8
        q1_metrics.numPassedDistinctUsers == 8
        q1_metrics.numFailedDistinctUsers == 0

        q2_metrics.numTaken == 2
        q2_metrics.numPassed == 2
        q2_metrics.numFailed == 0

        q2_metrics.numTakenDistinctUsers == 2
        q2_metrics.numPassedDistinctUsers == 2
        q2_metrics.numFailedDistinctUsers == 0

        q3_metrics.numTaken == 0
        q3_metrics.numPassed == 0
        q3_metrics.numFailed == 0

        q3_metrics.numTakenDistinctUsers == 0
        q3_metrics.numPassedDistinctUsers == 0
        q3_metrics.numFailedDistinctUsers == 0
    }

    def "survey metrics - single choice question counts"() {
        List<String> users = getRandomUsers(9, true)

        def surveyInfo1 = createSimpleSurvey(1)
        reportSurvey(users[0], surveyInfo1, [[0], [0, 2]], "Cool Answer")
        reportSurvey(users[1], surveyInfo1, [[1], [1, 2]], "Cool Answer")
        reportSurvey(users[2], surveyInfo1, [[0], [0]], "Cool Answer")
        reportSurvey(users[3], surveyInfo1, [[1], [0, 2]], "Cool Answer")
        reportSurvey(users[4], surveyInfo1, [[0], [0, 1, 2]], "Cool Answer")
        reportSurvey(users[5], surveyInfo1, [[1], [0, 2]], "Cool Answer")
        reportSurvey(users[6], surveyInfo1, [[0], [0, 2]], "Cool Answer")
        reportSurvey(users[7], surveyInfo1, [[0], [0, 2]], "Cool Answer")
        reportSurvey(users[8], surveyInfo1, [[0], [0, 2]], "Cool Answer", true) // doesn't count

        def surveyInfo2 = createSimpleSurvey(2)
        reportSurvey(users[0], surveyInfo2, [[0], [0, 2]], "Cool Answer")

        def surveyInfo3 = createSimpleSurvey(3)
        reportSurvey(users[1], surveyInfo3, [[1], [1, 2]], "Cool Answer")

        when:
        def q1_metrics = skillsService.getQuizMetrics(surveyInfo1.quizId)
        def q2_metrics = skillsService.getQuizMetrics(surveyInfo2.quizId)
        def q3_metrics = skillsService.getQuizMetrics(surveyInfo3.quizId)
        then:
        def q1_question = q1_metrics.questions.find { it.questionType == QuizQuestionType.SingleChoice.toString() }
        q1_question.numAnsweredCorrect == 8
        q1_question.numAnsweredWrong == 0
        q1_question.answers[0].isCorrect == true
        q1_question.answers[0].numAnswered == 5
        q1_question.answers[0].numAnsweredCorrect == 5
        q1_question.answers[0].numAnsweredWrong == 0
        q1_question.answers[1].isCorrect == true
        q1_question.answers[1].numAnswered == 3
        q1_question.answers[1].numAnsweredCorrect == 3
        q1_question.answers[1].numAnsweredWrong == 0

        q2_metrics.numTaken == 1
        def q2_question = q2_metrics.questions.find { it.questionType == QuizQuestionType.SingleChoice.toString() }
        q2_question.numAnsweredCorrect == 1
        q2_question.numAnsweredWrong == 0
        q2_question.answers[0].isCorrect == true
        q2_question.answers[0].numAnswered == 1
        q2_question.answers[0].numAnsweredCorrect == 1
        q2_question.answers[0].numAnsweredWrong == 0
        q2_question.answers[1].isCorrect == true
        q2_question.answers[1].numAnswered == 0
        q2_question.answers[1].numAnsweredCorrect == 0
        q2_question.answers[1].numAnsweredWrong == 0

        q3_metrics.numTaken == 1
        def q3_question = q3_metrics.questions.find { it.questionType == QuizQuestionType.SingleChoice.toString() }
        q3_question.numAnsweredCorrect == 1
        q3_question.numAnsweredWrong == 0
        q3_question.answers[0].isCorrect == true
        q3_question.answers[0].numAnswered == 0
        q3_question.answers[0].numAnsweredCorrect == 0
        q3_question.answers[0].numAnsweredWrong == 0
        q3_question.answers[1].isCorrect == true
        q3_question.answers[1].numAnswered == 1
        q3_question.answers[1].numAnsweredCorrect == 1
        q3_question.answers[1].numAnsweredWrong == 0
    }

    def "survey metrics - multiple choice question counts"() {
        List<String> users = getRandomUsers(9, true)

        def surveyInfo1 = createSimpleSurvey(1)
        reportSurvey(users[0], surveyInfo1, [[0], [0, 2]], "Cool Answer")
        reportSurvey(users[1], surveyInfo1, [[1], [1, 2]], "Cool Answer")
        reportSurvey(users[2], surveyInfo1, [[0], [0]], "Cool Answer")
        reportSurvey(users[3], surveyInfo1, [[1], [0, 2]], "Cool Answer")
        reportSurvey(users[4], surveyInfo1, [[0], [0, 1, 2]], "Cool Answer")
        reportSurvey(users[5], surveyInfo1, [[1], [3]], "Cool Answer")
        reportSurvey(users[6], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer")
        reportSurvey(users[7], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer")
        reportSurvey(users[8], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer", true) // doesn't count

        def surveyInfo2 = createSimpleSurvey(2)
        reportSurvey(users[0], surveyInfo2, [[0], [0]], "Cool Answer")

        def surveyInfo3 = createSimpleSurvey(3)
        reportSurvey(users[1], surveyInfo3, [[1], [0, 1, 2, 3]], "Cool Answer")

        when:
        def q1_metrics = skillsService.getQuizMetrics(surveyInfo1.quizId)
        def q2_metrics = skillsService.getQuizMetrics(surveyInfo2.quizId)
        def q3_metrics = skillsService.getQuizMetrics(surveyInfo3.quizId)
        then:
        def q1_question = q1_metrics.questions.find { it.questionType == QuizQuestionType.MultipleChoice.toString() }
        q1_question.numAnsweredCorrect == 8
        q1_question.numAnsweredWrong == 0
        q1_question.answers[0].isCorrect == true
        q1_question.answers[0].numAnswered == 6
        q1_question.answers[0].numAnsweredCorrect == 6
        q1_question.answers[0].numAnsweredWrong == 0
        q1_question.answers[1].isCorrect == true
        q1_question.answers[1].numAnswered == 4
        q1_question.answers[1].numAnsweredCorrect == 4
        q1_question.answers[1].numAnsweredWrong == 0
        q1_question.answers[2].isCorrect == true
        q1_question.answers[2].numAnswered == 6
        q1_question.answers[2].numAnsweredCorrect == 6
        q1_question.answers[2].numAnsweredWrong == 0
        q1_question.answers[3].isCorrect == true
        q1_question.answers[3].numAnswered == 3
        q1_question.answers[3].numAnsweredCorrect == 3
        q1_question.answers[3].numAnsweredWrong == 0

        q2_metrics.numTaken == 1
        def q2_question = q2_metrics.questions.find { it.questionType == QuizQuestionType.MultipleChoice.toString() }
        q2_question.numAnsweredCorrect == 1
        q2_question.numAnsweredWrong == 0
        q2_question.answers[0].isCorrect == true
        q2_question.answers[0].numAnswered == 1
        q2_question.answers[0].numAnsweredCorrect == 1
        q2_question.answers[0].numAnsweredWrong == 0
        q2_question.answers[1].isCorrect == true
        q2_question.answers[1].numAnswered == 0
        q2_question.answers[1].numAnsweredCorrect == 0
        q2_question.answers[1].numAnsweredWrong == 0
        q2_question.answers[2].isCorrect == true
        q2_question.answers[2].numAnswered == 0
        q2_question.answers[2].numAnsweredCorrect == 0
        q2_question.answers[2].numAnsweredWrong == 0
        q2_question.answers[3].isCorrect == true
        q2_question.answers[3].numAnswered == 0
        q2_question.answers[3].numAnsweredCorrect == 0
        q2_question.answers[3].numAnsweredWrong == 0

        q3_metrics.numTaken == 1
        def q3_question = q3_metrics.questions.find { it.questionType == QuizQuestionType.MultipleChoice.toString() }
        q3_question.numAnsweredCorrect == 1
        q3_question.numAnsweredWrong == 0
        q3_question.answers[0].isCorrect == true
        q3_question.answers[0].numAnswered == 1
        q3_question.answers[0].numAnsweredCorrect == 1
        q3_question.answers[0].numAnsweredWrong == 0
        q3_question.answers[1].isCorrect == true
        q3_question.answers[1].numAnswered == 1
        q3_question.answers[1].numAnsweredCorrect == 1
        q3_question.answers[1].numAnsweredWrong == 0
        q3_question.answers[2].isCorrect == true
        q3_question.answers[2].numAnswered == 1
        q3_question.answers[2].numAnsweredCorrect == 1
        q3_question.answers[2].numAnsweredWrong == 0
        q3_question.answers[3].isCorrect == true
        q3_question.answers[3].numAnswered == 1
        q3_question.answers[3].numAnsweredCorrect == 1
        q3_question.answers[3].numAnsweredWrong == 0
    }

    def "survey metrics - text input question counts"() {
        List<String> users = getRandomUsers(9, true)

        def surveyInfo1 = createSimpleSurvey(1)
        reportSurvey(users[0], surveyInfo1, [[0], [0, 2]], "Answer")
        reportSurvey(users[1], surveyInfo1, [[1], [1, 2]], "Answer 1")
        reportSurvey(users[2], surveyInfo1, [[0], [0]], "Cool Answer 2")
        reportSurvey(users[3], surveyInfo1, [[1], [0, 2]], "Cool Answer 3")
        reportSurvey(users[4], surveyInfo1, [[0], [0, 1, 2]], "Cool Answer 4")
        reportSurvey(users[5], surveyInfo1, [[1], [3]], "Cool Answer 5")
        reportSurvey(users[6], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer 6")
        reportSurvey(users[7], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer 7")
        reportSurvey(users[8], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer 8", true) // doesn't count

        def surveyInfo2 = createSimpleSurvey(2)
        reportSurvey(users[0], surveyInfo2, [[0], [0]], "Cool Answer 9")

        def surveyInfo3 = createSimpleSurvey(3)
        reportSurvey(users[1], surveyInfo3, [[1], [0, 1, 2, 3]], "Cool Answer 10")

        when:
        def q1_metrics = skillsService.getQuizMetrics(surveyInfo1.quizId)
        def q2_metrics = skillsService.getQuizMetrics(surveyInfo2.quizId)
        def q3_metrics = skillsService.getQuizMetrics(surveyInfo3.quizId)
        then:
        def q1_question = q1_metrics.questions.find { it.questionType == QuizQuestionType.TextInput.toString() }
        q1_question.numAnsweredCorrect == 8
        q1_question.numAnsweredWrong == 0
        q1_question.answers[0].isCorrect == true
        q1_question.answers[0].numAnswered == 8
        q1_question.answers[0].numAnsweredCorrect == 8
        q1_question.answers[0].numAnsweredWrong == 0

        q2_metrics.numTaken == 1
        def q2_question = q2_metrics.questions.find { it.questionType == QuizQuestionType.TextInput.toString() }
        q2_question.numAnsweredCorrect == 1
        q2_question.numAnsweredWrong == 0
        q2_question.answers[0].isCorrect == true
        q2_question.answers[0].numAnswered == 1
        q2_question.answers[0].numAnsweredCorrect == 1
        q2_question.answers[0].numAnsweredWrong == 0

        q3_metrics.numTaken == 1
        def q3_question = q3_metrics.questions.find { it.questionType == QuizQuestionType.TextInput.toString() }
        q3_question.numAnsweredCorrect == 1
        q3_question.numAnsweredWrong == 0
        q3_question.answers[0].isCorrect == true
        q3_question.answers[0].numAnswered == 1
        q3_question.answers[0].numAnsweredCorrect == 1
        q3_question.answers[0].numAnsweredWrong == 0
    }


    static class AnswerRequestInfo {
        String userId
        List<List<Integer>> answerOptions
        String textAnswer
    }

    def "get text answers"() {
        List<String> users = getRandomUsers(9, true)
        def surveyInfo1 = createSimpleSurvey(1)

        List<AnswerRequestInfo> answerRequestsSorted = users[0..7].collect {
            new AnswerRequestInfo(userId: it, answerOptions:  [[0], [0, 2]], textAnswer: "answer-by-${it}")
        }.sort({ it.userId })
        answerRequestsSorted.each {
            reportSurvey(it.userId, surveyInfo1, it.answerOptions, it.textAnswer)
        }
        reportSurvey(users[8], surveyInfo1, [[0], [0, 2]], "answer9", true) // doesn't count


        def surveyInfo2 = createSimpleSurvey(2)
        reportSurvey(users[0], surveyInfo2, [[0], [0]], "Cool Answer")

        when:
        def answers1 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, surveyInfo1.questions[2].answerOptions[0].id)
        def answers1_pg1 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, surveyInfo1.questions[2].answerOptions[0].id, 3, 1)
        def answers1_pg2 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, surveyInfo1.questions[2].answerOptions[0].id, 3, 2)
        def answers1_pg3 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, surveyInfo1.questions[2].answerOptions[0].id, 3, 3)

        def answers2 = skillsService.getUserQuizAnswers(surveyInfo2.quizId, surveyInfo2.questions[2].answerOptions[0].id)

        println JsonOutput.prettyPrint(JsonOutput.toJson(answers1.data))
        then:
        answers1.count == 8
        answers1.totalCount == 8
        answers1.data.userId == answerRequestsSorted.userId
        answers1.data.answerTxt == answerRequestsSorted.textAnswer

        answers1_pg1.count == 8
        answers1_pg1.totalCount == 8
        answers1_pg1.data.userId == answerRequestsSorted[0..2].userId
        answers1_pg1.data.answerTxt == answerRequestsSorted[0..2].textAnswer

        answers1_pg2.count == 8
        answers1_pg2.totalCount == 8
        answers1_pg2.data.userId == answerRequestsSorted[3..5].userId
        answers1_pg2.data.answerTxt == answerRequestsSorted[3..5].textAnswer

        answers1_pg3.count == 8
        answers1_pg3.totalCount == 8
        answers1_pg3.data.userId == answerRequestsSorted[6..7].userId
        answers1_pg3.data.answerTxt == answerRequestsSorted[6..7].textAnswer

        answers2.count == 1
        answers2.totalCount == 1
        answers2.data.userId == [users[0]]
        answers2.data.answerTxt == ["Cool Answer"]
    }

    def "get multiple choice answers"() {
        List<String> users = getRandomUsers(9, true)

        def surveyInfo1 = createSimpleSurvey(1)
        reportSurvey(users[0], surveyInfo1, [[0], [0, 2]], "Cool Answer")
        reportSurvey(users[1], surveyInfo1, [[1], [1, 2]], "Cool Answer")
        reportSurvey(users[2], surveyInfo1, [[0], [0]], "Cool Answer")
        reportSurvey(users[3], surveyInfo1, [[1], [0, 2]], "Cool Answer")
        reportSurvey(users[4], surveyInfo1, [[0], [0, 1, 2]], "Cool Answer")
        reportSurvey(users[5], surveyInfo1, [[1], [3]], "Cool Answer")
        reportSurvey(users[6], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer")
        reportSurvey(users[7], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer")
        reportSurvey(users[8], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer", true) // doesn't count

        def surveyInfo2 = createSimpleSurvey(2)
        reportSurvey(users[0], surveyInfo2, [[0], [0]], "Cool Answer")

        when:
        def s1_answ1 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, surveyInfo1.questions[1].answerOptions[0].id)
        def s1_answ2 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, surveyInfo1.questions[1].answerOptions[1].id)
        def s1_answ3 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, surveyInfo1.questions[1].answerOptions[2].id)
        def s1_answ4 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, surveyInfo1.questions[1].answerOptions[3].id)

        println JsonOutput.prettyPrint(JsonOutput.toJson(s1_answ1.data))
        then:
        s1_answ1.count == 6
        s1_answ1.totalCount == 6
        s1_answ1.data.userId == [users[0], users[2], users[3], users[4], users[6], users[7]].sort()
        s1_answ1.data.answerTxt == [null, null, null, null, null, null]

        s1_answ2.count == 4
        s1_answ2.totalCount == 4
        s1_answ2.data.userId == [users[1], users[4], users[6], users[7]].sort()
        s1_answ2.data.answerTxt == [null, null, null, null]

        s1_answ3.count == 6
        s1_answ3.totalCount == 6
        s1_answ3.data.userId == [users[0], users[1], users[3], users[4], users[6], users[7]].sort()
        s1_answ3.data.answerTxt == [null, null, null, null, null, null]

        s1_answ4.count == 3
        s1_answ4.totalCount == 3
        s1_answ4.data.userId == [users[5], users[6], users[7]].sort()
        s1_answ4.data.answerTxt == [null, null, null]

    }

    def createSimpleSurvey(Integer num) {
        def survey = QuizDefFactory.createQuizSurvey(num, "Fancy Description")
        skillsService.createQuizDef(survey)
        def questions = [
                QuizDefFactory.createSingleChoiceSurveyQuestion(num, 1, 3),
                QuizDefFactory.createMultipleChoiceSurveyQuestion(num, 2, 4),
                QuizDefFactory.createTextInputSurveyQuestion(num, 3)
        ]
        skillsService.createQuizQuestionDefs(questions)
        def quizInfo = skillsService.getQuizInfo(survey.quizId)
        quizInfo.quizId = survey.quizId
        return quizInfo
    }

    void reportSurvey(String userId, def quizInfo, List<List<Integer>> answerOptions, String textAnswer, boolean inProgress = false) {
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quizInfo.quizId, userId).body

        answerOptions.eachWithIndex{ List<Integer> answers, int questionNum ->
            answers.each {
                skillsService.reportQuizAnswerForUserId(quizInfo.quizId, quizAttempt.id, quizInfo.questions[questionNum].answerOptions[it].id, userId)
            }
        }

        skillsService.reportQuizAnswerForUserId(quizInfo.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, userId, [isSelected:true, answerText: textAnswer])

        if (!inProgress) {
            skillsService.completeQuizAttemptForUserId(quizInfo.quizId, quizAttempt.id, userId).body
        }
    }

}
