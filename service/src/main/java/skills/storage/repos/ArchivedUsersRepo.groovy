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
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.ArchivedUser
import skills.storage.model.UserAttrs

interface ArchivedUsersRepo extends CrudRepository<ArchivedUser, Long> {
    interface ArchivedUserWithAttrs {
        UserAttrs getAttrs()
        ArchivedUser getArchivedUser()
    }
    @Query('''SELECT ua as attrs, au as archivedUser
        from UserAttrs ua, ArchivedUser au 
        where
            au.userId = ua.userId and 
            au.projectId = ?1''')
    Page<ArchivedUserWithAttrs> findAllByProjectId(String projectId, Pageable pageable)

    @Nullable
    ArchivedUser findByProjectIdAndUserId(String projectId, String userId)

    Boolean existsByProjectIdAndUserId(String projectId, String userId)
}
