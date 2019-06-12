package skills.storage.repos

import org.springframework.data.repository.CrudRepository
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole

interface UserRoleRepo extends CrudRepository<UserRole, Integer> {

    List<UserRole> findAllByProjectId(String projectId)

    List<UserRole> findAllByProjectIdAndUserId(String projectId, String userId)

    boolean existsByRoleName(RoleName roleName)

    List<UserRole> findAllByRoleName(RoleName roleName)

    List<UserRole> findAllByUserIdNotIn(List<String> userIds)

    boolean existsByUserIdAndRoleName(String userId, RoleName roleName)

    int countByRoleName(RoleName roleName)
}
