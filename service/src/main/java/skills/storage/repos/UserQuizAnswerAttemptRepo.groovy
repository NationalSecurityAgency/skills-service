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

    @Query(value = '''select answerAttempt.updated         as updated,
                   answerAttempt.answer          as answerTxt,
                   answerAttempt.status          as status,
                   userAttrs.user_id             as userId,
                   userAttrs.user_id_for_display as userIdForDisplay,
                   quizAttempt.id                as userQuizAttemptId,
                   ut.value                      as userTag
            from user_quiz_answer_attempt answerAttempt,
                 user_quiz_attempt quizAttempt,
                 user_attrs userAttrs
                     left join (SELECT ut.user_id, max(ut.value) AS value
                                FROM user_tags ut
                                WHERE lower(ut.key) = lower(?2)
                                group by ut.user_id) ut ON ut.user_id = userAttrs.user_id
            where answerAttempt.user_id = userAttrs.user_id
              and answerAttempt.user_quiz_attempt_ref_id = quizAttempt.id
              and quizAttempt.status <> 'INPROGRESS'
              and answerAttempt.quiz_answer_definition_ref_id = ?1
     ''', nativeQuery = true)
    List<UserQuizAnswer> findUserAnswers(Integer quizAnswerDefinitionRefId, String usersTableAdditionalUserTagKey, PageRequest pageRequest)

    @Query('''select count (answerAttempt)
        from UserQuizAnswerAttempt answerAttempt, UserQuizAttempt quizAttempt
        where answerAttempt.userQuizAttemptRefId = quizAttempt.id
            and quizAttempt.status <> 'INPROGRESS'
            and answerAttempt.quizAnswerDefinitionRefId = ?1
     ''')
    long countByQuizAnswerDefinitionRefId(Integer quizAnswerDefinitionRefId)
}
