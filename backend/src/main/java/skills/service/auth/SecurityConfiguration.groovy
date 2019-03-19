package skills.service.auth

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Configuration('securityConfig')
@ConfigurationProperties(prefix = "skills.authorization")
@Slf4j
class SecurityConfiguration {

    @Value('${skills.authorization.authMode:#{T(skills.service.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Component
    static class PortalWebSecurityHelper {
        HttpSecurity configureHttpSecurity(HttpSecurity http) {
            http
                    .csrf().disable()
                    .authorizeRequests()
                    .antMatchers("/", "/favicon.ico", "/icons/**", "/static/**", "/error", "/oauth/**", "/login*", "/bootstrap/**", "/performLogin", "/createAccount", "/createRootAccount", '/grantFirstRoot', "/app/userInfo", "/app/users/validExistingDashboardUserId/*", "/app/oAuthProviders", "index.html").permitAll()
                    .antMatchers('/admin/**').hasRole('PROJECT_ADMIN')
                    .antMatchers('/server/**').hasAnyRole('SERVER', 'PROJECT_ADMIN')
                    .antMatchers('/grantRoot').hasRole('SUPER_DUPER_USER')
                    .anyRequest().authenticated()
            http.headers().frameOptions().sameOrigin()
            return http
        }
    }

    @Component
    @Configuration
    @Order(99)
    static class CorsSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Autowired
        private PortalWebSecurityHelper portalWebSecurityHelper

        @Autowired
        private RestAuthenticationEntryPoint restAuthenticationEntryPoint

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/api/**").cors()
            portalWebSecurityHelper.configureHttpSecurity(http)
                    .exceptionHandling().authenticationEntryPoint(restAuthenticationEntryPoint)
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
}
