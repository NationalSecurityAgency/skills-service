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
import org.springframework.stereotype.Component;
import skills.PublicProps;
import skills.auth.UserInfoService;
import skills.controller.exceptions.ErrorCode;
import skills.controller.exceptions.SkillException;
import skills.controller.exceptions.SkillsValidator;
import skills.controller.request.model.SkillEventRequest;
import skills.services.ProjectErrorService;
import skills.services.events.SkillEventResult;
import skills.services.events.SkillEventsService;
import skills.utils.RetryUtil;

import java.util.Date;
import java.util.Locale;

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

    public SkillEventResult addSkill(String projectId, String skillId, SkillEventRequest skillEventRequest) {
        String requestedUserId = skillEventRequest != null ? skillEventRequest.getUserId() : null;
        Long requestedTimestamp = skillEventRequest != null ? skillEventRequest.getTimestamp() : null;
        Boolean notifyIfSkillNotApplied = skillEventRequest != null ? skillEventRequest.getNotifyIfSkillNotApplied() : false;
        Boolean isRetry = skillEventRequest != null ? skillEventRequest.getIsRetry() : false;

        Date incomingDate = null;

        if (skillEventRequest != null && requestedTimestamp != null && requestedTimestamp > 0) {
            //let's account for some possible clock drift
            SkillsValidator.isTrue(requestedTimestamp <= (System.currentTimeMillis() + 30000), "Skill Events may not be in the future", projectId, skillId);
            incomingDate = new Date(requestedTimestamp);
        }

        if (skillEventRequest != null && skillEventRequest.getSkillApprovalParams() != null && skillEventRequest.getSkillApprovalParams().getApprovalRequestedMsg() != null) {
            int maxLength = publicProps.getInt(PublicProps.UiProp.maxSelfReportMessageLength);
            String approvalMsg = skillEventRequest.getSkillApprovalParams().getApprovalRequestedMsg();
            int msgLength = approvalMsg.length();
            SkillsValidator.isTrue(msgLength <= maxLength, String.format("message has length of %d, maximum allowed length is %d", msgLength, maxLength), projectId, skillId);
        }

        SkillEventResult result;
        String currentUser = userInfoService.getCurrentUserId();
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
                    SkillEventsService.SkillApprovalParams skillApprovalParams = skillEventRequest.getSkillApprovalParams();
                    skillApprovalParams.setForAnotherUser(forAnotherUser);
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
