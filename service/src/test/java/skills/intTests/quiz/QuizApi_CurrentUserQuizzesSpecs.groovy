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
import skills.services.quiz.QuizQuestionType
import skills.storage.model.QuizDefParent
import skills.storage.model.UserAttrs
import skills.storage.model.UserQuizAttempt

import java.time.Instant

class QuizApi_CurrentUserQuizzesSpecs extends DefaultIntSpec {

    def "quiz attempts - no attempts yet"() {
        when:
        def res = skillsService.getCurrentUserQuizAttempts()
        then:
        res.count == 0
        res.totalCount == 0
        res.data == []
    }

    private static Integer runQuizOrSurvey(SkillsService service, Integer num, int answerNumToReport = 0, boolean complete = true) {
        String quizId = QuizDefFactory.getDefaultQuizId(num)
        def quizAttempt =  service.startQuizAttempt(quizId).body
        service.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[answerNumToReport].id)
        if (complete) {
            service.completeQuizAttempt(quizId, quizAttempt.id)
        }
        return quizAttempt.id
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

    def "attempt info - passed quiz with all questions types"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.MultipleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 3, QuizQuestionType.SingleChoice),
                QuizDefFactory.createTextInputQuestion(1, 3)
        ]
        skillsService.createQuizQuestionDefs(questions)

        String quizId = quiz.quizId
        def quizAttempt =  skillsService.startQuizAttempt(quizId).body
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[2].id)
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected: true, answerText: "answer"])
        skillsService.completeQuizAttempt(quizId, quizAttempt.id)
        skillsService.gradeAnswer(skillsService.userName, quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, true, "Good answer")

        def now = new Date()
        def fiveMinutesAgo = now - 5 * 60 * 1000
        def fiveMinutesFromNow = now + 5 * 60 * 1000

        when:
        def res = skillsService.getCurrentUserSingleQuizAttempt(quizAttempt.id)
        then:
        res.quizName == "Test Quiz #1"
        res.userId == skillsService.userName
        res.quizType == QuizDefParent.QuizType.Quiz.toString()
        res.status == UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        res.allQuestionsReturned == true
        res.numQuestions == 3
        res.numQuestionsPassed == 3
        Date started = Date.from(Instant.parse(res.started))
        started >= fiveMinutesAgo && started <= fiveMinutesFromNow
        Date completed = Date.from(Instant.parse(res.completed))
        completed >= fiveMinutesAgo && completed <= fiveMinutesFromNow

        def q = res.questions
        q.questionNum == [1, 2, 3]
        q.question == ["This is questions #1", "This is questions #2", "This is questions #3"]
        q.questionType == [QuizQuestionType.MultipleChoice.toString(), QuizQuestionType.SingleChoice.toString(), QuizQuestionType.TextInput.toString()]
        q.isCorrect == [true, true, true]
        q.needsGrading == [false, false, false]
        q[0].answers.answer == ["Answer #1", "Answer #2", "Answer #3", "Answer #4"]
        q[0].answers.isConfiguredCorrect == [true, false, true, false]
        q[0].answers.isSelected == [true, false, true, false]
        q[0].answers.needsGrading == [false, false, false, false]
        q[0].answers.gradingResult == [null, null, null, null]

        q[1].answers.answer == ["Answer #1", "Answer #2", "Answer #3"]
        q[1].answers.isConfiguredCorrect == [true, false, false]
        q[1].answers.isSelected == [true, false, false]
        q[1].answers.needsGrading == [false, false, false]
        q[1].answers.gradingResult == [null, null, null]

        q[2].answers.answer == ["answer"]
        q[2].answers.isConfiguredCorrect == [false]
        q[2].answers.isSelected == [true]
        q[2].answers.needsGrading == [false]
        q[2].answers.gradingResult.graderUserId == [skillsService.userName]
        q[2].answers.gradingResult.feedback == ["Good answer"]
        UserAttrs userAttrs =userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        q[2].answers.gradingResult.graderUserIdForDisplay == [userAttrs.userIdForDisplay]
        q[2].answers.gradingResult.graderFirstname == [userAttrs.firstName]
        q[2].answers.gradingResult.graderLastname == [userAttrs.lastName]
        Date gradedOn = Date.from(Instant.parse(q[2].answers.gradingResult.gradedOn[0]))
        gradedOn >= fiveMinutesAgo && gradedOn <= fiveMinutesFromNow
    }

    def "attempt info - failed quiz with all questions types - quizAlwaysShowCorrectAnswers=true"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.MultipleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 3, QuizQuestionType.SingleChoice),
                QuizDefFactory.createTextInputQuestion(1, 3)
        ]
        skillsService.createQuizQuestionDefs(questions)
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.AlwaysShowCorrectAnswers.setting, value: true],
        ])

        String quizId = quiz.quizId
        def quizAttempt =  skillsService.startQuizAttempt(quizId).body
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[2].id)
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id)
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected: true, answerText: "answer"])
        skillsService.completeQuizAttempt(quizId, quizAttempt.id)
        skillsService.gradeAnswer(skillsService.userName, quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, false, "Bad answer")

        def now = new Date()
        def fiveMinutesAgo = now - 5 * 60 * 1000
        def fiveMinutesFromNow = now + 5 * 60 * 1000

        when:
        def res = skillsService.getCurrentUserSingleQuizAttempt(quizAttempt.id)
        then:
        res.quizName == "Test Quiz #1"
        res.userId == skillsService.userName
        res.quizType == QuizDefParent.QuizType.Quiz.toString()
        res.status == UserQuizAttempt.QuizAttemptStatus.FAILED.toString()
        res.allQuestionsReturned == true
        res.numQuestions == 3
        res.numQuestionsPassed == 0
        Date started = Date.from(Instant.parse(res.started))
        started >= fiveMinutesAgo && started <= fiveMinutesFromNow
        Date completed = Date.from(Instant.parse(res.completed))
        completed >= fiveMinutesAgo && completed <= fiveMinutesFromNow

        def q = res.questions
        q.questionNum == [1, 2, 3]
        q.question == ["This is questions #1", "This is questions #2", "This is questions #3"]
        q.questionType == [QuizQuestionType.MultipleChoice.toString(), QuizQuestionType.SingleChoice.toString(), QuizQuestionType.TextInput.toString()]
        q.isCorrect == [false, false, false]
        q.needsGrading == [false, false, false]
        q[0].answers.answer == ["Answer #1", "Answer #2", "Answer #3", "Answer #4"]
        q[0].answers.isConfiguredCorrect == [true, false, true, false]
        q[0].answers.isSelected == [false, true, true, false]
        q[0].answers.needsGrading == [false, false, false, false]
        q[0].answers.gradingResult == [null, null, null, null]

        q[1].answers.answer == ["Answer #1", "Answer #2", "Answer #3"]
        q[1].answers.isConfiguredCorrect == [true, false, false]
        q[1].answers.isSelected == [false, true, false]
        q[1].answers.needsGrading == [false, false, false]
        q[1].answers.gradingResult == [null, null, null]

        q[2].answers.answer == ["answer"]
        q[2].answers.isConfiguredCorrect == [false]
        q[2].answers.isSelected == [true]
        q[2].answers.needsGrading == [false]
        q[2].answers.gradingResult.graderUserId == [skillsService.userName]
        q[2].answers.gradingResult.feedback == ["Bad answer"]
        UserAttrs userAttrs =userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        q[2].answers.gradingResult.graderUserIdForDisplay == [userAttrs.userIdForDisplay]
        q[2].answers.gradingResult.graderFirstname == [userAttrs.firstName]
        q[2].answers.gradingResult.graderLastname == [userAttrs.lastName]
        Date gradedOn = Date.from(Instant.parse(q[2].answers.gradingResult.gradedOn[0]))
        gradedOn >= fiveMinutesAgo && gradedOn <= fiveMinutesFromNow
    }

    def "attempt info - failed quiz does not return graded results on all attempts - quizAlwaysShowCorrectAnswers=false"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.MultipleChoice),
        ]
        skillsService.createQuizQuestionDefs(questions)
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MaxNumAttempts.setting, value: 3],
        ])
        String quizId = quiz.quizId

        when:
        def quizAttempt1 =  skillsService.startQuizAttempt(quizId).body
        skillsService.reportQuizAnswer(quizId, quizAttempt1.id, quizAttempt1.questions[0].answerOptions[1].id)
        def attempt1Res = skillsService.completeQuizAttempt(quizId, quizAttempt1.id).body
        def attempt1Info = skillsService.getCurrentUserSingleQuizAttempt(quizAttempt1.id)

        def quizAttempt2 =  skillsService.startQuizAttempt(quizId).body
        skillsService.reportQuizAnswer(quizId, quizAttempt2.id, quizAttempt2.questions[0].answerOptions[1].id)
        def attempt2Res = skillsService.completeQuizAttempt(quizId, quizAttempt2.id).body
        def attempt2Info = skillsService.getCurrentUserSingleQuizAttempt(quizAttempt2.id)

        def quizAttempt3 =  skillsService.startQuizAttempt(quizId).body
        skillsService.reportQuizAnswer(quizId, quizAttempt3.id, quizAttempt3.questions[0].answerOptions[1].id)
        def attempt3Res = skillsService.completeQuizAttempt(quizId, quizAttempt3.id).body
        def attempt3Info = skillsService.getCurrentUserSingleQuizAttempt(quizAttempt3.id)

        then:
        !attempt1Res.gradedQuestions
        !attempt1Info.questions

        !attempt2Res.gradedQuestions
        !attempt2Info.questions

        !attempt3Res.gradedQuestions
        !attempt3Info.questions
    }

    def "attempt info - failed quiz returns graded results on all attempts - quizAlwaysShowCorrectAnswers=true"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.MultipleChoice),
        ]
        skillsService.createQuizQuestionDefs(questions)
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MaxNumAttempts.setting, value: 3],
                [setting: QuizSettings.AlwaysShowCorrectAnswers.setting, value: true],
        ])
        String quizId = quiz.quizId

        when:
        def quizAttempt1 =  skillsService.startQuizAttempt(quizId).body
        skillsService.reportQuizAnswer(quizId, quizAttempt1.id, quizAttempt1.questions[0].answerOptions[1].id)
        def attempt1Res = skillsService.completeQuizAttempt(quizId, quizAttempt1.id).body
        def attempt1Info = skillsService.getCurrentUserSingleQuizAttempt(quizAttempt1.id)

        def quizAttempt2 =  skillsService.startQuizAttempt(quizId).body
        skillsService.reportQuizAnswer(quizId, quizAttempt2.id, quizAttempt2.questions[0].answerOptions[1].id)
        def attempt2Res = skillsService.completeQuizAttempt(quizId, quizAttempt2.id).body
        def attempt2Info = skillsService.getCurrentUserSingleQuizAttempt(quizAttempt2.id)

        def quizAttempt3 =  skillsService.startQuizAttempt(quizId).body
        skillsService.reportQuizAnswer(quizId, quizAttempt3.id, quizAttempt3.questions[0].answerOptions[1].id)
        def attempt3Res = skillsService.completeQuizAttempt(quizId, quizAttempt3.id).body
        def attempt3Info = skillsService.getCurrentUserSingleQuizAttempt(quizAttempt3.id)

        then:
        attempt1Res.gradedQuestions
        attempt1Info.questions

        attempt2Res.gradedQuestions
        attempt2Info.questions

        attempt3Res.gradedQuestions
        attempt3Info.questions
    }

    def "attempt info - failed quiz with all questions types - only TextInput questions are returned"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.MultipleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 3, QuizQuestionType.SingleChoice),
                QuizDefFactory.createTextInputQuestion(1, 3)
        ]
        skillsService.createQuizQuestionDefs(questions)

        String quizId = quiz.quizId
        def quizAttempt =  skillsService.startQuizAttempt(quizId).body
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[2].id)
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id)
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected: true, answerText: "answer"])
        skillsService.completeQuizAttempt(quizId, quizAttempt.id)
        skillsService.gradeAnswer(skillsService.userName, quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, false, "Bad answer")

        def now = new Date()
        def fiveMinutesAgo = now - 5 * 60 * 1000
        def fiveMinutesFromNow = now + 5 * 60 * 1000

        when:
        def res = skillsService.getCurrentUserSingleQuizAttempt(quizAttempt.id)
        then:
        res.quizName == "Test Quiz #1"
        res.userId == skillsService.userName
        res.quizType == QuizDefParent.QuizType.Quiz.toString()
        res.status == UserQuizAttempt.QuizAttemptStatus.FAILED.toString()
        res.allQuestionsReturned == false
        res.numQuestions == 3
        res.numQuestionsPassed == 0
        Date started = Date.from(Instant.parse(res.started))
        started >= fiveMinutesAgo && started <= fiveMinutesFromNow
        Date completed = Date.from(Instant.parse(res.completed))
        completed >= fiveMinutesAgo && completed <= fiveMinutesFromNow
        def q = res.questions
        q.questionNum == [3]
        q.question == ["This is questions #3"]
        q.questionType == [QuizQuestionType.TextInput.toString()]
        q.isCorrect == [false]
        q.needsGrading == [false]
        q[0].answers.answer == ["answer"]
        q[0].answers.isConfiguredCorrect == [false]
        q[0].answers.isSelected == [true]
        q[0].answers.needsGrading == [false]
        q[0].answers.gradingResult.graderUserId == [skillsService.userName]
        q[0].answers.gradingResult.feedback == ["Bad answer"]
        UserAttrs userAttrs =userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        q[0].answers.gradingResult.graderUserIdForDisplay == [userAttrs.userIdForDisplay]
        q[0].answers.gradingResult.graderFirstname == [userAttrs.firstName]
        q[0].answers.gradingResult.graderLastname == [userAttrs.lastName]
        Date gradedOn = Date.from(Instant.parse(q[0].answers.gradingResult.gradedOn[0]))
        gradedOn >= fiveMinutesAgo && gradedOn <= fiveMinutesFromNow
    }

    def "attempt info - failed quiz with all questions types - no questions returned"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.MultipleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 3, QuizQuestionType.SingleChoice),
        ]
        skillsService.createQuizQuestionDefs(questions)

        String quizId = quiz.quizId
        def quizAttempt =  skillsService.startQuizAttempt(quizId).body
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[2].id)
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id)
        skillsService.completeQuizAttempt(quizId, quizAttempt.id)

        def now = new Date()
        def fiveMinutesAgo = now - 5 * 60 * 1000
        def fiveMinutesFromNow = now + 5 * 60 * 1000

        when:
        def res = skillsService.getCurrentUserSingleQuizAttempt(quizAttempt.id)
        then:
        res.quizName == "Test Quiz #1"
        res.userId == skillsService.userName
        res.quizType == QuizDefParent.QuizType.Quiz.toString()
        res.status == UserQuizAttempt.QuizAttemptStatus.FAILED.toString()
        res.allQuestionsReturned == false
        res.numQuestions == 2
        res.numQuestionsPassed == 0
        Date started = Date.from(Instant.parse(res.started))
        started >= fiveMinutesAgo && started <= fiveMinutesFromNow
        Date completed = Date.from(Instant.parse(res.completed))
        completed >= fiveMinutesAgo && completed <= fiveMinutesFromNow
        !res.questions
    }

    def "attempt info - needs grading - questions are not returned"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.MultipleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 3, QuizQuestionType.SingleChoice),
                QuizDefFactory.createTextInputQuestion(1, 3)
        ]
        skillsService.createQuizQuestionDefs(questions)

        String quizId = quiz.quizId
        def quizAttempt =  skillsService.startQuizAttempt(quizId).body
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[2].id)
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        skillsService.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected: true, answerText: "answer"])
        skillsService.completeQuizAttempt(quizId, quizAttempt.id)

        def now = new Date()
        def fiveMinutesAgo = now - 5 * 60 * 1000
        def fiveMinutesFromNow = now + 5 * 60 * 1000

        when:
        def res = skillsService.getCurrentUserSingleQuizAttempt(quizAttempt.id)
        then:
        res.quizName == "Test Quiz #1"
        res.userId == skillsService.userName
        res.quizType == QuizDefParent.QuizType.Quiz.toString()
        res.status == UserQuizAttempt.QuizAttemptStatus.NEEDS_GRADING.toString()
        res.allQuestionsReturned == false
        res.numQuestions == 3
        res.numQuestionsPassed == 2
        Date started = Date.from(Instant.parse(res.started))
        started >= fiveMinutesAgo && started <= fiveMinutesFromNow
        Date completed = Date.from(Instant.parse(res.completed))
        completed >= fiveMinutesAgo && completed <= fiveMinutesFromNow

        !res.questions
    }

    def "quiz with sub set of questions in an attempt - passed"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 1, 2),
                QuizDefFactory.createChoiceQuestion(1, 2, 2),
                QuizDefFactory.createChoiceQuestion(1, 3, 2),
                QuizDefFactory.createChoiceQuestion(1, 4, 2)
        ]
        questions.each {
            it.answers[0].isCorrect = true
            it.answers[1].isCorrect = false
        }
        skillsService.createQuizQuestionDefs(questions)
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.QuizLength.setting, value: 2],
        ])

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id)

        when:
        def res = skillsService.getCurrentUserSingleQuizAttempt(quizAttempt.id)
        then:
        res.status == UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        res.numQuestions == 2
        res.numQuestionsToPass == 2
        res.numQuestionsPassed == 2
        res.allQuestionsReturned == true
        res.questions.size() == 2
    }

    def "quiz with sub set of questions in an attempt - failed"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createChoiceQuestion(1, 1, 2),
                QuizDefFactory.createChoiceQuestion(1, 2, 2),
                QuizDefFactory.createChoiceQuestion(1, 3, 2),
                QuizDefFactory.createChoiceQuestion(1, 4, 2)
        ]
        questions.each {
            it.answers[0].isCorrect = true
            it.answers[1].isCorrect = false
        }
        skillsService.createQuizQuestionDefs(questions)
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.QuizLength.setting, value: 2],
        ])

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id)
        skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id)

        when:
        def res = skillsService.getCurrentUserSingleQuizAttempt(quizAttempt.id)
        then:
        res.status == UserQuizAttempt.QuizAttemptStatus.FAILED.toString()
        res.numQuestions == 2
        res.numQuestionsToPass == 2
        res.numQuestionsPassed == 1
        res.allQuestionsReturned == false
        !res.questions
    }

}


