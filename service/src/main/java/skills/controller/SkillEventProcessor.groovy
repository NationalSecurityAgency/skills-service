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
package skills.controller

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
import skills.services.UserAttrsService
import skills.services.events.SkillEventResult
import skills.services.events.SkillEventsService

@Component
class SkillEventProcessor {

    @Autowired
    SkillEventsService skillsManagementFacade;

    @Autowired
    UserAttrsService userAttrsService

    @Autowired(required = false)
    PkiUserLookup pkiUserLookup;

    @Autowired
    UserInfoService userInfoService;

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    SkillEventResult processSkillForUser(String skillId, String userId, String projectId, Date incomingDate, String currentUser) {
        String userIdToProcess = getUserName(userId);
        SkillEventsService.SkillApprovalParams skillApprovalParams = SkillEventsService.getDefaultSkillApprovalParams();
        skillApprovalParams.setForAnotherUser(true);
        return skillsManagementFacade.reportSkill(projectId, skillId, userIdToProcess, true, incomingDate, skillApprovalParams);
    }
    
    private String getUserName(String userIdToProcess) {
            String idType = "ID";
            if(authMode == AuthMode.PKI) {
                List<UserInfo> userInfos = pkiUserLookup.suggestUsers(userIdToProcess, "");

                // need to validate that only 1 returned
                if (CollectionUtils.isEmpty(userInfos)) {
                    throw new SkillException("User [" + userIdToProcess + "] was not found");
                }
                if(userInfos.size() > 1) {
                    throw new SkillException("Multiple users found for [" + userIdToProcess + "]");
                }

                userIdToProcess = userInfos.get(0).getUserDn();
                if (!userIdToProcess) {
                    throw new SkillException("User [" + userIdToProcess + "] as found but did not have a DN set");
                }
                idType = "DN";
            }

            return userInfoService.getUserName(userIdToProcess, false, idType);
    }

}
