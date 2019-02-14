package skills.service.auth

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.*
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextPersistenceFilter
import org.springframework.stereotype.Component
import skills.service.auth.jwt.JwtUserDetailsService
import skills.service.auth.jwt.OAuth2UserConverterService
import skills.service.auth.jwt.SkillsHttpSessionSecurityContextRepository
import skills.service.auth.pki.PkiUserDetailsService

@Component
@Configuration('securityConfig')
@ConfigurationProperties(prefix = "skills.authorization")
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

    @Component
    static class PortalWebSecurityHelper {
        HttpSecurity configureHttpSecurity(HttpSecurity http) {
            http
                    .cors().and().csrf().disable()
                    .authorizeRequests()
                    .antMatchers("/", "/icons/**", "/static/**", "/login*", "/performLogin", "/createAccount", "/app/userInfo", "/oauth/**", "/app/oAuthProviders", "index.html").permitAll()
                    .antMatchers('/admin/**').hasRole('PROJECT_ADMIN')
                    .anyRequest().authenticated()
            return http
        }
    }

    @Configuration
    static class PortalHttpSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Value('#{securityConfig.authMode}}')
        AuthMode authMode = AuthMode.DEFAULT_AUTH_MODE

        @Autowired
        UserDetailsService userDetailsService

        @Autowired
        PortalWebSecurityHelper portalWebSecurityHelper

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            log.info("Configuring authorization mode [${authMode}]")

            // Portal endpoints config
            portalWebSecurityHelper.configureHttpSecurity(http)

            switch (authMode) {
                case AuthMode.PKI:
                    http
                            .x509()
                            .subjectPrincipalRegex(/(.*)/)
                            .and()
                            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    break

                case AuthMode.JWT:
                    http
                            .addFilter(securityContextPersistenceFilter())
                            .formLogin()
                            .loginPage("/")
                            .loginProcessingUrl("/performLogin")
                            .failureUrl("/")
                            .and()
                            .oauth2Login()
                            .loginPage("/")
                            .failureUrl("/")
                    break

                default:
                    throw new RuntimeException("Unknown AuthMode [${authMode}]")
            }
        }

        @Bean
        @Conditional(JwtCondition)
        SecurityContextPersistenceFilter securityContextPersistenceFilter() {
            return new SecurityContextPersistenceFilter(httpSessionSecurityContextRepository())
        }

        @Bean
        @Conditional(JwtCondition)
        HttpSessionSecurityContextRepository httpSessionSecurityContextRepository() {
            return new SkillsHttpSessionSecurityContextRepository()
        }

        @Override
        @Conditional(JwtCondition)
        @Bean
        AuthenticationManager authenticationManagerBean() throws Exception {
            // provides the default AuthenticationManager as a Bean
            return super.authenticationManagerBean()
        }

        @Conditional(JwtCondition)
        @Bean(name = 'oauth2UserConverters')
        Map<String, OAuth2UserConverterService.OAuth2UserConverter> oAuth2UserConverterMap() {
            return [
                    google : new OAuth2UserConverterService.GoogleUserConverter(),
                    github : new OAuth2UserConverterService.GitHubUserConverter(),
            ]
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
