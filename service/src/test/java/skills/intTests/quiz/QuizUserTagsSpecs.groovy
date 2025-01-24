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


import org.springframework.beans.factory.annotation.Value
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService

class QuizUserTagsSpecs extends DefaultIntSpec {

    @Value('${skills.config.ui.usersTableAdditionalUserTagKey}')
    String usersTableAdditionalUserTagKey

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


    void runQuiz(String userId, def quiz, def quizInfo, boolean pass, boolean complete = true) {
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, userId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, userId)
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[pass ? 0 : 1].id, userId)
        if (complete) {
            skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, userId).body
        }
    }

}
