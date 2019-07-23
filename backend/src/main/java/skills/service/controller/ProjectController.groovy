package skills.service.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import skills.service.controller.exceptions.ErrorCode
import skills.service.controller.exceptions.SkillException
import skills.service.controller.exceptions.SkillsValidator
import skills.service.controller.request.model.ProjectRequest
import skills.service.controller.result.model.RequestResult
import skills.service.controller.result.model.CustomIconResult
import skills.service.controller.result.model.ProjectResult
import skills.service.datastore.services.AdminProjService
import skills.service.icons.CustomIconFacade
import skills.service.icons.IconCssNameUtil
import skills.service.profile.EnableCallStackProf
import skills.storage.model.CustomIcon
import skills.storage.model.ProjDef

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
        if (!projectRequest?.name) {
            throw new SkillException("Project name was not provided.", projectId, null, ErrorCode.BadParam)
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
