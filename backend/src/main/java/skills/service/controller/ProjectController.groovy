package skills.service.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.service.controller.request.model.ProjectRequest
import skills.service.controller.result.model.ProjectResult
import skills.service.datastore.services.AdminProjService

@RestController
@RequestMapping("/app")
@Slf4j
class ProjectController {

    @Autowired
    AdminProjService projectAdminStorageService

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

}
