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
package skills.storage.repos

import groovy.transform.CompileStatic
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.SkillApprovalConf

@CompileStatic
interface SkillApprovalConfRepo extends CrudRepository<SkillApprovalConf, Integer> {

    @Nullable
    SkillApprovalConf findByProjectIdAndApproverUserIdAndUserId(String projectId, String approverId, String userId)

    @Nullable
    SkillApprovalConf findByProjectIdAndApproverUserIdAndUserTagKeyAndUserTagValue(String projectId, String approverId, String userTagKey, String userTagValue)

    @Nullable
    SkillApprovalConf findByProjectIdAndApproverUserIdAndSkillRefId(String projectId, String approverId, Integer userRefId)

    @Query('''select count(sac.id) > 0 from SkillApprovalConf sac where sac.projectId = ?1 and sac.approverUserId = ?2''')
    Boolean confExistForApprover(String projectId, String approverId)
}
