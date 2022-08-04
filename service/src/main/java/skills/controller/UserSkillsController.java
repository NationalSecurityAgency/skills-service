/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.controller;

import callStack.profiler.CProf;
import callStack.profiler.Profile;
import groovy.lang.Closure;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import skills.PublicProps;
import skills.auth.UserInfoService;
import skills.controller.exceptions.ErrorCode;
import skills.controller.exceptions.SkillException;
import skills.controller.exceptions.SkillsValidator;
import skills.controller.request.model.PageVisitRequest;
import skills.controller.request.model.SkillEventRequest;
import skills.controller.request.model.SkillsClientVersionRequest;
import skills.controller.result.model.RequestResult;
import skills.dbupgrade.DBUpgradeSafe;
import skills.icons.CustomIconFacade;
import skills.services.ProjectErrorService;
import skills.services.SelfReportingService;
import skills.services.VersionService;
import skills.services.events.SkillEventResult;
import skills.services.events.SkillEventsService;
import skills.skillLoading.RankingLoader;
import skills.skillLoading.SkillsLoader;
import skills.skillLoading.model.*;
import skills.utils.MetricsLogger;
import skills.utils.RetryUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

@CrossOrigin(allowCredentials = "true", originPatterns = {"*"})
@RestController
@RequestMapping("/api")
@skills.auth.aop.AdminUsersOnlyWhenUserIdSupplied
@skills.profile.EnableCallStackProf
class UserSkillsController {
    private Logger log = LoggerFactory.getLogger(UserSkillsController.class);

    static final DateTimeFormatter DTF = ISODateTimeFormat.dateTimeNoMillis().withLocale(Locale.ENGLISH).withZoneUTC();

    @Autowired
    private SkillEventsService skillsManagementFacade;

    @Autowired
    private SkillsLoader skillsLoader;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private CustomIconFacade customIconFacade;

    @Autowired
    private RankingLoader rankingLoader;

    @Autowired
    private PublicProps publicProps;

    @Autowired
    SelfReportingService selfReportingService;

    @Value("${skills.config.ui.pointHistoryInDays:1825}")
    Integer maxDaysBack;

    @Value("${skills.config.ui.skillHistoryInDays:1825}")
    Integer maxDaysBackForSkill;

    @Autowired
    AddSkillHelper addSkillHelper;

    @Autowired
    MetricsLogger metricsLogger;

    @Autowired
    VersionService versionService;

    private int getProvidedVersionOrReturnDefault(Integer versionParam) {
        if (versionParam != null) {
            return versionParam;
        }

        return publicProps.getInt(PublicProps.UiProp.maxSkillVersion);
    }

    @DBUpgradeSafe
    @RequestMapping(value = "/projects/{projectId}/skillsClientVersion", method = {RequestMethod.PUT, RequestMethod.POST}, produces = "application/json")
    @ResponseBody
    RequestResult setSkillsClientVersion(@PathVariable(name = "projectId") String projectId,
                                         @RequestBody SkillsClientVersionRequest skillsClientVersion,
                                         @RequestHeader(value = "User-Agent") String userAgent,
                                         @RequestHeader(value = "X-FORWARDED-FOR", required = false) String remoteAddr,
                                         HttpServletRequest request) {
        String remoteIp = StringUtils.isNotBlank(remoteAddr) ? remoteAddr : request.getRemoteAddr();
        log.info("SkillsClient ["+skillsClientVersion.getSkillsClientVersion()+"], " +
                "projectId ["+projectId+"], " +
                "User-Agent ["+userAgent+"], " +
                "remoteIp ["+remoteIp+"]");

        versionService.compareClientVersions(skillsClientVersion.getSkillsClientVersion(), projectId);

        return RequestResult.success();
    }

    @RequestMapping(value = "/projects/{projectId}/level", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Integer getUserLevel(@PathVariable(name = "projectId") String projectId,
                                @RequestParam(name = "userId", required = false) String userIdParam) {
        String userId = userInfoService.getUserName(userIdParam);
        return skillsLoader.getUserLevel(projectId, userId);
    }

    @RequestMapping(value = "/projects/{projectId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Profile
    public OverallSkillSummary getSkillsSummary(HttpServletRequest request,
                                                @PathVariable("projectId") String projectId,
                                                @RequestParam(name = "userId", required = false) String userIdParam,
                                                @RequestParam(name = "version", required = false) Integer version,
                                                @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);

        log.debug("userId is {} and userIdParam is {}", userId, userIdParam);
        return skillsLoader.loadOverallSummary(projectId, userId, getProvidedVersionOrReturnDefault(version));
    }

    private boolean isRequestFromDashboard(HttpServletRequest request) throws UnknownHostException{
            InetAddress requestorIp = InetAddress.getByName(request.getRemoteAddr());
            log.debug("remote port: [{}], local port: [{}]", request.getRemotePort(), request.getLocalPort());
            return (requestorIp.isAnyLocalAddress() || requestorIp.isLoopbackAddress())
                    && (request.getRemotePort() == request.getLocalPort());
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SkillSubjectSummary getSubjectSummary(@PathVariable("projectId") String projectId,
                                                 @PathVariable("subjectId") String subjectId,
                                                 @RequestParam(name = "userId", required = false) String userIdParam,
                                                 @RequestParam(name = "version", required = false) Integer version,
                                                 @RequestParam(name = "idType", required = false) String idType,
                                                 @RequestParam(name = "includeSkills", required = false, defaultValue = "true") String includeSkills) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return skillsLoader.loadSubject(projectId, userId, subjectId, getProvidedVersionOrReturnDefault(version), Boolean.valueOf(includeSkills));
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/descriptions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<SkillDescription> getSubjectSkillsDescriptions(@PathVariable("projectId") String projectId,
                                                               @PathVariable("subjectId") String subjectId,
                                                               @RequestParam(name = "userId", required = false) String userIdParam,
                                                               @RequestParam(name = "version", required = false) Integer version) {
        String userId = userInfoService.getUserName(userIdParam, true);
        return skillsLoader.loadSubjectDescriptions(projectId, subjectId, userId, getProvidedVersionOrReturnDefault(version));
    }

    /**
     * Note: skill version is not applicable to a single skill;
     * there is no reason exclude dependency skills as the system will not allow to dependent skills with later version
     */
    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SkillSummary getSkillSummary(@PathVariable("projectId") String projectId,
                                        @PathVariable("skillId") String skillId,
                                        @RequestParam(name = "userId", required = false) String userIdParam,
                                        @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return skillsLoader.loadSkillSummary(projectId, userId, null, skillId);
    }

    /**
     * Note: skill version is not applicable to a single skill;
     * there is no reason exclude dependency skills as the system will not allow to dependent skills with later version
     */
    @RequestMapping(value = "/projects/{projectId}/projects/{crossProjectId}/skills/{skillId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SkillSummary getCrossProjectSkillSummary(@PathVariable("projectId") String projectId,
                                                    @PathVariable("crossProjectId") String crossProjectId,
                                                    @PathVariable("skillId") String skillId,
                                                    @RequestParam(name = "userId", required = false) String userIdParam,
                                                    @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return skillsLoader.loadSkillSummary(projectId, userId, crossProjectId, skillId);
    }

    @RequestMapping(value = "/projects/{projectId}/badges/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<SkillBadgeSummary> getAllBadgesSummary(@PathVariable("projectId") String projectId,
                                                       @RequestParam(name = "userId", required = false) String userIdParam,
                                                       @RequestParam(name = "version", required = false) Integer version,
                                                       @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        List<SkillBadgeSummary> badgeSummaries = skillsLoader.loadBadgeSummaries(projectId, userId, getProvidedVersionOrReturnDefault(version));

        // add any global badges as well
        badgeSummaries.addAll(skillsLoader.loadGlobalBadgeSummaries(userId, projectId, getProvidedVersionOrReturnDefault(version)));
        return badgeSummaries;
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}/descriptions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<SkillDescription> getBadgeSkillsDescriptions(@PathVariable("projectId") String projectId,
                                                             @PathVariable("badgeId") String badgeId,
                                                             @RequestParam(name = "version", required = false) Integer version,
                                                             @RequestParam(name = "userId", required = false) String userIdParam,
                                                             @RequestParam(name = "global", required = false) Boolean isGlobal) {
        String userId = userInfoService.getUserName(userIdParam, true);
        if (isGlobal != null && isGlobal) {
            return skillsLoader.loadGlobalBadgeDescriptions(badgeId, userId, getProvidedVersionOrReturnDefault(version));
        } else {
            return skillsLoader.loadBadgeDescriptions(projectId, badgeId, userId, getProvidedVersionOrReturnDefault(version));
        }
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SkillBadgeSummary getBadgeSummary(@PathVariable("projectId") String projectId,
                                             @PathVariable("badgeId") String badgeId,
                                             @RequestParam(name = "userId", required = false) String userIdParam,
                                             @RequestParam(name = "version", required = false) Integer version,
                                             @RequestParam(name = "global", required = false) Boolean isGlobal,
                                             @RequestParam(name = "idType", required = false) String idType,
                                             @RequestParam(name = "includeSkills", required = false, defaultValue = "true") String includeSkills) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        if (isGlobal != null && isGlobal) {
            return skillsLoader.loadGlobalBadge(userId, projectId, badgeId, getProvidedVersionOrReturnDefault(version), Boolean.valueOf(includeSkills));
        } else {
            return skillsLoader.loadBadge(projectId, userId, badgeId, getProvidedVersionOrReturnDefault(version), Boolean.valueOf(includeSkills));
        }
    }

    @RequestMapping(value = "/projects/{projectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public UserPointHistorySummary getProjectsPointHistory(@PathVariable("projectId") String projectId,
                                                           @RequestParam(name = "userId", required = false) String userIdParam,
                                                           @RequestParam(name = "version", required = false) Integer version,
                                                           @RequestParam(name = "idType", required = false) String idType
                                                           ) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return skillsLoader.loadPointHistorySummary(projectId, userId, maxDaysBack, null, getProvidedVersionOrReturnDefault(version));
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/pointHistory", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public UserPointHistorySummary getSubjectsPointHistory(@PathVariable("projectId") String projectId,
                                                           @PathVariable("subjectId") String subjectId,
                                                           @RequestParam(name = "userId", required = false) String userIdParam,
                                                           @RequestParam(name = "version", required = false) Integer version,
                                                           @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return skillsLoader.loadPointHistorySummary(projectId, userId, maxDaysBack, subjectId, getProvidedVersionOrReturnDefault(version));
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/dependencies", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SkillDependencyInfo loadSkillDependencyInfo(@PathVariable("projectId") String projectId,
                                                       @PathVariable("skillId") String skillId,
                                                       @RequestParam(name = "userId", required = false) String userIdParam,
                                                       @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return skillsLoader.loadSkillDependencyInfo(projectId, userId, skillId);
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}", method = {RequestMethod.PUT, RequestMethod.POST}, produces = "application/json")
    @ResponseBody
    @Profile
    public SkillEventResult addSkill(@PathVariable("projectId") String projectId,
                                     @PathVariable("skillId") String skillId,
                                     @RequestBody(required = false) SkillEventRequest skillEventRequest) {
        return addSkillHelper.addSkill(projectId, skillId, skillEventRequest, maxDaysBackForSkill);
    }

    @RequestMapping(value = "/projects/{projectId}/rank", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SkillsRanking getRanking(@PathVariable("projectId") String projectId,
                                    @RequestParam(name = "userId", required = false) String userIdParam,
                                    @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return rankingLoader.getUserSkillsRanking(projectId, userId);
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/rank", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SkillsRanking getRankingBySubject(@PathVariable("projectId") String projectId,
                                             @PathVariable("subjectId") String subjectId,
                                             @RequestParam(name = "userId", required = false) String userIdParam,
                                             @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return rankingLoader.getUserSkillsRanking(projectId, userId, subjectId);
    }

    @RequestMapping(value = "/projects/{projectId}/rankDistribution/usersPerLevel", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<UsersPerLevel> getUsersPerLevel(@PathVariable("projectId") String projectId) {
        return rankingLoader.getUserCountsPerLevel(projectId);
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/rankDistribution/usersPerLevel", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<UsersPerLevel> getUsersPerLevelForSubject(@PathVariable("projectId") String projectId, @PathVariable("subjectId") String subjectId) {
        return rankingLoader.getUserCountsPerLevel(projectId, false, subjectId);
    }


    @RequestMapping(value = "/projects/{projectId}/rankDistribution", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SkillsRankingDistribution getRankingDistribution(@PathVariable("projectId") String projectId,
                                                            @RequestParam(name = "userId", required = false) String userIdParam,
                                                            @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return rankingLoader.getRankingDistribution(projectId, userId);
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/rankDistribution", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SkillsRankingDistribution getRankingDistributionBySubject(@PathVariable("projectId") String projectId,
                                                                     @PathVariable("subjectId") String subjectId,
                                                                     @RequestParam(name = "userId", required = false) String userIdParam,
                                                                     @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return rankingLoader.getRankingDistribution(projectId, userId, subjectId);
    }

    @RequestMapping(value = "/projects/{id}/customIconCss", method = RequestMethod.GET, produces = "text/css")
    @ResponseBody
    public String getCustomIconCss(@PathVariable("id") String projectId) {
        return customIconFacade.generateCss(projectId);
    }

    @RequestMapping(value = "/icons/customIconCss", method = RequestMethod.GET, produces = "text/css")
    @ResponseBody
    public String getCustomGlogbalIconCss() {
        return customIconFacade.generateGlobalCss();
    }

    private String toDateString(Long timestamp) {
        if (timestamp != null) {
            return DTF.print(timestamp);
        }

        return "";
    }


    @RequestMapping(value = "/projects/{projectId}/rejections/{id}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public RequestResult removeRejectionFromView(@PathVariable("projectId") String projectId,
                                                 @PathVariable("id") Integer approvalId,
                                                 @RequestParam(name = "userId", required = false) String userIdParam,
                                                 @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        selfReportingService.removeRejectionFromView(projectId, userId, approvalId);

        return RequestResult.success();
    }

    @RequestMapping(value = "/projects/{projectId}/leaderboard", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public LeaderboardRes getLeaderboard(@PathVariable("projectId") String projectId,
                                    @RequestParam(name = "userId", required = false) String userIdParam,
                                    @RequestParam(name = "idType", required = false) String idType,
                                    @RequestParam(name = "type", required = false) LeaderboardRes.Type type) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return rankingLoader.getLeaderboard(projectId, userId, type);
    }

    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/leaderboard", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public LeaderboardRes getSubjectLeaderboard(@PathVariable("projectId") String projectId,
                                                @PathVariable("subjectId") String subjectId,
                                                @RequestParam(name = "userId", required = false) String userIdParam,
                                                @RequestParam(name = "idType", required = false) String idType,
                                                @RequestParam(name = "type", required = false) LeaderboardRes.Type type) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return rankingLoader.getLeaderboard(projectId, userId, type, subjectId);
    }

    @RequestMapping(value = "/pageVisit", method = {RequestMethod.PUT, RequestMethod.POST}, produces = "application/json")
    @ResponseBody
    @Profile
    public RequestResult pageVisit(@RequestBody(required = true) PageVisitRequest pageVisitRequest) {
        metricsLogger.logPageVisit(pageVisitRequest);
        return RequestResult.success();
    }
}
