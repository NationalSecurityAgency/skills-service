/**
 * Copyright 2024 SkillTree
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
package skills.auth.limitAdminAccess

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.data.util.Pair
import org.springframework.http.HttpMethod
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.security.web.util.UrlUtils
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.stereotype.Component
import skills.auth.UserInfo
import skills.auth.UserSkillsGrantedAuthority
import skills.storage.model.auth.RoleName

import java.util.function.Supplier

@CompileStatic
@Slf4j
@Component
@Order(99)
class LimitAdminAccessAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    @Value('#{"${skills.config.ui.limitAdminAccess:false}"}')
    boolean limitAdminAccess

    private final static List<Pair<AntPathRequestMatcher, List<String>>> IGNORED_PATHS = [
            Pair.of(new AntPathRequestMatcher('/app/userInfo'), [HttpMethod.GET.toString()]),
            Pair.of(new AntPathRequestMatcher('/app/userInfo/settings'), [HttpMethod.GET.toString(), HttpMethod.PUT.toString(), HttpMethod.POST.toString()]),
            Pair.of(new AntPathRequestMatcher('/app/public/**'), [HttpMethod.GET.toString(), HttpMethod.PUT.toString(), HttpMethod.POST.toString()]),
            Pair.of(new AntPathRequestMatcher('/app/projects/*/validateInvite/*'), [HttpMethod.GET.toString()]),
            Pair.of(new AntPathRequestMatcher('/app/projects/*/join/*'), [HttpMethod.POST.toString()]),
            Pair.of(new AntPathRequestMatcher('/app/projects/*/customIcons'), [HttpMethod.GET.toString()]),
            Pair.of(new AntPathRequestMatcher('/app/projects/*/versions'), [HttpMethod.GET.toString()]),
            Pair.of(new AntPathRequestMatcher('/app/projects/*/description'), [HttpMethod.GET.toString()]),
    ]

    @Override
    AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext authorizationContext) {
        AuthorizationDecision vote = null // ACCESS_ABSTAIN

        if (!limitAdminAccess) {
            return vote
        }

        HttpServletRequest request = authorizationContext.getRequest()
        boolean foundIgnoredPath = IGNORED_PATHS.find {it.first.matches(request) && it.second.contains(request.method) }
        if (foundIgnoredPath) {
            return vote
        }

        Collection<? extends GrantedAuthority> grantedAuthorities = authentication.get().getAuthorities()
        GrantedAuthority foundAuthority = grantedAuthorities?.find({ it instanceof UserSkillsGrantedAuthority && it.getRole().roleName == RoleName.ROLE_DASHBOARD_ADMIN_ACCESS })
        if (foundAuthority) {
            return new AuthorizationDecision(true) //ACCESS_GRANTED;
        }

        UserInfo principal = (UserInfo) authentication.get().getPrincipal()
        String uid = principal?.userDn ?: principal?.username
        throw new AccessDeniedException("User [${uid}] is not permitted to access this resource [${request.method}: ${UrlUtils.buildRequestUrl(request)}]")
    }
}
