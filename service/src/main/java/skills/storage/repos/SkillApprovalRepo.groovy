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
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDef

import java.util.stream.Stream

@CompileStatic
interface SkillApprovalRepo extends CrudRepository<SkillApproval, Integer> {

    interface SimpleSkillApproval {
        Integer getApprovalId()
        String getUserId()
        String getUserIdForDisplay()
        String getApproverUserId()
        String getApproverUserIdForDisplay()
        String getSkillId()
        String getSubjectId()
        String getSkillName()
        Date getRequestedOn()
        Date getApproverActionTakenOn()
        String getRequestMsg()
        Date getRejectedOn()
        String getRejectionMsg()
        Integer getPoints()
    }

    @Query('''SELECT
        s.id as approvalId,
        sd.skillId as skillId,
        subjectDef.skillId as subjectId,
        sd.name as skillName,
        s.userId as userId,
        uAttrs.userIdForDisplay as userIdForDisplay,
        s.requestedOn as requestedOn,
        s.approverActionTakenOn as approverActionTakenOn,
        s.rejectedOn as rejectedOn,
        s.requestMsg as requestMsg,
        s.rejectionMsg as rejectionMsg,
        sd.pointIncrement as points
        from SkillApproval s, SkillDef sd, UserAttrs uAttrs, SkillDef subjectDef, SkillRelDef srd 
        where 
            subjectDef = srd.parent and 
            sd = srd.child and
            (srd.type = 'RuleSetDefinition' or srd.type = 'GroupSkillToSubject') and
            subjectDef.type = 'Subject' and
            s.projectId = ?1 and 
            s.skillRefId = sd.id and 
            s.userId = uAttrs.userId and
            s.approverUserId is null''')
    List<SimpleSkillApproval> findToApproveByProjectIdAndNotRejectedOrApproved(String projectId, Pageable pageable)
    long countByProjectIdAndApproverUserIdIsNull(String projectId)

    @Query('''SELECT
        s.id as approvalId,
        sd.skillId as skillId,
        subjectDef.skillId as subjectId,
        sd.name as skillName,
        s.userId as userId,
        uAttrs.userIdForDisplay as userIdForDisplay,
        approverUAttrs.userId as approverUserId,
        approverUAttrs.userIdForDisplay as approverUserIdForDisplay,
        s.requestedOn as requestedOn,
        s.approverActionTakenOn as approverActionTakenOn,
        s.rejectedOn as rejectedOn,
        s.requestMsg as requestMsg,
        s.rejectionMsg as rejectionMsg,
        sd.pointIncrement as points
        from SkillApproval s, SkillDef sd, UserAttrs uAttrs, UserAttrs approverUAttrs, SkillDef subjectDef, SkillRelDef srd 
        where 
            subjectDef = srd.parent and 
            sd = srd.child and
            (srd.type = 'RuleSetDefinition' or srd.type = 'GroupSkillToSubject') and
            subjectDef.type = 'Subject' and
            s.projectId = :projectId and 
            s.skillRefId = sd.id and 
            s.userId = uAttrs.userId and
            s.approverUserId = approverUAttrs.userId and 
            s.approverUserId is not null and
            upper(sd.name) like UPPER(CONCAT('%', :skillNameFilter, '%')) and
            upper(uAttrs.userIdForDisplay) like UPPER(CONCAT('%', :userIdFilter, '%')) and
            upper(approverUAttrs.userIdForDisplay) like UPPER(CONCAT('%', :approverUserIdFilter, '%'))
    ''')
    List<SimpleSkillApproval> findApprovalsHistory(@Param("projectId") String projectId,
                                                   @Param("skillNameFilter") String skillNameFilter,
                                                   @Param("userIdFilter") String userIdFilter,
                                                   @Param("approverUserIdFilter") String approverUserIdFilter,
                                                   Pageable pageable)
    @Query('''SELECT count(s)
        from SkillApproval s, SkillDef sd, UserAttrs uAttrs, UserAttrs approverUAttrs
        where 
            s.projectId = :projectId and 
            s.skillRefId = sd.id and 
            s.userId = uAttrs.userId and
            s.approverUserId = approverUAttrs.userId and 
            s.approverUserId is not null and
            upper(sd.name) like UPPER(CONCAT('%', :skillNameFilter, '%')) and
            upper(uAttrs.userIdForDisplay) like UPPER(CONCAT('%', :userIdFilter, '%')) and
            upper(approverUAttrs.userIdForDisplay) like UPPER(CONCAT('%', :approverUserIdFilter, '%'))
    ''')
    long countApprovalsHistory(@Param("projectId") String projectId,
                               @Param("skillNameFilter") String skillNameFilter,
                               @Param("userIdFilter") String userIdFilter,
                               @Param("approverUserIdFilter") String approverUserIdFilter)

    long deleteByProjectIdAndSkillRefId(String projectId, Integer skillRefId)

    Stream<SkillApproval> findAllBySkillRefIdAndRejectedOnIsNull(Integer skillRefId)

    @Nullable
    @Query('''select s 
        from SkillApproval s 
        where 
            s.userId = ?1 and s.projectId = ?2 and s.skillRefId = ?3 and  
            s.approverActionTakenOn is null 
            ''')
    SkillApproval findByUserIdProjectIdAndSkillIdAndApproverActionTakenOnIsNull(String userId, String projectId, Integer skillRefId)

    @Modifying
    @Query('''update SkillApproval s set s.rejectionAcknowledgedOn = CURRENT_DATE
        where 
            s.userId = ?1 and s.projectId = ?2 and s.skillRefId = ?3 and  
            s.approverActionTakenOn is not null 
            ''')
    void acknowledgeAllRejectedApprovalsForUserAndProjectAndSkill(String userId, String projectId, Integer skillRefId)

    @Nullable
    @Query('''select s 
        from SkillApproval s
        where s.userId = ?1 and s.projectId = ?2 and s.skillRefId = ?3 and 
            (
                s.approverActionTakenOn is null or 
                (s.rejectionAcknowledgedOn is null and s.rejectedOn is not null)
            )''')
    List<SkillApproval> findApprovalForSkillsDisplay(String userId, String projectId, Integer skillRefId, Pageable pageable)

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
            ( s.skillRefId = sd.id or (sd.copiedFrom > 0 and s.skillRefId = sd.copiedFrom) ) and
            (
                s.approverActionTakenOn is null or 
                (s.rejectionAcknowledgedOn is null and s.rejectedOn is not null)
            )''')
    List<SkillApprovalPlusSkillId> findsApprovalWithSkillIdForSkillsDisplay(String userId, String projectId, String subjectId)

    @Query('''SELECT s as skillApproval, sd.skillId as skillId
        from SkillApproval s, SkillDef subject, SkillRelDef  srd, SkillDef sd 
        where
            subject = srd.parent and 
            sd = srd.child and
            s.userId = ?1 and
            s.projectId = ?2 and
            subject.skillId in ?3 and
            srd.type = 'SkillsGroupRequirement' and
            s.skillRefId = sd.id and
            (
                s.approverActionTakenOn is null or 
                (s.rejectionAcknowledgedOn is null and s.rejectedOn is not null)
            )''')
    List<SkillApprovalPlusSkillId> findsApprovalWithSkillIdInForSkillsDisplay(String userId, String projectId, List<String> skillIds)

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
