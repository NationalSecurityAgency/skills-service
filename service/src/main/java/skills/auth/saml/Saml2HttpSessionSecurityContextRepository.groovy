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
package skills.auth.saml

import groovy.util.logging.Slf4j
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.web.context.HttpRequestResponseHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import skills.auth.UserInfo

/**
 * Responsible for converting SAML2 authenticated principal's to UserInfo objects
 * and storing them in the SecurityContextHolder.  It also reloads the users granted_authorities
 * on each request.
 */
@Slf4j
class Saml2HttpSessionSecurityContextRepository extends HttpSessionSecurityContextRepository {

    @Autowired
    SAML2Utils saml2Utils

    @Autowired
    skills.auth.UserAuthService userAuthService

    @Override
    synchronized
    SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {

        SecurityContext context

        // Let the parent class actually get the SecurityContext from the HTTPSession first.
        Authentication auth = super.loadContext(requestResponseHolder)?.getAuthentication()
        if (auth && auth instanceof Saml2Authentication && auth.principal instanceof Saml2AuthenticatedPrincipal) {
            auth = saml2Utils.convertToSkillsAuth(auth)
        } else if (auth && auth.principal instanceof UserInfo) {
            // reload the granted_authorities for this skills user if loaded from the HTTP Session)
            UserInfo userInfo = auth.principal
            userInfo.authorities = userAuthService.loadAuthorities(userInfo.username)
            auth = new UsernamePasswordAuthenticationToken(userInfo, auth.credentials, userInfo.authorities)
        }
        context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth)

        return context
    }

    @Override
    void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
          super.saveContext(context, request, response)
    }

}
