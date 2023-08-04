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
package skills.auth

import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Conditional
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Component

@Conditional(SecurityMode.FormAuth)
@Component
@Slf4j
class AutoLoginService {

    private AutoLoginProcessor autoLoginProcessor

    @Autowired
    @Qualifier('formSecurityFilterChain')
    SecurityFilterChain formSecurityFilterChain

    @Autowired
    SecurityContextRepository securityContextRepository

    @PostConstruct
    void init() {
        if (formSecurityFilterChain) {
            autoLoginProcessor = new AutoLoginProcessor(formSecurityFilterChain.getFilters().find { it.class == UsernamePasswordAuthenticationFilter }, securityContextRepository)
        }
    }

    void autologin(UserInfo userInfo, String password, HttpServletRequest request, HttpServletResponse response) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userInfo, password, userInfo.getAuthorities())
        autoLoginProcessor.autoLogin(usernamePasswordAuthenticationToken, request, response)
    }

    static class AutoLoginProcessor extends UsernamePasswordAuthenticationFilter {
        private UsernamePasswordAuthenticationFilter delegate
        private UsernamePasswordAuthenticationToken currentAuthRequest = null

        AutoLoginProcessor(UsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter, SecurityContextRepository securityContextRepository) {
            this.delegate = usernamePasswordAuthenticationFilter
            this.setSecurityContextRepository(securityContextRepository)
            this.setAuthenticationSuccessHandler(this.delegate.getSuccessHandler())
            this.setSessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy())
        }

        void autoLogin(UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToke, HttpServletRequest request, HttpServletResponse response) {
            this.currentAuthRequest = usernamePasswordAuthenticationToke
            super.doFilter(request, response, () -> {})
            this.currentAuthRequest = null
        }

        @Override
        Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
            return this.delegate.getAuthenticationManager().authenticate(this.currentAuthRequest);
        }

        @Override
        protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
            return true
        }
    }
}
