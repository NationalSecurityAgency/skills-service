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
import skills.services.quiz.QuizQuestionType
import skills.storage.model.QuizAnswerDef

interface QuizAnswerDefRepo extends JpaRepository<QuizAnswerDef, Long> {

    @Nullable
    List<QuizAnswerDef> findAllByQuizIdIgnoreCase(String quizId)

    @Nullable
    List<QuizAnswerDef> findAllByQuestionRefIdIn(List<Integer> questionRefIds)

    @Nullable
    List<QuizAnswerDef> findAllByQuestionRefId(Integer questionRefId)

    @Query('''select count(*) > 0 from QuizAnswerDef as aDef, QuizQuestionDef as qDef, UserQuizQuestionAttempt qAttempt 
            where aDef.questionRefId = qDef.id and qDef.id=qAttempt.quizQuestionDefinitionRefId and qAttempt.userQuizAttemptRefId = ?1 and aDef.id = ?2''')
    boolean doesAnswerBelongToQuizAttempt(Integer quizAttemptId, Integer answerDefId)

    static interface AnswerDefPartialInfo {
        String getIsCorrectAnswer()
        String getQuizId()
        Integer getQuestionRefId()

        QuizQuestionType getQuestionType()
    }
    @Nullable
    @Query(value = '''select answer.isCorrectAnswer as isCorrectAnswer, 
                answer.quizId as quizId, 
                question.type as questionType,
                question.id as questionRefId
            from QuizAnswerDef answer, QuizQuestionDef question 
            where answer.id = ?1
                and question.id=answer.questionRefId''')
    AnswerDefPartialInfo getPartialDefByAnswerDefId(Integer id)

    static interface AnswerIdAndCorrectness {
        Integer getAnswerRefId()
        String getIsCorrectAnswer()
    }

    @Query('''select a.id as answerRefId, a.isCorrectAnswer as isCorrectAnswer
            from QuizQuestionDef q, QuizAnswerDef a
            where q.id = a.questionRefId
                  and q.id = ?1
        ''')
    List<AnswerIdAndCorrectness> getAnswerIdsAndCorrectnessIndicator(Integer questionDefId)

}
