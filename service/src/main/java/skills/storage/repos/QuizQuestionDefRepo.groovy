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
import skills.services.attributes.SkillVideoAttrs
import skills.storage.model.QuizDefWithDescription
import skills.storage.model.QuizQuestionDef
import org.springframework.data.repository.query.Param

import java.util.stream.Stream

interface QuizQuestionDefRepo extends JpaRepository<QuizQuestionDef, Long> {

    @Nullable
    List<QuizQuestionDef> findAllByQuizIdIgnoreCase(String quizId)

    @Nullable
    @Query('''select qDef from QuizQuestionDef as qDef, UserQuizQuestionAttempt qAttempt where qAttempt.quizQuestionDefinitionRefId=qDef.id and qAttempt.userQuizAttemptRefId = ?1''')
    List<QuizQuestionDef> findQuestionDefsForSpecificQuizAttempt(Integer quizAttemptId)

    @Nullable
    @Query('''select qDef from QuizQuestionDef as qDef, UserQuizQuestionAttempt qAttempt where qAttempt.quizQuestionDefinitionRefId=qDef.id and qAttempt.userQuizAttemptRefId = ?1 and qAttempt.status="WRONG"''')
    List<QuizQuestionDef> findIncorrectQuestionDefsForSpecificQuizAttempt(Integer quizAttemptId)

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

    @Nullable
    @Query(value='''select attributes->>'videoConf' from quiz_question_definition where quiz_id = ?1 and id=?2''', nativeQuery = true)
    String getVideoAttributes(String quizId, Integer questionId)

    @Modifying
    @Query(value='''update quiz_question_definition 
                        set attributes = jsonb_set(COALESCE(attributes, '{}'), '{videoConf}', CAST(:attrs AS JSONB)) 
                        where quiz_id = :quizId and id = :questionId''', nativeQuery = true)
    void saveVideoAttributes(@Param("quizId") String quizId, @Param("questionId") Integer questionId, @Param("attrs") String attrs)

    @Modifying
    @Query('''update QuizQuestionDef set attributes = null where quizId = ?1 and id = ?2''')
    void deleteVideoAttrs(String quizId, Integer questionId)


    @Nullable
    @Query(value='''select attributes->>'textInputAiGradingConf' from quiz_question_definition where quiz_id = ?1 and id=?2''', nativeQuery = true)
    String getTextInputAiGradingAttrs(String quizId, Integer questionId)

    @Modifying
    @Query(value='''update quiz_question_definition 
                        set attributes = jsonb_set(COALESCE(attributes, '{}'), '{textInputAiGradingConf}', CAST(:attrs AS JSONB)) 
                        where quiz_id = :quizId and id = :questionId''', nativeQuery = true)
    void saveTextInputAiGradingAttrs(@Param("quizId") String quizId, @Param("questionId") Integer questionId, @Param("attrs") String attrs)

    Integer countByQuizId(String quizId)

    @Nullable
    @Query(value = '''select attributes -> 'videoConf' ->> 'captions'
        from quiz_question_definition
        where quiz_id = ?1
              and id = ?2
    ''', nativeQuery = true)
    String getVideoCaptions(String quizId, Integer questionId)

    @Nullable
    @Query(value = '''select attributes -> 'videoConf' ->> 'transcript'
        from quiz_question_definition
        where quiz_id = ?1
            and id = ?2
    ''', nativeQuery = true)
    String getVideoTranscripts(String quizId, Integer questionId)


    @Query('''SELECT s FROM QuizQuestionDef s''')
    Stream<QuizQuestionDef> streamAll()
}
