package skills.auth

import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.stereotype.Component
import skills.storage.model.auth.RoleName

@Component
class PortalWebSecurityHelper {
    HttpSecurity configureHttpSecurity(HttpSecurity http) {
        http
                .csrf().disable()
                .authorizeRequests()
                .antMatchers("/", "/favicon.ico", "/icons/**", "/static/**", "/skills.ico", "/skills.jpeg", "/error", "/oauth/**", "/app/oAuthProviders", "/login*", "/bootstrap/**", "/performLogin", "/createAccount", "/createRootAccount", '/grantFirstRoot', '/userExists/**', "/app/userInfo", "/app/users/validExistingDashboardUserId/*", "/app/oAuthProviders", "index.html", "/public/**").permitAll()
                .antMatchers('/admin/**').hasRole('PROJECT_ADMIN')
                .antMatchers('/supervisor/**').hasAnyAuthority(RoleName.ROLE_SUPERVISOR.name(), RoleName.ROLE_SUPER_DUPER_USER.name())
                .antMatchers('/root/isRoot').hasAnyAuthority(RoleName.values().collect {it.name()}.toArray(new String[0]))
                .antMatchers('/root/**').hasRole('SUPER_DUPER_USER')
                .anyRequest().authenticated()
        http.headers().frameOptions().disable()

        return http
    }
}
