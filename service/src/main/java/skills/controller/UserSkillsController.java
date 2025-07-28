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

import callStack.profiler.Profile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import skills.PublicProps;
import skills.auth.UserInfoService;
import skills.auth.aop.AdminOrApproverGetRequestUsersOnlyWhenUserIdSupplied;
import skills.auth.inviteOnly.InviteOnlyAccessDeniedException;
import skills.controller.exceptions.AttachmentValidator;
import skills.controller.exceptions.SkillException;
import skills.controller.exceptions.SkillsValidator;
import skills.controller.request.model.PageVisitRequest;
import skills.controller.request.model.SkillEventRequest;
import skills.controller.request.model.SkillsClientVersionRequest;
import skills.controller.result.model.RequestResult;
import skills.controller.result.model.TableResult;
import skills.controller.result.model.UploadAttachmentResult;
import skills.dbupgrade.DBUpgradeSafe;
import skills.icons.CustomIconFacade;
import skills.services.*;
import skills.services.admin.InviteOnlyProjectService;
import skills.services.events.SkillEventResult;
import skills.services.events.SkillEventsService;
import skills.skillLoading.RankingLoader;
import skills.skillLoading.SkillsLoader;
import skills.skillLoading.SkillsService;
import skills.skillLoading.model.*;
import skills.storage.model.Attachment;
import skills.storage.repos.SkillDefRepo;
import skills.utils.MetricsLogger;
import skills.utils.TablePageUtil;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@CrossOrigin(allowCredentials = "true", originPatterns = {"*"})
@RestController
@RequestMapping("/api")
@AdminOrApproverGetRequestUsersOnlyWhenUserIdSupplied
@skills.profile.EnableCallStackProf
class UserSkillsController {
    private Logger log = LoggerFactory.getLogger(UserSkillsController.class);

    static final DateTimeFormatter DTF = ISODateTimeFormat.dateTimeNoMillis().withLocale(Locale.ENGLISH).withZoneUTC();

    @Autowired
    private SkillEventsService skillsManagementFacade;

    @Autowired
    private SkillsLoader skillsLoader;

    @Autowired
    private SkillsService skillsService;

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

    @Value("${skills.config.ui.pointHistoryInDays:3650}")
    Integer maxDaysBack;

    @Autowired
    AddSkillHelper addSkillHelper;

    @Autowired
    MetricsLogger metricsLogger;

    @Autowired
    VersionService versionService;

    @Autowired
    AttachmentService attachmentService;

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService;

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService;

    @Autowired
    VideoCaptionsService videoCaptionsService;

    @Value("${skills.config.allowedAttachmentMimeTypes}")
    List<MediaType> allowedAttachmentMimeTypes;

    @Value("${skills.config.allowedVideoUploadMimeTypes}")
    List<MediaType> allowedMediaUploadTypes;

    @Value("${skills.config.maxAttachmentSize:10MB}")
    DataSize maxAttachmentSize;

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

    @RequestMapping(value = "/projects/{projectId}/skills", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Profile
    public TableResult getProjectSkills(HttpServletRequest request,
                                        @PathVariable("projectId") String projectId,
                                        @RequestParam(name = "userId", required = false) String userIdParam,
                                        @RequestParam(name = "idType", required = false) String idType,
                                        @RequestParam(required = false, defaultValue = "10") int limit,
                                        @RequestParam(required = false, defaultValue = "1") int page,
                                        @RequestParam(required = false, defaultValue = "skillName") String orderBy,
                                        @RequestParam(required = false, defaultValue = "true") Boolean ascending,
                                        @RequestParam(required = false, defaultValue = "") String query) {

        PageRequest pageRequest = TablePageUtil.createPagingRequestWithValidation(projectId, limit, page, orderBy, ascending);
        String userId = userInfoService.getUserName(userIdParam, true, idType);

        return skillsService.getSkillsForProject(userId, projectId, query, pageRequest);
    }
    @RequestMapping(value = "/projects/{projectId}/skillsSubjectsAndBadges", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Profile
    public List<SkillDefRepo.SkillWithAchievementDetails> getAllProjectSkillsSubjectsAndBadges(HttpServletRequest request,
                                                                                               @PathVariable("projectId") String projectId,
                                                                                               @RequestParam(name = "userId", required = false) String userIdParam,
                                                                                               @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return skillsService.getAllSkillsSubjectsAndBadgesWithAchievementDetails(projectId, userId);
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
                                                               @RequestParam(name = "version", required = false) Integer version,
                                                               @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
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
        return skillsLoader.loadSkillSummary(projectId, userId, null, skillId, null);
    }

    /**
     * Note: skill version is not applicable to a single skill;
     * there is no reason exclude dependency skills as the system will not allow to dependent skills with later version
     */
    @RequestMapping(value = "/projects/{projectId}/subjects/{subjectId}/skills/{skillId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SkillSummary getSkillSummary(@PathVariable("projectId") String projectId,
                                        @PathVariable("subjectId") String subjectId,
                                        @PathVariable("skillId") String skillId,
                                        @RequestParam(name = "userId", required = false) String userIdParam,
                                        @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return skillsLoader.loadSkillSummary(projectId, userId, null, skillId, subjectId);
    }

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/description", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, String> getSkillDescription(@PathVariable("projectId") String projectId,
                                                   @PathVariable("skillId") String skillId) {
        return skillsLoader.loadGroupDescription(projectId, skillId);
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
        return skillsLoader.loadSkillSummary(projectId, userId, crossProjectId, skillId, null);
    }

    /**
     * Note: skill version is not applicable to a single skill;
     * there is no reason exclude dependency skills as the system will not allow to dependent skills with later version
     */
    @RequestMapping(value = "/projects/{projectId}/projects/{crossProjectId}/subjects/{subjectId}/skills/{skillId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SkillSummary getCrossProjectSkillSummary(@PathVariable("projectId") String projectId,
                                                    @PathVariable("subjectId") String subjectId,
                                                    @PathVariable("crossProjectId") String crossProjectId,
                                                    @PathVariable("skillId") String skillId,
                                                    @RequestParam(name = "userId", required = false) String userIdParam,
                                                    @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return skillsLoader.loadSkillSummary(projectId, userId, crossProjectId, skillId, subjectId);
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
        return addSkillHelper.addSkill(projectId, skillId, skillEventRequest);
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

    @RequestMapping(value = "/projects/{projectId}/skills/visited/{skillId}", method = {RequestMethod.PUT, RequestMethod.POST}, produces = "application/json")
    @ResponseBody
    @Profile
    public RequestResult updateLastSkillIdViewed(@PathVariable("projectId") String projectId,
                                                 @PathVariable("skillId") String skillId) {
        skillsLoader.documentLastViewedSkillId(projectId, skillId);
        return RequestResult.success();
    }

    @RequestMapping(value = "/upload", method = {RequestMethod.PUT, RequestMethod.POST}, produces = "application/json")
    @ResponseBody
    @Profile
    public UploadAttachmentResult uploadFile(@RequestParam("file") MultipartFile file,
                                             @RequestParam(name = "projectId", required = false) String projectId,
                                             @RequestParam(name = "quizId", required = false) String quizId,
                                             @RequestParam(name = "skillId", required = false) String skillId) {
        log.info("Project ID ["+projectId+"], quizId ["+quizId+"], skillId ["+skillId+"]");
        SkillsValidator.isTrue(StringUtils.isBlank(projectId) || StringUtils.isBlank(quizId),
                "Attachment cannot be associated to both a projectId and a quizId");
        AttachmentValidator.isWithinMaxAttachmentSize(file.getSize(), maxAttachmentSize);
        AttachmentValidator.isAllowedAttachmentMimeType(file.getContentType(), allowedAttachmentMimeTypes);
        return attachmentService.saveAttachment(file, projectId, quizId, skillId);
    }

    @GetMapping(value = "/projects/{projectId}/skills/{skillId}/videoCaptions", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    @Profile
    public String getVideoCaptions(@PathVariable("projectId") String projectId, @PathVariable("skillId") String skillId) {
        return videoCaptionsService.getVideoCaptions(projectId, skillId);
    }

    @GetMapping(value = "/projects/{projectId}/skills/{skillId}/videoTranscript", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    @Profile
    public String getVideoTranscript(@PathVariable("projectId") String projectId, @PathVariable("skillId") String skillId) {
        return videoCaptionsService.getVideoTranscript(projectId, skillId);
    }

    @RequestMapping(value = "/download/{uuid}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Transactional(readOnly = true)
    public void download(@PathVariable("uuid") String uuid,
                         @RequestParam(name = "alwaysReturnContentDispositionForPdf", required = false, defaultValue = "false") Boolean alwaysReturnContentDispositionForPdf,
                         HttpServletResponse response) {
        Attachment attachment = attachmentService.getAttachment(uuid);
        if (attachment == null) {
            throw new SkillException("Attachment for uuid [" + uuid + "] does not exist");
        }

        if (attachment.getProjectId() != null && inviteOnlyProjectService.isInviteOnlyProject(attachment.getProjectId())) {
            String userId = userInfoService.getCurrentUserId();
            if (!inviteOnlyProjectService.isPrivateProjRoleOrAdminRole(attachment.getProjectId(), userId) && !userInfoService.isCurrentUserASuperDuperUser()) {
                throw new InviteOnlyAccessDeniedException("Access is denied", attachment.getProjectId());
            }
        }

        try (InputStream inputStream = attachment.getContent().getBinaryStream();
             OutputStream outputStream = response.getOutputStream()) {
            response.setContentType(attachment.getContentType());
            if (alwaysReturnContentDispositionForPdf || !StringUtils.equalsIgnoreCase(attachment.getContentType(), "application/pdf")) {
                response.setHeader("Content-Disposition", "attachment; filename=\"" + attachment.getFilename() + "\"");
            }

            String contentType = attachment.getContentType().toLowerCase();
            if (AttachmentValidator.isAllowedAttachmentMimeTypeBoolean(contentType, allowedMediaUploadTypes)) {
                response.setHeader("Content-Length", attachment.getSize().toString());
                long attachmentSize = attachment.getSize() - 1;
                response.setHeader("Content-Range", "bytes 0-" + attachmentSize + "/" + attachment.getSize().toString());
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            }
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            throw new SkillException("Error closing stream resources", e);
        }
    }

}
