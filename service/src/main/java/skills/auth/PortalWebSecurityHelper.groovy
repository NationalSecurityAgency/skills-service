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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.DependsOn
import org.springframework.http.HttpMethod
import org.springframework.security.authorization.AuthorityAuthorizationManager
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.authorization.AuthorizationManagers
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.stereotype.Component
import skills.auth.inviteOnly.InviteOnlyProjectAuthorizationManager
import skills.auth.userCommunity.UserCommunityAuthorizationManager
import skills.storage.model.auth.RoleName

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

    @Autowired
    InviteOnlyProjectAuthorizationManager inviteOnlyProjectAuthorizationManager

    @Autowired
    UserCommunityAuthorizationManager userCommunityAuthorizationManager

    HttpSecurity configureHttpSecurity(HttpSecurity http) {

        http.csrf().disable()

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
                    "/resendEmailVerification/**", "/verifyEmail", "/userEmailIsVerified/*").permitAll()
                .requestMatchers('/root/isRoot').hasAnyAuthority(RoleName.values().collect {it.name()}.toArray(new String[0]))
                .requestMatchers('/root/**').access(hasAnyAuthorityPlus([inviteOnlyProjectAuthorizationManager, userCommunityAuthorizationManager], RoleName.ROLE_SUPER_DUPER_USER.name()))
                .requestMatchers('/supervisor/**').access(hasAnyAuthorityPlus([inviteOnlyProjectAuthorizationManager, userCommunityAuthorizationManager], RoleName.ROLE_SUPERVISOR.name(), RoleName.ROLE_SUPER_DUPER_USER.name()))
                .requestMatchers('/admin/quiz-definitions/**').hasAnyAuthority(RoleName.ROLE_QUIZ_ADMIN.name(), RoleName.ROLE_QUIZ_READ_ONLY.name(), RoleName.ROLE_SUPER_DUPER_USER.name())
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
