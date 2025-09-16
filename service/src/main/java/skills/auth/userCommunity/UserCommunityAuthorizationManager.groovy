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
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthenticatedAuthorizationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.security.web.util.UrlUtils
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component
import skills.auth.UserAuthService
import skills.auth.UserInfo
import skills.services.AttachmentService
import skills.services.admin.UserCommunityService
import skills.storage.model.Attachment

import java.util.function.Supplier
import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
@Slf4j
@Component
@Order(99)
class UserCommunityAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final Pattern PROJECT_ID = ~/(?i)\/.*\/(?:my)?projects\/([^\/]+).*/
    private static final Pattern QUIZ_ID = ~/(?i)\/.*\/(?:quizzes|quiz-definitions)\/([^\/]+).*/
    private static final Pattern ATTACHMENT_UUID = ~/(?i)\/api\/download\/(.+)/
    private static final Pattern ADMIN_GROUP_ID = ~/(?i)\/.*\/admin-group-definitions\/([^\/]+).*/
    private static final Pattern BADGE_ID = ~/(?i)\/.*\/badge(?:s)?\/([^\/]+).*/

    private RequestMatcher projectsRequestMatcher
    private RequestMatcher quizzesAdminRequestMatcher
    private RequestMatcher quizzesApiRequestMatcher
    private RequestMatcher attachmentsRequestMatcher
    private RequestMatcher adminGroupRequestMatcher
    private RequestMatcher badgeRequestMatcher

    @Autowired
    @Lazy
    UserCommunityService userCommunityService

    @Autowired
    @Lazy
    UserAuthService userAuthService

    @Autowired
    @Lazy
    AttachmentService attachmentService

    AuthenticatedAuthorizationManager authenticatedAuthorizationManager

    @PostConstruct
    void init() {
        authenticatedAuthorizationManager = AuthenticatedAuthorizationManager.authenticated()
        projectsRequestMatcher = new AntPathRequestMatcher("/**/*projects/**")
        quizzesAdminRequestMatcher = new AntPathRequestMatcher("/**/quiz-definitions/**")
        quizzesApiRequestMatcher = new AntPathRequestMatcher("/**/quizzes/**")
        attachmentsRequestMatcher = new AntPathRequestMatcher("/api/download/**")
        adminGroupRequestMatcher = new AntPathRequestMatcher("/**/admin-group-definitions/**")
        badgeRequestMatcher = new AntPathRequestMatcher("/**/badge*/**")
    }

    @Override
    AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext authorizationContext) {
        HttpServletRequest request = authorizationContext.getRequest()
        log.debug("evaluating request [{}] for invite-only protection", request.getRequestURI())
        AuthorizationDecision authenticatedDecision = authenticatedAuthorizationManager.check(authentication, authorizationContext)
        if (!authenticatedDecision.isGranted()) {
            log.debug("unauthenticated access attempt to protected resource", request.getRequestURI())
            return authenticatedDecision
        }
        AuthorizationDecision vote = null // ACCESS_ABSTAIN
        String projectId = null
        String quizId = null
        String adminGroupId = null
        String badgeId = null
        if (projectsRequestMatcher.matches(request)) {
            projectId = extractProjectId(request)
        } else if (attachmentsRequestMatcher.matches(request)) {
            AttachmentExtractRes extractRes = extractProjectIdForAttachment(request)
            if (extractRes?.projectId) {
                projectId = extractRes.projectId
            }
            if (extractRes?.quizId) {
                quizId = extractRes.quizId
            }
            if (extractRes?.skillId) {
                badgeId = extractRes.skillId
            }
        } else if (adminGroupRequestMatcher.matches(request)) {
            adminGroupId = extractAdminGroupId(request)
        } else if (quizzesApiRequestMatcher.matches(request) || quizzesAdminRequestMatcher.matches(request)) {
            quizId = extractQuizId(request)
        }
        if (badgeRequestMatcher.matches(request)) {
            badgeId = extractBadgeId(request)
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

    private String extractProjectId(HttpServletRequest request) {
        String res = StringUtils.EMPTY
        String url = getRequestUrl(request)
        Matcher pid = PROJECT_ID.matcher(url)
        if (pid.matches()) {
            res = pid.group(1)
            if (res?.equalsIgnoreCase("null") ) {
                res = StringUtils.EMPTY
            }
        }
        return res
    }

    static class AttachmentExtractRes {
        String projectId
        String quizId
        String skillId
    }
    private AttachmentExtractRes extractProjectIdForAttachment(HttpServletRequest request) {
        String url = getRequestUrl(request)
        Matcher pid = ATTACHMENT_UUID.matcher(url)
        if (pid.matches()) {
            String uuid = pid.group(1)
            Attachment attachment = attachmentService.getAttachment(uuid);
            return new AttachmentExtractRes(projectId: attachment?.projectId, quizId: attachment?.quizId, skillId: attachment?.skillId)
        }
        return null
    }

    private String extractAdminGroupId(HttpServletRequest request) {
        String res = StringUtils.EMPTY
        String url = getRequestUrl(request)
        Matcher gid = ADMIN_GROUP_ID.matcher(url)
        if (gid.matches()) {
            res = gid.group(1)
            if (res?.equalsIgnoreCase("null") ) {
                res = StringUtils.EMPTY
            }
        }
        return res
    }

    private String extractQuizId(HttpServletRequest request) {
        String res = StringUtils.EMPTY
        String url = getRequestUrl(request)
        Matcher gid = QUIZ_ID.matcher(url)
        if (gid.matches()) {
            res = gid.group(1)
            if (res?.equalsIgnoreCase("null") ) {
                res = StringUtils.EMPTY
            }
        }
        return res
    }

    private String extractBadgeId(HttpServletRequest request) {
        String res = StringUtils.EMPTY
        String url = getRequestUrl(request)
        Matcher bid = BADGE_ID.matcher(url)
        if (bid.matches()) {
            res = bid.group(1)
            if (res?.equalsIgnoreCase("null") ) {
                res = StringUtils.EMPTY
            }
        }
        return res
    }

    private String getRequestUrl(HttpServletRequest request) {
        return UrlUtils.buildRequestUrl(request);
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
