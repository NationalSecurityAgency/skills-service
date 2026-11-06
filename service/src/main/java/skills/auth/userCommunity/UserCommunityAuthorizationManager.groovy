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
package skills.auth.userCommunity

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthenticatedAuthorizationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.authorization.AuthorizationResult
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component
import skills.auth.AuthUtils
import skills.auth.UserInfo
import skills.services.AttachmentService
import skills.services.admin.UserCommunityService
import skills.storage.model.Attachment

import java.util.function.Supplier

@CompileStatic
@Slf4j
@Component
@Order(99)
class UserCommunityAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final List<RequestMatcher> QUIZ_MATCHERS = [
            '/api/quizzes/{quizId}/**',
            '/api/quiz-definitions/{quizId}/**',
            '/admin/quiz-definitions/{quizId}/**',
            '/app/quiz-definitions/{quizId}/**',
    ].collect { String pattern -> (RequestMatcher) PathPatternRequestMatcher.pathPattern(pattern) }
    private static final List<RequestMatcher> ADMIN_GROUP_MATCHERS = [
            '/admin/admin-group-definitions/{adminGroupId}/**',
            '/app/admin-group-definitions/{adminGroupId}/**',
    ].collect { String pattern -> (RequestMatcher) PathPatternRequestMatcher.pathPattern(pattern) }
    private static final List<RequestMatcher> BADGE_MATCHERS = [
            '/admin/badges/{badgeId}/**',
            '/api/badges/{badgeId}/**',
            '/app/badges/{badgeId}/**',
            '/api/projects/{projectId}/badges/{badgeId}/**',
            '/admin/projects/{projectId}/badges/{badgeId}/**',
            '/admin/projects/{projectId}/badge/{badgeId}/**',
            '/admin/admin-group-definitions/{adminGroupId}/badges/{badgeId}/**',
    ].collect { String pattern -> (RequestMatcher) PathPatternRequestMatcher.pathPattern(pattern) }
    private static final List<RequestMatcher> ATTACHMENT_MATCHERS = [
            PathPatternRequestMatcher.pathPattern('/api/download/{uuid}'),
    ]

    @Autowired
    @Lazy
    UserCommunityService userCommunityService

    @Autowired
    @Lazy
    AttachmentService attachmentService

    AuthenticatedAuthorizationManager authenticatedAuthorizationManager

    @PostConstruct
    void init() {
        authenticatedAuthorizationManager = AuthenticatedAuthorizationManager.authenticated()
    }

    @Override
    AuthorizationResult authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext authorizationContext) {
        HttpServletRequest request = authorizationContext.getRequest()
        log.debug("evaluating request [{}] for invite-only protection", request.getRequestURI())
        AuthorizationResult authenticatedDecision = authenticatedAuthorizationManager.authorize(authentication, authorizationContext)
        if (!authenticatedDecision.isGranted()) {
            log.debug("unauthenticated access attempt to protected resource", request.getRequestURI())
            return authenticatedDecision
        }
        AuthorizationDecision vote = null // ACCESS_ABSTAIN
        String projectId = AuthUtils.getProjectIdForProtection(request)
        String quizId = AuthUtils.getPathVariable(request, 'quizId', QUIZ_MATCHERS)
        String adminGroupId = AuthUtils.getPathVariable(request, 'adminGroupId', ADMIN_GROUP_MATCHERS)
        String badgeId = AuthUtils.getPathVariable(request, 'badgeId', BADGE_MATCHERS)
        String uuid = AuthUtils.getPathVariable(request, 'uuid', ATTACHMENT_MATCHERS)
        if (uuid != null) {
            AttachmentExtractRes extractRes = extractProjectIdForAttachment(uuid)
            if (extractRes?.projectId) {
                projectId = extractRes.projectId
            }
            if (extractRes?.quizId) {
                quizId = extractRes.quizId
            }
            if (extractRes?.skillId) {
                badgeId = extractRes.skillId
            }
        }
        if (projectId || adminGroupId || quizId || badgeId) {
            log.debug("evaluating request [{}] for user community protection", request.getRequestURI())
            Boolean isUserCommunityOnlyProject = projectId && userCommunityService.isUserCommunityOnlyProject(projectId)
            Boolean isUserCommunityOnlyAdminGroup = adminGroupId && userCommunityService.isUserCommunityOnlyAdminGroup(adminGroupId)
            Boolean isUserCommunityOnlyQuiz = quizId && userCommunityService.isUserCommunityOnlyQuiz(quizId)
            Boolean isUserCommunityOnlyBadge = badgeId && userCommunityService.isUserCommunityOnlyGlobalBadge(badgeId)
            if (isUserCommunityOnlyProject || isUserCommunityOnlyAdminGroup || isUserCommunityOnlyQuiz || isUserCommunityOnlyBadge) {
                log.debug("project id [{}], admin group [{}], quiz [{}], or badge [{}] requires user community only access", projectId, adminGroupId, quizId, badgeId)
                Boolean belongsToUserCommunity = userCommunityService.isUserCommunityMember(getUsername(authentication.get()))
                if (belongsToUserCommunity) {
                    return new AuthorizationDecision(true) // ACCESS_GRANTED;
                } else {
                    log.debug("user [{}] is not permitted to access project [{}], admin group [{}], quiz [{}], or badge [{}]", authentication.get().getPrincipal(), projectId, adminGroupId, quizId, badgeId)
                    throw new AccessDeniedException("Access is denied")
                }
            }
            return vote
        }
        return vote
    }

    static class AttachmentExtractRes {
        String projectId
        String quizId
        String skillId
    }
    private AttachmentExtractRes extractProjectIdForAttachment(String uuid) {
        Attachment attachment = attachmentService.getAttachment(uuid)
        return new AttachmentExtractRes(projectId: attachment?.projectId, quizId: attachment?.quizId, skillId: attachment?.skillId)
    }

    private String getUsername(Authentication authentication) {
        String username = null
        if (authentication.getPrincipal() instanceof UserInfo) {
            UserInfo userInfo = (UserInfo) authentication.getPrincipal()
            username = userInfo.getUsername()
        }
        return username
    }

}
