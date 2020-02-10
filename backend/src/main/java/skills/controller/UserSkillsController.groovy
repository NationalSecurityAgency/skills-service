package skills.controller

import callStack.profiler.CProf
import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.PublicProps
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.SkillEventRequest
import skills.icons.CustomIconFacade
import skills.services.events.SkillEventResult
import skills.services.events.SkillEventsService
import skills.skillLoading.RankingLoader
import skills.skillLoading.SkillsLoader
import skills.skillLoading.model.*
import skills.utils.RetryUtil

@CrossOrigin(allowCredentials = 'true')
@RestController
@RequestMapping("/api")
@Slf4j
@CompileStatic
@skills.auth.aop.AdminUsersOnlyWhenUserIdSupplied
@skills.profile.EnableCallStackProf
class UserSkillsController {

    static final DateTimeFormatter DTF = ISODateTimeFormat.dateTimeNoMillis().withLocale(Locale.ENGLISH).withZoneUTC()

    @Autowired
    SkillEventsService skillsManagementFacade

    @Autowired
    SkillsLoader skillsLoader

    @Autowired
    UserInfoService userInfoService

    @Autowired
    CustomIconFacade customIconFacade

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
        String userId = getUserId(userIdParam)
        return skillsLoader.getUserLevel(projectId, userId)
    }

    @RequestMapping(value = "/projects/{projectId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    @Profile
    OverallSkillSummary getSkillsSummary(@PathVariable("projectId") String projectId,
                                         @RequestParam(name = "userId", required = false) String userIdParam,
                                         @RequestParam(name = 'version', required = false) Integer version) {
        String userId = getUserId(userIdParam)
        return skillsLoader.loadOverallSummary(projectId, userId, getProvidedVersionOrReturnDefault(version))
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillSubjectSummary getSubjectSummary(@PathVariable("projectId") String projectId,
                                          @PathVariable("subjectId") String subjectId,
                                          @RequestParam(name = "userId", required = false) String userIdParam,
                                          @RequestParam(name = 'version', required = false) Integer version) {
        String userId = getUserId(userIdParam)
        return skillsLoader.loadSubject(projectId, userId, subjectId, getProvidedVersionOrReturnDefault(version))
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
        String userId = getUserId(userIdParam)
        return skillsLoader.loadSkillSummary(projectId, userId, null, skillId)
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
        String userId = getUserId(userIdParam)
        return skillsLoader.loadSkillSummary(projectId, userId, crossProjectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    List<SkillBadgeSummary>  getAllBadgesSummary(@PathVariable("projectId") String projectId,
                                                 @RequestParam(name = "userId", required = false) String userIdParam,
                                                 @RequestParam(name = 'version', required = false) Integer version) {
        String userId = getUserId(userIdParam)
        List<SkillBadgeSummary> badgeSummaries = skillsLoader.loadBadgeSummaries(projectId, userId, getProvidedVersionOrReturnDefault(version))

        // add any global badges as well
        badgeSummaries.addAll(skillsLoader.loadGlobalBadgeSummaries(userId, projectId, getProvidedVersionOrReturnDefault(version)))
        return badgeSummaries
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}/descriptions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    List<SkillDescription> getBadgeSkillsDescriptions(@PathVariable("projectId") String projectId,
                                                      @PathVariable("badgeId") String badgeId,
                                                      @RequestParam(name = 'version', required = false) Integer version,
                                                      @RequestParam(name = 'global', required = false) Boolean isGlobal) {
        if (isGlobal) {
            return skillsLoader.loadGlobalBadgeDescriptions(badgeId, getProvidedVersionOrReturnDefault(version))
        } else {
            return skillsLoader.loadBadgeDescriptions(projectId, badgeId, getProvidedVersionOrReturnDefault(version))
        }
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillBadgeSummary getBadgeSummary(@PathVariable("projectId") String projectId,
                                      @PathVariable("badgeId") String badgeId,
                                      @RequestParam(name = "userId", required = false) String userIdParam,
                                      @RequestParam(name = 'version', required = false) Integer version,
                                      @RequestParam(name = 'global', required = false) Boolean isGlobal) {
        String userId = getUserId(userIdParam)
        if (isGlobal) {
            return skillsLoader.loadGlobalBadge(userId, projectId, badgeId, getProvidedVersionOrReturnDefault(version))
        } else {
            return skillsLoader.loadBadge(projectId, userId, badgeId, getProvidedVersionOrReturnDefault(version))
        }
    }

    @RequestMapping(value = "/projects/{projectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    UserPointHistorySummary getProjectsPointHistory(@PathVariable("projectId") String projectId,
                                                    @RequestParam(name = "userId", required = false) String userIdParam,
                                                    @RequestParam(name = 'version', required = false) Integer version) {
        String userId = getUserId(userIdParam)
        return skillsLoader.loadPointHistorySummary(projectId, userId, 365, null, getProvidedVersionOrReturnDefault(version))
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    UserPointHistorySummary getSubjectsPointHistory(@PathVariable("projectId") String projectId,
                                                    @PathVariable("subjectId") String subjectId,
                                                    @RequestParam(name = "userId", required = false) String userIdParam,
                                                    @RequestParam(name = 'version', required = false) Integer version) {
        String userId = getUserId(userIdParam)
        return skillsLoader.loadPointHistorySummary(projectId, userId, 365, subjectId, getProvidedVersionOrReturnDefault(version))
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependencies", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillDependencyInfo loadSkillDependencyInfo(@PathVariable("projectId") String projectId,
                                                @PathVariable("skillId") String skillId,
                                                @RequestParam(name = "userId", required = false) String userIdParam) {
        String userId = getUserId(userIdParam)
        return skillsLoader.loadSkillDependencyInfo(projectId, userId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    @CompileStatic
    @Profile
    SkillEventResult addSkill(@PathVariable("projectId") String projectId,
                              @PathVariable("skillId") String skillId,
                              @RequestBody(required = false) SkillEventRequest skillEventRequest) {

        Date incomingDate = null

        if (skillEventRequest?.timestamp){
            //let's account for some possible clock drift
            SkillsValidator.isTrue(skillEventRequest.timestamp <= (System.currentTimeMillis()+30000), "Skill Events may not be in the future", projectId, skillId)
            incomingDate = new Date(skillEventRequest.timestamp)
        } else {
            incomingDate = new Date()
        }

        SkillEventResult result
        String userId = getUserId(skillEventRequest?.userId)
        if (log.isInfoEnabled()) {
            log.info("ReportSkill (ProjectId=[${projectId}], SkillId=[${skillId}], CurrentUser=[${userInfoService.getCurrentUserId()}], RequestUser=[${skillEventRequest?.userId}], RequestDate=[${toDateString(skillEventRequest?.timestamp)}])")
        }

        CProf.prof('retry-reportSkill') {
            result = (SkillEventResult) RetryUtil.withRetry(3, false) {skillsManagementFacade.reportSkill(projectId, skillId, userId, skillEventRequest?.notifyIfNotApplied, incomingDate)
            }
        }
        return result
    }

    @RequestMapping(value = "/projects/{projectId}/rank", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRanking getRanking(@PathVariable("projectId") String projectId,
                             @RequestParam(name = "userId", required = false) String userIdParam) {
        String userId = getUserId(userIdParam)
        return rankingLoader.getUserSkillsRanking(projectId, userId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/rank", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRanking getRankingBySubject(@PathVariable("projectId") String projectId,
                                      @PathVariable("subjectId") String subjectId,
                                      @RequestParam(name = "userId", required = false) String userIdParam) {
        String userId = getUserId(userIdParam)
        return rankingLoader.getUserSkillsRanking(projectId, userId, subjectId)
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
        String userId = getUserId(userIdParam)
        return rankingLoader.getRankingDistribution(projectId, userId)
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/rankDistribution", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillsRankingDistribution getRankingDistributionBySubject(@PathVariable("projectId") String projectId,
                                                              @PathVariable("subjectId") String subjectId,
                                                              @RequestParam(name = "userId", required = false) String userIdParam) {
        String userId = getUserId(userIdParam)
        return rankingLoader.getRankingDistribution(projectId, userId, subjectId)
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
        return userInfoService.getUserName(userIdParam)
    }

    private String toDateString(Long timestamp) {
        if (timestamp != null) {
            return DTF.print(timestamp)
        }
    }
}
