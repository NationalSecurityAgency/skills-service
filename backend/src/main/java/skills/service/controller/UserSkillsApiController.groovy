package skills.service.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.service.auth.UserInfoService
import skills.service.skillLoading.RankingLoader
import skills.service.skillLoading.SkillsLoader
import skills.service.skillsManagement.SkillsManagementFacade

@RestController
@RequestMapping("/api")
@Slf4j
@CompileStatic
class UserSkillsApiController {

    @Autowired
    SkillsLoader skillsLoader

    @Autowired
    RankingLoader rankingLoader

    @Autowired
    UserInfoService userInfoService

    @Autowired
    SkillsManagementFacade skillsManagementFacade

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsManagementFacade.AddSkillResult addSkill(@PathVariable("projectId") String projectId,
                                                   @PathVariable("skillId") String skillId,
                                                   @RequestParam(name = "userId", required = false) String userId) {
        skillsManagementFacade.addSkill(projectId, skillId, userId ?: userInfoService.currentUser.username)
    }
}
