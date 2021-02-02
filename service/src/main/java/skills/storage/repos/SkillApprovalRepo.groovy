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
import skills.storage.model.SkillApproval

@CompileStatic
interface SkillApprovalRepo extends CrudRepository<SkillApproval, Integer> {

    interface SimpleSkillApproval {
        Integer getApprovalId()
        String getUserId()
        String getSkillId()
        String getSkillName()
        Date getRequestedOn()
        String getRequestMsg()
    }

    @Query('''SELECT
        s.id as approvalId,
        sd.skillId as skillId,
        sd.name as skillName,
        s.userId as userId,
        s.requestedOn as requestedOn,
        s.requestMsg as requestMsg
        from SkillApproval s, SkillDef sd 
        where s.projectId = ?1 and s.skillRefId = sd.id and s.rejectedOn is null''')
    List<SimpleSkillApproval> findToApproveByProjectId(String projectId)


    @Query('''select case when count(s) > 0 then true else false end  
        from SkillApproval s, SkillDef sd 
        where s.projectId = ?1 and s.skillRefId = sd.id and sd.skillId = ?2''')
    boolean existsByProjectIdAndSkillId(String projectId, String skillId)


}
