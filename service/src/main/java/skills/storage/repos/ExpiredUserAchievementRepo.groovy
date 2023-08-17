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
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import skills.storage.model.ExpiredUserAchievement

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
        WITH expired_rows AS (
        DELETE FROM user_achievement
        WHERE
            achieved_on <= :expirationDate AND
            skill_ref_id = :skillRefId
        RETURNING *)
        INSERT INTO expired_user_achievement SELECT * FROM expired_rows;
    ''', nativeQuery = true)
    void expireAchievementsForSkillAchievedBefore(@Param("skillRefId") Integer skillRefId,
                                                  @Param("expirationDate") Date expirationDate)
}
