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
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.OAuth2Request
import org.springframework.security.oauth2.provider.endpoint.TokenEndpoint
import org.springframework.security.oauth2.provider.token.DefaultTokenServices
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter
import org.springframework.web.bind.annotation.*
import skills.auth.SecurityMode
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

//    @PostConstruct
//    void afterPropertiesSet() {
//        tokenServices = new DefaultTokenServices()
//        tokenServices.setTokenStore(jwtAccessTokenConverter)
//        tokenServices.setTokenEnhancer(jwtAccessTokenConverter)
//    }

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
     * token for current user already authenticated via third party OAuth2 provider (eg, google, gitlab, etc.)
     * @param projectId
     * @return
     */
    @RequestMapping(value = "/api/projects/{projectId}/token", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @CrossOrigin(allowCredentials = 'true')
    ResponseEntity getOAuth2UserToken(@PathVariable("projectId") String projectId) {
        Object authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.credentials instanceof OAuth2AuthenticationToken && authentication.isAuthenticated()) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = authentication.credentials
            String oauthProvider = oAuth2AuthenticationToken.authorizedClientRegistrationId
            String userId = authentication.name
            log.debug("Creating self-proxy OAUth Token for [{}] or project [{}], authenticated via [{}] OAuth provider", userId, projectId, oauthProvider)
            return createSkillsProxyToken(projectId, userId)
        }
    }

    private OAuth2Authentication convertAuthentication(Authentication authentication, String clientId) {
        OAuth2Request request = new OAuth2Request(null, clientId, null, true, null, null, null, null, null)
        return new OAuth2Authentication(request, authentication)
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
