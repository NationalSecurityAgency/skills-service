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
package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.endpoint.TokenEndpoint
import org.springframework.security.oauth2.provider.token.DefaultTokenServices
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter
import org.springframework.web.bind.annotation.*
import skills.auth.SecurityMode
import skills.auth.UserInfoService
import skills.auth.form.jwt.JwtHelper
import skills.services.InceptionProjectService

@Conditional(SecurityMode.FormAuth)
@RestController
@skills.profile.EnableCallStackProf
@Slf4j
class UserTokenController {

    @Autowired
    TokenEndpoint tokenEndpoint

    @Autowired
    InMemoryOAuth2AuthorizedClientService authorizedClientService

    @Autowired
    JwtHelper jwtHelper

    @Autowired
    private JwtAccessTokenConverter jwtAccessTokenConverter;

    @Autowired
    private DefaultTokenServices tokenServices

    @Autowired
    private UserInfoService userInfoService

    /**
     * token for inception
     * @param userId
     * @return
     */
    @RequestMapping(value = "/app/projects/Inception/users/{userId}/token", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<OAuth2AccessToken> getUserToken(@PathVariable("userId") String userId) {
        return createSkillsProxyToken(InceptionProjectService.inceptionProjectId, userId)
    }

    /**
     * token for current user already authenticated FORM auth (via third party OAuth2 provider (eg, google, gitlab, etc.),
     * or username/password)
     * @param projectId
     * @return
     */
    @RequestMapping(value = "/api/projects/{projectId}/token", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @CrossOrigin(allowCredentials = 'true')
    ResponseEntity getSelfUserToken(@PathVariable("projectId") String projectId) {
        Object authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId
        if (authentication.credentials instanceof OAuth2AuthenticationToken && authentication.isAuthenticated()) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = authentication.credentials
            String oauthProvider = oAuth2AuthenticationToken.authorizedClientRegistrationId
            userId = authentication.name
            log.debug("Creating self-proxy OAuth Token for current user [{}] and project [{}], authenticated via [{}] OAuth provider", userId, projectId, oauthProvider)
        } else {
            userId = userInfoService.currentUserId
            log.debug("Creating self-proxy OAuth Token for current user [{}] and project [{}]", userId, projectId)
        }
        return createSkillsProxyToken(projectId, userId)
    }

    /**
     * utilized by client-display within a project that previews that project's points
     * @param projectId
     * @param userId
     * @return
     */
    @RequestMapping(value = "/admin/projects/{projectId}/token/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<OAuth2AccessToken> getUserToken(@PathVariable("projectId") String projectId, @PathVariable("userId") String userId) {
        return createSkillsProxyToken(projectId, userId)
    }

    private ResponseEntity<OAuth2AccessToken> createSkillsProxyToken(String projectId, String userId) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(projectId, "Project Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(userId, "User Id")

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(projectId, null, [])
        Map<String, String> parameters = [grant_type: 'client_credentials', proxy_user: userId]
        return tokenEndpoint.postAccessToken(principal, parameters)
    }
}
