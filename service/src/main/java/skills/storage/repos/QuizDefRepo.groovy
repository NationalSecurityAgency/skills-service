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

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.QuizDef

interface QuizDefRepo extends CrudRepository<QuizDef, Long> {

    @Nullable
    QuizDef findByQuizIdIgnoreCase(String projectId)



    static interface QuizDefSummaryResult {
        String getQuizId();

        String getName();

        int getNumQuestions();

        Date getCreated();
    }
    @Query(value="""
                SELECT 
                    qd.quiz_id AS quizId,
                    qd.name AS name,
                    qd.created
                FROM quiz_definition qd
                JOIN user_roles ur on (ur.quiz_id = qd.quiz_id AND ur.role_name in ('ROLE_QUIZ_ADMIN'))
                WHERE ur.user_id = ?1
            """, nativeQuery = true)
    @Nullable
    List<QuizDefSummaryResult> getQuizDefSummariesByUser(String userId)
}
