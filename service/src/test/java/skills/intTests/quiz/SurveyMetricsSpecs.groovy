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


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import skills.intTests.utils.CertificateRegistry
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType
import skills.storage.model.UserQuizAttempt
import skills.storage.repos.UserQuizAttemptRepo
import spock.lang.IgnoreIf

import java.text.SimpleDateFormat

class SurveyMetricsSpecs extends DefaultIntSpec {

    @Value('${skills.config.ui.usersTableAdditionalUserTagKey}')
    String usersTableAdditionalUserTagKey

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    def "survey metrics counts"() {
        List<String> users = getRandomUsers(9, true)

        def surveyInfo1 = createSimpleSurvey(1)
        reportSurvey(users[0], surveyInfo1, [[0], [0, 2]], "Cool Answer", 3)
        reportSurvey(users[1], surveyInfo1, [[1], [1, 2]], "Cool Answer", 3)
        reportSurvey(users[2], surveyInfo1, [[0], [0]], "Cool Answer", 3)
        reportSurvey(users[3], surveyInfo1, [[1], [0, 2]], "Cool Answer", 3)
        reportSurvey(users[4], surveyInfo1, [[0], [0, 1, 2]], "Cool Answer", 3)
        reportSurvey(users[5], surveyInfo1, [[1], [0, 2]], "Cool Answer", 3)
        reportSurvey(users[6], surveyInfo1, [[0], [0, 2]], "Cool Answer", 3)
        reportSurvey(users[7], surveyInfo1, [[0], [0, 2]], "Cool Answer", 3)
        reportSurvey(users[8], surveyInfo1, [[0], [0, 2]], "Cool Answer", 3, true) // doesn't count

        def surveyInfo2 = createSimpleSurvey(2)
        reportSurvey(users[0], surveyInfo2, [[0], [0, 2]], "Cool Answer", 3)
        reportSurvey(users[1], surveyInfo2, [[1], [1, 2]], "Cool Answer", 3)

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
        reportSurvey(users[0], surveyInfo1, [[0], [0, 2]], "Cool Answer", 1)
        reportSurvey(users[1], surveyInfo1, [[1], [1, 2]], "Cool Answer", 2)
        reportSurvey(users[2], surveyInfo1, [[0], [0]], "Cool Answer", 3)
        reportSurvey(users[3], surveyInfo1, [[1], [0, 2]], "Cool Answer", 4)
        reportSurvey(users[4], surveyInfo1, [[0], [0, 1, 2]], "Cool Answer", 5)
        reportSurvey(users[5], surveyInfo1, [[1], [0, 2]], "Cool Answer", 1)
        reportSurvey(users[6], surveyInfo1, [[0], [0, 2]], "Cool Answer", 1)
        reportSurvey(users[7], surveyInfo1, [[0], [0, 2]], "Cool Answer", 2)
        reportSurvey(users[8], surveyInfo1, [[0], [0, 2]], "Cool Answer", 3, true) // doesn't count

        def surveyInfo2 = createSimpleSurvey(2)
        reportSurvey(users[0], surveyInfo2, [[0], [0, 2]], "Cool Answer", 3)

        def surveyInfo3 = createSimpleSurvey(3)
        reportSurvey(users[1], surveyInfo3, [[1], [1, 2]], "Cool Answer", 3)

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
        reportSurvey(users[0], surveyInfo1, [[0], [0, 2]], "Cool Answer", 1)
        reportSurvey(users[1], surveyInfo1, [[1], [1, 2]], "Cool Answer", 1)
        reportSurvey(users[2], surveyInfo1, [[0], [0]], "Cool Answer", 2)
        reportSurvey(users[3], surveyInfo1, [[1], [0, 2]], "Cool Answer", 2)
        reportSurvey(users[4], surveyInfo1, [[0], [0, 1, 2]], "Cool Answer", 3)
        reportSurvey(users[5], surveyInfo1, [[1], [3]], "Cool Answer", 3)
        reportSurvey(users[6], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer", 4)
        reportSurvey(users[7], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer", 5)
        reportSurvey(users[8], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer", 4, true) // doesn't count

        def surveyInfo2 = createSimpleSurvey(2)
        reportSurvey(users[0], surveyInfo2, [[0], [0]], "Cool Answer", 3)

        def surveyInfo3 = createSimpleSurvey(3)
        reportSurvey(users[1], surveyInfo3, [[1], [0, 1, 2, 3]], "Cool Answer", 2)

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
        reportSurvey(users[0], surveyInfo1, [[0], [0, 2]], "Answer", 1)
        reportSurvey(users[1], surveyInfo1, [[1], [1, 2]], "Answer 1", 1)
        reportSurvey(users[2], surveyInfo1, [[0], [0]], "Cool Answer 2", 2)
        reportSurvey(users[3], surveyInfo1, [[1], [0, 2]], "Cool Answer 3", 2)
        reportSurvey(users[4], surveyInfo1, [[0], [0, 1, 2]], "Cool Answer 4", 3)
        reportSurvey(users[5], surveyInfo1, [[1], [3]], "Cool Answer 5", 3)
        reportSurvey(users[6], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer 6", 4)
        reportSurvey(users[7], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer 7", 4)
        reportSurvey(users[8], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer 8", 5, true) // doesn't count

        def surveyInfo2 = createSimpleSurvey(2)
        reportSurvey(users[0], surveyInfo2, [[0], [0]], "Cool Answer 9", 3)

        def surveyInfo3 = createSimpleSurvey(3)
        reportSurvey(users[1], surveyInfo3, [[1], [0, 1, 2, 3]], "Cool Answer 10", 2)

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

    def "survey metrics - rating counts"() {
        List<String> users = getRandomUsers(9, true)

        def surveyInfo1 = createSimpleSurvey(1)
        reportSurvey(users[0], surveyInfo1, [[0], [0, 2]], "Cool Answer", 1)
        reportSurvey(users[1], surveyInfo1, [[1], [1, 2]], "Cool Answer", 2)
        reportSurvey(users[2], surveyInfo1, [[0], [0]], "Cool Answer", 3)
        reportSurvey(users[3], surveyInfo1, [[1], [0, 2]], "Cool Answer", 4)
        reportSurvey(users[4], surveyInfo1, [[0], [0, 1, 2]], "Cool Answer", 5)
        reportSurvey(users[5], surveyInfo1, [[1], [0, 2]], "Cool Answer", 1)
        reportSurvey(users[6], surveyInfo1, [[0], [0, 2]], "Cool Answer", 1)
        reportSurvey(users[7], surveyInfo1, [[0], [0, 2]], "Cool Answer", 2)
        reportSurvey(users[8], surveyInfo1, [[0], [0, 2]], "Cool Answer", 3, true) // doesn't count

        def surveyInfo2 = createSimpleSurvey(2)
        reportSurvey(users[0], surveyInfo2, [[0], [0, 2]], "Cool Answer", 3)

        def surveyInfo3 = createSimpleSurvey(3)
        reportSurvey(users[1], surveyInfo3, [[1], [1, 2]], "Cool Answer", 3)

        when:
        def q1_metrics = skillsService.getQuizMetrics(surveyInfo1.quizId)
        def q2_metrics = skillsService.getQuizMetrics(surveyInfo2.quizId)
        def q3_metrics = skillsService.getQuizMetrics(surveyInfo3.quizId)
        then:
        def q1_question = q1_metrics.questions.find { it.questionType == QuizQuestionType.Rating.toString() }
        q1_question.numAnsweredCorrect == 8
        q1_question.numAnsweredWrong == 0
        q1_question.answers[0].isCorrect == true
        q1_question.answers[0].numAnswered == 3
        q1_question.answers[1].isCorrect == true
        q1_question.answers[1].numAnswered == 2
        q1_question.answers[2].isCorrect == true
        q1_question.answers[2].numAnswered == 1
        q1_question.answers[3].isCorrect == true
        q1_question.answers[3].numAnswered == 1
        q1_question.answers[4].isCorrect == true
        q1_question.answers[4].numAnswered == 1

        q2_metrics.numTaken == 1
        def q2_question = q2_metrics.questions.find { it.questionType == QuizQuestionType.Rating.toString() }
        q2_question.numAnsweredCorrect == 1
        q2_question.numAnsweredWrong == 0
        q2_question.answers[2].isCorrect == true
        q2_question.answers[2].numAnswered == 1

        q3_metrics.numTaken == 1
        def q3_question = q3_metrics.questions.find { it.questionType == QuizQuestionType.Rating.toString() }
        q3_question.numAnsweredCorrect == 1
        q3_question.numAnsweredWrong == 0
        q3_question.answers[2].isCorrect == true
        q3_question.answers[2].numAnswered == 1
    }


    static class AnswerRequestInfo {
        String userId
        List<List<Integer>> answerOptions
        String textAnswer
        String userTag
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "get text answers"() {
        List<String> users = getRandomUsers(9, true)
        List<String> exclude = [[DEFAULT_ROOT_USER_ID, SkillsService.UseParams.DEFAULT_USER_NAME],users].flatten()
        SkillsService rootSkillsService = createRootSkillService(getRandomUsers(1, true, exclude)[0])
        Map<String, String> usersToTagLookup = [:]
        usersToTagLookup[users[0]] = "ABC"
        usersToTagLookup[users[2]] = "ABC1"
        usersToTagLookup.each { rootSkillsService.saveUserTag(it.key, usersTableAdditionalUserTagKey, [it.value]) }

        def surveyInfo1 = createSimpleSurvey(1)
        List<AnswerRequestInfo> answerRequestsSorted = users[0..7].collect {
            new AnswerRequestInfo(userId: it, answerOptions:  [[0], [0, 2]], textAnswer: "answer-by-${it}", userTag: usersToTagLookup[it])
        }.sort({
            // "-" char is sorted different in postgres vs groovy
            return it.userId.replace("-", "Z")
        })
        answerRequestsSorted.each {
            reportSurvey(it.userId, surveyInfo1, it.answerOptions, it.textAnswer, 3)
        }
        def survey1Attempt = reportSurvey(users[8], surveyInfo1, [[0], [0, 2]], "answer9", 3, true) // doesn't count


        def surveyInfo2 = createSimpleSurvey(2)
        def survey2Attempt = reportSurvey(users[0], surveyInfo2, [[0], [0]], "Cool Answer", 3)

        when:
        def answers1 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, survey1Attempt.questions[2].answerOptions[0].id)
        def answers1_pg1 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, survey1Attempt.questions[2].answerOptions[0].id, 3, 1)
        def answers1_pg2 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, survey1Attempt.questions[2].answerOptions[0].id, 3, 2)
        def answers1_pg3 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, survey1Attempt.questions[2].answerOptions[0].id, 3, 3)

        def answers2 = skillsService.getUserQuizAnswers(surveyInfo2.quizId, survey2Attempt.questions[2].answerOptions[0].id)

        then:
        answers1.count == 8
        answers1.totalCount == 8
        answers1.data.userId.sort() == answerRequestsSorted.userId.sort()
        answers1.data.answerTxt.sort() == answerRequestsSorted.textAnswer.sort()
        answers1.data.userTag.sort() == answerRequestsSorted.userTag.sort()

        answers1_pg1.count == 8
        answers1_pg1.totalCount == 8
        answers1_pg1.data.userId.sort() == answerRequestsSorted[0..2].userId.sort()
        answers1_pg1.data.answerTxt.sort() == answerRequestsSorted[0..2].textAnswer.sort()
        answers1_pg1.data.userTag.sort() == answerRequestsSorted[0..2].userTag.sort()

        answers1_pg2.count == 8
        answers1_pg2.totalCount == 8
        answers1_pg2.data.userId.sort() == answerRequestsSorted[3..5].userId.sort()
        answers1_pg2.data.answerTxt.sort() == answerRequestsSorted[3..5].textAnswer.sort()
        answers1_pg2.data.userTag.sort() == answerRequestsSorted[3..5].userTag.sort()

        answers1_pg3.count == 8
        answers1_pg3.totalCount == 8
        answers1_pg3.data.userId.sort() == answerRequestsSorted[6..7].userId.sort()
        answers1_pg3.data.answerTxt.sort() == answerRequestsSorted[6..7].textAnswer.sort()
        answers1_pg3.data.userTag.sort() == answerRequestsSorted[6..7].userTag.sort()

        answers2.count == 1
        answers2.totalCount == 1
        answers2.data.userId == [users[0]]
        answers2.data.answerTxt == ["Cool Answer"]
    }

    @Autowired(required=false)
    CertificateRegistry certificateRegistry

    def "get multiple choice answers"() {
        // "-" char is sorted different in postgres vs groovy
        List<String> usersWithSlash = certificateRegistry ? certificateRegistry.getAllUserIds().findAll( { it.contains( "-") }) : []
        List<String> users = getRandomUsers(9, true, [DEFAULT_ROOT_USER_ID, SkillsService.UseParams.DEFAULT_USER_NAME, usersWithSlash].flatten())

        def surveyInfo1 = createSimpleSurvey(1)
        def attempt0 = reportSurvey(users[0], surveyInfo1, [[0], [0, 2]], "Cool Answer", 3)
        def attempt1 = reportSurvey(users[1], surveyInfo1, [[1], [1, 2]], "Cool Answer", 3)
        def attempt2 = reportSurvey(users[2], surveyInfo1, [[0], [0]], "Cool Answer", 2)
        def attempt3 = reportSurvey(users[3], surveyInfo1, [[1], [0, 2]], "Cool Answer", 2)
        def attempt4 = reportSurvey(users[4], surveyInfo1, [[0], [0, 1, 2]], "Cool Answer", 3)
        def attempt5 = reportSurvey(users[5], surveyInfo1, [[1], [3]], "Cool Answer", 3)
        def attempt6 = reportSurvey(users[6], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer", 4)
        def attempt7 = reportSurvey(users[7], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer", 4)
        def attempt8 = reportSurvey(users[8], surveyInfo1, [[0], [0, 1, 2, 3]], "Cool Answer", 4, true) // doesn't count

        def surveyInfo2 = createSimpleSurvey(2)
        reportSurvey(users[0], surveyInfo2, [[0], [0]], "Cool Answer", 2)

        when:
        def s1_answ1 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, attempt8.questions[1].answerOptions[0].id)
        def s1_answ2 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, attempt8.questions[1].answerOptions[1].id)
        def s1_answ3 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, attempt8.questions[1].answerOptions[2].id)
        def s1_answ4 = skillsService.getUserQuizAnswers(surveyInfo1.quizId, attempt8.questions[1].answerOptions[3].id)

        then:
        s1_answ1.count == 6
        s1_answ1.totalCount == 6

        s1_answ1.data.userId.sort() == [users[0], users[2], users[3], users[4], users[6], users[7]].sort()
        s1_answ1.data.answerTxt == [null, null, null, null, null, null]
        s1_answ1.data.userQuizAttemptId.sort() == [attempt0.id, attempt2.id, attempt3.id, attempt4.id, attempt6.id, attempt7.id].sort()

        s1_answ2.count == 4
        s1_answ2.totalCount == 4
        s1_answ2.data.userId.sort() == [users[1], users[4], users[6], users[7]].sort()
        s1_answ2.data.answerTxt == [null, null, null, null]

        s1_answ3.count == 6
        s1_answ3.totalCount == 6
        s1_answ3.data.userId.sort() == [users[0], users[1], users[3], users[4], users[6], users[7]].sort()
        s1_answ3.data.answerTxt == [null, null, null, null, null, null]

        s1_answ4.count == 3
        s1_answ4.totalCount == 3
        s1_answ4.data.userId.sort() == [users[5], users[6], users[7]].sort()
        s1_answ4.data.answerTxt == [null, null, null]

    }
    
    def "survey metrics are filtered appropriately"() {
        def surveyInfo = createSimpleSurvey(1)

        List<String> users = getRandomUsers(10, true)
        List<Date> dates = (0..3).collect { new Date() - it }.reverse()

        reportSurvey(users[0], surveyInfo, [[0], [0, 2]], "Cool Answer", 3, false, dates[0])
        reportSurvey(users[1], surveyInfo, [[0], [0, 2]], "Cool Answer", 3, false, dates[0])
        reportSurvey(users[2], surveyInfo, [[0], [0, 2]], "Cool Answer", 3, false, dates[0])
        reportSurvey(users[3], surveyInfo, [[0], [0, 2]], "Cool Answer", 3, false, dates[0])
        reportSurvey(users[4], surveyInfo, [[0], [0, 2]], "Cool Answer", 3, false, dates[1])
        reportSurvey(users[5], surveyInfo, [[0], [0, 2]], "Cool Answer", 3, false, dates[1])
        reportSurvey(users[6], surveyInfo, [[0], [0, 2]], "Cool Answer", 3, false, dates[2])
        reportSurvey(users[7], surveyInfo, [[0], [0, 2]], "Cool Answer", 3, false, dates[3])
        reportSurvey(users[8], surveyInfo, [[0], [0, 2]], "Cool Answer", 3, false, dates[3])
        reportSurvey(users[9], surveyInfo, [[0], [0, 2]], "Cool Answer", 3, false, dates[3])

        def format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        String tomorrow = format.format(new Date() + 1)

        when:
        def metrics_day4 = skillsService.getQuizMetrics(surveyInfo.quizId, format.format(dates[0]), format.format(dates[1]))
        def metrics_day3 = skillsService.getQuizMetrics(surveyInfo.quizId, format.format(dates[1]), format.format(dates[2]))
        def metrics_day2 = skillsService.getQuizMetrics(surveyInfo.quizId, format.format(dates[2]), format.format(dates[3]))
        def metrics_day1 = skillsService.getQuizMetrics(surveyInfo.quizId, format.format(dates[3]), tomorrow)
        def metrics_day4_to_2 = skillsService.getQuizMetrics(surveyInfo.quizId, format.format(dates[0]), format.format(dates[2]))
        def metrics_day4_to_1 = skillsService.getQuizMetrics(surveyInfo.quizId, format.format(dates[0]), tomorrow)
        def metrics_day3_to_1 = skillsService.getQuizMetrics(surveyInfo.quizId, format.format(dates[1]), tomorrow)

        then:
        metrics_day4.numTaken == 4
        metrics_day4.numPassed == 4
        metrics_day4.numPassedDistinctUsers == 4
        metrics_day4.numTakenDistinctUsers == 4

        metrics_day3.numTaken == 2
        metrics_day3.numPassed == 2
        metrics_day3.numPassedDistinctUsers == 2
        metrics_day3.numTakenDistinctUsers == 2

        metrics_day2.numTaken == 1
        metrics_day2.numPassed == 1
        metrics_day2.numPassedDistinctUsers == 1
        metrics_day2.numTakenDistinctUsers == 1

        metrics_day1.numTaken == 3
        metrics_day1.numPassed == 3
        metrics_day1.numPassedDistinctUsers == 3
        metrics_day1.numTakenDistinctUsers == 3

        metrics_day4_to_2.numTaken == 6
        metrics_day4_to_2.numPassed == 6
        metrics_day4_to_2.numPassedDistinctUsers == 6
        metrics_day4_to_2.numTakenDistinctUsers == 6

        metrics_day4_to_1.numTaken == 10
        metrics_day4_to_1.numPassed == 10
        metrics_day4_to_1.numPassedDistinctUsers == 10
        metrics_day4_to_1.numTakenDistinctUsers == 10

        metrics_day3_to_1.numTaken == 6
        metrics_day3_to_1.numPassed == 6
        metrics_day3_to_1.numPassedDistinctUsers == 6
        metrics_day3_to_1.numTakenDistinctUsers == 6
    }


    def createSimpleSurvey(Integer num) {
        def survey = QuizDefFactory.createQuizSurvey(num, "Fancy Description")
        skillsService.createQuizDef(survey)
        def questions = [
                QuizDefFactory.createSingleChoiceSurveyQuestion(num, 1, 3),
                QuizDefFactory.createMultipleChoiceSurveyQuestion(num, 2, 4),
                QuizDefFactory.createTextInputQuestion(num, 3),
                QuizDefFactory.createRatingSurveyQuestion(num, 4)
        ]
        skillsService.createQuizQuestionDefs(questions)
        def quizInfo = skillsService.getQuizInfo(survey.quizId)
        quizInfo.quizId = survey.quizId
        return quizInfo
    }

    Object reportSurvey(String userId, def quizInfo, List<List<Integer>> answerOptions, String textAnswer, Integer rating, boolean inProgress = false, Date completionDate = null) {
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quizInfo.quizId, userId).body

        answerOptions.eachWithIndex{ List<Integer> answers, int questionNum ->
            answers.each {
                skillsService.reportQuizAnswerForUserId(quizInfo.quizId, quizAttempt.id, quizAttempt.questions[questionNum].answerOptions[it].id, userId)
            }
        }

        skillsService.reportQuizAnswerForUserId(quizInfo.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, userId, [isSelected:true, answerText: textAnswer])
        skillsService.reportQuizAnswerForUserId(quizInfo.quizId, quizAttempt.id, quizAttempt.questions[3].answerOptions[rating - 1].id, userId)

        if (!inProgress) {
            skillsService.completeQuizAttemptForUserId(quizInfo.quizId, quizAttempt.id, userId).body
        }

        if(completionDate) {
            UserQuizAttempt userQuizAttempt = userQuizAttemptRepo.findById(quizAttempt.id).get()
            userQuizAttempt.started = completionDate
            userQuizAttempt.completed = completionDate
            userQuizAttemptRepo.save(userQuizAttempt)
        }

        return quizAttempt
    }
}
