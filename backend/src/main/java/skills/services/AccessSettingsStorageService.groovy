package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.pki.PkiUserLookup
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.UserSettingsRequest
import skills.controller.result.model.SettingsResult
import skills.controller.result.model.UserInfoRes
import skills.controller.result.model.UserRoleRes
import skills.services.settings.SettingsService
import skills.storage.model.Setting
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.User

import skills.storage.model.auth.UserRole
import skills.storage.repos.UserRepo
import skills.storage.repos.UserRoleRepo

import static skills.controller.exceptions.SkillException.NA

@Service
@Slf4j
class AccessSettingsStorageService {

    static String USER_INFO_SETTING_GROUP = "user_info"

    @Autowired
    UserRoleRepo userRoleRepository

    @Autowired
    UserRepo userRepository

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserDetailsService userDetailsService

    @Autowired
    InceptionProjectService inceptionProjectService

    @Autowired(required = false)
    PkiUserLookup pkiUserLookup

    @Autowired
    SettingsService settingsService

    @Autowired
    SortingService sortingService

    @Transactional(readOnly = true)
    List<UserRoleRes> getUserRolesForProjectId(String projectId) {
        List<UserRole> res = userRoleRepository.findAllByProjectId(projectId)
        return res.collect { convert(it) }
    }

    @Transactional(readOnly = true)
    List<UserRoleRes> getUserRolesForProjectIdAndUserId(String projectId, String userId) {
        List<UserRole> res = userRoleRepository.findAllByProjectIdAndUserId(projectId, userId)
        return res.collect { convert(it) }
        return res
    }

    @Transactional(readOnly = true)
    List<UserRoleRes> getUserRolesWithRole(RoleName roleName) {
        List<UserRole> roles = userRoleRepository.findAllByRoleName(roleName)
        return roles.collect { convert(it) }
    }

    @Transactional(readOnly = true)
    List<UserRoleRes> getUserRolesWithoutRole(RoleName roleName) {
        List<UserRoleRes> usersWithRole = getUserRolesWithRole(roleName)
        List<UserRole> res
        if (usersWithRole) {
            res = userRoleRepository.findAllByUserIdNotIn(usersWithRole.collect { it.userId }.unique())
        } else {
            res = userRepository.findAll()
        }
        return res.collect { convert(it) }
    }

    @Transactional(readOnly = true)
    List<UserRoleRes> getRootUsers() {
        List<UserRole> roles = userRoleRepository.findAllByRoleName(RoleName.ROLE_SUPER_DUPER_USER)
        return roles.collect { convert(it) }
    }

    @Transactional(readOnly = true)
    List<UserRoleRes> getNonRootUsers() {
        List<UserRole> rootUsers = getRootUsers()
        List<UserRole> roles = userRoleRepository.findAllByUserIdNotIn(rootUsers.collect {it.userId}.unique())
        return roles.collect { convert(it) }
    }

    @Transactional(readOnly = true)
    boolean isRoot(String userId) {
        return userRoleRepository.existsByUserIdAndRoleName(userId, RoleName.ROLE_SUPER_DUPER_USER)
    }

    @Transactional
    UserRoleRes addRoot(String userId) {
        UserRole userRole = addUserRoleInternal(userId, null, RoleName.ROLE_SUPER_DUPER_USER)
        inceptionProjectService.createInceptionAndAssignUser(userId)
        return convert(userRole)
    }

    @Transactional
    void deleteRoot(String userId) {
        userId = userId?.toLowerCase()
        deleteUserRoleInternal(userId, null, RoleName.ROLE_SUPER_DUPER_USER)
        inceptionProjectService.removeUser(userId)
    }

    @Transactional()
    void deleteUserRole(String userId, String projectId, RoleName roleName) {
        userId = userId?.toLowerCase()
        if (userId != userInfoService.getCurrentUser().username.toLowerCase()) {
            deleteUserRoleInternal(userId, projectId, roleName)
        } else {
            throw new SkillException("You cannot delete yourself.")
        }
    }

    @Transactional(readOnly = true)
    int getRootAdminCount() {
        return userRoleRepository.countByRoleName(RoleName.ROLE_SUPER_DUPER_USER)
    }

    private void deleteUserRoleInternal(String userId, String projectId, RoleName roleName) {
        log.debug('Deleting user-role for userId [{}] and role [{}] on project [{}]', userId, roleName, projectId)
        User user = userRepository.findByUserIdIgnoreCase(userId)
        UserRole userRole = user?.roles?.find {it.projectId == projectId && it.roleName == roleName}
        assert userRole, "DELETE FAILED -> no user-role with project id [$projectId], userId [$userId] and roleName [$roleName]"

        assert user.roles.remove(userRole), "DELETE FAILED -> failed to remove user-role with project id [$projectId], userId [$userId] and roleName [$roleName]"
        userRepository.save(user)
        log.debug("Deleted userRole [{}]", userRole)
    }

    @Transactional()
    UserRoleRes addUserRole(String userId, String projectId, RoleName roleName) {
        UserRole role = addUserRoleInternal(userId, projectId, roleName)
        return convert(role)
    }

    private UserRole addUserRoleInternal(String userId, String projectId, RoleName roleName) {
        log.debug('Creating user-role for ID [{}] and role [{}] on project [{}]', userId, roleName, projectId)
        User user = userRepository.findByUserIdIgnoreCase(userId)
        if (user) {
            // check that the new user role does not already exist
            UserRole existingUserRole = user?.roles?.find {it.projectId == projectId && it.roleName == roleName}
            assert !existingUserRole, "CREATE FAILED -> user-role with project id [$projectId], userId [$userId] and roleName [$roleName] already exists"
        } else {
            throw new SkillException("User [$userId]  does not exist", (String) projectId)
        }

        UserRole userRole = new UserRole(userId: userId, roleName: roleName, projectId: projectId)
        user.roles.add(userRole)
        userRepository.save(user)
        log.debug("Created userRole [{}]", userRole)

        log.debug("setting sort order for user [{}] on project [{}]", userId, projectId)
        sortingService.setNewProjectDisplayOrder(projectId, userId)
        return userRole
    }

    @Transactional()
    User createAppUser(UserInfo userInfo, boolean createOrUpdate) {
        validateUserInfo(userInfo)
        User user = userRepository.findByUserIdIgnoreCase(userInfo.username?.toLowerCase())
        if (!createOrUpdate) {
            if (user) {
                SkillException exception = new SkillException("User [${userInfo.username?.toLowerCase()}] already exists.")
                exception.errorCode = ErrorCode.UserAlreadyExists
                throw exception
            }
        }

        if (user) {
            // updating an existing user
            log.debug("Updating existing app user for ID [{}], DN [{}]", userInfo.username, userInfo.userDn)
            updateUser(userInfo, user)
        } else {
            // create new user with APP_USER role
            log.debug("Creating new app user for ID [{}], DN [{}]", userInfo.username, userInfo.userDn)
            user = createNewUser(userInfo)
        }
        userRepository.save(user)
        saveSettings(user.userId, userInfo)

        return user
    }

    private void validateUserInfo(UserInfo userInfo) {
        if (!userInfo.firstName || userInfo.firstName.length() > 30) {
            throw new SkillException("First Name is required and can be no longer than 30 characters", NA, NA, ErrorCode.BadParam)
        }
        if (!userInfo.lastName || userInfo.lastName.length() > 30) {
            throw new SkillException("Last Name is required and can be no longer than 30 characters", NA, NA, ErrorCode.BadParam)
        }
        // nickname by default is "firstName lastName"
        if (userInfo.nickname && userInfo.nickname.length() > 70) {
            throw new SkillException("Nickname cannot be over 70 characters", NA, NA, ErrorCode.BadParam)
        }
    }

    @Transactional(readOnly = true)
    boolean rootAdminExists() {
        return userRoleRepository.existsByRoleName(RoleName.ROLE_SUPER_DUPER_USER)
    }

    @Transactional
    UserRoleRes grantRoot(String userId) {
        User user = userRepository.findByUserIdIgnoreCase(userId.toLowerCase())
        if (!user) {
            SkillException exception = new SkillException("User [${userId.toLowerCase()}] does not exist.")
            exception.errorCode = ErrorCode.BadParam
            throw exception
        }
        UserRole role = new UserRole(
                userId: userId,
                roleName: RoleName.ROLE_SUPER_DUPER_USER
        )

        user.roles.add(role)
        userRepository.save(user)
        return convert(role)
    }

    UserInfoRes loadUserInfo(String userId) {
        User user = userRepository.findByUserIdIgnoreCase(userId)
        List<SettingsResult> settings = settingsService.getUserSettingsForGroup(user.userId, USER_INFO_SETTING_GROUP)
        new UserInfoRes(
                userId: user.userId,
                first: settings.find({it.setting == "firstName"})?.value ?: "",
                last: settings.find({it.setting == "lastName"})?.value ?: "",
                nickname: settings.find({it.setting == "nickname"})?.value ?: "",
                dn: settings.find({it.setting == "DN"})?.value ?: "",
        )
    }

    private User createNewUser(UserInfo userInfo) {
        String userId = userInfo.username?.toLowerCase()
        User user = new User(
                userId: userId,
                password: userInfo.password,
                roles: getRoles(userInfo),
        )
        return user
    }



    private void updateUser(UserInfo userInfo, User user) {
        user.userId = userInfo.username?.toLowerCase()
        user.password = userInfo.password

        saveSettings(user.userId, userInfo)
    }

    void saveSettings(String userId, UserInfo userInfo) {
        List<UserSettingsRequest> settingsRequests = [
                createSetting(userId, 'DN', userInfo.userDn ?: ""),
                createSetting(userId, 'email', userInfo.email ?: ""),
                createSetting(userId, 'firstName', userInfo.firstName ?: ""),
                createSetting(userId, 'lastName', userInfo.lastName ?: ""),
                createSetting(userId, 'nickname', userInfo.nickname ?: ""),
        ]
        settingsService.saveSettings(settingsRequests)
    }

    private UserSettingsRequest createSetting(String userId, String prop, String value) {
        UserSettingsRequest settingsRequest =
                new UserSettingsRequest(
                        userId: userId,
                        settingGroup: USER_INFO_SETTING_GROUP,
                        setting: prop,
                        value: value
                )
        return settingsRequest
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

    private UserRoleRes convert(UserRole inputRole) {
        UserRoleRes res = new UserRoleRes(
                userId: inputRole.userId,
                projectId: inputRole.projectId,
                roleName: inputRole.roleName,
        )
        return res
    }
}
