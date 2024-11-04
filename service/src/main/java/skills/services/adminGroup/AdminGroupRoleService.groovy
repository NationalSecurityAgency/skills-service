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
package skills.services.adminGroup

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.auth.UserNameService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.result.model.ProjectResult
import skills.controller.result.model.UserRoleRes
import skills.services.AccessSettingsStorageService
import skills.services.admin.ProjAdminService
import skills.services.admin.UserCommunityService
import skills.services.quiz.QuizDefService
import skills.services.quiz.QuizRoleService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.AdminGroupDef
import skills.storage.model.ProjDef
import skills.storage.model.QuizDef
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.repos.AdminGroupDefRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserRoleRepo

@Service
@Slf4j
class AdminGroupRoleService {
    private static Set<RoleName> adminGroupSupportedRoles = [RoleName.ROLE_ADMIN_GROUP_OWNER, RoleName.ROLE_ADMIN_GROUP_MEMBER].toSet()

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    UserInfoService userInfoService

    @Autowired
    QuizDefService quizDefService

    @Autowired
    QuizRoleService quizRoleService

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    UserNameService userNameService

    @Autowired
    AdminGroupDefRepo adminGroupDefRepo

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    ProjDefRepo projectDefRepo

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    UserCommunityService userCommunityService

    @Transactional(readOnly = true)
    List<UserRoleRes> getAdminGroupMemberUserRoles(String adminGroupId) {
        AdminGroupDef adminGroupDef = findAdminGroupDef(adminGroupId)
        return accessSettingsStorageService.findAllAdminGroupMembers(adminGroupDef.adminGroupId)
    }

    @Transactional
    void addAdminGroupRole(String userIdParam, String adminGroupId, RoleName roleName) {
        AdminGroupDef adminGroupDef = findAdminGroupDef(adminGroupId)
        ensureValidRole(roleName, adminGroupDef.adminGroupId)
        String userId = userNameService.normalizeUserId(userIdParam)
        String currentUser = userInfoService.getCurrentUserId()
        if (currentUser?.toLowerCase() == userId?.toLowerCase()) {
            throw new SkillException("Cannot add roles to yourself. userId=[${userId}], adminGroupName=[${adminGroupDef.name}]", ErrorCode.AccessDenied)
        }
        Boolean existingMemberOrOwner = userRoleRepo.isUserGroupAdminMemberOrOwner(userId, adminGroupDef.adminGroupId)
        accessSettingsStorageService.addAdminGroupDefUserRoleForUser(userId, adminGroupDef.adminGroupId, roleName)

        if (!existingMemberOrOwner) {
            List<String> quizIds = userRoleRepo.findQuizIdsByAdminGroupId(adminGroupDef.adminGroupId)
            quizIds.each { quizId ->
                quizRoleService.addQuizRole(userId, quizId, RoleName.ROLE_QUIZ_ADMIN, adminGroupDef.adminGroupId)
            }
            List<String> projectIds = userRoleRepo.findProjectIdsByAdminGroupId(adminGroupDef.adminGroupId)
            projectIds.each { projectId ->
                accessSettingsStorageService.addUserRole(userId, projectId, RoleName.ROLE_PROJECT_ADMIN, true, adminGroupDef.adminGroupId)
            }
        }

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)
        String userIdForDisplay = userAttrs?.userIdForDisplay ?: userId
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Create, item: DashboardItem.UserRole,
                itemId: userIdForDisplay,
                actionAttributes: [
                        userRole: roleName,
                        adminGroupName: adminGroupDef.name,
                        adminGroupId: adminGroupDef.adminGroupId,
                ]
        ))
    }

    @Transactional
    void deleteAdminGroupRole(String userIdParam, String adminGroupId, RoleName roleName) {
        AdminGroupDef adminGroupDef = findAdminGroupDef(adminGroupId)
        ensureValidRole(roleName, adminGroupDef.adminGroupId)
        String userId = userNameService.normalizeUserId(userIdParam)
        String currentUser = userInfoService.getCurrentUserId()
        if (currentUser?.toLowerCase() == userId?.toLowerCase()) {
            throw new SkillException("Cannot remove roles from yourself. userId=[${userId}], adminGroupName=[${adminGroupDef.name}]", ErrorCode.AccessDenied)
        }
        accessSettingsStorageService.deleteAdminGroupUserRole(userId, adminGroupDef.adminGroupId, roleName)
        accessSettingsStorageService.deleteProjectAndQuizAdminUserRolesWithAdminGroupIdForUser(userId, adminGroupDef.adminGroupId)

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)
        String userIdForDisplay = userAttrs?.userIdForDisplay ?: userId
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete, item: DashboardItem.UserRole,
                itemId: userIdForDisplay,
                actionAttributes: [
                        userRole: roleName,
                        adminGroupName: adminGroupDef.name,
                        adminGroupId: adminGroupDef.adminGroupId,
                ]
        ))
    }

    @Transactional()
    void addQuizToAdminGroup(String adminGroupId, String quizId) {
        AdminGroupDef adminGroupDef = findAdminGroupDef(adminGroupId)
        QuizDef quizDef = quizDefService.findQuizDef(quizId)
        accessSettingsStorageService.findAllAdminGroupMembers(adminGroupDef.adminGroupId).each { UserRoleRes userRoleRes ->
            accessSettingsStorageService.addQuizDefUserRoleForUser(userRoleRes.userId, quizDef.quizId, RoleName.ROLE_QUIZ_ADMIN, adminGroupId)
        }
    }

    @Transactional()
    void removeQuizFromAdminGroup(String adminGroupId, String quizId) {
        QuizDef quizDef = quizDefService.findQuizDef(quizId)
        removeProjectOrQuizFromAdminGroup(adminGroupId, quizDef.quizId, RoleName.ROLE_QUIZ_ADMIN)
    }

    @Transactional()
    void addProjectToAdminGroup(String adminGroupId, String projectId) {
        AdminGroupDef adminGroupDef = findAdminGroupDef(adminGroupId)
        ProjectResult projectDef = findProjectDef(projectId)

        if (userCommunityService.isUserCommunityOnlyProject(projectId) && !userCommunityService.isUserCommunityOnlyAdminGroup(adminGroupId)) {
            throw new SkillException("Project [${projectDef.name}] is not allowed to be assigned [${adminGroupDef.name}] Admin Group", ErrorCode.AccessDenied)
        }

        accessSettingsStorageService.findAllAdminGroupMembers(adminGroupDef.adminGroupId).each { UserRoleRes userRoleRes ->
            accessSettingsStorageService.addUserRole(userRoleRes.userId, projectDef.projectId, RoleName.ROLE_PROJECT_ADMIN, false, adminGroupId)
        }
    }

    @Transactional()
    void removeProjectFromAdminGroup(String adminGroupId, String quizId) {
        ProjDef projDef = projectDefRepo.findByProjectId(quizId)
        removeProjectOrQuizFromAdminGroup(adminGroupId, projDef.projectId, RoleName.ROLE_PROJECT_ADMIN)
    }

    private void removeProjectOrQuizFromAdminGroup(String adminGroupId, String projectOrQuizId, RoleName roleName) {
        AdminGroupDef adminGroupDef = findAdminGroupDef(adminGroupId)
        String currentUser = userInfoService.getCurrentUserId()
        // remove all members of this admin group, except current user (unless the current user remains an admin from a different group)
        if (roleName == RoleName.ROLE_QUIZ_ADMIN) {
            userRoleRepo.deleteByQuizIdAndAdminGroupIdAndRoleName(projectOrQuizId, adminGroupDef.adminGroupId, RoleName.ROLE_QUIZ_ADMIN)
            if (!userRoleRepo.isUserQuizGroupAdmin(currentUser, projectOrQuizId)) {
                accessSettingsStorageService.addQuizDefUserRoleForUser(currentUser, projectOrQuizId, RoleName.ROLE_QUIZ_ADMIN)
            }
        } else if (roleName == RoleName.ROLE_PROJECT_ADMIN) {
            userRoleRepo.deleteByProjectIdAndAdminGroupIdAndRoleName(projectOrQuizId, adminGroupDef.adminGroupId, RoleName.ROLE_PROJECT_ADMIN)
            if (!userRoleRepo.isUserProjectGroupAdmin(currentUser, projectOrQuizId)) {
                accessSettingsStorageService.addUserRole(currentUser, projectOrQuizId, RoleName.ROLE_PROJECT_ADMIN)
            }
        } else {
            throw new SkillException("Unexpected role [${roleName}] removing project or quiz from admin group, adminGroupName=[${adminGroupDef.name}]", ErrorCode.BadParam)
        }

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(currentUser)
        String userIdForDisplay = userAttrs?.userIdForDisplay ?: currentUser
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Delete, item: DashboardItem.AdminGroup,
                itemId: userIdForDisplay,
                actionAttributes: [
                    adminGroupName: adminGroupDef.name,
                    adminGroupId: adminGroupDef.adminGroupId,
                ]
        ))
    }


    static void ensureValidRole(RoleName roleName, String adminGroupId) {
        if (!adminGroupSupportedRoles.contains(roleName)) {
            throw new SkillException("Provided [${roleName}] is not a admin group role, admin group id [${adminGroupId}].", ErrorCode.BadParam)
        }
    }

    private AdminGroupDef findAdminGroupDef(String adminGroupId) {
        AdminGroupDef adminGroupDef = adminGroupDefRepo.findByAdminGroupIdIgnoreCase(adminGroupId)
        if (!adminGroupDef) {
            throw new SkillException("Failed to find admin group id [${adminGroupId}].", ErrorCode.BadParam)
        }
        return adminGroupDef
    }

    private ProjectResult findProjectDef(String projectId) {
        ProjectResult projectDef = projAdminService.getProject(projectId)
        if (!projectDef) {
            throw new SkillException("Failed to find project with id [${projectId}].", ErrorCode.BadParam)
        }
        return projectDef
    }
}
