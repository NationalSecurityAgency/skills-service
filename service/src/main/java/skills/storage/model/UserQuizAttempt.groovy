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
@Table(name = 'user_quiz_attempt')
@EntityListeners(AuditingEntityListener)
@CompileStatic
@ToString(includeNames = true)
class UserQuizAttempt {

    static enum QuizAttemptStatus {
        INPROGRESS, PASSED, FAILED, NEEDS_GRADING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    Integer quizDefinitionRefId
    @Enumerated(EnumType.STRING)
    QuizAttemptStatus status

    String userId

    Date started
    Date completed

    // store a copy of this configured param at the time of the quiz run
    // update at the start and the end of the run
    Integer numQuestionsToPass

    @Column(name="created", updatable = false, insertable = false)
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated

}
