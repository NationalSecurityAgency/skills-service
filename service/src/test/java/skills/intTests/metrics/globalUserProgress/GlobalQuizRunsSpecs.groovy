/**
 * Copyright 2026 SkillTree
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
package skills.intTests.metrics.globalUserProgress

import groovy.json.JsonOutput
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType
import skills.storage.model.QuizDefParent
import skills.storage.model.UserQuizAttempt
import spock.lang.IgnoreIf

import static skills.storage.model.UserQuizAttempt.QuizAttemptStatus.PASSED
import static skills.storage.model.UserQuizAttempt.QuizAttemptStatus.FAILED
import static skills.storage.model.UserQuizAttempt.QuizAttemptStatus.INPROGRESS
import static skills.storage.model.QuizDefParent.QuizType.Survey


class GlobalQuizRunsSpecs extends DefaultIntSpec {
    List<SkillsService> users
    List quizzes
    List surveys

    List attempts
    List attemptsUsers
    List attemptsQuizzes

    def setup() {
        users = getRandomUsers(10).collect {createService(it) }
        quizzes = (1..5).collect { createQuiz(it)}
        surveys = (6..10).collect { createSurvey(it)}

        attempts = []
        attemptsUsers = []
        attemptsQuizzes = []
        attempts << runQuizOrSurvey(users[9], quizzes[1])
        attempts << runQuizOrSurvey(users[4], quizzes[2], false)
        attempts << runQuizOrSurvey(users[1], quizzes[0], false)
        attempts << runQuizOrSurvey(users[6], quizzes[3], false)
        attempts << runQuizOrSurvey(users[0], quizzes[1])
        attempts << runQuizOrSurvey(users[3], quizzes[0], false)
        attempts << runQuizOrSurvey(users[5], surveys[1])
        attempts << runQuizOrSurvey(users[1], quizzes[4], true)
        attempts << runQuizOrSurvey(users[7], quizzes[1])
        attempts << runQuizOrSurvey(users[2], quizzes[0], false)

        attempts << runQuizOrSurvey(users[3], quizzes[2])
        attempts << runQuizOrSurvey(users[4], quizzes[0], true)
        attempts << runQuizOrSurvey(users[8], quizzes[1])
        attempts << runQuizOrSurvey(users[1], quizzes[0], false)
        attempts << runQuizOrSurvey(users[5], quizzes[3], true, false)
        attempts << runQuizOrSurvey(users[0], surveys[0], true, false)
        attempts << runQuizOrSurvey(users[6], quizzes[1])
        attempts << runQuizOrSurvey(users[3], quizzes[0], true)
        attempts << runQuizOrSurvey(users[2], quizzes[1])
        attempts << runQuizOrSurvey(users[4], quizzes[1])

        attempts << runQuizOrSurvey(users[1], quizzes[1])
        attempts << runQuizOrSurvey(users[0], quizzes[4], true, false)
        attempts << runQuizOrSurvey(users[1], quizzes[0], true)
        attempts << runQuizOrSurvey(users[3], quizzes[1])
    }

    def "get empty quiz runs progress"() {
        quizDefRepo.deleteAll()
        projDefRepo.deleteAll()
        when:
        def res = skillsService.getGlobalQuizRuns()
        then:
        res.data == []
        res.count == 0
        res.totalCount == 0
    }

    def "must not be able to retrieve more than 500 items"() {
        when:
        skillsService.getGlobalQuizRuns('', '', 10000)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.resBody.contains("[limit] must be <= 500")
    }

    def "get quiz runs for multiple users and different quizzes"() {
        when:
        def resPage1 = skillsService.getGlobalQuizRuns()
        def resPage2 = skillsService.getGlobalQuizRuns('', '', 10, 2)
        def resPage3 = skillsService.getGlobalQuizRuns('', '', 10, 3)

        assertQuiz(resPage1.data[0], new QValidInfo(attemptId: attempts[0].id, uId: users[9].userName, qId: quizzes[1].quizId, qName: quizzes[1].name, status: PASSED))
        assertQuiz(resPage1.data[1], new QValidInfo(attemptId: attempts[1].id, uId: users[4].userName, qId: quizzes[2].quizId, qName: quizzes[2].name, status: FAILED, numCorrect: 0))
        assertQuiz(resPage1.data[2], new QValidInfo(attemptId: attempts[2].id, uId: users[1].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: FAILED, numCorrect: 0))
        assertQuiz(resPage1.data[3], new QValidInfo(attemptId: attempts[3].id, uId: users[6].userName, qId: quizzes[3].quizId, qName: quizzes[3].name, status: FAILED, numCorrect: 0))
        assertQuiz(resPage1.data[4], new QValidInfo(attemptId: attempts[4].id, uId: users[0].userName, qId: quizzes[1].quizId, qName: quizzes[1].name, status: PASSED))
        assertQuiz(resPage1.data[5], new QValidInfo(attemptId: attempts[5].id, uId: users[3].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: FAILED, numCorrect: 0))
        assertQuiz(resPage1.data[6], new QValidInfo(attemptId: attempts[6].id, uId: users[5].userName, qId: surveys[1].quizId, qName: surveys[1].name, status: PASSED, type: Survey))
        assertQuiz(resPage1.data[7], new QValidInfo(attemptId: attempts[7].id, uId: users[1].userName, qId: quizzes[4].quizId, qName: quizzes[4].name, status: PASSED))
        assertQuiz(resPage1.data[8], new QValidInfo(attemptId: attempts[8].id, uId: users[7].userName, qId: quizzes[1].quizId, qName: quizzes[1].name, status: PASSED))
        assertQuiz(resPage1.data[9], new QValidInfo(attemptId: attempts[9].id, uId: users[2].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: FAILED, numCorrect: 0))

        assertQuiz(resPage2.data[0], new QValidInfo(attemptId: attempts[10].id, uId: users[3].userName, qId: quizzes[2].quizId, qName: quizzes[2].name, status: PASSED))
        assertQuiz(resPage2.data[1], new QValidInfo(attemptId: attempts[11].id, uId: users[4].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: PASSED))
        assertQuiz(resPage2.data[2], new QValidInfo(attemptId: attempts[12].id, uId: users[8].userName, qId: quizzes[1].quizId, qName: quizzes[1].name, status: PASSED))
        assertQuiz(resPage2.data[3], new QValidInfo(attemptId: attempts[13].id, uId: users[1].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: FAILED, numCorrect: 0))
        assertQuiz(resPage2.data[4], new QValidInfo(attemptId: attempts[14].id, uId: users[5].userName, qId: quizzes[3].quizId, qName: quizzes[3].name, status: INPROGRESS, numCorrect: 0))
        assertQuiz(resPage2.data[5], new QValidInfo(attemptId: attempts[15].id, uId: users[0].userName, qId: surveys[0].quizId, qName: surveys[0].name, status: INPROGRESS, type: Survey, numCorrect: 0))
        assertQuiz(resPage2.data[6], new QValidInfo(attemptId: attempts[16].id, uId: users[6].userName, qId: quizzes[1].quizId, qName: quizzes[1].name, status: PASSED))
        assertQuiz(resPage2.data[7], new QValidInfo(attemptId: attempts[17].id, uId: users[3].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: PASSED))
        assertQuiz(resPage2.data[8], new QValidInfo(attemptId: attempts[18].id, uId: users[2].userName, qId: quizzes[1].quizId, qName: quizzes[1].name, status: PASSED))
        assertQuiz(resPage2.data[9], new QValidInfo(attemptId: attempts[19].id, uId: users[4].userName, qId: quizzes[1].quizId, qName: quizzes[1].name, status: PASSED))

        assertQuiz(resPage3.data[0], new QValidInfo(attemptId: attempts[20].id, uId: users[1].userName, qId: quizzes[1].quizId, qName: quizzes[1].name, status: PASSED))
        assertQuiz(resPage3.data[1], new QValidInfo(attemptId: attempts[21].id, uId: users[0].userName, qId: quizzes[4].quizId, qName: quizzes[4].name, status: INPROGRESS, numCorrect: 0))
        assertQuiz(resPage3.data[2], new QValidInfo(attemptId: attempts[22].id, uId: users[1].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: PASSED))
        assertQuiz(resPage3.data[3], new QValidInfo(attemptId: attempts[23].id, uId: users[3].userName, qId: quizzes[1].quizId, qName: quizzes[1].name, status: PASSED))
        then:
        resPage1.data.size() == 10
        resPage2.data.size() == 10
        resPage3.data.size() == 4

        resPage1.count == 24
        resPage1.totalCount == 24
        resPage2.count == 24
        resPage2.totalCount == 24
        resPage3.count == 24
        resPage3.totalCount == 24
    }

    def "filter by quiz name"() {
        when:
        def resPage1 = skillsService.getGlobalQuizRuns('', 'qUiZ 1', 4, 1)
        def resPage2 = skillsService.getGlobalQuizRuns('', 'qUiZ 1', 4, 2)
        println JsonOutput.toJson(resPage1)
        assertQuiz(resPage1.data[0], new QValidInfo(attemptId: attempts[2].id, uId: users[1].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: FAILED, numCorrect: 0))
        assertQuiz(resPage1.data[1], new QValidInfo(attemptId: attempts[5].id, uId: users[3].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: FAILED, numCorrect: 0))
        assertQuiz(resPage1.data[2], new QValidInfo(attemptId: attempts[9].id, uId: users[2].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: FAILED, numCorrect: 0))
        assertQuiz(resPage1.data[3], new QValidInfo(attemptId: attempts[11].id, uId: users[4].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: PASSED))
        assertQuiz(resPage2.data[0], new QValidInfo(attemptId: attempts[13].id, uId: users[1].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: FAILED, numCorrect: 0))
        assertQuiz(resPage2.data[1], new QValidInfo(attemptId: attempts[17].id, uId: users[3].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: PASSED))
        assertQuiz(resPage2.data[2], new QValidInfo(attemptId: attempts[22].id, uId: users[1].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: PASSED))
        then:
        resPage1.data.size() == 4
        resPage2.data.size() == 3

        resPage1.count == 7
        resPage2.count == 7
    }

    def "filter by user id for display"() {
        when:
        def resPage1 = skillsService.getGlobalQuizRuns(userAttrsRepo.findByUserIdIgnoreCase(users[1].userName).userIdForDisplay, '', 4, 1)
        def resPage2 = skillsService.getGlobalQuizRuns(userAttrsRepo.findByUserIdIgnoreCase(users[1].userName).userIdForDisplay, '', 4, 2)

        assertQuiz(resPage1.data[0], new QValidInfo(attemptId: attempts[2].id, uId: users[1].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: FAILED, numCorrect: 0))
        assertQuiz(resPage1.data[1], new QValidInfo(attemptId: attempts[7].id, uId: users[1].userName, qId: quizzes[4].quizId, qName: quizzes[4].name, status: PASSED))
        assertQuiz(resPage1.data[2], new QValidInfo(attemptId: attempts[13].id, uId: users[1].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: FAILED, numCorrect: 0))
        assertQuiz(resPage1.data[3], new QValidInfo(attemptId: attempts[20].id, uId: users[1].userName, qId: quizzes[1].quizId, qName: quizzes[1].name, status: PASSED))
        assertQuiz(resPage2.data[0], new QValidInfo(attemptId: attempts[22].id, uId: users[1].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: PASSED))
        then:
        resPage1.data.size() == 4
        resPage2.data.size() == 1

        resPage1.count == 5
        resPage1.totalCount == 5
        resPage2.count == 5
        resPage2.totalCount == 5
    }

    @Autowired
    JdbcTemplate jdbcTemplate
    def "filter by date"() {
        jdbcTemplate.update("update user_quiz_attempt set started='2026-03-03 14:00:00' where id='${attempts[2].id}'")
        jdbcTemplate.update("update user_quiz_attempt set started='2026-03-02 14:00:00' where id='${attempts[7].id}'")

        when:
        def resPage1 = skillsService.getGlobalQuizRuns('', '', 10, 1, 'started', true, '2026-03-03 13:00:00', '2026-03-03 15:00:00')
        def res2 = skillsService.getGlobalQuizRuns('', '', 10, 1, 'started', true, '2026-03-02 13:00:00', '2026-03-03 15:00:00')

        assertQuiz(resPage1.data[0], new QValidInfo(attemptId: attempts[2].id, uId: users[1].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: FAILED, numCorrect: 0))

        assertQuiz(res2.data[0], new QValidInfo(attemptId: attempts[7].id, uId: users[1].userName, qId: quizzes[4].quizId, qName: quizzes[4].name, status: PASSED))
        assertQuiz(res2.data[1], new QValidInfo(attemptId: attempts[2].id, uId: users[1].userName, qId: quizzes[0].quizId, qName: quizzes[0].name, status: FAILED, numCorrect: 0))
        then:
        resPage1.data.size() == 1
        resPage1.count == 1

        res2.data.size() == 2
        res2.count == 2
    }


    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "sort by userIdForDisplay"() {
        when:
        def resPage1 = skillsService.getGlobalQuizRuns('', '', 10, 1, 'userIdForDisplay')
        def resPage2 = skillsService.getGlobalQuizRuns('', '', 10, 2, 'userIdForDisplay')
        def resPage3 = skillsService.getGlobalQuizRuns('', '', 10, 3, 'userIdForDisplay')

        List<String> expectedOrder = attemptsUsers.collect { userAttrsRepo.findByUserIdIgnoreCase(it.userName)}.userIdForDisplay.sort()

        then:
        resPage1.data.userIdForDisplay == expectedOrder[0..9]
        resPage2.data.userIdForDisplay == expectedOrder[10..19]
        resPage3.data.userIdForDisplay == expectedOrder[20..23]
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "sort by userIdForDisplay - desc"() {
        when:
        def resPage1 = skillsService.getGlobalQuizRuns('', '', 10, 1, 'userIdForDisplay', false)
        def resPage2 = skillsService.getGlobalQuizRuns('', '', 10, 2, 'userIdForDisplay', false)
        def resPage3 = skillsService.getGlobalQuizRuns('', '', 10, 3, 'userIdForDisplay', false)

        List<String> expectedOrder = attemptsUsers.collect { userAttrsRepo.findByUserIdIgnoreCase(it.userName)}.userIdForDisplay.sort().reverse()

        then:
        resPage1.data.userIdForDisplay == expectedOrder[0..9]
        resPage2.data.userIdForDisplay == expectedOrder[10..19]
        resPage3.data.userIdForDisplay == expectedOrder[20..23]
    }

    def "sort by quizName"() {
        when:
        def resPage1 = skillsService.getGlobalQuizRuns('', '', 10, 1, 'quizName')
        def resPage2 = skillsService.getGlobalQuizRuns('', '', 10, 2, 'quizName')
        def resPage3 = skillsService.getGlobalQuizRuns('', '', 10, 3, 'quizName')

        List<String> expectedOrder = attemptsQuizzes.name.sort()

        then:
        resPage1.data.quizName == expectedOrder[0..9]
        resPage2.data.quizName == expectedOrder[10..19]
        resPage3.data.quizName == expectedOrder[20..23]
    }

    def "sort by quiz type"() {
        when:
        def resPage1 = skillsService.getGlobalQuizRuns('', '', 10, 1, 'quizType')
        def resPage2 = skillsService.getGlobalQuizRuns('', '', 10, 2, 'quizType')
        def resPage3 = skillsService.getGlobalQuizRuns('', '', 10, 3, 'quizType')

        List<String> expectedOrder = attemptsQuizzes.type.sort()

        then:
        resPage1.data.quizType == expectedOrder[0..9]
        resPage2.data.quizType == expectedOrder[10..19]
        resPage3.data.quizType == expectedOrder[20..23]
    }

    def "sort by quiz status"() {
        when:
        def resPage1 = skillsService.getGlobalQuizRuns('', '', 10, 1, 'status')
        def resPage2 = skillsService.getGlobalQuizRuns('', '', 10, 2, 'status')
        def resPage3 = skillsService.getGlobalQuizRuns('', '', 10, 3, 'status')

        List<String> expectedOrder = [PASSED, FAILED, FAILED, FAILED, PASSED, FAILED, PASSED, PASSED, PASSED, FAILED,
                                      PASSED, PASSED, PASSED, FAILED, INPROGRESS, INPROGRESS, PASSED, PASSED, PASSED, PASSED,
                                      PASSED, INPROGRESS, PASSED, PASSED].collect{it.toString()}.sort()

        then:
        resPage1.data.status == expectedOrder[0..9]
        resPage2.data.status == expectedOrder[10..19]
        resPage3.data.status == expectedOrder[20..23]
    }

    static class QValidInfo {
        Integer attemptId
        String uId
        String qId
        String qName
        UserQuizAttempt.QuizAttemptStatus status
        QuizDefParent.QuizType type = QuizDefParent.QuizType.Quiz
        String uTag = null
        Integer numCorrect = 1
        Integer totalAnswers = 1
    }

    private assertQuiz(def res, QValidInfo quizRunValidInfo) {
        String userIdForDisplay = userAttrsRepo.findByUserIdIgnoreCase(quizRunValidInfo.uId).userIdForDisplay
        assert res.attemptId == quizRunValidInfo.attemptId
        assert res.userId == quizRunValidInfo.uId
        assert res.userIdForDisplay == userIdForDisplay
        assert res.quizId == quizRunValidInfo.qId
        assert res.quizName == quizRunValidInfo.qName
        assert res.status == quizRunValidInfo.status.toString()
        assert res.quizType == quizRunValidInfo.type.toString()
        assert res.userTag == quizRunValidInfo.uTag
        assert res.numberCorrect == quizRunValidInfo.numCorrect
        assert res.totalAnswers == quizRunValidInfo.totalAnswers
    }


    private def createQuiz(int num) {
        def quiz = QuizDefFactory.createQuiz(num)
        quiz.name = "My Quiz ${num}".toString()
        skillsService.createQuizDef(quiz)
        def question = QuizDefFactory.createChoiceQuestion(num, 1, 2)
        skillsService.createQuizQuestionDef(question)
        return quiz
    }

    private def createSurvey(int num) {
        def survey = QuizDefFactory.createQuizSurvey(num)
        survey.name = "My Survey ${num}".toString()
        skillsService.createQuizDef(survey)
        def question = QuizDefFactory.createSingleChoiceSurveyQuestion(num, 1, 2)
        skillsService.createQuizQuestionDef(question)
        return survey
    }

    private def runQuizOrSurvey(SkillsService user, def quiz, boolean pass = true, boolean complete = true) {
        attemptsUsers.push(user)
        attemptsQuizzes.push(quiz)
        def quizInfo = user.getQuizInfo(quiz.quizId)
        def quizAttempt = user.startQuizAttempt(quiz.quizId).body
        quizAttempt.questions.eachWithIndex { it, index ->
            int answerIndex = 0
            if (it.questionType == QuizQuestionType.SingleChoice.toString()) {
                answerIndex = pass ? 0 : 1
            }
            user.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[index].answerOptions[answerIndex].id, [isSelected: true, answerText: 'This is user provided answer'])
        }

        if (complete) {
            user.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        }

        return quizAttempt
    }


}
