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
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.storage.model.UserQuizAttempt
import skills.storage.repos.UserQuizAttemptRepo

class QuizUsageOverTimeSpecs extends DefaultIntSpec {

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    def "get usage over time"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quiz2 = QuizDefFactory.createQuiz(2, "Fancy Description")
        skillsService.createQuizDef(quiz2)
        def questions2 = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        skillsService.createQuizQuestionDefs(questions2)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quiz2Info = skillsService.getQuizInfo(quiz2.quizId)

        List<String> users = getRandomUsers(10, true)
        List<Date> dates = (0..3).collect { new Date() - it }.reverse()

        runQuiz(users[0], quiz, quizInfo, true, dates[0], true)
        runQuiz(users[1], quiz, quizInfo, true, dates[0], true)
        runQuiz(users[2], quiz, quizInfo, true, dates[0], false)
        runQuiz(users[3], quiz, quizInfo, true, dates[0], true)
        runQuiz(users[4], quiz, quizInfo, true, dates[1], true)
        runQuiz(users[5], quiz, quizInfo, true, dates[1], false)
        runQuiz(users[6], quiz, quizInfo, true, dates[2], false)
        runQuiz(users[7], quiz, quizInfo, true, dates[3], true)
        runQuiz(users[8], quiz, quizInfo, true, dates[3], false)
        runQuiz(users[9], quiz, quizInfo, true, dates[3], true)

        runQuiz(users[1], quiz2, quiz2Info, true, dates[0], true)
        runQuiz(users[2], quiz2, quiz2Info, true, dates[2], true)
        runQuiz(users[3], quiz2, quiz2Info, true, dates[2], true)

        when:
        def q1UsageOverTime = skillsService.getUsageOverTime(quiz.quizId)
        def q2UsageOverTime = skillsService.getUsageOverTime(quiz2.quizId)
        then:
        q1UsageOverTime.value == dates.collect { new Date(it.time).clearTime() }.time
        q1UsageOverTime.count == [4, 2, 1, 3]

        q2UsageOverTime.value == dates.collect { new Date(it.time).clearTime() }.time
        q2UsageOverTime.count == [1, 0, 2, 0]
    }

    def "get usage over time - will in lengthy gaps"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(5, true)
        List<Date> dates = (0..100).collect { new Date() - it }.reverse()

        runQuiz(users[0], quiz, quizInfo, true, dates[0], true)
        runQuiz(users[1], quiz, quizInfo, true, dates[0], true)

        runQuiz(users[2], quiz, quizInfo, true, dates[30], false)

        runQuiz(users[3], quiz, quizInfo, true, dates[50], false)
        runQuiz(users[4], quiz, quizInfo, true, dates[51], true)

        List<Integer> expectedCounts = [2]
        29.times { expectedCounts.add(0)}
        expectedCounts.add(1)
        19.times { expectedCounts.add(0)}
        expectedCounts.addAll([1, 1])
        49.times { expectedCounts.add(0)}

        when:
        def q1UsageOverTime = skillsService.getUsageOverTime(quiz.quizId)
        then:
        q1UsageOverTime.value == dates.collect { new Date(it.time).clearTime() }.time
        q1UsageOverTime.count == expectedCounts
    }

    def "get usage over time - empty"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quiz2 = QuizDefFactory.createQuiz(2, "Fancy Description")
        skillsService.createQuizDef(quiz2)
        def questions2 = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        skillsService.createQuizQuestionDefs(questions2)

        when:
        def q1UsageOverTime = skillsService.getUsageOverTime(quiz.quizId)
        then:
        !q1UsageOverTime
    }


    void runQuiz(String userId, def quiz, def quizInfo, boolean pass, Date startDate, boolean complete = true) {
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, userId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, userId)
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[pass ? 0 : 1].id, userId)
        if (complete) {
            skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, userId).body
        }

        UserQuizAttempt userQuizAttempt = userQuizAttemptRepo.findById(quizAttempt.id).get()
        userQuizAttempt.started = startDate
        userQuizAttemptRepo.save(userQuizAttempt)
    }

}
