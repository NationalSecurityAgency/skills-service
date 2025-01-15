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

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.QuizDef

interface QuizDefRepo extends CrudRepository<QuizDef, Long> {

    @Nullable
    QuizDef findByQuizIdIgnoreCase(String quizId)

    @Nullable
    @Query(value = "select q.id from QuizDef q where lower(q.quizId) = lower(?1)")
    Integer getQuizRefIdByQuizIdIgnoreCase(String quizId)

    @Modifying
    int deleteByQuizIdIgnoreCase(String quizId)

    @Nullable
    QuizDef findByNameIgnoreCase(String quizName)

    @Query(value = "select count(q.id) from QuizDef q, UserRole u where q.quizId = u.quizId and u.userId=?1")
    Integer getQuizCountByUserId(String userId)

    Boolean existsByQuizIdIgnoreCase(String quizId)
    Boolean existsByNameIgnoreCase(String quizName)

    static interface QuizDefBasicResult {
        String getQuizId();

        String getName();

        String getQuizType()

        Date getCreated();

        String getUserCommunityEnabled()
    }
    @Query(value="""
                SELECT DISTINCT
                    qd.quiz_id AS quizId,
                    qd.name AS name,
                    qd.type as quizType,
                    qd.created
                FROM quiz_definition qd
                JOIN user_roles ur on (ur.quiz_id = qd.quiz_id AND ur.role_name in ('ROLE_QUIZ_ADMIN'))
                WHERE ur.user_id = ?1
            """, nativeQuery = true)
    @Nullable
    List<QuizDefBasicResult> getQuizDefSummariesByUser(String userId)

    static interface QuizDefSummaryRes {
        String getName();
        String getQuizType()
        Date getCreated();
        Integer getNumQuestions()
    }
    @Query(value="""
                select 
                    qd.name AS name,
                    qd.type as quizType,
                    qd.created as created,
                    COALESCE(questions.questionCount, 0) as numQuestions
                FROM quiz_definition qd
                LEFT JOIN (SELECT max(quiz_id) as quiz_id, COUNT(id) AS questionCount FROM quiz_question_definition WHERE LOWER(quiz_id) = LOWER(?1)) questions ON questions.quiz_id = qd.quiz_id
                WHERE qd.quiz_id = ?1
            """, nativeQuery = true)
    QuizDefSummaryRes getQuizDefSummary(String quizId)

    @Query(value = '''select count(id) > 0
            from quiz_question_definition
            where question like CONCAT('%(/api/download/', ?3, ')%')
              and id <> ?2
              and LOWER(quiz_id) = LOWER(?1) ''', nativeQuery = true)
    Boolean otherQuestionsExistInQuizWithAttachmentUUID(String quizId, Integer notQuestionRefId, String attachmentUUID)
}
