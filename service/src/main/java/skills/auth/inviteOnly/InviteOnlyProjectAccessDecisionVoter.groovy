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
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.security.access.vote.RoleVoter
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.web.FilterInvocation
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component
import skills.auth.UserAuthService
import skills.auth.UserInfo
import skills.auth.UserSkillsGrantedAuthority
import skills.storage.model.auth.RoleName

import javax.annotation.PostConstruct
import java.time.Duration
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
class InviteOnlyProjectAccessDecisionVoter extends RoleVoter {

    private static final Pattern PROJECT_ID = ~/(?i)\/api\/(?:my)?projects\/([^\/]+).*/

    private RequestMatcher projectsApiRequestMatcher

    private LoadingCache<String, Boolean> privateProjects

    @Value('#{"${skills.config.privateProject.cache-expiration-time:PT5M}"}')
    String privateProjectsCacheExpirationTime="PT5M"

    @Value('#{"${skills.config.privateProject.cache-refresh-time:PT30S}"}')
    String privateProjectCacheRefreshTime="PT90S"

    @Autowired
    PrivateProjectCacheLoader cacheLoader

    @Autowired
    @Lazy
    UserAuthService userAuthService

    @PostConstruct
    public void init() {
        projectsApiRequestMatcher = new AntPathRequestMatcher("/api/*projects/**")
        privateProjects = Caffeine.newBuilder()
                .expireAfterWrite(Duration.parse(privateProjectsCacheExpirationTime))
                .refreshAfterWrite(Duration.parse(privateProjectCacheRefreshTime))
                .build(cacheLoader)
    }

    @Override
    int vote(Authentication authentication, Object object, Collection collection) {
        if ((!object instanceof FilterInvocation)) {
            return ACCESS_ABSTAIN
        }
        FilterInvocation filterInvocation = (FilterInvocation)object

        int vote = ACCESS_ABSTAIN

        log.debug("evaluating request [{}] for invite-only protection", filterInvocation.getRequest().getRequestURI())
        if (projectsApiRequestMatcher.matches(filterInvocation.getRequest())) {
            log.debug("filterInvocation.request [{}] should be protected", filterInvocation.getRequest().getRequestURI())
            String projectId = extractProjectId(filterInvocation)
            Boolean isInviteOnly = privateProjects.get(projectId)
            if (isInviteOnly) {
                log.debug("project id [{}] requires invite only access", projectId)
                Collection<? extends GrantedAuthority> authorities = getAuthorities(authentication)
                vote = ACCESS_DENIED;
                // Attempt to find a matching granted authority
                for (GrantedAuthority authority : authorities) {
                    if (authority instanceof UserSkillsGrantedAuthority && isPermitted(projectId, authority)) {
                        return ACCESS_GRANTED;
                    }
                }
                log.debug("user [{}] is not permitted to access project [{}]", authentication.getPrincipal(), projectId)
            }
        }

        return vote
    }

    private static String extractProjectId(FilterInvocation filterInvocation) {
        String url = filterInvocation.getRequestUrl();
        Matcher pid = PROJECT_ID.matcher(url)
        if (pid.matches()) {
            return pid.group(1)
        }
        return StringUtils.EMPTY
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
        if (grantedAuthority.getRole().roleName == RoleName.ROLE_SUPER_DUPER_USER) {
            return true
        }
        if (grantedAuthority.getRole().roleName == RoleName.ROLE_PRIVATE_PROJECT_USER && grantedAuthority.getRole().projectId == projectId) {
            return true
        }
        if (grantedAuthority.getRole().roleName == RoleName.ROLE_PROJECT_ADMIN && grantedAuthority.getRole().projectId == projectId) {
            return true
        }
        return false
    }

}
