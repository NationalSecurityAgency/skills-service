/**
 * Copyright 2026 SkillTree
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
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import org.springframework.lang.Nullable

interface ProjectParticipationRepo extends CrudRepository<ProjectParticipation, Integer> {
    @Nullable
    ProjectParticipation findByProjectIdAndUserId(String projectId, String userId)
    boolean existsByProjectIdAndUserId(String projectId, String userId)

    @Query(value = "select * from project_participation where project_id = ?1 and backfill_status = 'PENDING' and id > ?2 order by id", nativeQuery = true)
    List<ProjectParticipation> findPending(String projectId, Integer afterId, Pageable pageable)

    @Nullable
    @Query('select p.backfillStatus from ProjectParticipation p where p.projectId = ?1 and p.userId = ?2')
    BackfillStatus readStatus(String projectId, String userId)

    @Modifying
    @Query(value = '''update project_participation set updated = ?3
        where project_id = ?1 and user_id = ?2 and updated < ?3''', nativeQuery = true)
    int touchActivity(String projectId, String userId, Date now)
}
