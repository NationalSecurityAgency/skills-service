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
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.LabeledCount
import skills.storage.model.UserQuizAttempt
import skills.storage.model.UserQuizAttempt.QuizAttemptStatus

interface UserQuizAttemptRepo extends JpaRepository<UserQuizAttempt, Long> {

    @Query('''select
        quizAttempt.status as label, count(quizAttempt.id) as count
        from UserQuizAttempt quizAttempt, QuizDef quizDef
        where quizAttempt.quizDefinitionRefId = quizDef.id
            and quizDef.quizId = ?1
        group by quizAttempt.status
     ''')
    List<LabeledCount> getUserQuizAttemptCounts(String quizId)


    @Nullable
    @Query('''select quizAttempt      
        from UserQuizAttempt quizAttempt, QuizDef quizDef
        where quizAttempt.quizDefinitionRefId = quizDef.id
            and quizAttempt.userId = ?1
            and quizDef.quizId = ?2
            and quizAttempt.status = ?3
     ''')
    UserQuizAttempt getByUserIdAndQuizIdAndState(String userId, String quizId, QuizAttemptStatus quizAttemptStatus)

    static interface UserQuizAttemptStats {
        Boolean getIsAttemptAlreadyInProgress()
        Integer getUserNumPreviousQuizAttempts()
        Boolean getUserQuizPassed()
        Date getUserLastQuizAttemptCompleted()
    }

    @Nullable
    @Query('''select
            (sum(case when quizAttempt.status = :inProgressStatus then 1 else 0 end) > 0) as isAttemptAlreadyInProgress, 
            (sum(case when quizAttempt.status = :passedStatus then 1 else 0 end) > 0) as userQuizPassed,
            sum(case when quizAttempt.completed is not null then 1 else 0 end) as userNumPreviousQuizAttempts,
            max(quizAttempt.completed) as userLastQuizAttemptCompleted   
        from UserQuizAttempt quizAttempt
        where quizAttempt.userId = :userId
            and quizAttempt.quizDefinitionRefId = :quizDefId
     ''')
    UserQuizAttemptStats getUserAttemptsStats(@Param('userId') String userId,
                                              @Param('quizDefId') Integer quizDefId,
                                              @Param('inProgressStatus') QuizAttemptStatus inProgressStatus,
                                              @Param('passedStatus') QuizAttemptStatus passedStatus)


    @Query('''select count(quizAttempt.id) > 0      
        from UserQuizAttempt quizAttempt, QuizDef quizDef
        where quizAttempt.quizDefinitionRefId = quizDef.id
            and quizAttempt.userId = ?1
            and quizAttempt.id = ?2
            and quizDef.quizId = ?3
     ''')
    boolean existsByUserIdAndIdAndQuizId(String userId, Integer id, String quizId)

}
