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
import skills.storage.model.ProjectAccessToken

interface ProjectAccessTokenRepo extends CrudRepository<ProjectAccessToken, Integer> {

    @Nullable
    @Query("select p from ProjectAccessToken p where p.token = ?1")
    ProjectAccessToken findByToken(String token)

    @Nullable
    @Query("select p from ProjectAccessToken p where p.token = ?1 and p.project.projectId = ?2")
    ProjectAccessToken findByTokenAndProjectId(String token, String projectId)

    @Nullable
    @Query("select p from ProjectAccessToken p where p.project.projectId = :projectId and p.recipientEmail = :recipientEmail")
    ProjectAccessToken findByProjectIdAndRecipientEmail(@Param("projectId") String projectId, @Param("recipientEmail") String recipientEmail)

    void deleteByToken(String token)

    @Modifying
    @Query("delete from ProjectAccessToken p where p.token = ?1 and p.project.projectId = ?2")
    void deleteByTokenAndProjectId(String token, String projectId)

    @Modifying
    @Query("delete from ProjectAccessToken p where p.claimed is null and p.expires < ?1")
    void deleteExpiredTokensOlderThen(Date deleteBefore)

    @Modifying
    @Query("delete from ProjectAccessToken  p where p.claimed is not null and p.claimed < ?1")
    void deleteClaimedTokensOlderThen(Date deleteBefore)

    void deleteByExpiresBefore(Date date)

    @Nullable
    @Query("select p from ProjectAccessToken p where p.project.projectId = :projectId and p.claimed is null and p.recipientEmail like lower(CONCAT('%', :userEmail, '%'))")
    List<ProjectAccessToken> findAllUnclaimedByProjectId(@Param("projectId") String projectId, @Param("userEmail") String userEmail, Pageable pageRequest)

    @Query("select count(p.id) from ProjectAccessToken p where p.project.projectId = :projectId and p.claimed is null and p.recipientEmail like lower(CONCAT('%', :userEmail, '%'))")
    long countAllUnclaimedByProjectId(@Param("projectId") String projectId, @Param("userEmail") String userEmail)

    @Modifying
    @Query(value = "delete from project_access_token p where p.proj_ref_id in (select pd.id from project_definition pd where pd.project_id = :projectId) and p.recipient_email = :recipientEmail", nativeQuery = true)
    void deleteByProjectIdAndRecipientEmail(@Param("projectId") String projectId, @Param("recipientEmail") String recipientEmail)
}
