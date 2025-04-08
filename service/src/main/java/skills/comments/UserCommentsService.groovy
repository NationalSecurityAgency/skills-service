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
package skills.comments

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestParam
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.UserCommentRequest
import skills.controller.result.model.TableResult
import skills.skillLoading.model.UserCommentRes
import skills.skillLoading.model.UserCommentsRes
import skills.storage.model.UserComment
import skills.storage.model.UserCommentThread
import skills.storage.model.auth.RoleName
import skills.storage.repos.UserCommentRepo
import skills.storage.repos.UserCommentThreadRepo
import skills.storage.repos.UserRoleRepo
import skills.utils.InputSanitizer

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

@Service
@Slf4j
class UserCommentsService {

    @Autowired
    UserCommentRepo userCommentsRepo

    @Autowired
    UserCommentThreadRepo userCommentThreadRepo

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    UserInfoService userInfoService

    UserCommentRes saveComment(String projectId, String skillId, UserCommentRequest userCommentRequest) {
        UserInfo currentUser = userInfoService.currentUser

        UserCommentThread thread = new UserCommentThread(
                projectId: projectId,
                skillId: skillId,
                needsResponse: true
        )
        UserCommentThread savedThread = userCommentThreadRepo.saveAndFlush(thread)

        String comment = InputSanitizer.sanitize(userCommentRequest.comment)
        UserComment userComment = new UserComment(
                userCommentThreadId: savedThread.id,
                comment: comment,
                fromUserId: currentUser.username,
                toUserId: userCommentRequest.toUserId,
        )

        UserComment saved = userCommentsRepo.saveAndFlush(userComment)

        savedThread.originalCommentId = saved.id
        savedThread.lastCommentId = saved.id
        savedThread.lastCommentDate = saved.created
        userCommentThreadRepo.saveAndFlush(thread)

        return new UserCommentRes(
                threadId: savedThread.id,
                userIdForDisplay: currentUser.usernameForDisplay,
                comment: comment,
                commentedOn: saved.created
        )
    }

    UserCommentRes respondToThread(String projectId, Integer threadId, UserCommentRequest userCommentRequest) {
        UserInfo currentUser = userInfoService.currentUser

        UserCommentThread thread = userCommentThreadRepo.findById(threadId)?.get()
        if (!thread) {
            throw new SkillException("Thread with id [${threadId}] does not exist", projectId, null, ErrorCode.BadParam)
        }
        if (thread.projectId != projectId) {
            throw new SkillException("Thread with id [${threadId}] does not belong to project [${projectId}]", projectId, null, ErrorCode.BadParam)
        }
        UserComment originalComment = userCommentsRepo.findById(thread.originalCommentId)?.get()
        if (!originalComment) {
            throw new SkillException("Thread with id [${threadId}] does not have an original comment", projectId, null, ErrorCode.InternalError)
        }
        if (originalComment.fromUserId != currentUser.username) {
            boolean isTrainingAdmin =userRoleRepo.existsByUserIdAndProjectIdAndRoleNameIn(currentUser.username, projectId, [RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER])
            if (!isTrainingAdmin) {
                throw new SkillException("Only an admin or the original comment creator can respond to a thread", projectId, null, ErrorCode.AccessDenied)
            }
        }

        String comment = InputSanitizer.sanitize(userCommentRequest.comment)
        UserComment userComment = new UserComment(
                userCommentThreadId: thread.id,
                comment: comment,
                fromUserId: currentUser.username,
                toUserId: userCommentRequest.toUserId,
        )

        UserComment saved = userCommentsRepo.saveAndFlush(userComment)

        thread.lastCommentId = saved.id
        thread.lastCommentDate = saved.created
        thread.needsResponse = true
        userCommentThreadRepo.save(thread)

        return new UserCommentRes(
                threadId: thread.id,
                userIdForDisplay: currentUser.usernameForDisplay,
                comment: comment,
                commentedOn: saved.created
        )
    }

    UserCommentsRes getComments(String projectId, String skillId) {
        UserInfo currentUser = userInfoService.currentUser
        PageRequest pageRequest = PageRequest.of(0, 500, DESC, "lastCommentDate")
        Closure<Page<UserCommentThread>> getThreadsStrategy = {
            return userCommentThreadRepo.findAllByProjectIdAndSkillIdAndUserId(projectId, skillId, currentUser.username, pageRequest)
        }
        Closure<List<UserCommentRepo.SimpleUserComment>> getCommentsStrategy = {  List<Integer> threadIds ->
            return userCommentsRepo.findAllByUserIdAndThreadIdIn(currentUser.username, threadIds)
        }
        TableResult result = doGetComments(getThreadsStrategy, getCommentsStrategy)
        return new UserCommentsRes(comments: result.data)
    }

    TableResult getCommentsTableResult(String projectId, Pageable pageRequest) {
        Closure<Page<UserCommentThread>> getThreadsStrategy = {  UserInfo currentUser ->
            return userCommentThreadRepo.findAllByProjectId(projectId, pageRequest)
        }
        Closure<List<UserCommentRepo.SimpleUserComment>> getCommentsStrategy = {  List<Integer> threadIds ->
            return userCommentsRepo.findAllByThreadIdIn(threadIds)
        }
        return doGetComments(getThreadsStrategy, getCommentsStrategy)
    }

    private static TableResult doGetComments(Closure<Page<UserCommentThread>> getThreadsStrategy, Closure<List<UserCommentRepo.SimpleUserComment>> getCommentsStrategy ) {
        Page<UserCommentThread> threadsPage = getThreadsStrategy.call()
        long totalElements = threadsPage.getTotalElements()
        List<UserCommentThread> threads = threadsPage.getContent()
        List<Integer> threadIds = threads.collect { it.id }

        List<UserCommentRepo.SimpleUserComment> commentsForThreads = getCommentsStrategy.call(threadIds)
        Map<Integer, List<UserCommentRepo.SimpleUserComment>> commentsByThreadId = commentsForThreads.groupBy { it.userCommentThreadId }

        List<UserCommentRes> commentsRes = []
        for (UserCommentThread thread : threads) {
            List<UserCommentRepo.SimpleUserComment> commentsForThread = commentsByThreadId[thread.id]?.sort { it.created }
            UserCommentRepo.SimpleUserComment firstComment = commentsForThread?.first()
            UserCommentRes commentRes = new UserCommentRes(
                    threadId: thread.id,
                    userIdForDisplay: firstComment?.userIdForDisplay,
                    comment: firstComment?.comment,
                    commentedOn: firstComment?.created,
                    skillId: firstComment.skillId,
                    skillName: firstComment.skillName,
                    needsResponse: firstComment.needsResponse
            )

            if (commentsForThread.size() > 1) {
                commentsForThread[1..-1]
                        .sort { a, b -> b.created <=> a.created }
                        .each { UserCommentRepo.SimpleUserComment replyComment ->
                            commentRes.replies.add(new UserCommentRes(
                                    threadId: thread.id,
                                    userIdForDisplay: replyComment?.userIdForDisplay,
                                    comment: replyComment?.comment,
                                    commentedOn: replyComment?.created,
                                    skillId: replyComment.skillId,
                                    skillName: replyComment.skillName
                            ))
                        }
            }

            commentsRes.add(commentRes)
        }
        return new TableResult(totalCount: totalElements, data: commentsRes, count: totalElements)
    }


}
