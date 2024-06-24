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
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.access.AccessDeniedHandlerImpl
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy
import org.springframework.security.web.firewall.HttpFirewall
import org.springframework.security.web.firewall.StrictHttpFirewall
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextListener
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import skills.auth.util.AccessDeniedExplanation
import skills.auth.util.AccessDeniedExplanationGenerator

@Component
@Configuration('securityConfig')
@ConfigurationProperties(prefix = "skills.authorization")
@Slf4j
class SecurityConfiguration {

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Autowired
    ObjectMapper objectMapper

    @Value('#{"${skills.authorization.allowUrlEncodedPercent:false}"}')
    Boolean allowUrlEncodedPercent

    @Value('#{"${skills.authorization.allowUrlEncodedForwardSlash:false}"}')
    Boolean allowUrlEncodedForwardSlash

    @Value('#{"${skills.authorization.allowUrlEncodedDoubleForwardSlash:false}"}')
    Boolean allowUrlEncodedDoubleForwardSlash

    @Value('#{"${skills.authorization.allowUrlEncodedBackSlash:false}"}')
    Boolean allowUrlEncodedBackSlash

    @Component
    @Configuration
    @Order(99)
    static class CorsSecurityConfiguration {

        @Value('#{securityConfig.authMode}}')
        AuthMode authMode = AuthMode.DEFAULT_AUTH_MODE

        @Autowired
        private PortalWebSecurityHelper portalWebSecurityHelper

        @Autowired
        private RestAuthenticationEntryPoint restAuthenticationEntryPoint

        @Autowired(required = false)  // not required for PKI_AUTH
        SecurityContextRepository securityContextRepository

        @Autowired
        AccessDeniedHandler accessDeniedHandler

        @Bean('corsSecurityFilterChain')
        @Order(102)
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/api/**").cors()
            portalWebSecurityHelper.configureHttpSecurity(http)
                    .securityContext().securityContextRepository(securityContextRepository)
            .and()
                    .exceptionHandling().accessDeniedHandler(accessDeniedHandler).authenticationEntryPoint(restAuthenticationEntryPoint)

            if (this.authMode == AuthMode.PKI) {
                http
                        .x509()
                        .subjectPrincipalRegex(/(.*)/)
                        .and()
                        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            return http.build()
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
    CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource()
        CorsConfiguration configuration = new CorsConfiguration()
        configuration.setAllowedOriginPatterns(['*'])
        configuration.setAllowCredentials(true)
        configuration.setAllowedMethods([HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.DELETE.name()])
        configuration.applyPermitDefaultValues()
        source.registerCorsConfiguration('/**', configuration)
        return source
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
        return new AccessDeniedHandlerImpl() {
            @Override
            void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                log.warn("Received AccessDeniedException for [${request.getRequestURI()}]", accessDeniedException)
                if (response.isCommitted()) {
                    logger.trace("Did not write to response since already committed");
                    return;
                }
                AccessDeniedExplanation explanation = new AccessDeniedExplanationGenerator().generateExplanation(request.getServerName(), accessDeniedException)
                if (explanation) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN)
                    String asJson = objectMapper.writeValueAsString(explanation)
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE)
                    response.setContentLength(asJson.bytes.length)
                    response.getWriter().print(asJson)
                    response.getWriter().flush()
                } else {
                    super.handle(request, response, accessDeniedException)
                }
            }
        }
    }

    @Bean
    HttpFirewall getHttpFirewall() {
        StrictHttpFirewall strictHttpFirewall = new StrictHttpFirewall()
        strictHttpFirewall.setAllowUrlEncodedPercent(allowUrlEncodedPercent)
        strictHttpFirewall.setAllowUrlEncodedSlash(allowUrlEncodedForwardSlash)
        strictHttpFirewall.setAllowUrlEncodedDoubleSlash(allowUrlEncodedDoubleForwardSlash)
        strictHttpFirewall.setAllowBackSlash(allowUrlEncodedBackSlash)
        return strictHttpFirewall
    }

    @Bean
    CookieCsrfTokenRepository cookieCsrfTokenRepository() {
        CookieCsrfTokenRepository cookieCsrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        cookieCsrfTokenRepository.setCookiePath("/");
        return cookieCsrfTokenRepository;
    }

    @Bean
    SessionAuthenticationStrategy csrfAuthenticationStrategy(CookieCsrfTokenRepository cookieCsrfTokenRepository) {
        if (this.authMode == AuthMode.PKI) {
            return new NullAuthenticatedSessionStrategy()
        } else {
            return new CsrfAuthenticationStrategy(cookieCsrfTokenRepository);
        }
    }

}
