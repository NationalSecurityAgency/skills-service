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

    @Nullable
    @Query('''SELECT COUNT(DISTINCT up.userId) as numUsers, ut.value as tag
        from UserPoints up
        join UserTag ut on ut.userId = up.userId 
        where up.skillRefId in (
                select case when sd.copiedFrom is not null then sd.copiedFrom else sd.id end as id 
                from SkillDef sd
                where sd.type = 'Skill' and sd.projectId = ?1 and sd.enabled = 'true'
            )
            and up.skillRefId is not null 
            and ut.key = ?2 
            and LOWER(ut.value) LIKE LOWER(CONCAT('%',?3,'%'))
            and not exists (select 1 from ArchivedUser au where au.userId = up.userId and au.projectId = ?1)   
        group by ut.value''')
    List<UserTagCount> findDistinctUserIdByProjectIdAndUserTag(String projectId, String tagKey, String tagFilter, Pageable pageable)


    @Query('''SELECT COUNT(DISTINCT ut.value)
        from UserPoints up
        join UserTag ut on ut.userId = up.userId 
        where up.skillRefId in (
                select case when sd.copiedFrom is not null then sd.copiedFrom else sd.id end as id 
                from SkillDef sd
                where sd.type = 'Skill' and sd.projectId = ?1 and sd.enabled = 'true'
            )  
            and up.skillRefId is not null 
            and ut.key = ?2 
            and LOWER(ut.value) LIKE LOWER(CONCAT('%',?3,'%'))
            and not exists (select 1 from ArchivedUser au where au.userId = up.userId and au.projectId = ?1)''')
    Integer countDistinctUserTag(String projectId, String tagKey, String tagFilter)

}
