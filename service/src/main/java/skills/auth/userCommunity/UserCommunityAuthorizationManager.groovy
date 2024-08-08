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

/**
 * If a project is defined as inviteOnly, only root users, project admins, or
 * users with ROLE_PRIVATE_PROJECT_USER for the project should be allowed to access
 * /api/* methods for that project
 *
 */
@CompileStatic
@Slf4j
@Component
@Order(99)
class UserCommunityAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final Pattern PROJECT_ID = ~/(?i)\/.*\/(?:my)?projects\/([^\/]+).*/
    private static final Pattern ATTACHMENT_UUID = ~/(?i)\/api\/download\/(.+)/

    private RequestMatcher projectsRequestMatcher
    private RequestMatcher attachmentsRequestMatcher

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
        attachmentsRequestMatcher = new AntPathRequestMatcher("/api/download/**")
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
        if (projectsRequestMatcher.matches(request)) {
            projectId = extractProjectId(request)
        } else if (attachmentsRequestMatcher.matches(request)) {
            projectId = extractProjectIdForAttachment(request)
        }
        if (projectId) {
            log.debug("evaluating request [{}] for user community protection", request.getRequestURI())
            Boolean isUserCommunityOnlyProject = projectId && userCommunityService.isUserCommunityOnlyProject(projectId)
            if (isUserCommunityOnlyProject) {
                log.debug("project id [{}] requires user community only access", projectId)
                Boolean belongsToUserCommunity = userCommunityService.isUserCommunityMember(getUsername(authentication.get()))
                if (belongsToUserCommunity) {
                    return new AuthorizationDecision(true) // ACCESS_GRANTED;
                } else {
                    log.debug("user [{}] is not permitted to access project [{}]", authentication.get().getPrincipal(), projectId)
                    throw new AccessDeniedException("Access is denied")
                }
            }
            return vote
        }
        return vote
    }

    private String extractProjectId(HttpServletRequest request) {
        String url = getRequestUrl(request)
        Matcher pid = PROJECT_ID.matcher(url)
        if (pid.matches()) {
            return pid.group(1)
        }
        return StringUtils.EMPTY
    }

    private String extractProjectIdForAttachment(HttpServletRequest request) {
        String url = getRequestUrl(request)
        Matcher pid = ATTACHMENT_UUID.matcher(url)
        if (pid.matches()) {
            String uuid = pid.group(1)
            Attachment attachment = attachmentService.getAttachment(uuid);
            return attachment?.projectId
        }
        return StringUtils.EMPTY
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
