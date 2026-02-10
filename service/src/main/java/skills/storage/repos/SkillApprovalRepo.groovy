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
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.SkillRequestApprovalStats

import java.util.stream.Stream

@CompileStatic
interface SkillApprovalRepo extends CrudRepository<SkillApproval, Integer> {

    interface SimpleSkillApproval {
        Integer getApprovalId()
        String getUserId()
        String getUserIdForDisplay()
        @Nullable
        String getApproverUserId()
        @Nullable
        String getApproverUserIdForDisplay()
        String getSkillId()
        String getSubjectId()
        String getSkillName()
        Date getRequestedOn()
        @Nullable
        Date getApproverActionTakenOn()
        @Nullable
        String getRequestMsg()
        @Nullable
        Date getRejectedOn()
        @Nullable
        String getMessage()
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
        s.message as message,
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
            (?2 is null OR lower(s.userId) like lower(concat('%', ?2, '%'))) and
            s.approverUserId is null''')
    Page<SimpleSkillApproval> findToApproveByProjectIdAndNotRejectedOrApproved(String projectId, String userFilter, Pageable pageable)

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
        s.message as message,
        sd.pointIncrement as points
        from SkillApproval s, SkillDef sd, UserAttrs uAttrs, SkillDef subjectDef, SkillRelDef srd
        where 
            subjectDef = srd.parent and 
            sd = srd.child and
            (srd.type = 'RuleSetDefinition' or srd.type = 'GroupSkillToSubject') and
            subjectDef.type = 'Subject' and
            s.projectId = :projectId and 
            s.skillRefId = sd.id and 
            s.userId = uAttrs.userId and
            s.approverUserId is null and
            (:userFilter is null OR lower(s.userId) like lower(concat('%', :userFilter, '%'))) and
            (
                (exists (select 1 from SkillApprovalConf sac 
                        where sac.approverUserId = :approverId
                            and sac.projectId = :projectId
                            and (
                                (sac.userId is not null and sac.userId = s.userId) OR  
                                (sac.skillRefId is not null and sac.skillRefId = s.skillRefId)
                            )
                    )
                ) OR 
                (exists (select 1 from SkillApprovalConf sac, UserTag ut
                    where ut.userId = s.userId 
                        and lower(ut.key) = lower(sac.userTagKey)
                        and sac.projectId = :projectId
                        and sac.approverUserId = :approverId
                        and sac.userTagValue is not null
                        and lower(ut.value) like CONCAT(lower(sac.userTagValue), '%')
                    )
                )
            )''')
    Page<SimpleSkillApproval> findToApproveWithApproverConf(
            @Param("projectId") String projectId,
            @Param("approverId") String approverId,
            @Param("userFilter") String userFilter,
            Pageable pageable)

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
        s.message as message,
        sd.pointIncrement as points
        from SkillApproval s, SkillDef sd, UserAttrs uAttrs, SkillDef subjectDef, SkillRelDef srd
        where 
            subjectDef = srd.parent and 
            sd = srd.child and
            (srd.type = 'RuleSetDefinition' or srd.type = 'GroupSkillToSubject') and
            subjectDef.type = 'Subject' and
            s.projectId = :projectId and 
            s.skillRefId = sd.id and
            s.userId = uAttrs.userId and
            s.approverUserId is null and 
            (:userFilter is null OR lower(s.userId) like lower(concat('%', :userFilter, '%'))) and
            (not exists (select 1 from SkillApprovalConf sac
                            where sac.approverUserId is not null
                                and sac.projectId = :projectId 
                                and (
                                  (sac.userId is not null and sac.userId = s.userId) OR  
                                  (sac.skillRefId is not null and sac.skillRefId = s.skillRefId)
                                  )
                        )
            ) and 
            (not exists (select 1 from SkillApprovalConf sac, UserTag ut
                where sac.approverUserId is not null
                    and sac.projectId = :projectId 
                    and ut.userId = s.userId 
                    and lower(ut.key) = lower(sac.userTagKey)
                    and sac.userTagValue is not null
                    and lower(ut.value) like CONCAT(lower(sac.userTagValue), '%')
                )
            )            
        ''')
    Page<SimpleSkillApproval> findFallbackApproverConf(
            @Param("projectId") String projectId,
            @Param("userFilter") String userFilter,
            Pageable pageable)

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
        s.message as message,
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
            (s.approverUserId = :optionalApproverUserIdOrKeywordAll or 'All' = :optionalApproverUserIdOrKeywordAll) and
            lower(sd.name) like lower(CONCAT('%', :skillNameFilter, '%')) and
            lower(uAttrs.userIdForDisplay) like lower(CONCAT('%', :userIdFilter, '%')) and
            lower(approverUAttrs.userIdForDisplay) like lower(CONCAT('%', :approverUserIdFilter, '%'))
    ''')
    Page<SimpleSkillApproval> findApprovalsHistory(@Param("projectId") String projectId,
                                                   @Param("skillNameFilter") String skillNameFilter,
                                                   @Param("userIdFilter") String userIdFilter,
                                                   @Param("approverUserIdFilter") String approverUserIdFilter,
                                                   @Param("optionalApproverUserIdOrKeywordAll") String optionalApproverUserIdOrKeywordAll,
                                                   Pageable pageable)

    Long deleteByProjectIdAndSkillRefId(String projectId, Integer skillRefId)
    void deleteAllByProjectIdAndUserId(String projectId, String userId)

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
    @Query('''update SkillApproval s set s.rejectionAcknowledgedOn = CURRENT_TIMESTAMP
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

    @Nullable
    @Query('''
        SELECT
            s.id as approvalId,
            s.userId as userId,
            uAttrs.userIdForDisplay as userIdForDisplay,
            approverUAttrs.userId as approverUserId,
            approverUAttrs.userIdForDisplay as approverUserIdForDisplay,
            s.requestedOn as requestedOn,
            s.approverActionTakenOn as approverActionTakenOn,
            s.rejectedOn as rejectedOn,
            s.requestMsg as requestMsg,
            s.message as message
        FROM SkillApproval s
            JOIN UserAttrs uAttrs on uAttrs.userId = s.userId
            LEFT OUTER JOIN UserAttrs approverUAttrs on approverUAttrs.userId = s.approverUserId
        WHERE s.userId = ?1 and 
            s.projectId = ?2 and 
            s.skillRefId = ?3
        ORDER BY s.requestedOn DESC
            ''')
    List<SimpleSkillApproval> findApprovalHistoryForSkillsDisplay(String userId, String projectId, Integer skillRefId)

    interface SkillApprovalPlusSkillId {
        SkillApproval getSkillApproval()
        String getSkillId()
    }

    @Query('''SELECT s as skillApproval, sd.skillId as skillId
        from SkillApproval s, SkillDef subject, SkillRelDef  srd, SkillDef sd 
        where
            subject = srd.parent and sd = srd.child and srd.type = ?4 and
            s.userId = ?1 and
            subject.projectId = ?2 and subject.skillId = ?3 and
            (s.skillRefId = sd.id or s.skillRefId = sd.copiedFrom) and
            (
                s.approverActionTakenOn is null or 
                (s.rejectionAcknowledgedOn is null and s.rejectedOn is not null)
            )''')
    List<SkillApprovalPlusSkillId> findsApprovalWithSkillIdForSkillsDisplay(String userId, String projectId, String subjectId, SkillRelDef.RelationshipType relationshipType)

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
        @Nullable
        SkillDef.SelfReportingType getType()
        Integer getCount()
    }

    @Query('''SELECT sd.selfReportingType as type, count(sd) as count from SkillDef sd where sd.projectId = ?1 and sd.type = 'Skill' group by sd.selfReportingType''')
    List<SkillReportingTypeAndCount> skillCountsGroupedByApprovalType(String projectId)

    @Nullable
    @Query('''SELECT sum(case when sa.approverUserId is null and sa.approverActionTakenOn is null and sa.rejectedOn is null then 1 else 0 end) as pending,
                    sum(case when sa.approverUserId is not null and sa.approverActionTakenOn is not null and sa.rejectedOn is null then 1 else 0 end) as approved,
                    sum(case when sa.approverUserId is not null and sa.rejectedOn is not null then 1 else 0 end) as rejected
            from SkillApproval sa
            join SkillDef sd on sa.skillRefId = sd.id  
            where
                sd.projectId = :projectId and 
                sa.projectId = :projectId and
                sd.skillId = :skillId
            group by sa.skillRefId
                ''')
    SkillRequestApprovalStats countSkillRequestApprovals(@Param("projectId") String projectId, @Param("skillId") String skillId)
}
