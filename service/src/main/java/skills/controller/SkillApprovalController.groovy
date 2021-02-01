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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.SkillApprovalRejection
import skills.controller.request.model.SkillApprovalRequest
import skills.controller.result.model.BadgeResult
import skills.controller.result.model.RequestResult
import skills.controller.result.model.SkillApprovalResult
import skills.services.SkillApprovalService


@RestController
@RequestMapping("/admin")
@Slf4j
@skills.profile.EnableCallStackProf
class SkillApprovalController {

    @Autowired
    SkillApprovalService skillApprovalService

    @RequestMapping(value = "/projects/{projectId}/approvals", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<SkillApprovalResult> getApprovals(@PathVariable("projectId") String projectId) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        return skillApprovalService.getApprovals(projectId)
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

        skillApprovalService.reject(projectId, rejectRequest.skillApprovalIds, rejectRequest.rejectionMessage)
        return new RequestResult(success: true)
    }
}
