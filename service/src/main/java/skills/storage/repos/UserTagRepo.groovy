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

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.UserTag

import skills.storage.model.auth.User

interface UserTagRepo extends CrudRepository<UserTag, Integer> {

    @Nullable
    User findByUserId(String userId)

    @Modifying
    @Query("delete from UserTag ut where ut.userId = ?1")
    void deleteByUserId(String userId)

    static interface UserTagCount {
        Integer getNumUsers()
        String getTag()
    }

    @Nullable
    @Query('''SELECT COUNT(DISTINCT ups.userId) as numUsers, ut.value as tag
        from UserPerformedSkill ups
        join UserTag ut on ut.userId = ups.userId 
        where ups.projectId=?1 and ut.key = ?2 
        group by ut.value''')
    List<UserTagCount> countDistinctUserIdByProjectIdAndUserTag(String projectId, String tagKey)

}
