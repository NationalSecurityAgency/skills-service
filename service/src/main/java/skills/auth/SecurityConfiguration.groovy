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
package skills.auth

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.access.AccessDeniedHandlerImpl
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextListener
import skills.auth.util.AccessDeniedExplanation
import skills.auth.util.AccessDeniedExplanationGenerator
import skills.storage.model.auth.RoleName

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Configuration('securityConfig')
@ConfigurationProperties(prefix = "skills.authorization")
@Slf4j
class SecurityConfiguration {

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Autowired
    ObjectMapper objectMapper

    @Component
    @Configuration
    @Order(99)
    static class CorsSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Value('#{securityConfig.authMode}}')
        AuthMode authMode = AuthMode.DEFAULT_AUTH_MODE

        @Autowired
        private PortalWebSecurityHelper portalWebSecurityHelper

        @Autowired
        private RestAuthenticationEntryPoint restAuthenticationEntryPoint

        @Autowired(required = false)  // not required for PKI_AUTH
        HttpSessionSecurityContextRepository securityContextRepository

        @Autowired
        PasswordEncoder passwordEncoder

        @Autowired
        UserDetailsService userDetailsService

        AccessDeniedExplanationGenerator explanationGenerator = new AccessDeniedExplanationGenerator()

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/api/**").cors()
            portalWebSecurityHelper.configureHttpSecurity(http)
                    .securityContext().securityContextRepository(securityContextRepository)
            .and()
                    .exceptionHandling().authenticationEntryPoint(restAuthenticationEntryPoint)

            if (this.authMode == AuthMode.PKI) {
                http
                        .x509()
                        .subjectPrincipalRegex(/(.*)/)
                        .and()
                        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
        }
    }

    @Component
    static final class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
        @Override
        void commence(
                final HttpServletRequest request,
                final HttpServletResponse response,
                final AuthenticationException authException) throws IOException {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
        }
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder()
    }

    @Bean
    RequestContextListener requestContextListener() {
        return new RequestContextListener()
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {
        return new CustomHandler()
    }

    static class CustomHandler extends AccessDeniedHandlerImpl {
        @Override
        void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
            log.warn("Received AccessDeniedException for [${request.getRequestURI()}]", accessDeniedException)
            super.handle(request, response, accessDeniedException)
            AccessDeniedExplanation explanation = new AccessDeniedExplanationGenerator().generateExplanation(request.getServerName())
            response.setStatus(HttpServletResponse.SC_FORBIDDEN)
            if(explanation) {
                String asJson = objectMapper.writeValueAsString(explanation)
                response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                response.setContentLength(asJson.bytes.length)
                response.getWriter().print(asJson)
                response.getWriter().flush()
            }
        }
    }
}
