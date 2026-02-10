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

import groovy.time.TimeCategory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.UserQuizAttempt
import skills.storage.repos.UserQuizAnswerAttemptRepo
import skills.storage.repos.UserQuizAttemptRepo
import skills.storage.repos.UserQuizQuestionAttemptRepo
import spock.lang.IgnoreIf

import java.text.SimpleDateFormat

class QuizRunsSpecs extends DefaultIntSpec {

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    @Autowired
    UserQuizQuestionAttemptRepo userQuizQuestionAttemptRepo

    @Autowired
    UserQuizAnswerAttemptRepo userQuizAnswerAttemptRepo

    @Value('${skills.config.ui.usersTableAdditionalUserTagKey}')
    String usersTableAdditionalUserTagKey

    def "get quiz runs page"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(10, true)
        users.eachWithIndex { it, index ->
            runQuiz(it, quiz, quizInfo, index % 2 == 0)
        }

        when:
        def quizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        def quizRuns_pg1 = skillsService.getQuizRuns(quiz.quizId, 3, 1, 'started', true, '')
        def quizRuns_pg2 = skillsService.getQuizRuns(quiz.quizId, 3, 2, 'started', true, '')
        def quizRuns_pg3 = skillsService.getQuizRuns(quiz.quizId, 3, 3, 'started', true, '')
        def quizRuns_pg4 = skillsService.getQuizRuns(quiz.quizId, 3, 4, 'started', true, '')
        then:
        quizRuns.totalCount == users.size()
        quizRuns.count == users.size()
        quizRuns.data.userId == users
        quizRuns.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
        ]
        quizRuns.data.numberCorrect == [2, 1, 2, 1, 2, 1, 2, 1, 2, 1]
        quizRuns.data.totalAnswers == [2, 2, 2, 2, 2, 2, 2, 2, 2, 2]

        quizRuns_pg1.totalCount == users.size()
        quizRuns_pg1.count == users.size()
        quizRuns_pg1.data.userId == users[0..2]
        quizRuns_pg1.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
        ]
        quizRuns_pg1.data.numberCorrect == [2, 1, 2]
        quizRuns_pg1.data.totalAnswers == [2, 2, 2]

        quizRuns_pg2.totalCount == users.size()
        quizRuns_pg2.count == users.size()
        quizRuns_pg2.data.userId == users[3..5]
        quizRuns_pg2.data.status == [UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
        ]
        quizRuns_pg2.data.numberCorrect == [1, 2, 1]
        quizRuns_pg2.data.totalAnswers == [2, 2, 2]

        quizRuns_pg3.totalCount == users.size()
        quizRuns_pg3.count == users.size()
        quizRuns_pg3.data.userId == users[6..8]
        quizRuns_pg3.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
        ]
        quizRuns_pg3.data.numberCorrect == [2, 1, 2]
        quizRuns_pg3.data.totalAnswers == [2, 2, 2]

        quizRuns_pg4.totalCount == users.size()
        quizRuns_pg4.count == users.size()
        quizRuns_pg4.data.userId == users[9..9]
        quizRuns_pg4.data.status == [UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
        ]
        quizRuns_pg4.data.numberCorrect == [1]
        quizRuns_pg4.data.totalAnswers == [2]
    }

    def "get quiz runs empty page"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        def quizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        then:
        quizRuns.totalCount == 0
        quizRuns.count == 0
        !quizRuns.data
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "query user id"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = (1..10).collect { "user${it}"}
        users.eachWithIndex { it, index ->
            createService(it)
            runQuiz(it, quiz, quizInfo, index % 2 == 0)
        }

        when:
        def quizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, 'uSeR1')
        def quizRuns_pg1 = skillsService.getQuizRuns(quiz.quizId, 3, 1, 'started', true, 'R1')
        then:
        quizRuns.totalCount == users.size()
        quizRuns.count == 2
        quizRuns.data.userId == ["user1", "user10"]
        quizRuns.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
        ]

        quizRuns_pg1.totalCount == users.size()
        quizRuns_pg1.count == 2
        quizRuns_pg1.data.userId == ["user1", "user10"]
        quizRuns_pg1.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
        ]
    }

    def "delete quiz run"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(10, true)
        users.eachWithIndex { it, index ->
            runQuiz(it, quiz, quizInfo, index % 2 == 0)
        }
        def quizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        assert userQuizAttemptRepo.findAll().find({it.id == quizRuns.data[1].attemptId})
        assert userQuizQuestionAttemptRepo.findAll().find({it.userQuizAttemptRefId == quizRuns.data[1].attemptId})
        assert userQuizAnswerAttemptRepo.findAll().find({it.userQuizAttemptRefId == quizRuns.data[1].attemptId})
        when:
        skillsService.deleteQuizRun(quiz.quizId, quizRuns.data[1].attemptId)
        def quizRunsAfter = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        then:
        quizRuns.totalCount == users.size()
        quizRuns.count == users.size()
        quizRuns.data.userId == users

        quizRunsAfter.totalCount == users.size() - 1
        quizRunsAfter.count == users.size() - 1
        quizRunsAfter.data.userId == [users[0], users[2..users.size()-1]].flatten()

        !userQuizAttemptRepo.findAll().find({it.id == quizRuns.data[1].attemptId})
        !userQuizQuestionAttemptRepo.findAll().find({it.userQuizAttemptRefId == quizRuns.data[1].attemptId})
        !userQuizAnswerAttemptRepo.findAll().find({it.userQuizAttemptRefId == quizRuns.data[1].attemptId})
    }

    def "delete quiz run validation: attempt id does not exist"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(1, true)
        users.eachWithIndex { it, index ->
            runQuiz(it, quiz, quizInfo, index % 2 == 0)
        }
        def quizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        def badAttemptId = quizRuns.data[0].attemptId+1
        when:
        skillsService.deleteQuizRun(quiz.quizId, badAttemptId)
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Quiz attempt with id [${badAttemptId}] does not exist")
    }

    def "delete quiz run validation: attempt id is for a different quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quiz1 = QuizDefFactory.createQuiz(2, "Fancy Description")
        skillsService.createQuizDef(quiz1)
        def questions1 = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        skillsService.createQuizQuestionDefs(questions1)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(1, true)
        users.eachWithIndex { it, index ->
            runQuiz(it, quiz, quizInfo, index % 2 == 0)
        }
        def quizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        when:
        skillsService.deleteQuizRun(quiz1.quizId, quizRuns.data[0].attemptId)
        then:
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Provided attempt id [${quizRuns.data[0].attemptId}] does not belong to quiz [${quiz1.quizId}]")
    }

    def "get quiz runs page with user tags"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(10, true)

        List<String> exclude = [[DEFAULT_ROOT_USER_ID, SkillsService.UseParams.DEFAULT_USER_NAME],users].flatten()
        SkillsService rootSkillsService = createRootSkillService(getRandomUsers(1, true, exclude)[0])
        rootSkillsService.saveUserTag(users[0], usersTableAdditionalUserTagKey, ["ABC"])
        rootSkillsService.saveUserTag(users[1], usersTableAdditionalUserTagKey, ["ABC1"])

        users.eachWithIndex { it, index ->
            runQuiz(it, quiz, quizInfo, index % 2 == 0)
        }

        when:
        def quizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        then:
        quizRuns.totalCount == users.size()
        quizRuns.count == users.size()
        quizRuns.data.userId == users
        quizRuns.data.userTag == ["ABC",
                                  "ABC1",
                                  null,
                                  null,
                                  null,
                                  null,
                                  null,
                                  null,
                                  null,
                                  null,
        ]
    }

    def "get quiz runs page with date filtering"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<Date> dates = (0..3).collect { new Date() - it }.reverse()

        List<String> users = getRandomUsers(10, true)

        use (TimeCategory) {
            runQuiz(users[0], quiz, quizInfo, true, dates[0])
            runQuiz(users[1], quiz, quizInfo, true, dates[0] + 30.minutes)
            runQuiz(users[2], quiz, quizInfo, false, dates[0] + 45.minutes)
            runQuiz(users[3], quiz, quizInfo, true, dates[0] + 60.minutes)
            runQuiz(users[4], quiz, quizInfo, true, dates[1])
            runQuiz(users[5], quiz, quizInfo, false, dates[1] + 30.minutes)
            runQuiz(users[6], quiz, quizInfo, true, dates[1] + 60.minutes)
            runQuiz(users[7], quiz, quizInfo, true, dates[2])
            runQuiz(users[8], quiz, quizInfo, false, dates[2] + 30.minutes)
            runQuiz(users[9], quiz, quizInfo, true, dates[2] + 60.minutes)
        }

        def format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        when:
        def quizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '', format.format(dates[0]), format.format(dates[3]))
        def quizRuns_filter1 = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '', format.format(dates[0]), format.format(dates[1]))
        def quizRuns_filter2 = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '', format.format(dates[1]), format.format(dates[2]))
        def quizRuns_filter3 = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '', format.format(dates[2]), format.format(dates[3]))
        def quizRuns_filter4 = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '', format.format(dates[1]), format.format(dates[3]))
        then:
        quizRuns.totalCount == users.size()
        quizRuns.count == users.size()
        quizRuns.data.userId == users
        quizRuns.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                 UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
        ]

        quizRuns_filter1.count == 7
        quizRuns_filter1.data.userId == users[0..6]
        quizRuns_filter1.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
        ]

        quizRuns_filter2.totalCount == users.size()
        quizRuns_filter2.count == 6
        quizRuns_filter2.data.userId == users[4..9]
        quizRuns_filter2.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
        ]

        quizRuns_filter3.totalCount == users.size()
        quizRuns_filter3.count == 3
        quizRuns_filter3.data.userId == users[7..9]
        quizRuns_filter3.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
        ]

        quizRuns_filter4.totalCount == users.size()
        quizRuns_filter4.count == 6
        quizRuns_filter4.data.userId == users[4..9]
        quizRuns_filter4.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
        ]

    }

    void runQuiz(String userId, def quiz, def quizInfo, boolean pass, Date startDate = null) {
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, userId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, userId)
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[pass ? 0 : 1].id, userId)
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, userId).body

        if(startDate) {
            UserQuizAttempt userQuizAttempt = userQuizAttemptRepo.findById(quizAttempt.id).get()
            userQuizAttempt.started = startDate
            userQuizAttemptRepo.save(userQuizAttempt)
        }
    }

}
