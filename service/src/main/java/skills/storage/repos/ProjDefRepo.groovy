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
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.ProjDef

interface ProjDefRepo extends CrudRepository<ProjDef, Long> {

    @Nullable
    ProjDef findByProjectIdIgnoreCase(String projectId)

    @Nullable
    List<ProjDef> findAllByProjectIdIn(List<String> projectIds)

    List<ProjDef> findAll()

    void deleteByProjectIdIgnoreCase(String projectId)

    @Nullable
    ProjDef findByProjectId(String projectId)

    @Nullable
    ProjDef findByNameIgnoreCase(String projectId)

    boolean existsByProjectIdIgnoreCase(String projectId)
    boolean existsByNameIgnoreCase(String projectName)

    @Query(value = "select p from ProjDef p, UserRole u where p.projectId = u.projectId and u.userId=?1")
    List<ProjDef> getProjectsByUser(String userId)

    @Query("select p from ProjDef p where upper(p.name) like UPPER(CONCAT('%', ?1, '%'))")
    List<ProjDef> findByNameLike(String search)

    @Query(value = "select count(p.id) from ProjDef p, UserRole u where p.projectId = u.projectId and u.userId=?1")
    Integer getProjectsByUserCount(String userId)

    @Query("select p from ProjDef p where lower(p.name) LIKE %?1% and p.projectId<>?2" )
    List<ProjDef> queryProjectsByNameQueryAndNotProjectId(String nameQuery, String notProjectId, Pageable pageable)
}
