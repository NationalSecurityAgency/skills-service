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
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDef

import java.util.stream.Stream

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
    List<SimpleSkillApproval> findToApproveByProjectIdAndNotRejected(String projectId, Pageable pageable)

    long countByProjectIdAndRejectedOnIsNull(String projectId)

    long deleteByProjectIdAndSkillRefId(String projectId, Integer skillRefId)

    Stream<SkillApproval> findAllBySkillRefIdAndRejectedOnIsNull(Integer skillRefId)

    @Nullable
    @Query('''select s 
        from SkillApproval s, SkillDef sd 
        where s.userId = ?1 and s.projectId = ?2 and s.skillRefId = sd.id and sd.skillId = ?3''')
    SkillApproval findByUserIdProjectIdAndSkillId(String userId, String projectId, String skillId)

    @Nullable
    SkillApproval findByUserIdAndProjectIdAndSkillRefId(String userId, String projectId, Integer skillRefId)

    interface SkillApprovalPlusSkillId {
        SkillApproval getSkillApproval()
        String getSkillId()
    }

    @Query('''SELECT s as skillApproval, sd.skillId as skillId
        from SkillApproval s, SkillDef subject, SkillRelDef  srd, SkillDef sd 
        where
            subject = srd.parent and 
            sd = srd.child and
            s.userId = ?1 and
            s.projectId = ?2 and
            subject.skillId = ?3 and
            srd.type = 'RuleSetDefinition' and
            s.skillRefId = sd.id''')
    List<SkillApprovalPlusSkillId> findSkillApprovalsByProjectIdAndSubjectId(String userId, String projectId, String subjectId)

    interface SkillReportingTypeAndCount {
        SkillDef.SelfReportingType getType()
        Integer getCount()
    }

    @Query('''SELECT sd.selfReportingType as type, count(sd) as count from SkillDef sd where sd.projectId = ?1 and sd.type = 'Skill' group by sd.selfReportingType''')
    List<SkillReportingTypeAndCount> skillCountsGroupedByApprovalType(String projectId)

    @Query('''SELECT count(sa) from SkillApproval sa, SkillDef sd  
            where 
                sa.skillRefId = sd.id and 
                sa.projectId = ?1 and
                sd.projectId = ?1 and
                sd.skillId = ?2 and
                sa.rejectedOn is null''')
    long countByProjectIdSkillIdAndRejectedOnIsNull(String projectId, String skillId)

    @Query('''SELECT count(sa) from SkillApproval sa, SkillDef sd  
            where 
                sa.skillRefId = sd.id and 
                sa.projectId = ?1 and
                sd.projectId = ?1 and
                sd.skillId = ?2 and
                sa.rejectedOn is not null''')
    long countByProjectIdSkillIdAndRejectedOnIsNotNull(String projectId, String skillId)
}
