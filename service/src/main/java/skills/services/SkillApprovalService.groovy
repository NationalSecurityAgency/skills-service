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

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.UIConfigProperties
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.UserSkillsGrantedAuthority
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.SkillApproverConfRequest
import skills.controller.result.model.*
import skills.notify.EmailNotifier
import skills.notify.Notifier
import skills.services.admin.SkillCatalogService
import skills.services.admin.UserCommunityService
import skills.services.events.SkillEventResult
import skills.services.events.SkillEventsService
import skills.services.settings.SettingsService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.accessors.SkillDefAccessor
import skills.storage.model.*
import skills.storage.model.auth.RoleName
import skills.storage.repos.*
import skills.utils.InputSanitizer

import java.util.stream.Stream

@Service
@Slf4j
class SkillApprovalService {

    @Value('#{"${skills.config.ui.maxTagValueLengthInApprovalWorkloadConfig:15}"}')
    int maxTagValueLengthInApprovalWorkloadConfig

    @Value('#{"${skills.config.ui.maxTagKeyLengthInApprovalWorkloadConfig:15}"}')
    int maxTagKeyLengthInApprovalWorkloadConfig

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    SkillEventsService skillEventsService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    SkillDefAccessor skillDefAccessor

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    SettingsService settingsService

    @Autowired
    UserInfoService userInfoService

    @Autowired
    EmailNotifier notifier

    @Autowired
    FeatureService featureService

    @Autowired
    SkillCatalogService skillCatalogService

    @Autowired
    SkillApprovalConfRepo skillApprovalConfRepo

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    UserCommunityService userCommunityService

    @Autowired
    UIConfigProperties uiConfigProperties

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    static class ConfExistInfo {
        boolean projConfExist = false;
        boolean fallBackApprover = false
        boolean approverHasConf = false
    }

    @Profile
    private ConfExistInfo getConfExistForApprover(String projectId, String currentApproverId) {
        Boolean confExistForProject = skillApprovalConfRepo.confExistForProject(projectId)
        if (!confExistForProject) {
            return new ConfExistInfo()
        }

        List<String> usersConfiguredForFallback = skillApprovalConfRepo.getUsersConfiguredForFallback(projectId)
        boolean hasExplicitFallbackConfigured = usersConfiguredForFallback?.find { it.equalsIgnoreCase(currentApproverId)}

        Boolean approverHasConf = hasExplicitFallbackConfigured || skillApprovalConfRepo.confExistForApprover(projectId, currentApproverId)

        new ConfExistInfo(
                projConfExist: true,
                fallBackApprover: hasExplicitFallbackConfigured || (!approverHasConf && !usersConfiguredForFallback),
                approverHasConf: approverHasConf,
        )

    }

    TableResult getApprovals(String projectId, PageRequest pageRequest) {
        String currentApproverId = userInfoService.currentUser.username
        ConfExistInfo confExistInfo = getConfExistForApprover(projectId, currentApproverId)

        return buildApprovalsResult(projectId, pageRequest, {
            if (confExistInfo.projConfExist) {
                List<SkillApprovalRepo.SimpleSkillApproval> res = []
                if (confExistInfo.fallBackApprover) {
                    res = skillApprovalRepo.findFallbackApproverConf(projectId, pageRequest)
                } else if (confExistInfo.approverHasConf) {
                    res = skillApprovalRepo.findToApproveWithApproverConf(projectId, currentApproverId, pageRequest)
                }
                return res
            }
            return skillApprovalRepo.findToApproveByProjectIdAndNotRejectedOrApproved(projectId, pageRequest)
        }, {
            if (confExistInfo.projConfExist) {
                long res = 0
                if (confExistInfo.fallBackApprover) {
                    res = skillApprovalRepo.countFallbackApproverConf(projectId)
                } else if (confExistInfo.approverHasConf) {
                    res = skillApprovalRepo.countToApproveWithApproverConf(projectId, currentApproverId)
                }
                return res
            }
            return skillApprovalRepo.countByProjectIdAndApproverUserIdIsNull(projectId)
        })
    }

    TableResult getApprovalsHistory(String projectId, String skillNameFilter, String userIdFilter, String approverUserIdFilter, PageRequest pageRequest) {
        UserInfo userInfo = userInfoService.currentUser
        boolean isApprover = userInfo.authorities?.find() {
            it instanceof UserSkillsGrantedAuthority && RoleName.ROLE_PROJECT_APPROVER == it.role?.roleName
        }
        String optionalApproverUserIdOrKeywordAll = isApprover ? userInfo.username.toLowerCase() : "All"

        return buildApprovalsResult(projectId, pageRequest, {
            skillApprovalRepo.findApprovalsHistory(projectId, skillNameFilter, userIdFilter, approverUserIdFilter, optionalApproverUserIdOrKeywordAll, pageRequest)
        }, {
            skillApprovalRepo.countApprovalsHistory(projectId, skillNameFilter, userIdFilter, approverUserIdFilter, optionalApproverUserIdOrKeywordAll)
        })
    }

    private TableResult buildApprovalsResult(String projectId, PageRequest pageRequest, Closure<List<SkillApprovalRepo.SimpleSkillApproval>> getData, Closure<Integer> getCount) {
        List<SkillApprovalRepo.SimpleSkillApproval> approvalsFromDB = getData.call()
        List<SkillApprovalResult> approvals = approvalsFromDB.collect { SkillApprovalRepo.SimpleSkillApproval simpleSkillApproval ->
            new SkillApprovalResult(
                    id: simpleSkillApproval.getApprovalId(),
                    userId: simpleSkillApproval.getUserId(),
                    userIdForDisplay: simpleSkillApproval.getUserIdForDisplay(),
                    projectId: projectId,
                    subjectId: simpleSkillApproval.getSubjectId(),
                    skillId: simpleSkillApproval.getSkillId(),
                    skillName: InputSanitizer.unsanitizeName(simpleSkillApproval.getSkillName()),
                    requestedOn: simpleSkillApproval.getRequestedOn().time,
                    approverActionTakenOn: simpleSkillApproval.getApproverActionTakenOn()?.time,
                    rejectedOn: simpleSkillApproval.getRejectedOn()?.time,
                    requestMsg: simpleSkillApproval.getRequestMsg(),
                    message: simpleSkillApproval.getMessage(),
                    points: simpleSkillApproval.getPoints(),
                    approverUserId: simpleSkillApproval.getApproverUserId(),
                    approverUserIdForDisplay: simpleSkillApproval.getApproverUserIdForDisplay(),
            )
        }

        Integer count = approvals.size()
        Integer totalCount = approvals.size()
        if (totalCount >= pageRequest.pageSize || pageRequest.pageSize > 1) {
            totalCount = getCount.call()
            // always the same since filter is never provided
            count = totalCount
        }

        TableResult tableResult = new TableResult(
                data: approvals,
                count: count,
                totalCount: totalCount
        )

        return tableResult
    }

    void approve(String projectId, List<Integer> approvalRequestIds, String approvalMsg) {
        List<SkillApproval> toApprove = skillApprovalRepo.findAllById(approvalRequestIds)
        toApprove.each {
            validateProjId(it, projectId)

            Optional<SkillDef> optional = skillDefRepo.findById(it.skillRefId)
            if (optional.isPresent()) {
                SkillDef skillDef = optional.get()
                // enter SkillEventResult for all copies
                SkillEventResult res = skillEventsService.reportSkill(projectId, skillDef.skillId, it.userId, false, it.requestedOn,
                        new SkillEventsService.SkillApprovalParams(disableChecks: true))

                if (log.isDebugEnabled()) {
                    log.debug("Approval for ${it} yielded:\n${res}")
                }

                it.message = approvalMsg
                it.approverActionTakenOn = new Date()
                it.approverUserId = userInfoService.currentUser.username
                skillApprovalRepo.save(it)

                // send email
                sendApprovalNotifications(it, skillDef, true)
            }
        }
    }

    void reject(String projectId, List<Integer> approvalRequestIds, String rejectionMsg) {
        List<SkillApproval> toReject = skillApprovalRepo.findAllById(approvalRequestIds)
        toReject.each {
            validateProjId(it, projectId)

            Date now = new Date()
            it.message = rejectionMsg
            it.rejectedOn = now
            it.approverActionTakenOn = now
            it.approverUserId = userInfoService.currentUser.username

            skillApprovalRepo.save(it)

            // send email
            Optional<SkillDef> optional = skillDefRepo.findById(it.skillRefId)
            SkillDef skillDef = optional.get()
            sendApprovalNotifications(it, skillDef, false)
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

    List<LabelCountItem> getSkillApprovalsStats(String projectId, String skillId) {
        SkillRequestApprovalStats stats = skillApprovalRepo.countSkillRequestApprovals(projectId, skillId)
        return [
                new LabelCountItem(value: 'SkillApprovalsRequests', count: stats != null ? stats.getPending() : 0),
                new LabelCountItem(value: 'SkillApprovalsRejected', count: stats != null ? stats.getRejected() : 0),
                new LabelCountItem(value: 'SkillApprovalsApproved', count: stats != null ? stats.getApproved() : 0)
        ]
    }

    @Profile
    void modifyApprovalsWhenSelfReportingTypeChanged(SkillDefWithExtra existing, SkillDef.SelfReportingType incomingType) {
        if (existing.selfReportingType == incomingType) {
            return;
        }

        if (existing.selfReportingType == SkillDef.SelfReportingType.Approval && (!incomingType || incomingType == SkillDef.SelfReportingType.Quiz || incomingType == SkillDef.SelfReportingType.Video)) {
            skillApprovalRepo.deleteByProjectIdAndSkillRefId(existing.projectId, existing.id)
        } else if (existing.selfReportingType == SkillDef.SelfReportingType.Approval && incomingType == SkillDef.SelfReportingType.HonorSystem) {
            skillApprovalRepo.findAllBySkillRefIdAndRejectedOnIsNull(existing.id).withCloseable { Stream<SkillApproval> existingApprovals ->
                existingApprovals.forEach({ SkillApproval skillApproval ->
                    SkillEventResult res = skillEventsService.reportSkill(existing.projectId, existing.skillId, skillApproval.userId, false,
                            skillApproval.requestedOn, new SkillEventsService.SkillApprovalParams(disableChecks: true))
                    if (log.isDebugEnabled()) {
                        log.debug("Approval for ${skillApproval} yielded:\n${res}")
                    }
                })
            }
            skillApprovalRepo.deleteByProjectIdAndSkillRefId(existing.projectId, existing.id)
        }
    }

    private void validateProjId(SkillApproval skillApproval, String projectId) {
        if (skillApproval.projectId != projectId) {
            throw new SkillException("Provided approval id [${skillApproval.id}] does not belong to [${projectId}]", projectId, null, ErrorCode.BadParam)
        }
    }

    private void sendApprovalNotifications(SkillApproval skillApproval, SkillDef skillDefinition, boolean approved) {
        String publicUrl = featureService.getPublicUrl()
        if(!publicUrl) {
            return
        }

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillApproval.userId)
        if (!userAttrs.email) {
            return
        }

        UserInfo currentUser = userInfoService.getCurrentUser()
        String sender = currentUser.getEmail()

        String subjectId = skillRelDefRepo.findSubjectSkillIdByChildId(skillDefinition.id)

        ProjectSummaryResult projDef = projDefRepo.getProjectName(skillDefinition.projectId)
        Boolean isUcProject = userCommunityService.isUserCommunityOnlyProject(skillDefinition.projectId)
        Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                userIds: [skillApproval.userId],
                type: Notification.Type.SkillApprovalResponse.toString(),
                keyValParams: [
                        approver     : userInfoService.currentUser.usernameForDisplay,
                        approved     : approved,
                        skillName    : skillDefinition.name,
                        skillId      : skillDefinition.skillId,
                        subjectId    : subjectId,
                        projectName  : projDef.getProjectName(),
                        projectId    : skillDefinition.projectId,
                        publicUrl    : publicUrl,
                        replyTo      : sender,
                        communityHeaderDescriptor : isUcProject ? uiConfigProperties.ui.userCommunityRestrictedDescriptor : uiConfigProperties.ui.defaultCommunityDescriptor
                ],
        )
        notifier.sendNotification(request)
    }

    @Transactional
    ApproverConfResult configureApprover(String projectId, String approverId, SkillApproverConfRequest skillApproverConfRequest) {
        validateParamConsistency(skillApproverConfRequest)
        validateApproverAccess(projectId, approverId)
        validateNotFallbackApprover(projectId, approverId)
        validatePresenceOfFallbackApprover(approverId, projectId)

        SkillApprovalConf saved
        if (skillApproverConfRequest.userId) {
            String userId = userInfoService.lookupUserId(skillApproverConfRequest.userId);
            if(!userAttrsRepo.findByUserIdIgnoreCase(userId)) {
                throw new SkillException("Provided user id [${userId}] does not exist", projectId)
            }

            SkillApprovalConf found = skillApprovalConfRepo.findByProjectIdAndApproverUserIdAndUserId(projectId, approverId, userId)
            SkillsValidator.isTrue(!found, "Already exist for projectId=[${projectId}], approverId=[${approverId}], userId=[${userId}] already exist.")
            SkillApprovalConf conf = new SkillApprovalConf(projectId: projectId, approverUserId: approverId, userId: userId)
            skillApprovalConfRepo.save(conf)
            saved = skillApprovalConfRepo.findByProjectIdAndApproverUserIdAndUserId(projectId, approverId, userId)
            assert saved
        }
        else if (skillApproverConfRequest.userTagKey) {
            SkillsValidator.isNotBlank(skillApproverConfRequest.userTagValue, "userTagValue", projectId)
            SkillsValidator.isTrue(skillApproverConfRequest.userTagKey.size() <= maxTagKeyLengthInApprovalWorkloadConfig,
                    "userTagKey must be < $maxTagKeyLengthInApprovalWorkloadConfig")
            SkillsValidator.isTrue(skillApproverConfRequest.userTagValue.size() <= maxTagValueLengthInApprovalWorkloadConfig,
                "userTagValue must be < $maxTagValueLengthInApprovalWorkloadConfig")

            String userTagKey = skillApproverConfRequest.userTagKey
            String userTagValue = skillApproverConfRequest.userTagValue
            SkillApprovalConf found = skillApprovalConfRepo.findByProjectIdAndApproverUserIdAndUserTagKeyIgnoreCaseAndUserTagValueIgnoreCase(projectId, approverId, userTagKey, userTagValue)
            SkillsValidator.isTrue(!found, "Already exist for projectId=[${projectId}], approverId=[${approverId}], userTagKey=[${userTagKey}], userTagValue=[${userTagValue}] already exist.")
            SkillApprovalConf conf = new SkillApprovalConf(projectId: projectId, approverUserId: approverId, userTagKey: userTagKey, userTagValue: userTagValue)
            skillApprovalConfRepo.save(conf)
            saved = skillApprovalConfRepo.findByProjectIdAndApproverUserIdAndUserTagKeyIgnoreCaseAndUserTagValueIgnoreCase(projectId, approverId, userTagKey, userTagValue)
            assert saved
        }
        else if (skillApproverConfRequest.skillId) {
            SkillDef skillDef = skillDefAccessor.getSkillDef(projectId, skillApproverConfRequest.skillId, [SkillDef.ContainerType.Skill])
            SkillApprovalConf found = skillApprovalConfRepo.findByProjectIdAndApproverUserIdAndSkillRefId(projectId, approverId, skillDef.id)
            SkillsValidator.isTrue(!found, "Already exist for projectId=[${projectId}], approverId=[${approverId}], skillId=[${skillApproverConfRequest.skillId}] already exist.")
            SkillApprovalConf conf = new SkillApprovalConf(projectId: projectId, approverUserId: approverId, skillRefId: skillDef.id)
            skillApprovalConfRepo.save(conf)
            saved = skillApprovalConfRepo.findByProjectIdAndApproverUserIdAndSkillRefId(projectId, approverId, skillDef.id)
            assert saved
        }

        log.info("Saved {}", saved)
        SkillApprovalConfRepo.ApproverConfResult dbRes = skillApprovalConfRepo.findConfResultById(saved.id)

        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(approverId)
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Configure,
                item: DashboardItem.Approver,
                actionAttributes: skillApproverConfRequest,
                itemId: userAttrs?.userIdForDisplay ?: approverId,
                projectId: projectId,
        ))
        return convertToClientRes(dbRes)
    }

    private void validateParamConsistency(SkillApproverConfRequest skillApproverConfRequest) {
        if (skillApproverConfRequest.userId) {
            SkillsValidator.isTrue(!skillApproverConfRequest.skillId && !skillApproverConfRequest.userTagValue, "Must provide only one of the config params -> approvalConf.userId || approvalConf.skillId || approvalConf.userTagPattern")
        }
        if (skillApproverConfRequest.skillId) {
            SkillsValidator.isTrue(!skillApproverConfRequest.userId && !skillApproverConfRequest.userTagValue, "Must provide only one of the config params -> approvalConf.userId || approvalConf.skillId || approvalConf.userTagPattern")
        }
        if (skillApproverConfRequest.userTagValue) {
            SkillsValidator.isTrue(!skillApproverConfRequest.userId && !skillApproverConfRequest.skillId, "Must provide only one of the config params -> approvalConf.userId || approvalConf.skillId || approvalConf.userTagPattern")
        }
    }

    private static Closure<SkillApprovalConfRepo.ApproverConfResult> isFallBackConf = { SkillApprovalConfRepo.ApproverConfResult conf ->
        !conf.userId && !conf.userTagValue && !conf.userTagKey && !conf.skillId
    }
    @Profile
    private void validatePresenceOfFallbackApprover(String approverId, String projectId) {
        List<ApproverConfResult> existingProjectConf = skillApprovalConfRepo.findAllByProjectId(projectId)
        PageRequest pageRequest = PageRequest.of(0, Integer.MAX_VALUE)
        List<RoleName> roles = [RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER]
        List<UserRoleRepo.UserRoleWithAttrs> userRoles = userRoleRepo.findRoleWithAttrsByProjectIdAndUserRoles(projectId, roles, pageRequest)
        List<String> allApprovers = userRoles.collect { it.role.userId }
        boolean hasFallBackApprover = existingProjectConf.find(isFallBackConf)
        if (!hasFallBackApprover) {
            // must have at 1 implicit fallback approver
            Set<String> existingApprovers = existingProjectConf.collect { it.approverUserId }.toSet()
            List<String> implicitFallbackApprovers = allApprovers.findAll({ !existingApprovers.contains(it) && it != approverId })
            if (!implicitFallbackApprovers) {
                throw new SkillException("Must have a least 1 fallback implicit or explicit approver. This operation will assign the last approver [${approverId}] away from fallback duties, which is sadly not allowed.", projectId, null, ErrorCode.BadParam)
            }
        }
    }

    @Profile
    private void validateApproverAccess(String projectId, String approverId) {
        List<UserRoleRes> requestedApproverRoles = accessSettingsStorageService.getUserRolesForProjectIdAndUserId(projectId, approverId)
        List<RoleName> validRoleNames = [RoleName.ROLE_PROJECT_APPROVER, RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_SUPER_DUPER_USER]
        boolean hasValidRole = requestedApproverRoles?.find({ validRoleNames.contains(it.roleName) })
        if (!hasValidRole) {
            throw new SkillException("Approver [${approverId}] does not have permission to approve for the project [${projectId}]", projectId, null, ErrorCode.AccessDenied)
        }
    }

    int countApprovalsForProject(String projectId) {
        return skillApprovalConfRepo.countConfForProject(projectId)
    }

    List<ApproverConfResult> getProjectApproverConf(String projectId) {
        List<SkillApprovalConfRepo.ApproverConfResult> approverConfResults = skillApprovalConfRepo.findAllByProjectId(projectId)
        List<ApproverConfResult> res = approverConfResults.collect {
            convertToClientRes(it)
        }
        return res?.sort({ it.id })
    }

    private ApproverConfResult convertToClientRes(SkillApprovalConfRepo.ApproverConfResult it) {
        new ApproverConfResult(
                id: it.getId(),
                approverUserId: it.getApproverUserId(),
                userIdForDisplay: it.getUserIdForDisplay(),
                userId: it.getUserId(),
                userTagKey: it.getUserTagKey(),
                userTagValue: it.getUserTagValue(),
                skillName: it.getSkillName(),
                skillId: it.getSkillId(),
                updated: it.getUpdated()
        )
    }

    @Transactional
    void deleteApproverConfId(String projectId, Integer approverConfId) {
        Optional<SkillApprovalConf> found = skillApprovalConfRepo.findById(approverConfId)
        if (found?.isPresent()) {
            SkillApprovalConf approvalConf = found.get()
            if (approvalConf.projectId != projectId) {
                throw new SkillException("You are not authorized to delete approval with id [${approverConfId}]", projectId, null, ErrorCode.AccessDenied)
            }
            skillApprovalConfRepo.delete(approvalConf)

            UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(approvalConf.approverUserId)
            String skillId
            if (approvalConf.skillRefId) {
                Optional<SkillDef> skillDefOptional = skillDefRepo.findById(approvalConf.skillRefId)
                if (skillDefOptional.isPresent()) {
                    skillId = skillDefOptional.get().skillId
                }
            }
            userActionsHistoryService.saveUserAction(new UserActionInfo(
                    action: DashboardAction.RemoveConfiguration,
                    item: DashboardItem.Approver,
                    itemId: userAttrs?.userIdForDisplay ?: approvalConf.approverUserId,
                    projectId: projectId,
                    actionAttributes: [
                            approverUserId: approvalConf.approverUserId,
                            userId: approvalConf.userId,
                            userTagKey: approvalConf.userTagKey,
                            userTagValue: approvalConf.userTagValue,
                            skillId: skillId,
                    ]
            ))
            log.info("Removed {}", approvalConf)
        } else {
            log.warn("Failed to find SkillApprovalConf with id [{}]", approverConfId)
        }
    }

    @Transactional
    void deleteApproverForProject(String projectId, String approverUserId) {
        long numRemoved = skillApprovalConfRepo.deleteByProjectIdAndApproverUserId(projectId, approverUserId)
        log.info("Removed [{}]approver [{}] conf for [{}]", numRemoved, approverUserId, projectId)
    }

    @Transactional
    ApproverConfResult configureFallBackApprover(String projectId, String approverId) {
        validateApproverAccess(projectId, approverId)
        validateNotFallbackApprover(projectId, approverId)

        long count = skillApprovalConfRepo.countByProjectIdAndApproverUserId(projectId, approverId)
        if (count > 0) {
            throw new SkillException("Cannot configure fallback approver since this approver already has existing workload config. Approver Id = [${approverId}]", projectId, null, ErrorCode.BadParam)
        }

        SkillApprovalConf conf = new SkillApprovalConf(projectId: projectId, approverUserId: approverId)
        skillApprovalConfRepo.save(conf)
        SkillApprovalConf saved = skillApprovalConfRepo.findByProjectIdAndApproverUserIdAndRestAttributesAreNull(projectId, approverId)
        assert saved

        log.info("Saved {}", saved)
        SkillApprovalConfRepo.ApproverConfResult dbRes = skillApprovalConfRepo.findConfResultById(saved.id)
        UserAttrs approverUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(dbRes.approverUserId)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Configure,
                item: DashboardItem.Approver,
                actionAttributes: [
                        fallbackApprover: true,
                ],
                itemId: approverUserAttrs?.userIdForDisplay ?: dbRes.approverUserId,
                projectId: projectId,
        ))
        return convertToClientRes(dbRes)
    }

    private void validateNotFallbackApprover(String projectId, String approverId) {
        SkillApprovalConf found = skillApprovalConfRepo.findByProjectIdAndApproverUserIdAndRestAttributesAreNull(projectId, approverId);
        if (found) {
            throw new SkillException(" [${approverId}] is already a fallback approver.", projectId, null, ErrorCode.BadParam)
        }
    }
}
