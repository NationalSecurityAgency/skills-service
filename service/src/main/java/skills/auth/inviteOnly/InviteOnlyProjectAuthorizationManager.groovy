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
package skills.auth.inviteOnly


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.authorization.AuthenticatedAuthorizationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.authorization.AuthorizationResult
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component
import skills.auth.AuthUtils
import skills.auth.UserAuthService
import skills.auth.UserInfo
import skills.auth.UserSkillsGrantedAuthority
import skills.services.admin.InviteOnlyProjectService
import skills.storage.model.auth.RoleName

import java.util.function.Supplier

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
class InviteOnlyProjectAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final List<RequestMatcher> EXCEPTIONS = [
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, '/api/projects/{projectId}/contact'),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, '/api/projects/{projectId}/newInviteRequest'),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, '/app/projects/{projectId}/join/{inviteCode}'),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, '/app/projects/{projectId}/validateInvite/{inviteCode}'),
            // MVC supports HEAD for a GET mapping as well.
            PathPatternRequestMatcher.pathPattern(HttpMethod.HEAD, '/app/projects/{projectId}/validateInvite/{inviteCode}'),
    ]

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    @Autowired
    @Lazy
    UserAuthService userAuthService

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
        AuthorizationDecision vote = null //ACCESS_ABSTAIN
        String projectId = AuthUtils.getProjectIdForProtection(request)
        if (projectId != null) {
            log.debug("evaluating request [{}] for invite-only protection", request.getRequestURI())
            Boolean isInviteOnly = inviteOnlyProjectService.isInviteOnlyProject(projectId)
            if (isInviteOnly && !isExceptionUrl(request)) {
                log.debug("project id [{}] requires invite only access", projectId)
                Collection<? extends GrantedAuthority> authorities = getAuthorities(authentication.get())
                vote = new AuthorizationDecision(false) //ACCESS_DENIED;
                // Attempt to find a matching granted authority
                for (GrantedAuthority authority : authorities) {
                    if (authority instanceof UserSkillsGrantedAuthority && isPermitted(projectId, authority)) {
                        return new AuthorizationDecision(true) //ACCESS_GRANTED;
                    }
                }
                // Authorities are scoped to the primary project for role/proxy
                // checks. Cross-project summaries must instead check the loaded
                // project's current access, without granting proxy identities roles.
                if (authentication.get().getPrincipal() instanceof UserInfo) {
                    UserInfo userInfo = (UserInfo) authentication.get().getPrincipal()
                    if (!userInfo.proxied && projectId != AuthUtils.getProjectIdFromPath(AuthUtils.getRequestPath(request)) &&
                            inviteOnlyProjectService.canUserAccess(projectId, userInfo.username)) {
                        return new AuthorizationDecision(true)
                    }
                }
                log.debug("user [{}] is not permitted to access project [{}]", authentication.get().getPrincipal(), projectId)
                throw new InviteOnlyAccessDeniedException("Access is denied", projectId)
            }
            return vote
        }
        return vote
    }

    private boolean isExceptionUrl(HttpServletRequest request) {
        for (RequestMatcher exception : EXCEPTIONS) {
            if (exception.matches(request)) {
                return true
            }
        }
        return false
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Authentication authentication) {
        Collection<? extends GrantedAuthority> grantedAuthorities = authentication.getAuthorities()
        if (!grantedAuthorities && authentication.getPrincipal() instanceof UserInfo) {
            UserInfo userInfo = (UserInfo) authentication.getPrincipal()
            grantedAuthorities = userAuthService.loadAuthorities(userInfo)
        }
        return grantedAuthorities
    }

    private static boolean isPermitted(String projectId, UserSkillsGrantedAuthority grantedAuthority) {
        if (grantedAuthority.getRole().roleName == RoleName.ROLE_SUPER_DUPER_USER) {
            return true
        }
        return projectId.equalsIgnoreCase(grantedAuthority.role.projectId) &&
                grantedAuthority.role.roleName in [RoleName.ROLE_PRIVATE_PROJECT_USER, RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER]
    }
}
