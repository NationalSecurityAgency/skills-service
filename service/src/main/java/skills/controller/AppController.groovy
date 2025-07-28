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
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import skills.PublicProps
import skills.auth.UserInfoService
import skills.auth.aop.ExcludeFromLimitDashboardAccess
import skills.auth.aop.LimitDashboardAccess
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.AdminGroupDefExistsRequest
import skills.controller.request.model.AdminGroupDefRequest
import skills.controller.request.model.BadgeRequest
import skills.controller.request.model.NameExistsRequest
import skills.controller.request.model.ProjectExistsRequest
import skills.controller.request.model.ProjectRequest
import skills.controller.request.model.QuizDefExistsRequest
import skills.controller.request.model.QuizDefRequest
import skills.controller.result.model.*
import skills.dbupgrade.DBUpgradeSafe
import skills.icons.CustomIconFacade
import skills.profile.EnableCallStackProf
import skills.services.GlobalBadgesService
import skills.services.IdFormatValidator
import skills.services.admin.InviteOnlyProjectService
import skills.services.admin.ProjAdminService
import skills.services.admin.ShareSkillsService
import skills.services.admin.SkillsAdminService
import skills.services.adminGroup.AdminGroupService
import skills.services.quiz.QuizDefService
import skills.utils.InputSanitizer

import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/app")
@Slf4j
@EnableCallStackProf
@LimitDashboardAccess
class AppController {

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    QuizDefService quizDefService

    @Autowired
    AdminGroupService adminGroupService

    @Autowired
    GlobalBadgesService globalBadgesService

    @Autowired
    CustomIconFacade customIconFacade

    @Autowired
    UserInfoService userInfoService

    @Autowired
    PublicPropsBasedValidator propsBasedValidator

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    static final RESERVERED_PROJECT_ID = ShareSkillsService.ALL_SKILLS_PROJECTS

    @RequestMapping(value = "/projects", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<ProjectResult> getProjects() {
        return projAdminService.getProjects()
    }

    @RequestMapping(value = "/quiz-definitions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<QuizDefResult> getQuizDefs() {
        return quizDefService.getCurrentUsersQuizDefs()
    }

    @RequestMapping(value = "/admin-group-definitions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<AdminGroupDefResult> getAdminGroupDefs() {
        return adminGroupService.getCurrentUsersAdminGroupDefs()
    }

    @RequestMapping(value = "/projects/{id}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    RequestResult saveProject(@PathVariable("id") String projectId, @RequestBody ProjectRequest projectRequest) {
        // project id is optional
        if (projectRequest.projectId && projectId != projectRequest.projectId) {
            throw new SkillException("Project id in the request doesn't equal to project id in the URL [${projectRequest?.projectId}]<>[${projectId}]. Cannot edit project id using /app/projects/{id} endpoint. Please use /admin/projects/{id}", null, null, ErrorCode.AccessDenied)
        }
        IdFormatValidator.validate(projectId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "Project Id", projectId)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "Project Id", projectId)

        if (!projectRequest.projectId) {
            projectRequest.projectId = projectId
        } else {
            IdFormatValidator.validate(projectRequest.projectId)
            propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "Project Id", projectRequest.projectId)
            propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "Project Id", projectRequest.projectId)
        }

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxProjectNameLength, "Project Name", projectRequest.name)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minNameLength, "Project Name", projectRequest.name)

        if (projectRequest.projectId == RESERVERED_PROJECT_ID) {
            throw new SkillException("Project id uses a reserved id, please choose a different project id.", projectId, null, ErrorCode.BadParam)
        }
        if (!projectRequest?.name) {
            throw new SkillException("Project name was not provided.", projectId, null, ErrorCode.BadParam)
        }

        projectRequest.projectId = InputSanitizer.sanitize(projectRequest.projectId)
        projectRequest.name = InputSanitizer.sanitize(projectRequest.name)?.trim()
        projectRequest.description = StringUtils.trimToNull(InputSanitizer.sanitize(projectRequest.description))

        projAdminService.saveProject(null, projectRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/quiz-definitions/{id}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    QuizDefResult saveQuizDef(@PathVariable("id") String quizId, @RequestBody QuizDefRequest quizDefRequest) {
        return quizDefService.saveQuizDef(null, quizId, quizDefRequest)
    }

    @RequestMapping(value = "/admin-group-definitions/{id}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    AdminGroupDefResult insertAdminGroupDef(@PathVariable("id") String adminGroupId, @RequestBody AdminGroupDefRequest adminGroupDefRequest) {
        SkillsValidator.isFirstOrMustEqualToSecond(adminGroupDefRequest.adminGroupId, adminGroupId, "Admin Group Id")
        SkillsValidator.isNotBlank(adminGroupId, "adminGroupId")
        SkillsValidator.isNotBlank(adminGroupDefRequest.adminGroupId, "adminGroupId")
        return adminGroupService.saveAdminGroupDef(false, adminGroupDefRequest)
    }

    @RequestMapping(value = "/projects/{id}/join/{invite_code}", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    @ExcludeFromLimitDashboardAccess
    RequestResult joinProject(@PathVariable("id") String projectId, @PathVariable("invite_code") String inviteCode) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(inviteCode, "invite_code", projectId)
        inviteOnlyProjectService.joinProject(inviteCode, projectId)
        return RequestResult.success()
    }

    @RequestMapping(value = "/projects/{id}/validateInvite/{invite_code}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ExcludeFromLimitDashboardAccess
    InviteTokenValidationResponse validateProjectInvite(@PathVariable("id") String projectId, @PathVariable("invite_code") String inviteCode) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(inviteCode, "inviteCode")
        return inviteOnlyProjectService.validateInvite(inviteCode, projectId)
    }

    @DBUpgradeSafe
    @RequestMapping(value = "/projectExist", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    boolean doesProjectExist(@RequestBody ProjectExistsRequest existsRequest) {
        String projectId = existsRequest.projectId
        String projectName = existsRequest.name?.trim()

        SkillsValidator.isTrue((projectId || projectName), "One of Project Id or Project Name must be provided.")
        SkillsValidator.isTrue(!(projectId && projectName), "Only Project Id or Project Name may be provided, not both.")

        if (projectId) {
            return projAdminService.existsByProjectId(InputSanitizer.sanitize(projectId))
        }

        return projAdminService.existsByProjectName(InputSanitizer.sanitize(projectName))
    }

    @DBUpgradeSafe
    @RequestMapping(value = "/quizDefExist", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    boolean doesQuizExist(@RequestBody QuizDefExistsRequest existsRequest) {
        String quizId = existsRequest.quizId?.trim()
        String name = existsRequest.name?.trim()

        SkillsValidator.isTrue((quizId || name), "One of Quiz Id or Quiz Name must be provided.")
        SkillsValidator.isTrue(!(quizId && name), "Only Quiz Id or Quiz Name may be provided, not both.")

        if (quizId) {
            return quizDefService.existsByQuizId(InputSanitizer.sanitize(quizId))
        }

        return quizDefService.existsByQuizName(InputSanitizer.sanitize(name))
    }

    @DBUpgradeSafe
    @RequestMapping(value = "/adminGroupDefExist", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    boolean doesAdminGroupExist(@RequestBody AdminGroupDefExistsRequest existsRequest) {
        String quizId = existsRequest.adminGroupId?.trim()
        String name = existsRequest.name?.trim()

        SkillsValidator.isTrue((quizId || name), "One of Admin Group Id or Admin Group Name must be provided.")
        SkillsValidator.isTrue(!(quizId && name), "Only Admin Group Id Admin Group Quiz Name may be provided, not both.")

        if (quizId) {
            return adminGroupService.existsByAdminGroupId(InputSanitizer.sanitize(quizId))
        }

        return adminGroupService.existsByAdminGroupName(InputSanitizer.sanitize(name))
    }

    @RequestMapping(value = "/projects/{id}/customIcons", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ExcludeFromLimitDashboardAccess
    List<CustomIconResult> getCustomIcons(@PathVariable("id") String projectId) {
        return projAdminService.getCustomIcons(projectId)
    }

    @RequestMapping(value = "/projects/{id}/versions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ExcludeFromLimitDashboardAccess
    List<Integer> listVersions(@PathVariable("id") String projectId) {
        return skillsAdminService.getUniqueSkillVersionList(projectId)
    }

    @RequestMapping(value = "/projects/{id}/description", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ExcludeFromLimitDashboardAccess
    ProjectDescription getProjectDescription(@PathVariable("id") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return projAdminService.getProjectDescription(projectId)
    }


    @RequestMapping(value = "/badges", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<GlobalBadgeResult> getBadges() {
        return globalBadgesService.getBadgesForUser()
    }

    @DBUpgradeSafe
    @RequestMapping(value = "/badges/name/exists", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesBadgeNameExist(@RequestBody() NameExistsRequest nameExistsRequest) {
        String badgeName = nameExistsRequest.name?.trim()
        SkillsValidator.isNotBlank(badgeName, "Badge Name")
        String decodedName = InputSanitizer.sanitize(badgeName)
        return globalBadgesService.existsByBadgeName(decodedName)
    }

    @RequestMapping(value = "/badges/id/{badgeId}/exists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesBadgeIdExist(@PathVariable("badgeId") String badgeId) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")
        String decodedId = InputSanitizer.sanitize(URLDecoder.decode(badgeId,  StandardCharsets.UTF_8.toString()))
        return globalBadgesService.existsByBadgeId(decodedId)
    }

    @RequestMapping(value = "/badges/{badgeId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult saveBadge(@PathVariable("badgeId") String badgeId,
                            @RequestBody BadgeRequest badgeRequest) {
        SkillsValidator.isNotBlank(badgeId, "Badge Id")
        badgeRequest.badgeId = badgeRequest.badgeId ?: badgeId
        SkillsValidator.isNotBlank(badgeRequest?.name, "Badge Name")

        IdFormatValidator.validate(badgeRequest.badgeId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "Badge Id", badgeRequest.badgeId)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "Badge Id", badgeRequest.badgeId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxBadgeNameLength, "Badge Name", badgeRequest.name)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minNameLength, "Badge Name", badgeRequest.name)
        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.descriptionMaxLength, "Badge Description", badgeRequest.description)

        badgeRequest.name = InputSanitizer.sanitize(badgeRequest.name)?.trim()
        badgeRequest.badgeId = InputSanitizer.sanitize(badgeRequest.badgeId)
        badgeRequest.description = InputSanitizer.sanitize(badgeRequest.description)
        badgeRequest.helpUrl = InputSanitizer.sanitizeUrl(badgeRequest.helpUrl)

        globalBadgesService.saveBadge(badgeId, badgeRequest)
        return new RequestResult(success: true)
    }
}
