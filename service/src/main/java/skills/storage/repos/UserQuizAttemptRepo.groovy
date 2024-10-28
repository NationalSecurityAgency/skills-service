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

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.controller.result.model.MyQuizAttempt
import skills.controller.result.model.QuizRun
import skills.storage.model.UserQuizAttempt
import skills.storage.model.UserQuizAttempt.QuizAttemptStatus

interface UserQuizAttemptRepo extends JpaRepository<UserQuizAttempt, Long> {

    static interface QuizCounts {
        QuizAttemptStatus getStatus()
        Integer getNumAttempts()
        Integer getNumDistinctUsers()
    }
    @Nullable
    @Query('''select
        quizAttempt.status as status, count(quizAttempt.id) as numAttempts, count(distinct quizAttempt.userId) as numDistinctUsers
        from UserQuizAttempt quizAttempt, QuizDef quizDef
        where quizAttempt.quizDefinitionRefId = quizDef.id
            and quizDef.quizId = ?1
            and quizAttempt.status in ('PASSED', 'FAILED')
        group by quizAttempt.status
     ''')
    List<QuizCounts> getUserQuizAttemptCounts(String quizId)

    @Nullable
    @Query(value = '''select AVG((extract('epoch' from quizAttempt.completed) * 1000)- (extract('epoch' from quizAttempt.started) * 1000))
            from user_quiz_attempt quizAttempt,
            quiz_definition quizDef
            where quizAttempt.quiz_definition_ref_id = quizDef.id
                    and quizDef.quiz_id = ?1
                    and quizAttempt.status  in ('PASSED', 'FAILED')
     ''', nativeQuery = true)
    Double getAverageMsRuntimeForQuiz(String quizId)


    @Query('''select count(distinct quizAttempt.userId)
        from UserQuizAttempt quizAttempt, QuizDef quizDef
        where quizAttempt.quizDefinitionRefId = quizDef.id
            and quizDef.quizId = ?1
            and quizAttempt.status in ('PASSED', 'FAILED')
     ''')
    Integer getDistinctNumUsersByQuizId(String quizId)

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

    @Query('''select count(quizAttempt.id) > 0      
        from UserQuizAttempt quizAttempt, QuizDef quizDef
        where quizAttempt.quizDefinitionRefId = quizDef.id
            and quizAttempt.userId = ?1
            and quizAttempt.id = ?2
            and quizDef.quizId = ?3
            and quizAttempt.status = ?4
     ''')
    boolean checkQuizStatus(String userId, Integer id, String quizId, QuizAttemptStatus quizStatus)

    @Query('''select count(quizAttempt)
        from UserQuizAttempt quizAttempt, QuizDef quizDef
        where quizAttempt.quizDefinitionRefId = quizDef.id
            and quizDef.quizId = ?1
     ''')
    long countByQuizId(String quizId)

    @Query(value = '''select quizAttempt.id                as attemptId,
                           quizAttempt.started           as started,
                           quizAttempt.completed         as completed,
                           quizAttempt.status            as status,
                           userAttrs.user_id             as userId,
                           userAttrs.user_id_for_display as userIdForDisplay,
                           ut.value                      as userTag, 
                           userAttrs.first_name          as firstName,
                           userAttrs.last_name           as lastName
                    from user_quiz_attempt quizAttempt,
                         quiz_definition quizDef,
                         user_attrs userAttrs
                             left join (SELECT ut.user_id, max(ut.value) AS value
                                        FROM user_tags ut
                                        WHERE lower(ut.key) = lower(:usersTableAdditionalUserTagKey)
                                        group by ut.user_id) ut ON ut.user_id = userAttrs.user_id
                    where quizAttempt.quiz_definition_ref_id = quizDef.id
                      and quizAttempt.user_id = userAttrs.user_id
                      and (quizAttempt.status = :quizAttemptStatus OR :quizAttemptStatus IS NULL)
                      and (lower(userAttrs.user_id_for_display) like lower(CONCAT('%', :userQuery, '%')) or
                      (lower(CONCAT(userAttrs.first_name, ' ', userAttrs.last_name, ' (',  userAttrs.user_id_for_display, ')')) like lower(CONCAT(\'%\', :userQuery, \'%\'))) OR
                      (lower(CONCAT(userAttrs.user_id_for_display, ' (', userAttrs.last_name, ', ', userAttrs.first_name,  ')')) like lower(CONCAT(\'%\', :userQuery, \'%\'))))
                      and quizDef.quiz_id = :quizId
     ''', nativeQuery = true)
    Page<QuizRun> findQuizRuns(@Param('quizId')  String quizId,
                               @Param('userQuery') String userQuery,
                               @Param('usersTableAdditionalUserTagKey') String usersTableAdditionalUserTagKey,
                               @Nullable@Param('quizAttemptStatus') String quizAttemptStatus,
                               PageRequest pageRequest)

    @Query(value = '''
        select attempts.id as attemptId,
               attempts.status as status,
               attempts.started as started,
               attempts.completed as completed,
               quizDef.name as quizName,
               quizDef.quizId as quizId,
               quizDef.type as quizType
        from UserQuizAttempt attempts, QuizDef quizDef
        where attempts.userId=:userId and 
            attempts.quizDefinitionRefId = quizDef.id and 
            lower(quizDef.name) LIKE lower(CONCAT('%', :quizNameQuery, '%'))
     ''')
    @Nullable
    Page<MyQuizAttempt> findUserQuizAttempts(
            @Param('userId') String userId,
            @Param('quizNameQuery') String quizNameQuery,
            PageRequest pageRequest)

    @Nullable
    @Query('''select quizAttempt from UserQuizAttempt quizAttempt where quizAttempt.quizDefinitionRefId = ?1 and quizAttempt.status = ?2''')
    List<UserQuizAttempt> findByQuizRefIdByStatus(Integer quizRefId, QuizAttemptStatus status, PageRequest pageRequest)

    List<UserQuizAttempt> findByUserIdAndQuizDefinitionRefIdAndStatus(String userId, Integer quizRefId, QuizAttemptStatus status)

    List<UserQuizAttempt> findByUserIdAndQuizDefinitionRefId(String userId, Integer quizRefId)

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

    static interface TagValueCount {
        String getTagValue()
        Integer getTagCount()
    }
    @Nullable
    @Query(value = '''select tags.value as tagValue, count(attempt.id) as tagCount
                    from user_quiz_attempt attempt,
                         quiz_definition quizDef,
                         user_tags tags
                    where attempt.user_id = tags.user_id
                        and attempt.quiz_definition_ref_id = quizDef.id
                      and quizDef.quiz_id = ?1
                      and tags.key = ?2
                    group by tags.value''', nativeQuery = true)
    List<TagValueCount> getUserTagCounts(String quizId, String userTag, PageRequest pageRequest)


    static interface DateCount {
        Date getDateVal()
        Integer getCount()
    }

    @Nullable
    @Query(value = '''select DATE_TRUNC ('day', attempt.started) as dateVal, count(attempt.id) as count
                from user_quiz_attempt attempt,
                quiz_definition quizDef
                where attempt.quiz_definition_ref_id = quizDef.id
                        and quizDef.quiz_id = ?
                group by DATE_TRUNC ('day', attempt.started)
                order by dateVal''', nativeQuery = true)
    List<DateCount> getUsageOverTime(String quizId)

    static interface AttemptCounts {
        Integer getNumAttempts()
        Integer getNumQuizAttempts()
    }
    @Query('''select 
            count(*) as numAttempts,
            sum(case when quizDef.type = 'Quiz' then 1 else 0 end) as numQuizAttempts 
        from UserQuizAttempt attempt, QuizDef quizDef 
        where attempt.userId = ?1
            and attempt.quizDefinitionRefId = quizDef.id''')
    AttemptCounts getAttemptCountsForUser(String userId)

}
