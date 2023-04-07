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

import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.lang.Nullable
import skills.controller.result.model.QuizRun
import skills.controller.result.model.UserQuizAnswer
import skills.storage.model.UserQuizAnswerAttempt
import skills.storage.model.UserQuizAttempt

interface UserQuizAnswerAttemptRepo extends JpaRepository<UserQuizAnswerAttempt, Long> {

    static interface AnswerIdAndAnswerText {
        Integer getAnswerId()
        String getAnswerText()
    }

    @Query('''select answerAttempt.quizAnswerDefinitionRefId as answerId, answerAttempt.answer as answerText
        from UserQuizAnswerAttempt answerAttempt
        where answerAttempt.userQuizAttemptRefId = ?1
     ''')
    List<AnswerIdAndAnswerText> getSelectedAnswerIdsAndText(Integer attemptId)


    @Query('''select answerAttempt.quizAnswerDefinitionRefId
        from UserQuizAnswerAttempt answerAttempt
        where answerAttempt.userQuizAttemptRefId = ?1
     ''')
    List<Integer> getSelectedAnswerIds(Integer attemptId)

    @Nullable
    UserQuizAnswerAttempt findByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefId(Integer attemptId, Integer quizAnswerDefinitionRefId)

    boolean existsByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefId(Integer attemptId, Integer quizAnswerDefinitionRefId)

    void deleteByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefId(Integer attemptId, Integer quizAnswerDefinitionRefId)

    @Query('''select answerAttempt.updated as updated,
                    answerAttempt.answer as answerTxt,
                    answerAttempt.status as status,
                    userAttrs.userId as userId,
                    userAttrs.userIdForDisplay as userIdForDisplay
        from UserQuizAnswerAttempt answerAttempt, UserAttrs userAttrs, UserQuizAttempt quizAttempt
        where answerAttempt.userId = userAttrs.userId
            and answerAttempt.userQuizAttemptRefId = quizAttempt.id
            and quizAttempt.status <> 'INPROGRESS'
            and answerAttempt.quizAnswerDefinitionRefId = ?1
     ''')
    List<UserQuizAnswer> findUserAnswers(Integer quizAnswerDefinitionRefId, PageRequest pageRequest)

    @Query('''select count (answerAttempt)
        from UserQuizAnswerAttempt answerAttempt, UserQuizAttempt quizAttempt
        where answerAttempt.userQuizAttemptRefId = quizAttempt.id
            and quizAttempt.status <> 'INPROGRESS'
            and answerAttempt.quizAnswerDefinitionRefId = ?1
     ''')
    long countByQuizAnswerDefinitionRefId(Integer quizAnswerDefinitionRefId)
}
