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

import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.ProjDef
import skills.storage.model.SkillsDBLock
import skills.storage.model.UserAttrs
import skills.storage.model.UserPoints

import javax.persistence.LockModeType

interface SkillsDBLockRepo extends CrudRepository<SkillsDBLock, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    SkillsDBLock findByLock(String lock)

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query('''select p 
        from ProjDef p 
        where
            lower(p.projectId) = lower(?1)''')
    ProjDef findByProjectIdIgnoreCase(String projectId)

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query('''select attrs 
        from UserAttrs attrs 
        where
            attrs.userId = ?1''')
    UserAttrs findUserAttrsByUserId(String userId)


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Nullable
    @Query('''select up 
        from UserPoints up 
        where
            up.projectId = ?1 and
            up.userId = ?1''')
    UserPoints findUserPointsByProjectIdAndUserId(String projectId, String userId)
}
