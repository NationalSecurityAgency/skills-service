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
package skills.storage.model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import jakarta.persistence.*

@Entity
@Table(name = 'user_quiz_answer_attempt')
@EntityListeners(AuditingEntityListener)
@CompileStatic
@ToString(includeNames = true)
class UserQuizAnswerAttempt {

    static enum QuizAnswerStatus {
        CORRECT, WRONG, NEEDS_GRADING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    // link it back to the quiz rather than question
    // to document user's current progress answers will exist before a question is completed
    Integer userQuizAttemptRefId
    Integer quizAnswerDefinitionRefId

    String userId
    @Enumerated(EnumType.STRING)
    QuizAnswerStatus status

    // optional - used for QuizQuestionType.TextInput
    String answer

    // optional - used by AI Grader Task (TextInputQuestionAiGradingExecutor)
    Integer aiGradingAttemptCount = 0

    @Column(name="created", updatable = false, insertable = false)
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated

}
