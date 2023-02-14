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
import skills.storage.model.UserQuizAttempt
import spock.lang.IgnoreIf

class QuizRunsSpecs extends DefaultIntSpec {

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
        println JsonOutput.prettyPrint(JsonOutput.toJson(quizRuns))
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

        quizRuns_pg1.totalCount == users.size()
        quizRuns_pg1.count == users.size()
        quizRuns_pg1.data.userId == users[0..2]
        quizRuns_pg1.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
        ]

        quizRuns_pg2.totalCount == users.size()
        quizRuns_pg2.count == users.size()
        quizRuns_pg2.data.userId == users[3..5]
        quizRuns_pg2.data.status == [UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
        ]

        quizRuns_pg3.totalCount == users.size()
        quizRuns_pg3.count == users.size()
        quizRuns_pg3.data.userId == users[6..8]
        quizRuns_pg3.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                                     UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
        ]

        quizRuns_pg4.totalCount == users.size()
        quizRuns_pg4.count == users.size()
        quizRuns_pg4.data.userId == users[9..9]
        quizRuns_pg4.data.status == [UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
        ]

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
        println JsonOutput.prettyPrint(JsonOutput.toJson(quizRuns))
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


    void runQuiz(String userId, def quiz, def quizInfo, boolean pass) {
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, userId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, userId)
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[pass ? 0 : 1].id, userId)
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, userId).body
    }

}
