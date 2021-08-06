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
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import skills.PublicProps
import skills.auth.UserInfoService
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.*
import skills.controller.result.model.*
import skills.services.*
import skills.services.admin.*
import skills.services.inception.InceptionProjectService
import skills.services.settings.SettingsService
import skills.services.settings.listeners.ValidationRes
import skills.utils.ClientSecretGenerator
import skills.utils.InputSanitizer

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
    ProjAdminService projAdminService

    @Autowired
    SubjAdminService subjAdminService

    @Autowired
    BadgeAdminService badgeAdminService

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    SkillsDepsService skillsDepsService

    @Autowired
    ShareSkillsService shareSkillsService

    @Value('#{"${skills.config.ui.maxTimeWindowInMinutes}"}')
    int maxTimeWindowInMinutes

    @Autowired
    ProjectErrorService errorService

    @Autowired
    SkillEventAdminService skillEventAdminService

    @Autowired
    ContactUsersService contactUsersService

    @Autowired
    UserInfoService userInfoService


    @RequestMapping(value = "/projects/{id}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    RequestResult saveProject(@PathVariable("id") String projectId, @RequestBody skills.controller.request.model.ProjectRequest projectRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(projectRequest.projectId, "Project Id")
        SkillsValidator.isNotBlank(projectRequest.name, " Name")

        IdFormatValidator.validate(projectId)
        IdFormatValidator.validate(projectRequest.projectId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "Project Id", projectId)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "Project Id", projectId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "Project Id", projectRequest.projectId)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "Project Id", projectRequest.projectId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxProjectNameLength, "Project Name", projectRequest.name)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minNameLength, "Project Name", projectRequest.name)

        projectRequest.name = InputSanitizer.sanitize(projectRequest.name)
        projectRequest.projectId = InputSanitizer.sanitize(projectRequest.projectId)

        projAdminService.saveProject(InputSanitizer.sanitize(projectId), projectRequest)
        return new RequestResult(success: true)
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
        subjectRequest.name = InputSanitizer.sanitize(subjectRequest.name)
        subjectRequest.iconClass = InputSanitizer.sanitize(subjectRequest.iconClass)
        subjectRequest.helpUrl = InputSanitizer.sanitizeUrl(subjectRequest.helpUrl)

        subjAdminService.saveSubject(InputSanitizer.sanitize(projectId), subjectId, subjectRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/subjectNameExists", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesSubjectNameExist(@PathVariable("projectId") String projectId,
                             @RequestBody NameExistsRequest existsRequest) {
        String subjectName = existsRequest.name
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectName, "Subject Name")
        return subjAdminService.existsBySubjectName(InputSanitizer.sanitize(projectId), InputSanitizer.sanitize(subjectName))
    }

    @RequestMapping(value = "/projects/{projectId}/badgeNameExists", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesBadgeExist(@PathVariable("projectId") String projectId,
                             @RequestBody NameExistsRequest nameExistsRequest) {
        String badgeName = nameExistsRequest.name
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeName, "Badge Name")
        return badgeAdminService.existsByBadgeName(InputSanitizer.sanitize(projectId), InputSanitizer.sanitize(badgeName))
    }
    @RequestMapping(value = "/projects/{projectId}/skillNameExists", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesSkillNameExist(@PathVariable("projectId") String projectId,
                           @RequestBody NameExistsRequest existsRequest) {
        String skillName = existsRequest.name
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

        badgeRequest.name = InputSanitizer.sanitize(badgeRequest.name)
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
            @PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)

        return skillsAdminService.getSkills(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillDefRes getSkill(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId, @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        return skillsAdminService
                .getSkill(projectId, subjectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult saveSkill(@PathVariable("projectId") String projectId,
                            @PathVariable("subjectId") String subjectId,
                            @PathVariable("skillId") String skillId,
                            @RequestBody SkillRequest skillRequest) {

        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        SkillsValidator.isFirstOrMustEqualToSecond(skillRequest.projectId, projectId, "Project Id")
        skillRequest.projectId = skillRequest.projectId ?: projectId

        SkillsValidator.isFirstOrMustEqualToSecond(skillRequest.subjectId, subjectId, "Subject Id")
        skillRequest.subjectId = skillRequest.subjectId ?: subjectId
        skillRequest.skillId = skillRequest.skillId ?: skillId

        SkillsValidator.isTrue(skillRequest.pointIncrement > 0, "pointIncrement must be > 0", projectId, skillId)
        propsBasedValidator.validateMaxIntValue(PublicProps.UiProp.maxPointIncrement, "pointIncrement", skillRequest.pointIncrement)

        SkillsValidator.isTrue(skillRequest.pointIncrementInterval >= 0, "pointIncrementInterval must be >= 0", projectId, skillId)
        SkillsValidator.isTrue(skillRequest.pointIncrementInterval <= maxTimeWindowInMinutes, "pointIncrementInterval must be <= $maxTimeWindowInMinutes", projectId, skillId)
        SkillsValidator.isTrue(skillRequest.numPerformToCompletion > 0, "numPerformToCompletion must be > 0", projectId, skillId)
        propsBasedValidator.validateMaxIntValue(PublicProps.UiProp.maxNumPerformToCompletion, "numPerformToCompletion", skillRequest.numPerformToCompletion)

        if ( skillRequest.pointIncrementInterval > 0) {
            // if pointIncrementInterval is disabled then this validation is not needed
            SkillsValidator.isTrue(skillRequest.numMaxOccurrencesIncrementInterval > 0, "numMaxOccurrencesIncrementInterval must be > 0", projectId, skillId)
            SkillsValidator.isTrue(skillRequest.numPerformToCompletion >= skillRequest.numMaxOccurrencesIncrementInterval, "numPerformToCompletion must be >= numMaxOccurrencesIncrementInterval", projectId, skillId)
            propsBasedValidator.validateMaxIntValue(PublicProps.UiProp.maxNumPointIncrementMaxOccurrences, "numMaxOccurrencesIncrementInterval", skillRequest.numMaxOccurrencesIncrementInterval)
        }

        SkillsValidator.isTrue(skillRequest.version >= 0, "version must be >= 0", projectId, skillId)
        propsBasedValidator.validateMaxIntValue(PublicProps.UiProp.maxSkillVersion, "Skill Version", skillRequest.version)

        IdFormatValidator.validate(skillRequest.skillId)
        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "Skill Id", skillRequest.skillId)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "Skill Id", skillRequest.skillId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxSkillNameLength, "Skill Name", skillRequest.name)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minNameLength, "Skill Name", skillRequest.name)
        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.descriptionMaxLength, "Skill Description", skillRequest.description)

        skillRequest.name = InputSanitizer.sanitize(skillRequest.name)
        skillRequest.projectId = InputSanitizer.sanitize(skillRequest.projectId)
        skillRequest.skillId = InputSanitizer.sanitize(skillRequest.skillId)
        skillRequest.description = InputSanitizer.sanitize(skillRequest.description)
        skillRequest.subjectId = InputSanitizer.sanitize(skillRequest.subjectId)
        skillRequest.helpUrl = InputSanitizer.sanitizeUrl(skillRequest.helpUrl)

        skillsAdminService.saveSkill(skillId, skillRequest)
        return new RequestResult(success: true)
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

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/{dependentSkillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult assignDependency(@PathVariable("projectId") String projectId,
                                   @PathVariable("skillId") String skillId,
                                   @PathVariable("dependentSkillId") String dependentSkillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        SkillsValidator.isNotBlank(dependentSkillId, "Dependent Skill Id", projectId)

        skillsDepsService.assignSkillDependency(projectId, skillId, dependentSkillId)
        return new RequestResult(success: true)
    }


    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/projects/{dependentProjectId}/skills/{dependentSkillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult assignDependencyFromAnotherProject(@PathVariable("projectId") String projectId,
                                                     @PathVariable("skillId") String skillId,
                                                     @PathVariable("dependentProjectId") String dependentProjectId,
                                                     @PathVariable("dependentSkillId") String dependentSkillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        SkillsValidator.isNotBlank(dependentProjectId, "Dependent Project Id", projectId)
        SkillsValidator.isNotBlank(dependentSkillId, "Dependent Skill Id", projectId)

        skillsDepsService.assignSkillDependency(projectId, skillId, dependentSkillId, dependentProjectId)
        return new RequestResult(success: true)
    }


    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/{dependentSkillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult removeDependency(@PathVariable("projectId") String projectId,
                                   @PathVariable("skillId") String skillId,
                                   @PathVariable("dependentSkillId") String dependentSkillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        SkillsValidator.isNotBlank(dependentSkillId, "Dependent Skill Id", projectId)

        skillsDepsService.removeSkillDependency(projectId, skillId, dependentSkillId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/projects/{dependentProjectId}/skills/{dependentSkillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult removeDependencyFromAnotherProject(@PathVariable("projectId") String projectId,
                                                     @PathVariable("skillId") String skillId,
                                                     @PathVariable("dependentProjectId") String dependentProjectId,
                                                     @PathVariable("dependentSkillId") String dependentSkillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        SkillsValidator.isNotBlank(dependentProjectId, "Dependent Project Id", projectId)
        SkillsValidator.isNotBlank(dependentSkillId, "Dependent Skill Id", projectId)

        skillsDepsService.removeSkillDependency(projectId, skillId, dependentSkillId, dependentProjectId)
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

        skillsAdminService.deleteSkill(projectId, subjectId, skillId)
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
            @PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")

        List<SkillDefSkinnyRes> res = skillsAdminService.getSkinnySkills(projectId)
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
    TableResult getProjectUsers(@PathVariable("projectId") String projectId,
                                @RequestParam String query,
                                @RequestParam int limit,
                                @RequestParam int page,
                                @RequestParam String orderBy,
                                @RequestParam Boolean ascending) {
        SkillsValidator.isNotBlank(projectId, "Project Id")

        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        return adminUsersService.loadUsersPage(projectId, query, pageRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/users/{userId}", method = RequestMethod.GET)
    UserInfoRes getUserInfo(
            @PathVariable("projectId") String projectId,
            @PathVariable("userId") String userId) {
        return adminUsersService.getUserForProject(projectId, userId?.toLowerCase())
    }

    @GetMapping(value = "/projects/{projectId}/subjects/{subjectId}/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    TableResult getSubjectUsers(@PathVariable("projectId") String projectId,
                                @PathVariable("subjectId") String subjectId,
                                @RequestParam String query,
                                @RequestParam int limit,
                                @RequestParam int page,
                                @RequestParam String orderBy,
                                @RequestParam Boolean ascending) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)

        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        List<SkillDefPartialRes> subjectSkills = getSkills(projectId, subjectId)
        List<String> skillIds = subjectSkills.collect { it.skillId }
        return adminUsersService.loadUsersPage(projectId, skillIds, query, pageRequest)
    }

    @GetMapping(value = "/projects/{projectId}/skills/{skillId}/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    TableResult getSkillUsers(@PathVariable("projectId") String projectId,
                              @PathVariable("skillId") String skillId,
                              @RequestParam String query,
                              @RequestParam int limit,
                              @RequestParam int page,
                              @RequestParam String orderBy,
                              @RequestParam Boolean ascending) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        return adminUsersService.loadUsersPage(projectId, Collections.singletonList(skillId), query, pageRequest)
    }

    @GetMapping(value = "/projects/{projectId}/badges/{badgeId}/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    TableResult getBadgeUsers(@PathVariable("projectId") String projectId,
                              @PathVariable("badgeId") String badgeId,
                              @RequestParam String query,
                              @RequestParam int limit,
                              @RequestParam int page,
                              @RequestParam String orderBy,
                              @RequestParam Boolean ascending) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)

        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        List<SkillDefRes> badgeSkills = getBadgeSkills(projectId, badgeId)
        List<String> skillIds = badgeSkills.collect { it.skillId }
        if (!skillIds) {
            return new TableResult()
        }
        return adminUsersService.loadUsersPage(projectId, skillIds, query, pageRequest)
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

    @RequestMapping(value = "/projects/{projectId}/errors/{errorType}/{error}", method = [RequestMethod.DELETE], produces = "application/json")
    @ResponseBody
    RequestResult deleteProjectError(@PathVariable("projectId") String projectId, @PathVariable("errorType") String errorType, @PathVariable("error") String error){
        errorService.deleteError(projectId, errorType, error)
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
        contactUsersService.previewEmail(contactUsersRequest.emailSubject, contactUsersRequest.emailBody, userId)
        return RequestResult.success()
    }

}

