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

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.UserTag

interface UserTagRepo extends CrudRepository<UserTag, Integer> {

    @Nullable
    List<UserTag> findAllByUserId(String userId)

    @Nullable
    List<UserTag> findAllByUserIdAndKey(String userId, String key)

    @Nullable
    @Query("select ut from UserTag ut where ut.userId = ?1 and lower(ut.key) in ?2")
    List<UserTag> findAllByUserIdAndKeyIn(String userId, Set<String> keys)

    @Modifying
    @Query("delete from UserTag ut where lower(ut.userId) = lower(?1)")
    void deleteByUserId(String userId)

    static interface UserTagCount {
        Integer getNumUsers()
        String getTag()
    }

    final static String SELECT_DISTINCT_USER_BY_TAG_SQL = '''WITH
        valid_skills AS (
            SELECT CASE WHEN sd.copied_from_skill_ref IS NOT NULL THEN sd.copied_from_skill_ref ELSE sd.id END as skill_ref_id
            FROM skill_definition sd
            WHERE sd.type = 'Skill'
              AND sd.project_id = :projectId
              AND sd.enabled = 'true'
        ),
        users_tmp AS (
             SELECT DISTINCT up.user_id
             FROM user_performed_skill up
                      JOIN valid_skills vs ON up.skill_ref_id = vs.skill_ref_id
                      LEFT JOIN archived_users au ON (au.user_id = up.user_id AND au.project_id = :projectId)
             WHERE up.performed_on >= :startDate
               AND up.performed_on <= :endDate
               AND au.user_id IS NULL
        ),
        tag_counts AS (
            SELECT COUNT(DISTINCT users_tmp.user_id) as numUsers, ut.value as tag
            FROM users_tmp
                     JOIN user_tags ut ON ut.user_id = users_tmp.user_id
            WHERE ut.key = :tagKey
              AND ut.value IS NOT NULL
              AND (LOWER(ut.value) LIKE LOWER(CONCAT('%', :tagFilter, '%')) OR :tagFilter is null)
            GROUP BY ut.value
        )
        SELECT numUsers, tag from tag_counts'''

    final static String COUNT_DISTINCT_USER_BY_TAG_SQL = '''WITH
        valid_skills AS (
            SELECT CASE WHEN sd.copied_from_skill_ref IS NOT NULL THEN sd.copied_from_skill_ref ELSE sd.id END as skill_ref_id
            FROM skill_definition sd
            WHERE sd.type = 'Skill'
              AND sd.project_id = :projectId
              AND sd.enabled = 'true'
        ),
        users_tmp AS (
             SELECT DISTINCT up.user_id
             FROM user_performed_skill up
                      JOIN valid_skills vs ON up.skill_ref_id = vs.skill_ref_id
                      LEFT JOIN archived_users au ON (au.user_id = up.user_id AND au.project_id = :projectId)
             WHERE up.performed_on >= :startDate
               AND up.performed_on <= :endDate
               AND au.user_id IS NULL
        ),
        tag_counts AS (
            SELECT COUNT(DISTINCT users_tmp.user_id) as numUsers, ut.value as tag
            FROM users_tmp
                     JOIN user_tags ut ON ut.user_id = users_tmp.user_id
            WHERE ut.key = :tagKey
              AND ut.value IS NOT NULL
              AND (LOWER(ut.value) LIKE LOWER(CONCAT('%', :tagFilter, '%')) OR :tagFilter is null)
            GROUP BY ut.value
        )
        SELECT COUNT(*) from tag_counts'''

    @Nullable
    @Query(value = SELECT_DISTINCT_USER_BY_TAG_SQL, nativeQuery = true)
    List<UserTagCount> findDistinctUserIdByProjectIdAndUserTag(@Param("projectId") String projectId,
                                                               @Param("tagKey") String tagKey,
                                                               @Nullable @Param("tagFilter") String tagFilter,
                                                               @Param("startDate") Date startDate,
                                                               @Param("endDate") Date endDate,
                                                               Pageable pageable)


    @Query(value = COUNT_DISTINCT_USER_BY_TAG_SQL, nativeQuery = true)
    Integer countDistinctUserTag(@Param("projectId") String projectId,
                                 @Param("tagKey") String tagKey,
                                 @Nullable @Param("tagFilter") String tagFilter,
                                 @Param("startDate") Date startDate,
                                 @Param("endDate") Date endDate)

}
