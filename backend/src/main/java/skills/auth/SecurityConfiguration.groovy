package skills.auth

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
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextListener
import skills.storage.model.auth.RoleName

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Configuration('securityConfig')
@ConfigurationProperties(prefix = "skills.authorization")
@Slf4j
class SecurityConfiguration {

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Component
    static class PortalWebSecurityHelper {
        HttpSecurity configureHttpSecurity(HttpSecurity http) {
            http
                    .csrf().disable()
                    .authorizeRequests()
                    .antMatchers("/", "/favicon.ico", "/icons/**", "/static/**", "/skills.ico", "/skills.jpeg", "/error", "/oauth/**", "/app/oAuthProviders", "/login*", "/bootstrap/**", "/performLogin", "/createAccount", "/createRootAccount", '/grantFirstRoot', '/userExists/**', "/app/userInfo", "/app/users/validExistingDashboardUserId/*", "/app/oAuthProviders", "index.html").permitAll()
                    .antMatchers('/admin/**').hasRole('PROJECT_ADMIN')
                    .antMatchers('/root/isRoot').hasAnyAuthority(RoleName.values().collect {it.name()}.toArray(new String[0]))
                    .antMatchers('/root/**').hasRole('SUPER_DUPER_USER')
                    .anyRequest().authenticated()
            http.headers().frameOptions().disable()

            return http
        }
    }

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

        @Autowired
        void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
            auth
                    .userDetailsService(userDetailsService)
                    .passwordEncoder(passwordEncoder)
        }

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

    static class PkiAuth implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            AuthMode authMode = AuthMode.getFromContext(context)
            return authMode == AuthMode.PKI
        }
    }

    static class FormAuth implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            AuthMode authMode = AuthMode.getFromContext(context)
            return authMode == AuthMode.FORM
        }
    }

    @Bean
    RequestContextListener requestContextListener() {
        return new RequestContextListener()
    }
}
