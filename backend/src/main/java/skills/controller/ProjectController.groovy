package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import skills.auth.UserInfoService
import skills.auth.UserSkillsGrantedAuthority
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.services.AdminProjService
import skills.storage.model.auth.RoleName

import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/app")
@Slf4j
@skills.profile.EnableCallStackProf
class ProjectController {
    @Autowired
    AdminProjService projectAdminStorageService

    @Autowired
    skills.icons.CustomIconFacade customIconFacade

    @Autowired
    PasswordEncoder passwordEncoder

    @Autowired
    UserInfoService userInfoService

    @RequestMapping(value = "/projects", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<skills.controller.result.model.ProjectResult> getProjects() {
        return projectAdminStorageService.getProjects()
    }

    @RequestMapping(value = "/projects/{id}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    skills.controller.result.model.RequestResult saveProject(@PathVariable("id") String projectId, @RequestBody skills.controller.request.model.ProjectRequest projectRequest) {
        // project id is optional
        if (projectRequest.projectId && projectId != projectRequest.projectId) {
            throw new skills.controller.exceptions.SkillException("Project id in the request doesn't equal to project id in the URL. [${projectRequest?.projectId}]<>[${projectId}]", null, null, skills.controller.exceptions.ErrorCode.BadParam)
        }
        if (!projectRequest.projectId) {
            projectRequest.projectId = projectId
        }
        if (!projectRequest?.name) {
            throw new skills.controller.exceptions.SkillException("Project name was not provided.", projectId, null, skills.controller.exceptions.ErrorCode.BadParam)
        }

        // if the id is provided then this is an 'edit operation' then user must be an amdin of this project
        if (projectRequest.id) {
            throw new SkillException("Can not edit project id using /app/projects/{id} endpoint. Plese use /admin/projects/{id}", projectId, null, ErrorCode.AccessDenied)
        }

        projectAdminStorageService.saveProject(projectRequest)
        return new skills.controller.result.model.RequestResult(success: true)
    }

    @RequestMapping(value="/projects/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    skills.controller.result.model.ProjectResult getProject(@PathVariable("id") String projectId){
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "id")
        return projectAdminStorageService.getProject(projectId)
    }

    @RequestMapping(value = "/projectExist", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    boolean doesProjectExist(@RequestParam(value = "projectId", required = false) String projectId,
                             @RequestParam(value = "projectName", required = false) String projectName) {
        skills.controller.exceptions.SkillsValidator.isTrue((projectId || projectName), "One of Project Id or Project Name must be provided.")
        skills.controller.exceptions.SkillsValidator.isTrue(!(projectId && projectName), "Only Project Id or Project Name may be provided, not both.")

        if (projectId) {
            projectId = URLDecoder.decode(projectId, StandardCharsets.UTF_8.toString())
            return projectAdminStorageService.existsByProjectId(projectId)
        }

        projectName = URLDecoder.decode(projectName, StandardCharsets.UTF_8.toString())
        return projectAdminStorageService.existsByProjectName(projectName)
    }

    @RequestMapping(value = "/projects/{id}/customIcons", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<skills.controller.result.model.CustomIconResult> getCustomIcons(@PathVariable("id") String projectId) {
        return projectAdminStorageService.getCustomIcons(projectId)
    }

    @RequestMapping(value = "/projects/{id}/versions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<Integer> listVersions(@PathVariable("id") String projectId) {
        return projectAdminStorageService.getUniqueVersionList(projectId)
    }
}
