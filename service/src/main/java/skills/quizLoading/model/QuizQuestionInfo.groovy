package skills.quizLoading.model

class QuizQuestionInfo {
    Integer id
    String question
    Boolean canSelectMoreThanOne
    List<QuizAnswerOptionsInfo> answerOptions
}
