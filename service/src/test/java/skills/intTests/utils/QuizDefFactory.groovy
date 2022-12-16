package skills.intTests.utils

class QuizDefFactory {

    static String DEFAULT_QUIZ_NAME = "Test Quiz"
    static String DEFAULT_QUIZ_ID_PREPEND = DEFAULT_QUIZ_NAME.replaceAll(" ", "")

    static String getDefaultQuizId(int projNum = 1) {
        DEFAULT_QUIZ_ID_PREPEND + "${projNum}"
    }

    static String getDefaultQuizName(int projNum = 1) {
        DEFAULT_QUIZ_NAME + "#${projNum}"
    }

    static createQuiz(int projNumber = 1, String description = null) {
        return [quizId: getDefaultQuizId(projNumber), name: getDefaultQuizName(projNumber), description: description]
    }
}
