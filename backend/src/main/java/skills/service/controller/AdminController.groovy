package skills.service.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import skills.service.controller.exceptions.SkillsValidator
import skills.service.controller.request.model.*
import skills.service.controller.result.model.*
import skills.service.datastore.services.AdminProjService
import skills.service.datastore.services.AdminUsersService
import skills.service.datastore.services.LevelDefinitionStorageService
import skills.service.datastore.services.UserAdminService
import skills.service.datastore.services.settings.SettingsService
import skills.service.skillsManagement.SkillsManagementFacade
import skills.utils.ClientSecretGenerator
import skills.utils.Constants

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

@RestController
@RequestMapping("/admin")
@Slf4j
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
    SkillsManagementFacade skillsManagementFacade

    @RequestMapping(value = "/projects/{id}", method = RequestMethod.DELETE)
    void deleteProject(@PathVariable("id") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        projectAdminStorageService.deleteProject(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult setProjectDisplayOrder(
            @PathVariable("projectId") String projectId, @RequestBody ActionPatchRequest projectPatchRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotNull(projectPatchRequest.action, "Action", projectId)

        projectAdminStorageService.setProjectDisplayOrder(projectId, projectPatchRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ProjectResult getProject(@PathVariable("id") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.getProject(projectId)
    }

    @RequestMapping(value = "/projects/{id}/projectSearch", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SimpleProjectResult> searchProjects(@PathVariable("id") String projectId, @RequestParam("nameQuery") nameQuery) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.searchProjects(nameQuery)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult saveSubject(@PathVariable("projectId") String projectId,
                              @PathVariable("subjectId") String subjectId,
                              @RequestBody SubjectRequest subjectRequest) {
        SkillsValidator.isFirstOrMustEqualToSecond(subjectRequest.subjectId, subjectId, "Subject Id")
        subjectRequest.subjectId = subjectRequest.subjectId ?: subjectId
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        SkillsValidator.isNotBlank(subjectRequest?.name, "Subject name", projectId)

        projectAdminStorageService.saveSubject(projectId, subjectRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/subjectExists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesSubjectExist(@PathVariable("projectId") String projectId,
                             @RequestParam(value = "subjectId", required = false) String subjectId,
                             @RequestParam(value = "subjectName", required = false) String subjectName) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isTrue(subjectId || subjectName, 'Subject Id and Subject Name must be specified', projectId)
        SkillsValidator.isTrue(!(subjectId && subjectName), 'Both Subject Id and Subject Name can NOT be provided', projectId)

        if (subjectId) {
            return projectAdminStorageService.existsBySubjectId(projectId, subjectId)
        }

        return projectAdminStorageService.existsBySubjectName(projectId, subjectName)
    }

    @RequestMapping(value = "/projects/{projectId}/skillExists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesSkillExist(@PathVariable("projectId") String projectId,
                           @RequestParam(value = "skillId", required = false) String skillId,
                           @RequestParam(value = "skillName", required = false) String skillName) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isTrue(skillId || skillName, 'Skill Id and Skill Name must be specified', projectId)
        SkillsValidator.isTrue(!(skillId && skillName), 'Both Skill Id and Skill Name can NOT be provided', projectId)

        if (skillId) {
            return projectAdminStorageService.existsBySkillId(projectId, skillId)
        }

        return projectAdminStorageService.existsBySkillName(projectId, skillName)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = RequestMethod.DELETE)
    void deleteSubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)

        projectAdminStorageService.deleteSubject(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SubjectResult> getSubjects(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.getSubjects(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SubjectResult getSubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        return projectAdminStorageService.getSubject(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult setSubjectDisplayOrder(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId, @RequestBody ActionPatchRequest subjectPatchRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        SkillsValidator.isNotNull(subjectPatchRequest.action, "Action must be provided", projectId)
        projectAdminStorageService.setSubjectDisplayOrder(projectId, subjectId, subjectPatchRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult saveBadge(@PathVariable("projectId") String projectId,
                            @PathVariable("badgeId") String badgeId,
                            @RequestBody BadgeRequest badgeRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)
        SkillsValidator.isFirstOrMustEqualToSecond(badgeRequest.badgeId, badgeId, "Badge Id")
        badgeRequest.badgeId = badgeRequest.badgeId ?: badgeId
        SkillsValidator.isNotBlank(badgeRequest?.name, "Badge Name", projectId)
        SkillsValidator.isTrue((badgeRequest.startDate && badgeRequest.endDate) || (!badgeRequest.startDate && !badgeRequest.endDate),
                "If one date is provided then both start and end dates must be provided", projectId)

        projectAdminStorageService.saveBadge(projectId, badgeRequest)
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

        projectAdminStorageService.addSkillToBadge(projectId, badgeId, skillId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/badge/{badgeId}/skills/{skillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult removeSkillFromBadge(@PathVariable("projectId") String projectId,
                                       @PathVariable("badgeId") String badgeId,
                                       @PathVariable("skillId") String skillId) {
        projectAdminStorageService.removeSkillFromBadge(projectId, badgeId, skillId)
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.DELETE)
    void deleteBadge(@PathVariable("projectId") String projectId, @PathVariable("badgeId") String badgeId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)

        projectAdminStorageService.deleteBadge(projectId, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<BadgeResult> getBadges(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")

        return projectAdminStorageService.getBadges(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    BadgeResult getBadge(@PathVariable("projectId") String projectId,
                         @PathVariable("badgeId") String badgeId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)

        return projectAdminStorageService.getBadge(projectId, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void setBadgeDisplayOrder(
            @PathVariable("projectId") String projectId,
            @PathVariable("badgeId") String badgeId, @RequestBody ActionPatchRequest badgePatchRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)
        SkillsValidator.isNotNull(badgePatchRequest.action, "Action must be provided", projectId)

        projectAdminStorageService.setBadgeDisplayOrder(projectId, badgeId, badgePatchRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SkillDefRes> getSkills(
            @PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)

        return projectAdminStorageService.getSkills(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillDefRes getSkill(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId, @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        return projectAdminStorageService.getSkill(projectId, subjectId, skillId)
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

        SkillsValidator.isFirstOrMustEqualToSecond(skillRequest.skillId, skillId, "Skill Id")
        skillRequest.skillId = skillRequest.skillId ?: skillId

        SkillsValidator.isTrue(skillRequest.pointIncrement > 0, "pointIncrement must be > 0", projectId, skillId)
        SkillsValidator.isTrue(skillRequest.pointIncrementInterval > 0, "pointIncrementInterval must be > 0", projectId, skillId)
        SkillsValidator.isTrue(skillRequest.numPerformToCompletion > 0, "numPerformToCompletion must be > 0", projectId, skillId)
        SkillsValidator.isTrue(skillRequest.version >= 0, "version must be >= 0", projectId, skillId)
        SkillsValidator.isTrue(skillRequest.version < Constants.MAX_VERSION, "version exceeds max version", projectId, skillId)

        projectAdminStorageService.saveSkill(skillRequest)
        return new RequestResult(success: true)
    }

    @GetMapping(value = '/projects/{projectId}/latestVersion', produces = 'application/json')
    Integer findLatestSkillVersion(@PathVariable('projectId') String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.findLatestSkillVersion(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/dependency/availableSkills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SkillDefForDependencyRes> getSkillsAvailableForDependency(@PathVariable("projectId") String projectId, @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.getSkillsAvailableForDependency(projectId, version)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/{dependentSkillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult assignDependency(@PathVariable("projectId") String projectId,
                                   @PathVariable("skillId") String skillId,
                                   @PathVariable("dependentSkillId") String dependentSkillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        SkillsValidator.isNotBlank(dependentSkillId, "Dependent Skill Id", projectId)

        projectAdminStorageService.assignSkillDependency(projectId, skillId, dependentSkillId)
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

        projectAdminStorageService.assignSkillDependency(projectId, skillId, dependentSkillId, dependentProjectId)
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

        projectAdminStorageService.removeSkillDependency(projectId, skillId, dependentSkillId)
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

        projectAdminStorageService.removeSkillDependency(projectId, skillId, dependentSkillId, dependentProjectId)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/graph", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillsGraphRes getDependencyForSkill(@PathVariable("projectId") String projectId,
                                         @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

        return projectAdminStorageService.getDependentSkillsGraph(projectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/dependency/graph", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillsGraphRes getDependencyForProject(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")

        return projectAdminStorageService.getDependentSkillsGraph(projectId)
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

        projectAdminStorageService.updateSkillDisplayOrder(projectId, subjectId, skillId, patchRequest)
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

        projectAdminStorageService.deleteSkill(projectId, subjectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillEventId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillsManagementFacade.SkillEventResult deleteSkillEvent(@PathVariable("projectId") String projectId,
                                                             @PathVariable("skillEventId") Integer skillEventId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotNull(skillEventId, "Skill Event Id", "$skillEventId")

        return skillsManagementFacade.deleteSkillEvent(skillEventId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SkillDefRes> getAllSkillsForProject(
            @PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")

        List<SkillDefRes> res = projectAdminStorageService.getSkills(projectId)
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
    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/levels/edit/{levelId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult editLevel(@PathVariable("projectId") String projectId,
                            @PathVariable("subjectId") String subjectId,
                            @PathVariable("levelId") String levelId, @RequestBody EditLevelRequest editLevelRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)
        SkillsValidator.isNotBlank(levelId, "Level Id", projectId)

        levelDefinitionStorageService.editLevel(projectId, editLevelRequest, levelId, subjectId)
        return new RequestResult(success: true)
    }

    //Need new methods to edit existing level methods for project and subject
    @RequestMapping(value = "/projects/{projectId}/levels/edit/{levelId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult editLevel(@PathVariable("projectId") String projectId,
                            @PathVariable("levelId") String levelId, @RequestBody EditLevelRequest editLevelRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(levelId, "Level Id", projectId)
        levelDefinitionStorageService.editLevel(projectId, editLevelRequest, levelId)
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

        PageRequest pageRequest = new PageRequest(page - 1, limit, ascending ? ASC : DESC, orderBy)
        return userAdminService.loadUserPerformedSkillsPage(projectId, userId, query, pageRequest)
    }

    @GetMapping(value = '/projects/{projectId}/users/{userId}/metrics', produces = 'application/json')
    @ResponseBody
    @CompileStatic
    UserSkillsMetrics getUserSkillsMetrics(@PathVariable('projectId') String projectId,
                               @PathVariable('userId') String userId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(userId, "User Id", projectId)

        return userAdminService.getUserSkillsMetrics(projectId, userId)
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
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(subjectId, "Subject Id", projectId)

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
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)

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
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)

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
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(badgeId, "Badge Id", projectId)

        return projectAdminStorageService.getSkillsForBadge(projectId, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/shared/projects/{sharedProjectId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    void shareSkillToAnotherProject(@PathVariable("projectId") String projectId,
                                    @PathVariable("skillId") String skillId,
                                    @PathVariable("sharedProjectId") String sharedProjectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        SkillsValidator.isNotBlank(sharedProjectId, "Shared Project Id", projectId)

        projectAdminStorageService.shareSkillToExternalProject(projectId, skillId, sharedProjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/shared/projects/{sharedProjectId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    void deleteSkillShare(@PathVariable("projectId") String projectId,
                          @PathVariable("skillId") String skillId,
                          @PathVariable("sharedProjectId") String sharedProjectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id", projectId)
        SkillsValidator.isNotBlank(sharedProjectId, "Shared Project Id", projectId)

        projectAdminStorageService.deleteSkillShare(projectId, skillId, sharedProjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/shared", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SharedSkillResult> getSharedSkills(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.getSharedSkillsWithOtherProjects(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/sharedWithMe", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SharedSkillResult> getSharedWithMeSkills(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.getSharedSkillsFromOtherProjects(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/settings", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SettingsResult> getProjectSettings(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return settingsService.loadSettingsForProject(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/settings/{setting}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    SettingsResult getProjectSetting(@PathVariable("projectId") String projectId, @PathVariable("setting") String setting) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(setting, "Setting Id", projectId)
        return settingsService.getSetting(projectId, setting)
    }

    @RequestMapping(value = "/projects/{projectId}/settings/{setting}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult saveProjectSetting(@PathVariable("projectId") String projectId, @PathVariable("setting") String setting, @RequestBody SettingsRequest value) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(setting, "Setting Id", projectId)
        SkillsValidator.isTrue(projectId == value.projectId, "Project Id must equal", projectId)
        SkillsValidator.isTrue(setting == value.setting, "Setting Id must equal", projectId)

        settingsService.saveSetting(value)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/resetClientSecret", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult resetClientSecret(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")

        String clientSecret = new ClientSecretGenerator().generateClientSecret()
        projectAdminStorageService.updateClientSecret(projectId, clientSecret)
        return new RequestResult(success: true)
    }

<<<<<<< HEAD
    @RequestMapping(value = "/projects/{projectId}/clientSecret", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    String getProjectClientSecret(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.getProjectSecret(projectId)
=======
    @RequestMapping(value = "/projects/{projectId}/clientSecret", method = [RequestMethod.GET], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    String getClientSecret(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return projectAdminStorageService.getProjDef(projectId).clientSecret
>>>>>>> #4 Removed duplicated controller method.
    }
}
