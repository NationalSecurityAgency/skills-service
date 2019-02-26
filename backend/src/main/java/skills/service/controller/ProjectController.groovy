package skills.service.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import skills.service.controller.request.model.ProjectRequest
import skills.service.controller.result.model.CustomIconResult
import skills.service.controller.result.model.ProjectResult
import skills.service.datastore.services.AdminProjService
import skills.service.icons.CustomIconFacade
import skills.service.icons.IconCssNameUtil
import skills.storage.model.CustomIcon
import skills.storage.model.ProjDef
import skills.utils.SecretCodeGenerator

@RestController
@RequestMapping("/app")
@Slf4j
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
    ProjectResult saveProject(@PathVariable("id") String projectId, @RequestBody ProjectRequest projectRequest) {
        assert projectRequest?.projectId
        assert projectRequest?.name
        assert projectId == projectRequest.projectId

        projectRequest.secretCode = new SecretCodeGenerator().generateSecretCode()
        return projectAdminStorageService.saveProject(projectRequest)
    }

    @RequestMapping(value = "/projectExist", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    boolean doesProjectExist(@RequestParam(value = "projectId", required = false) String projectId,
                             @RequestParam(value = "projectName", required = false) String projectName) {
        assert projectId || projectName
        assert !(projectId && projectName)

        if (projectId) {
            return projectAdminStorageService.existsByProjectId(projectId)
        }

        return projectAdminStorageService.existsByProjectName(projectName)
    }

    @RequestMapping(value = "/projects/{id}/customIcons", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<CustomIconResult> getCustomIcons(@PathVariable("id") String projectId) {
        ProjDef project = projectAdminStorageService.getProjDef(projectId)
        return project.getCustomIcons().collect { CustomIcon icon ->
            String cssClassname = IconCssNameUtil.getCssClass(icon.projectId, icon.filename)
            return new CustomIconResult(filename: icon.filename, cssClassname: cssClassname)
        }
    }

    @RequestMapping(value = "/projects/{id}/customIconCss", method = RequestMethod.GET, produces = "text/css")
    @ResponseBody
    String getCustomIconCss(@PathVariable("id") String projectId) {
        return customIconFacade.generateCss(projectId)
    }
}
