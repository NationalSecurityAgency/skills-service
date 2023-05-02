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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.JpaSort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import skills.PublicProps
import skills.auth.UserInfoService
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.*
import skills.controller.result.model.*
import skills.dbupgrade.DBUpgradeSafe
import skills.services.*
import skills.services.admin.*
import skills.services.admin.moveSkills.SkillsMoveService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.admin.skillReuse.SkillReuseService
import skills.services.events.BulkSkillEventResult
import skills.services.events.pointsAndAchievements.InsufficientPointsValidator
import skills.services.inception.InceptionProjectService
import skills.services.settings.ProjectSettingsValidator
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.services.settings.listeners.ValidationRes
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.utils.ClientSecretGenerator
import skills.utils.InputSanitizer

import java.nio.charset.StandardCharsets

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

@RestController
@RequestMapping("/admin")
@Slf4j
@skills.profile.EnableCallStackProf
class AdminController {

    @Autowired
    LevelDefinitionStorageService levelDefinitionStorageService

    @Autowired
    AdminUsersService adminUsersService

    @Autowired
    UserAdminService userAdminService

    @Autowired
    SettingsService settingsService

    @Autowired
    SkillEventAdminService skillEventService

    @Autowired
    GlobalBadgesService globalBadgesService

    @Autowired
    PublicPropsBasedValidator propsBasedValidator

    @Autowired
    ControllerPropsValidatorAndSanitizer controllerPropsValidatorAndSanitizer

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    SubjAdminService subjAdminService

    @Autowired
    ProjectCopyService projectCopyService

    @Autowired
    BadgeAdminService badgeAdminService

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    SaveSkillService saveSkillService

    @Autowired
    SkillsGroupAdminService skillsGroupAdminService

    @Autowired
    SkillsDepsService skillsDepsService

    @Autowired
    ShareSkillsService shareSkillsService

    @Autowired
    SkillTagService skillTagService

    @Autowired
    ProjectSettingsValidator projectSettingsValidator

    @Value('#{"${skills.config.ui.maxTimeWindowInMinutes}"}')
    int maxTimeWindowInMinutes

    @Value('#{"${skills.config.maxUserIdsForBulkSkillReporting:1000}"}')
    int maxUserIdsForBulkSkillReporting

    @Autowired
    ProjectErrorService errorService

    @Autowired
    SkillEventAdminService skillEventAdminService

    @Autowired
    ContactUsersService contactUsersService

    @Autowired
    UserInfoService userInfoService

    @Autowired
    SkillCatalogService skillCatalogService

    @Autowired
    SkillReuseService skillReuseService

    @Autowired
    SkillsMoveService skillsMoveService

    @Autowired
    InsufficientPointsValidator insufficientPointsValidator

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    @Value('#{"${skills.config.ui.maxSkillsInBulkImport}"}')
    int maxBulkImport

    @Value('#{"${skills.config.ui.maxProjectInviteEmails:50}"}')
    int maxInviteEmails


    @RequestMapping(value = "/projects/{id}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    RequestResult saveProject(@PathVariable("id") String projectId, @RequestBody ProjectRequest projectRequest) {
        projectRequest = controllerPropsValidatorAndSanitizer.validateAndSanitizeProjectRequest(projectRequest)
        projectId = controllerPropsValidatorAndSanitizer.validateAndSanitizeProjectId(projectId)
        projAdminService.saveProject(projectId, projectRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{id}/copy", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    RequestResult copyProject(@PathVariable("id") String projectId, @RequestBody ProjectRequest projectRequest) {
        projectRequest = controllerPropsValidatorAndSanitizer.validateAndSanitizeProjectRequest(projectRequest)
        projectId = controllerPropsValidatorAndSanitizer.validateAndSanitizeProjectId(projectId)
        projectCopyService.copyProject(projectId, projectRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{id}/invite", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    InviteUsersResult generateInvite(@PathVariable("id") String projectId, @RequestBody ProjectInviteRequest inviteRequest) {
        SkillsValidator.isNotBlank(projectId, "Project ID")
        SkillsValidator.isTrue(!inviteRequest?.recipients?.isEmpty(), "at least one email is required")
        SkillsValidator.isTrue(inviteRequest?.recipients?.size() <= maxInviteEmails, "No more than ${maxInviteEmails} project invites may  be sent at one time")

        InviteUsersResult res = inviteOnlyProjectService.inviteUsers(projectId, inviteRequest.recipients, inviteRequest.validityDuration)
        return res
    }

    @RequestMapping(value ="/projects/{id}/invites/status", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    TableResult getInviteStatus(@PathVariable("id") String projectId, @RequestParam int limit,
                                              @RequestParam int page,
                                              @RequestParam String orderBy,
                                              @RequestParam Boolean ascending,
                                              @RequestParam(required = false, defaultValue = "") String query) {

        PageRequest pagingRequest = createPagingRequestWithValidation(projectId, limit, page, orderBy, ascending)
        return inviteOnlyProjectService.getPendingInvites(projectId, URLDecoder.decode(query, StandardCharsets.UTF_8), pagingRequest)
    }

    @RequestMapping(value="/projects/{id}/invites/extend", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    RequestResult extendInviteExpiration(@PathVariable("id") String projectId, @RequestBody InviteExtensionRequest inviteExtensionRequest) {
        SkillsValidator.isNotBlank(projectId, "Project ID")
        SkillsValidator.isNotBlank(inviteExtensionRequest?.extensionDuration, "Extension Duration")
        SkillsValidator.isNotBlank(inviteExtensionRequest?.recipientEmail, "Recipient Email")

        inviteOnlyProjectService.extendValidity(projectId, inviteExtensionRequest.recipientEmail, inviteExtensionRequest.extensionDuration)
        return RequestResult.success()
    }

    @RequestMapping(value="/projects/{id}/invites/{recipient}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    RequestResult deleteInvite(@PathVariable("id") String projectId, @PathVariable("recipient") String recipientEmail) {
        SkillsValidator.isNotBlank(projectId, "Project ID")
        SkillsValidator.isNotBlank(recipientEmail, "Recipient Email")

        inviteOnlyProjectService.deleteInvite(projectId, recipientEmail)
        return RequestResult.success()
    }

    @RequestMapping(value="/projects/{id}/invites/{recipient}/remind", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    RequestResult sendInviteReminder(@PathVariable("id") String projectId, @PathVariable("recipient") String recipientEmail) {
        SkillsValidator.isNotBlank(projectId, "Project ID")
        SkillsValidator.isNotBlank(recipientEmail, "Recipient Email")

        inviteOnlyProjectService.remindUser(projectId, recipientEmail)
        return RequestResult.success()
    }

    @RequestMapping(value="/projects/{id}/cancelExpiration")
    RequestResult cancelExpiration(@PathVariable("id") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        projAdminService.cancelProjectExpiration(projectId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{id}", method = RequestMethod.DELETE)
    void deleteProject(@PathVariable("id") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        projAdminService.deleteProject(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult setProjectDisplayOrder(
            @PathVariable("projectId") String projectId, @RequestBody ActionPatchRequest projectPatchRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotNull(projectPatchRequest.action, "Action", projectId)

        projAdminService.setProjectDisplayOrder(projectId, projectPatchRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ProjectResult getProject(@PathVariable("id") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return projAdminService.getProject(projectId)
    }

    @RequestMapping(value = "/projects/{id}/projectSearch", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SimpleProjectResult> searchProjects(@PathVariable("id") String projectId, @RequestParam("nameQuery") nameQuery) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return projAdminService.searchProjects(projectId, nameQuery)?.findAll {it.projectId != InceptionProjectService.inceptionProjectId}
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult saveSubject(@PathVariable("projectId") String projectId,
                              @PathVariable("subjectId") String subjectId,
                              @RequestBody SubjectRequest subjectRequest) {
        subjectRequest.subjectId = subjectRequest.subjectId ?: subjectId
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        SkillsValidator.isNotBlank(subjectRequest?.name, "Subject name", projectId)

        IdFormatValidator.validate(subjectRequest.subjectId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "Subject Id", subjectRequest.subjectId)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "Subject Id", subjectRequest.subjectId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxSubjectNameLength, "Subject Name", subjectRequest.name)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minNameLength, "Subject Name", subjectRequest.name)
        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.descriptionMaxLength, "Subject Description", subjectRequest.description)

        subjectRequest.subjectId = InputSanitizer.sanitize(subjectRequest.subjectId)
        subjectRequest.description = InputSanitizer.sanitize(subjectRequest.description)
        subjectRequest.name = InputSanitizer.sanitize(subjectRequest.name)?.trim()
        subjectRequest.iconClass = InputSanitizer.sanitize(subjectRequest.iconClass)
        subjectRequest.helpUrl = InputSanitizer.sanitizeUrl(subjectRequest.helpUrl)

        subjAdminService.saveSubject(InputSanitizer.sanitize(projectId), subjectId, subjectRequest)
        return new RequestResult(success: true)
    }

    @DBUpgradeSafe
    @RequestMapping(value = "/projects/{projectId}/subjectNameExists", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesSubjectNameExist(@PathVariable("projectId") String projectId,
                             @RequestBody NameExistsRequest existsRequest) {
        String subjectName = existsRequest.name?.trim()
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectName, "Subject Name")

        def sanitize = InputSanitizer.sanitize(subjectName)
        return subjAdminService.existsBySubjectName(InputSanitizer.sanitize(projectId), sanitize)
    }

    @DBUpgradeSafe
    @RequestMapping(value = "/projects/{projectId}/badgeNameExists", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesBadgeExist(@PathVariable("projectId") String projectId,
                             @RequestBody NameExistsRequest nameExistsRequest) {
        String badgeName = nameExistsRequest.name?.trim()
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeName, "Badge Name")
        return badgeAdminService.existsByBadgeName(InputSanitizer.sanitize(projectId), InputSanitizer.sanitize(badgeName))
    }

    @DBUpgradeSafe
    @RequestMapping(value = "/projects/{projectId}/skillNameExists", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesSkillNameExist(@PathVariable("projectId") String projectId,
                           @RequestBody NameExistsRequest existsRequest) {
        String skillName = existsRequest.name?.trim()
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillName, "Skill Name")
        return skillsAdminService.existsBySkillName(InputSanitizer.sanitize(projectId), InputSanitizer.sanitize(skillName))
    }

    /**
     * checks whether any entities with the given id exist, which can be id for subject, skill, badge, etc...
     * @param projectId
     * @param id - this id can be subject, skill, badge, et..
     * @return true or false
     */
    @RequestMapping(value = "/projects/{projectId}/entityIdExists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesProjectEntityIdExist(@PathVariable("projectId") String projectId,
                                    @RequestParam(value = "id") String id) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(id, "Entity Id")

        return skillsAdminService.existsBySkillId(InputSanitizer.sanitize(projectId), InputSanitizer.sanitize(id))
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = RequestMethod.DELETE)
    void deleteSubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)

        subjAdminService.deleteSubject(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SubjectResult> getSubjects(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return subjAdminService.getSubjects(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SubjectResult getSubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        return subjAdminService.getSubject(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult setSubjectDisplayOrder(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId, @RequestBody ActionPatchRequest subjectPatchRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        SkillsValidator.isNotNull(subjectPatchRequest.action, "Action must be provided", projectId)
        subjAdminService.setSubjectDisplayOrder(projectId, subjectId, subjectPatchRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult saveBadge(@PathVariable("projectId") String projectId,
                            @PathVariable("badgeId") String badgeId,
                            @RequestBody BadgeRequest badgeRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)
        badgeRequest.badgeId = badgeRequest.badgeId ?: badgeId
        SkillsValidator.isNotBlank(badgeRequest?.name, "Badge Name", projectId)
        SkillsValidator.isTrue((badgeRequest.startDate && badgeRequest.endDate) || (!badgeRequest.startDate && !badgeRequest.endDate),
                "If one date is provided then both start and end dates must be provided", projectId)

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

        badgeAdminService.saveBadge(projectId, badgeId, badgeRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/badge/{badgeId}/skills/{skillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult assignSkillToBadge(@PathVariable("projectId") String projectId,
                                     @PathVariable("badgeId") String badgeId,
                                     @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        SkillsValidator.isTrue(!skillId.toUpperCase().contains(SkillReuseIdUtil.REUSE_TAG.toUpperCase()), "Skill ID must not contain reuse tag", projectId, skillId)

        badgeAdminService.addSkillToBadge(projectId, badgeId, skillId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/badge/{badgeId}/skills/{skillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult removeSkillFromBadge(@PathVariable("projectId") String projectId,
                                       @PathVariable("badgeId") String badgeId,
                                       @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        badgeAdminService.removeSkillFromBadge(projectId, badgeId, skillId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.DELETE)
    void deleteBadge(@PathVariable("projectId") String projectId, @PathVariable("badgeId") String badgeId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)

        badgeAdminService.deleteBadge(projectId, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<BadgeResult> getBadges(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")

        return badgeAdminService.getBadges(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    BadgeResult getBadge(@PathVariable("projectId") String projectId,
                         @PathVariable("badgeId") String badgeId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)

        return badgeAdminService.getBadge(projectId, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void setBadgeDisplayOrder(
            @PathVariable("projectId") String projectId,
            @PathVariable("badgeId") String badgeId, @RequestBody ActionPatchRequest badgePatchRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)
        SkillsValidator.isNotNull(badgePatchRequest.action, "Action must be provided", projectId)

        badgeAdminService.setBadgeDisplayOrder(projectId, badgeId, badgePatchRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SkillDefPartialRes> getSkills(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId,
            @RequestParam(required = false, value = "includeGroupSkills", defaultValue = 'false') Boolean includeGroupSkillsStr) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)

        Boolean includeGroupSkills = Boolean.valueOf(includeGroupSkillsStr)
        return skillsAdminService.getSkillsForSubjectWithCatalogStatus(projectId, subjectId, includeGroupSkills)
    }

    @RequestMapping(value = "/projects/{projectId}/groups/{groupId}/skills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SkillDefPartialRes> getSkillsForSkillsGroup(
            @PathVariable("projectId") String projectId, @PathVariable("groupId") String groupId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(groupId, "Skills Group Id", projectId)

        return skillsAdminService.getSkillsByProjectSkillAndType(projectId, groupId, SkillDef.ContainerType.SkillsGroup, SkillRelDef.RelationshipType.SkillsGroupRequirement)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillDefRes getSkill(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId, @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        return skillsAdminService.getSkill(projectId, subjectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult saveSkill(@PathVariable("projectId") String projectId,
                            @PathVariable("subjectId") String subjectId,
                            @PathVariable("skillId") String skillId,
                            @RequestBody SkillRequest skillRequest) {

        validateAndSaveSkill(projectId, subjectId, skillId, skillRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/groups/{groupId}/skills/{skillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult saveSkillAndAssignToSkillsGroup(@PathVariable("projectId") String projectId,
                                                  @PathVariable("subjectId") String subjectId,
                                                  @PathVariable("groupId") String groupId,
                                                  @PathVariable("skillId") String skillId,
                                                  @RequestBody SkillRequest skillRequest) {
        SkillsValidator.isNotBlank(groupId, "Skills Group Id", projectId)
        validateAndSaveSkill(projectId, subjectId, skillId, skillRequest, groupId)
        return new RequestResult(success: true)
    }

    private void validateAndSaveSkill(String projectId, String subjectId, String skillId, SkillRequest skillRequest, String groupId=null) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        SkillsValidator.isFirstOrMustEqualToSecond(skillRequest.projectId, projectId, "Project Id")
        skillRequest.projectId = skillRequest.projectId ?: projectId

        SkillsValidator.isFirstOrMustEqualToSecond(skillRequest.subjectId, subjectId, "Subject Id")
        skillRequest.subjectId = skillRequest.subjectId ?: subjectId
        skillRequest.skillId = skillRequest.skillId ?: skillId
        SkillsValidator.isTrue(!skillRequest.skillId.toUpperCase().contains(SkillReuseIdUtil.REUSE_TAG.toUpperCase()), "Skill ID must not contain reuse tag", projectId, skillId)
        SkillsValidator.isTrue(!skillRequest.name?.toUpperCase()?.contains(SkillReuseIdUtil.REUSE_TAG.toUpperCase()), "Skill Name must not contain reuse tag", projectId, skillId)

        // if type is not provided then we default to skill
        skillRequest.type = skillRequest.type ?: SkillDef.ContainerType.Skill.toString()
        Boolean isBasicSkill = skillRequest.type == SkillDef.ContainerType.Skill.toString()

        IdFormatValidator.validate(skillRequest.skillId, isBasicSkill)
        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "Skill Id", skillRequest.skillId)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "Skill Id", skillRequest.skillId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxSkillNameLength, "Skill Name", skillRequest.name)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minNameLength, "Skill Name", skillRequest.name)

        skillRequest.name = InputSanitizer.sanitize(skillRequest.name)?.trim()
        skillRequest.projectId = InputSanitizer.sanitize(skillRequest.projectId)
        skillRequest.skillId = InputSanitizer.sanitize(skillRequest.skillId)
        skillRequest.subjectId = InputSanitizer.sanitize(skillRequest.subjectId)

        if (isBasicSkill) {
            SkillsValidator.isTrue(skillRequest.pointIncrement > 0, "pointIncrement must be > 0", projectId, skillId)
            propsBasedValidator.validateMaxIntValue(PublicProps.UiProp.maxPointIncrement, "pointIncrement", skillRequest.pointIncrement)

            SkillsValidator.isTrue(skillRequest.pointIncrementInterval >= 0, "pointIncrementInterval must be >= 0", projectId, skillId)
            SkillsValidator.isTrue(skillRequest.pointIncrementInterval <= maxTimeWindowInMinutes, "pointIncrementInterval must be <= $maxTimeWindowInMinutes", projectId, skillId)
            SkillsValidator.isTrue(skillRequest.numPerformToCompletion > 0, "numPerformToCompletion must be > 0", projectId, skillId)
            propsBasedValidator.validateMaxIntValue(PublicProps.UiProp.maxNumPerformToCompletion, "numPerformToCompletion", skillRequest.numPerformToCompletion)

            if (skillRequest.pointIncrementInterval > 0) {
                // if pointIncrementInterval is disabled then this validation is not needed
                SkillsValidator.isTrue(skillRequest.numMaxOccurrencesIncrementInterval > 0, "numMaxOccurrencesIncrementInterval must be > 0", projectId, skillId)
                SkillsValidator.isTrue(skillRequest.numPerformToCompletion >= skillRequest.numMaxOccurrencesIncrementInterval, "numPerformToCompletion must be >= numMaxOccurrencesIncrementInterval", projectId, skillId)
                propsBasedValidator.validateMaxIntValue(PublicProps.UiProp.maxNumPointIncrementMaxOccurrences, "numMaxOccurrencesIncrementInterval", skillRequest.numMaxOccurrencesIncrementInterval)
            }

            SkillsValidator.isTrue(skillRequest.version >= 0, "version must be >= 0", projectId, skillId)
            propsBasedValidator.validateMaxIntValue(PublicProps.UiProp.maxSkillVersion, "Skill Version", skillRequest.version)

            propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.descriptionMaxLength, "Skill Description", skillRequest.description)
            skillRequest.description = InputSanitizer.sanitize(skillRequest.description)
            skillRequest.helpUrl = InputSanitizer.sanitizeUrl(skillRequest.helpUrl)
        }

        // default to enabled
        skillRequest.enabled = skillRequest.enabled == null ? "true" : skillRequest.enabled

        if (skillRequest.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()) {
            SkillsValidator.isTrue(StringUtils.isNotBlank(skillRequest.quizId), "When selfReportingType=Quiz then quizId param must not be blank", projectId, skillId)
        }

        if (skillRequest.quizId) {
            SkillsValidator.isTrue(skillRequest.selfReportingType == SkillDef.SelfReportingType.Quiz.toString(), "When quizId is provided then selfReportingType must equal 'Quiz'", projectId, skillId)
            SkillsValidator.isTrue(skillRequest.numPerformToCompletion == 1, "When quizId is provided numPerformToCompletion must be equal 1", projectId, skillId)
        }

        saveSkillService.saveSkillAndSchedulePropagationToImportedSkills(skillId, skillRequest, true, groupId)
    }

    @GetMapping(value = '/projects/{projectId}/latestVersion', produces = 'application/json')
    Integer findLatestSkillVersion(@PathVariable('projectId') String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return skillsAdminService.findLatestSkillVersion(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/dependency/availableSkills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SkillDefForDependencyRes> getSkillsAvailableForDependency(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return skillsDepsService.getSkillsAvailableForDependency(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{dependentSkillId}/dependency/{dependencySkillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult assignDependency(@PathVariable("projectId") String projectId,
                                   @PathVariable("dependentSkillId") String dependentSkillId,
                                   @PathVariable("dependencySkillId") String dependencySkillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(dependentSkillId, "Dependent Skill Id", projectId)
        SkillsValidator.isNotBlank(dependencySkillId, "Dependency Skill Id", projectId)
        SkillsValidator.isTrue(!dependentSkillId.toUpperCase().contains(SkillReuseIdUtil.REUSE_TAG.toUpperCase()), "Skill ID must not contain reuse tag", projectId, dependentSkillId)
        SkillsValidator.isTrue(!dependencySkillId.toUpperCase().contains(SkillReuseIdUtil.REUSE_TAG.toUpperCase()), "Skill ID must not contain reuse tag", projectId, dependencySkillId)

        skillsDepsService.assignSkillDependency(projectId, dependentSkillId, dependencySkillId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/hasDependency", method = [RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SkillDepResult> doSkillsHaveDependency(@PathVariable("projectId") String projectId,
                                                @RequestBody SkillIdsRequest checkSkillsDepsRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotEmpty(checkSkillsDepsRequest?.skillIds, "Skill Ids", projectId)

        return skillsDepsService.checkIfSkillsHaveDeps(projectId, checkSkillsDepsRequest?.skillIds)
    }


    @RequestMapping(value = "/projects/{projectId}/skills/{dependentSkillId}/dependency/projects/{dependencyProjectId}/skills/{dependencySkillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult assignDependencyFromAnotherProject(@PathVariable("projectId") String projectId,
                                                     @PathVariable("dependentSkillId") String dependentSkillId,
                                                     @PathVariable("dependencyProjectId") String dependencyProjectId,
                                                     @PathVariable("dependencySkillId") String dependencySkillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(dependencySkillId, "Dependent Skill Id", projectId)
        SkillsValidator.isNotBlank(dependencyProjectId, "Dependency Project Id", projectId)
        SkillsValidator.isNotBlank(dependentSkillId, "Dependency Skill Id", projectId)
        SkillsValidator.isTrue(!dependentSkillId.toUpperCase().contains(SkillReuseIdUtil.REUSE_TAG.toUpperCase()), "Skill ID must not contain reuse tag", projectId, dependentSkillId)
        SkillsValidator.isTrue(!dependencySkillId.toUpperCase().contains(SkillReuseIdUtil.REUSE_TAG.toUpperCase()), "Skill ID must not contain reuse tag", projectId, dependencySkillId)

        skillsDepsService.assignSkillDependency(projectId, dependentSkillId, dependencySkillId, dependencyProjectId)
        return new RequestResult(success: true)
    }


    @RequestMapping(value = "/projects/{projectId}/skills/{dependentSkillId}/dependency/{dependencySkillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult removeDependency(@PathVariable("projectId") String projectId,
                                   @PathVariable("dependentSkillId") String dependentSkillId,
                                   @PathVariable("dependencySkillId") String dependencySkillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(dependentSkillId, "Dependent Skill Id", projectId)
        SkillsValidator.isNotBlank(dependencySkillId, "Dependency Skill Id", projectId)

        skillsDepsService.removeSkillDependency(projectId, dependentSkillId, dependencySkillId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{dependentSkillId}/dependency/projects/{dependencyProjectId}/skills/{dependencySkillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult removeDependencyFromAnotherProject(@PathVariable("projectId") String projectId,
                                                     @PathVariable("dependentSkillId") String dependentSkillId,
                                                     @PathVariable("dependencyProjectId") String dependencyProjectId,
                                                     @PathVariable("dependencySkillId") String dependencySkillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(dependentSkillId, "Dependent Skill Id", projectId)
        SkillsValidator.isNotBlank(dependencyProjectId, "Dependency Project Id", projectId)
        SkillsValidator.isNotBlank(dependencySkillId, "Dependency Skill Id", projectId)

        skillsDepsService.removeSkillDependency(projectId, dependentSkillId, dependencySkillId, dependencyProjectId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/graph", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillsGraphRes getDependencyForSkill(@PathVariable("projectId") String projectId,
                                         @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        return skillsDepsService.getDependentSkillsGraph(projectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/dependency/graph", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillsGraphRes getDependencyForProject(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")

        return skillsDepsService.getDependentSkillsGraph(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = RequestMethod.PATCH)
    @ResponseBody
    RequestResult updateSkillDisplayOrder(@PathVariable("projectId") String projectId,
                                          @PathVariable("subjectId") String subjectId,
                                          @PathVariable("skillId") String skillId,
                                          @RequestBody ActionPatchRequest patchRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        SkillsValidator.isNotNull(patchRequest.action, "Action must be provided", projectId)

        skillsAdminService.updateSkillDisplayOrder(projectId, subjectId, skillId, patchRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void deleteSkill(@PathVariable("projectId") String projectId,
                     @PathVariable("subjectId") String subjectId,
                     @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        skillsAdminService.deleteSkill(projectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/users/{userId}/events", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult deleteAllSkillEventsForUser(@PathVariable("projectId") String projectId,
                                              @PathVariable("userId") String userId) {

        return skillEventService.bulkDeleteSkillEventsForUser(projectId, userId?.toLowerCase())
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/users/{userId}/events/{timestamp}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult deleteSkillEvent(@PathVariable("projectId") String projectId,
                                      @PathVariable("skillId") String skillId,
                                      @PathVariable("userId") String userId,
                                      @PathVariable("timestamp") Long timestamp) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotNull(skillId, "Skill Id", projectId)
        SkillsValidator.isNotNull(userId, "User Id", projectId)
        SkillsValidator.isNotNull(timestamp, "Timestamp", projectId)

        return skillEventService.deleteSkillEvent(projectId, skillId, userId?.toLowerCase(), timestamp)
    }

    @RequestMapping(value = "/projects/{projectId}/skills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SkillDefSkinnyRes> getAllSkillsForProject(
            @PathVariable("projectId") String projectId,
            @RequestParam(required = false, value = "skillNameQuery") String skillNameQuery,
            @RequestParam(required = false, value = "excludeImportedSkills") Boolean excludeImportedSkills,
            @RequestParam(required = false, value = "excludeReusedSkills") Boolean excludeReusedSkills,
            @RequestParam(required = false, value = "includeDisabled", defaultValue = "false") Boolean includeDisabled) {
        SkillsValidator.isNotBlank(projectId, "Project Id")

        boolean excludeImportedSkillsBol = excludeImportedSkills
        boolean includeDisabledBool = includeDisabled
        List<SkillDefSkinnyRes> res = skillsAdminService.getSkinnySkills(projectId, skillNameQuery ?: '', excludeImportedSkillsBol, includeDisabledBool)
        if (excludeReusedSkills) {
            res = res.findAll { !it.isReused }
        }
        return res
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    SkillDefSkinnyRes getSkillInfo(
            @PathVariable("projectId") String projectId,
            @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id")
        SkillDefSkinnyRes res = skillsAdminService.getSkinnySkill(projectId, skillId)
        return res
    }


    @RequestMapping(value = "/projects/{projectId}/levels", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<LevelDefinitionRes> getLevels(
            @PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")

        List<LevelDefinitionRes> res = levelDefinitionStorageService.getLevels(projectId)
        return res
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/levels", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<LevelDefinitionRes> getLevels(
            @PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)

        List<LevelDefinitionRes> res = levelDefinitionStorageService.getLevels(projectId, subjectId)
        return res
    }

    @RequestMapping(value = "/projects/{projectId}/levels/last", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    void deleteLastLevel(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")

        levelDefinitionStorageService.deleteLastLevel(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/levels/next", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult addNextLevel(@PathVariable("projectId") String projectId, @RequestBody NextLevelRequest nextLevelRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        levelDefinitionStorageService.addNextLevel(projectId, nextLevelRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/levels/last", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    void deleteLastLevel(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        levelDefinitionStorageService.deleteLastLevel(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/levels/next", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult addNextLevel(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId, @RequestBody NextLevelRequest nextLevelRequest) {
        levelDefinitionStorageService.addNextLevel(projectId, nextLevelRequest, subjectId)
        return new RequestResult(success: true)
    }

    //Need new methods to edit existing level methods for project and subject
    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/levels/edit/{level}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult editLevel(@PathVariable("projectId") String projectId,
                            @PathVariable("subjectId") String subjectId,
                            @PathVariable("level") Integer level, @RequestBody EditLevelRequest editLevelRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        SkillsValidator.isNotNull(level, "Level", projectId)

        levelDefinitionStorageService.editLevel(projectId, editLevelRequest, level, subjectId)
        return new RequestResult(success: true)
    }

    //Need new methods to edit existing level methods for project and subject
    @RequestMapping(value = "/projects/{projectId}/levels/edit/{level}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult editLevel(@PathVariable("projectId") String projectId,
                            @PathVariable("level") Integer level, @RequestBody EditLevelRequest editLevelRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotNull(level, "Level", projectId)
        levelDefinitionStorageService.editLevel(projectId, editLevelRequest, level)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/performedSkills/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @CompileStatic
    TableResult getUserPerformedSkills(@PathVariable("projectId") String projectId,
                                       @PathVariable("userId") String userId,
                                       @RequestParam String query,
                                       @RequestParam int limit,
                                       @RequestParam int page,
                                       @RequestParam String orderBy,
                                       @RequestParam Boolean ascending) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(userId, "User Id", projectId)

        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        return userAdminService.loadUserPerformedSkillsPage(projectId, userId?.toLowerCase(), query, pageRequest)
    }

    @GetMapping(value = '/projects/{projectId}/users/{userId}/stats', produces = 'application/json')
    @ResponseBody
    @CompileStatic
    UserSkillsStats getUserSkillsStats(@PathVariable('projectId') String projectId,
                                       @PathVariable('userId') String userId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(userId, "User Id", projectId)

        return userAdminService.getUserSkillsStats(projectId, userId?.toLowerCase())
    }

    @GetMapping(value = "/projects/{projectId}/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @CompileStatic
    TableResultWithTotalPoints getProjectUsers(@PathVariable("projectId") String projectId,
                                @RequestParam String query,
                                @RequestParam int limit,
                                @RequestParam int page,
                                @RequestParam String orderBy,
                                @RequestParam Boolean ascending,
                                @RequestParam int minimumPoints) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isTrue(minimumPoints >=0, "Minimum Points is less than 0", projectId)

        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        return adminUsersService.loadUsersPageForProject(projectId, query, pageRequest, minimumPoints)
    }

    @GetMapping(value="/projects/{projectId}/{userId}/canAccess", produces='application/json')
    @ResponseBody
    @CompileStatic
    Boolean canUserAccess(@PathVariable("projectId") String projectId, @PathVariable("userId") String userId,
                          @RequestParam(required=false, value="idType") String idType) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(userId, "User ID")

        if (inviteOnlyProjectService.isInviteOnlyProject(projectId)) {
            return inviteOnlyProjectService.canUserAccess(projectId, userId, idType)
        }
        return Boolean.TRUE
    }

    @RequestMapping(value = "/projects/{projectId}/users/{userId}", method = RequestMethod.GET)
    UserInfoRes getUserInfo(
            @PathVariable("projectId") String projectId,
            @PathVariable("userId") String userId) {
        return adminUsersService.getUserForProject(projectId, userId?.toLowerCase())
    }

    @GetMapping(value = "/projects/{projectId}/subjects/{subjectId}/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    TableResultWithTotalPoints getSubjectUsers(@PathVariable("projectId") String projectId,
                                @PathVariable("subjectId") String subjectId,
                                @RequestParam String query,
                                @RequestParam int limit,
                                @RequestParam int page,
                                @RequestParam String orderBy,
                                @RequestParam Boolean ascending,
                                @RequestParam int minimumPoints) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        SkillsValidator.isTrue(minimumPoints >=0, "Minimum Points is less than 0", projectId)

        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        return adminUsersService.loadUsersPageForSubject(projectId, subjectId, query, pageRequest, minimumPoints)
    }

    @GetMapping(value = "/projects/{projectId}/skills/{skillId}/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    TableResultWithTotalPoints getSkillUsers(@PathVariable("projectId") String projectId,
                              @PathVariable("skillId") String skillId,
                              @RequestParam String query,
                              @RequestParam int limit,
                              @RequestParam int page,
                              @RequestParam String orderBy,
                              @RequestParam Boolean ascending,
                              @RequestParam int minimumPoints) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        SkillsValidator.isTrue(minimumPoints >=0, "Minimum Points is less than 0", projectId)

        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        return adminUsersService.loadUsersPageForSkills(projectId, Collections.singletonList(skillId), query, pageRequest, minimumPoints)
    }

    @GetMapping(value = "/projects/{projectId}/badges/{badgeId}/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    TableResult getBadgeUsers(@PathVariable("projectId") String projectId,
                              @PathVariable("badgeId") String badgeId,
                              @RequestParam String query,
                              @RequestParam int limit,
                              @RequestParam int page,
                              @RequestParam String orderBy,
                              @RequestParam Boolean ascending,
                              @RequestParam int minimumPoints) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)
        SkillsValidator.isTrue(minimumPoints >=0, "Minimum Points is less than 0", projectId)

        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        List<SkillDefRes> badgeSkills = getBadgeSkills(projectId, badgeId)
        List<String> skillIds = badgeSkills.collect { it.skillId }
        if (!skillIds) {
            return new TableResult()
        }
        return adminUsersService.loadUsersPageForSkills(projectId, skillIds, query, pageRequest, minimumPoints)
    }

    @GetMapping(value = "/projects/{projectId}/userTags/{userTagKey}/{userTagValue}/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    TableResultWithTotalPoints getUserTagUsers(@PathVariable("projectId") String projectId,
                                               @PathVariable("userTagKey") String userTagKey,
                                               @PathVariable("userTagValue") String userTagValue,
                                               @RequestParam String query,
                                               @RequestParam int limit,
                                               @RequestParam int page,
                                               @RequestParam String orderBy,
                                               @RequestParam Boolean ascending) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(userTagKey, "Tag Key", projectId)
        SkillsValidator.isNotBlank(userTagValue, "Tag Value", projectId)

        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        return adminUsersService.loadUsersPageForUserTag(projectId, userTagKey, userTagValue, query, pageRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/badge/{badgeId}/skills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SkillDefRes> getBadgeSkills(@PathVariable("projectId") String projectId, @PathVariable("badgeId") String badgeId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)

        return skillsAdminService.getSkillsForBadge(projectId, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/shared/projects/{sharedProjectId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    void shareSkillToAnotherProject(@PathVariable("projectId") String projectId,
                                    @PathVariable("skillId") String skillId,
                                    @PathVariable("sharedProjectId") String sharedProjectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        SkillsValidator.isNotBlank(sharedProjectId, "Shared Project Id", projectId)
        SkillsValidator.isTrue(!skillId.toUpperCase().contains(SkillReuseIdUtil.REUSE_TAG.toUpperCase()), "Skill ID must not contain reuse tag", projectId, skillId)

        shareSkillsService.shareSkillToExternalProject(projectId, skillId, sharedProjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/shared/projects/{sharedProjectId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    void deleteSkillShare(@PathVariable("projectId") String projectId,
                          @PathVariable("skillId") String skillId,
                          @PathVariable("sharedProjectId") String sharedProjectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        SkillsValidator.isNotBlank(sharedProjectId, "Shared Project Id", projectId)

        shareSkillsService.deleteSkillShare(projectId, skillId, sharedProjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/shared", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SharedSkillResult> getSharedSkills(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return shareSkillsService.getSharedSkillsWithOtherProjects(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/sharedWithMe", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SharedSkillResult> getSharedWithMeSkills(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return shareSkillsService.getSharedSkillsFromOtherProjects(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/settings", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SettingsResult> getProjectSettings(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return settingsService.loadSettingsForProject(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/settings/{setting}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SettingsResult> getProjectSetting(@PathVariable("projectId") String projectId, @PathVariable("setting") String setting) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(setting, "Setting Id", projectId)

        SettingsResult result = settingsService.getProjectSetting(projectId, setting)
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        if(!result) {
            headers.setContentLength(0)
        }
        ResponseEntity<SettingsResult> response = new ResponseEntity<>(result, headers, HttpStatus.OK)
        return response
    }

    @RequestMapping(value = "/projects/{projectId}/settings/{setting}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult saveProjectSetting(@PathVariable("projectId") String projectId, @PathVariable("setting") String setting, @RequestBody ProjectSettingsRequest value) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(setting, "Setting Id", projectId)
        SkillsValidator.isTrue(projectId == value.projectId, "Project Id must equal", projectId)
        SkillsValidator.isTrue(setting == value.setting, "Setting Id must equal", projectId)

        projectSettingsValidator.validate(value)
        if (StringUtils.isBlank(value?.value)) {
            settingsService.deleteProjectSetting(setting)
        } else {
            settingsService.saveSetting(value)
        }
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/settings/checkValidity", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    def checkSettingsValidity(@PathVariable("projectId") String projectId, @RequestBody List<ProjectSettingsRequest> values) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotNull(values, "Settings")

        ValidationRes validationRes = settingsService.isValid(values)

        if (validationRes.isValid) {
            try {
                projectSettingsValidator.validate(values)
            } catch (SkillException ske) {
                validationRes.isValid = false
                String msg = ske.getMessage()
                validationRes.explanation = msg.replaceAll(Settings.PRODUCTION_MODE.settingName, "Discoverable").replaceAll(Settings.INVITE_ONLY_PROJECT.settingName, "Invite Only")
            }
        }

        return [
                success: true,
                valid: validationRes.isValid,
                explanation: validationRes.explanation
        ]
    }

    @RequestMapping(value = "/projects/{projectId}/settings", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult saveProjectSettings(@PathVariable("projectId") String projectId, @RequestBody List<ProjectSettingsRequest> values) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotNull(values, "Settings")

        projectSettingsValidator.validate(values)

        List<ProjectSettingsRequest> toDelete = values.findAll { StringUtils.isBlank(it.value)}
        if (toDelete) {
            settingsService.deleteProjectSettings(toDelete)
        }

        List<ProjectSettingsRequest> toSave = values.findAll { !StringUtils.isBlank(it.value)}
        if (toSave) {
            settingsService.saveSettings(toSave)
        }

        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/resetClientSecret", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult resetClientSecret(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")

        String clientSecret = new ClientSecretGenerator().generateClientSecret()
        projAdminService.updateClientSecret(projectId, clientSecret)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/clientSecret", method = [RequestMethod.GET], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    String getClientSecret(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return projAdminService.getProjectSecret(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/globalBadge/exists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean isSkillReferencedByGlobalBadge(@PathVariable("projectId") String projectId,
                                           @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id")
        return globalBadgesService.isSkillUsedInGlobalBadge(InputSanitizer.sanitize(projectId), InputSanitizer.sanitize(skillId))
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/globalBadge/exists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean isSubjectReferencedByGlobalBadge(@PathVariable("projectId") String projectId,
                                           @PathVariable("subjectId") String subjectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id")
        return globalBadgesService.isSubjectUsedInGlobalBadge(InputSanitizer.sanitize(projectId), InputSanitizer.sanitize(subjectId))
    }

    @RequestMapping(value = "/projects/{projectId}/globalBadge/exists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean isProjectReferencedByGlobalBadge(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return globalBadgesService.isProjectUsedInGlobalBadge(InputSanitizer.sanitize(projectId))
    }

    @RequestMapping(value = "/projects/{projectId}/levels/{level}/globalBadge/exists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean isProjectLevelReferencedByGlobalBadge(@PathVariable("projectId") String projectId,
                                                  @PathVariable("level") Integer level) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotNull(level, "Level")
        return globalBadgesService.isProjectLevelUsedInGlobalBadge(InputSanitizer.sanitize(projectId), level)
    }

    @RequestMapping(value = "/projects/{projectId}/errors", method = [RequestMethod.GET], produces = "application/json")
    @ResponseBody
    TableResult getErrors(@PathVariable("projectId") String projectId,
                                 @RequestParam int limit,
                                 @RequestParam int page,
                                 @RequestParam String orderBy,
                                 @RequestParam Boolean ascending) {

        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)

        return errorService.getAllErrorsForProject(projectId, pageRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/errors", method = [RequestMethod.DELETE], produces = "application/json")
    @ResponseBody
    RequestResult deleteAllErrors(@PathVariable("projectId") String projectId) {
        errorService.deleteAllErrors(projectId)
        return RequestResult.success()
    }

    @RequestMapping(value = "/projects/{projectId}/errors/{errorId}", method = [RequestMethod.DELETE], produces = "application/json")
    @ResponseBody
    RequestResult deleteProjectError(@PathVariable("projectId") String projectId, @PathVariable("errorId") Integer errorId){
        errorService.deleteError(projectId, errorId)
        return RequestResult.success()
    }


    @RequestMapping(value = "/projects/{id}/contactUsersCount", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    Integer countMatchingUsers(@PathVariable("id") String projectId, @RequestBody QueryUsersCriteriaRequest contactUsersCriteria) {
        contactUsersCriteria.projectId = projectId
        return contactUsersService.countMatchingUsers(contactUsersCriteria)
    }

    @RequestMapping(value = "/projects/{id}/contactUsers", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    RequestResult contactUsers(@PathVariable("id") String projectId, @RequestBody ContactUsersRequest contactUsersRequest) {
        contactUsersRequest?.queryCriteria?.projectId = projectId
        SkillsValidator.isNotBlank(contactUsersRequest?.emailSubject, "emailSubject")
        SkillsValidator.isNotBlank(contactUsersRequest?.emailBody, "emailBody")
        contactUsersService.contactUsers(contactUsersRequest)
        return RequestResult.success()
    }

    @RequestMapping(value="/projects/{id}/previewEmail", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    RequestResult testEmail(@RequestBody ContactUsersRequest contactUsersRequest) {
        SkillsValidator.isNotBlank(contactUsersRequest?.emailSubject, "emailSubject")
        SkillsValidator.isNotBlank(contactUsersRequest?.emailBody, "emailBody")
        String userId = userInfoService.getCurrentUserId()
        contactUsersService.sendEmail(contactUsersRequest.emailSubject, contactUsersRequest.emailBody, userId)
        return RequestResult.success()
    }

    @RequestMapping(value="/projects/{projectId}/skills/{skillId}/export", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    RequestResult exportSkillToCatalog(@PathVariable("projectId") String projectId, @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(skillId, "skillId")

        skillCatalogService.exportSkillToCatalog(projectId, skillId)
        return RequestResult.success()
    }

    @DBUpgradeSafe
    @RequestMapping(value="/projects/{projectId}/skills/catalog/exists/{skillId}", method = [RequestMethod.POST], produces = "application/json")
    ExportableToCatalogSkillValidationResult doesSkillAlreadyExistInCatalog(@PathVariable("projectId") String projectId, @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(skillId, "skillId")
        boolean isInCatalog = skillCatalogService.isAvailableInCatalog(projectId, skillId)
        if (isInCatalog) {
            return new ExportableToCatalogSkillValidationResult(skillAlreadyInCatalog: true)
        }

        boolean isSkillIdInCatalog = skillCatalogService.doesSkillIdAlreadyExistInCatalog(skillId)
        if (isSkillIdInCatalog) {
            return new ExportableToCatalogSkillValidationResult(skillIdConflictsWithExistingCatalogSkill: true)
        }

        boolean isSkillNameInCatalog = skillCatalogService.doesSkillNameAlreadyExistInCatalog(projectId, skillId)
        if (isSkillNameInCatalog) {
            return new ExportableToCatalogSkillValidationResult(skillNameConflictsWithExistingCatalogSkill: true)
        }

        return new ExportableToCatalogSkillValidationResult()
    }

    @RequestMapping(value = "/projects/{projectId}/skills/catalog/exportable", method = [RequestMethod.POST], produces = "application/json")
    ExportableToCatalogValidationResult areSkillsExportable(@PathVariable("projectId") String projectId, @RequestBody List<String> skillIds) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotEmpty(skillIds, "skillIds")

        // assume that all skills are under the same subject
        boolean hasSufficientSubjectPoints = insufficientPointsValidator.hasSufficientSubjectPointByProjectAndSkillId(projectId, skillIds[0])
        if (!hasSufficientSubjectPoints) {
            return new ExportableToCatalogValidationResult(hasSufficientSubjectPoints: hasSufficientSubjectPoints)
        }

        List<ExportableToCatalogSkillValidationResult> validationResults = skillCatalogService.canSkillIdsBeExported(projectId, skillIds)
        Map<String, ExportableToCatalogSkillValidationResult> skillsValidationRes = validationResults.collectEntries() { [it.skillId, it]}
        return new ExportableToCatalogValidationResult(skillsValidationRes: skillsValidationRes)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/catalog/exist", method = [RequestMethod.POST], produces = "application/json")
    Map<String, Boolean> doSkillsExistInCatalog(@PathVariable("projectId") String projectId, @RequestBody List<String> skillIds) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotEmpty(skillIds, "skillIds")

        Map<String, Boolean> inCatalogStatus = skillIds.collectEntries() { [it, false]}
        List<String> skillIdsInCatalog = skillCatalogService.getSkillIdsInCatalog(projectId, skillIds)

        skillIdsInCatalog?.each {
            inCatalogStatus[it] = true
        }

        return inCatalogStatus
    }

    @RequestMapping(value="/projects/{projectId}/skills/{skillId}/export", method = [RequestMethod.DELETE], produces = "application/json")
    RequestResult removeSkillFromCatalog(@PathVariable("projectId") String projectId, @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(skillId, "skillId")

        skillCatalogService.removeSkillFromCatalog(projectId, skillId)
        return RequestResult.success()
    }

    @RequestMapping(value="/projects/{projectId}/skills/export", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    RequestResult bulkExportSkillsToCatalog(@PathVariable("projectId") String projectId, @RequestBody List<String> skillIds) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isTrue(skillIds?.size() > 0, "No SkillIds specified to export to catalog", projectId)
        skillCatalogService.exportSkillToCatalog(projectId, skillIds)
        return RequestResult.success()
    }

    @RequestMapping(value="/projects/{projectId}/subjects/{subjectId}/import", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    RequestResult bulkImportSkillFromCatalog(@PathVariable("projectId") String projectId,
                                             @PathVariable("subjectId") String subjectId,
                                             @RequestBody List<CatalogSkill> bulkImport) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(subjectId, "subjectId")
        SkillsValidator.isTrue(bulkImport?.size() <= maxBulkImport, "Bulk imports are limited to no more than ${maxBulkImport} Skills at once", projectId)
        skillCatalogService.importSkillsFromCatalog(projectId, subjectId, bulkImport)
        RequestResult success = RequestResult.success()
        success.explanation = "imported [${bulkImport?.size()}] skills from the catalog into [${projectId}] - [${subjectId}]"
        return success
    }


    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/groups/{groupId}/import", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    RequestResult bulkImportSkillsUnderGroupFromCatalog(@PathVariable("projectId") String projectId,
                                                        @PathVariable("subjectId") String subjectId,
                                                        @PathVariable("groupId") String groupId,
                                                        @RequestBody List<CatalogSkill> bulkImport) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(subjectId, "subjectId")
        SkillsValidator.isNotBlank(groupId, "groupId")
        SkillsValidator.isTrue(bulkImport?.size() <= maxBulkImport, "Bulk imports are limited to no more than ${maxBulkImport} Skills at once", projectId)
        skillCatalogService.importSkillsFromCatalog(projectId, subjectId, bulkImport, groupId)
        RequestResult success = RequestResult.success()
        success.explanation = "imported [${bulkImport?.size()}] skills from the catalog into [${projectId}] - [${subjectId}] - [${groupId}]"
        return success
    }


    @RequestMapping(value = "/projects/{projectId}/import/skills/{skillId}", method = [RequestMethod.PATCH], produces = "application/json")
    RequestResult updatedImportedSkill(@PathVariable("projectId") String projectId,
                                       @PathVariable("skillId") String skillId,
                                       @RequestBody ImportedSkillUpdate update) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(skillId, "skillId")
        skillCatalogService.updateImportedSkill(projectId, skillId, update)
        return RequestResult.success()
    }

    @RequestMapping(value="/projects/{projectId}/catalog/finalize", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    RequestResult finalizeSkillsImport(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        skillCatalogService.requestFinalizationOfImportedSkills(projectId)
        RequestResult success = RequestResult.success()
        success.explanation = "Finalized skills import from the catalog into [${projectId}]"
        return success
    }

    @RequestMapping(value="/projects/{projectId}/catalog/finalize/info", method = [RequestMethod.GET], produces = "application/json")
    CatalogFinalizeInfoResult getCatalogFinalizeInfo(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        return skillCatalogService.getFinalizeInfo(projectId)
    }

    @RequestMapping(value="/projects/{projectId}/skills/catalog", method=RequestMethod.GET, produces = "application/json")
    TableResult getCatalogSkills(@PathVariable("projectId") String projectId,
                                  @RequestParam int limit,
                                  @RequestParam int page,
                                  @RequestParam String orderBy,
                                  @RequestParam Boolean ascending,
                                  @RequestParam(required=false) String projectNameSearch,
                                  @RequestParam(required=false) String subjectNameSearch,
                                  @RequestParam(required=false) String skillNameSearch) {
        TotalCountAwareResult<ProjectNameAwareSkillDefRes> res = skillCatalogService.getSkillsAvailableInCatalog(projectId,
                URLDecoder.decode(projectNameSearch, StandardCharsets.UTF_8),
                URLDecoder.decode(subjectNameSearch, StandardCharsets.UTF_8),
                URLDecoder.decode(skillNameSearch, StandardCharsets.UTF_8), createPagingRequestWithValidation(projectId, limit, page, orderBy, ascending))
        TableResult tr = new TableResult()
        tr.count = res.results?.size()
        tr.totalCount = res.total
        tr.data = res.results

        return tr
    }

    @RequestMapping(value="/projects/{projectId}/pendingFinalization/pointTotals", method=RequestMethod.GET, produces = "application/json")
    PointsIncludingNotFinalized getPointTotalsIncNotFinalized(@PathVariable("projectId") String projectId) {
        return skillCatalogService.getTotalPointsIncludingPendingFinalization(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/exported", method = RequestMethod.GET, produces = "application/json")
    TableResult getExportedSkills(@PathVariable("projectId") String projectId,
                                        @RequestParam int limit,
                                        @RequestParam int page,
                                        @RequestParam String orderBy,
                                        @RequestParam Boolean ascending) {
        TotalCountAwareResult<ExportedSkillRes> res = skillCatalogService.getSkillsExportedByProject(projectId, createPagingRequestWithValidation(projectId, limit, page, orderBy, ascending, orderBy=='importedProjectCount'))

        TableResult tr = new TableResult()
        tr.count = res.results?.size()
        tr.totalCount = res.total
        tr.data = res.results

        return tr
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/exported/stats", method = RequestMethod.GET, produces = "application/json")
    ExportedSkillStats getExportedSkillStats(@PathVariable("projectId") String projectId,
                                             @PathVariable("skillId") String skillId) {
        skillCatalogService.getExportedSkillStats(projectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/imported", method = RequestMethod.GET, produces = "application/json")
    List<SkillDefRes> getSkillsImportedFromCatalog(@PathVariable("projectId") String projectId,
                                                   @RequestParam int limit,
                                                   @RequestParam int page,
                                                   @RequestParam String orderBy,
                                                   @RequestParam Boolean ascending) {
        skillCatalogService.getSkillsImportedFromCatalog(projectId, createPagingRequestWithValidation(projectId, limit, page, orderBy, ascending))
    }

    @RequestMapping(value = "/projects/{projectId}/skills/imported/stats", method = RequestMethod.GET, produces = "application/json")
    ImportedSkillStats getImportedSkillsStats(@PathVariable("projectId") String projectId) {
        return skillCatalogService.getSkillsImportedStats(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/exported/stats", method = RequestMethod.GET, produces = "application/json")
    ExportedSkillsStats getExportedSkillsStats(@PathVariable("projectId") String projectId) {
        return skillCatalogService.getSkillsExportedStats(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    BulkSkillEventResult addSkills(@PathVariable("projectId") String projectId,
                                   @PathVariable("skillId") String skillId,
                                   @RequestBody(required = true) BulkSkillEventRequest bulkSkillEventRequest) {

        Long requestedTimestamp = bulkSkillEventRequest.timestamp
        SkillsValidator.isNotNull(requestedTimestamp, 'timestamp', projectId, skillId)
        SkillsValidator.isTrue(requestedTimestamp > 0, "timestamp must be greater than 0", projectId, skillId)
        SkillsValidator.isTrue(requestedTimestamp <= (System.currentTimeMillis() + 30000), "Skill Events may not be in the future", projectId, skillId);

        List<String> userIds = bulkSkillEventRequest.userIds
        userIds.removeAll { StringUtils.isBlank(it) }
        SkillsValidator.isNotEmpty(userIds, 'userIds', projectId, skillId)
        SkillsValidator.isTrue(userIds.size() <= maxUserIdsForBulkSkillReporting, "number of userIds cannot exceed ${maxUserIdsForBulkSkillReporting}", projectId, skillId)

        return skillEventService.bulkReportSkills(projectId, skillId, userIds, new Date(requestedTimestamp))
    }

    @RequestMapping(value = "/projects/{projectId}/skills/reuse", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    RequestResult reuseASkill(@PathVariable("projectId") String projectId,
                              @RequestBody SkillsActionRequest skillReuseRequest) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotEmpty(skillReuseRequest.skillIds, "skillReuseRequest.skillIds")
        SkillsValidator.isNotBlank(skillReuseRequest.subjectId, "skillReuseRequest.subjectId")

        skillReuseService.reuseSkill(projectId, skillReuseRequest)
        RequestResult success = RequestResult.success()
        success.explanation = "Successfully reused skills"
        return success
    }

    @RequestMapping(value = "/projects/{projectId}/reused/{parentSkillId}/skills", method = RequestMethod.GET, produces = "application/json")
    List<SkillDefSkinnyRes> getReusedSkills(@PathVariable("projectId") String projectId, @PathVariable("parentSkillId") String parentSkillId) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(parentSkillId, "parentSkillId")
        return skillReuseService.getReusedSkills(projectId, parentSkillId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/reuse/destinations", method = RequestMethod.GET, produces = "application/json")
    List<SkillReuseDestination> getReuseDestinationsForASkill(@PathVariable("projectId") String projectId, @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(skillId, "skillId")
        return skillReuseService.getReuseDestinationsForASkill(projectId, skillId)
    }


    @RequestMapping(value = "/projects/{projectId}/skills/move", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    RequestResult moveSkills(@PathVariable("projectId") String projectId,
                             @RequestBody SkillsActionRequest skillReuseRequest) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotEmpty(skillReuseRequest.skillIds, "skillReuseRequest.skillIds")
        SkillsValidator.isNotBlank(skillReuseRequest.subjectId, "skillReuseRequest.subjectId")

        skillsMoveService.moveSkills(projectId, skillReuseRequest)
        RequestResult success = RequestResult.success()
        success.explanation = "Successfully reused skills"
        return success
    }

    @RequestMapping(value = "/projects/{projectId}/lastSkillEvent", method = RequestMethod.GET, produces = "application/json")
    LatestEvent getLatestEventForProject(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        return projAdminService.getLastReportedSkillEvent(projectId)
    }

    @RequestMapping(value="/projects/{projectId}/requestAccess", method = RequestMethod.POST, produces = "application/json")
    RequestResult requestAccess(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, projectId)
        return RequestResult.success()
    }

    @RequestMapping(value = "/projects/{projectId}/skills/tag", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    RequestResult addTagToSkills(@PathVariable("projectId") String projectId,
                                 @RequestBody SkillsTagRequest skillsTagRequest) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotNull(skillsTagRequest, "skillsTagRequest", projectId)
        SkillsValidator.isNotEmpty(skillsTagRequest.skillIds, "skillsTagRequest.skillIds", projectId)
        SkillsValidator.isNotBlank(skillsTagRequest.tagId, "skillsTagRequest.tagId", projectId)
        SkillsValidator.isNotBlank(skillsTagRequest.tagValue, "skillsTagRequest.tagValue", projectId)

        skillsTagRequest.tagId = InputSanitizer.sanitize(skillsTagRequest.tagId)?.trim()?.toLowerCase()
        skillsTagRequest.tagValue = InputSanitizer.sanitize(skillsTagRequest.tagValue)?.trim()

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxSkillTagLength, "Tag Value", skillsTagRequest.tagValue)

        skillTagService.addTag(projectId, skillsTagRequest)

        RequestResult success = RequestResult.success()
        success.explanation = "Successfully tagged skills"
        return success
    }

    @RequestMapping(value = "/projects/{projectId}/skills/tags", method = [RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    List<SkillTagRes> getTagsForSkills(@PathVariable("projectId") String projectId,
                                       @RequestBody SkillIdsRequest skillsTagRequest) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotNull(skillsTagRequest, "skillsTagRequest", projectId)
        SkillsValidator.isNotEmpty(skillsTagRequest.skillIds, "skillsTagRequest.skillIds", projectId)
        return skillTagService.getTagsForSkills(projectId, skillsTagRequest.skillIds)?.collect { new SkillTagRes(tagId: it.tagId, tagValue: it.tagValue) }
    }

    @RequestMapping(value = "/projects/{projectId}/skills/tags", method = RequestMethod.GET, produces = "application/json")
    List<SkillTagRes> getTagsForProject(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        return skillTagService.getTagsForProject(projectId)?.collect { new SkillTagRes(tagId: it.tagId, tagValue: it.tagValue) }
    }

    @RequestMapping(value = "/projects/{projectId}/skills/tag", method = [RequestMethod.DELETE], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult deleteTagForSkills(@PathVariable("projectId") String projectId,
                                     @RequestBody SkillsTagRequest skillsTagRequest) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotNull(skillsTagRequest, "skillsTagRequest", projectId)
        SkillsValidator.isNotBlank(skillsTagRequest.tagId, "skillsTagRequest.tagId", projectId)
        SkillsValidator.isNotEmpty(skillsTagRequest.skillIds, "skillsTagRequest.skillIds", projectId)
        skillTagService.deleteTagForSkills(projectId, skillsTagRequest)

        RequestResult success = RequestResult.success()
        success.explanation = "Successfully removed tag from skills"
        return success
    }

    private static PageRequest createPagingRequestWithValidation(String projectId, int limit, int page, String orderBy, Boolean ascending, Boolean useUnsafeSort=false) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isTrue(limit <= 200, "Cannot ask for more than 200 items, provided=[${limit}]", projectId)
        SkillsValidator.isTrue(page >= 0, "Cannot provide negative page. provided =[${page}]", projectId)
        PageRequest pageRequest
        if (useUnsafeSort) {
            pageRequest = PageRequest.of(page - 1, limit, JpaSort.unsafe(ascending ? ASC : DESC, "(${orderBy})"))
        } else {
            pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        }

        return pageRequest
    }
}

