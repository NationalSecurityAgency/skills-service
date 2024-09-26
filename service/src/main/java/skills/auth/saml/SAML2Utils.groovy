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
package skills.auth.saml

import groovy.util.logging.Slf4j
import org.opensaml.saml.saml2.core.Assertion
import org.opensaml.saml.saml2.core.Attribute
import org.opensaml.saml.saml2.core.AttributeStatement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.auth.SecurityMode
import skills.auth.SkillsAuthorizationException
import skills.auth.UserInfo

@Component
@Conditional(SecurityMode.SAML2Auth)
@Slf4j
class SAML2Utils {

    @Autowired
    skills.auth.UserAuthService userAuthService

    static final String FIRST_NAME = 'firstName'
    static final String EMAIL = 'email'
    static final String LAST_NAME = 'lastName'

    @Transactional
    Authentication convertToSkillsAuth(Saml2Authentication auth) {
        Authentication skillsAuth = auth
        if (auth && auth instanceof Saml2Authentication && auth.principal instanceof Saml2AuthenticatedPrincipal) {
            Saml2AuthenticatedPrincipal principal = ((Saml2AuthenticatedPrincipal)auth.principal)
            if(principal!=null) {
                String username =  principal.getName()
                String firstName = principal.getFirstAttribute(FIRST_NAME)
                String lastName = principal.getFirstAttribute(LAST_NAME)
                String email  = principal.getFirstAttribute(EMAIL)
                String displayName = firstName + ' ' + lastName

                if (!username) {
                    throw new SkillsAuthorizationException("Username must be available for you Okta profile")
                }
                UserInfo currentUser = new UserInfo(
                                                username: username,
                                                usernameForDisplay: displayName,
                                                email:email,
                                                firstName: firstName,
                                                lastName: lastName,
                                     )

                // Create/Update the UserInfo in the database.
                currentUser = userAuthService.createOrUpdateUser(currentUser)

                // Create new Authentication using UserInfo Object
                skillsAuth = new UsernamePasswordAuthenticationToken(currentUser, auth, currentUser.authorities)
            }
        }
        return skillsAuth
    }

    @Transactional
    UserInfo convertToUserInfo(Saml2Authentication auth, boolean persist) {
        UserInfo currentUser = null;
        if (auth && auth instanceof Saml2Authentication && auth.principal instanceof Saml2AuthenticatedPrincipal) {
            Saml2AuthenticatedPrincipal principal = ((Saml2AuthenticatedPrincipal)auth.principal)
            if(principal!=null) {
                String username =  principal.getName()
                String firstName = principal.getFirstAttribute(FIRST_NAME)
                String lastName = principal.getFirstAttribute(LAST_NAME)
                String email  = principal.getFirstAttribute(EMAIL)
                String displayName = firstName + ' ' + lastName

                if (!username) {
                    throw new SkillsAuthorizationException("Username must be available for you Okta profile")
                }
                currentUser = new UserInfo(
                        username: username,
                        usernameForDisplay: displayName,
                        email:email,
                        firstName: firstName,
                        lastName: lastName,
                )

                // Create/Update the UserInfo in the database.
                if(persist)
                    currentUser = userAuthService.createOrUpdateUser(currentUser)
            }
        }
        return currentUser;

    }


}
