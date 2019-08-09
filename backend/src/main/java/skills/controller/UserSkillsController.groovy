package skills.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.services.events.SkillEventResult
import skills.skillLoading.RankingLoader
import skills.skillLoading.SkillsLoader
import skills.skillLoading.model.OverallSkillSummary
import skills.skillLoading.model.SkillBadgeSummary
import skills.skillLoading.model.SkillDependencyInfo
import skills.skillLoading.model.SkillDescription
import skills.skillLoading.model.SkillSubjectSummary
import skills.skillLoading.model.SkillSummary
import skills.skillLoading.model.SkillsRanking
import skills.skillLoading.model.SkillsRankingDistribution
import skills.skillLoading.model.UserPointHistorySummary
import skills.services.events.SkillEventsService
import skills.utils.Constants

@CrossOrigin(allowCredentials = 'true')
@RestController
@RequestMapping("/api")
@Slf4j
@CompileStatic
@skills.auth.aop.AdminUsersOnlyWhenUserIdSupplied
@skills.profile.EnableCallStackProf
class UserSkillsController {

    @Autowired
    SkillEventsService skillsManagementFacade

    @Autowired
    SkillsLoader skillsLoader

    @Autowired
    skills.auth.UserInfoService userInfoService

    @Autowired
    skills.icons.CustomIconFacade customIconFacade

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
    SkillSubjectSummary getSubjectSummary(@PathVariable("projectId") String projectId,
                                          @PathVariable("subjectId") String subjectId,
                                          @RequestParam(name = "userId", required = false) String userIdParam,
                                          @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadSubject(projectId, getUserId(userIdParam), subjectId, version)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/descriptions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    List<SkillDescription> getSubjectSkillsDescriptions(@PathVariable("projectId") String projectId,
                                                  @PathVariable("subjectId") String subjectId,
                                                  @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadSubjectDescriptions(projectId, subjectId, version)
    }

    /**
     * Note: skill version is not applicable to a single skill;
     * there is no reason exclude dependency skills as the system will not allow to dependent skills with later version
     */
    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillSummary getSkillSummary(@PathVariable("projectId") String projectId,
                                 @PathVariable("skillId") String skillId,
                                 @RequestParam(name = "userId", required = false) String userIdParam) {
        return skillsLoader.loadSkillSummary(projectId, getUserId(userIdParam), null, skillId)
    }

    /**
     * Note: skill version is not applicable to a single skill;
     * there is no reason exclude dependency skills as the system will not allow to dependent skills with later version
     */
    @RequestMapping(value = "/projects/{projectId}/projects/{crossProjectId}/skills/{skillId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillSummary getCrossProjectSkillSummary(@PathVariable("projectId") String projectId,
                                             @PathVariable("crossProjectId") String crossProjectId,
                                             @PathVariable("skillId") String skillId,
                                             @RequestParam(name = "userId", required = false) String userIdParam) {
        return skillsLoader.loadSkillSummary(projectId, getUserId(userIdParam), crossProjectId, skillId)
    }


    @RequestMapping(value = "/projects/{projectId}/badges/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    List<SkillBadgeSummary>  getAllBadgesSummary(@PathVariable("projectId") String projectId,
                                                 @RequestParam(name = "userId", required = false) String userIdParam,
                                                 @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadBadgeSummaries(projectId, getUserId(userIdParam), version)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}/descriptions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    List<SkillDescription> getBadgeSkillsDescriptions(@PathVariable("projectId") String projectId,
                                                        @PathVariable("badgeId") String badgeId,
                                                        @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadBadgeDescriptions(projectId, badgeId, version)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillBadgeSummary getBadgeSummary(@PathVariable("projectId") String projectId,
                                      @PathVariable("badgeId") String badgeId,
                                      @RequestParam(name = "userId", required = false) String userIdParam,
                                      @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadBadge(projectId, getUserId(userIdParam), badgeId, version)
    }

    @RequestMapping(value = "/projects/{projectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    UserPointHistorySummary getProjectsPointHistory(@PathVariable("projectId") String projectId,
                                                    @RequestParam(name = "userId", required = false) String userIdParam,
                                                    @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadPointHistorySummary(projectId, getUserId(userIdParam), 365, null, version)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    UserPointHistorySummary getSubjectsPointHistory(@PathVariable("projectId") String projectId,
                                                    @PathVariable("subjectId") String subjectId,
                                                    @RequestParam(name = "userId", required = false) String userIdParam,
                                                    @RequestParam(name = 'version', required = false, defaultValue = Constants.MAX_VERSION_STRING) Integer version) {
        return skillsLoader.loadPointHistorySummary(projectId, getUserId(userIdParam), 365, subjectId, version)
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
    SkillEventResult addSkill(@PathVariable("projectId") String projectId,
                              @PathVariable("skillId") String skillId,
                              @RequestBody(required = false) skills.controller.request.model.SkillEventRequest skillEventRequest) {
        Date incomingDate = skillEventRequest?.timestamp != null ? new Date(skillEventRequest.timestamp) : new Date()
        skillsManagementFacade.reportSkill(projectId, skillId,  getUserId(skillEventRequest?.userId), incomingDate)
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
            return userIdParam
        } else {
            return userInfoService.getCurrentUser().username
        }
    }
}
