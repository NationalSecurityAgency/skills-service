package skills.service.datastore.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.service.auth.UserInfo
import skills.service.auth.UserInfoService
import skills.storage.model.auth.*
import skills.storage.repos.AllowedOriginRepo
import skills.storage.repos.UserRepo
import skills.storage.repos.UserRoleRepo

@Service
@Slf4j
class AccessSettingsStorageService {

    @Autowired
    UserRoleRepo userRoleRepository

    @Autowired
    UserRepo userRepository

    @Autowired
    AllowedOriginRepo allowedOriginRepository

    @Autowired
    UserInfoService userInfoService

    @Transactional(readOnly = true)
    List<UserRole> getUserRoles(String projectId) {
        List<UserRole> res = userRoleRepository.findAllByProjectId(projectId)
        return res
    }

    @Transactional()
    void deleteUserRole(UserInfo userInfo, String projectId, RoleName roleName) {
        String userId = userInfo.username?.toLowerCase()
        log.info("Deleting user-role for DN [{}] and role [{}] on project [{}]", userId, roleName, projectId)
        User user = userRepository.findByUserId(userId)
        UserRole userRole = user?.roles?.find {it.projectId == projectId && it.roleName == roleName}
        assert userRole, "DELETE FAILED -> no user-role with project id [$projectId], userId [$userId] and roleName [$roleName]"

        assert user.roles.remove(userRole), "DELETE FAILED -> failed to remove user-role with project id [$projectId], userId [$userId] and roleName [$roleName]"
        userRepository.save(user)
        log.info("Deleted userRole [{}]", userRole)
    }

    @Transactional()
    UserRole addUserRole(UserInfo userInfo, String projectId, RoleName roleName) {
        String userId = userInfo.username?.toLowerCase()
        String userDn = userInfo.userDn?.toLowerCase()
        log.info("Creating user-role for ID [{}], DN [{}] and role [{}] on project [{}]", userId, userDn, roleName, projectId)

        User user = userRepository.findByUserId(userId)
        if (!user) {
            // create user
            user = createNewUser(userInfo)
        } else {
            // check that the new user role does not already exist
            UserRole existingUserRole = user?.roles?.find {it.projectId == projectId && it.roleName == roleName}
            assert !existingUserRole, "CREATE FAILED -> user-role with project id [$projectId], userId [$userId] and roleName [$roleName] already exists"
        }

        UserRole userRole = new UserRole(userId: userId, roleName: roleName, projectId: projectId)
        user.roles.add(userRole)
        userRepository.save(user)
        log.info("Created userRole [{}]", userRole)
        return userRole
    }

    @Transactional()
    UserRole createAppUser(UserInfo userInfo) {
        log.info("Creating new app user for ID [{}], DN [{}]", userInfo.username, userInfo.userDn)

        User user = userRepository.findByUserId(userInfo.username?.toLowerCase())
        assert !user, "CREATE FAILED -> user with userId [$user.userId] already exists"

        // create new user with APP_USER role
        user = createNewUser(userInfo)
        userRepository.save(user)
        log.info("Created app user [{}]", userInfo.username)
        return user.roles.first()
    }

    @Transactional(readOnly = true)
    List<AllowedOrigin> getAllowedOrigins(String projectId) {
        List<AllowedOrigin> res = allowedOriginRepository.findAllByProjectId(projectId)
        return res
    }

    @Transactional()
    AllowedOrigin saveOrUpdateAllowedOrigin(String projectId, AllowedOrigin update) {
        assert update
        assert update.allowedOrigin
        assert update.projectId
        assert update.projectId == projectId

        String allowedOriginName = update.allowedOrigin

        log.info("Updating allowed origin [{}] for project [{}]", allowedOriginName, projectId)
        allowedOriginRepository.save(update)
        log.info("Updated allowedOrigin [{}]", update)
        return update
    }

    @Transactional()
    void deleteAllowedOrigin(String projectId, Integer allowedOriginId) {
        log.info("Deleting allowedOrigin [{}] for project [{}]", allowedOriginId, projectId)
        Optional<AllowedOrigin> existing = allowedOriginRepository.findById(allowedOriginId)
        assert existing.present, "DELETE FAILED -> no allowedOrigin [$allowedOriginId] found for project id [$projectId]"

        AllowedOrigin toDelete = existing.get()
        allowedOriginRepository.delete(toDelete)
        log.info("Deleted allowedOrigin [{}]", toDelete)
    }

    private User createNewUser(UserInfo userInfo) {
        User user = new User(
                userId: userInfo.username?.toLowerCase(),
                password: userInfo.password,
                roles: [new UserRole(userId: userInfo.username?.toLowerCase(), roleName: RoleName.ROLE_APP_USER)],
                userProps: [
                        new UserProp(name: 'DN', value: userInfo.userDn ?: ""),
                        new UserProp(name: 'email', value: userInfo.email ?: ""),
                        new UserProp(name: 'firstName', value: userInfo.firstName ?: ""),
                        new UserProp(name: 'lastName', value: userInfo.lastName ?: ""),
                ]
        )
        return user
    }
}
