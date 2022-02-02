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

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.util.matcher.AndRequestMatcher
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component
import skills.storage.model.auth.RoleName

import javax.servlet.http.HttpServletRequest

@Component
class PortalWebSecurityHelper {

    @Value('#{"${management.server.port:8808}"}')
    Integer managementPort

    @Value('#{"${server.port:8080}"}')
    Integer serverPort

    @Value('#{"${skills.config.publiclyExposePrometheusMetrics:false}"}')
    Boolean publiclyExposePrometheusMetrics

    @Value('#{"${management.endpoints.web.base-path:/actuator}"}')
    String managementPath

    @Value('#{"${management.endpoints.web.path-mapping.prometheus:prometheus}"}')
    String prometheusPath

    HttpSecurity configureHttpSecurity(HttpSecurity http) {

        http
            .csrf().disable()

        if (publiclyExposePrometheusMetrics) {
            Integer port = serverPort
            if (managementPort) {
                port = managementPort
            }

            http.authorizeRequests()
                    .requestMatchers(forPortAndPath(port, HttpMethod.GET, "${managementPath}/${prometheusPath}")).permitAll()
        }

        http.authorizeRequests().antMatchers("/", "/favicon.ico",
                    "/icons/**", "/static/**",
                    "/skilltree.ico",
                    "/error", "/oauth/**",
                    "/app/oAuthProviders", "/login*", "/login/**",
                    "/performLogin", "/createAccount",
                    "/createRootAccount", '/grantFirstRoot',
                    '/userExists/**', "/app/userInfo",
                    "/app/users/validExistingDashboardUserId/*", "/app/oAuthProviders",
                    "index.html", "/public/**",
                    "/skills-websocket/**", "/requestPasswordReset",
                    "/resetPassword/**", "/performPasswordReset",
                    "/resendEmailVerification/**", "/verifyEmail", "/userEmailIsVerified/*").permitAll()
            .antMatchers('/admin/**').hasAnyAuthority(RoleName.ROLE_PROJECT_ADMIN.name(), RoleName.ROLE_SUPER_DUPER_USER.name())
            .antMatchers('/supervisor/**').hasAnyAuthority(RoleName.ROLE_SUPERVISOR.name(), RoleName.ROLE_SUPER_DUPER_USER.name())
            .antMatchers('/root/isRoot').hasAnyAuthority(RoleName.values().collect {it.name()}.toArray(new String[0]))
            .antMatchers('/root/**').hasRole('SUPER_DUPER_USER')
            .antMatchers("/${managementPath}/**").hasRole('SUPER_DUPER_USER')
            .anyRequest().authenticated()
        http.headers().frameOptions().disable()

        return http
    }

    private RequestMatcher forPortAndPath(final int port, final String pathPattern) {
        return new AndRequestMatcher(forPort(port), new AntPathRequestMatcher(pathPattern));
    }

    private RequestMatcher forPortAndPath(final int port, final HttpMethod method,
                                          final String pathPattern) {
        return new AndRequestMatcher(forPort(port), new AntPathRequestMatcher(pathPattern, method.name()));
    }

    private RequestMatcher forPort(final int port) {
        return {HttpServletRequest request -> port == request.getLocalPort()};
    }
}
