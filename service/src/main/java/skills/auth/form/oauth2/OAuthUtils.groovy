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
import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component
import skills.auth.SecurityMode
import skills.auth.UserInfo

@Component
@Conditional(SecurityMode.FormAuth)
@Slf4j
class OAuthUtils {

    @Autowired
    OAuth2UserConverterService userConverter

    @Autowired
    skills.auth.UserAuthService userAuthService

    @Autowired
    OAuthRequestedMatcher oAuthRequestedMatcher

    Authentication convertToSkillsAuth(JwtAuthenticationToken auth) {
        // JwtAuthenticationToken is used when then the OAuth2 client uses the client_credentials grant_type we
        // look for a custom "proxy_user" field where the trusted client must specify the skills user that the
        // request is being performed on behalf of.  The proxy_user field is required for client_credentials grant_type
        Authentication skillsAuth
        Map claims = auth.token.claims
        if (claims && claims.get(AuthorizationServerConfig.SKILLS_PROXY_USER)) {
            String proxyUserId = claims.get(AuthorizationServerConfig.SKILLS_PROXY_USER)
            log.debug("Loading proxyUser [{}]", proxyUserId)
            UserInfo currentUser = new UserInfo(
                    username: proxyUserId,
                    proxied: true,
                    proxyingSystemId: auth.name,
                    firstName: null,
                    lastName: null
            )

            // if the user exists, make sure to load them so that user roles are populated
            UserInfo existingUser = userAuthService.get(currentUser)
            if (existingUser) {
                currentUser = existingUser
                currentUser.proxyingSystemId = auth.name
            }

            skillsAuth = new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.authorities)
        } else {
            OAuth2Error error = new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_TOKEN,
                    "client_credentials grant_type must specify${AuthorizationServerConfig.SKILLS_PROXY_USER} field",
                    null)
            throw new OAuth2AuthenticationException(error)
        }
        return skillsAuth
    }

    @Transactional
    Authentication convertToSkillsAuth(OAuth2AuthenticationToken auth) {
        Authentication skillsAuth = auth
        if (auth && auth instanceof OAuth2AuthenticationToken && auth.principal instanceof OAuth2User) {
            String clientId = auth.authorizedClientRegistrationId
            OAuth2User oAuth2User = auth.principal
            // convert to UserInfo using configured converter for registrationId (fail if none available)
            UserInfo currentUser = userConverter.convert(clientId, oAuth2User)

            // also create/update the UserInfo in the database.
            currentUser = userAuthService.createOrUpdateUser(currentUser, false)

            // Create new Authentication using UserInfo
            skillsAuth = new UsernamePasswordAuthenticationToken(currentUser, auth, currentUser.authorities)
        }
        return skillsAuth
    }


    @Component
    static class OAuthRequestedMatcher implements RequestMatcher {
        boolean matches(HttpServletRequest request) {
            String auth = request.getHeader("Authorization")
            // Determine if the client request contained an OAuth Authorization
            boolean haveOauth2Token = (auth != null) && auth.startsWith("Bearer")
            boolean haveAccessToken = request.getParameter("access_token") != null
            return haveOauth2Token || haveAccessToken
        }
    }
}
