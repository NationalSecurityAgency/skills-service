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
package skills.auth.form.oauth2

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager
import org.springframework.security.oauth2.provider.token.DefaultTokenServices
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import skills.auth.SecurityMode
import skills.auth.UserInfo

import javax.servlet.http.HttpServletRequest

@Component('skillsOAuth2AuthManager')
@Conditional(SecurityMode.FormAuth)
@Slf4j
class SkillsOAuth2AuthenticationManager extends OAuth2AuthenticationManager {

    @Autowired
    OAuthUtils oAuthUtils

    SkillsOAuth2AuthenticationManager(DefaultTokenServices tokenServices) {
        setTokenServices(tokenServices)
    }

    @Override
    Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication auth = super.authenticate(authentication)
        if (auth.isAuthenticated() && auth instanceof OAuth2Authentication) {
            String projectId = skills.auth.AuthUtils.getProjectIdFromRequest(servletRequest)
            auth = oAuthUtils.convertToSkillsAuth(auth)
            if (projectId && auth && auth.principal instanceof UserInfo) {
                String proxyingSystemId = auth.principal.proxyingSystemId
                if (projectId != proxyingSystemId) {
                    throw new OAuth2AccessDeniedException("Invalid token - proxyingSystemId [${proxyingSystemId}] does not match resource projectId [${projectId}]");
                }
            }
        }
        return auth
    }

    HttpServletRequest getServletRequest() {
        HttpServletRequest httpServletRequest
        try {
            ServletRequestAttributes currentRequestAttributes = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
            httpServletRequest = currentRequestAttributes.getRequest()
        } catch (Exception e) {
            log.debug("Unable to current request attributes. Error Recieved [$e]")
        }
        return httpServletRequest
    }
}
