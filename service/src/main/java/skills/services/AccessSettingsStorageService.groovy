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
import skills.services.admin.ProjAdminService
import skills.services.admin.UserCommunityService
import skills.services.inception.InceptionProjectService
import skills.services.settings.SettingsService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.User
import skills.storage.model.auth.UserRole
import skills.storage.repos.UserAttrsRepo
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

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    UserAttrsRepo userAttrsRepo

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

    @Transactional(readOnly = true)
    Integer countUserRolesForProjectId(String projectId, List<RoleName> roles) {
        return userRoleRepository.countUserRolesByProjectIdAndUserRoles(projectId, roles)
    }

    List<UserRoleRes> findAllGlobalBadgeAdminRoles(String quizId) {
        List<UserRoleRepo.UserRoleWithAttrs> rolesFromDB = userRoleRepository.findRoleWithAttrsByGlobalBadgeId(quizId)
        return rolesFromDB.collect { convert(it)}
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
        inceptionProjectService.createInceptionAndAssignUser(userId)

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId.toLowerCase())
        String userIdForDisplay = userAttrs?.userIdForDisplay ?: userId
        saveUserRoleActions(userId, RoleName.ROLE_SUPER_DUPER_USER, DashboardAction.Create)
        return convert(userRole)
    }

    @Transactional
    void deleteRoot(String userId) {
        userId = userId?.toLowerCase()
        deleteUserRoleInternal(userId, null, RoleName.ROLE_SUPER_DUPER_USER)

        inceptionProjectService.removeUser(userId)

        saveUserRoleActions(userId, RoleName.ROLE_SUPER_DUPER_USER, DashboardAction.Delete)
    }

    @Transactional()
    void deleteGlobalBadgeUserRoles(String badgeId) {
        userRoleRepository.deleteByGlobalBadgeIdAndRoleName(badgeId, RoleName.ROLE_GLOBAL_BADGE_ADMIN)
    }

    @Profile
    void saveUserRoleActions(String userId, RoleName roleName, DashboardAction action) {
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId.toLowerCase())
        String userIdForDisplay = userAttrs?.userIdForDisplay ?: userId
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: action, item: DashboardItem.UserRole,
                actionAttributes: [
                        userId  : userIdForDisplay,
                        userRole: roleName
                ],
                itemId: userIdForDisplay,
        ))
    }

    @Transactional()
    void deleteUserRole(String userId, String projectId, RoleName roleName, boolean saveUserAction = false) {
        userId = userId?.toLowerCase()
        boolean isPrivateProjRole = roleName == RoleName.ROLE_PRIVATE_PROJECT_USER
        if (userId != userInfoService.getCurrentUser().username.toLowerCase() || isPrivateProjRole) {
            deleteUserRoleInternal(userId, projectId, roleName)
            if (isPrivateProjRole) {
                additionalCleanupForPrivateProjectUserRole(userId, projectId)
            }
            if (saveUserAction) {
                saveUserRoleActions(userId, roleName, DashboardAction.Delete)
            }
        } else {
            throw new SkillException("You cannot delete yourself.")
        }
    }

    private void additionalCleanupForPrivateProjectUserRole(String userId, String projectId) {
        settingsService.deleteUserProjectSetting(
                userId,
                ProjAdminService.myProjectGroup,
                ProjAdminService.myProjectSetting,
                projectId
        )
    }

    @Transactional(readOnly = true)
    int getRootAdminCount() {
        return userRoleRepository.countByRoleName(RoleName.ROLE_SUPER_DUPER_USER)
    }

    private void deleteUserRoleInternal(String userId, String projectId, RoleName roleName, String adminGroupId = null) {
        log.debug('Deleting user-role for userId [{}] and role [{}] on project [{}]', userId, roleName, projectId)
        UserRole userRole = userRoleRepository.findByUserIdAndRoleNameAndProjectIdAndAdminGroupId(userId, roleName, projectId, adminGroupId)
        assert userRole, "DELETE FAILED -> no user-role with project id [$projectId], userId [$userId] and roleName [$roleName]"

        userRoleRepository.delete(userRole)
        log.debug("Deleted userRole [{}]", userRole)
    }

    void deleteQuizUserRole(String userId, String quizId, RoleName roleName, String adminGroupId = null) {
        log.debug('Deleting user-role for userId [{}] and role [{}] on quiz [{}]', userId, roleName, quizId)
        UserRole userRole = userRoleRepository.findByUserIdAndRoleNameAndQuizIdAndAdminGroupId(userId, roleName, quizId, adminGroupId)
        assert userRole, "DELETE FAILED -> no user-role with quiz id [$quizId], userId [$userId] and roleName [$roleName]"

        userRoleRepository.delete(userRole)
        log.debug("Deleted userRole [{}]", userRole)
    }

    void deleteGlobalBadgeAdminUserRole(String userId, String badgeId, RoleName roleName, String adminGroupId = null) {
        log.debug('Deleting user-role for userId [{}] and role [{}] on global badge [{}]', userId, roleName, badgeId)
        UserRole userRole = userRoleRepository.findByUserIdAndRoleNameAndQuizIdAndAdminGroupId(userId, roleName, badgeId, adminGroupId)
        assert userRole, "DELETE FAILED -> no user-role with global badge id [$badgeId], userId [$userId] and roleName [$roleName]"

        userRoleRepository.delete(userRole)
        log.debug("Deleted userRole [{}]", userRole)
    }

    @Transactional()
    UserRoleRes addUserRole(String userId, String projectId, RoleName roleName, boolean saveRoleAction = false, String adminGroupId = null) {
        UserRole role = addUserRoleInternal(userId, projectId, roleName, adminGroupId)
        if (saveRoleAction) {
            saveUserRoleActions(userId, roleName, DashboardAction.Create)
        }
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
    private UserRole addUserRoleInternal(String userId, String projectId, RoleName roleName, String adminGroupId = null) {
        log.debug('Creating user-role for ID [{}] and role [{}] on project [{}]', userId, roleName, projectId)
        String userIdLower = userId?.toLowerCase()
        User user = userRepository.findByUserId(userIdLower)
        if (user) {
            UserRole userRole = new UserRole(userRefId: user.id, userId: userIdLower, roleName: roleName, projectId: projectId, adminGroupId: adminGroupId)
            // check that the new user role does not already exist
            UserRole existingUserRole = userRoleRepository.findByUserIdAndRoleNameAndProjectIdAndAdminGroupId(userId, roleName, projectId, adminGroupId)
            assert !existingUserRole, "CREATE FAILED -> user-role with project id [$projectId], userIdLower [$userIdLower] and roleName [$roleName], admin group id [${adminGroupId}] already exists"

            String userIdForDisplay = loadUserInfo(userId)?.userIdForDisplay
            if (projectId && !userCommunityService.isUserCommunityMember(userIdLower) && userCommunityService.isUserCommunityOnlyProject(projectId)) {
                throw new SkillException("User [${userIdForDisplay}] is not allowed to be assigned [${roleName?.displayName}] user role", projectId, null, ErrorCode.AccessDenied)
            }

            // if assigning ROLE_PROJECT_APPROVER, need to make sure they are not already a group admin, and if so not allow it.  "local" admin to approver is okay
            if (adminGroupId && roleName == RoleName.ROLE_PROJECT_APPROVER && userRoleRepository.isUserProjectGroupAdmin(userId, projectId)) {
                throw new SkillException("User [${userIdForDisplay}] is already assigned as a group admin and must first be removed from all groups", projectId, null, ErrorCode.AccessDenied)
            }
            if (adminGroupId && roleName == RoleName.ROLE_PROJECT_ADMIN) {
                // need to check if the role already exists outside of the admin group (ie, local project admin)
                existingUserRole = userRoleRepository.findByUserIdAndRoleNameAndProjectIdAndAdminGroupId(userId, roleName, projectId, null)
                if (existingUserRole) {
                    log.debug("Assigning admin group [{}] to existing local project admin [{}]", adminGroupId, userIdLower)
                    existingUserRole.adminGroupId = adminGroupId
                    userRole = existingUserRole
                }
            }

            // remove mutually exclusive roles (non-group)
            mutuallyExclusiveRoles.each {List<RoleName> roles ->
                if (roles.contains(roleName)) {
                    List<RoleName> rolesToRemove = roles.findAll { it != roleName}
                    if (rolesToRemove) {
                        rolesToRemove.each { RoleName roleToRemove ->
                            UserRole found = userRoleRepository.findByUserIdAndRoleNameAndProjectIdAndAdminGroupId(userId, roleToRemove, projectId, null)
                            if (found) {
                                log.info("Removing [{}] role for userId=[{}] and projectId=[{}]", found.roleName, userId, projectId)
                                userRoleRepository.delete(found)
                            }
                        }
                    }
                }
            }

            userRoleRepository.save(userRole)
            log.debug("Created userRole [{}]", userRole)

            log.debug("setting sort order for user [{}] on project [{}]", userIdLower, projectId)
            sortingService.setNewProjectDisplayOrder(projectId, userIdLower)
            return userRole
        } else {
            throw new SkillException("User [$userIdLower]  does not exist", (String) projectId ?: SkillException.NA, SkillException.NA, ErrorCode.UserNotFound)
        }
    }

    UserRole addQuizDefUserRoleForUser(String userId, String quizId, RoleName roleName, String adminGroupId = null) {
        log.debug('Creating quiz id user-role for ID [{}] and role [{}] on quiz [{}]', userId, roleName, quizId, adminGroupId)
        String userIdLower = userId?.toLowerCase()
        User user = userRepository.findByUserId(userIdLower)
        if (user) {
            if (!userCommunityService.isUserCommunityMember(userIdLower) && userCommunityService.isUserCommunityOnlyQuiz(quizId)) {
                String userIdForDisplay = loadUserInfo(userId)?.userIdForDisplay ?: userId
                throw new SkillQuizException("User [${userIdForDisplay}] is not allowed to be assigned [${roleName?.displayName}] user role", quizId, ErrorCode.AccessDenied)
            }

            UserRole userRole = new UserRole(userRefId: user.id, userId: userIdLower, roleName: roleName, quizId: quizId, adminGroupId: adminGroupId)
            // check that the new user role does not already exist
            UserRole existingUserRole = userRoleRepository.findByUserIdAndRoleNameAndQuizIdAndAdminGroupId(userId, roleName, quizId, adminGroupId)
            if (existingUserRole) {
                throw new SkillQuizException("CREATE FAILED -> user-role with quiz id [$quizId], userIdLower [$userIdLower] and roleName [$roleName], adming group id [${adminGroupId}] already exists", quizId, ErrorCode.BadParam)
            }
            if (adminGroupId && roleName == RoleName.ROLE_QUIZ_ADMIN) {
                // need to check if the role already exists outside of the admin group (ie, local quiz admin)
                existingUserRole = userRoleRepository.findByUserIdAndRoleNameAndQuizIdAndAdminGroupId(userId, roleName, quizId, null)
                if (existingUserRole) {
                    log.debug("Assigning admin group [{}] to existing local quiz admin [{}]", adminGroupId, userIdLower)
                    existingUserRole.adminGroupId = adminGroupId
                    userRole = existingUserRole
                }
            }
            userRoleRepository.save(userRole)
            log.debug("Created userRole [{}]", userRole)

//        log.debug("setting sort order for user [{}] on project [{}]", userIdLower, projectId)
//        sortingService.setNewProjectDisplayOrder(projectId, userIdLower)
            return userRole
        } else {
            throw new SkillQuizException("User [$userIdLower] does not exist", (String) quizId ?: SkillException.NA, ErrorCode.UserNotFound)
        }
    }

    UserRole addGlobalAdminUserRoleForUser(String userId, String globalBadgeId, RoleName roleName, String adminGroupId = null) {
        log.debug('Creating quiz id user-role for ID [{}] and role [{}] on quiz [{}]', userId, roleName, globalBadgeId, adminGroupId)
        String userIdLower = userId?.toLowerCase()
        User user = userRepository.findByUserId(userIdLower)
        if (user) {
            UserRole userRole = new UserRole(userRefId: user.id, userId: userIdLower, roleName: roleName, globalBadgeId: globalBadgeId, adminGroupId: adminGroupId)
            // check that the new user role does not already exist
            UserRole existingUserRole = userRoleRepository.findByUserIdAndRoleNameAndGlobalBadgeIdAndAdminGroupId(userId, roleName, globalBadgeId, adminGroupId)
            if (existingUserRole) {
                throw new SkillException("CREATE FAILED -> user-role with global badge id [$globalBadgeId], userIdLower [$userIdLower] roleName [$roleName] and admin group id [${adminGroupId}] already exists", ErrorCode.BadParam)
            }
            if (adminGroupId && roleName == RoleName.ROLE_QUIZ_ADMIN) {
                // need to check if the role already exists outside of the admin group (ie, local global badge admin)
                existingUserRole = userRoleRepository.findByUserIdAndRoleNameAndGlobalBadgeIdAndAdminGroupId(userId, roleName, globalBadgeId, null)
                if (existingUserRole) {
                    log.debug("Assigning admin group [{}] to existing local global badge admin [{}]", adminGroupId, userIdLower)
                    existingUserRole.adminGroupId = adminGroupId
                    userRole = existingUserRole
                }
            }
            userRoleRepository.save(userRole)
            log.debug("Created userRole [{}]", userRole)
            return userRole
        } else {
            throw new SkillException("User [$userIdLower] does not exist", ErrorCode.UserNotFound)
        }
    }

    UserRole addAdminGroupDefUserRoleForUser(String userId, String adminGroupId, RoleName roleName) {
        log.debug('Creating admin group user-role for ID [{}] and role [{}] on admin group [{}]', userId, roleName, adminGroupId)
        String userIdLower = userId?.toLowerCase()
        User user = userRepository.findByUserId(userIdLower)
        if (user) {
            UserRole userRole = new UserRole(userRefId: user.id, userId: userIdLower, roleName: roleName, adminGroupId: adminGroupId)
            // check that the new user role does not already exist
            UserRole existingUserRole = userRoleRepository.findByUserIdAndRoleNameAndAdminGroupId(userId, roleName, adminGroupId)
            if (existingUserRole) {
                throw new SkillException("CREATE FAILED -> user-role with admin group id [$adminGroupId], userIdLower [$userIdLower] and roleName [$roleName] already exists", ErrorCode.BadParam)
            }

            if (!userCommunityService.isUserCommunityMember(userIdLower) && userCommunityService.isUserCommunityOnlyAdminGroup(adminGroupId)) {
                String userIdForDisplay = loadUserInfo(userId)?.userIdForDisplay
                throw new SkillException("User [${userIdForDisplay}] is not allowed to be assigned [${roleName?.displayName}] user role for admin group [${adminGroupId}]", ErrorCode.AccessDenied)
            }

            if (roleName == RoleName.ROLE_ADMIN_GROUP_OWNER) {
                // check if the user is already a group member
                existingUserRole = userRoleRepository.findByUserIdAndRoleNameAndAdminGroupId(userId, RoleName.ROLE_ADMIN_GROUP_MEMBER, adminGroupId)
                if (existingUserRole) {
                    log.debug("Changing existing group member [{}] to group owner", userIdLower)
                    existingUserRole.roleName = RoleName.ROLE_ADMIN_GROUP_OWNER
                    userRole = existingUserRole
                }
            } else if (roleName == RoleName.ROLE_ADMIN_GROUP_MEMBER) {
                existingUserRole = userRoleRepository.findByUserIdAndRoleNameAndAdminGroupId(userId, RoleName.ROLE_ADMIN_GROUP_OWNER, adminGroupId)
                if (existingUserRole) {
//                    throw new SkillException("CREATE FAILED -> user-role with admin group id [$adminGroupId], userIdLower [$userIdLower] and roleName [$roleName] is already a group admin", ErrorCode.BadParam)
                    log.debug("Changing existing group owner [{}] to group member", userIdLower)
                    existingUserRole.roleName = RoleName.ROLE_ADMIN_GROUP_MEMBER
                    userRole = existingUserRole
                }
            } else {
                throw new SkillException("Provided [${roleName}] is not a admin group role, admin group id [${adminGroupId}].", ErrorCode.BadParam)
            }
            userRoleRepository.save(userRole)
            log.debug("Created userRole [{}]", userRole)
            return userRole
        } else {
            throw new SkillException("User [$userIdLower] does not exist", ErrorCode.UserNotFound)
        }
    }

    void deleteAdminGroupUserRole(String userId, String adminGroupId, RoleName roleName) {
        log.debug('Deleting user-role for userId [{}] and role [{}] in admin group [{}]', userId, roleName, adminGroupId)
        UserRole userRole = userRoleRepository.findByUserIdAndRoleNameAndAdminGroupId(userId, roleName, adminGroupId)
        assert userRole, "DELETE FAILED -> no user-role with admin group id [$adminGroupId], userId [$userId] and roleName [$roleName]"

        userRoleRepository.delete(userRole)
        log.debug("Deleted userRole [{}]", userRole)
    }

    void deleteProjectAndQuizAdminUserRolesWithAdminGroupIdForUser(String userId, String adminGroupId) {
        log.debug('Deleting project and quiz admin user-role for userId [{}] in admin group [{}]', userId, adminGroupId)
        userRoleRepository.deleteByUserIdAndAdminGroupIdAndRoleNameIn(userId, adminGroupId, [RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_QUIZ_ADMIN])
    }

    List<UserRoleRes> findAllAdminGroupMembers(String adminGroupId) {
        List<UserRoleRepo.UserRoleWithAttrs> rolesFromDB = userRoleRepository.findRoleWithAttrsByAdminGroupIdAndRoleNameIn(adminGroupId, [RoleName.ROLE_ADMIN_GROUP_OWNER, RoleName.ROLE_ADMIN_GROUP_MEMBER])
        return rolesFromDB.collect { convert(it)}
    }

    @Transactional(readOnly = true)
    TableResult getUserRolesForAdminGroupId(String adminGroupId, List<RoleName> roles, PageRequest pageRequest) {
        TableResult tableResult = new TableResult()
        tableResult.totalCount = userRoleRepository.countUserRolesByAdminGroupIdAndUserRoles(adminGroupId, roles)
        if (tableResult.totalCount > 0) {
            List<UserRoleRepo.UserRoleWithAttrs> res = userRoleRepository.findRoleWithAttrsByAdminGroupIdAndUserRoles(adminGroupId, roles, pageRequest)
            tableResult.data = res.collect { convert(it) }
            tableResult.count = res?.size()
        }
        return tableResult
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

        userRoleRepository.saveAll([role])
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
                adminGroupId: inputRole.adminGroupId,
        )
        return res
    }

    private UserRoleRes convert(UserRoleRepo.UserRoleWithAttrs input) {
        UserRoleRes res = new UserRoleRes(
                userId: input.role.userId,
                userIdForDisplay: input.attrs.userIdForDisplay,
                projectId: input.role.projectId,
                adminGroupId: input.role.adminGroupId,
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
