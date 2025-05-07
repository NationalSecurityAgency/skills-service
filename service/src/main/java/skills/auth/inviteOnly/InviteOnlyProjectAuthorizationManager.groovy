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

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.security.authorization.AuthenticatedAuthorizationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.security.web.util.UrlUtils
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component
import skills.auth.UserAuthService
import skills.auth.UserInfo
import skills.auth.UserSkillsGrantedAuthority
import skills.services.admin.InviteOnlyProjectService
import skills.storage.model.auth.RoleName

import java.time.Duration
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
class InviteOnlyProjectAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final Pattern PROJECT_ID = ~/(?i)\/.*\/(?:my)?projects\/([^\/]+).*/

    private RequestMatcher projectsApiRequestMatcher

    private static final Pattern CONTACT_EXCEPTION = ~/(?i)api\/projects\/[^\/]+\/contact/
    private static final Pattern NEW_INVITE_EXCEPTION = ~/(?i)api\/projects\/[^\/]+\/newInviteRequest/
    private static final Pattern JOIN_EXCEPTION = ~/(?i)app\/projects\/[^\/]+\/join\/.*/
    private static final Pattern VALIDATE_EXCEPTION = ~/(?i)app\/projects\/[^\/]+\/validateInvite\/.*/

    private static final List<Pattern> EXCEPTIONS = [CONTACT_EXCEPTION, JOIN_EXCEPTION, VALIDATE_EXCEPTION, NEW_INVITE_EXCEPTION]

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    @Autowired
    @Lazy
    UserAuthService userAuthService

    AuthenticatedAuthorizationManager authenticatedAuthorizationManager

    @PostConstruct
    void init() {
        authenticatedAuthorizationManager = AuthenticatedAuthorizationManager.authenticated()
        projectsApiRequestMatcher = new AntPathRequestMatcher("/**/*projects/**")
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
        AuthorizationDecision vote = null //ACCESS_ABSTAIN
        if (projectsApiRequestMatcher.matches(request)) {
            log.debug("evaluating request [{}] for invite-only protection", request.getRequestURI())
            String projectId = extractProjectId(request)
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
                log.debug("user [{}] is not permitted to access project [{}]", authentication.get().getPrincipal(), projectId)
                throw new InviteOnlyAccessDeniedException("Access is denied", projectId)
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

    private boolean isExceptionUrl(HttpServletRequest request) {
        String url = getRequestUrl(request)
        log.debug("checking to see if url [{}] matches exception paths [{}]", url, EXCEPTIONS)
        for (Pattern exception : EXCEPTIONS) {
            if (exception.matcher(url)) {
                return true
            }
        }
        return false
    }

    private String getRequestUrl(HttpServletRequest request) {
        return UrlUtils.buildRequestUrl(request);
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Authentication authentication) {
        Collection<? extends GrantedAuthority> grantedAuthorities = authentication.getAuthorities()
        if (!grantedAuthorities && authentication.getPrincipal() instanceof UserInfo) {
            UserInfo userInfo = (UserInfo) authentication.getPrincipal()
            grantedAuthorities = load(userInfo.getUsername())
        }
        return grantedAuthorities
    }

    Collection<GrantedAuthority> load(String username) throws Exception {
        Collection<GrantedAuthority> grantedAuthorities = userAuthService.loadAuthorities(username)
        return grantedAuthorities ?: Collections.EMPTY_LIST
    }

    private static boolean isPermitted(String projectId, UserSkillsGrantedAuthority grantedAuthority) {
        if (grantedAuthority.getRole().roleName in [RoleName.ROLE_PRIVATE_PROJECT_USER, RoleName.ROLE_SUPER_DUPER_USER, RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER]) {
            return true
        }
        return false
    }
}
