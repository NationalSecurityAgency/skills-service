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
import skills.storage.model.QuizDefParent
import skills.storage.model.QuizToSkillDef

interface QuizToSkillDefRepo extends JpaRepository<QuizToSkillDef, Long> {

    static interface QuizNameAndId {
        Integer getSkillRefId()
        String getQuizName()
        String getQuizId()
        QuizDefParent.QuizType getQuizType()
        Integer getNumQuestions()
    }

    static interface ProjectIdAndSkillId {
        Integer getSkillRefId()
        String getProjectId()
        String getSkillId()
    }

    @Nullable
    @Query('''select q.quizId as quizId,
                     max(q.name) as quizName,
                     max(q.type) as quizType,
                     max(qToS.skillRefId) as skillRefId, 
                     count(question.id) as numQuestions
            from QuizToSkillDef qToS, QuizDef q
                left join QuizQuestionDef question on (q.quizId = question.quizId)
            where qToS.skillRefId = ?1
                and q.id = qToS.quizRefId
            group by q.quizId''')
    QuizNameAndId getQuizIdBySkillIdRef(Integer skillIdRef)

    @Nullable
    @Query('''select skill.id as skillRefId, skill.skillId as skillId, skill.projectId as projectId
            from QuizToSkillDef qToS, SkillDef skill 
            where qToS.quizRefId = ?1
                and skill.id = qToS.skillRefId''')
    List<ProjectIdAndSkillId> getSkillsForQuiz(Integer quizRefId)

    @Nullable
    @Query('''select q.quizId as quizId, 
                    max(q.name) as quizName,
                    max(q.type) as quizType,
                    qToS.skillRefId as skillRefId,
                    count(question.id) as numQuestions
            from QuizToSkillDef qToS, QuizDef q
             left join QuizQuestionDef question on (q.quizId = question.quizId) 
            where qToS.skillRefId in ?1
                and q.id = qToS.quizRefId
            group by q.quizId, qToS.skillRefId''')
    List<QuizNameAndId> getQuizInfoSkillIdRef(List<Integer> skillIdRef)

    void deleteBySkillRefId(Integer skillRefId)

    Integer countByQuizRefId(Integer quizRefId)

    @Query('''select count(quiz) > 0
            from QuizToSkillDef qToS, QuizDef  quiz, SkillDef  skill
            where quiz.id = qToS.quizRefId
                and skill.id = qToS.skillRefId
                and quiz.quizId = ?1
                and skill.projectId in ?2 ''')
    boolean existQuizIdToOneOfTheProjectIdsAssociation(String quizId, List<String> projectIds)

    @Nullable
    @Query('''select qToS.skillRefId
            from QuizToSkillDef qToS, SkillDef skill 
            where skill.projectId = ?1
                and skill.id = qToS.skillRefId''')
    List<Integer> getSkillRefIdsWithQuizByProjectId(String projectId)

}

