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
package skills.storage.repos

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.lang.Nullable
import skills.storage.model.UserQuizAnswerAttempt
import skills.storage.model.UserQuizAnswerGraded

interface UserQuizAnswerGradedRepo extends JpaRepository<UserQuizAnswerGraded, Long> {


    static interface GradedInfo {
        Integer getAnswerAttemptId()
        Date getGradedOn()
        String getFeedback()
        Integer getAiConfidenceLevel()
        String getGraderUserId()
        String getGraderUserIdForDisplay()
        String getGraderFirstname()
        String getGraderLastname()

    }

    @Query('''select 
            answerAttempt.id as answerAttemptId,    
            graded.created as gradedOn, 
            graded.feedback as feedback,
            graded.aiConfidenceLevel as aiConfidenceLevel,
            userAttrs.userId as graderUserId, 
            userAttrs.userIdForDisplay as graderUserIdForDisplay, 
            userAttrs.firstName as graderFirstname, 
            userAttrs.lastName as graderLastname
        from UserQuizAnswerGraded graded, UserQuizAnswerAttempt answerAttempt, UserAttrs userAttrs
        where graded.userQuizAnswerAttemptRefId = answerAttempt.id
          and graded.graderUserAttrsRefId = userAttrs.id
          and answerAttempt.userQuizAttemptRefId = ?1
     ''')
    List<GradedInfo> getGradedAnswersForQuizAttemptId(Integer attemptId)


    @Nullable
    UserQuizAnswerGraded findByUserQuizAnswerAttemptRefId(Integer userQuizAnswerAttemptRefId)
}
