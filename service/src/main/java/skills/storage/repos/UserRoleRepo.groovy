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

import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole

import java.util.stream.Stream

interface UserRoleRepo extends CrudRepository<UserRole, Integer> {

    interface UserRoleWithAttrs {
        UserRole getRole()
        UserAttrs getAttrs()
    }

    @Nullable
    UserRole findByUserIdAndRoleNameAndProjectId(String userId, RoleName roleName, @Nullable String projectId)

    @Nullable
    UserRole findByUserIdAndRoleNameAndQuizId(String userId, RoleName roleName, String quizId)

    @Nullable
    List<UserRole> findAllByUserId(String userId)

    @Query('''SELECT count(ur.id)
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.projectId = ?1 and 
            ur.roleName in ?2 ''')
    Integer countUserRolesByProjectIdAndUserRoles(String projectId, List<RoleName> roles)

    @Query('''SELECT ur as role, ua as attrs
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.projectId = ?1 and 
            ur.roleName in ?2 ''')
    List<UserRoleWithAttrs> findRoleWithAttrsByProjectIdAndUserRoles(String projectId, List<RoleName> roles, PageRequest pageRequest)

    @Query('''SELECT ur as role, ua as attrs
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.projectId = ?1 and
            ur.roleName = ?2 and
            ur.userId like lower(CONCAT('%', ?3, '%'))''')
    List<UserRoleWithAttrs> findRoleWithAttrsByProjectIdAndRoleNameAndUserIdLike(String projectId, RoleName roleName, String userIdQuery, PageRequest pageRequest)

    @Query('''SELECT count(ur.id) from UserRole ur, UserAttrs ua 
            where
                ur.userId = ua.userId and
                ur.projectId = ?1 and
                ur.roleName = ?2 and
                ur.userId like lower(CONCAT('%', ?3, '%'))''')
    Integer countRoleWithAttrsByProjectIdAndRoleNameAndUserIdLike(String projectId, RoleName roleName, String userIdQuery)

    @Query('''SELECT ur as role, ua as attrs
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.projectId = ?1 and
            ur.userId = ?2''')
    List<UserRoleWithAttrs> findAllByProjectIdAndUserId(String projectId, String userId)

    @Query('''SELECT ur.id as role, ua as attrs
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.projectId = ?1 and
            ur.userId = ?2 and
            ur.roleName = ?3''')
    @Nullable
    Integer findIdByProjectIdAndUserIdRoleName(String projectId, String userId, RoleName roleName)

    @Query('''SELECT ur.userId
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.projectId = ?1 and
            ur.roleName = ?2''')
    List<String> findUserIdsByProjectIdAndRoleName(String projectId, RoleName roleName)

    boolean existsByRoleName(RoleName roleName)

    @Query('''SELECT ur as role, ua as attrs
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.roleName = ?1''')
    List<UserRoleWithAttrs> findAllByRoleName(RoleName roleName)

    @Query('''SELECT ur as role, ua as attrs
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.roleName = ?1''')
    List<UserRoleWithAttrs> findAllByRoleNameWithPaging(RoleName roleName, PageRequest pageRequest)

    @Query('''SELECT count(ur.id)
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.roleName = ?1''')
    Integer countAllByRoleName(RoleName roleName)

    @Query('''SELECT ur as role, ua as attrs
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.userId not in (?1)''')
    List<UserRoleWithAttrs> findAllByUserIdNotIn(List<String> userIds)


    @Query('''
        select distinct ua.userId from UserAttrs ua 
            join UserRole ur on ur.userId = ua.userId 
            where ua.email is not null and ur.roleName = ?1 
            group by ua.userId order by ua.userId asc
    ''')
    Stream<String> findAllUserIdsWithRoleAndEmail(RoleName roleName)

    @Query(value='''
        select sum(count(distinct ua.user_id)) over () totalTotal 
        from user_attrs ua join user_roles ur on ur.user_id = ua.user_id 
        where ua.email is not null and ur.role_name = :#{#roleName.name()} group by ua.user_id order by ua.user_id asc limit 1
    ''', nativeQuery = true)
    Long countAllUserIdsWithRoleAndEmail(@Param("roleName") RoleName roleName)

    boolean existsByUserIdAndRoleName(String userId, RoleName roleName)

    int countByRoleName(RoleName roleName)
}
