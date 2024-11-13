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
package skills.controller

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.web.bind.annotation.*
import skills.UIConfigProperties
import skills.auth.UserInfoService
import skills.auth.UserNameService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.result.model.RequestResult
import skills.controller.result.model.TableResult
import skills.controller.result.model.UserRoleRes
import skills.services.AccessSettingsStorageService
import skills.services.ContactUsersService
import skills.services.FeatureService
import skills.services.SkillApprovalService
import skills.services.admin.InviteOnlyProjectService
import skills.services.admin.ProjAdminService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.ProjDef
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.User
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserRepo
import skills.storage.repos.UserRoleRepo

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

@RestController
@RequestMapping("/admin")
@Slf4j
@skills.profile.EnableCallStackProf
class AccessSettingsController {

    private static List<RoleName> projectSupportedRoles = [RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER]

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserDetailsService userDetailsService

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    SkillApprovalService skillApprovalService

    @Autowired
    UserRepo userRepo

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    ContactUsersService contactUsersService

    @Autowired
    FeatureService featureService

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    @Autowired
    UserNameService userNameService

    @Autowired
    UIConfigProperties uiConfigProperties

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    UserRoleRepo userRoleRepo

    @Value('#{securityConfig.authMode}}')
    skills.auth.AuthMode authMode = skills.auth.AuthMode.DEFAULT_AUTH_MODE

    @RequestMapping(value = "/projects/{projectId}/userRoles", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    TableResult getProjectUserRoles(
            @PathVariable("projectId") String projectId,
            @RequestParam List<RoleName> roles,
            @RequestParam int limit,
            @RequestParam int page,
            @RequestParam String orderBy,
            @RequestParam Boolean ascending) {
        PageRequest pageRequest = createPagingRequestWithValidation(projectId, limit, page, orderBy, ascending)
        return accessSettingsStorageService.getUserRolesForProjectId(projectId, roles, pageRequest)
    }

    @RequestMapping(value = "/admin-group-definitions/{adminGroupId}/userRoles", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    TableResult getAdminGroupUserRoles(
            @PathVariable("adminGroupId") String adminGroupId,
            @RequestParam List<RoleName> roles,
            @RequestParam int limit,
            @RequestParam int page,
            @RequestParam String orderBy,
            @RequestParam Boolean ascending) {
        PageRequest pageRequest = createPagingRequestWithValidation(adminGroupId, limit, page, orderBy, ascending, 'Admin Group')
        return accessSettingsStorageService.getUserRolesForAdminGroupId(adminGroupId, roles, pageRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/countUserRoles", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    Integer countProjectUserRoles(
            @PathVariable("projectId") String projectId,
            @RequestParam List<RoleName> roles) {
        return accessSettingsStorageService.countUserRolesForProjectId(projectId, roles)
    }

    @RequestMapping(value = "/projects/{projectId}/userRoles/{roleName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    TableResult getProjectUserRolesWithPaging(@PathVariable("projectId") String projectId, @PathVariable("roleName") String roleName,
                                              @RequestParam int limit,
                                              @RequestParam int page,
                                              @RequestParam String orderBy,
                                              @RequestParam Boolean ascending,
                                            @RequestParam(required = false, defaultValue = "") String query) {
        SkillsValidator.isNotBlank(roleName, "Role Name")
        PageRequest pageRequest = createPagingRequestWithValidation(projectId, limit, page, orderBy, ascending)
        return accessSettingsStorageService.getUserRolesForProjectId(projectId, roleName, query, pageRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/users/{userId}/roles", method = RequestMethod.GET)
    List<UserRoleRes> getUserRoles(
            @PathVariable("projectId") String projectId,
            @PathVariable("userId") String userId) {
        String uid = userNameService.normalizeUserId(userId)
        accessSettingsStorageService.getUserRolesForProjectIdAndUserId(projectId, uid?.toLowerCase())
    }

    @RequestMapping(value = "/projects/{projectId}/users/{userId}/roles/{roleName}", method = RequestMethod.DELETE)
    RequestResult deleteUserRole(
            @PathVariable("projectId") String projectId,
            @PathVariable("userId") String userId, @PathVariable("roleName") RoleName roleName) {
        String currentUser = userInfoService.getCurrentUserId()
        String userIdLower = userId?.toLowerCase()
        if (currentUser?.toLowerCase() == userIdLower && roleName != RoleName.ROLE_PRIVATE_PROJECT_USER) {
            throw new SkillException("Cannot delete roles for myself. userId=[${userIdLower}]", projectId, null, ErrorCode.AccessDenied)
        }

        accessSettingsStorageService.deleteUserRole(userIdLower, projectId, roleName)

        if(roleName == RoleName.ROLE_PROJECT_ADMIN && accessSettingsStorageService.isRoot(userIdLower)) {
            User user = userRepo.findByUserId(userIdLower)
            if (!user) {
                throw new SkillException("Failed to find user with id [${userIdLower}]")
            }
            projAdminService.unpinProjectForRootUser(projectId, user)
        }

        skillApprovalService.deleteApproverForProject(projectId, userIdLower)

        saveUserAction(userId, roleName, projectId, DashboardAction.Delete)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/users/{userKey}/roles/{roleName}", method = [RequestMethod.PUT, RequestMethod.POST])
    RequestResult addUserRole(
            @PathVariable("projectId") String projectId,
            @PathVariable("userKey") String userKey, @PathVariable("roleName") RoleName roleName) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(userKey, "User Id")
        if (!projectSupportedRoles.contains(roleName)){
            throw new SkillException("Provided [${roleName}] is not a project role.", projectId, null, ErrorCode.BadParam)
        }
        String userId = userNameService.normalizeUserId(userKey)
        String currentUser = userInfoService.getCurrentUserId()
        if (currentUser?.toLowerCase() == userId?.toLowerCase()) {
            throw new SkillException("Cannot add roles to myself. userId=[${userId}]", projectId, null, ErrorCode.AccessDenied)
        }
        if (userRoleRepo.isUserProjectGroupAdmin(userId, projectId)) {
            throw new SkillException("User is already part of an Admin Group and cannot be added as a local admin. userId=[${userId}]", projectId, null, ErrorCode.AccessDenied)
        }
        accessSettingsStorageService.addUserRole(userId, projectId, roleName)

        handleNewRoleEmail(roleName, projectId, userId)

        if(roleName == RoleName.ROLE_PROJECT_ADMIN && accessSettingsStorageService.isRoot(userId)) {
            User user = userRepo.findByUserId(userId.toLowerCase())
            if (!user) {
                throw new SkillException("Failed to find user with id [${userId.toLowerCase()}]")
            }
            projAdminService.pinProjectForRootUser(projectId, user)
        }

        saveUserAction(userId, roleName, projectId, DashboardAction.Create)
        return new RequestResult(success: true)
    }

    @Profile
    private void saveUserAction(String userId, RoleName roleName, String projectId, DashboardAction action) {
        UserAttrs removeUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId.toLowerCase())
        String removeUserId = removeUserAttrs?.userIdForDisplay ?: userId
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: action,
                item: DashboardItem.UserRole,
                actionAttributes: [
                        userId  : removeUserId,
                        userRole: roleName
                ],
                itemId: removeUserId,
                projectId: projectId,
        ))
    }

    @Profile
    private void handleNewRoleEmail(RoleName roleName, String projectId, String userId) {
        boolean willEmail = roleName == RoleName.ROLE_PROJECT_ADMIN || roleName == RoleName.ROLE_PROJECT_APPROVER
        if (willEmail) {
            ProjDef project = projDefAccessor.getProjDef(projectId)
            String publicUrl = featureService.getPublicUrl()
            if (roleName == RoleName.ROLE_PROJECT_ADMIN) {
                def emailBody = "Congratulations!  You've just been added as a Project Administrator for the SkillTree project [${project.name}](${publicUrl}administrator/projects/${project.projectId}).\n\n" +
                        "The Project administrator role enables management of the training profile for this project such as creating and " +
                        "modifying subjects, skills and badges.  Thank you for being part of the SkillTree Community!\n\n" +
                        "Always yours,\n\n" +
                        "-SkillTree Bot"
                contactUsersService.sendEmail("SkillTree - You've been added as an admin", emailBody, userId, null, uiConfigProperties.ui.defaultCommunityDescriptor)
            } else if (roleName == RoleName.ROLE_PROJECT_APPROVER) {
                def emailBody = "Congratulations!  You've just been added as a Project Approver for the SkillTree project [${project.name}](${publicUrl}administrator/projects/${project.projectId}).\n\n" +
                        "The Project Approver role is allowed to approve and deny Self Reporting approval requests while only getting a read-only view of the project." +
                        " Thank you for being part of the SkillTree Community!\n\n" +
                        "Always yours,\n\n" +
                        "-SkillTree Bot"
                contactUsersService.sendEmail("SkillTree - You've been added as an approver", emailBody, userId, null, uiConfigProperties.ui.defaultCommunityDescriptor)
            }
        }

    }

    private PageRequest createPagingRequestWithValidation(String projectId, int limit, int page, String orderBy, Boolean ascending, String type='Project') {
        SkillsValidator.isNotBlank(projectId, "${type} Id")
        SkillsValidator.isTrue(limit <= 200, "Cannot ask for more than 200 items, provided=[${limit}]", projectId)
        SkillsValidator.isTrue(page >= 0, "Cannot provide negative page. provided =[${page}]", projectId)
        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)

        return pageRequest
    }

}
