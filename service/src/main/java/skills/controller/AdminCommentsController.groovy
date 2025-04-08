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

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*
import skills.comments.UserCommentsService
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.UserCommentRequest
import skills.controller.result.model.RequestResult
import skills.controller.result.model.TableResult
import skills.profile.EnableCallStackProf
import skills.skillLoading.model.UserCommentRes
import skills.utils.TablePageUtil

@RestController
@RequestMapping("/admin")
@Slf4j
@EnableCallStackProf
class AdminCommentsController {

    @Autowired
    UserCommentsService userCommentsService

    @PostMapping(value = "/projects/{projectId}/skills/{skillId}/comment")
    UserCommentRes saveSkillComment(@PathVariable("projectId") String projectId,
                                    @PathVariable("skillId") String skillId,
                                    @RequestBody UserCommentRequest userCommentRequest) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotBlank(skillId, "skillId")
        SkillsValidator.isNotBlank(userCommentRequest?.comment, "comment")
        SkillsValidator.isNotBlank(userCommentRequest?.toUserId, "toUserId")

        UserCommentRes res = userCommentsService.saveComment(projectId, skillId, userCommentRequest)
        return res
    }

    @PostMapping(value = "/projects/{projectId}/commentThreads/{threadId}")
    UserCommentRes respondToComment(@PathVariable("projectId") String projectId,
                                    @PathVariable("threadId") Integer threadId,
                                    @RequestBody UserCommentRequest userCommentRequest) {
        SkillsValidator.isNotBlank(projectId, "projectId")
        SkillsValidator.isNotNull(threadId, "threadId")
        SkillsValidator.isNotBlank(userCommentRequest?.comment, "comment")
        SkillsValidator.isNotBlank(userCommentRequest?.toUserId, "toUserId")

        UserCommentRes res = userCommentsService.respondToThread(projectId, threadId, userCommentRequest)
        return res
    }


    @ResponseBody
    @GetMapping(value = "/projects/{projectId}/comments")
    TableResult getProjectComments(@PathVariable("projectId") String projectId,
                             @RequestParam int limit,
                             @RequestParam int page,
                             @RequestParam String orderBy,
                             @RequestParam Boolean ascending) {
        PageRequest pageRequest = TablePageUtil.createPagingRequestWithValidation(projectId, limit, page, orderBy, ascending);
        return userCommentsService.getCommentsTableResult(projectId, pageRequest)
    }
}
