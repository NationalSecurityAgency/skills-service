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
package skills.services

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.pki.PkiUserLookup
import skills.controller.UserInfoController
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.result.model.UserInfoRes
import skills.controller.result.model.UserRoleRes
import skills.services.inception.InceptionProjectService
import skills.services.settings.SettingsService
import skills.storage.model.UserAttrs
import skills.storage.model.UserTag
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.User
import skills.storage.model.auth.UserRole
import skills.storage.repos.UserRepo
import skills.storage.repos.UserRoleRepo
import skills.storage.repos.UserTagRepo

import java.util.stream.Stream

@Service
@Slf4j
class AccessSettingsStorageService {

    static final String USER_PREFS_GROUP = UserInfoController.USER_PREFS_GROUP
    static final String HOME_PAGE_PREF = UserInfoController.HOME_PAGE_PREF

    @Autowired
    UserRoleRepo userRoleRepository

    @Autowired
    UserRepo userRepository

    @Autowired
    UserTagRepo userTagsRepository

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
    ProjectSortingService sortingService

    @Autowired
    UserInfoValidator userInfoValidator

    @Autowired
    UserAttrsService userAttrsService

    @Value('#{"${skills.config.ui.defaultLandingPage:admin}"}')
    String defaultLandingPage

    @Transactional(readOnly = true)
    List<UserRoleRes> getUserRolesForProjectId(String projectId) {
        List<UserRoleRepo.UserRoleWithAttrs> res = userRoleRepository.findRoleWithAttrsByProjectId(projectId)
        return res.collect { convert(it) }
    }

    @Transactional(readOnly = true)
    List<UserRoleRes> getUserRolesForProjectIdAndUserId(String projectId, String userId) {
        List<UserRoleRepo.UserRoleWithAttrs> res = userRoleRepository.findAllByProjectIdAndUserId(projectId, userId)
        return res.collect { convert(it) }
        return res
    }

    @Transactional(readOnly = true)
    boolean isProjectAdmin(String projectId, String userId) {
        Integer id = userRoleRepository.findIdByProjectIdAndUserIdRoleName(projectId, userId, RoleName.ROLE_PROJECT_ADMIN)
        return id != null;
    }

    @Transactional(readOnly = true)
    List<String> getProjectAdminIds(String projectId) {
        return userRoleRepository.findUserIdsByProjectIdAndRoleName(projectId, RoleName.ROLE_PROJECT_ADMIN)
    }

    @Transactional(readOnly = true)
    getUserRolesWithRole(RoleName roleName) {
        List<UserRoleRepo.UserRoleWithAttrs> roles = userRoleRepository.findAllByRoleName(roleName)
        return roles.collect { convert(it) }
    }

    @Transactional(readOnly = true)
    Stream<String> getUsersIdsWithRoleAndEmail(RoleName roleName) {
        return userRoleRepository.findAllUserIdsWithRoleAndEmail(roleName)
    }

    @Transactional(readOnly = true)
    Long countUserIdsWithRoleAndEmail(RoleName roleName) {
        return userRoleRepository.countAllUserIdsWithRoleAndEmail(roleName)
    }

    @Transactional(readOnly = true)
    List<UserRoleRes> getUserRolesWithoutRole(RoleName roleName) {

        List<UserRoleRes> usersWithRole = getUserRolesWithRole(roleName)
        List<UserRoleRepo.UserRoleWithAttrs> res
        if (usersWithRole) {
            res = userRoleRepository.findAllByUserIdNotIn(usersWithRole.collect { it.userId }.unique())
        } else {
            res = userRoleRepository.findAll()
        }
        return res.collect { convert(it) }
    }

    @Transactional(readOnly = true)
    List<UserRoleRes> getRootUsers() {
        List<UserRoleRepo.UserRoleWithAttrs> roles = userRoleRepository.findAllByRoleName(RoleName.ROLE_SUPER_DUPER_USER)
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
        User user = userRepository.findByUserId(userId)
        if (!(user?.roles?.find {it.projectId == null && it.roleName == RoleName.ROLE_SUPERVISOR})) {
            addUserRoleInternal(userId, null, RoleName.ROLE_SUPERVISOR)
        }
        inceptionProjectService.createInceptionAndAssignUser(userId)
        return convert(userRole)
    }

    @Transactional
    void deleteRoot(String userId) {
        userId = userId?.toLowerCase()
        deleteUserRoleInternal(userId, null, RoleName.ROLE_SUPER_DUPER_USER)

        User user = userRepository.findByUserId(userId)
        if (user?.roles?.find {it.projectId == null && it.roleName == RoleName.ROLE_SUPERVISOR}) {
            deleteUserRoleInternal(userId, null, RoleName.ROLE_SUPERVISOR)
        }
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
        User user = userRepository.findByUserId(userId?.toLowerCase())
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
        String userIdLower = userId?.toLowerCase()
        User user = userRepository.findByUserId(userIdLower)
        if (user) {
            // check that the new user role does not already exist
            UserRole existingUserRole = user?.roles?.find {it.projectId == projectId && it.roleName == roleName}
            assert !existingUserRole, "CREATE FAILED -> user-role with project id [$projectId], userIdLower [$userIdLower] and roleName [$roleName] already exists"
        } else {
            throw new SkillException("User [$userIdLower]  does not exist", (String) projectId ?: SkillException.NA, SkillException.NA, ErrorCode.UserNotFound)
        }

        UserRole userRole = new UserRole(userId: userIdLower, roleName: roleName, projectId: projectId)
        user.roles.add(userRole)
        userRepository.save(user)
        log.debug("Created userRole [{}]", userRole)

        log.debug("setting sort order for user [{}] on project [{}]", userIdLower, projectId)
        sortingService.setNewProjectDisplayOrder(projectId, userIdLower)
        return userRole
    }

    @Transactional()
    @Profile
    UserAndUserAttrsHolder createAppUser(UserInfo userInfo, boolean createOrUpdate) {
        userInfoValidator.validate(userInfo)
        String userId = userInfo.username?.toLowerCase()
        UserAttrs userAttrs = userAttrsService.saveUserAttrs(userId, userInfo)

        User user = loadUserFromLocalDb(userId)
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
        userTagsRepository.deleteByUserId(userId)
        userTagsRepository.saveAll(userInfo.additionalAttributes.collect { new UserTag(userId: userId, key: it.key, value: it.value) })

        return new UserAndUserAttrsHolder(user: user, userAttrs: userAttrs)
    }

    @Profile
    private User loadUserFromLocalDb(String userId) {
        return userRepository.findByUserId(userId.toLowerCase())
    }

    @Transactional(readOnly = true)
    boolean rootAdminExists() {
        return userRoleRepository.existsByRoleName(RoleName.ROLE_SUPER_DUPER_USER)
    }

    @Transactional
    UserRoleRes grantRoot(String userId) {
        User user = userRepository.findByUserId(userId?.toLowerCase())
        if (!user) {
            SkillException exception = new SkillException("User [${userId.toLowerCase()}] does not exist.")
            exception.errorCode = ErrorCode.BadParam
            throw exception
        }
        UserRole role = new UserRole(
                userId: userId,
                roleName: RoleName.ROLE_SUPER_DUPER_USER
        )
        UserRole supervisorRole = new UserRole(
                userId: userId,
                roleName: RoleName.ROLE_SUPERVISOR
        )

        user.roles.add(role)
        user.roles.add(supervisorRole)
        userRepository.save(user)
        return convert(role)
    }

    UserInfoRes loadUserInfo(String userId) {
        UserAttrs userAttrs = userAttrsService.findByUserId(userId)
        new UserInfoRes(userAttrs)
    }

    @Profile
    private User createNewUser(UserInfo userInfo) {
        String userId = userInfo.username?.toLowerCase()
        User user = new User(
                userId: userId,
                password: userInfo.password,
                roles: getRoles(userInfo),
        )
        userRepository.save(user)

        return user
    }

    @Profile
    private void updateUser(UserInfo userInfo, User user) {
        if ( !user.userId?.equalsIgnoreCase(userInfo.username) ||
                (!(user.password == null && userInfo?.password == null) && !user.password?.equalsIgnoreCase(userInfo?.password))) {
            user.userId = userInfo.username?.toLowerCase()
            user.password = userInfo.password
            userRepository.save(user)
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

    private UserRoleRes convert(UserRole inputRole) {
        UserRoleRes res = new UserRoleRes(
                userId: inputRole.userId,
                projectId: inputRole.projectId,
                roleName: inputRole.roleName,
        )
        return res
    }

    private UserRoleRes convert(UserRoleRepo.UserRoleWithAttrs input) {
        UserRoleRes res = new UserRoleRes(
                userId: input.role.userId,
                userIdForDisplay: input.attrs.userIdForDisplay,
                projectId: input.role.projectId,
                roleName: input.role.roleName,
                firstName: input.attrs.firstName,
                lastName: input.attrs.lastName,
                email: input.attrs.email
        )
        return res
    }

    static class UserAndUserAttrsHolder {
        User user
        UserAttrs userAttrs
    }
}
