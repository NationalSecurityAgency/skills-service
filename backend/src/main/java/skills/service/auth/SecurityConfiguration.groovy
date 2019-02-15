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
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextPersistenceFilter
import org.springframework.stereotype.Component
import skills.service.auth.form.JwtUserDetailsService
import skills.service.auth.form.OAuth2UserConverterService
import skills.service.auth.form.SkillsHttpSessionSecurityContextRepository
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
        if (authMode == AuthMode.FORM) {
            auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder())
        }
    }

    @Bean
    @Conditional(FormAuth)
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder()
    }

    @Bean
    @Conditional(PkiAuth)
    UserDetailsService pkiUserDetailsService() {
        new PkiUserDetailsService()
    }

    @Bean
    @Conditional(FormAuth)
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

                case AuthMode.FORM:
                    http
                            .addFilter(securityContextPersistenceFilter())
                            .formLogin()
                            .loginPage("/")
                                .loginProcessingUrl("/performLogin")
                                .failureHandler(new SimpleUrlAuthenticationFailureHandler())
                            .and()
                            .oauth2Login()
                                .loginPage("/")
                                .failureHandler(new SimpleUrlAuthenticationFailureHandler())
                            .and().logout()
                    break

                default:
                    throw new RuntimeException("Unknown AuthMode [${authMode}]")
            }
        }

        @Bean
        @Conditional(FormAuth)
        SecurityContextPersistenceFilter securityContextPersistenceFilter() {
            return new SecurityContextPersistenceFilter(httpSessionSecurityContextRepository())
        }

        @Bean
        @Conditional(FormAuth)
        HttpSessionSecurityContextRepository httpSessionSecurityContextRepository() {
            return new SkillsHttpSessionSecurityContextRepository()
        }

        @Override
        @Bean
        AuthenticationManager authenticationManagerBean() throws Exception {
            // provides the default AuthenticationManager as a Bean
            return super.authenticationManagerBean()
        }

        @Conditional(FormAuth)
        @Bean(name = 'oauth2UserConverters')
        Map<String, OAuth2UserConverterService.OAuth2UserConverter> oAuth2UserConverterMap() {
            return [
                    google : new OAuth2UserConverterService.GoogleUserConverter(),
                    github : new OAuth2UserConverterService.GitHubUserConverter(),
            ]
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
