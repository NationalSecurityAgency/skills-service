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
package skills.quizLoading.model


class QuizInfo {
    String name
    String description
    String quizType

    List<QuizQuestionInfo> questions

    Integer minNumQuestionsToPass

    Boolean isAttemptAlreadyInProgress
    Integer userNumPreviousQuizAttempts
    Integer maxAttemptsAllowed
    Boolean userQuizPassed
    Date userLastQuizAttemptDate
    Integer quizLength
    Integer quizTimeLimit
    Boolean multipleTakes
    Boolean canStartQuiz
    String errorMessage
}
