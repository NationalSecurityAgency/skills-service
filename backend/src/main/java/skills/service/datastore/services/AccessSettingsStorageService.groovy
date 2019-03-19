package skills.service.datastore.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.service.auth.UserInfo
import skills.service.auth.UserInfoService
import skills.service.controller.exceptions.ErrorCode
import skills.service.controller.exceptions.SkillException
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
        if (user) {
            // check that the new user role does not already exist
            UserRole existingUserRole = user?.roles?.find {it.projectId == projectId && it.roleName == roleName}
            assert !existingUserRole, "CREATE FAILED -> user-role with project id [$projectId], userId [$userId] and roleName [$roleName] already exists"
        } else {
            throw new SkillException("User [$userId]  does not exist", projectId)
        }

        UserRole userRole = new UserRole(userId: userId, roleName: roleName, projectId: projectId)
        user.roles.add(userRole)
        userRepository.save(user)
        log.info("Created userRole [{}]", userRole)
        return userRole
    }

    @Transactional()
    User createAppUser(UserInfo userInfo, boolean createOrUpdate) {
        log.info("Creating new app user for ID [{}], DN [{}]", userInfo.username, userInfo.userDn)

        User user = userRepository.findByUserId(userInfo.username?.toLowerCase())
        if (!createOrUpdate) {
            if (user) {
                SkillException exception = new SkillException("User [${userInfo.username?.toLowerCase()}] already exists.")
                exception.errorCode = ErrorCode.UserAlreadyExists
                throw exception
            }
        }

        if (user) {
            // updating an existing user
            updateUser(userInfo, user)
        } else {
            // create new user with APP_USER role
            user = createNewUser(userInfo)
        }
        userRepository.save(user)
        log.info("Created app user [{}]", userInfo.username)
        return user
    }

    @Transactional(readOnly = true)
    boolean rootAdminExists() {
        return userRoleRepository.existsByRoleName(RoleName.ROLE_SUPER_DUPER_USER)
    }

    @Transactional
    void grantRoot(String userId) {
        userRoleRepository.save(
                new UserRole(
                        userId: userId,
                        roleName: RoleName.ROLE_SUPER_DUPER_USER
                )
        )
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
                roles: getRoles(userInfo),
                userProps: [
                        new UserProp(name: 'DN', value: userInfo.userDn ?: ""),
                        new UserProp(name: 'email', value: userInfo.email ?: ""),
                        new UserProp(name: 'firstName', value: userInfo.firstName ?: ""),
                        new UserProp(name: 'lastName', value: userInfo.lastName ?: ""),
                ]
        )
        return user
    }

    private User updateUser(UserInfo userInfo, User user) {
        user.userId = userInfo.username?.toLowerCase()
        user.password = userInfo.password
        getOrSetUserProp(user, 'DN',  userInfo.userDn ?: "")
        getOrSetUserProp(user, 'email', userInfo.email ?: "")
        getOrSetUserProp(user, 'firstName',  userInfo.firstName ?: "")
        getOrSetUserProp(user, 'lastName',  userInfo.lastName ?: "")
//        user.roles = getRoles(userInfo)
        return user
    }

    private void getOrSetUserProp(User user, String name, value) {
        UserProp userProp = user.userProps.find {it.name == name}
        if (userProp) {
            userProp.value = value
        } else {
            user.userProps.add(new UserProp(name: name, value: value))
        }
    }

    private List<UserRole> getRoles(UserInfo userInfo) {
        List<UserRole> roles
        if (userInfo.authorities) {
            roles = []
            for (GrantedAuthority authority : userInfo.authorities) {
                roles.add(new UserRole(userId: userInfo.username?.toLowerCase(), roleName: RoleName.valueOf(authority.authority)))
            }
        } else {
            roles = [new UserRole(userId: userInfo.username?.toLowerCase(), roleName: RoleName.ROLE_APP_USER)]
        }
        return roles
    }
}
