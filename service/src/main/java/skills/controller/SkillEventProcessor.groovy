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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.services.UserAttrsService
import skills.services.events.SkillEventResult
import skills.services.events.SkillEventsService
import skills.storage.model.UserAttrs

@Component
class SkillEventProcessor {

    @Autowired
    SkillEventsService skillsManagementFacade;

    @Autowired
    UserAttrsService userAttrsService

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    SkillEventResult processSkillForUser(String skillId, String userId, String projectId, Date incomingDate, String currentUser) {
        boolean forAnotherUser = userId != null && currentUser != null && !userId.equalsIgnoreCase(currentUser);
        SkillEventsService.SkillApprovalParams skillApprovalParams = SkillEventsService.getDefaultSkillApprovalParams();
        skillApprovalParams.setForAnotherUser(forAnotherUser);
        return skillsManagementFacade.reportSkill(projectId, skillId, userId, true, incomingDate, skillApprovalParams);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    UserAttrs createNewUser(String userId) {
        return userAttrsService.saveUserAttrs(userId, new UserInfo(username: userId));
    }
}
