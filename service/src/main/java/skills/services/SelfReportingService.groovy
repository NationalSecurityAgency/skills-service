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

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.services.events.SkillEventsService
import skills.storage.model.SkillApproval
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.SkillEventsSupportRepo
import groovy.util.logging.Slf4j

@Service
@Slf4j
class SelfReportingService {

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    CustomValidator customValidator

    SkillEventsService.AppliedCheckRes requestApproval(String userId, SkillEventsSupportRepo.SkillDefMin skillDefinition, Date performedOn, String requestMsg) {

        if (StringUtils.isNoneBlank(requestMsg)) {
            CustomValidationResult customValidationResult = customValidator.validateDescription(requestMsg)
            if (!customValidationResult.valid) {
                String msg = "Custom validation failed: msg=[${customValidationResult.msg}], type=[selfReportApprovalMsg], requestMsg=[${requestMsg}], userId=${userId}, performedOn=[${performedOn}]]"
                throw new SkillException(msg, skillDefinition.projectId, skillDefinition.skillId, ErrorCode.BadParam)
            }
        }

        SkillEventsService.AppliedCheckRes res
        SkillApproval existing = skillApprovalRepo.findByUserIdProjectIdAndSkillId(userId, skillDefinition.projectId, skillDefinition.skillId)
        if (existing && !existing.rejectedOn) {
            res = new SkillEventsService.AppliedCheckRes(
                    skillApplied: false,
                    explanation: "This skill was already submitted for approval and is still pending approval"
            )
        } else if (existing && existing.rejectedOn) {
            // override rejection with new submission
            existing.rejectedOn = null
            existing.rejectionMsg = null
            existing.requestedOn = performedOn
            existing.requestMsg = requestMsg
            skillApprovalRepo.save(existing)

            res = new SkillEventsService.AppliedCheckRes(
                    skillApplied: false,
                    explanation: "Skill was submitted for approval"
            )
        } else {
            SkillApproval skillApproval = new SkillApproval(
                    projectId: skillDefinition.projectId,
                    userId: userId,
                    skillRefId: skillDefinition.id,
                    requestedOn: performedOn,
                    requestMsg: requestMsg
            )

            skillApprovalRepo.save(skillApproval)

            res = new SkillEventsService.AppliedCheckRes(
                    skillApplied: false,
                    explanation: "Skill was submitted for approval"
            )
        }

        return res
    }

    void removeRejection(String projectId, String userId, Integer approvalId) {

        Optional<SkillApproval> existing = skillApprovalRepo.findById(approvalId)
        if (existing.isPresent()) {
            SkillApproval approval = existing.get();
            if (approval.userId != userId) {
                throw new SkillException("SkillApproval record for id [${approvalId}] has userId that does not match provided userId. Provided userId=[${userId}]", projectId);
            }
            if (approval.projectId != projectId) {
                throw new SkillException("SkillApproval record for id [${approvalId}] has projectId that does not match provided projectId. Provided projectId=[${projectId}]", projectId);
            }
            if (!approval.rejectedOn) {
                throw new SkillException("SkillApproval with id [${approvalId}] was not rejected, user can only remove rejected SkillApproval record! projectId=[${projectId}], userId=[${userId}], approvalId=[${approvalId}]", projectId);
            }

            skillApprovalRepo.delete(approval);
        } else {
            log.warn("Failed to find existing approval with id of [${approvalId}]. Could be a bug OR could be that it was removed by another admin or in a different tab:" +
                    " projectId=[${projectId}], userId=[${userId}], approvalId=[${approvalId}]")
        }
    }
}
