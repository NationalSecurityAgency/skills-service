package skills.service.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import skills.service.controller.exceptions.InvalidContentTypeException
import skills.service.controller.exceptions.MaxIconSizeExceeded
import skills.service.controller.request.model.*
import skills.service.controller.result.model.LevelDefinitionRes
import skills.service.controller.result.model.ProjectResult
import skills.service.controller.result.model.SharedSkillResult
import skills.service.controller.result.model.SimpleProjectResult
import skills.service.controller.result.model.SkillsGraphRes
import skills.service.controller.result.model.TableResult
import skills.service.datastore.services.AdminProjService
import skills.service.datastore.services.AdminUsersService
import skills.service.datastore.services.LevelDefinitionStorageService
import skills.service.datastore.services.UserAdminService
import skills.service.icons.CustomIconFacade
import skills.service.icons.UploadedIcon
import skills.storage.model.SkillDef

import javax.servlet.http.HttpServletResponse

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

@RestController
@RequestMapping("/admin")
@Slf4j
class AdminController {

    private static final long maxIconFileSize = 1024*1024

    @Autowired
    LevelDefinitionStorageService levelDefinitionStorageService

    @Autowired
    AdminProjService projectAdminStorageService

    @Autowired
    CustomIconFacade iconFacade

    @Autowired
    AdminUsersService adminUsersService

    @Autowired
    UserAdminService userAdminService

    @RequestMapping(value = "/projects/{id}", method = RequestMethod.DELETE)
    void deleteProject(@PathVariable("id") String projectId) {
        projectAdminStorageService.deleteProject(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.PATCH, produces = "application/json")
    @ResponseBody
    void setProjectDisplayOrder(
            @PathVariable("projectId") String projectId, @RequestBody ActionPatchRequest projectPatchRequest) {
        assert projectPatchRequest.action
        projectAdminStorageService.setProjectDisplayOrder(projectId, projectPatchRequest)
    }

    @RequestMapping(value = "/projects/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    ProjectResult getProject(@PathVariable("id") String projectId) {
        return projectAdminStorageService.getProject(projectId)
    }

    @RequestMapping(value = "/projects/{id}/projectSearch", method = RequestMethod.GET, produces = "application/json")
    List<SimpleProjectResult> searchProjects(@PathVariable("id") String projectId, @RequestParam("nameQuery") nameQuery) {
        return projectAdminStorageService.searchProjects(nameQuery)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    SubjectResult saveSubject(@PathVariable("projectId") String projectId,
                              @PathVariable("subjectId") String subjectId,
                              @RequestBody SubjectRequest subjectRequest) {
        assert subjectRequest.name
        assert subjectId == subjectRequest.subjectId

        return projectAdminStorageService.saveSubject(projectId, subjectRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/subjectExists", method = RequestMethod.GET, produces = "application/json")
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

    @RequestMapping(value = "/projects/{projectId}/skillExists", method = RequestMethod.GET, produces = "application/json")
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

    @RequestMapping(value = "/projects/{projectId}/subjects", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<SubjectResult> getSubjects(@PathVariable("projectId") String projectId) {
        return projectAdminStorageService.getSubjects(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    SubjectResult getSubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        return projectAdminStorageService.getSubject(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}", method = RequestMethod.PATCH, produces = "application/json")
    @ResponseBody
    void setSubjectDisplayOrder(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId, @RequestBody ActionPatchRequest subjectPatchRequest) {
        assert subjectPatchRequest.action
        projectAdminStorageService.setSubjectDisplayOrder(projectId, subjectId, subjectPatchRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.PUT, produces = "application/json")
    @ResponseBody
    BadgeResult saveBadge(@PathVariable("projectId") String projectId,
                          @PathVariable("badgeId") String badgeId,
                          @RequestBody BadgeRequest badgeRequest) {
        assert badgeRequest.name
        assert badgeId == badgeRequest.badgeId
        if (badgeRequest.startDate) { assert badgeRequest.endDate }
        if (badgeRequest.endDate) { assert badgeRequest.startDate }

        return projectAdminStorageService.saveBadge(projectId, badgeRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/badge/{badgeId}/skills/{skillId}", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    def assignSkillToBadge(@PathVariable("projectId") String projectId,
                           @PathVariable("badgeId") String badgeId,
                           @PathVariable("skillId") String skillId) {
        projectAdminStorageService.addSkillToBadge(projectId, badgeId, skillId)
        return [status: 'success']
    }

    @RequestMapping(value = "/projects/{projectId}/badge/{badgeId}/skills/{skillId}", method = RequestMethod.DELETE, produces = "application/json")
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

    @RequestMapping(value = "/projects/{projectId}/badges", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<BadgeResult> getBadges(@PathVariable("projectId") String projectId) {
        return projectAdminStorageService.getBadges(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    BadgeResult getBadge(@PathVariable("projectId") String projectId,
                          @PathVariable("badgeId") String badgeId) {
        return projectAdminStorageService.getBadge(projectId, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}", method = RequestMethod.PATCH, produces = "application/json")
    @ResponseBody
    void setBadgeDisplayOrder(
            @PathVariable("projectId") String projectId,
            @PathVariable("badgeId") String badgeId, @RequestBody ActionPatchRequest badgePatchRequest) {
        assert badgePatchRequest.action
        projectAdminStorageService.setBadgeDisplayOrder(projectId, badgeId, badgePatchRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills", method = RequestMethod.GET, produces = "application/json")
    List<SkillDefRes> getSkills(
            @PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        return projectAdminStorageService.getSkills(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    SkillDefRes getSkill(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId, @PathVariable("skillId") String skillId) {
        return projectAdminStorageService.getSkill(projectId, subjectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    SkillDefRes saveSkill(@PathVariable("projectId") String projectId,
                          @PathVariable("subjectId") String subjectId,
                          @PathVariable("skillId") String skillId,
                          @RequestBody SkillRequest skillRequest) {

//        assert subjectId == skillRequest.activityId
        assert projectId == skillRequest.projectId
        assert skillId == skillRequest.skillId

        assert skillRequest.pointIncrement > 0
        assert skillRequest.pointIncrementInterval > 0
        assert skillRequest.maxSkillAchievedCount > 0

        skillRequest.totalPoints = skillRequest.pointIncrement * skillRequest.maxSkillAchievedCount
//        assert skillRequest.subjectId == subjectId

        return projectAdminStorageService.saveSkill(skillRequest)
    }


    @RequestMapping(value = "/projects/{projectId}/dependency/availableSkills", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<SkillDefForDependencyRes> getSkillsAvailableForDependency(@PathVariable("projectId") String projectId) {
        return projectAdminStorageService.getSkillsAvailableForDependency(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/{dependentSkillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    def assignDependency(@PathVariable("projectId") String projectId,
                         @PathVariable("skillId") String skillId,
                         @PathVariable("dependentSkillId") String dependentSkillId) {
        projectAdminStorageService.assignSkillDependency(projectId, skillId, dependentSkillId)
        return [status: 'success']
    }


    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/projects/{dependentProjectId}/skills/{dependentSkillId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    def assignDependencyFromAnotherProject(@PathVariable("projectId") String projectId,
                                           @PathVariable("skillId") String skillId,
                                           @PathVariable("dependentProjectId") String dependentProjectId,
                                           @PathVariable("dependentSkillId") String dependentSkillId) {
        projectAdminStorageService.assignSkillDependency(projectId, skillId, dependentSkillId, dependentProjectId)
        return [status: 'success']
    }


    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/{dependentSkillId}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    def removeDependency(@PathVariable("projectId") String projectId,
                         @PathVariable("skillId") String skillId,
                         @PathVariable("dependentSkillId") String dependentSkillId) {
        projectAdminStorageService.removeSkillDependency(projectId, skillId, dependentSkillId)
        return [status: 'success']
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/projects/{dependentProjectId}/skills/{dependentSkillId}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    def removeDependencyFromAnotherProject(@PathVariable("projectId") String projectId,
                         @PathVariable("skillId") String skillId,
                         @PathVariable("dependentProjectId") String dependentProjectId,
                         @PathVariable("dependentSkillId") String dependentSkillId) {
        projectAdminStorageService.removeSkillDependency(projectId, skillId, dependentSkillId, dependentProjectId)
        return [status: 'success']
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependency/graph", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    SkillsGraphRes getDependencyForSkill(@PathVariable("projectId") String projectId,
                                         @PathVariable("skillId") String skillId) {
        return projectAdminStorageService.getDependentSkillsGraph(projectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/dependency/graph", method = RequestMethod.GET, produces = "application/json")
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

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    void deleteSkill(@PathVariable("projectId") String projectId,
                     @PathVariable("subjectId") String subjectId,
                     @PathVariable("skillId") String skillId) {
        projectAdminStorageService.deleteSkill(projectId, subjectId, skillId)
    }


    @RequestMapping(value = "/projects/{projectId}/skills", method = RequestMethod.GET, produces = "application/json")
    List<SkillDefRes> getAllSkillsForProject(
            @PathVariable("projectId") String projectId) {
        List<SkillDefRes> res = projectAdminStorageService.getSkills(projectId)
        return res
    }


    @RequestMapping(value = "/projects/{projectId}/levels", method = RequestMethod.GET, produces = "application/json")
    List<LevelDefinitionRes> getLevels(
            @PathVariable("projectId") String projectId) {
        List<LevelDefinitionRes> res = levelDefinitionStorageService.getLevels(projectId)
        return res
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/levels", method = RequestMethod.GET, produces = "application/json")
    List<LevelDefinitionRes> getLevels(
            @PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        List<LevelDefinitionRes> res = levelDefinitionStorageService.getLevels(projectId, subjectId)
        return res
    }

    @RequestMapping(value = "/projects/{projectId}/levels/last", method = RequestMethod.DELETE, produces = "application/json")
    void deleteLastLevel(@PathVariable("projectId") String projectId) {
        levelDefinitionStorageService.deleteLastLevel(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/levels/next", method = RequestMethod.PUT, produces = "application/json")
    void addNextLevel(@PathVariable("projectId") String projectId, @RequestBody NextLevelRequest nextLevelRequest) {
        assert nextLevelRequest.percent
        levelDefinitionStorageService.addNextLevel(projectId, nextLevelRequest.percent)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/levels/last", method = RequestMethod.DELETE, produces = "application/json")
    void deleteLastLevel(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        levelDefinitionStorageService.deleteLastLevel(projectId, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/levels/next", method = RequestMethod.PUT, produces = "application/json")
    void addNextLevel(
            @PathVariable("projectId") String projectId,
            @PathVariable("subjectId") String subjectId, @RequestBody NextLevelRequest nextLevelRequest) {
        assert nextLevelRequest.percent
        levelDefinitionStorageService.addNextLevel(projectId, nextLevelRequest.percent, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/icons/upload", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    UploadedIcon addCustomIcon(
            @PathVariable("projectId") String projectId,
            @RequestParam("customIcon") MultipartFile icon) {

        String iconFilename = icon.originalFilename
        byte[] file = icon.bytes
        icon.contentType

        if (!icon.contentType?.toLowerCase()?.startsWith("image/")){
            throw new InvalidContentTypeException("content-type [${icon.contentType}] is unacceptable, only image/ content-types are allowed")
        }

        if (file.length > maxIconFileSize){
            throw new MaxIconSizeExceeded("[${file.length}] exceeds the maximum icon size of [${FileUtils.byteCountToDisplaySize(maxIconFileSize)}]")
        }


        UploadedIcon result = iconFacade.saveIcon(projectId, iconFilename, icon.contentType, file)

        return result
    }

    @RequestMapping(value = "/projects/{projectId}/icons/{filename}", method =  RequestMethod.DELETE)
    void delete(@PathVariable("projectId") String projectId, @PathVariable("filename") filename, HttpServletResponse response){
        iconFacade.deleteIcon(projectId, filename)
        response.setStatus(HttpStatus.OK.value())
        response.setContentLength(0)
        response.setContentType("application/json")
        response.getWriter().println("{}")
        response.getWriter().flush()
    }

    @RequestMapping(value = "/projects/{projectId}/performedSkills/{userId}", method = RequestMethod.GET, produces = "application/json")
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

    @GetMapping(value = "/projects/{projectId}/users", produces = "application/json")
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

    @GetMapping(value = "/projects/{projectId}/subjects/{subjectId}/users", produces = "application/json")
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

    @GetMapping(value = "/projects/{projectId}/skills/{skillId}/users", produces = "application/json")
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

    @GetMapping(value = "/projects/{projectId}/badges/{badgeId}/users", produces = "application/json")
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

    @RequestMapping(value = "/projects/{projectId}/badge/{badgeId}/skills", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<SkillDefRes> getBadgeSkills(@PathVariable("projectId") String projectId, @PathVariable("badgeId") String badgeId) {
        return projectAdminStorageService.getSkillsForBadge(projectId, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/suggestUsers/{query}", method = RequestMethod.GET, produces = "application/json")
    List<String> suggestExistingUsers(@PathVariable("projectId") String projectId, @PathVariable("query") String query) {
        return userAdminService.suggestUsersForProject(projectId, query, new PageRequest(0, 10))
    }

    @RequestMapping(value = "/projects/{projectId}/validExistingUserId/{userId}", method = RequestMethod.GET, produces = "application/json")
    Boolean isValidExistingUserId(@PathVariable("projectId") String projectId, @PathVariable("userId") String userId) {
        return userAdminService.isValidExistingUserIdForProject(projectId, userId)
    }

    @RequestMapping(value = "/suggestUsers/{query}", method = RequestMethod.GET, produces = "application/json")
    List<String> suggestExistingUsers(@PathVariable("query") String query) {
        return userAdminService.suggestUsers(query, new PageRequest(0, 10))
    }

    @RequestMapping(value = "/validExistingUserId/{userId}", method = RequestMethod.GET, produces = "application/json")
    Boolean isValidExistingUserId(@PathVariable("userId") String userId) {
        return userAdminService.isValidExistingUserId(userId)
    }


    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/shared/projects/{sharedProjectId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    void shareSkillToAnotherProject(@PathVariable("projectId") String projectId,
                                       @PathVariable("skillId") String skillId,
                                       @PathVariable("sharedProjectId") String sharedProjectId) {
        projectAdminStorageService.shareSkillToExternalProject(projectId, skillId, sharedProjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/shared/projects/{sharedProjectId}", method = RequestMethod.DELETE, produces = "application/json")
    void deleteSkillShare(@PathVariable("projectId") String projectId,
                                    @PathVariable("skillId") String skillId,
                                    @PathVariable("sharedProjectId") String sharedProjectId) {
        projectAdminStorageService.deleteSkillShare(projectId, skillId, sharedProjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/shared", method = RequestMethod.GET, produces = "application/json")
    List<SharedSkillResult>  getSharedSkills(@PathVariable("projectId") String projectId) {
        return projectAdminStorageService.getSharedSkillsWithOtherProjects(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/sharedWithMe", method = RequestMethod.GET, produces = "application/json")
    List<SharedSkillResult>  getSharedWithMeSkills(@PathVariable("projectId") String projectId) {
        return projectAdminStorageService.getSharedSkillsFromOtherProjects(projectId)
    }

}
