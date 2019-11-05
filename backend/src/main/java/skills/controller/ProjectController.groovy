package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import skills.PublicProps
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
import skills.services.IdFormatValidator
import skills.services.admin.ProjAdminService
import skills.services.admin.ShareSkillsService
import skills.services.admin.SkillsAdminService

import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/app")
@Slf4j
@EnableCallStackProf
class ProjectController {

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    CustomIconFacade customIconFacade

    @Autowired
    PasswordEncoder passwordEncoder

    @Autowired
    UserInfoService userInfoService

    @Autowired
    PublicPropsBasedValidator propsBasedValidator

    @Autowired
    SkillsAdminService skillsAdminService

    static final RESERVERED_PROJECT_ID = ShareSkillsService.ALL_SKILLS_PROJECTS

    @RequestMapping(value = "/projects", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<ProjectResult> getProjects() {
        return projAdminService.getProjects()
    }

    @RequestMapping(value = "/projects/{id}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    RequestResult saveProject(@PathVariable("id") String projectId, @RequestBody ProjectRequest projectRequest) {
        // project id is optional
        if (projectRequest.projectId && projectId != projectRequest.projectId) {
            throw new SkillException("Project id in the request doesn't equal to project id in the URL [${projectRequest?.projectId}]<>[${projectId}]. Cannot edit project id using /app/projects/{id} endpoint. Please use /admin/projects/{id}", null, null, ErrorCode.AccessDenied)
        }
        IdFormatValidator.validate(projectId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "Project Id", projectId)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "Project Id", projectId)

        if (!projectRequest.projectId) {
            projectRequest.projectId = projectId
        } else {
            IdFormatValidator.validate(projectRequest.projectId)
            propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "Project Id", projectRequest.projectId)
            propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "Project Id", projectRequest.projectId)
        }

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxProjectNameLength, "Project Name", projectRequest.name)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minNameLength, "Project Name", projectRequest.name)

        if (projectRequest.projectId == RESERVERED_PROJECT_ID) {
            throw new SkillException("Project id uses a reserved id, please choose a different project id.", projectId, null, ErrorCode.BadParam)
        }
        if (!projectRequest?.name) {
            throw new SkillException("Project name was not provided.", projectId, null, ErrorCode.BadParam)
        }

        projAdminService.saveProject(null, projectRequest)
        return new RequestResult(success: true)
    }

    @RequestMapping(value="/projects/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    ProjectResult getProject(@PathVariable("id") String projectId){
        SkillsValidator.isNotBlank(projectId, "id")
        return projAdminService.getProject(projectId)
    }

    @RequestMapping(value = "/projectExist", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    boolean doesProjectExist(@RequestParam(value = "projectId", required = false) String projectId,
                             @RequestParam(value = "projectName", required = false) String projectName) {
        SkillsValidator.isTrue((projectId || projectName), "One of Project Id or Project Name must be provided.")
        SkillsValidator.isTrue(!(projectId && projectName), "Only Project Id or Project Name may be provided, not both.")

        if (projectId) {
            projectId = URLDecoder.decode(projectId, StandardCharsets.UTF_8.toString())
            return projAdminService.existsByProjectId(projectId)
        }

        projectName = URLDecoder.decode(projectName, StandardCharsets.UTF_8.toString())
        return projAdminService.existsByProjectName(projectName)
    }

    @RequestMapping(value = "/projects/{id}/customIcons", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<CustomIconResult> getCustomIcons(@PathVariable("id") String projectId) {
        return projAdminService.getCustomIcons(projectId)
    }

    @RequestMapping(value = "/projects/{id}/versions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<Integer> listVersions(@PathVariable("id") String projectId) {
        return skillsAdminService.getUniqueSkillVersionList(projectId)
    }
}
