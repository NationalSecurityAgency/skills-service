package skills.service.auth

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.*
import org.springframework.core.annotation.Order
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import skills.service.auth.jwt.JwtAuthenticationFilter
import skills.service.auth.jwt.JwtAuthorizationFilter
import skills.service.auth.jwt.JwtUserDetailsService
import skills.service.auth.pki.PkiUserDetailsService

@Configuration('securityConfig')
@ConfigurationProperties(prefix="skills.authorization")
@Slf4j
class SecurityConfiguration {

    @Value('${skills.authorization.authMode:#{T(skills.service.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Autowired
    UserDetailsService userDetailsService

    @Autowired
    void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        if (authMode == AuthMode.JWT) {
            auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder())
        }
    }

    @Bean
    @Conditional(JwtCondition)
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder()
    }

    @Bean
    @Conditional(PkiCondition)
    UserDetailsService pkiUserDetailsService() {
        new PkiUserDetailsService()
    }

    @Bean
    @Conditional(JwtCondition)
    UserDetailsService jwtUserDetailsService() {
        new JwtUserDetailsService()
    }

    @Configuration
    @Order(1)
    class CorsHttpSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            // CORS endpoints config
            http
                .antMatcher("/api/**")
                .csrf().disable()
                .cors().configurationSource(corsConfig())
                .and()
                .authorizeRequests().anyRequest().permitAll()
        }

        @Bean
        SkillsCorsConfigurationSource corsConfig() {
            return new SkillsCorsConfigurationSource()
        }
    }

    @Configuration
    static class PortalHttpSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Value('#{securityConfig.authMode}}')
        AuthMode authMode = AuthMode.DEFAULT_AUTH_MODE

        @Autowired
        UserDetailsService userDetailsService

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            log.info("Configuting authorization mode [${authMode}]")
            // Portal endpoints config
            switch (authMode) {
                case AuthMode.PKI:
                    http
                            .cors().and().csrf().disable()
                            .authorizeRequests()
                            .antMatchers("/", "/api/**", "/icons/**", "/static/**", "/login*", "/performLogin", "/createAccount", "/app/userInfo", "index.html").permitAll()
                            .antMatchers('/admin/**').hasRole('PROJECT_ADMIN')
                            .anyRequest().authenticated()
                            .and()
                            .x509()
                            .subjectPrincipalRegex(/(.*)/)
                            .userDetailsService(userDetailsService)
                            .and()
                            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    break

                case AuthMode.JWT:
                    http
                            .cors().and().csrf().disable()
                            .authorizeRequests()
                            .antMatchers("/", "/api/**", "/icons/**", "/static/**", "/login*", "/performLogin", "/createAccount", "/app/userInfo", "index.html").permitAll()
                            .antMatchers('/admin/**').hasRole('PROJECT_ADMIN')
                            .anyRequest().authenticated()
                            .and()
                            .addFilter(jwtAuthenticationFilter())
                            .addFilter(jwtAuthorizationFilter())
                            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    break

                default:
                    throw new RuntimeException("Unknown AuthMode [${authMode}]")
            }
        }

        @Bean
        @Conditional(JwtCondition)
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            JwtAuthenticationFilter filter = new JwtAuthenticationFilter(authenticationManager())
            filter.setFilterProcessesUrl('/performLogin')
            return filter
        }

        @Bean
        @Conditional(JwtCondition)
        JwtAuthorizationFilter jwtAuthorizationFilter() {
            JwtAuthorizationFilter filter = new JwtAuthorizationFilter(authenticationManager())
            return filter
        }

    }

    static class PkiCondition implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            AuthMode authMode = AuthMode.getFromContext(context)
            return authMode == AuthMode.PKI
        }
    }
    static class JwtCondition implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            AuthMode authMode = AuthMode.getFromContext(context)
            return authMode == AuthMode.JWT
        }
    }
}
