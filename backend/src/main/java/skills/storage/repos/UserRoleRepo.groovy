package skills.storage.repos


import org.springframework.data.repository.CrudRepository
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole

interface UserRoleRepo extends CrudRepository<UserRole, Integer> {

    List<UserRole> findAllByProjectId(String projectId)

    boolean existsByRoleName(RoleName roleName)
}
