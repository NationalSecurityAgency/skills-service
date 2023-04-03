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
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.controller.result.model.QuizRun
import skills.storage.model.LabeledCount
import skills.storage.model.UserQuizAttempt
import skills.storage.model.UserQuizAttempt.QuizAttemptStatus

interface UserQuizAttemptRepo extends JpaRepository<UserQuizAttempt, Long> {

    @Nullable
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

    @Query('''select count(quizAttempt)
        from UserQuizAttempt quizAttempt, QuizDef quizDef
        where quizAttempt.quizDefinitionRefId = quizDef.id
            and quizDef.quizId = ?1
     ''')
    long countByQuizId(String quizId)

    @Query('''select count(quizAttempt)
        from UserQuizAttempt quizAttempt, QuizDef quizDef, UserAttrs userAttrs
        where quizAttempt.quizDefinitionRefId = quizDef.id
            and quizAttempt.userId = userAttrs.userId
            and lower(userAttrs.userIdForDisplay) like lower(CONCAT('%', ?2, '%'))
            and quizDef.quizId = ?1
     ''')
    Integer countQuizRuns(String quizId, String userQuery)

    @Query('''select quizAttempt.id as attemptId,
                    quizAttempt.started as started,
                    quizAttempt.completed as completed,
                    quizAttempt.status as status,
                    userAttrs.userId as userId,
                    userAttrs.userIdForDisplay as userIdForDisplay
        from UserQuizAttempt quizAttempt, QuizDef quizDef, UserAttrs userAttrs
        where quizAttempt.quizDefinitionRefId = quizDef.id
            and quizAttempt.userId = userAttrs.userId
            and lower(userAttrs.userIdForDisplay) like lower(CONCAT('%', ?2, '%'))
            and quizDef.quizId = ?1
     ''')
    List<QuizRun> findQuizRuns(String quizId, String userQuery, PageRequest pageRequest)

    @Query('''select quizAttempt from UserQuizAttempt quizAttempt where quizAttempt.quizDefinitionRefId = ?1 and quizAttempt.status = ?2''')
    List<UserQuizAttempt> findByQuizRefIdByStatus(Integer quizRefId, QuizAttemptStatus status, PageRequest pageRequest)

    List<UserQuizAttempt> findByUserIdAndQuizDefinitionRefIdAndStatus(String userId, Integer quizRefId, QuizAttemptStatus status)

    @Query('''select q
              from UserQuizAttempt q, QuizToSkillDef qtoS
              where q.quizDefinitionRefId = qtoS.quizRefId
                    and qtoS.skillRefId in ?1
                    and q.status = ?2''')
    List<UserQuizAttempt> findByInSkillRefIdAndByStatus(List<Integer> skillRefIds, QuizAttemptStatus status, PageRequest pageRequest)


    @Modifying
    @Query(value = '''delete from user_quiz_attempt
                      where id in (select attempt.id
                                 from skill_definition skill,
                                      quiz_to_skill_definition q_to_s,
                                      user_quiz_attempt attempt
                                 where skill.project_id = ?1
                                   and attempt.user_id = ?2
                                   and self_reporting_type = 'Quiz'
                                   and q_to_s.skill_ref_id = skill.id
                                   and q_to_s.quiz_ref_id = attempt.quiz_definition_ref_id
                                   and attempt.status = 'PASSED')''' , nativeQuery = true)
    int deleteAllAttemptsForQuizzesAssociatedToProjectAndByUserId(String projectId, String userId)
}
