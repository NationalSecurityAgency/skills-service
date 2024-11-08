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
import skills.intTests.utils.SkillsService
import skills.storage.model.UserAchievement
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizToSkillDefRepo
import skills.storage.repos.UserQuizAttemptRepo

class QuizSkillAchievementsBaseIntSpec extends DefaultIntSpec{

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    protected def createQuiz(Integer num) {
        def quiz = QuizDefFactory.createQuiz(num)
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(num, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        quizInfo.quizId = quiz.quizId
        return quizInfo
    }

    protected Integer passQuiz(SkillsService userService, def quizInfo) {
        def quizAttempt =  userService.startQuizAttempt(quizInfo.quizId).body
        userService.reportQuizAnswer(quizInfo.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        def gradedQuizAttempt = userService.completeQuizAttempt(quizInfo.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.passed == true
        return quizAttempt.id
    }
    protected Integer failQuiz(SkillsService userService, def quizInfo) {
        def quizAttempt =  userService.startQuizAttempt(quizInfo.quizId).body
        userService.reportQuizAnswer(quizInfo.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        def gradedQuizAttempt = userService.completeQuizAttempt(quizInfo.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.passed == false
        return quizAttempt.id
    }

    protected static class UserAchievementsInfo {
        List<UserAchievement> user1
        List<UserAchievement> user2
        List<UserAchievement> user3
    }

    protected UserAchievementsInfo loadAchievements(List<SkillsService> userServices, String projectId = null) {
        List<UserAchievement> achievements = userAchievedRepo.findAll()
        return new UserAchievementsInfo(
                user1: achievements.findAll({ it.userId == userServices[0].userName && (projectId == null || projectId == it.projectId)}),
                user2: achievements.findAll({ it.userId == userServices[1].userName && (projectId == null || projectId == it.projectId)}),
                user3: achievements.findAll({ it.userId == userServices[2].userName && (projectId == null || projectId == it.projectId)}),
        )
    }
}
