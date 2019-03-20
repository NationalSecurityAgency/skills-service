package skills.service.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.endpoint.TokenEndpoint
import org.springframework.web.bind.annotation.*
import skills.service.controller.exceptions.ErrorCode
import skills.service.controller.exceptions.SkillException
import skills.service.controller.request.model.*
import skills.service.controller.result.model.*
import skills.service.datastore.services.AdminProjService
import skills.service.datastore.services.AdminUsersService
import skills.service.datastore.services.LevelDefinitionStorageService
import skills.service.datastore.services.settings.SettingsService
import skills.service.datastore.services.UserAdminService
import skills.storage.model.SkillDef
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
    TokenEndpoint tokenEndpoint

    @RequestMapping(value = "/projects/{id}", method = RequestMethod.DELETE)
    void deleteProject(@PathVariable("id") String projectId) {
        projectAdminStorageService.deleteProject(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void setProjectDisplayOrder(
            @PathVariable("projectId") String projectId, @RequestBody ActionPatchRequest projectPatchRequest) {
        assert projectPatchRequest.action
        projectAdminStorageService.setProjectDisplayOrder(projectId, projectPatchRequest)
    }

    @RequestMapping(value = "/projects/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ProjectResult getProject(@PathVariable("id") String projectId) {
        return projectAdminStorageService.getProject(projectId)
    }

    @RequestMapping(value = "/projects/{id}/projectSearch", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SimpleProjectResult> searchProjects(@PathVariable("id") String projectId, @RequestParam("nameQuery") nameQuery) {
        return projectAdminStorageService.searchProjects(nameQuery)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SubjectResult saveSubject(@PathVariable("projectId") String projectId,
                              @PathVariable("subjectId") String subjectId,
                              @RequestBody SubjectRequest subjectRequest) {
        // subject id is optional
        if (subjectRequest.subjectId && subjectId != subjectRequest.subjectId) {
            throw new SkillException("Subject id in the request doesn't equal to subject id in the URL. [${subjectRequest.subjectId}]<>[${subjectId}]", null, null, ErrorCode.BadParam)
        }
        if (!subjectRequest.subjectId) {
            subjectRequest.subjectId = subjectId
        }
        if (!subjectRequest?.name) {
            throw new SkillException("Subject name was not provided.", projectId, null, ErrorCode.BadParam)
        }



        return projectAdminStorageService.saveSubject(projectId, subjectRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/subjectExists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    boolean doesSubjectExist(@PathVariable("projectId") String projectId,
                             @RequestParam(value = "subjectId", required = false) String subjectId,
                             @RequestParam(value = "subjectName", required = false) String subjectName) {
        assert subjectId || subjectName, 'One of subjectId and subjectName must be specificed'
        assert !(subjectId && subjectName), 'Only one of subjectId and subjectName can be specified'

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
        assert skillId || skillName, 'One of skillId and skillName must be specificed'
        assert !(skillId && skillName), 'Only one of skillId and skillName can be specified'

        if (skillId) {
            return projectAdminStorageService.existsBySkillId(projectId, skillId)
        }

        return projectAdminStorageService.existsBySkillName(projectId, skillName)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = RequestMethod.DELETE)
    void deleteSubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        projectAdminStorageService.deleteSubject(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SubjectResult> getSubjects(@PathVariable("projectId") String projectId) {
        return projectAdminStorageService.getSubjects(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SubjectResult getSubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        return projectAdminStorageService.getSubject(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void setSubjectDisplayOrder(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId, @RequestBody ActionPatchRequest subjectPatchRequest) {
        assert subjectPatchRequest.action
        projectAdminStorageService.setSubjectDisplayOrder(projectId, subjectId, subjectPatchRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    BadgeResult saveBadge(@PathVariable("projectId") String projectId,
                          @PathVariable("badgeId") String badgeId,
                          @RequestBody BadgeRequest badgeRequest) {

        // badge id is optional
        if (badgeRequest.badgeId && badgeId != badgeRequest.badgeId) {
            throw new SkillException("Badge id in the request doesn't equal to badge id in the URL. [${badgeRequest.badgeId}]<>[${badgeId}]", null, null, ErrorCode.BadParam)
        }
        if (!badgeRequest.badgeId) {
            badgeRequest.badgeId = badgeId
        }

        if (!badgeRequest?.name) {
            throw new SkillException("Badge name was not provided.", projectId, null, ErrorCode.BadParam)
        }

        if (badgeRequest.startDate && !badgeRequest.endDate){
            throw new SkillException("If start date is provided then end date must be provided", projectId, null, ErrorCode.BadParam)
        }

        if (badgeRequest.endDate && !badgeRequest.startDate){
            throw new SkillException("If end date is provided then start date must be provided", projectId, null, ErrorCode.BadParam)
        }

        return projectAdminStorageService.saveBadge(projectId, badgeRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/badge/{badgeId}/skills/{skillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    def assignSkillToBadge(@PathVariable("projectId") String projectId,
                           @PathVariable("badgeId") String badgeId,
                           @PathVariable("skillId") String skillId) {
        projectAdminStorageService.addSkillToBadge(projectId, badgeId, skillId)
        return [status: 'success']
    }

    @RequestMapping(value = "/projects/{projectId}/badge/{badgeId}/skills/{skillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    def removeSkillFromBadge(@PathVariable("projectId") String projectId,
                           @PathVariable("badgeId") String badgeId,
                           @PathVariable("skillId") String skillId) {
        projectAdminStorageService.removeSkillFromBadge(projectId, badgeId, skillId)
        return [status: 'success']
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.DELETE)
    void deleteBadge(@PathVariable("projectId") String projectId, @PathVariable("badgeId") String badgeId) {
        projectAdminStorageService.deleteBadge(projectId, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<BadgeResult> getBadges(@PathVariable("projectId") String projectId) {
        return projectAdminStorageService.getBadges(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    BadgeResult getBadge(@PathVariable("projectId") String projectId,
                          @PathVariable("badgeId") String badgeId) {
        return projectAdminStorageService.getBadge(projectId, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void setBadgeDisplayOrder(
            @PathVariable("projectId") String projectId,
            @PathVariable("badgeId") String badgeId, @RequestBody ActionPatchRequest badgePatchRequest) {
        assert badgePatchRequest.action
        projectAdminStorageService.setBadgeDisplayOrder(projectId, badgeId, badgePatchRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SkillDefRes> getSkills(
            @PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        return projectAdminStorageService.getSkills(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillDefRes getSkill(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId, @PathVariable("skillId") String skillId) {
        return projectAdminStorageService.getSkill(projectId, subjectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillDefRes saveSkill(@PathVariable("projectId") String projectId,
                          @PathVariable("subjectId") String subjectId,
                          @PathVariable("skillId") String skillId,
                          @RequestBody SkillRequest skillRequest) {

        // project id is optional
        if (skillRequest.projectId && projectId != skillRequest.projectId) {
            throw new SkillException("Project id in the request doesn't equal to project id in the URL. [${skillRequest?.projectId}]<>[${projectId}]", null, null, ErrorCode.BadParam)
        }
        if (!skillRequest.projectId) {
            skillRequest.projectId = projectId
        }

        // subject id is optional
        if (skillRequest.subjectId && subjectId != skillRequest.subjectId) {
            throw new SkillException("Subject id in the request doesn't equal to subject id in the URL. [${skillRequest.subjectId}]<>[${subjectId}]", null, null, ErrorCode.BadParam)
        }
        if (!skillRequest.subjectId) {
            skillRequest.subjectId = subjectId
        }

        // skill id is optional
        if (skillRequest.skillId && skillId != skillRequest.skillId) {
            throw new SkillException("Skill id in the request doesn't equal to skillId id in the URL. [${skillRequest?.skillId}]<>[${skillId}]", null, null, ErrorCode.BadParam)
        }
        if (!skillRequest.skillId) {
            skillRequest.skillId = skillId
        }

        assert skillRequest.pointIncrement > 0
        assert skillRequest.pointIncrementInterval > 0
        assert skillRequest.numPerformToCompletion > 0
        assert skillRequest.version >= 0
        assert skillRequest.version < Constants.MAX_VERSION

        skillRequest.totalPoints = skillRequest.pointIncrement * skillRequest.numPerformToCompletion

        return projectAdminStorageService.saveSkill(skillRequest)
    }

    @GetMapping(value =  '/projects/{projectId}/latestVersion', produces = 'application/json')
    Integer findLatestSkillVersion(@PathVariable('projectId') String projectId) {
        return projectAdminStorageService.findLatestSkillVersion(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/dependency/availableSkills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SkillDefForDependencyRes> getSkillsAvailableForDependency(@PathVariable("projectId") String projectId, @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return projectAdminStorageService.getSkillsAvailableForDependency(projectId, version)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/{dependentSkillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    def assignDependency(@PathVariable("projectId") String projectId,
                         @PathVariable("skillId") String skillId,
                         @PathVariable("dependentSkillId") String dependentSkillId) {
        projectAdminStorageService.assignSkillDependency(projectId, skillId, dependentSkillId)
        return [status: 'success']
    }


    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/projects/{dependentProjectId}/skills/{dependentSkillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    def assignDependencyFromAnotherProject(@PathVariable("projectId") String projectId,
                                           @PathVariable("skillId") String skillId,
                                           @PathVariable("dependentProjectId") String dependentProjectId,
                                           @PathVariable("dependentSkillId") String dependentSkillId) {
        projectAdminStorageService.assignSkillDependency(projectId, skillId, dependentSkillId, dependentProjectId)
        return [status: 'success']
    }


    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/{dependentSkillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    def removeDependency(@PathVariable("projectId") String projectId,
                         @PathVariable("skillId") String skillId,
                         @PathVariable("dependentSkillId") String dependentSkillId) {
        projectAdminStorageService.removeSkillDependency(projectId, skillId, dependentSkillId)
        return [status: 'success']
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/projects/{dependentProjectId}/skills/{dependentSkillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    def removeDependencyFromAnotherProject(@PathVariable("projectId") String projectId,
                         @PathVariable("skillId") String skillId,
                         @PathVariable("dependentProjectId") String dependentProjectId,
                         @PathVariable("dependentSkillId") String dependentSkillId) {
        projectAdminStorageService.removeSkillDependency(projectId, skillId, dependentSkillId, dependentProjectId)
        return [status: 'success']
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/graph", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillsGraphRes getDependencyForSkill(@PathVariable("projectId") String projectId,
                                         @PathVariable("skillId") String skillId) {
        return projectAdminStorageService.getDependentSkillsGraph(projectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/dependency/graph", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SkillsGraphRes getDependencyForProject(@PathVariable("projectId") String projectId) {
        return projectAdminStorageService.getDependentSkillsGraph(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = RequestMethod.PATCH)
    @ResponseBody
    SkillDef updateSkillDisplayOrder(@PathVariable("projectId") String projectId,
                                     @PathVariable("subjectId") String subjectId,
                                     @PathVariable("skillId") String skillId,
                                     @RequestBody ActionPatchRequest patchRequest) {
        assert patchRequest.action

        return projectAdminStorageService.updateSkillDisplayOrder(projectId, subjectId, skillId, patchRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void deleteSkill(@PathVariable("projectId") String projectId,
                     @PathVariable("subjectId") String subjectId,
                     @PathVariable("skillId") String skillId) {
        projectAdminStorageService.deleteSkill(projectId, subjectId, skillId)
    }


    @RequestMapping(value = "/projects/{projectId}/skills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SkillDefRes> getAllSkillsForProject(
            @PathVariable("projectId") String projectId) {
        List<SkillDefRes> res = projectAdminStorageService.getSkills(projectId)
        return res
    }


    @RequestMapping(value = "/projects/{projectId}/levels", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<LevelDefinitionRes> getLevels(
            @PathVariable("projectId") String projectId) {
        List<LevelDefinitionRes> res = levelDefinitionStorageService.getLevels(projectId)
        return res
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/levels", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<LevelDefinitionRes> getLevels(
            @PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        List<LevelDefinitionRes> res = levelDefinitionStorageService.getLevels(projectId, subjectId)
        return res
    }

    @RequestMapping(value = "/projects/{projectId}/levels/last", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    void deleteLastLevel(@PathVariable("projectId") String projectId) {
        levelDefinitionStorageService.deleteLastLevel(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/levels/next", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    void addNextLevel(@PathVariable("projectId") String projectId, @RequestBody NextLevelRequest nextLevelRequest) {
        levelDefinitionStorageService.addNextLevel(projectId, nextLevelRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/levels/last", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    void deleteLastLevel(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        levelDefinitionStorageService.deleteLastLevel(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/levels/next", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    void addNextLevel(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId, @RequestBody NextLevelRequest nextLevelRequest) {
        levelDefinitionStorageService.addNextLevel(projectId, nextLevelRequest, subjectId)
    }

    //Need new methods to edit existing level methods for project and subject
    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/levels/edit/{levelId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    void editLevel(@PathVariable("projectId") String projectId,
                   @PathVariable("subjectId") String subjectId,
                   @PathVariable("levelId")   String levelId, @RequestBody EditLevelRequest editLevelRequest){
        levelDefinitionStorageService.editLevel(projectId, editLevelRequest, levelId, subjectId)
    }

    //Need new methods to edit existing level methods for project and subject
    @RequestMapping(value = "/projects/{projectId}/levels/edit/{levelId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    void editLevel(@PathVariable("projectId") String projectId,
                   @PathVariable("levelId")   String levelId, @RequestBody EditLevelRequest editLevelRequest){
        levelDefinitionStorageService.editLevel(projectId, editLevelRequest, levelId)
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
        PageRequest pageRequest = new PageRequest(page-1, limit, ascending ? ASC : DESC, orderBy)
        return userAdminService.loadUserPerformedSkillsPage(projectId, userId, query, pageRequest)
    }

    @GetMapping(value = '/projects/{projectId}/users/{userId}/skillsCount', produces = 'application/json')
    @ResponseBody
    @CompileStatic
    Integer getUserSkillsCount(@PathVariable('projectId') String projectId,
                               @PathVariable('userId') String userId) {
        return userAdminService.distinctSkillsCount(projectId, userId)
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
        PageRequest pageRequest = new PageRequest(page-1, limit, ascending ? ASC : DESC, orderBy)
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
        PageRequest pageRequest = new PageRequest(page-1, limit, ascending ? ASC : DESC, orderBy)
        List<SkillDefRes> subjectSkills = getSkills(projectId, subjectId)
        List<String> skillIds = subjectSkills.collect {it.skillId}
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
        PageRequest pageRequest = new PageRequest(page-1, limit, ascending ? ASC : DESC, orderBy)
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
        PageRequest pageRequest = new PageRequest(page-1, limit, ascending ? ASC : DESC, orderBy)
        List<SkillDefRes> badgeSkills = getBadgeSkills(projectId, badgeId)
        List<String> skillIds = badgeSkills.collect {it.skillId}
        if(!skillIds) {
            return new TableResult()
        }
        return adminUsersService.loadUsersPage(projectId, skillIds, query, pageRequest)
    }

    @RequestMapping(value  = "/projects/{projectId}/badge/{badgeId}/skills", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SkillDefRes> getBadgeSkills(@PathVariable("projectId") String projectId, @PathVariable("badgeId") String badgeId) {
        return projectAdminStorageService.getSkillsForBadge(projectId, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/shared/projects/{sharedProjectId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    void shareSkillToAnotherProject(@PathVariable("projectId") String projectId,
                                       @PathVariable("skillId") String skillId,
                                       @PathVariable("sharedProjectId") String sharedProjectId) {
        projectAdminStorageService.shareSkillToExternalProject(projectId, skillId, sharedProjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/shared/projects/{sharedProjectId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    void deleteSkillShare(@PathVariable("projectId") String projectId,
                                    @PathVariable("skillId") String skillId,
                                    @PathVariable("sharedProjectId") String sharedProjectId) {
        projectAdminStorageService.deleteSkillShare(projectId, skillId, sharedProjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/shared", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SharedSkillResult>  getSharedSkills(@PathVariable("projectId") String projectId) {
        return projectAdminStorageService.getSharedSkillsWithOtherProjects(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/sharedWithMe", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SharedSkillResult>  getSharedWithMeSkills(@PathVariable("projectId") String projectId) {
        return projectAdminStorageService.getSharedSkillsFromOtherProjects(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/settings", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SettingsResult> getProjectSettings(@PathVariable("projectId") String projectId) {
        return settingsService.loadSettingsForProject(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/settings/{setting}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    SettingsResult getProjectSetting(@PathVariable("projectId") String projectId, @PathVariable("setting") String setting) {
        return settingsService.getSetting(projectId, setting)
    }

    @RequestMapping(value = "/projects/{projectId}/settings/{setting}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    SettingsResult saveProjectSetting(@PathVariable("projectId") String projectId, @PathVariable("setting") String setting, @RequestBody SettingsRequest value){
        settingsService.saveSetting(value)
    }

    @RequestMapping(value = "/projects/{projectId}/resetClientSecret", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    String resetClientSecret(@PathVariable("projectId") String projectId) {
        String clientSecret = new ClientSecretGenerator().generateClientSecret()
        projectAdminStorageService.updateClientSecret(projectId, clientSecret)
        return clientSecret
    }

    @RequestMapping(value = "/projects/{projectId}/token/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<OAuth2AccessToken> getUserToken(@PathVariable("projectId") String projectId, @PathVariable("userId") String userId) {
        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(projectId, null ,[])
        Map<String, String> parameters = [grant_type : 'client_credentials', proxy_user : userId]
        return tokenEndpoint.postAccessToken(principal, parameters)
    }
}
