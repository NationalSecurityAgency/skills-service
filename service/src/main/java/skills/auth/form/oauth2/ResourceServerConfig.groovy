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

import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import groovy.util.logging.Slf4j
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.resource.BearerTokenError
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import skills.auth.AuthUtils
import skills.auth.PortalWebSecurityHelper
import skills.auth.SecurityMode
import skills.auth.UserInfo

@Conditional(SecurityMode.FormAuth)
@Configuration
class ResourceServerConfig {

    PortalWebSecurityHelper portalWebSecurityHelper
    SecurityContextRepository securityContextRepository
    OAuthUtils oAuthUtils

    @Autowired
    ResourceServerConfig(PortalWebSecurityHelper portalWebSecurityHelper,
                         SecurityContextRepository securityContextRepository,
                         OAuthUtils oAuthUtils) {
        this.portalWebSecurityHelper = portalWebSecurityHelper
        this.securityContextRepository = securityContextRepository
        this.oAuthUtils = oAuthUtils
    }

    @Bean('resourceServerSecurityFilterChain')
    @Order(101)
    SecurityFilterChain filterChain(HttpSecurity http, SkillsJwtAuthenticationConverter skillsJwtAuthenticationConverter) throws Exception {
        portalWebSecurityHelper.configureHttpSecurity(
            http.securityContext((securityContext) ->
                securityContext.securityContextRepository(securityContextRepository)
            ).securityMatcher(oAuthUtils.oAuthRequestedMatcher)
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                            .jwtAuthenticationConverter(skillsJwtAuthenticationConverter)
                    )
                )
        )
        return http.build()
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        NimbusJwtDecoder jwtDecoder = OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>([JwtValidators.createDefault(), new JwtClaimValidator<>(AuthorizationServerConfig.SKILLS_PROXY_USER, Objects::nonNull)]))
        return jwtDecoder
    }

    @Bean
    SkillsJwtAuthenticationConverter skillsJwtAuthenticationConverter() {
        return new SkillsJwtAuthenticationConverter(oAuthUtils)
    }

    @Slf4j
    static class SkillsJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

        private OAuthUtils oAuthUtils

        private JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter()

        SkillsJwtAuthenticationConverter(OAuthUtils oAuthUtils) {
            this.oAuthUtils = oAuthUtils
        }

        @Override
        AbstractAuthenticationToken convert(Jwt jwt) {
            AbstractAuthenticationToken auth = jwtAuthenticationConverter.convert(jwt)
            if (auth.isAuthenticated()) {
                String projectId = AuthUtils.getRequestAttributes().getProjectId()
                auth = oAuthUtils.convertToSkillsAuth(auth)
                if (projectId && auth && auth.principal instanceof UserInfo) {
                    String proxyingSystemId = ((UserInfo) auth.principal).proxyingSystemId
                    if (projectId != proxyingSystemId) {
                        OAuth2Error error = new BearerTokenError(
                                OAuth2ErrorCodes.INVALID_CLIENT,
                                HttpStatus.FORBIDDEN,
                                "Invalid token - proxyingSystemId [${proxyingSystemId}] does not match resource projectId [${projectId}]",
                                null)
                        log.error("Invalid token - proxyingSystemId [{}] does not match resource projectId [{}]", proxyingSystemId, projectId)
                        throw new OAuth2AuthenticationException(error)
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
}
