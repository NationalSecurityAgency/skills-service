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
import groovy.lang.Closure;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Propagation;
import skills.PublicProps;
import skills.auth.AuthMode;
import skills.auth.UserAuthService;
import skills.auth.UserInfo;
import org.springframework.transaction.annotation.Transactional;
import skills.auth.UserInfoService;
import skills.auth.pki.PkiUserLookup;
import skills.controller.exceptions.ErrorCode;
import skills.controller.exceptions.SkillException;
import skills.controller.exceptions.SkillsValidator;
import skills.controller.request.model.BatchSkillEventRequest;
import skills.controller.request.model.SkillEventRequest;
import skills.services.ProjectErrorService;
import skills.services.UserAttrsService;
import skills.services.events.BatchSkillEventResult;
import skills.services.events.SkillEventResult;
import skills.services.events.SkillEventsService;
import skills.services.userActions.DashboardAction;
import skills.services.userActions.DashboardItem;
import skills.services.userActions.UserActionInfo;
import skills.services.userActions.UserActionsHistoryService;
import skills.storage.model.UserAttrs;
import skills.utils.RetryUtil;

import java.util.*;

@Component
public class AddSkillHelper {

    static final DateTimeFormatter DTF = ISODateTimeFormat.dateTimeNoMillis().withLocale(Locale.ENGLISH).withZoneUTC();
    private Logger log = LoggerFactory.getLogger(AddSkillHelper.class);
    @Autowired
    PublicProps publicProps;
    @Autowired
    UserInfoService userInfoService;
    @Autowired
    SkillEventsService skillsManagementFacade;
    @Autowired
    ProjectErrorService projectErrorService;

    @Autowired
    UserAuthService userAuthService;

    @Autowired
    UserActionsHistoryService userActionsHistoryService;

    @Autowired
    SkillEventProcessor skillEventProcessor;

    @Value("${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}")
    AuthMode authMode;
    @Autowired
    private UserAttrsService userAttrsService;

    @Transactional
    public BatchSkillEventResult addBatchSkillsForBatchUsers(String projectId, BatchSkillEventRequest batchSkillEventRequest) {
        Long requestedTimestamp = batchSkillEventRequest != null ? batchSkillEventRequest.getTimestamp() : null;
        List<String> skillIds = batchSkillEventRequest != null ? batchSkillEventRequest.getSkillIds() : null;
        String skillBatchString = !skillIds.isEmpty() ? String.join(", ", skillIds) : null;
        Date incomingDate = validateTime(projectId, requestedTimestamp);
        String currentUser = userInfoService.getCurrentUserId();
        BatchSkillEventResult batchResults = new BatchSkillEventResult();
        ArrayList<SkillEventResult> results = new ArrayList<>();

        for(String userIdToProcess : batchSkillEventRequest.getUserIds()) {
            String userId = "";

            try {
                if(authMode == AuthMode.PKI) {
                    String mode = "ID";
                    if (userIdToProcess.substring(0, 3).equalsIgnoreCase("CN=")) {
                        mode = "DN";
                    }
                    userId = userInfoService.getUserName(userIdToProcess, false, mode);
                    boolean userExists = userAuthService.userExists(userId);
                    if (!userExists) {
                        throw new SkillException("User [" + userIdToProcess + "] does not exist");
                    }
                } else {
                    log.info("User [" + userIdToProcess + "] does not exist, will be created");
                    userId = skillEventProcessor.createNewUser(userIdToProcess).getUserId();
                }
            } catch (SkillException e) {
                SkillEventResult res = createNewEventResult("User [" + userIdToProcess + "] could not be processed: " + e.getMessage(), projectId, "", userIdToProcess);
                results.add(res);
                log.error("Error applying skills, user [{}], error [{}]", userIdToProcess, res.getExplanation());
                continue;
            }

            if (log.isInfoEnabled()) {
                log.info("ReportSkills (ProjectId=[{}], SkillIds=[{}], CurrentUser=[{}], UserId=[{}], RequestDate=[{}])",
                        projectId, skillBatchString, userInfoService.getCurrentUserId(), userIdToProcess, toDateString(requestedTimestamp));
            }
            for (String skillId : skillIds) {
                SkillEventResult result = null;
                try {
                    result = skillEventProcessor.processSkillForUser(skillId, userId, projectId, incomingDate, currentUser);
                } catch(SkillException ske) {
                    log.error("Error applying skill [{}], user [{}], error [{}]", skillId, userId, ske.getMessage());

                    if (ske.getErrorCode() == ErrorCode.SkillNotFound) {
                        projectErrorService.invalidSkillReported(projectId, skillId);
                    }
                    result = createNewEventResult(ske.getMessage(), projectId, skillId, userId);
                } finally {
                    results.add(result);
                }
            }
        }

        batchResults.setResults(results);

        String userIdsString = !batchSkillEventRequest.getUserIds().isEmpty() ? String.join(", ", batchSkillEventRequest.getUserIds()) : null;
        HashMap<String, String> actionAttributes = new HashMap<>();
        actionAttributes.put("userIds", userIdsString);

        for (String skillId : skillIds) {
            UserActionInfo userActionInfo = new UserActionInfo();
            userActionInfo.setProjectId(projectId);
            userActionInfo.setItem(DashboardItem.SkillEvents);
            userActionInfo.setAction(DashboardAction.ManuallyAddSkillEvent);
            userActionInfo.setItemId(skillId);
            userActionInfo.setActionAttributes(actionAttributes);
            userActionsHistoryService.saveUserAction(userActionInfo);
        }

        return batchResults;
    }

    private Date validateTime(String projectId, Long requestedTimestamp) {
        if (requestedTimestamp != null && requestedTimestamp > 0) {
            //let's account for some possible clock drift
            SkillsValidator.isTrue(requestedTimestamp <= (System.currentTimeMillis() + 30000), "Skill Events may not be in the future", projectId);
            return new Date(requestedTimestamp);
        }
        return null;
    }

    private SkillEventResult createNewEventResult(String failureMessage, String projectId, String skillId, String userId) {
        SkillEventResult res = new SkillEventResult();
        res.setSkillApplied(false);
        res.setExplanation(failureMessage);
        res.setProjectId(projectId);
        res.setSkillId(skillId);
        res.setUserId(userId);
        return res;
    }

    public SkillEventResult addSkill(String projectId, String skillId, SkillEventRequest skillEventRequest) {
        String requestedUserId = skillEventRequest != null ? skillEventRequest.getUserId() : null;
        Long requestedTimestamp = skillEventRequest != null ? skillEventRequest.getTimestamp() : null;
        Boolean notifyIfSkillNotApplied = skillEventRequest != null ? skillEventRequest.getNotifyIfSkillNotApplied() : false;
        Boolean isRetry = skillEventRequest != null ? skillEventRequest.getIsRetry() : false;
        Date incomingDate = validateTime(projectId, requestedTimestamp);

        if (skillEventRequest != null && skillEventRequest.getApprovalRequestedMsg() != null) {
            int maxLength = publicProps.getInt(PublicProps.UiProp.maxSelfReportMessageLength);
            int msgLength = skillEventRequest.getApprovalRequestedMsg().length();
            SkillsValidator.isTrue(msgLength <= maxLength, String.format("message has length of %d, maximum allowed length is %d", msgLength, maxLength), projectId, skillId);
        }

        SkillEventResult result;
        String currentUser = userInfoService.getCurrentUserId();
        if (skillEventRequest != null && skillEventRequest.getDoNotRequireApproval()) {
            if (!userInfoService.isCurrentUserAProjectAdmin()) {
                throw new SkillException("Only project admins can apply approval-based skills without approval", projectId, skillId, ErrorCode.AccessDenied);
            }
        }
        boolean forAnotherUser = requestedUserId != null && currentUser != null && !requestedUserId.equalsIgnoreCase(currentUser);
        String idType = (skillEventRequest != null && StringUtils.isNotBlank(skillEventRequest.getUserId())) ? skillEventRequest.getIdType() : null;
        String userId = userInfoService.getUserName(requestedUserId, false, idType);
        if (log.isInfoEnabled()) {
            log.info("ReportSkill (ProjectId=[{}], SkillId=[{}], CurrentUser=[{}], RequestUser=[{}], UserId=[{}], RequestDate=[{}], IsRetry=[{}])",
                    projectId, skillId, userInfoService.getCurrentUserId(), requestedUserId, userId, toDateString(requestedTimestamp), isRetry.toString());
        }

        String prof = "retry-reportSkill";
        CProf.start(prof);
        try {
            final Date dataParam = incomingDate;
            Closure<SkillEventResult> closure = new Closure<SkillEventResult>(null) {
                @Override
                public SkillEventResult call() {
                    SkillEventsService.SkillApprovalParams skillApprovalParams = (skillEventRequest !=null && skillEventRequest.getApprovalRequestedMsg() != null) ?
                            new SkillEventsService.SkillApprovalParams(skillEventRequest.getApprovalRequestedMsg()) : SkillEventsService.getDefaultSkillApprovalParams();
                    skillApprovalParams.setForAnotherUser(forAnotherUser);
                    skillApprovalParams.setDoNotRequireApproval(skillEventRequest != null && skillEventRequest.getDoNotRequireApproval() != null ? skillEventRequest.getDoNotRequireApproval() : false);
                    return skillsManagementFacade.reportSkill(projectId, skillId, userId, notifyIfSkillNotApplied, dataParam, skillApprovalParams);
                }
            };
            result = (SkillEventResult) RetryUtil.withRetry(3, false, closure);
        } catch(SkillException ske) {
            if (ske.getErrorCode() == ErrorCode.SkillNotFound) {
                projectErrorService.invalidSkillReported(projectId, skillId);
            }
            throw ske;
        }finally {
            CProf.stop(prof);
        }
        return result;
    }

    private String toDateString(Long timestamp) {
        if (timestamp != null) {
            return DTF.print(timestamp);
        }

        return "";
    }
}
