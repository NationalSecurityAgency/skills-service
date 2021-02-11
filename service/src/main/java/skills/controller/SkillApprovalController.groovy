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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.SkillApprovalRejection
import skills.controller.request.model.SkillApprovalRequest
import skills.controller.result.model.BadgeResult
import skills.controller.result.model.LabelCountItem
import skills.controller.result.model.RequestResult
import skills.controller.result.model.SkillApprovalResult
import skills.controller.result.model.TableResult
import skills.services.CustomValidationResult
import skills.services.CustomValidator
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

    @RequestMapping(value = "/projects/{projectId}/approvals", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    TableResult getApprovals(@PathVariable("projectId") String projectId,
                             @RequestParam int limit,
                             @RequestParam int page,
                             @RequestParam String orderBy,
                             @RequestParam Boolean ascending) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        return skillApprovalService.getApprovals(projectId, pageRequest)
    }

    @RequestMapping(value = "/projects/{projectId}/approvals/approve", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult approve(@PathVariable("projectId") String projectId,
                            @RequestBody SkillApprovalRequest approveRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isTrue(approveRequest?.skillApprovalIds?.size() > 0, "Must supply [skillApprovalIds]", projectId)

        skillApprovalService.approve(projectId, approveRequest.skillApprovalIds)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/approvals/reject", method = [RequestMethod.POST, RequestMethod.PUT], produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestResult reject(@PathVariable("projectId") String projectId,
                            @RequestBody SkillApprovalRejection rejectRequest) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isTrue(rejectRequest?.skillApprovalIds?.size() > 0, "Must supply [skillApprovalIds]", projectId)

        if (StringUtils.isNoneBlank(rejectRequest.rejectionMessage)) {
            CustomValidationResult customValidationResult = customValidator.validateDescription(rejectRequest.rejectionMessage)
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

}
