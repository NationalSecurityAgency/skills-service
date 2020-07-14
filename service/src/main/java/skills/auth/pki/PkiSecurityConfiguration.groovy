/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    @Lazy
    AuthenticationManager authenticationManagerBean() throws Exception {
        // provides the default AuthenticationManager as a Bean
        return super.authenticationManagerBean()
    }

    @Bean
    NullSecurityContextRepository httpSessionSecurityContextRepository() {
        return new NullSecurityContextRepository()
    }
}
