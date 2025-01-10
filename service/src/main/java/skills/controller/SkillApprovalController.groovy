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
package skills.controller

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.SkillApproverConfRequest
import skills.controller.request.model.SkillApprovalRejection
import skills.controller.request.model.SkillApprovalRequest
import skills.controller.result.model.ApproverConfResult
import skills.controller.result.model.LabelCountItem
import skills.controller.result.model.RequestResult
import skills.controller.result.model.TableResult
import skills.services.CustomValidationResult
import skills.services.CustomValidator
import skills.services.SelfReportingService
import skills.services.SkillApprovalService

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

@RestController
@RequestMapping("/admin")
@Slf4j
@skills.profile.EnableCallStackProf
class SkillApprovalController {

    @Autowired
    SkillApprovalService skillApprovalService

    @Autowired
    CustomValidator customValidator

    @Autowired
    SelfReportingService selfReportingService

    @RequestMapping(value = "/projects/{projectId}/approvals", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    TableResult getApprovals(@PathVariable("projectId") String projectId,
                             @RequestParam int limit,
                             @RequestParam int page,
                             @RequestParam String orderBy,
                             @RequestParam Boolean ascending) {
        PageRequest pageRequest = createPagingRequestWithValidation(projectId, limit, page, orderBy, ascending)
        return skillApprovalService.getApprovals(projectId, pageRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/approvals/history", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    TableResult getApprovalsHistory(@PathVariable("projectId") String projectId,
                             @RequestParam int limit,
                             @RequestParam int page,
                             @RequestParam String orderBy,
                             @RequestParam Boolean ascending,
                             @RequestParam String skillNameFilter,
                             @RequestParam String userIdFilter,
                             @RequestParam String approverUserIdFilter) {
        PageRequest pageRequest = createPagingRequestWithValidation(projectId, limit, page, orderBy, ascending)
        return skillApprovalService.getApprovalsHistory(projectId, skillNameFilter, userIdFilter, approverUserIdFilter, pageRequest)
    }

    private PageRequest createPagingRequestWithValidation(String projectId, int limit, int page, String orderBy, Boolean ascending) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isTrue(limit <= 200, "Cannot ask for more than 200 items, provided=[${limit}]", projectId)
        SkillsValidator.isTrue(page >= 0, "Cannot provide negative page. provided =[${page}]", projectId)
        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)

        return pageRequest
    }

    @RequestMapping(value = "/projects/{projectId}/approvals/approve", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult approve(@PathVariable("projectId") String projectId,
                            @RequestBody SkillApprovalRequest approveRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isTrue(approveRequest?.skillApprovalIds?.size() > 0, "Must supply [skillApprovalIds]", projectId)

        if (StringUtils.isNoneBlank(approveRequest.approvalMessage)) {
            CustomValidationResult customValidationResult = customValidator.validateDescription(approveRequest.approvalMessage, projectId)
            if (!customValidationResult.valid) {
                String msg = "Custom validation failed: msg=[${customValidationResult.msg}], type=[skillApprovalApprove], approvalMsg=[${approveRequest.approvalMessage}], skillApprovalIds=${approveRequest.skillApprovalIds}]"
                throw new SkillException(msg, projectId, null, ErrorCode.BadParam)
            }
        }

        skillApprovalService.approve(projectId, approveRequest.skillApprovalIds, approveRequest.approvalMessage)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/approvals/reject", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult reject(@PathVariable("projectId") String projectId,
                            @RequestBody SkillApprovalRejection rejectRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isTrue(rejectRequest?.skillApprovalIds?.size() > 0, "Must supply [skillApprovalIds]", projectId)

        if (StringUtils.isNoneBlank(rejectRequest.rejectionMessage)) {
            CustomValidationResult customValidationResult = customValidator.validateDescription(rejectRequest.rejectionMessage, projectId)
            if (!customValidationResult.valid) {
                String msg = "Custom validation failed: msg=[${customValidationResult.msg}], type=[skillApprovalRejection], rejectionMsg=[${rejectRequest.rejectionMessage}], skillApprovalIds=${rejectRequest.skillApprovalIds}]"
                throw new SkillException(msg, projectId, null, ErrorCode.BadParam)
            }
        }

        skillApprovalService.reject(projectId, rejectRequest.skillApprovalIds, rejectRequest.rejectionMessage)
        return new RequestResult(success: true)
    }



    @RequestMapping(value = "/projects/{projectId}/selfReport/stats", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<LabelCountItem> getSelfReportDefStats(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return skillApprovalService.getSelfReportStats(projectId)
    }


    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/approvals/stats", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<LabelCountItem> getSkillApprovalsStats(@PathVariable("projectId") String projectId, @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(skillId, "Skill Id")

        return skillApprovalService.getSkillApprovalsStats(projectId, skillId)
    }

    @RequestMapping(value = "/projects/{projectId}/approvalEmails/unsubscribe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult unsubscribeFromApprovalEmails(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        selfReportingService.unsubscribeCurrentUserFromApprovalRequestEmails(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/approvalEmails/subscribe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult subscribeToApprovalEmails(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        selfReportingService.subscribeCurrentUserToApprovalRequestEmails(projectId);
    }

    @RequestMapping(value = "/projects/{projectId}/approvalEmails/isSubscribed", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    Boolean isUserSubscribed(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        selfReportingService.getApprovalRequestEmailSubscriptionStatus(projectId);
    }

    @RequestMapping(value = "/projects/{projectId}/approverConf/{approverUserId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    ApproverConfResult configureApprover(@PathVariable("projectId") String projectId,
                                    @PathVariable("approverUserId") String approverUserId,
                                    @RequestBody SkillApproverConfRequest approverConfRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(approverUserId, "Approver User Id")
        SkillsValidator.isTrue(approverConfRequest.userId || approverConfRequest.skillId || approverConfRequest.userTagValue, "Must provide one of the config params -> approvalConf.userId || approvalConf.skillId || approvalConf.userTagPattern")
        return skillApprovalService.configureApprover(projectId, approverUserId, approverConfRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/approverConf", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<ApproverConfResult>  getProjectApproverConf(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return skillApprovalService.getProjectApproverConf(projectId);
    }

    @RequestMapping(value = "/projects/{projectId}/approverConf/count", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    int countProjectApproverConf(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return skillApprovalService.countApprovalsForProject(projectId);
    }

    @RequestMapping(value = "/projects/{projectId}/approverConf/{aproverConfId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    ApproverConfResult deleteConfig(@PathVariable("projectId") String projectId,
                                         @PathVariable("aproverConfId") Integer aproverConfId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isTrue(aproverConfId >= 0, "Approver Conf Id")
        return skillApprovalService.deleteApproverConfId(projectId, aproverConfId)
    }


    @RequestMapping(value = "/projects/{projectId}/approverConf/{approverUserId}/fallback", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    ApproverConfResult assignFallbackUser(@PathVariable("projectId") String projectId,
                                         @PathVariable("approverUserId") String approverUserId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(approverUserId, "Approver User Id")
        return skillApprovalService.configureFallBackApprover(projectId, approverUserId)
    }


}
