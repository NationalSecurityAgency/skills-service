package skills.intTests.quiz

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.UserAchievement
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizToSkillDefRepo

class QuizSkillAchievementsBaseIntSpec extends DefaultIntSpec{

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    QuizDefRepo quizDefRepo

    protected def createQuiz(Integer num) {
        def quiz = QuizDefFactory.createQuiz(num)
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(num, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        quizInfo.quizId = quiz.quizId
        return quizInfo
    }

    protected void passQuiz(SkillsService userService, def quizInfo) {
        def quizAttempt =  userService.startQuizAttempt(quizInfo.quizId).body
        userService.reportQuizAnswer(quizInfo.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        def gradedQuizAttempt = userService.completeQuizAttempt(quizInfo.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.passed == true
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
