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
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.UserQuizAttempt
import skills.storage.repos.UserQuizAttemptRepo

import java.text.SimpleDateFormat

class QuizUserTagsSpecs extends DefaultIntSpec {

    @Value('${skills.config.ui.usersTableAdditionalUserTagKey}')
    String usersTableAdditionalUserTagKey

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    def "get user tag counts"() {
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

        List<String> exclude = [[DEFAULT_ROOT_USER_ID, SkillsService.UseParams.DEFAULT_USER_NAME],users].flatten()
        SkillsService rootSkillsService = createRootSkillService(getRandomUsers(1, true, exclude)[0])
        rootSkillsService.saveUserTag(users[0], usersTableAdditionalUserTagKey, ["ABC"])
        rootSkillsService.saveUserTag(users[1], usersTableAdditionalUserTagKey, ["ABC1"])
        rootSkillsService.saveUserTag(users[2], usersTableAdditionalUserTagKey, ["ABC1"])
        rootSkillsService.saveUserTag(users[3], usersTableAdditionalUserTagKey, ["ABC2"])
        rootSkillsService.saveUserTag(users[4], usersTableAdditionalUserTagKey, ["ABC2"])
        rootSkillsService.saveUserTag(users[5], usersTableAdditionalUserTagKey, ["ABC2"])
        rootSkillsService.saveUserTag(users[6], usersTableAdditionalUserTagKey, ["ABC2"])

        users.eachWithIndex { it, index ->
            runQuiz(it, quiz, quizInfo, index % 2 == 0, index % 2 == 0)
        }

        runQuiz(users[1], quiz2, quiz2Info, true, true)
        runQuiz(users[2], quiz2, quiz2Info, true, true)
        runQuiz(users[3], quiz2, quiz2Info, true, true)

        when:
        def q1UserTagCounts = skillsService.getQuizUserTagCounts(quiz.quizId, usersTableAdditionalUserTagKey)
        def q2UserTagCounts = skillsService.getQuizUserTagCounts(quiz2.quizId, usersTableAdditionalUserTagKey)
        then:
        q1UserTagCounts.value == ["ABC2", "ABC1", "ABC"]
        q1UserTagCounts.count == [4, 2, 1]

        q2UserTagCounts.value == ["ABC1", "ABC2"]
        q2UserTagCounts.count == [2, 1]
    }

    def "get user tag counts - empty"() {
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

        users.eachWithIndex { it, index ->
            runQuiz(it, quiz, quizInfo, index % 2 == 0, index % 2 == 0)
        }

        when:
        def q1UserTagCounts = skillsService.getQuizUserTagCounts(quiz.quizId, usersTableAdditionalUserTagKey)
        then:
        !q1UserTagCounts
    }

    def "get user tag counts filtered by date"() {
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

        List<String> exclude = [[DEFAULT_ROOT_USER_ID, SkillsService.UseParams.DEFAULT_USER_NAME],users].flatten()
        SkillsService rootSkillsService = createRootSkillService(getRandomUsers(1, true, exclude)[0])
        rootSkillsService.saveUserTag(users[0], usersTableAdditionalUserTagKey, ["ABC"])
        rootSkillsService.saveUserTag(users[1], usersTableAdditionalUserTagKey, ["ABC1"])
        rootSkillsService.saveUserTag(users[2], usersTableAdditionalUserTagKey, ["ABC1"])
        rootSkillsService.saveUserTag(users[3], usersTableAdditionalUserTagKey, ["ABC2"])
        rootSkillsService.saveUserTag(users[4], usersTableAdditionalUserTagKey, ["ABC2"])
        rootSkillsService.saveUserTag(users[5], usersTableAdditionalUserTagKey, ["ABC2"])
        rootSkillsService.saveUserTag(users[6], usersTableAdditionalUserTagKey, ["ABC2"])
        rootSkillsService.saveUserTag(users[7], usersTableAdditionalUserTagKey, ["ABC3"])
        rootSkillsService.saveUserTag(users[8], usersTableAdditionalUserTagKey, ["ABC3"])
        rootSkillsService.saveUserTag(users[9], usersTableAdditionalUserTagKey, ["ABC3"])

        List<Date> dates = (0..3).collect { new Date() - it }.reverse()

        users.eachWithIndex { it, index ->
            runQuiz(it, quiz, quizInfo, index % 2 == 0, index % 2 == 0, dates[0])
        }

        runQuiz(users[0], quiz2, quiz2Info, true, true, dates[0])
        runQuiz(users[1], quiz2, quiz2Info, true, true, dates[0])
        runQuiz(users[2], quiz2, quiz2Info, true, false, dates[0])
        runQuiz(users[3], quiz2, quiz2Info, true, true, dates[0])
        runQuiz(users[4], quiz2, quiz2Info, true, true, dates[1])
        runQuiz(users[5], quiz2, quiz2Info, true, false, dates[1])
        runQuiz(users[6], quiz2, quiz2Info, true, false, dates[1])
        runQuiz(users[7], quiz2, quiz2Info, true, true, dates[2])
        runQuiz(users[8], quiz2, quiz2Info, true, false, dates[2])
        runQuiz(users[9], quiz2, quiz2Info, true, true, dates[2])

        def format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        when:
        def q1UserTagCounts = skillsService.getQuizUserTagCounts(quiz.quizId, usersTableAdditionalUserTagKey, format.format(dates[0]), format.format(dates[3]))
        def q1UserTagCountsFiltered = skillsService.getQuizUserTagCounts(quiz.quizId, usersTableAdditionalUserTagKey, format.format(dates[1]), format.format(dates[2]))
        def q2UserTagCounts = skillsService.getQuizUserTagCounts(quiz2.quizId, usersTableAdditionalUserTagKey, format.format(dates[0]), format.format(dates[3]))
        def q2UserTagCountsFiltered = skillsService.getQuizUserTagCounts(quiz2.quizId, usersTableAdditionalUserTagKey, format.format(dates[0]), format.format(dates[1]))
        def q2UserTagCountsFiltered2 = skillsService.getQuizUserTagCounts(quiz2.quizId, usersTableAdditionalUserTagKey, format.format(dates[2]), format.format(dates[3]))
        then:
        q1UserTagCounts.value == ["ABC2", "ABC3", "ABC1", "ABC"]
        q1UserTagCounts.count == [4, 3, 2, 1]
        q1UserTagCountsFiltered.value == []
        q1UserTagCountsFiltered.count == []

        q2UserTagCounts.value == ["ABC2", "ABC3", "ABC1", "ABC"]
        q2UserTagCounts.count == [4, 3, 2, 1]
        q2UserTagCountsFiltered.value == ["ABC2", "ABC1", "ABC"]
        q2UserTagCountsFiltered.count == [4, 2, 1]
        q2UserTagCountsFiltered2.value == ["ABC3"]
        q2UserTagCountsFiltered2.count == [3]
    }

    void runQuiz(String userId, def quiz, def quizInfo, boolean pass, boolean complete = true, Date startDate = null) {
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, userId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, userId)
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[pass ? 0 : 1].id, userId)
        if (complete) {
            skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, userId).body
        }

        if(startDate) {
            UserQuizAttempt userQuizAttempt = userQuizAttemptRepo.findById(quizAttempt.id).get()
            userQuizAttempt.started = startDate
            userQuizAttemptRepo.save(userQuizAttempt)
        }
    }

}
