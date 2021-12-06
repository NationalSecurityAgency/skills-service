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
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.result.model.LabelCountItem
import skills.controller.result.model.SkillApprovalResult
import skills.controller.result.model.TableResult
import skills.notify.EmailNotifier
import skills.notify.Notifier
import skills.services.admin.SkillCatalogService
import skills.services.events.SkillEventResult
import skills.services.events.SkillEventsService
import skills.services.settings.SettingsService
import skills.storage.model.*
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAttrsRepo

import java.util.stream.Stream

@Service
@Slf4j
class SkillApprovalService {

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    SkillEventsService skillEventsService

    @Autowired
    SkillDefRepo skillDefRepo

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

    TableResult getApprovals(String projectId, PageRequest pageRequest) {
        return buildApprovalsResult(projectId, pageRequest, {
            skillApprovalRepo.findToApproveByProjectIdAndNotRejectedOrApproved(projectId, pageRequest)
        }, {
            skillApprovalRepo.countByProjectIdAndApproverUserIdIsNull(projectId)
        })
    }

    TableResult getApprovalsHistory(String projectId, String skillNameFilter, String userIdFilter, String approverUserIdFilter, PageRequest pageRequest) {
        return buildApprovalsResult(projectId, pageRequest, {
            skillApprovalRepo.findApprovalsHistory(projectId, skillNameFilter, userIdFilter, approverUserIdFilter, pageRequest)
        }, {
            skillApprovalRepo.countApprovalsHistory(projectId, skillNameFilter, userIdFilter, approverUserIdFilter)
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
                    skillName: simpleSkillApproval.getSkillName(),
                    requestedOn: simpleSkillApproval.getRequestedOn().time,
                    approverActionTakenOn: simpleSkillApproval.getApproverActionTakenOn()?.time,
                    rejectedOn: simpleSkillApproval.getRejectedOn()?.time,
                    requestMsg: simpleSkillApproval.getRequestMsg(),
                    rejectionMsg: simpleSkillApproval.getRejectionMsg(),
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

    void approve(String projectId, List<Integer> approvalRequestIds) {
        List<SkillApproval> toApprove = skillApprovalRepo.findAllById(approvalRequestIds)
        toApprove.each {
            validateProjId(it, projectId)

            Optional<SkillDef> optional = skillDefRepo.findById(it.skillRefId)
            SkillDef skillDef = optional.get()
            // enter SkillEventResult for all copies
            SkillEventResult res = skillEventsService.reportSkill(projectId, skillDef.skillId, it.userId, false, it.requestedOn,
                    new SkillEventsService.SkillApprovalParams(disableChecks: true))

            // enter  event for all imported copies if in catalog
            if (skillCatalogService.isAvailableInCatalog(skillDef)) {
                //TODO: this may need to be moved to an async process eventually
                skillCatalogService.getRelatedSkills(skillDef)?.each { SkillDef copy ->
                    skillEventsService.reportSkill(copy.projectId, copy.skillId, it.userId, false, it.requestedOn,
                            new SkillEventsService.SkillApprovalParams(disableChecks: true))
                }
            }

            if (log.isDebugEnabled()){
                log.debug("Approval for ${it} yielded:\n${res}")
            }

            it.approverActionTakenOn = new Date()
            it.approverUserId = userInfoService.currentUser.username
            skillApprovalRepo.save(it)

            // send email
            sentNotifications(it, skillDef, true)
        }
    }

    void reject(String projectId, List<Integer> approvalRequestIds, String rejectionMsg) {
        List<SkillApproval> toReject = skillApprovalRepo.findAllById(approvalRequestIds)
        toReject.each {
            validateProjId(it, projectId)

            Date now = new Date()
            it.rejectionMsg = rejectionMsg
            it.rejectedOn = now
            it.approverActionTakenOn = now
            it.approverUserId = userInfoService.currentUser.username

            skillApprovalRepo.save(it)

            // send email
            Optional<SkillDef> optional = skillDefRepo.findById(it.skillRefId)
            SkillDef skillDef = optional.get()
            sentNotifications(it, skillDef, false, rejectionMsg)
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
        int countNotRejected = skillApprovalRepo.countByProjectIdSkillIdAndRejectedOnIsNull(projectId, skillId)
        int countRejected = skillApprovalRepo.countByProjectIdSkillIdAndRejectedOnIsNotNull(projectId, skillId)
        return [
                new LabelCountItem(value: 'SkillApprovalsRequests', count: countNotRejected),
                new LabelCountItem(value: 'SkillApprovalsRejected', count: countRejected),
        ]
    }

    void modifyApprovalsWhenSelfReportingTypeChanged(SkillDefWithExtra existing, SkillDef.SelfReportingType incomingType) {
        if (existing.selfReportingType == incomingType) {
            return;
        }
        //TODO: this probably needs to take into account catalog skills as well
        if (existing.selfReportingType == SkillDef.SelfReportingType.Approval && !incomingType) {
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

    private void sentNotifications(SkillApproval skillApproval, SkillDef skillDefinition, boolean approved, String rejectionMsg=null) {
        String publicUrl = featureService.getPublicUrl()
        if(!publicUrl) {
            return
        }

        UserAttrs userAttrs = userAttrsRepo.findByUserId(skillApproval.userId)
        if (!userAttrs.email) {
            return
        }

        ProjDef projDef = projDefRepo.findByProjectId(skillDefinition.projectId)
        Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                userIds: [skillApproval.userId],
                type: Notification.Type.SkillApprovalResponse.toString(),
                keyValParams: [
                        approver     : userInfoService.currentUser.usernameForDisplay,
                        approved     : approved,
                        skillName    : skillDefinition.name,
                        skillId      : skillDefinition.skillId,
                        projectName  : projDef.name,
                        projectId    : skillDefinition.projectId,
                        rejectionMsg : rejectionMsg,
                        publicUrl    : publicUrl,
                ],
        )
        notifier.sendNotification(request)
    }
}
