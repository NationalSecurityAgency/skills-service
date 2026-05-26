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
import org.apache.commons.collections4.CollectionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import skills.auth.AuthMode
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.pki.PkiUserLookup
import skills.controller.exceptions.SkillException
import skills.storage.repos.UserAttrsRepo

@Component
@Slf4j
class BatchReportSkillEventProcessor {

    @Autowired
    SkillEventsService skillsManagementFacade;

    @Autowired(required = false)
    PkiUserLookup pkiUserLookup;

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Value('#{"${skills.batchManualSkillEvents.dnCheckStr:CN=}"}')
    String dnCheckStr

    private final static String notFoundToken = "ST_TOKEN_USER_NOT_FOUND"

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Profile
    SkillEventResult processSkillForUserInItsOwnTransaction(String projectId, String skillId, String userId, Date incomingDate, String userSuggestOption,
                                                            Map<String, List<UserInfo>> userInfoCache,
                                                            Map<String, String> userIdForDisplayCache) {
        String userIdToProcess = getUserName(userId, userSuggestOption, userInfoCache);
        SkillEventsService.SkillApprovalParams skillApprovalParams = SkillEventsService.getDefaultSkillApprovalParams();
        skillApprovalParams.setForAnotherUser(true);
        skillApprovalParams.setDoNotRequireApproval(true)
        SkillEventResult res = skillsManagementFacade.reportSkill(projectId, skillId, userIdToProcess, true, incomingDate, skillApprovalParams);

        handleUserIdForDisplay(userIdForDisplayCache, userIdToProcess, res)

        return res
    }

    private void handleUserIdForDisplay(Map<String, String> userIdForDisplayCache, String userIdToProcess, SkillEventResult res) {
        String userIdForDisplay = userIdForDisplayCache.get(userIdToProcess)
        if (userIdForDisplay) {
            if (userIdForDisplay != notFoundToken) {
                res.userIdForDisplay = userIdForDisplay
            }
        } else {
            userIdForDisplay = userAttrsRepo.findUserNameForDisplayByUserId(userIdToProcess)
            res.userIdForDisplay = userIdForDisplay ?: userIdToProcess
            userIdForDisplayCache.put(userIdToProcess, userIdForDisplay ?: notFoundToken)
        }
    }

    @Profile
    private String getUserName(String userIdToProcess, String userSuggestOption, Map<String, List<UserInfo>> userInfoCache) {
        boolean isPki = authMode == AuthMode.PKI
        String idType = isPki ? "DN" : "ID"
        log.debug("UserId To Process=[{}], Mode=[{}], dnCheckStr=[{}]", userIdToProcess, authMode, dnCheckStr)
        if (isPki && !userIdToProcess.toLowerCase().contains(dnCheckStr.toLowerCase())) {
            List<UserInfo> userInfos = userInfoCache.get(userIdToProcess)
            if (!userInfos) {
                userInfos = pkiUserLookup.suggestUsers(userIdToProcess, userSuggestOption);
                log.debug("Returned suggest {} for provided userId [{}]", userInfos, userIdToProcess)
                userInfoCache.put(userIdToProcess, userInfos)
            }

            // need to validate that only 1 returned
            if (CollectionUtils.isEmpty(userInfos)) {
                throw new SkillException("User [" + userIdToProcess + "] was not found");
            }
            if (userInfos.size() > 1) {
                String msg = "Ambiguous user ID [" + userIdToProcess + "]. Found multiple DNs: " + userInfos.collect { "\"${it.userDn}\"" }.take(5)
                throw new SkillException(msg);

            }

            userIdToProcess = userInfos.get(0).userDn
            if (!userIdToProcess) {
                throw new SkillException("User [" + userIdToProcess + "] was found but did not have a DN set");
            }
        }

        return userInfoService.getUserName(userIdToProcess, false, idType);
    }
}
