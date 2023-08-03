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
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.pki.PkiUserLookup
import skills.controller.UserInfoController
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillQuizException
import skills.controller.result.model.TableResult
import skills.controller.result.model.UserInfoRes
import skills.controller.result.model.UserRoleRes
import skills.services.admin.UserCommunityService
import skills.services.inception.InceptionProjectService
import skills.services.settings.SettingsService
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.User
import skills.storage.model.auth.UserRole
import skills.storage.repos.UserRepo
import skills.storage.repos.UserRoleRepo

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
    UserInfoService userInfoService

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

    @Autowired
    UserCommunityService userCommunityService

    @Value('#{"${skills.config.ui.defaultLandingPage:admin}"}')
    String defaultLandingPage

    @Transactional(readOnly = true)
    List<UserRoleRes> getUserRolesByProjectIdAndRoles(String projectId, List<RoleName> roles) {
        List<UserRoleRepo.UserRoleWithAttrs> res = userRoleRepository.findRoleWithAttrsByProjectIdAndUserRoles(projectId, roles, PageRequest.ofSize(Integer.MAX_VALUE))
        return res.collect { convert(it) }
    }

    @Transactional(readOnly = true)
    TableResult getUserRolesForProjectId(String projectId, List<RoleName> roles, PageRequest pageRequest) {
        TableResult tableResult = new TableResult()
        tableResult.totalCount = userRoleRepository.countUserRolesByProjectIdAndUserRoles(projectId, roles)
        if (tableResult.totalCount > 0) {
            List<UserRoleRepo.UserRoleWithAttrs> res = userRoleRepository.findRoleWithAttrsByProjectIdAndUserRoles(projectId, roles, pageRequest)
            tableResult.data = res.collect { convert(it) }
            tableResult.count = res?.size()
        }
        return tableResult
    }

    List<UserRoleRes> findAllQuizRoles(String quizId) {
        List<UserRoleRepo.UserRoleWithAttrs> rolesFromDB = userRoleRepository.findRoleWithAttrsByQuizId(quizId)
        return rolesFromDB.collect { convert(it)}
    }

    @Transactional(readOnly = true)
    TableResult getUserRolesForProjectId(String projectId, String roleName, String userIdQuery, PageRequest pagingRequest) {
        //account for userId query
        RoleName rn = RoleName.valueOf(roleName)
        TableResult tableResult = new TableResult()
        tableResult.totalCount = userRoleRepository.countRoleWithAttrsByProjectIdAndRoleNameAndUserIdLike(projectId, rn, userIdQuery)
        if (tableResult.totalCount > 0) {
            List<UserRoleRepo.UserRoleWithAttrs> res = userRoleRepository.findRoleWithAttrsByProjectIdAndRoleNameAndUserIdLike(projectId, rn, userIdQuery, pagingRequest)
            tableResult.data = res.collect { convert(it) }
            tableResult.count = res?.size()
        }
        return tableResult
    }

    @Transactional(readOnly = true)
    List<UserRoleRes> getUserRolesForProjectIdAndUserId(String projectId, String userId) {
        List<UserRoleRepo.UserRoleWithAttrs> res = userRoleRepository.findAllByProjectIdAndUserId(projectId, userId)
        return res.collect { convert(it) }
        return res
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
    TableResult getUserRolesWithRole(RoleName roleName, PageRequest pageRequest) {
        TableResult tableResult = new TableResult()
        tableResult.totalCount = userRoleRepository.countAllByRoleName(roleName)
        if (tableResult.totalCount > 0) {
            List<UserRoleRepo.UserRoleWithAttrs> roles = userRoleRepository.findAllByRoleNameWithPaging(roleName, pageRequest)
            tableResult.data = roles.collect { convert(it) }
            tableResult.count = roles?.size()
        }
        return tableResult
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
        UserRole supervisorRole = userRoleRepository.findByUserIdAndRoleNameAndProjectId(userId, RoleName.ROLE_SUPERVISOR, null)
        if (!supervisorRole) {
            addUserRoleInternal(userId, null, RoleName.ROLE_SUPERVISOR)
        }
        inceptionProjectService.createInceptionAndAssignUser(userId)
        return convert(userRole)
    }

    @Transactional
    void deleteRoot(String userId) {
        userId = userId?.toLowerCase()
        deleteUserRoleInternal(userId, null, RoleName.ROLE_SUPER_DUPER_USER)

        UserRole supervisorRole = userRoleRepository.findByUserIdAndRoleNameAndProjectId(userId, RoleName.ROLE_SUPERVISOR, null)
        if (supervisorRole) {
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
        UserRole userRole = userRoleRepository.findByUserIdAndRoleNameAndProjectId(userId, roleName, projectId)
        assert userRole, "DELETE FAILED -> no user-role with project id [$projectId], userId [$userId] and roleName [$roleName]"

        userRoleRepository.delete(userRole)
        log.debug("Deleted userRole [{}]", userRole)
    }

    void deleteQuizUserRole(String userId, String quizId, RoleName roleName) {
        log.debug('Deleting user-role for userId [{}] and role [{}] on quiz [{}]', userId, roleName, quizId)
        UserRole userRole = userRoleRepository.findByUserIdAndRoleNameAndQuizId(userId, roleName, quizId)
        assert userRole, "DELETE FAILED -> no user-role with quiz id [$quizId], userId [$userId] and roleName [$roleName]"

        userRoleRepository.delete(userRole)
        log.debug("Deleted userRole [{}]", userRole)
    }

    @Transactional()
    UserRoleRes addUserRole(String userId, String projectId, RoleName roleName) {
        UserRole role = addUserRoleInternal(userId, projectId, roleName)
        return convert(role)
    }

    @Transactional()
    UserRole addUserRoleReturnRaw(String userId, String projectId, RoleName roleName) {
        UserRole role = addUserRoleInternal(userId, projectId, roleName)
        return role
    }

    private static List<List<RoleName>> mutuallyExclusiveRoles = [
            [RoleName.ROLE_PROJECT_APPROVER, RoleName.ROLE_PROJECT_ADMIN],
    ]
    private UserRole addUserRoleInternal(String userId, String projectId, RoleName roleName) {
        log.debug('Creating user-role for ID [{}] and role [{}] on project [{}]', userId, roleName, projectId)
        String userIdLower = userId?.toLowerCase()
        User user = userRepository.findByUserId(userIdLower)
        if (user) {
            // check that the new user role does not already exist
            UserRole existingUserRole = userRoleRepository.findByUserIdAndRoleNameAndProjectId(userId, roleName, projectId)
            assert !existingUserRole, "CREATE FAILED -> user-role with project id [$projectId], userIdLower [$userIdLower] and roleName [$roleName] already exists"
        } else {
            throw new SkillException("User [$userIdLower]  does not exist", (String) projectId ?: SkillException.NA, SkillException.NA, ErrorCode.UserNotFound)
        }

        if (projectId && !userCommunityService.isUserCommunityMember(userIdLower) && userCommunityService.isUserCommunityOnlyProject(projectId)) {
            String userIdForDisplay = loadUserInfo(userId)?.userIdForDisplay
            throw new SkillException("User [${userIdForDisplay}] is not allowed to be assigned [${roleName?.displayName}] user role", projectId, null, ErrorCode.AccessDenied)
        }

        // remove mutually exclusive roles
        mutuallyExclusiveRoles.each {List<RoleName> roles ->
            if (roles.contains(roleName)) {
                List<RoleName> rolesToRemove = roles.findAll { it != roleName}
                if (rolesToRemove) {
                    rolesToRemove.each { RoleName roleToRemove ->
                        UserRole found = userRoleRepository.findByUserIdAndRoleNameAndProjectId(userId, roleToRemove, projectId)
                        if (found) {
                            log.info("Removing [{}] role for userId=[{}] and projectId=[{}]", found.roleName, userId, projectId)
                            userRoleRepository.delete(found)
                        }
                    }
                }
            }
        }

        UserRole userRole = new UserRole(userRefId: user.id, userId: userIdLower, roleName: roleName, projectId: projectId)
        userRoleRepository.save(userRole)
        log.debug("Created userRole [{}]", userRole)

        log.debug("setting sort order for user [{}] on project [{}]", userIdLower, projectId)
        sortingService.setNewProjectDisplayOrder(projectId, userIdLower)
        return userRole
    }

    UserRole addQuizDefUserRole(String userId, String quizId, RoleName roleName) {
        log.debug('Creating quiz id user-role for ID [{}] and role [{}] on quiz [{}]', userId, roleName, quizId)
        String userIdLower = userId?.toLowerCase()
        User user = userRepository.findByUserId(userIdLower)
        if (user) {
            // check that the new user role does not already exist
            UserRole existingUserRole = userRoleRepository.findByUserIdAndRoleNameAndQuizId(userId, roleName, quizId)
            if (existingUserRole) {
                throw new SkillQuizException("CREATE FAILED -> user-role with quiz id [$quizId], userIdLower [$userIdLower] and roleName [$roleName] already exists", quizId, ErrorCode.BadParam)
            }
        } else {
            throw new SkillQuizException("User [$userIdLower] does not exist", (String) quizId ?: SkillException.NA, ErrorCode.UserNotFound)
        }

        UserRole userRole = new UserRole(userRefId: user.id, userId: userIdLower, roleName: roleName, quizId: quizId)
        userRoleRepository.save(userRole)
        log.debug("Created userRole [{}]", userRole)

//        log.debug("setting sort order for user [{}] on project [{}]", userIdLower, projectId)
//        sortingService.setNewProjectDisplayOrder(projectId, userIdLower)
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
        return new UserAndUserAttrsHolder(user: user, userAttrs: userAttrs)
    }

    /**
     * Retreives the existing User and UserAttrs or creates a new one if they do not
     * already exist. This method DOES NOT update any attributes on existing records
     * @param userInfo
     * @return
     */
    @Transactional()
    @Profile
    UserAndUserAttrsHolder getOrCreate(UserInfo userInfo) {
        userInfoValidator.validate(userInfo)
        String userId = userInfo.username?.toLowerCase()
        UserAttrs userAttrs = userAttrsService.getOrCreate(userId, userInfo)

        User user = loadUserFromLocalDb(userId)

        if (user) {
            new UserAndUserAttrsHolder(user: user, userAttrs: userAttrs)
        } else {
            // create new user with APP_USER role
            log.debug("Creating new app user for ID [{}], DN [{}]", userInfo.username, userInfo.userDn)
            userAttrsService.lockUser(userId)
            user = loadUserFromLocalDb(userId)
            if (!user) {
                user = createNewUser(userInfo)
                log.debug("Created new user for ID [{}], DN [{}]", userInfo.username, userInfo.userDn)
            } else {
                log.debug("Was going to create but already exist for ID [{}], DN [{}]", userInfo.username, userInfo.userDn)
            }
            return new UserAndUserAttrsHolder(user: user, userAttrs: userAttrs)
        }
    }

    @Transactional(readOnly=true)
    @Profile
    UserAndUserAttrsHolder get(UserInfo userInfo) {
        userInfoValidator.validate(userInfo)
        String userId = userInfo.username?.toLowerCase()
        UserAttrs userAttrs = userAttrsService.get(userId, userInfo)

        User user = loadUserFromLocalDb(userId)

        if (user) {
            return new UserAndUserAttrsHolder(user: user, userAttrs: userAttrs)
        }
        return null
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
                userRefId: user.id,
                userId: userId,
                roleName: RoleName.ROLE_SUPER_DUPER_USER
        )
        UserRole supervisorRole = new UserRole(
                userRefId: user.id,
                userId: userId,
                roleName: RoleName.ROLE_SUPERVISOR
        )

        userRoleRepository.saveAll([role, supervisorRole])
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
        )
        userRepository.save(user)

        User savedUser = userRepository.findByUserId(user.userId)
        List<UserRole> roles = getRoles(userInfo)
        roles.each {it.userRefId = savedUser.id }
        userRoleRepository.saveAll(roles)

        return user
    }

    @Profile
    private void updateUser(UserInfo userInfo, User user) {
        if ( !user.userId?.equalsIgnoreCase(userInfo.username) ||
                (!(user.password == null && userInfo?.password == null) && !user.password?.equalsIgnoreCase(userInfo?.password))) {
            user.userId = userInfo.username?.toLowerCase()
            //don't overwrite an existing password with blank
            user.password = StringUtils.defaultIfBlank(userInfo.password, user.password)
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
                email: input.attrs.email,
                dn: input.attrs.dn,
        )
        return res
    }

    static class UserAndUserAttrsHolder {
        User user
        UserAttrs userAttrs
    }
}
