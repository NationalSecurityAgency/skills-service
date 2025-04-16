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
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import skills.auth.SkillsAuthorizationException
import skills.auth.UserAuthService
import skills.auth.UserInfo
import skills.utils.RetryUtil

@Slf4j
class PkiUserDetailsService implements UserDetailsService, AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    @Autowired
    UserAuthService userAuthService

    @Autowired
    PkiUserLookup pkiUserLookup

    @Override
    UserDetails loadUserByUsername(String dn) throws UsernameNotFoundException {
        this.doLoadUserByUsername(dn)
    }

    private UserDetails  doLoadUserByUsername(String dn) throws UsernameNotFoundException {
        try {
            return (UserInfo)RetryUtil.withRetry(3) {
                UserInfo userInfo = pkiUserLookup.lookupUserDn(dn)
                if (!userInfo) {
                    throw new SkillsAuthorizationException("Unknown user [$dn]")
                }
                UserInfo existingUserInfo = userAuthService.loadByUserId(userInfo.username?.toLowerCase())
                userInfo.password = existingUserInfo?.password ?: 'PKI_AUTHENTICATED'
                userInfo.nickname = existingUserInfo?.nickname
                userInfo = userAuthService.addAuthorities(userInfo)

                return userInfo
            }
        } catch (Exception e) {
            String msg = "Unable to retrieve user info for [${dn}] - ${e.getMessage()}"
            if (e.getCause() instanceof RestClientResponseException) {
                msg = ((RestClientResponseException) e.getCause()).getResponseBodyAsString()
            } else if (e instanceof RestClientResponseException) {
                msg = ((RestClientResponseException) e).getResponseBodyAsString()
            }
            throw new BadCredentialsException(msg, e)
        }
    }

    @Override
    UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        return loadUserByUsername(token.getName())
    }
}
