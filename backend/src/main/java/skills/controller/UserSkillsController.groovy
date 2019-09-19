package skills.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.PublicProps
import skills.services.events.SkillEventResult
import skills.services.events.SkillEventsService
import skills.skillLoading.RankingLoader
import skills.skillLoading.SkillsLoader
import skills.skillLoading.model.*

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

    @Autowired
    PublicProps publicProps

    int getProvidedVersionOrReturnDefault(Integer versionParam) {
        if (versionParam != null) {
            return versionParam
        }

        return publicProps.getInt(PublicProps.UiProp.maxSkillVersion)
    }

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
                                         @RequestParam(name = 'version', required = false) Integer version) {
        return skillsLoader.loadOverallSummary(projectId, getUserId(userIdParam), getProvidedVersionOrReturnDefault(version))
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillSubjectSummary getSubjectSummary(@PathVariable("projectId") String projectId,
                                          @PathVariable("subjectId") String subjectId,
                                          @RequestParam(name = "userId", required = false) String userIdParam,
                                          @RequestParam(name = 'version', required = false) Integer version) {
        return skillsLoader.loadSubject(projectId, getUserId(userIdParam), subjectId, getProvidedVersionOrReturnDefault(version))
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/descriptions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    List<SkillDescription> getSubjectSkillsDescriptions(@PathVariable("projectId") String projectId,
                                                  @PathVariable("subjectId") String subjectId,
                                                  @RequestParam(name = 'version', required = false) Integer version) {
        return skillsLoader.loadSubjectDescriptions(projectId, subjectId, getProvidedVersionOrReturnDefault(version))
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
                                                 @RequestParam(name = 'version', required = false) Integer version) {
        List<SkillBadgeSummary> badgeSummaries = skillsLoader.loadBadgeSummaries(projectId, getUserId(userIdParam), getProvidedVersionOrReturnDefault(version))

        // add any global badges as well
        badgeSummaries.addAll(skillsLoader.loadGlobalBadgeSummaries(getUserId(userIdParam), getProvidedVersionOrReturnDefault(version)))
        return badgeSummaries
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}/descriptions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    List<SkillDescription> getBadgeSkillsDescriptions(@PathVariable("projectId") String projectId,
                                                        @PathVariable("badgeId") String badgeId,
                                                        @RequestParam(name = 'version', required = false) Integer version) {
        return skillsLoader.loadBadgeDescriptions(projectId, badgeId, getProvidedVersionOrReturnDefault(version))
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillBadgeSummary getBadgeSummary(@PathVariable("projectId") String projectId,
                                      @PathVariable("badgeId") String badgeId,
                                      @RequestParam(name = "userId", required = false) String userIdParam,
                                      @RequestParam(name = 'version', required = false) Integer version,
                                      @RequestParam(name = 'global', required = false) Boolean isGlobal) {
        if (isGlobal) {
            return skillsLoader.loadGlobalBadge(getUserId(userIdParam), badgeId, getProvidedVersionOrReturnDefault(version))
        } else {
            return skillsLoader.loadBadge(projectId, getUserId(userIdParam), badgeId, getProvidedVersionOrReturnDefault(version))
        }
    }

    @RequestMapping(value = "/projects/{projectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    UserPointHistorySummary getProjectsPointHistory(@PathVariable("projectId") String projectId,
                                                    @RequestParam(name = "userId", required = false) String userIdParam,
                                                    @RequestParam(name = 'version', required = false) Integer version) {
        return skillsLoader.loadPointHistorySummary(projectId, getUserId(userIdParam), 365, null, getProvidedVersionOrReturnDefault(version))
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    UserPointHistorySummary getSubjectsPointHistory(@PathVariable("projectId") String projectId,
                                                    @PathVariable("subjectId") String subjectId,
                                                    @RequestParam(name = "userId", required = false) String userIdParam,
                                                    @RequestParam(name = 'version', required = false) Integer version) {
        return skillsLoader.loadPointHistorySummary(projectId, getUserId(userIdParam), 365, subjectId, getProvidedVersionOrReturnDefault(version))
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
    @RequestMapping(value = "/projects/{projectId}/rankDistribution/usersPerLevel", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    List<UsersPerLevel> getUsersPerLevel(@PathVariable("projectId") String projectId) {
        return rankingLoader.getUserCountsPerLevel(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/rankDistribution/usersPerLevel", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    List<UsersPerLevel> getUsersPerLevelForSubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        return rankingLoader.getUserCountsPerLevel(projectId, false, subjectId)
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

    @RequestMapping(value = "/icons/customIconCss", method = RequestMethod.GET, produces = "text/css")
    @ResponseBody
    String getCustomGlogbalIconCss() {
        return customIconFacade.generateGlobalCss()
    }

    private String getUserId(String userIdParam) {
        if (userIdParam) {
            return userIdParam
        } else {
            return userInfoService.getCurrentUser().username
        }
    }
}
