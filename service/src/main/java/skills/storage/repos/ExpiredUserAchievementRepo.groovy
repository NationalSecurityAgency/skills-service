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

import groovy.transform.CompileStatic
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import org.springframework.data.repository.query.Param
import skills.controller.result.model.ExpiredSkillRes
import skills.storage.model.ExpiredUserAchievement
import skills.storage.model.UserAchievement

@CompileStatic
interface ExpiredUserAchievementRepo extends CrudRepository<ExpiredUserAchievement, Integer> {

    @Modifying
    @Query(value = '''
        WITH expired_rows AS (
        DELETE FROM user_achievement
        WHERE
            skill_ref_id = :skillRefId
        RETURNING *)
        INSERT INTO expired_user_achievement SELECT * FROM expired_rows;
    ''', nativeQuery = true)
    void expireAchievementsForSkill(@Param("skillRefId") Integer skillRefId)

    @Modifying
    @Query(value = '''
        WITH expired_row AS (
        DELETE FROM user_achievement ua
        WHERE ua.id = ?1
        RETURNING *)
        INSERT INTO expired_user_achievement SELECT * FROM expired_row;
    ''', nativeQuery = true)
    void expireAchievementById(Integer id)

    @Query(value = '''
        SELECT ua
        FROM UserAchievement ua
             INNER JOIN UserPerformedSkill ups ON
                ua.skillRefId = ups.skillRefId AND
                ua.userId = ups.userId AND
                ups.performedOn = (
                    SELECT MAX(ups2.performedOn) 
                    FROM UserPerformedSkill ups2
                    WHERE ups2.skillRefId = ups.skillRefId AND ups2.userId = ups.userId
                )
        WHERE ups.performedOn < :olderThanDate
          AND ua.skillRefId = :skillRefId
    ''')
    List<UserAchievement> findUserAchievementsBySkillRefIdWithMostRecentUserPerformedSkillBefore(@Param("skillRefId") Integer skillRefId,
                                                                                                 @Param("olderThanDate") Date olderThanDate)

    @Query(value = '''
       SELECT eua.userId as userId, eua.skillId as skillId, eua.expiredOn as expiredOn, skill.name as skillName
       FROM ExpiredUserAchievement eua, SkillDef skill
       WHERE eua.projectId = :projectId AND eua.skillId = skill.skillId AND eua.projectId = skill.projectId
       AND(:userId is null OR lower(eua.userId) like lower(concat('%', :userId, '%')))
       AND(:skillNameFilter is null OR lower(skill.name) like lower(concat('%', :skillNameFilter, '%')))
    ''')
    Page<ExpiredSkillRes> findAllExpiredAchievements(@Param("projectId") String projectId,
                                                     @Param("userId") String userId,
                                                     @Param("skillNameFilter") String skillNameFilter, PageRequest pageRequest)

    @Query(value = '''
        SELECT eua
        FROM ExpiredUserAchievement eua
        WHERE eua.projectId = :projectId AND eua.skillId = :skillId AND eua.userId = :userId
        ORDER BY eua.expiredOn DESC LIMIT 1
    ''')
    @Nullable
    ExpiredUserAchievement findMostRecentExpirationForSkill(@Param("projectId") String projectId, @Param("userId") String userId, @Param("skillId") String skillId)
}
