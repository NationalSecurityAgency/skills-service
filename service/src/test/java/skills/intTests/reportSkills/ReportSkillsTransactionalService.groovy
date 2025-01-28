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
package skills.intTests.reportSkills

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.request.model.SkillEventRequest
import skills.services.events.SkillEventResult
import skills.services.events.SkillEventsService

@Component
@Slf4j
class ReportSkillsTransactionalService {

    @Autowired
    @Lazy
    SkillEventsService skillEventsService

    @Autowired
    UserInfoService userInfoService

    @Transactional
    SkillEventResult reportSkill(String projectId, String skillId, SkillEventRequest skillEventRequest, boolean shouldThrow) {
        log.info("reportSkill controller ${org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()}")
        SkillEventResult res = skillEventsService.reportSkill(projectId, skillId, userInfoService.getUserName(skillEventRequest.userId), false, new Date(skillEventRequest.timestamp))
        if (shouldThrow) {
            throw new RuntimeException("Throw exception so transaction would be rolled back")
        }
        res
    }
}
