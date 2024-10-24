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


import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.AdminGroupDefRequest
import skills.controller.result.model.*
import skills.services.adminGroup.AdminGroupRoleService
import skills.services.adminGroup.AdminGroupService
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.auth.RoleName

@RestController
@RequestMapping("/admin/admin-group-definitions")
@Slf4j
@skills.profile.EnableCallStackProf
class AdminGroupController {

    @Autowired
    AdminGroupService adminGroupService

    @Autowired
    AdminGroupRoleService adminGroupRoleService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @RequestMapping(value = "/{adminGroupId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    AdminGroupDefResult getAdminGroup(@PathVariable("adminGroupId") String adminGroupId) {
        SkillsValidator.isNotBlank(adminGroupId, "Admin Group Id")
        return adminGroupService.getAdminGroupDef(adminGroupId)
    }


    @RequestMapping(value = "/{adminGroupId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    AdminGroupDefResult updateAdminGroup(@PathVariable("adminGroupId") String adminGroupId, @RequestBody AdminGroupDefRequest adminGroupDefRequest) {
        SkillsValidator.isFirstOrMustEqualToSecond(adminGroupDefRequest.adminGroupId, adminGroupId, "Admin Group Id")
        SkillsValidator.isNotBlank(adminGroupId, "adminGroupId")
        SkillsValidator.isNotBlank(adminGroupDefRequest.adminGroupId, "adminGroupId")
        return adminGroupService.saveAdminGroupDef(true, adminGroupDefRequest)
    }

    @RequestMapping(value = "/{adminGroupId}", method = RequestMethod.DELETE)
    void deleteAdminGroup(@PathVariable("adminGroupId") String adminGroupId) {
        SkillsValidator.isNotBlank(adminGroupId, "Admin Group Id")
        adminGroupService.deleteAdminGroup(adminGroupId)
    }

    @RequestMapping(value = "/{adminGroupId}/members", method = RequestMethod.GET)
    List<UserRoleRes> getAdminGroupMembers(@PathVariable("adminGroupId") String adminGroupId) {
        SkillsValidator.isNotBlank(adminGroupId, "Admin Group Id")
        return adminGroupRoleService.getAdminGroupMemberUserRoles(adminGroupId)
    }

    @RequestMapping(value = "/{adminGroupId}/users/{userKey}/roles/{roleName}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult addAdminGroupRole(@PathVariable("adminGroupId") String adminGroupId,
                                    @PathVariable("userKey") String userKey,
                                    @PathVariable("roleName") RoleName roleName) {
        SkillsValidator.isNotBlank(adminGroupId, "Admin Group Id")
        SkillsValidator.isNotBlank(userKey, "userKey")
        SkillsValidator.isNotNull(roleName, "roleName")

        adminGroupRoleService.addAdminGroupRole(userKey, adminGroupId, roleName)

        return RequestResult.success()
    }

    @RequestMapping(value = "/{adminGroupId}/users/{userKey}/roles/{roleName}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult deleteAdminGroupRole(@PathVariable("adminGroupId") String adminGroupId,
                                       @PathVariable("userKey") String userKey,
                                       @PathVariable("roleName") RoleName roleName) {
        SkillsValidator.isNotBlank(adminGroupId, "Admin Group Id")
        SkillsValidator.isNotBlank(userKey, "userKey")
        SkillsValidator.isNotNull(roleName, "roleName")

        adminGroupRoleService.deleteAdminGroupRole(userKey, adminGroupId, roleName)
        return RequestResult.success()
    }

    @RequestMapping(value = "/{adminGroupId}/quizzes", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    AdminGroupQuizResult getAdminGroupQuizzesAndSurveys(@PathVariable("adminGroupId") String adminGroupId) {
        SkillsValidator.isNotBlank(adminGroupId, "Admin Group Id")
        AdminGroupQuizResult adminGroupQuizResult = adminGroupService.getAdminGroupQuizzesAndSurveys(adminGroupId)

        return adminGroupQuizResult
    }

    @RequestMapping(value = "/{adminGroupId}/quizzes/{quizId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    AdminGroupQuizResult addQuizToAdminGroup(@PathVariable("adminGroupId") String adminGroupId,
                                             @PathVariable("quizId") String quizId) {
        SkillsValidator.isNotBlank(adminGroupId, "Admin Group Id")
        SkillsValidator.isNotBlank(quizId, "Quiz Id")

        adminGroupRoleService.addQuizToAdminGroup(adminGroupId, quizId)
        return adminGroupService.getAdminGroupQuizzesAndSurveys(adminGroupId)
    }

    @RequestMapping(value = "/{adminGroupId}/quizzes/{quizId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    AdminGroupQuizResult removeQuizFromAdminGroup(@PathVariable("adminGroupId") String adminGroupId,
                                                  @PathVariable("quizId") String quizId) {
        SkillsValidator.isNotBlank(adminGroupId, "Admin Group Id")
        SkillsValidator.isNotBlank(quizId, "Quiz Id")

        adminGroupRoleService.removeQuizFromAdminGroup(adminGroupId, quizId)
        return adminGroupService.getAdminGroupQuizzesAndSurveys(adminGroupId)
    }

    @RequestMapping(value = "/{adminGroupId}/projects", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    AdminGroupProjectResult getAdminGroupProjects(@PathVariable("adminGroupId") String adminGroupId) {
        SkillsValidator.isNotBlank(adminGroupId, "Admin Group Id")
        AdminGroupProjectResult adminGroupProjectResult = adminGroupService.getAdminGroupProjects(adminGroupId)

        return adminGroupProjectResult
    }

    @RequestMapping(value = "/{adminGroupId}/projects/{projectId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    AdminGroupProjectResult addProjectToAdminGroup(@PathVariable("adminGroupId") String adminGroupId,
                                                   @PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(adminGroupId, "Admin Group Id")
        SkillsValidator.isNotBlank(projectId, "Project Id")

        adminGroupRoleService.addProjectToAdminGroup(adminGroupId, projectId)
        return adminGroupService.getAdminGroupProjects(adminGroupId)
    }

    @RequestMapping(value = "/{adminGroupId}/projects/{projectId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    AdminGroupProjectResult removeProjectFromAdminGroup(@PathVariable("adminGroupId") String adminGroupId,
                                                        @PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(adminGroupId, "Admin Group Id")
        SkillsValidator.isNotBlank(projectId, "Quiz Id")

        adminGroupRoleService.removeProjectFromAdminGroup(adminGroupId, projectId)
        return adminGroupService.getAdminGroupProjects(adminGroupId)
    }

    @RequestMapping(value = "/{adminGroupId}/validateEnablingCommunity", method = RequestMethod.GET, produces = "application/json")
    EnableUserCommunityValidationRes validateProjectForEnablingCommunity(@PathVariable("adminGroupId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        return adminGroupService.validateAdminGroupForEnablingCommunity(projectId)
    }
}
