package skills.service.auth.form

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextPersistenceFilter
import org.springframework.stereotype.Component

import static skills.service.auth.SecurityConfiguration.FormAuth
import static skills.service.auth.SecurityConfiguration.PortalWebSecurityHelper

@Conditional(FormAuth)
@Component
@Configuration
@Slf4j
class FormSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    PortalWebSecurityHelper portalWebSecurityHelper

    @Autowired
    void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(localUserDetailsService())
                .passwordEncoder(passwordEncoder())
    }


    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder()
    }

    @Bean
    UserDetailsService localUserDetailsService() {
        new LocalUserDetailsService()
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.info("Configuring FORM authorization mode")

        // Portal endpoints config
        portalWebSecurityHelper.configureHttpSecurity(http)
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
    }

    @Bean
    SecurityContextPersistenceFilter securityContextPersistenceFilter() {
        return new SecurityContextPersistenceFilter(httpSessionSecurityContextRepository())
    }

    @Bean
    HttpSessionSecurityContextRepository httpSessionSecurityContextRepository() {
        return new SkillsHttpSessionSecurityContextRepository()
    }

    @Override
    @Bean
    AuthenticationManager authenticationManagerBean() throws Exception {
        // provides the default AuthenticationManager as a Bean
        return super.authenticationManagerBean()
    }

    @Bean(name = 'oauth2UserConverters')
    Map<String, OAuth2UserConverterService.OAuth2UserConverter> oAuth2UserConverterMap() {
        return [
                google: new OAuth2UserConverterService.GoogleUserConverter(),
                github: new OAuth2UserConverterService.GitHubUserConverter(),
        ]
    }
}