package skills.service.auth.form

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextPersistenceFilter
import org.springframework.stereotype.Component

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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
    RestAccessDeniedHandler accessDeniedHandler

    @Autowired
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint

    @Autowired
    void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(localUserDetailsService())
                .passwordEncoder(passwordEncoder())
    }

    @Override
    @Bean
    AuthenticationManager authenticationManagerBean() throws Exception {
        // provides the default AuthenticationManager as a Bean
        return super.authenticationManagerBean()
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
                .addFilter(securityContextPersistenceFilter())
                .exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(restAuthenticationEntryPoint)
        .and()
                .formLogin()
                    .loginPage("/")
                    .loginProcessingUrl("/performLogin")
                    .failureHandler(new SimpleUrlAuthenticationFailureHandler())
        .and()
                .oauth2Login()
                    .loginPage("/")
                    .failureHandler(new SimpleUrlAuthenticationFailureHandler())
        .and()
                .logout()
                    .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
    }

    @Bean
    SecurityContextPersistenceFilter securityContextPersistenceFilter() {
        return new SecurityContextPersistenceFilter(httpSessionSecurityContextRepository())
    }

    @Bean
    HttpSessionSecurityContextRepository httpSessionSecurityContextRepository() {
        return new SkillsHttpSessionSecurityContextRepository()
    }

    @Bean(name = 'oauth2UserConverters')
    Map<String, OAuth2UserConverterService.OAuth2UserConverter> oAuth2UserConverterMap() {
        return [
                google: new OAuth2UserConverterService.GoogleUserConverter(),
                github: new OAuth2UserConverterService.GitHubUserConverter(),
        ]
    }

    @Component
    static class RestAccessDeniedHandler implements AccessDeniedHandler {
        @Override
        void handle(final HttpServletRequest request, final HttpServletResponse response, final AccessDeniedException ex) throws IOException, ServletException {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        }
    }

    @Component
    static final class RestAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
        RestAuthenticationEntryPoint() { super('/') }
        @Override
        void commence(
                final HttpServletRequest request,
                final HttpServletResponse response,
                final AuthenticationException authException) throws IOException {
            if (request.servletPath == '/performLogin') {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
            } else {
                // redirect to the login page and then forward to the requested page after successful login
                super.commence(request, response, authException)
            }
        }
    }
}