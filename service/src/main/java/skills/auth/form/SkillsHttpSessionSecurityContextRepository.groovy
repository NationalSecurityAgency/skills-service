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
package skills.auth.form

import groovy.util.logging.Slf4j
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.log.LogMessage
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.DeferredSecurityContext
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.context.HttpRequestResponseHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.NullSecurityContextRepository
import org.springframework.security.web.context.SupplierDeferredSecurityContext
import skills.auth.UserInfo
import skills.auth.form.oauth2.OAuthUtils

import java.util.function.Supplier

/**
 * this class is responsible for converting OAuth2 authenticated principal's to UserInfo objects
 * and storing them in the SecurityContextHolder.  It also reload the users granted_authorities
 * on each request.
 */
@Slf4j
class SkillsHttpSessionSecurityContextRepository extends HttpSessionSecurityContextRepository {

    @Autowired
    OAuthUtils oAuthUtils

    @Autowired
    skills.auth.UserAuthService userAuthService

    private NullSecurityContextRepository nullSecurityContextRepository = new NullSecurityContextRepository()

    @Override
    synchronized
    SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        SecurityContext context
        if (isProxyiedAuth(SecurityContextHolder.getContext(), requestResponseHolder.request)) {
            // do not store proxied authentication on the HttpSession
            context = nullSecurityContextRepository.loadContext(requestResponseHolder)
        } else {
            // Let the parent class actually get the SecurityContext from the HTTPSession first.
            Authentication auth = super.loadContext(requestResponseHolder)?.getAuthentication()
            if (auth) {
                auth = convertAuthentication(auth)
                context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(auth)
            }
        }
        return context
    }

    @Override
    DeferredSecurityContext loadDeferredContext(HttpServletRequest request) {
        Supplier<SecurityContext> supplier = () -> {
            synchronized (this) {
                SecurityContext context = super.loadDeferredContext(request).get()
                Authentication auth = context?.getAuthentication()
                if (auth) {
                    auth = convertAuthentication(auth)
                    context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(auth)
                }
                return context
            }
        }
        return new SupplierDeferredSecurityContext(supplier, SecurityContextHolder.getContextHolderStrategy());
    }

    private Authentication convertAuthentication(Authentication auth) {
        if (auth && auth instanceof JwtAuthenticationToken) {
            // JwtAuthenticationToken is used when then the OAuth2 client uses the client_credentials grant_type we
            // look for a custom "proxy_user" field where the trusted client must specify the skills user that the
            // request is being performed on behalf of.  The proxy_user field is required for client_credentials grant_type
            auth = oAuthUtils.convertToSkillsAuth(auth)
        } else if (auth && auth instanceof OAuth2AuthenticationToken && auth.principal instanceof OAuth2User) {
            auth = oAuthUtils.convertToSkillsAuth(auth)
        } else if (auth && auth.principal instanceof UserInfo) {
            // reload the granted_authorities for this skills user if loaded from the HTTP Session)
            UserInfo userInfo = auth.principal.clone()
            auth = new UsernamePasswordAuthenticationToken(userInfo, auth.credentials, userInfo.authorities)
        }
        return auth
    }

    @Override
    void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        if (isProxyiedAuth(context, request)) {
            // do not store proxied authentication on the HttpSession
            nullSecurityContextRepository.saveContext(context, request, response)
        } else {
            super.saveContext(context, request, response)
        }
    }

    private boolean isProxyiedAuth(SecurityContext context, HttpServletRequest request) {
        Authentication auth = context.authentication
        return (auth &&
                (auth instanceof JwtAuthenticationToken ||
                        (auth.principal &&
                                auth.principal instanceof skills.auth.UserInfo &&
                                auth.principal.isProxied()
                        )
                )
        ) || oAuthUtils.oAuthRequestedMatcher.matches(request)
    }
}
