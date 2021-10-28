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
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.result.model.UserRoleRes
import skills.notify.EmailNotifier
import skills.notify.Notifier
import skills.services.events.SkillEventsService
import skills.services.events.pointsAndAchievements.InsufficientPointsValidator
import skills.services.settings.SettingsService
import skills.storage.model.Notification
import skills.storage.model.ProjDef
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDefMin
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillEventsSupportRepo
import skills.storage.repos.UserAttrsRepo

@Service
@Slf4j
class SelfReportingService {

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    CustomValidator customValidator

    @Autowired
    EmailNotifier notifier

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    SettingsService settingsService

    @Autowired
    FeatureService featureService

    @Autowired
    InsufficientPointsValidator insufficientPointsValidator

    @Autowired
    SkillDefRepo skillDefRepo

    SkillEventsService.AppliedCheckRes requestApproval(String userId, SkillDefMin skillDefinition, Date performedOn, String requestMsg) {

        if (StringUtils.isNotBlank(requestMsg)) {
            CustomValidationResult customValidationResult = customValidator.validateDescription(requestMsg)
            if (!customValidationResult.valid) {
                String msg = "Custom validation failed: msg=[${customValidationResult.msg}], type=[selfReportApprovalMsg], requestMsg=[${requestMsg}], userId=${userId}, performedOn=[${performedOn}]]"
                throw new SkillException(msg, skillDefinition.projectId, skillDefinition.skillId, ErrorCode.BadParam)
            }
        }
        validateSufficientPoints(skillDefinition, userId)

        SkillEventsService.AppliedCheckRes res
        SkillApproval existing = skillApprovalRepo.findByUserIdProjectIdAndSkillIdAndApproverActionTakenOnIsNull(userId, skillDefinition.projectId, skillDefinition.id)
        if (existing && !existing.rejectedOn) {
            res = new SkillEventsService.AppliedCheckRes(
                    skillApplied: false,
                    explanation: "This skill was already submitted for approval and is still pending approval"
            )
        }  else {
            // must acknowledge existing rejection(s) so it does not show up for users again
            skillApprovalRepo.acknowledgeAllRejectedApprovalsForUserAndProjectAndSkill(userId, skillDefinition.projectId, skillDefinition.id)

            SkillApproval skillApproval = new SkillApproval(
                    projectId: skillDefinition.projectId,
                    userId: userId,
                    skillRefId: skillDefinition.id,
                    requestedOn: performedOn,
                    requestMsg: requestMsg
            )

            skillApprovalRepo.save(skillApproval)
            sentNotifications(skillDefinition, userId, requestMsg)

            res = new SkillEventsService.AppliedCheckRes(
                    skillApplied: false,
                    explanation: "Skill was submitted for approval"
            )
        }

        return res
    }

    private void sentNotifications(SkillDefMin skillDefinition, String userId, String requestMsg) {
        String publicUrl = featureService.getPublicUrl()
        if(!publicUrl) {
            return
        }

        List<UserRoleRes> userRoleRes = accessSettingsStorageService.getUserRolesForProjectId(skillDefinition.projectId)
                .findAll { it.roleName == RoleName.ROLE_PROJECT_ADMIN }
        ProjDef projDef = projDefRepo.findByProjectId(skillDefinition.projectId)
        UserAttrs userAttrs = userAttrsRepo.findByUserId(userId)
        Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                userIds: userRoleRes.collect { it.userId },
                type: Notification.Type.SkillApprovalRequested.toString(),
                keyValParams: [
                        userRequesting: userAttrs.userIdForDisplay,
                        numPoints     : skillDefinition.pointIncrement,
                        skillName     : skillDefinition.name,
                        approveUrl    : "${publicUrl}administrator/projects/${skillDefinition.projectId}/self-report",
                        skillId       : skillDefinition.skillId,
                        requestMsg    : requestMsg,
                        projectId     : skillDefinition.projectId,
                        publicUrl     : publicUrl,
                        projectName   : projDef.name
                ],
        )
        notifier.sendNotification(request)
    }

    void removeRejectionFromView(String projectId, String userId, Integer approvalId) {

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

            approval.rejectionAcknowledgedOn = new Date()

            skillApprovalRepo.save(approval);
        } else {
            log.warn("Failed to find existing approval with id of [${approvalId}]. Could be a bug OR could be that it was removed by another admin or in a different tab:" +
                    " projectId=[${projectId}], userId=[${userId}], approvalId=[${approvalId}]")
        }
    }

    private void validateSufficientPoints(SkillDefMin skillDefinition, String userId) {
        SkillDefRepo.ProjectAndSubjectPoints projectAndSubjectPoints = skillDefRepo.getProjectAndSubjectPoints(skillDefinition.projectId, skillDefinition.skillId)
        insufficientPointsValidator.validateProjectPoints(projectAndSubjectPoints.projectTotalPoints, skillDefinition.projectId, userId)
        insufficientPointsValidator.validateSubjectPoints(projectAndSubjectPoints.subjectTotalPoints, skillDefinition.projectId, userId)
    }
}
