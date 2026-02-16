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
package skills.controller.result.model


import skills.storage.model.QuizDefParent
import skills.storage.model.UserQuizAttempt

class UserGradedQuizQuestionsResult {
    String quizName
    String userId
    String userIdForDisplay
    QuizDefParent.QuizType quizType
    List<UserGradedQuizQuestionResult> questions
    UserQuizAttempt.QuizAttemptStatus status
    Boolean allQuestionsReturned
    Integer numQuestions
    Integer numQuestionsToPass
    Integer numQuestionsPassed
    Date started
    Date completed
    String userTag
    Boolean questionsHidden
}
