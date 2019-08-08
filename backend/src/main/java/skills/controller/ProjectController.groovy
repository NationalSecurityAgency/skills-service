package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.ProjectRequest
import skills.controller.result.model.CustomIconResult
import skills.controller.result.model.ProjectResult
import skills.controller.result.model.RequestResult
import skills.icons.CustomIconFacade
import skills.profile.EnableCallStackProf
import skills.services.AdminProjService

import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/app")
@Slf4j
@EnableCallStackProf
class ProjectController {
    @Autowired
    AdminProjService projectAdminStorageService

    @Autowired
    CustomIconFacade customIconFacade

    @Autowired
    PasswordEncoder passwordEncoder

    @Autowired
    UserInfoService userInfoService

    static final RESERVERED_PROJECT_ID = AdminProjService.ALL_SKILLS_PROJECTS

    @RequestMapping(value = "/projects", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<ProjectResult> getProjects() {
        return projectAdminStorageService.getProjects()
    }

    @RequestMapping(value = "/projects/{id}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    RequestResult saveProject(@PathVariable("id") String projectId, @RequestBody ProjectRequest projectRequest) {
        // project id is optional
        if (projectRequest.projectId && projectId != projectRequest.projectId) {
            throw new SkillException("Project id in the request doesn't equal to project id in the URL. [${projectRequest?.projectId}]<>[${projectId}]", null, null, ErrorCode.BadParam)
        }
        if (!projectRequest.projectId) {
            projectRequest.projectId = projectId
        }
        if (projectRequest.projectId == RESERVERED_PROJECT_ID) {
            throw new SkillException("Project id uses a reserved id, please choose a different project id.", projectId, null, ErrorCode.BadParam)
        }
        if (!projectRequest?.name) {
            throw new SkillException("Project name was not provided.", projectId, null, ErrorCode.BadParam)
        }

        // if the id is provided then this is an 'edit operation' then user must be an amdin of this project
        if (projectRequest.id) {
            throw new SkillException("Cannot edit project id using /app/projects/{id} endpoint. Please use /admin/projects/{id}", projectId, null, ErrorCode.AccessDenied)
        }

        projectAdminStorageService.saveProject(projectRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value="/projects/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    ProjectResult getProject(@PathVariable("id") String projectId){
        SkillsValidator.isNotBlank(projectId, "id")
        return projectAdminStorageService.getProject(projectId)
    }

    @RequestMapping(value = "/projectExist", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    boolean doesProjectExist(@RequestParam(value = "projectId", required = false) String projectId,
                             @RequestParam(value = "projectName", required = false) String projectName) {
        SkillsValidator.isTrue((projectId || projectName), "One of Project Id or Project Name must be provided.")
        SkillsValidator.isTrue(!(projectId && projectName), "Only Project Id or Project Name may be provided, not both.")

        if (projectId) {
            projectId = URLDecoder.decode(projectId, StandardCharsets.UTF_8.toString())
            return projectAdminStorageService.existsByProjectId(projectId)
        }

        projectName = URLDecoder.decode(projectName, StandardCharsets.UTF_8.toString())
        return projectAdminStorageService.existsByProjectName(projectName)
    }

    @RequestMapping(value = "/projects/{id}/customIcons", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<CustomIconResult> getCustomIcons(@PathVariable("id") String projectId) {
        return projectAdminStorageService.getCustomIcons(projectId)
    }

    @RequestMapping(value = "/projects/{id}/versions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<Integer> listVersions(@PathVariable("id") String projectId) {
        return projectAdminStorageService.getUniqueVersionList(projectId)
    }
}
