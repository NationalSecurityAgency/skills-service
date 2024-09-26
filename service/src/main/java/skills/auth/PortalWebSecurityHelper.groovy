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

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.DependsOn
import org.springframework.http.HttpMethod
import org.springframework.security.authorization.AuthorityAuthorizationManager
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.authorization.AuthorizationManagers
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.csrf.CsrfTokenRequestHandler
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import skills.auth.inviteOnly.InviteOnlyProjectAuthorizationManager
import skills.auth.userCommunity.UserCommunityAuthorizationManager
import skills.storage.model.auth.RoleName

import java.util.function.Supplier

@Component
@DependsOn(['inviteOnlyProjectAuthorizationManager', 'userCommunityAuthorizationManager'])
class PortalWebSecurityHelper {

    @Value('#{"${server.port:8080}"}')
    Integer serverPort

    @Value('#{"${skills.config.publiclyExposePrometheusMetrics:false}"}')
    Boolean publiclyExposePrometheusMetrics

    @Value('#{"${management.endpoints.web.base-path:/actuator}"}')
    String managementPath

    @Value('#{"${management.endpoints.web.path-mapping.prometheus:prometheus}"}')
    String prometheusPath

    @Value('#{"${skills.config.disableCsrfProtection:false}"}')
    Boolean disableCsrfProtection

    @Autowired
    InviteOnlyProjectAuthorizationManager inviteOnlyProjectAuthorizationManager

    @Autowired
    CookieCsrfTokenRepository cookieCsrfTokenRepository

    @Autowired
    UserCommunityAuthorizationManager userCommunityAuthorizationManager

    @Autowired
    SessionAuthenticationStrategy csrfAuthenticationStrategy

    HttpSecurity configureHttpSecurity(HttpSecurity http) {
        if (disableCsrfProtection) {
            http.csrf().disable()
        } else {
            http.csrf((csrf) -> csrf
                    .requireCsrfProtectionMatcher(new MultipartRequestMatcher())
                    .csrfTokenRepository(cookieCsrfTokenRepository)
//                    .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                    .sessionAuthenticationStrategy(csrfAuthenticationStrategy)
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                    .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
        }

        if (publiclyExposePrometheusMetrics) {
            http.authorizeHttpRequests().requestMatchers(HttpMethod.GET, "${managementPath}/${prometheusPath}").permitAll()
        }

        http.authorizeHttpRequests((authorize) ->
            authorize
                .requestMatchers("/", "/favicon.ico",
                    "/icons/**", "/static/**", "/assets/**", "/themes/**",
                    "/skilltree.ico",
                    "/error", "/oauth/**",
                    "/app/oAuthProviders", "/login*", "/login/**",
                    "/performLogin", "/createAccount",
                    "/createRootAccount", '/grantFirstRoot',
                    '/userExists/**', "/app/userInfo",
                    "/app/users/validExistingDashboardUserId/*", "/app/oAuthProviders",
                    "/index.html", "index.html", "/public/**",
                    "/skills-websocket/**", "/requestPasswordReset",
                    "/resetPassword/**", "/performPasswordReset",
                    "/resendEmailVerification/**", "/verifyEmail", "/userEmailIsVerified/*","/saml2/**").permitAll()
                .requestMatchers('/root/isRoot').hasAnyAuthority(RoleName.values().collect {it.name()}.toArray(new String[0]))
                .requestMatchers('/root/**').access(hasAnyAuthorityPlus([inviteOnlyProjectAuthorizationManager, userCommunityAuthorizationManager], RoleName.ROLE_SUPER_DUPER_USER.name()))
                .requestMatchers('/supervisor/**').access(hasAnyAuthorityPlus([inviteOnlyProjectAuthorizationManager, userCommunityAuthorizationManager], RoleName.ROLE_SUPERVISOR.name(), RoleName.ROLE_SUPER_DUPER_USER.name()))
                .requestMatchers('/admin/quiz-definitions/**').hasAnyAuthority(RoleName.ROLE_QUIZ_ADMIN.name(), RoleName.ROLE_QUIZ_READ_ONLY.name(), RoleName.ROLE_SUPER_DUPER_USER.name())
                .requestMatchers('/admin/admin-group-definitions/**').access(hasAnyAuthorityPlus([userCommunityAuthorizationManager], RoleName.ROLE_ADMIN_GROUP_OWNER.name(), RoleName.ROLE_SUPER_DUPER_USER.name()))
                .requestMatchers('/admin/**').access(hasAnyAuthorityPlus([inviteOnlyProjectAuthorizationManager, userCommunityAuthorizationManager], RoleName.ROLE_PROJECT_ADMIN.name(), RoleName.ROLE_SUPER_DUPER_USER.name(), RoleName.ROLE_PROJECT_APPROVER.name()))
                .requestMatchers('/app/**').access(AuthorizationManagers.allOf(inviteOnlyProjectAuthorizationManager, userCommunityAuthorizationManager))
                .requestMatchers('/api/**').access(AuthorizationManagers.allOf(inviteOnlyProjectAuthorizationManager, userCommunityAuthorizationManager))
                .requestMatchers("/${managementPath}/**").hasAuthority(RoleName.ROLE_SUPER_DUPER_USER.name())
                .anyRequest().authenticated()
        )
        http.headers().frameOptions().disable()

        return http
    }

    AuthorizationManager<RequestAuthorizationContext> hasAnyAuthorityPlus(List<AuthorizationManager<RequestAuthorizationContext>> managers, String... authorities) {
        return AuthorizationManagers.allOf(([AuthorityAuthorizationManager.hasAnyAuthority(authorities)] + managers) as AuthorizationManager[])
    }
}

final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
    private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler()

    @Override
    void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        /*
         * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
         * the CsrfToken when it is rendered in the response body.
         */
        this.delegate.handle(request, response, csrfToken)
    }

    @Override
    String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        /*
         * If the request contains a request header, use CsrfTokenRequestAttributeHandler
         * to resolve the CsrfToken. This applies when a single-page application includes
         * the header value automatically, which was obtained via a cookie containing the
         * raw CsrfToken.
         */
        if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
            return super.resolveCsrfTokenValue(request, csrfToken)
        }
        /*
         * In all other cases (e.g. if the request contains a request parameter), use
         * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
         * when a server-side rendered form includes the _csrf request parameter as a
         * hidden input.
         */
        return this.delegate.resolveCsrfTokenValue(request, csrfToken)
    }
}

final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf")
        // Render the token value to a cookie by causing the deferred token to be loaded
        csrfToken.getToken()

        filterChain.doFilter(request, response)
    }
}

final class MultipartRequestMatcher implements RequestMatcher {

    private final HashSet<String> allowedMethods = new HashSet<>(Arrays.asList("GET", "HEAD", "TRACE", "OPTIONS"))
    private final OrRequestMatcher pathMatcher = new OrRequestMatcher(
            new AntPathRequestMatcher("/api/upload"),
            new AntPathRequestMatcher("/admin/projects/*/icons/upload"),
            new AntPathRequestMatcher("/supervisor/icons/upload"),
            new AntPathRequestMatcher("/admin/projects/*/skills/*/video"),
    )

    @Override
    boolean matches(HttpServletRequest request) {
        Boolean matches = (pathMatcher.matches(request) && !this.allowedMethods.contains(request.getMethod()))
        return matches
    }
}