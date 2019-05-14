package skills.service.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.service.auth.UserInfoService
import skills.service.auth.aop.AdminUsersOnlyWhenUserIdSupplied
import skills.service.controller.request.model.SkillEventRequest
import skills.service.icons.CustomIconFacade
import skills.service.skillLoading.RankingLoader
import skills.service.skillLoading.SkillsLoader
import skills.service.skillLoading.model.*
import skills.service.skillsManagement.SkillsManagementFacade
import skills.utils.Constants

@CrossOrigin(allowCredentials = 'true')
@RestController
@RequestMapping("/api")
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
                         @RequestParam(name = "userId", required = false) String userIdParam) {
        return skillsLoader.getUserLevel(projectId, getUserId(userIdParam))
    }

    @RequestMapping(value = "/projects/{projectId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    OverallSkillSummary getSkillsSummary(@PathVariable("projectId") String projectId,
                                         @RequestParam(name = "userId", required = false) String userIdParam,
                                         @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadOverallSummary(projectId, getUserId(userIdParam), version)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillSubjectSummary getSubjectsSkillsSummary(@PathVariable("projectId") String projectId,
                                                 @PathVariable("subjectId") String subjectId,
                                                 @RequestParam(name = "userId", required = false) String userIdParam,
                                                 @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadSubject(projectId, getUserId(userIdParam), subjectId, version)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillSummary getSkillSummary(@PathVariable("projectId") String projectId,
                                 @PathVariable("skillId") String skillId,
                                 @RequestParam(name = "userId", required = false) String userIdParam,
                                 @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadSkillSummary(projectId, getUserId(userIdParam), null, skillId, version)
    }

    @RequestMapping(value = "/projects/{projectId}/projects/{crossProjectId}/skills/{skillId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillSummary getCrossProjectSkillSummary(@PathVariable("projectId") String projectId,
                                             @PathVariable("crossProjectId") String crossProjectId,
                                             @PathVariable("skillId") String skillId,
                                             @RequestParam(name = "userId", required = false) String userIdParam,
                                             @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadSkillSummary(projectId, getUserId(userIdParam), crossProjectId, skillId, version)
    }


    @RequestMapping(value = "/projects/{projectId}/badges/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    List<SkillBadgeSummary>  getAllBadgesSummary(@PathVariable("projectId") String projectId,
                                                 @RequestParam(name = "userId", required = false) String userIdParam,
                                                 @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadBadgeSummaries(projectId, getUserId(userIdParam), version)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillBadgeSummary getBadgeSummary(@PathVariable("projectId") String projectId,
                                      @PathVariable("badgeId") String badgeId,
                                      @RequestParam(name = "userId", required = false) String userIdParam) {
        return skillsLoader.loadBadge(projectId, getUserId(userIdParam), badgeId)
    }

    @RequestMapping(value = "/projects/{projectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    UserPointHistorySummary getProjectsPointHistory(@PathVariable("projectId") String projectId,
                                                    @RequestParam(name = "userId", required = false) String userIdParam) {
        return skillsLoader.loadPointHistorySummary(projectId, getUserId(userIdParam), 365)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    UserPointHistorySummary getSubjectsPointHistory(@PathVariable("projectId") String projectId,
                                                    @PathVariable("subjectId") String subjectId,
                                                    @RequestParam(name = "userId", required = false) String userIdParam) {
        return skillsLoader.loadPointHistorySummary(projectId, getUserId(userIdParam), 365, subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependencies", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillDependencyInfo loadSkillDependencyInfo(@PathVariable("projectId") String projectId,
                                                @PathVariable("skillId") String skillId,
                                                @RequestParam(name = "userId", required = false) String userIdParam) {
        return skillsLoader.loadSkillDependencyInfo(projectId, getUserId(userIdParam), skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsManagementFacade.AddSkillResult addSkill(@PathVariable("projectId") String projectId,
                                                   @PathVariable("skillId") String skillId,
                                                   @RequestBody(required = false) SkillEventRequest skillEventRequest) {
        Date incomingDate = skillEventRequest?.timestamp != null ? new Date(skillEventRequest.timestamp) : new Date()
        skillsManagementFacade.addSkill(projectId, skillId,  getUserId(skillEventRequest?.userId), incomingDate)
    }

    @RequestMapping(value = "/projects/{projectId}/rank", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRanking getRanking(@PathVariable("projectId") String projectId,
                             @RequestParam(name = "userId", required = false) String userIdParam) {
        return rankingLoader.getUserSkillsRanking(projectId, getUserId(userIdParam))
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/rank", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRanking getRankingBySubject(@PathVariable("projectId") String projectId,
                                      @PathVariable("subjectId") String subjectId,
                                      @RequestParam(name = "userId", required = false) String userIdParam) {
        return rankingLoader.getUserSkillsRanking(projectId, getUserId(userIdParam), subjectId)
    }

    @RequestMapping(value = "/projects/{projectId}/rankDistribution", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRankingDistribution getRankingDistribution(@PathVariable("projectId") String projectId,
                                                     @RequestParam(name = "userId", required = false) String userIdParam) {
        return rankingLoader.getRankingDistribution(projectId, getUserId(userIdParam))
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/rankDistribution", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRankingDistribution getRankingDistributionBySubject(@PathVariable("projectId") String projectId,
                                                              @PathVariable("subjectId") String subjectId,
                                                              @RequestParam(name = "userId", required = false) String userIdParam) {
        return rankingLoader.getRankingDistribution(projectId, getUserId(userIdParam), subjectId)
    }

    @RequestMapping(value = "/projects/{id}/customIconCss", method = RequestMethod.GET, produces = "text/css")
    @ResponseBody
    String getCustomIconCss(@PathVariable("id") String projectId) {
        return customIconFacade.generateCss(projectId)
    }

    private String getUserId(String userIdParam) {
        if (userIdParam) {
            return userInfoService.lookupUserId(userIdParam)
        } else {
            return userInfoService.currentUser.username
        }
    }
}
