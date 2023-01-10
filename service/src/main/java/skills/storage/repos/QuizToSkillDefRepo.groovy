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
import skills.storage.model.QuizToSkillDef

interface QuizToSkillDefRepo extends CrudRepository<QuizToSkillDef, Long> {

    @Nullable
    QuizToSkillDef findByQuizRefIdAndSkillRefId(Integer quizIdRef, Integer skillIdRef)

    static interface QuizNameAndId {
        String getQuizName()
        String getQuizId()
    }

    @Query('''select q.quizId as quizId, q.name as quizName
            from QuizToSkillDef qToS, QuizDef q 
            where qToS.skillRefId = ?1
                and q.id = qToS.quizRefId''')
    QuizNameAndId getQuizIdBySkillIdRef(Integer skillIdRef)

    static interface SkillToQuiz {
        Integer getSkillRefId()
        String getQuizId()
    }

    @Query('''select q.quizId as quizId, qToS.skillRefId as skillRefId
            from QuizToSkillDef qToS, QuizDef q 
            where qToS.skillRefId in ?1
                and q.id = qToS.quizRefId''')
    List<SkillToQuiz> getSkillToQuizAssociations(List<Integer> skillRefIds)

}

