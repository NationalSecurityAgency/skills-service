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
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.*
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler
import org.springframework.security.web.context.DelegatingSecurityContextRepository
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Component
import skills.auth.PortalWebSecurityHelper
import skills.auth.SecurityConfiguration
import skills.auth.SecurityMode
import skills.auth.UserAuthService
import skills.auth.form.oauth2.OAuth2UserConverterService
import skills.auth.form.oauth2.OAuthUtils

@Conditional(SecurityMode.FormAuth)
@Component
@Configuration
@Slf4j
class FormSecurityConfiguration {

    public static final String SKILLS_REDIRECT_URI = 'skillsRedirectUri'

    @Autowired
    private PortalWebSecurityHelper portalWebSecurityHelper

    @Autowired
    private RestAccessDeniedHandler restAccessDeniedHandler

    @Autowired
    private SecurityConfiguration.RestAuthenticationEntryPoint restAuthenticationEntryPoint

    @Autowired
    private AuthenticationFailureHandler restAuthenticationFailureHandler

    @Autowired
    private RestAuthenticationSuccessHandler restAuthenticationSuccessHandler

    @Autowired
    private SkillsClientOAuth2AuthenticationSuccessHandler oauthAuthenticationSuccessHandler

    @Autowired
    private SkillsClientOAuth2AuthorizationRequestRepository skillsClientOAuth2AuthorizationRequestRepository

    @Autowired
    private RestLogoutSuccessHandler restLogoutSuccessHandler

    @Autowired
    PasswordEncoder passwordEncoder

    @Autowired
    void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(localUserDetailsService())
                .passwordEncoder(passwordEncoder)
    }

    @Autowired
    @Lazy
    SecurityContextRepository securityContextRepository

    @Bean('formSecurityFilterChain')
    @Order(103)
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring FORM authorization mode")

        // Portal endpoints config
        portalWebSecurityHelper.configureHttpSecurity(http)
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
        .and()
                .securityContext().securityContextRepository(securityContextRepository)
        .and()
                .exceptionHandling()
                .accessDeniedHandler(restAccessDeniedHandler)
                .authenticationEntryPoint(restAuthenticationEntryPoint)
        .and()
                .formLogin()
                    .loginPage("/skills-login")
                    .loginProcessingUrl("/performLogin")
                    .successHandler(restAuthenticationSuccessHandler)
                    .failureHandler(restAuthenticationFailureHandler)
        .and()
                .logout()
                    .logoutSuccessHandler(restLogoutSuccessHandler)
        .and()
                .oauth2Login()
                    .loginPage("/skills-login")
                    .successHandler(oauthAuthenticationSuccessHandler)
                    .failureHandler(restAuthenticationFailureHandler)
                    .authorizationEndpoint()
                        .authorizationRequestRepository(skillsClientOAuth2AuthorizationRequestRepository)

        http.build()
    }

    @Bean(name = 'defaultAuthManager')
    @Primary
    @Lazy
    AuthenticationManager authenticationManagerBean(HttpSecurity http) throws Exception {
        // provides the default AuthenticationManager as a Bean
        return http.getSharedObject(AuthenticationManager.class);
    }

    @Bean
    @Conditional(SecurityMode.FormAuth)
    UserDetailsService localUserDetailsService() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        List<String> toPrint = elements.collect { StackTraceElement s ->
            "\tat " + s.getClassName() + "." + s.getMethodName() + "(" + s.getFileName() + ":" + s.getLineNumber() + ")"
        }
        log.error( "Called FormSecurityConfiguration.localUserDetailsService stack trace:\n${toPrint.join("\n")}")
        new LocalUserDetailsService()
    }

    @Bean
    SecurityContextRepository httpSessionSecurityContextRepository(OAuthUtils oAuthUtils, UserAuthService userAuthService) {
        new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new SkillsHttpSessionSecurityContextRepository(oAuthUtils: oAuthUtils, userAuthService: userAuthService)
        )
    }

    @Bean(name = 'oauth2UserConverters')
    Map<String, OAuth2UserConverterService.OAuth2UserConverter> oAuth2UserConverterMap() {
        return [
                google: new OAuth2UserConverterService.GoogleUserConverter(),
                github: new OAuth2UserConverterService.GitHubUserConverter(),
                gitlab: new OAuth2UserConverterService.GitLabUserConverter(),
                auth0: new OAuth2UserConverterService.Auth0UserConverter(),
                hydra: new OAuth2UserConverterService.HydraUserConverter(),
                keycloak: new OAuth2UserConverterService.KeycloakUserConverter(),
                azure: new OAuth2UserConverterService.AzureConverter(),
        ]
    }

    @Bean
    AuthenticationFailureHandler authenticationFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler()
    }

    @Component
    static final class RestAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
        @Override
        void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
            clearAuthenticationAttributes(request)
            writeNullJson(response)
        }
    }

    @Component
    static final class SkillsClientOAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
        @Override
        protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
            String targetUrl = request.getSession(false).getAttribute(SKILLS_REDIRECT_URI)
            if (targetUrl) {
                return targetUrl
            } else {
                return super.determineTargetUrl(request, response)
            }
        }
    }

    @Component
    static final class RestLogoutSuccessHandler extends HttpStatusReturningLogoutSuccessHandler {
        RestLogoutSuccessHandler() {
            super(HttpStatus.OK)
        }

        @Override
        void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
            writeNullJson(response)
            super.onLogoutSuccess(request, response, authentication)
        }
    }

    @Component
    static final class SkillsClientOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
        @Delegate
        HttpSessionOAuth2AuthorizationRequestRepository httpSessionOAuth2AuthorizationRequestRepository = new HttpSessionOAuth2AuthorizationRequestRepository()

        @Override
        void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
            httpSessionOAuth2AuthorizationRequestRepository.saveAuthorizationRequest(authorizationRequest, request, response)
            String skillsRedirectUri = request.getParameter(SKILLS_REDIRECT_URI)
            if (skillsRedirectUri) {
                request.getSession(false).setAttribute(SKILLS_REDIRECT_URI, skillsRedirectUri)
            }
        }
        @Override
        OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
            return httpSessionOAuth2AuthorizationRequestRepository.removeAuthorizationRequest(request, response)
        }

        OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
            return httpSessionOAuth2AuthorizationRequestRepository.loadAuthorizationRequest(request)
        }
    }

    static final String NULL_JSON = 'null'
    static writeNullJson(HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        response.setContentLength(NULL_JSON.bytes.length)
        response.getWriter().write(NULL_JSON)
        response.getWriter().flush()
    }
}
