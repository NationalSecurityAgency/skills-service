package skills.controller.result.model

import groovy.transform.Canonical
import groovy.transform.ToString
import skills.storage.model.UserQuizAttempt

@Canonical
@ToString(includeNames = true)
class QuizRunResult {
    Integer attemptId
    String userId
    String userIdForDisplay
    Date started
    Date completed

    UserQuizAttempt.QuizAttemptStatus status
    String userTag
    String firstName
    String lastName
    String quizType

    @Canonical
    @ToString(includeNames = true)
    static class QuestionAiGradingStatus extends AiGradingStatusResult {
        Integer questionId
    }

    List<QuestionAiGradingStatus> aiGradingStatus
}
