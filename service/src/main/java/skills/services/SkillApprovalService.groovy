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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.result.model.LabelCountItem
import skills.controller.result.model.SkillApprovalResult
import skills.controller.result.model.TableResult
import skills.services.events.SkillEventResult
import skills.services.events.SkillEventsService
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDef
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.SkillDefRepo

@Service
@Slf4j
class SkillApprovalService {

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    SkillEventsService skillEventsService

    @Autowired
    SkillDefRepo skillDefRepo

    TableResult getApprovals(String projectId, PageRequest pageRequest) {
        List<SkillApprovalRepo.SimpleSkillApproval> approvalsFromDB = skillApprovalRepo.findToApproveByProjectIdAndNotRejected(projectId, pageRequest)
         List<SkillApprovalResult> approvals = approvalsFromDB.collect { SkillApprovalRepo.SimpleSkillApproval simpleSkillApproval ->
            new SkillApprovalResult(
                    id: simpleSkillApproval.getApprovalId(),
                    userId: simpleSkillApproval.getUserId(),
                    skillId: simpleSkillApproval.getSkillId(),
                    skillName: simpleSkillApproval.getSkillName(),
                    requestedOn: simpleSkillApproval.getRequestedOn().time,
                    requestMsg: simpleSkillApproval.getRequestMsg()
            )
        }

        Integer count = approvals.size()
        Integer totalCount = approvals.size()
        if (totalCount >= pageRequest.pageSize || pageRequest.pageSize > 1) {
            totalCount = skillApprovalRepo.countByProjectIdAndRejectedOnIsNull(projectId)
            // alwasy the same since filter is never provided
            count = totalCount
        }

        TableResult tableResult = new TableResult(
                data: approvals,
                count: count,
                totalCount: totalCount
        )

        return tableResult
    }

    void approve(String projectId, List<Integer> approvalRequestIds) {
        List<SkillApproval> toApprove = skillApprovalRepo.findAllById(approvalRequestIds)
        toApprove.each {
            validateProjId(it, projectId)

            Optional<SkillDef> optional = skillDefRepo.findById(it.skillRefId)
            SkillDef skillDef = optional.get()
            SkillEventResult res = skillEventsService.reportSkill(projectId, skillDef.skillId, it.userId, false, it.requestedOn,
                    new SkillEventsService.SkillApprovalParams(disableChecks: true))

            if (log.isDebugEnabled()){
                log.debug("Approval for ${it} yielded:\n${res}")
            }

            skillApprovalRepo.delete(it)
        }
    }

    void reject(String projectId, List<Integer> approvalRequestIds, String rejectionMsg) {
        List<SkillApproval> toApprove = skillApprovalRepo.findAllById(approvalRequestIds)
        toApprove.each {
            validateProjId(it, projectId)

            it.rejectionMsg = rejectionMsg
            it.rejectedOn = new Date()

            skillApprovalRepo.save(it)
        }
    }

    List<LabelCountItem> getSelfReportStats(String projectId) {
        List<SkillApprovalRepo.SkillReportingTypeAndCount> drRes = skillApprovalRepo.skillCountsGroupedByApprovalType(projectId)
        return drRes.collect {
            new LabelCountItem(
                    value: it.getType() ?: 'Disabled',
                    count: it.getCount()
            )
        }
    }

    private void validateProjId(SkillApproval skillApproval, String projectId) {
        if (skillApproval.projectId != projectId) {
            throw new SkillException("Provided approval id [${skillApproval.id}] does not belong to [${projectId}]", projectId, null, ErrorCode.BadParam)
        }
    }

}
