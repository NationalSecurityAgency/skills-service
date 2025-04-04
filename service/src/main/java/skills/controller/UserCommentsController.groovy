/**
 * Copyright 2025 SkillTree
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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.auth.aop.AdminOrApproverGetRequestUsersOnlyWhenUserIdSupplied
import skills.comments.UserCommentsService
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.UserCommentRequest
import skills.profile.EnableCallStackProf
import skills.skillLoading.model.UserCommentRes
import skills.skillLoading.model.UserCommentsRes

@CrossOrigin(allowCredentials = "true", originPatterns = ["*"])
@RestController
@RequestMapping("/api")
@AdminOrApproverGetRequestUsersOnlyWhenUserIdSupplied
@EnableCallStackProf
@CompileStatic
@Slf4j
class UserCommentsController {

    @Autowired
    UserCommentsService userCommentsService

    @PostMapping(value = "/projects/{projectId}/skills/{skillId}/comment")
    UserCommentRes saveSkillComment(@PathVariable("projectId") String projectId,
                                    @PathVariable("skillId") String skillId,
                                    @RequestBody UserCommentRequest userCommentRequest) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(skillId, "skillId")
        SkillsValidator.isTrue(StringUtils.isBlank(userCommentRequest?.toUserId), "Is not allowed to provide [toUserId] param in the request's body")

        UserCommentRes res = userCommentsService.saveComment(projectId, skillId, userCommentRequest)
        return res
    }

    @PostMapping(value = "/projects/{projectId}/commentThreads/{threadId}")
    UserCommentRes respondToComment(@PathVariable("projectId") String projectId,
                                    @PathVariable("threadId") Integer threadId,
                                    @RequestBody UserCommentRequest userCommentRequest) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotNull(threadId, "threadId")
        SkillsValidator.isTrue(StringUtils.isBlank(userCommentRequest?.toUserId), "Is not allowed to provide [toUserId] param in the request's body")

        UserCommentRes res = userCommentsService.respondToThread(projectId, threadId, userCommentRequest)
        return res
    }


    @ResponseBody
    @GetMapping(value = "/projects/{projectId}/skills/{skillId}/comments")
    UserCommentsRes getSkillComments(@PathVariable("projectId") String projectId,
                                     @PathVariable("skillId") String skillId) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(skillId, "skillId")
        return userCommentsService.getComments(projectId, skillId)
    }
}
