package skills.service.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.service.auth.UserInfoService
import skills.service.auth.aop.AdminUsersOnlyWhenUserIdSupplied
import skills.service.controller.exceptions.SkillException
import skills.service.controller.request.model.AddSkillRequest
import skills.service.skillLoading.RankingLoader
import skills.service.skillLoading.SkillsLoader
import skills.service.skillsManagement.SkillsManagementFacade

@RestController
@RequestMapping("/admin")
@Slf4j
@CompileStatic
class UserSkillsAdminController {

    @Autowired
    SkillsLoader skillsLoader

    @Autowired
    RankingLoader rankingLoader

    @Autowired
    UserInfoService userInfoService

    @Autowired
    SkillsManagementFacade skillsManagementFacade

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    @CompileStatic
    @AdminUsersOnlyWhenUserIdSupplied
    SkillsManagementFacade.AddSkillResult addSkill(
            @PathVariable("projectId") String projectId,
            @PathVariable("skillId") String skillId,
            @RequestParam(name = "userId", required = false) String userIdss,
            @RequestBody AddSkillRequest skillRequest) {
        try {
            assert skillRequest?.userId
            Date incomingDate = skillRequest.timestamp != null ? new Date(skillRequest.timestamp) : null

            return skillsManagementFacade.addSkill(projectId, skillId, userInfoService.lookupUserId(skillRequest.userId), incomingDate)
        } catch (Exception e) {
            log.error("Failed for projetId=[$projectId], skillId=[$skillId]", e)
            throw new SkillException(e.message, projectId, skillId)
        }
    }
}
