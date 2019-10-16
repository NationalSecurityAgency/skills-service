package skills.storage.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole

interface UserRoleRepo extends CrudRepository<UserRole, Integer> {

    interface UserRoleWithAttrs {
        UserRole getRole()
        UserAttrs getAttrs()
    }

    @Query('''SELECT ur as role, ua as attrs
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.projectId = ?1''')
    List<UserRoleWithAttrs> findRoleWithAttrsByProjectId(String projectId)

    @Query('''SELECT ur as role, ua as attrs
        from UserRole ur, UserAttrs ua 
        where
            ur.userId = ua.userId and 
            ur.projectId = ?1 and
            ur.userId = ?2''')
    List<UserRoleWithAttrs> findAllByProjectIdAndUserId(String projectId, String userId)

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
            ur.userId not in (?1)''')
    List<UserRoleWithAttrs> findAllByUserIdNotIn(List<String> userIds)

    boolean existsByUserIdAndRoleName(String userId, RoleName roleName)

    int countByRoleName(RoleName roleName)
}
