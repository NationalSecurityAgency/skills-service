package skills.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.BadgeRequest
import skills.controller.request.model.EditLevelRequest
import skills.controller.request.model.NextLevelRequest
import skills.controller.request.model.ProjectSettingsRequest
import skills.controller.request.model.SettingsRequest
import skills.controller.request.model.SkillDefForDependencyRes
import skills.controller.request.model.SkillRequest
import skills.controller.request.model.SubjectRequest
import skills.controller.result.model.BadgeResult
import skills.controller.result.model.LevelDefinitionRes
import skills.controller.result.model.NumUsersRes
import skills.controller.result.model.ProjectResult
import skills.controller.result.model.RequestResult
import skills.controller.result.model.SettingsResult
import skills.controller.result.model.SharedSkillResult
import skills.controller.result.model.SimpleProjectResult
import skills.controller.result.model.SkillDefRes
import skills.controller.result.model.SkillDefPartialRes
import skills.controller.result.model.SkillDefSkinnyRes
import skills.controller.result.model.SkillsGraphRes
import skills.controller.result.model.SubjectResult
import skills.controller.result.model.TableResult
import skills.controller.result.model.UserSkillsStats
import skills.services.AdminProjService
import skills.services.AdminUsersService
import skills.services.LevelDefinitionStorageService
import skills.services.SkillEventAdminService
import skills.services.UserAdminService
import skills.services.events.SkillEventResult
import skills.services.settings.SettingsService
import skills.services.settings.listeners.ValidationRes
import skills.utils.ClientSecretGenerator
import skills.utils.Constants

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
    AdminProjService projectAdminStorageService

    @Autowired
    AdminUsersService adminUsersService

    @Autowired
    UserAdminService userAdminService

    @Autowired
    SettingsService settingsService

    @Autowired
    SkillEventAdminService skillEventService

    @Value('#{"${skills.config.ui.maxTimeWindowInMinutes}"}')
    int maxTimeWindowInMinutes

    @RequestMapping(value = "/projects/{id}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    RequestResult saveProject(@PathVariable("id") String projectId, @RequestBody skills.controller.request.model.ProjectRequest projectRequest) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectRequest.projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectRequest.name, " Name")
        projectAdminStorageService.saveProject(projectId, projectRequest)
        return new skills.controller.result.model.RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{id}", method = RequestMethod.DELETE)
    void deleteProject(@PathVariable("id") String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        projectAdminStorageService.deleteProject(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult setProjectDisplayOrder(
            @PathVariable("projectId") String projectId, @RequestBody ActionPatchRequest projectPatchRequest) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotNull(projectPatchRequest.action, "Action", projectId)

        projectAdminStorageService.setProjectDisplayOrder(projectId, projectPatchRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ProjectResult getProject(@PathVariable("id") String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.getProject(projectId)
    }

    @RequestMapping(value = "/projects/{id}/projectSearch", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SimpleProjectResult> searchProjects(@PathVariable("id") String projectId, @RequestParam("nameQuery") nameQuery) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.searchProjects(projectId, nameQuery)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult saveSubject(@PathVariable("projectId") String projectId,
                              @PathVariable("subjectId") String subjectId,
                              @RequestBody SubjectRequest subjectRequest) {
        subjectRequest.subjectId = subjectRequest.subjectId ?: subjectId
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(subjectRequest?.name, "Subject name", projectId)

        projectAdminStorageService.saveSubject(projectId, subjectId, subjectRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/subjectNameExists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesSubjectNameExist(@PathVariable("projectId") String projectId,
                             @RequestParam(value = "subjectName", required = false) String subjectName) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(subjectName, "Subject Name")
        String decodedName = URLDecoder.decode(subjectName,  StandardCharsets.UTF_8.toString())
        return projectAdminStorageService.existsBySubjectName(projectId, decodedName)
    }

    @RequestMapping(value = "/projects/{projectId}/badgeNameExists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesBadgeExist(@PathVariable("projectId") String projectId,
                             @RequestParam(value = "badgeName", required = false) String badgeName) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(badgeName, "Badge Name")
        String decodedName = URLDecoder.decode(badgeName,  StandardCharsets.UTF_8.toString())
        return projectAdminStorageService.existsByBadgeName(projectId, decodedName)
    }
    @RequestMapping(value = "/projects/{projectId}/skillNameExists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesSkillNameExist(@PathVariable("projectId") String projectId,
                           @RequestParam(value = "skillName", required = false) String skillName) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Skill Name")
        String decodedName = URLDecoder.decode(skillName,  StandardCharsets.UTF_8.toString())
        return projectAdminStorageService.existsBySkillName(projectId, decodedName)
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
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(id, "Entity Id")

        return projectAdminStorageService.existsBySkillId(projectId, id)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = RequestMethod.DELETE)
    void deleteSubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)

        projectAdminStorageService.deleteSubject(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SubjectResult> getSubjects(@PathVariable("projectId") String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.getSubjects(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SubjectResult getSubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        return projectAdminStorageService.getSubject(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult setSubjectDisplayOrder(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId, @RequestBody ActionPatchRequest subjectPatchRequest) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotNull(subjectPatchRequest.action, "Action must be provided", projectId)
        projectAdminStorageService.setSubjectDisplayOrder(projectId, subjectId, subjectPatchRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult saveBadge(@PathVariable("projectId") String projectId,
                            @PathVariable("badgeId") String badgeId,
                            @RequestBody BadgeRequest badgeRequest) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)
        badgeRequest.badgeId = badgeRequest.badgeId ?: badgeId
        skills.controller.exceptions.SkillsValidator.isNotBlank(badgeRequest?.name, "Badge Name", projectId)
        skills.controller.exceptions.SkillsValidator.isTrue((badgeRequest.startDate && badgeRequest.endDate) || (!badgeRequest.startDate && !badgeRequest.endDate),
                "If one date is provided then both start and end dates must be provided", projectId)

        projectAdminStorageService.saveBadge(projectId, badgeId, badgeRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/badge/{badgeId}/skills/{skillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult assignSkillToBadge(@PathVariable("projectId") String projectId,
                                     @PathVariable("badgeId") String badgeId,
                                     @PathVariable("skillId") String skillId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        projectAdminStorageService.addSkillToBadge(projectId, badgeId, skillId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/badge/{badgeId}/skills/{skillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult removeSkillFromBadge(@PathVariable("projectId") String projectId,
                                       @PathVariable("badgeId") String badgeId,
                                       @PathVariable("skillId") String skillId) {
        projectAdminStorageService.removeSkillFromBadge(projectId, badgeId, skillId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.DELETE)
    void deleteBadge(@PathVariable("projectId") String projectId, @PathVariable("badgeId") String badgeId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)

        projectAdminStorageService.deleteBadge(projectId, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<BadgeResult> getBadges(@PathVariable("projectId") String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")

        return projectAdminStorageService.getBadges(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    BadgeResult getBadge(@PathVariable("projectId") String projectId,
                         @PathVariable("badgeId") String badgeId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)

        return projectAdminStorageService.getBadge(projectId, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void setBadgeDisplayOrder(
            @PathVariable("projectId") String projectId,
            @PathVariable("badgeId") String badgeId, @RequestBody ActionPatchRequest badgePatchRequest) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotNull(badgePatchRequest.action, "Action must be provided", projectId)

        projectAdminStorageService.setBadgeDisplayOrder(projectId, badgeId, badgePatchRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SkillDefPartialRes> getSkills(
            @PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)

        return projectAdminStorageService.getSkills(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillDefRes getSkill(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId, @PathVariable("skillId") String skillId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        return projectAdminStorageService.getSkill(projectId, subjectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult saveSkill(@PathVariable("projectId") String projectId,
                            @PathVariable("subjectId") String subjectId,
                            @PathVariable("skillId") String skillId,
                            @RequestBody SkillRequest skillRequest) {

        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        skills.controller.exceptions.SkillsValidator.isFirstOrMustEqualToSecond(skillRequest.projectId, projectId, "Project Id")
        skillRequest.projectId = skillRequest.projectId ?: projectId

        skills.controller.exceptions.SkillsValidator.isFirstOrMustEqualToSecond(skillRequest.subjectId, subjectId, "Subject Id")
        skillRequest.subjectId = skillRequest.subjectId ?: subjectId
        skillRequest.skillId = skillRequest.skillId ?: skillId

        skills.controller.exceptions.SkillsValidator.isTrue(skillRequest.pointIncrement > 0, "pointIncrement must be > 0", projectId, skillId)
        skills.controller.exceptions.SkillsValidator.isTrue(skillRequest.pointIncrementInterval >= 0, "pointIncrementInterval must be >= 0", projectId, skillId)
        skills.controller.exceptions.SkillsValidator.isTrue(skillRequest.pointIncrementInterval <= maxTimeWindowInMinutes, "pointIncrementInterval must be <= $maxTimeWindowInMinutes", projectId, skillId)
        skills.controller.exceptions.SkillsValidator.isTrue(skillRequest.numPerformToCompletion > 0, "numPerformToCompletion must be > 0", projectId, skillId)
        skills.controller.exceptions.SkillsValidator.isTrue(skillRequest.numPerformToCompletion <= 10000, "numPerformToCompletion must be <= 10000", projectId, skillId)

        if ( skillRequest.pointIncrementInterval > 0) {
            // if pointIncrementInterval is disabled then this validation is not needed
            skills.controller.exceptions.SkillsValidator.isTrue(skillRequest.numMaxOccurrencesIncrementInterval > 0, "numMaxOccurrencesIncrementInterval must be > 0", projectId, skillId)
            skills.controller.exceptions.SkillsValidator.isTrue(skillRequest.numPerformToCompletion >= skillRequest.numMaxOccurrencesIncrementInterval, "numPerformToCompletion must be >= numMaxOccurrencesIncrementInterval", projectId, skillId)
        }

        skills.controller.exceptions.SkillsValidator.isTrue(skillRequest.version >= 0, "version must be >= 0", projectId, skillId)
        skills.controller.exceptions.SkillsValidator.isTrue(skillRequest.version < Constants.MAX_VERSION, "version exceeds max version", projectId, skillId)

        projectAdminStorageService.saveSkill(skillId, skillRequest)
        return new RequestResult(success: true)
    }

    @GetMapping(value = '/projects/{projectId}/latestVersion', produces = 'application/json')
    Integer findLatestSkillVersion(@PathVariable('projectId') String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.findLatestSkillVersion(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/dependency/availableSkills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SkillDefForDependencyRes> getSkillsAvailableForDependency(@PathVariable("projectId") String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.getSkillsAvailableForDependency(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/{dependentSkillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult assignDependency(@PathVariable("projectId") String projectId,
                                   @PathVariable("skillId") String skillId,
                                   @PathVariable("dependentSkillId") String dependentSkillId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(dependentSkillId, "Dependent Skill Id", projectId)

        projectAdminStorageService.assignSkillDependency(projectId, skillId, dependentSkillId)
        return new RequestResult(success: true)
    }


    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/projects/{dependentProjectId}/skills/{dependentSkillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult assignDependencyFromAnotherProject(@PathVariable("projectId") String projectId,
                                                     @PathVariable("skillId") String skillId,
                                                     @PathVariable("dependentProjectId") String dependentProjectId,
                                                     @PathVariable("dependentSkillId") String dependentSkillId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(dependentProjectId, "Dependent Project Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(dependentSkillId, "Dependent Skill Id", projectId)

        projectAdminStorageService.assignSkillDependency(projectId, skillId, dependentSkillId, dependentProjectId)
        return new RequestResult(success: true)
    }


    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/{dependentSkillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult removeDependency(@PathVariable("projectId") String projectId,
                                   @PathVariable("skillId") String skillId,
                                   @PathVariable("dependentSkillId") String dependentSkillId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(dependentSkillId, "Dependent Skill Id", projectId)

        projectAdminStorageService.removeSkillDependency(projectId, skillId, dependentSkillId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/projects/{dependentProjectId}/skills/{dependentSkillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult removeDependencyFromAnotherProject(@PathVariable("projectId") String projectId,
                                                     @PathVariable("skillId") String skillId,
                                                     @PathVariable("dependentProjectId") String dependentProjectId,
                                                     @PathVariable("dependentSkillId") String dependentSkillId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(dependentProjectId, "Dependent Project Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(dependentSkillId, "Dependent Skill Id", projectId)

        projectAdminStorageService.removeSkillDependency(projectId, skillId, dependentSkillId, dependentProjectId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/graph", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillsGraphRes getDependencyForSkill(@PathVariable("projectId") String projectId,
                                         @PathVariable("skillId") String skillId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        return projectAdminStorageService.getDependentSkillsGraph(projectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/dependency/graph", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillsGraphRes getDependencyForProject(@PathVariable("projectId") String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")

        return projectAdminStorageService.getDependentSkillsGraph(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = RequestMethod.PATCH)
    @ResponseBody
    RequestResult updateSkillDisplayOrder(@PathVariable("projectId") String projectId,
                                          @PathVariable("subjectId") String subjectId,
                                          @PathVariable("skillId") String skillId,
                                          @RequestBody ActionPatchRequest patchRequest) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotNull(patchRequest.action, "Action must be provided", projectId)

        projectAdminStorageService.updateSkillDisplayOrder(projectId, subjectId, skillId, patchRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void deleteSkill(@PathVariable("projectId") String projectId,
                     @PathVariable("subjectId") String subjectId,
                     @PathVariable("skillId") String skillId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        projectAdminStorageService.deleteSkill(projectId, subjectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/users/{userId}/events/{timestamp}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillEventResult deleteSkillEvent(@PathVariable("projectId") String projectId,
                                      @PathVariable("skillId") String skillId,
                                      @PathVariable("userId") String userId,
                                      @PathVariable("timestamp") Long timestamp) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotNull(skillId, "Skill Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotNull(userId, "User Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotNull(timestamp, "Timestamp", projectId)

        return skillEventService.deleteSkillEvent(projectId, skillId, userId, timestamp)
    }

    @RequestMapping(value = "/projects/{projectId}/skills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SkillDefSkinnyRes> getAllSkillsForProject(
            @PathVariable("projectId") String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")

        List<SkillDefSkinnyRes> res = projectAdminStorageService.getSkinnySkills(projectId)
        return res
    }


    @RequestMapping(value = "/projects/{projectId}/levels", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<LevelDefinitionRes> getLevels(
            @PathVariable("projectId") String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")

        List<LevelDefinitionRes> res = levelDefinitionStorageService.getLevels(projectId)
        return res
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/levels", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<LevelDefinitionRes> getLevels(
            @PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)

        List<LevelDefinitionRes> res = levelDefinitionStorageService.getLevels(projectId, subjectId)
        return res
    }

    @RequestMapping(value = "/projects/{projectId}/levels/last", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    void deleteLastLevel(@PathVariable("projectId") String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")

        levelDefinitionStorageService.deleteLastLevel(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/levels/next", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult addNextLevel(@PathVariable("projectId") String projectId, @RequestBody NextLevelRequest nextLevelRequest) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        levelDefinitionStorageService.addNextLevel(projectId, nextLevelRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/levels/last", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    void deleteLastLevel(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
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
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotNull(level, "Level", projectId)

        levelDefinitionStorageService.editLevel(projectId, editLevelRequest, level, subjectId)
        return new RequestResult(success: true)
    }

    //Need new methods to edit existing level methods for project and subject
    @RequestMapping(value = "/projects/{projectId}/levels/edit/{level}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult editLevel(@PathVariable("projectId") String projectId,
                            @PathVariable("level") Integer level, @RequestBody EditLevelRequest editLevelRequest) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotNull(level, "Level", projectId)
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
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(userId, "User Id", projectId)

        PageRequest pageRequest = new PageRequest(page - 1, limit, ascending ? ASC : DESC, orderBy)
        return userAdminService.loadUserPerformedSkillsPage(projectId, userId, query, pageRequest)
    }

    @GetMapping(value = '/projects/{projectId}/users/count', produces = 'application/json')
    @ResponseBody
    @CompileStatic
    NumUsersRes getUserSkillsStats(@PathVariable('projectId') String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.getNumUsersByProjectId(projectId)
    }

    @GetMapping(value = '/projects/{projectId}/users/{userId}/stats', produces = 'application/json')
    @ResponseBody
    @CompileStatic
    UserSkillsStats getUserSkillsStats(@PathVariable('projectId') String projectId,
                                       @PathVariable('userId') String userId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(userId, "User Id", projectId)

        return userAdminService.getUserSkillsStats(projectId, userId)
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
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")

        PageRequest pageRequest = new PageRequest(page - 1, limit, ascending ? ASC : DESC, orderBy)
        return adminUsersService.loadUsersPage(projectId, query, pageRequest)
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
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)

        PageRequest pageRequest = new PageRequest(page - 1, limit, ascending ? ASC : DESC, orderBy)
        List<SkillDefRes> subjectSkills = getSkills(projectId, subjectId)
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
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        PageRequest pageRequest = new PageRequest(page - 1, limit, ascending ? ASC : DESC, orderBy)
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
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)

        PageRequest pageRequest = new PageRequest(page - 1, limit, ascending ? ASC : DESC, orderBy)
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
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)

        return projectAdminStorageService.getSkillsForBadge(projectId, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/shared/projects/{sharedProjectId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    void shareSkillToAnotherProject(@PathVariable("projectId") String projectId,
                                    @PathVariable("skillId") String skillId,
                                    @PathVariable("sharedProjectId") String sharedProjectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(sharedProjectId, "Shared Project Id", projectId)

        projectAdminStorageService.shareSkillToExternalProject(projectId, skillId, sharedProjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/shared/projects/{sharedProjectId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    void deleteSkillShare(@PathVariable("projectId") String projectId,
                          @PathVariable("skillId") String skillId,
                          @PathVariable("sharedProjectId") String sharedProjectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        skills.controller.exceptions.SkillsValidator.isNotBlank(sharedProjectId, "Shared Project Id", projectId)

        projectAdminStorageService.deleteSkillShare(projectId, skillId, sharedProjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/shared", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SharedSkillResult> getSharedSkills(@PathVariable("projectId") String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.getSharedSkillsWithOtherProjects(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/sharedWithMe", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SharedSkillResult> getSharedWithMeSkills(@PathVariable("projectId") String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.getSharedSkillsFromOtherProjects(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/settings", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SettingsResult> getProjectSettings(@PathVariable("projectId") String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        return settingsService.loadSettingsForProject(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/settings/{setting}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    SettingsResult getProjectSetting(@PathVariable("projectId") String projectId, @PathVariable("setting") String setting) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(setting, "Setting Id", projectId)
        return settingsService.getProjectSetting(projectId, setting)
    }

    @RequestMapping(value = "/projects/{projectId}/settings/{setting}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult saveProjectSetting(@PathVariable("projectId") String projectId, @PathVariable("setting") String setting, @RequestBody ProjectSettingsRequest value) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(setting, "Setting Id", projectId)
        skills.controller.exceptions.SkillsValidator.isTrue(projectId == value.projectId, "Project Id must equal", projectId)
        skills.controller.exceptions.SkillsValidator.isTrue(setting == value.setting, "Setting Id must equal", projectId)

        settingsService.saveSetting(value)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/settings/checkValidity", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    def checkSettingsValidity(@PathVariable("projectId") String projectId, @RequestBody List<ProjectSettingsRequest> values) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotNull(values, "Settings")

        ValidationRes validationRes = settingsService.isValid(values)
        return [
                success: true,
                valid: validationRes.isValid,
                explanation: validationRes.explanation
        ]
    }

    @RequestMapping(value = "/projects/{projectId}/settings", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult saveProjectSettings(@PathVariable("projectId") String projectId, @RequestBody List<ProjectSettingsRequest> values) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotNull(values, "Settings")

        settingsService.saveSettings(values)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/resetClientSecret", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult resetClientSecret(@PathVariable("projectId") String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")

        String clientSecret = new ClientSecretGenerator().generateClientSecret()
        projectAdminStorageService.updateClientSecret(projectId, clientSecret)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/clientSecret", method = [RequestMethod.GET], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    String getClientSecret(@PathVariable("projectId") String projectId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.getProjectSecret(projectId)
    }
}
