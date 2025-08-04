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
    UserRole findByUserIdAndRoleNameAndProjectIdAndAdminGroupId(String userId, RoleName roleName, @Nullable String projectId, @Nullable String adminGroupId)

    @Nullable
    UserRole findByUserIdAndRoleNameAndQuizIdAndAdminGroupId(String userId, RoleName roleName, String quizId, @Nullable String adminGroupId)

    @Nullable
    UserRole findByUserIdAndRoleNameAndGlobalBadgeIdAndAdminGroupId(String userId, RoleName roleName, String globalBadgeId, @Nullable String adminGroupId)

    @Nullable
    UserRole findByUserIdAndRoleNameAndAdminGroupId(String userId, RoleName roleName, String adminGroupId)

    @Nullable
    @Query('''SELECT DISTINCT 'true' from UserRole ur where ur.userId = ?1 and ur.roleName = 'ROLE_QUIZ_ADMIN' and ur.quizId = ?2 and ur.adminGroupId is not null''')
    Boolean isUserQuizGroupAdmin(String userId, String quizId)

    @Nullable
    @Query('''SELECT DISTINCT 'true' from UserRole ur where ur.userId = ?1 and ur.roleName = 'ROLE_PROJECT_ADMIN' and ur.projectId = ?2 and ur.adminGroupId is not null''')
    Boolean isUserProjectGroupAdmin(String userId, String projectId)

    @Nullable
    @Query('''SELECT DISTINCT 'true' from UserRole ur where ur.userId = ?1 and ur.roleName = 'ROLE_PROJECT_ADMIN' and ur.projectId = ?2''')
    Boolean isUserProjectAdmin(String userId, String projectId)

    @Nullable
    @Query(value = '''SELECT DISTINCT 'true' FROM user_roles ur WHERE ur.user_id = ?1 and ur.role_name in ('ROLE_ADMIN_GROUP_MEMBER','ROLE_ADMIN_GROUP_OWNER') and ur.admin_group_id = ?2 ''', nativeQuery = true)
    Boolean isUserGroupAdminMemberOrOwner(String userId, String adminGroupId)

    @Nullable
    @Query('''SELECT DISTINCT 'true' from UserRole ur where ur.userId = ?1 and ur.roleName = 'ROLE_GLOBAL_BADGE_ADMIN' and ur.globalBadgeId = ?2''')
    Boolean isUserGlobalBadgeAdmin(String userId, String projectId)

    void deleteByQuizIdAndAdminGroupIdAndRoleName(String quizId, String adminGroupId, RoleName roleName)

    void deleteByProjectIdAndAdminGroupIdAndRoleName(String quizId, String adminGroupId, RoleName roleName)

    void deleteByUserIdAndAdminGroupIdAndRoleNameIn(String userId, String adminGroupId, List<RoleName> roleName)

    void deleteByGlobalBadgeIdAndRoleName(String globalBadgeId, RoleName roleName)

    @Nullable
    List<UserRole> findAllByUserId(String userId)

    @Nullable
    List<UserRole> findAllByProjectIdIgnoreCase(String projectId)

    @Nullable
    List<UserRole> findAllByAdminGroupIdIgnoreCase(String adminGroupId)

    @Nullable
    List<UserRole> findAllByQuizIdIgnoreCase(String quizId)

    @Query('''SELECT count(ur.id)
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.projectId = ?1 and 
            ur.roleName in ?2 ''')
    Integer countUserRolesByProjectIdAndUserRoles(String projectId, List<RoleName> roles)

    @Query('''SELECT ur.projectId from UserRole ur where ur.userId = ?1 and ur.roleName = ?2 and ur.projectId in ?3''')
    List<String> findProjectIdsByUserIdAndRoleNameAndProjectIdIn(String userId, RoleName roleName, List<String> projectIds)

    @Query('''SELECT count(ur.id)
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.adminGroupId = ?1 and 
            ur.roleName in ?2 ''')
    Integer countUserRolesByAdminGroupIdAndUserRoles(String projectId, List<RoleName> roles)

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
            ur.adminGroupId = ?1 and 
            ur.roleName in ?2 ''')
    List<UserRoleWithAttrs> findRoleWithAttrsByAdminGroupIdAndUserRoles(String adminGroupId, List<RoleName> roles, PageRequest pageRequest)

    @Query('''SELECT ur as role, ua as attrs
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.projectId = ?1 and
            ur.roleName = ?2 and
            (ur.userId like lower(CONCAT('%', ?3, '%')) or
            (lower(CONCAT(ua.userIdForDisplay, ' (', ua.lastName, ', ', ua.firstName,  ')')) like lower(CONCAT('%', ?3, '%'))))
    ''')
    List<UserRoleWithAttrs> findRoleWithAttrsByProjectIdAndRoleNameAndUserIdLike(String projectId, RoleName roleName, String userIdQuery, PageRequest pageRequest)

    @Query('''SELECT count(ur.id) from UserRole ur, UserAttrs ua 
            where
                ur.userId = ua.userId and
                ur.projectId = ?1 and
                ur.roleName = ?2 and
                (ur.userId like lower(CONCAT('%', ?3, '%')) or
                (lower(CONCAT(ua.userIdForDisplay, ' (', ua.lastName, ', ', ua.firstName,  ')')) like lower(CONCAT('%', ?3, '%'))))
    ''')
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

    @Query(value='''
        select ur.projectId from UserRole ur where ur.userId=?1 and ur.roleName=?2
    ''')
    List<String> getProjectIdByUserIdAndRoleName(String userId, RoleName roleName)

    int countByRoleName(RoleName roleName)

    @Query('''SELECT ur as role, ua as attrs
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.quizId = ?1''')
    List<UserRoleWithAttrs> findRoleWithAttrsByQuizId(String quizId)

    @Query('''SELECT ur as role, ua as attrs
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.globalBadgeId = ?1''')
    List<UserRoleWithAttrs> findRoleWithAttrsByGlobalBadgeId(String badgeId)


    @Query('''SELECT ur as role, ua as attrs
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and
            ur.roleName IN (:#{#roleNames.![name()]}) and
            ur.adminGroupId = ?1''')
    List<UserRoleWithAttrs> findRoleWithAttrsByAdminGroupIdAndRoleNameIn(String adminGroupId, List<RoleName> roleNames)

    @Query(value='''
        select distinct(ur.quizId) from UserRole ur where ur.adminGroupId=?1 and ur.roleName=skills.storage.model.auth.RoleName.ROLE_QUIZ_ADMIN
    ''')
    List<String> findQuizIdsByAdminGroupId(String adminGroupId)

    @Query(value='''
        select distinct(ur.projectId) from UserRole ur where ur.adminGroupId=?1 and ur.roleName=skills.storage.model.auth.RoleName.ROLE_PROJECT_ADMIN
    ''')
    List<String> findProjectIdsByAdminGroupId(String adminGroupId)

}
