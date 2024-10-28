/**
 * Copyright 2024 SkillTree
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
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.storage.model.QuizDefParent
import skills.storage.model.UserQuizAttempt

class QuizApi_CurrentUserQuizzesSpecs extends DefaultIntSpec {

    def "quiz attempts - no attempts yet"() {
        when:
        def res = skillsService.getCurrentUserQuizAttempts()
        then:
        res.count == 0
        res.totalCount == 0
        res.data == []
    }

    private static void runQuizOrSurvey(SkillsService service, Integer num, int answerNumToReport = 0, boolean complete = true) {
        String quizId = QuizDefFactory.getDefaultQuizId(num)
        def quizInfo = service.getQuizInfo(quizId)
        def quizAttempt =  service.startQuizAttempt(quizId).body
        service.reportQuizAnswer(quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[answerNumToReport].id)
        if (complete) {
            service.completeQuizAttempt(quizId, quizAttempt.id)
        }
    }
    private static void createSimpleSurvey(SkillsService service, Integer num) {
        def survey = QuizDefFactory.createQuizSurvey(num)
        service.createQuizDef(survey)
        def questions = [ QuizDefFactory.createSingleChoiceSurveyQuestion(num)]
        service.createQuizQuestionDefs(questions)
        service.saveQuizSettings(survey.quizId, [
                [setting: QuizSettings.MultipleTakes.setting, value: true],
        ])
    }
    private static void createSimpleQuiz(SkillsService service, Integer num, String name = null) {
        def quiz = QuizDefFactory.createQuiz(num)
        if (name) {
            quiz.name = name
        }
        service.createQuizDef(quiz)
        def questions = [ QuizDefFactory.createChoiceQuestion(num)]
        service.createQuizQuestionDefs(questions)
        service.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MultipleTakes.setting, value: true],
        ])
    }

    def "get quiz attempts with quizzes and surveys"() {
        createSimpleSurvey(skillsService, 1)
        createSimpleQuiz(skillsService, 2)
        runQuizOrSurvey(skillsService, 1)
        runQuizOrSurvey(skillsService, 2)
        runQuizOrSurvey(skillsService, 2, 1 )

        SkillsService otherUser = createService("otherUser")

        runQuizOrSurvey(otherUser, 2, 1)

        when:
        def res = skillsService.getCurrentUserQuizAttempts()
        def user1Res = otherUser.getCurrentUserQuizAttempts()
        then:
        res.count == 3
        res.totalCount == 3
        res.data.quizId == [
                QuizDefFactory.getDefaultQuizId(1),
                QuizDefFactory.getDefaultQuizId(2),
                QuizDefFactory.getDefaultQuizId(2),
        ]
        res.data.status == [
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString()
        ]
        res.data.quizType == [
                QuizDefParent.QuizType.Survey.toString(),
                QuizDefParent.QuizType.Quiz.toString(),
                QuizDefParent.QuizType.Quiz.toString(),
        ]
        res.data.quizName == [
                "Test Quiz #1",
                "Test Quiz #2",
                "Test Quiz #2",
        ]
        def user1Attempt1 = skillsService.getCurrentUserSingleQuizAttempt(res.data[0].attemptId)
        user1Attempt1.quizType == QuizDefParent.QuizType.Survey.toString()
        def user1Attempt2 = skillsService.getCurrentUserSingleQuizAttempt(res.data[1].attemptId)
        user1Attempt2.quizType == QuizDefParent.QuizType.Quiz.toString()
        def user1Attempt3 = skillsService.getCurrentUserSingleQuizAttempt(res.data[2].attemptId)
        user1Attempt3.quizType == QuizDefParent.QuizType.Quiz.toString()

        user1Res.count == 1
        user1Res.totalCount == 1
        user1Res.data.quizId == [
                QuizDefFactory.getDefaultQuizId(2),
        ]
        user1Res.data.status == [
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString()
        ]
        user1Res.data.quizType == [
                QuizDefParent.QuizType.Quiz.toString(),
        ]
        user1Res.data.quizName == [
                "Test Quiz #2",
        ]
        !res.data.attemptId.contains(user1Res.data[0].attemptId)
    }

    def "get quiz attempts - paging"() {
        10.times {
            createSimpleQuiz(skillsService, it+1)
            runQuizOrSurvey(skillsService, it+1)
        }

        when:
        def res = skillsService.getCurrentUserQuizAttempts("", 3, 1)
        def res_pg1 = skillsService.getCurrentUserQuizAttempts("", 3, 2)
        def res_pg2 = skillsService.getCurrentUserQuizAttempts("", 3, 3)
        def res_pg3 = skillsService.getCurrentUserQuizAttempts("", 3, 4)
        then:
        res.data.quizName == [
                "Test Quiz #1",
                "Test Quiz #2",
                "Test Quiz #3",
        ]
        res_pg1.data.quizName == [
                "Test Quiz #4",
                "Test Quiz #5",
                "Test Quiz #6",
        ]
        res_pg2.data.quizName == [
                "Test Quiz #7",
                "Test Quiz #8",
                "Test Quiz #9",
        ]
        res_pg3.data.quizName == [
                "Test Quiz #10",
        ]
    }

    def "get quiz attempts - sorting"() {
        10.times {
            createSimpleQuiz(skillsService, it+1)
            runQuizOrSurvey(skillsService, it+1)
        }

        when:
        def res = skillsService.getCurrentUserQuizAttempts("", 5, 1, "quizName", true)
        def res_pg2 = skillsService.getCurrentUserQuizAttempts("", 5, 2, "quizName", true)


        def res_reverse = skillsService.getCurrentUserQuizAttempts("", 5, 1, "quizName", false)
        def res_pg2_reverse = skillsService.getCurrentUserQuizAttempts("", 5, 2, "quizName", false)
        then:
        res.data.quizName == [
                "Test Quiz #1",
                "Test Quiz #10",
                "Test Quiz #2",
                "Test Quiz #3",
                "Test Quiz #4",
        ]
        res_pg2.data.quizName == [
                "Test Quiz #5",
                "Test Quiz #6",
                "Test Quiz #7",
                "Test Quiz #8",
                "Test Quiz #9",
        ]

        res_reverse.data.quizName == [
                "Test Quiz #9",
                "Test Quiz #8",
                "Test Quiz #7",
                "Test Quiz #6",
                "Test Quiz #5",
        ]
        res_pg2_reverse.data.quizName == [
                "Test Quiz #4",
                "Test Quiz #3",
                "Test Quiz #2",
                "Test Quiz #10",
                "Test Quiz #1",
        ]
    }

    def "get quiz attempts - quiz name filter"() {
        12.times {
            createSimpleQuiz(skillsService, it+1, "Test Quiz ${it+1}")
            runQuizOrSurvey(skillsService, it+1)
        }

        when:
        def res = skillsService.getCurrentUserQuizAttempts("qUiZ 1", 5, 1, "quizName", true)
        def res1 = skillsService.getCurrentUserQuizAttempts("quiz 12", 5, 1, "quizName", true)
        then:
        res.data.quizName == [
                "Test Quiz 1",
                "Test Quiz 10",
                "Test Quiz 11",
                "Test Quiz 12",
        ]

        res1.data.quizName == [
                "Test Quiz 12",
        ]
    }

    def "get quiz attempts - do not return in progress attempts"() {
        10.times {
            createSimpleQuiz(skillsService, it+1)
            runQuizOrSurvey(skillsService, it+1, 0, it % 2 == 0)
        }

        when:
        def res = skillsService.getCurrentUserQuizAttempts("", 20, 1)
        then:
        res.data.quizName == [
                "Test Quiz #1",
                "Test Quiz #3",
                "Test Quiz #5",
                "Test Quiz #7",
                "Test Quiz #9",
        ]
        res.data.status == [
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
        ]
    }

    def "cannot get an attempt id for another user"() {
        createSimpleQuiz(skillsService, 1)
        runQuizOrSurvey(skillsService, 1)

        SkillsService otherUser = createService("otherUser")
        runQuizOrSurvey(otherUser, 1)

        def user2Attempts = otherUser.getCurrentUserQuizAttempts()
        when:
        int attemptId = user2Attempts.data[0].attemptId
        skillsService.getCurrentUserSingleQuizAttempt(attemptId)
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.resBody.contains("Provided quiz attempt id [${attemptId}] is not for [${skillsService.userName}] user")
    }
}

