package skills.service.auth.form

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler
import org.springframework.stereotype.Component
import skills.service.auth.form.oauth2.OAuth2UserConverterService

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static skills.service.auth.SecurityConfiguration.*

@Conditional(FormAuth)
@Component
@Configuration
@Slf4j
class FormSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private PortalWebSecurityHelper portalWebSecurityHelper

    @Autowired
    private RestAccessDeniedHandler restAccessDeniedHandler

    @Autowired
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint

    @Autowired
    private AuthenticationFailureHandler restAuthenticationFailureHandler

    @Autowired
    private RestAuthenticationSuccessHandler restAuthenticationSuccessHandler

    @Autowired
    private RestLogoutSuccessHandler restLogoutSuccessHandler

    @Autowired
    void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(localUserDetailsService())
                .passwordEncoder(passwordEncoder())
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.info("Configuring FORM authorization mode")

        // Portal endpoints config
        portalWebSecurityHelper.configureHttpSecurity(http)
                .securityContext().securityContextRepository(httpSessionSecurityContextRepository())
        .and()
                .exceptionHandling()
                .accessDeniedHandler(restAccessDeniedHandler)
                .authenticationEntryPoint(restAuthenticationEntryPoint)
        .and()
                .formLogin()
                    .loginPage("/skills-login")
                    .loginProcessingUrl("/performLogin")
                    .successHandler(restAuthenticationSuccessHandler)
                    .failureHandler(restAuthenticationFailureHandler)
        .and()
                .oauth2Login()
                    .loginPage("/skills-login")
                    .failureHandler(restAuthenticationFailureHandler)
        .and()
                .logout()
                    .logoutSuccessHandler(restLogoutSuccessHandler)
    }

    @Override
    @Bean(name = 'defaultAuthManager')
    @Primary
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

    @Bean
    SkillsHttpSessionSecurityContextRepository httpSessionSecurityContextRepository() {
        return new SkillsHttpSessionSecurityContextRepository()
    }

    @Bean(name = 'oauth2UserConverters')
    Map<String, OAuth2UserConverterService.OAuth2UserConverter> oAuth2UserConverterMap() {
        return [
                google: new OAuth2UserConverterService.GoogleUserConverter(),
                github: new OAuth2UserConverterService.GitHubUserConverter(),
        ]
    }

    @Bean
    AuthenticationFailureHandler authenticationFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler()
    }

    @Component
    static class RestAccessDeniedHandler implements AccessDeniedHandler {
        @Override
        void handle(final HttpServletRequest request, final HttpServletResponse response, final AccessDeniedException ex) throws IOException, ServletException {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        }
    }

    @Component
    static final class RestAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
        @Override
        void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
            clearAuthenticationAttributes(request)
            writeNullJson(response)
        }
    }

    @Component
    static final class RestLogoutSuccessHandler extends HttpStatusReturningLogoutSuccessHandler {
        RestLogoutSuccessHandler() {
            super(HttpStatus.OK)
        }

        @Override
        void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
            writeNullJson(response)
            super.onLogoutSuccess(request, response, authentication)
        }
    }

    static final String NULL_JSON = 'null'
    static writeNullJson(HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        response.setContentLength(NULL_JSON.bytes.length)
        response.getWriter().write(NULL_JSON)
        response.getWriter().flush()
    }
}