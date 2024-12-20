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
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import skills.UIConfigProperties
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.UserProjectSettingsRequest
import skills.controller.result.model.SettingsResult
import skills.controller.result.model.UserRoleRes
import skills.notify.EmailNotifier
import skills.notify.Notifier
import skills.services.admin.ProjAdminService
import skills.services.admin.SkillCatalogService
import skills.services.admin.UserCommunityService
import skills.services.events.SkillEventsService
import skills.services.events.pointsAndAchievements.InsufficientPointsValidator
import skills.services.settings.SettingsService
import skills.storage.model.Notification
import skills.storage.model.ProjDef
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefMin
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillApprovalConfRepo
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAttrsRepo

@Service
@Slf4j
class SelfReportingService {

    @Value('#{"${skills.selfReport.noEmailableAdmins.create-project-issue:true}"}')
    boolean createProjectIssue

    public static final String SETTING_GROUP = "self.report"
    public static final String SUBSCRIBED_TO_EMAILS_SETTING = 'approval.emails.subscribed'

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

    @Autowired
    UserInfoService userInfoService

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    ProjectErrorService projectErrorService;
  
    @Autowired
    SkillCatalogService catalogService

    @Autowired
    SplitWorkloadService splitWorkloadService

    @Autowired
    UserCommunityService userCommunityService

    @Autowired
    UIConfigProperties uiConfigProperties

    @Autowired
    AttachmentService attachmentService

    SkillEventsService.AppliedCheckRes requestApproval(String userId, SkillDefMin skillDefinition, Date performedOn, String requestMsg) {

        if (StringUtils.isNotBlank(requestMsg)) {
            CustomValidationResult customValidationResult = customValidator.validateDescription(requestMsg, skillDefinition.projectId)
            if (!customValidationResult.valid) {
                String msg = "Custom validation failed: msg=[${customValidationResult.msg}], type=[selfReportApprovalMsg], requestMsg=[${requestMsg}], userId=${userId}, performedOn=[${performedOn}]]"
                throw new SkillException(msg, skillDefinition.projectId, skillDefinition.skillId, ErrorCode.BadParam)
            }

            requestMsg = attachmentService.updateAttachmentsAttrsBasedOnUuidsInMarkdown(requestMsg, skillDefinition.projectId, null, skillDefinition.skillId)
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
            sentNotifications(skillDefinition, userId)

            res = new SkillEventsService.AppliedCheckRes(
                    skillApplied: false,
                    explanation: "Skill was submitted for approval"
            )
        }

        return res
    }

    public void subscribeCurrentUserToApprovalRequestEmails(String projectId) {
        setApprovalEmailSubscriptionForCurrentUser(projectId, true)
    }

    public void unsubscribeCurrentUserFromApprovalRequestEmails(String projectId) {
        setApprovalEmailSubscriptionForCurrentUser(projectId, false)
    }

    public Boolean getApprovalRequestEmailSubscriptionStatus(String projectId) {
        return getApprovalRequestEmailSubscriptionStatus(projectId, userInfoService.getCurrentUserId())
    }

    public Boolean getApprovalRequestEmailSubscriptionStatus(String projectId, String userId) {
        SettingsResult settingsResult = settingsService.getUserProjectSetting(userId, projectId, SUBSCRIBED_TO_EMAILS_SETTING, SETTING_GROUP)
        //default to true if setting doesn't exist
        boolean isSubscribed = true
        if (settingsResult) {
            isSubscribed = Boolean.valueOf(settingsResult.value)
        }
        return isSubscribed
    }

    private void setApprovalEmailSubscriptionForCurrentUser(String projectId, Boolean subscribed=true) {
        if (!projAdminService.existsByProjectId(projectId)) {
            throw new SkillException("Project with id [${projectId}] does NOT exist")
        }
        UserProjectSettingsRequest userProjectSettingsRequest = new UserProjectSettingsRequest(
                projectId: projectId,
                setting: SUBSCRIBED_TO_EMAILS_SETTING,
                settingGroup: SETTING_GROUP,
                value: subscribed.toString()
        )

        settingsService.saveSetting(userProjectSettingsRequest)
    }

    private void sentNotifications(SkillDefMin skillDefinition, String userId) {
        String publicUrl = featureService.getPublicUrl()
        if(!publicUrl) {
            return
        }

        List<UserRoleRes> userRoleRes =
                accessSettingsStorageService.getUserRolesByProjectIdAndRoles(skillDefinition.projectId, [RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER])

        String projectId = skillDefinition.projectId
        userRoleRes = splitWorkloadService.findUsersForThisRequest(userRoleRes, skillDefinition, userId)
        userRoleRes = removeAdminsWhoHaveUnsubscribed(userRoleRes, projectId)

        if (!userRoleRes) {
            if (createProjectIssue) {
                projectErrorService.noEmailableAdminsForSkillApprovalRequest(projectId)
            }
            log.debug("There are no users for project [{}] who are subscribed to skill approval request emails", projectId)
        }

        if (userRoleRes) {
            ProjDef projDef = projDefRepo.findByProjectId(skillDefinition.projectId)
            UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)
            Boolean isUcProject = userCommunityService.isUserCommunityOnlyProject(projectId)
            Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                    userIds: userRoleRes.collect { it.userId },
                    type: Notification.Type.SkillApprovalRequested.toString(),
                    keyValParams: [
                            userRequesting: userAttrs.userIdForDisplay,
                            numPoints     : skillDefinition.pointIncrement,
                            skillName     : skillDefinition.name,
                            approveUrl    : "${publicUrl}administrator/projects/${skillDefinition.projectId}/self-report",
                            skillId       : skillDefinition.skillId,
                            projectId     : skillDefinition.projectId,
                            publicUrl     : publicUrl,
                            projectName   : projDef.name,
                            replyTo       : userAttrs?.email,
                            communityHeaderDescriptor : isUcProject ? uiConfigProperties.ui.userCommunityRestrictedDescriptor : uiConfigProperties.ui.defaultCommunityDescriptor
                    ],
            )
            notifier.sendNotification(request)
        }
    }

    private List<UserRoleRes> removeAdminsWhoHaveUnsubscribed(List<UserRoleRes> userRolesList, String projectId) {
        List<UserRoleRes> emailableAdmins = userRolesList.findAll() {
            getApprovalRequestEmailSubscriptionStatus(projectId, it.userId)
        }

        return emailableAdmins
    }

    private List<UserRoleRes> considerApprovalConf(List<UserRoleRes> userRolesList, String projectId, String userId) {
        List<UserRoleRes> res = userRolesList
        List<SkillApprovalConfRepo.ApproverConfResult> approverConfResults = skillApprovalConfRepo.findAllByProjectId(projectId)
        if (approverConfResults) {
            if (approverConfResults.userId) {

            }
        }
        return res
    }

    void removeRejectionFromView(String projectId, String userId, Integer approvalId) {

        Optional<SkillApproval> existing = skillApprovalRepo.findById(approvalId)
        if (existing.isPresent()) {
            SkillApproval approval = existing.get();
            //if the skill represented by approval.skillRefId is shared to the catalog
            //get the original project id, however we also need to check to see if projectId is in the
            //list of projects that have imported that skill?

            if (approval.userId != userId) {
                throw new SkillException("SkillApproval record for id [${approvalId}] has userId that does not match provided userId. Provided userId=[${userId}]", projectId);
            }
            if (!isProjectIdValid(projectId, approval)) {
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

    private boolean isProjectIdValid(String projectId, SkillApproval approval) {
        if (projectId == approval.projectId) {
            return true;
        }
        Optional<SkillDef> skillDef = skillDefRepo.findById(approval.skillRefId)
        //don't expect this to happen  but guard against just in case
        if (!skillDef.isPresent()) {
            log.error("attempt to acknowledge approval rejection [{}] for skill that no longer exists [{}]", approval.id, approval.skillRefId)
            throw new SkillException("invalid skill")
        }
        if (catalogService.isAvailableInCatalog(skillDef.get())) {
            List<SkillDefMin> importedCopies = catalogService.getSkillsCopiedFrom(skillDef.get().id)
            if (importedCopies.find() {it.projectId == projectId}) {
                return true
            }
        }
        return false
    }

    private void validateSufficientPoints(SkillDefMin skillDefinition, String userId) {
        SkillDefRepo.ProjectAndSubjectPoints projectAndSubjectPoints = skillDefRepo.getProjectAndSubjectPoints(skillDefinition.projectId, skillDefinition.skillId)
        insufficientPointsValidator.validateProjectPoints(projectAndSubjectPoints.projectTotalPoints, skillDefinition.projectId, userId)
        insufficientPointsValidator.validateSubjectPoints(projectAndSubjectPoints.subjectTotalPoints, skillDefinition.projectId, projectAndSubjectPoints.subjectId, userId)
    }
}
