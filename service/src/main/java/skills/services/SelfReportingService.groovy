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
package skills.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.storage.model.SkillApproval
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.SkillEventsSupportRepo
import groovy.util.logging.Slf4j

@Service
@Slf4j
class SelfReportingService {

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    void requestApproval(String userId, SkillEventsSupportRepo.SkillDefMin skillDefinition, Date performedOn, String requestMsg) {

        SkillApproval skillApproval = new SkillApproval(
                projectId: skillDefinition.projectId,
                userId: userId,
                skillRefId: skillDefinition.id,
                requestedOn: performedOn,
                requestMsg: requestMsg
        )

        skillApprovalRepo.save(skillApproval)
    }
}
