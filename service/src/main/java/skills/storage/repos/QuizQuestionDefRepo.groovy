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
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.lang.Nullable
import skills.storage.model.QuizQuestionDef

interface QuizQuestionDefRepo extends JpaRepository<QuizQuestionDef, Long> {

    @Nullable
    List<QuizQuestionDef> findAllByQuizIdIgnoreCase(String quizId)

    @Nullable
    @Query('''select qDef from QuizQuestionDef as qDef, UserQuizQuestionAttempt qAttempt where qAttempt.quizQuestionDefinitionRefId=qDef.id and qAttempt.userQuizAttemptRefId = ?1''')
    List<QuizQuestionDef> findQuestionDefsForSpecificQuizAttempt(Integer quizAttemptId)

    @Nullable
    @Query('''select max(displayOrder) from QuizQuestionDef where quizId = ?1''')
    Integer getMaxQuestionDisplayOrderByQuizId(String quizId)


    static interface DisplayOrder {
        Integer getId()
        Integer getDisplayOrder()
    }

    @Nullable
    @Query('''select q.id as id, q.displayOrder as displayOrder from QuizQuestionDef q where q.quizId = ?1''')
    List<DisplayOrder> getQuestionsDisplayOrder(String quizId)

    @Modifying
    @Query('''update QuizQuestionDef set displayOrder = ?2 where id = ?1''')
    void updateDisplayOrder(Integer id, Integer newDisplayOrder)


    Integer countByQuizId(String quizId)
}
