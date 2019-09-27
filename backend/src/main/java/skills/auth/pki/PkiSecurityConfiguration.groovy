package skills.auth.pki

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.*
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.context.NullSecurityContextRepository
import org.springframework.stereotype.Component
import skills.auth.PortalWebSecurityHelper
import skills.auth.SecurityMode

@Slf4j
@Conditional(SecurityMode.PkiAuth)
@Component
@Configuration
class PkiSecurityConfiguration extends WebSecurityConfigurerAdapter {

//    @Autowired
//    UserDetailsService userDetailsService

    @Bean
    @Conditional(SecurityMode.PkiAuth)
    @DependsOn('pkiUserLookup')
    UserDetailsService pkiUserDetailsService() {
        new PkiUserDetailsService()
    }

    @Autowired
    PortalWebSecurityHelper portalWebSecurityHelper

    @Autowired
    void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(pkiUserDetailsService())
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.info("Configuring PKI authorization mode")

        // Portal endpoints config
        portalWebSecurityHelper.configureHttpSecurity(http)
        http
                .x509()
                .subjectPrincipalRegex(/(.*)/)
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }

    @Override
    @Bean(name = 'defaultAuthManager')
    @Primary
    AuthenticationManager authenticationManagerBean() throws Exception {
        // provides the default AuthenticationManager as a Bean
        return super.authenticationManagerBean()
    }

    @Bean
    NullSecurityContextRepository httpSessionSecurityContextRepository() {
        return new NullSecurityContextRepository()
    }
}
