package skills.service.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.service.auth.UserInfoService
import skills.service.auth.aop.AdminUsersOnlyWhenUserIdSupplied
import skills.service.icons.CustomIconFacade
import skills.service.skillLoading.RankingLoader
import skills.service.skillLoading.SkillsLoader
import skills.service.skillLoading.model.*
import skills.service.skillsManagement.SkillsManagementFacade
import skills.utils.Constants

@CrossOrigin(allowCredentials = 'true')
@RestController
@RequestMapping(["/api", "/admin"])
@Slf4j
@CompileStatic
@AdminUsersOnlyWhenUserIdSupplied
class UserSkillsController {

    @Autowired
    SkillsManagementFacade skillsManagementFacade

    @Autowired
    SkillsLoader skillsLoader

    @Autowired
    UserInfoService userInfoService

    @Autowired
    CustomIconFacade customIconFacade

    @Autowired
    RankingLoader rankingLoader

    @RequestMapping(value = "/projects/{projectId}/level", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    Integer getUserLevel(@PathVariable(name = "projectId") String projectId,
                         @RequestParam(name = "userId", required = false) String userId) {
        return skillsLoader.getUserLevel(projectId, userId ?: userInfoService.currentUser.username)
    }

    @RequestMapping(value = "/projects/{projectId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    OverallSkillSummary getSkillsSummary(@PathVariable("projectId") String projectId,
                                         @RequestParam(name = "userId", required = false) String userId,
                                         @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadOverallSummary(projectId, userId ?: userInfoService.currentUser.username, version)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillSubjectSummary getSubjectsSkillsSummary(@PathVariable("projectId") String projectId,
                                                 @PathVariable("subjectId") String subjectId,
                                                 @RequestParam(name = "userId", required = false) String userId,
                                                 @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadSubject(projectId, userId ?: userInfoService.currentUser.username, subjectId, version)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillSummary getSkillSummary(@PathVariable("projectId") String projectId,
                                 @PathVariable("skillId") String skillId,
                                 @RequestParam(name = "userId", required = false) String userId,
                                 @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadSkillSummary(projectId, userId ?: userInfoService.currentUser.username, null, skillId, version)
    }

    @RequestMapping(value = "/projects/{projectId}/projects/{crossProjectId}/skills/{skillId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillSummary getCrossProjectSkillSummary(@PathVariable("projectId") String projectId,
                                             @PathVariable("crossProjectId") String crossProjectId,
                                             @PathVariable("skillId") String skillId,
                                             @RequestParam(name = "userId", required = false) String userId,
                                             @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadSkillSummary(projectId, userId ?: userInfoService.currentUser.username, crossProjectId, skillId, version)
    }


    @RequestMapping(value = "/projects/{projectId}/badges/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    List<SkillBadgeSummary>  getAllBadgesSummary(@PathVariable("projectId") String projectId,
                                                 @RequestParam(name = "userId", required = false) String userId,
                                                 @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadBadgeSummaries(projectId, userId ?: userInfoService.currentUser.username, version)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillBadgeSummary getBadgeSummary(@PathVariable("projectId") String projectId,
                                      @PathVariable("badgeId") String badgeId,
                                      @RequestParam(name = "userId", required = false) String userId) {
        return skillsLoader.loadBadge(projectId, userId ?: userInfoService.currentUser.username, badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    UserPointHistorySummary getProjectsPointHistory(@PathVariable("projectId") String projectId,
                                                    @RequestParam(name = "userId", required = false) String userId) {
        return skillsLoader.loadPointHistorySummary(projectId, userId ?: userInfoService.currentUser.username, 365)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    UserPointHistorySummary getSubjectsPointHistory(@PathVariable("projectId") String projectId,
                                                    @PathVariable("subjectId") String subjectId,
                                                    @RequestParam(name = "userId", required = false) String userId) {
        return skillsLoader.loadPointHistorySummary(projectId, userId ?: userInfoService.currentUser.username, 365, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependencies", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillDependencyInfo loadSkillDependencyInfo(@PathVariable("projectId") String projectId,
                                                @PathVariable("skillId") String skillId,
                                                @RequestParam(name = "userId", required = false) String userId) {
        return skillsLoader.loadSkillDependencyInfo(projectId, userId ?: userInfoService.currentUser.username, skillId)
    }

    // had to move the following addSkill methods into their own separate controllers
    // to avoid ambiguous naming conflict with both /api and /admin versions

//    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}", method = RequestMethod.POST, produces = "application/json")
//    @ResponseBody
//    @CompileStatic
//    SkillsManagementFacade.AddSkillResult addSkill(@PathVariable("projectId") String projectId,
//                                                   @PathVariable("skillId") String skillId,
//                                                   @RequestParam(name = "userId", required = false) String userId) {
//        skillsManagementFacade.addSkill(projectId, skillId, userInfoService.currentUser.username)
//    }
//
//    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
//    @ResponseBody
//    @CompileStatic
//    SkillsManagementFacade.AddSkillResult addSkill(
//            @PathVariable("projectId") String projectId,
//            @PathVariable("skillId") String skillId,
//            @RequestBody AddSkillRequest skillRequest) {
//        try {
//            assert skillRequest?.userId
//            Date incomingDate = skillRequest.timestamp != null ? new Date(skillRequest.timestamp) : null
//
//            return skillsManagementFacade.addSkill(projectId, skillId, userInfoService.lookupUserId(skillRequest.userId), incomingDate)
//        } catch (Exception e) {
//            log.error("Failed for projetId=[$projectId], skillId=[$skillId]", e)
//            throw new SkillException(e.message, projectId, skillId)
//        }
//    }

    @RequestMapping(value = "/projects/{projectId}/rank", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRanking getRanking(@PathVariable("projectId") String projectId,
                             @RequestParam(name = "userId", required = false) String userId) {
        return rankingLoader.getUserSkillsRanking(projectId, userId ?: userInfoService.currentUser.username)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/rank", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRanking getRankingBySubject(@PathVariable("projectId") String projectId,
                                      @PathVariable("subjectId") String subjectId,
                                      @RequestParam(name = "userId", required = false) String userId) {
        return rankingLoader.getUserSkillsRanking(projectId, userId ?: userInfoService.currentUser.username, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/rankDistribution", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRankingDistribution getRankingDistribution(@PathVariable("projectId") String projectId,
                                                     @RequestParam(name = "userId", required = false) String userId) {
        return rankingLoader.getRankingDistribution(projectId, userId ?: userInfoService.currentUser.username)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/rankDistribution", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRankingDistribution getRankingDistributionBySubject(@PathVariable("projectId") String projectId,
                                                              @PathVariable("subjectId") String subjectId,
                                                              @RequestParam(name = "userId", required = false) String userId) {
        return rankingLoader.getRankingDistribution(projectId, userId ?: userInfoService.currentUser.username, subjectId)
    }

    @RequestMapping(value = "/projects/{id}/customIconCss", method = RequestMethod.GET, produces = "text/css")
    @ResponseBody
    String getCustomIconCss(@PathVariable("id") String projectId) {
        return customIconFacade.generateCss(projectId)
    }
}
