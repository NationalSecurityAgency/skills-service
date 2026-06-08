/**
 * Copyright 2026 SkillTree
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
package skills.services.events

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.BatchSkillEventRequest
import skills.services.ProjectErrorService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.repos.UserAttrsRepo
import skills.utils.InputSanitizer

@Service
@Slf4j
class BatchReportService {

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    BatchReportSkillEventProcessor skillEventProcessor

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    ReportEventsValidateHelper reportEventsValidateHelper

    @Autowired
    ProjectErrorService projectErrorService

    @Transactional
    @Profile
    BatchSkillEventResult addBatchSkillsForBatchUsers(String projectId, BatchSkillEventRequest batchSkillEventRequest) {
        Long requestedTimestamp = batchSkillEventRequest.getTimestamp()
        List<String> skillIds = batchSkillEventRequest.getSkillIds()
        Date incomingDate = reportEventsValidateHelper.validateSkillEventTime(projectId, requestedTimestamp)
        BatchSkillEventResult batchResults = new BatchSkillEventResult()
        ArrayList<SkillEventResult> results = new ArrayList<>()

        Map<String, List<UserInfo>> userInfoCache = Collections.synchronizedMap(new HashMap<>())
        Map<String, String> userIdForDisplayCache = Collections.synchronizedMap(new HashMap<>())
        userIdLoop: for(String userIdToProcessTmp : batchSkillEventRequest.getUserIds()) {
            String userIdToProcess = InputSanitizer.sanitizeUserName(userIdToProcessTmp)
            for (String skillId : skillIds) {
                SkillEventResult result = null
                try {
                    if (!userIdToProcess || !userIdToProcessTmp?.equalsIgnoreCase(userIdToProcess)) {
                        throw new SkillException("Provided user id [${userIdToProcessTmp}] is not in a supported format")
                    }
                    result = skillEventProcessor.processSkillForUserInItsOwnTransaction(projectId, skillId, userIdToProcess, incomingDate,
                            batchSkillEventRequest.getUserSuggestOption(), userInfoCache, userIdForDisplayCache)
                } catch(SkillException ske) {
                    log.error("Error applying skill [{}], user [{}], error [{}]", skillId, userIdToProcess, ske.getMessage())

                    if (ske.getErrorCode() == ErrorCode.SkillNotFound) {
                        projectErrorService.invalidSkillReported(projectId, skillId)
                    }
                    result = createNewEventResult(ske.getMessage(), projectId, skillId, userIdToProcess)
                } finally {
                    results.add(result)
                }
            }
        }

        batchResults.setResults(results)

        String userIdsString = !batchSkillEventRequest.getUserIds().isEmpty() ? String.join(", ", batchSkillEventRequest.getUserIds()) : null
        HashMap<String, String> actionAttributes = new HashMap<>()
        actionAttributes.put("userIds", userIdsString)

        for (String skillId : skillIds) {
            UserActionInfo userActionInfo = new UserActionInfo()
            userActionInfo.setProjectId(projectId)
            userActionInfo.setItem(DashboardItem.SkillEvents)
            userActionInfo.setAction(DashboardAction.ManuallyAddSkillEvent)
            userActionInfo.setItemId(skillId)
            userActionInfo.setActionAttributes(actionAttributes)
            userActionsHistoryService.saveUserAction(userActionInfo)
        }

        return batchResults
    }

    private static SkillEventResult createNewEventResult(String failureMessage, String projectId, String skillId, String userId) {
        SkillEventResult res = new SkillEventResult()
        res.setSkillApplied(false)
        res.setExplanation(failureMessage)
        res.setProjectId(projectId)
        res.setSkillId(skillId)
        res.setUserId(userId)
        res.setUserIdForDisplay(userId)
        return res
    }
}
