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
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.web.client.HttpClientErrorException
import skills.auth.SkillsAuthorizationException
import skills.auth.UserInfo
import skills.utils.RetryUtil

import javax.transaction.Transactional

@Slf4j
class PkiUserDetailsService implements UserDetailsService, AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    @Autowired
    skills.auth.UserAuthService userAuthService

    @Autowired
    PkiUserLookup pkiUserLookup

    @Override
    @Transactional
    UserDetails loadUserByUsername(String dn) throws UsernameNotFoundException {
        this.doLoadUserByUsername(dn)
    }

    private UserDetails  doLoadUserByUsername(String dn) throws UsernameNotFoundException {
        try {
            return (UserInfo)RetryUtil.withRetry(3) {
                UserInfo userInfo
                userInfo = pkiUserLookup.lookupUserDn(dn)
                if (userInfo) {
                    UserInfo existingUserInfo = userAuthService.loadByUserId(userInfo.username?.toLowerCase())
                    if (existingUserInfo) {
                        userInfo.password = existingUserInfo.password
                        userInfo.nickname = existingUserInfo.nickname
                    } else {
                        userInfo.password = 'PKI_AUTHENTICATED'
                    }

                    // update user properties and load user roles, or create the account if this is the first time the user has connected
                    userInfo = userAuthService.createOrUpdateUser(userInfo)
                } else {
                    throw new SkillsAuthorizationException("Unknown user [$dn]")
                }

                return userInfo
            }
        } catch (Exception e) {
            String msg = "Unable to retrieve user info for [${dn}] - ${e.getMessage()}"
            if (e.getCause() instanceof HttpClientErrorException) {
                msg = ((HttpClientErrorException) e.getCause()).getResponseBodyAsString()
            } else if (e instanceof HttpClientErrorException) {
                msg = ((HttpClientErrorException) e).getResponseBodyAsString()
            }
            throw new BadCredentialsException(msg, e)
        }
    }

    @Override
    UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        return loadUserByUsername(token.getName())
    }
}
