package skills.service.auth.pki

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.context.NullSecurityContextRepository
import org.springframework.stereotype.Component

import static skills.service.auth.SecurityConfiguration.PkiAuth
import static skills.service.auth.SecurityConfiguration.PortalWebSecurityHelper

@Slf4j
@Conditional(PkiAuth)
@Component
@Configuration
class PkiSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    UserDetailsService userDetailsService

    @Bean
    @Conditional(PkiAuth)
    UserDetailsService pkiUserDetailsService() {
        new PkiUserDetailsService()
    }

    @Autowired
    PortalWebSecurityHelper portalWebSecurityHelper

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
