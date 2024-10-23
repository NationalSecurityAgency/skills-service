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
import skills.storage.model.UserQuizQuestionAttempt

import javax.annotation.Nullable

interface UserQuizQuestionAttemptRepo extends JpaRepository<UserQuizQuestionAttempt, Long> {

    interface IdAndStatusCount {
        Integer getId()
        String getStatus()
        Long getCount()
    }

    @Query('''select
        questionAttempt.quizQuestionDefinitionRefId as id, questionAttempt.status as status, count(questionAttempt.userId) as count
        from QuizQuestionDef questionDef, UserQuizQuestionAttempt questionAttempt, UserQuizAttempt quizAttempt
        where questionAttempt.quizQuestionDefinitionRefId = questionDef.id
            and questionDef.quizId = ?1
            and quizAttempt.id = questionAttempt.userQuizAttemptRefId
            and quizAttempt.status in ('PASSED', 'FAILED')
        group by questionAttempt.quizQuestionDefinitionRefId, questionAttempt.status
     ''')
    List<IdAndStatusCount> getUserQuizQuestionAttemptCounts(String quizId)

    @Query('''select
        answerAttempt.quizAnswerDefinitionRefId as id, answerAttempt.status as status, count(answerAttempt.userId) as count
        from QuizAnswerDef answerDef, UserQuizAnswerAttempt answerAttempt, UserQuizAttempt quizAttempt
        where answerAttempt.quizAnswerDefinitionRefId = answerDef.id
            and answerDef.quizId = ?1
            and quizAttempt.id = answerAttempt.userQuizAttemptRefId
            and quizAttempt.status in ('PASSED', 'FAILED')
        group by answerAttempt.quizAnswerDefinitionRefId, answerAttempt.status
     ''')
    List<IdAndStatusCount> getUserQuizAnswerAttemptCounts(String quizId)

    @Nullable
    List<UserQuizQuestionAttempt> findAllByUserQuizAttemptRefId(Integer userQuizAttemptRefId)

}
