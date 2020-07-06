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
package skills.auth.form.oauth2

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component
import skills.auth.SecurityMode
import skills.auth.SkillsAuthorizationException
import skills.auth.UserAuthService
import skills.auth.UserInfo

import javax.annotation.Resource

@Component
@Conditional(SecurityMode.FormAuth)
class OAuth2UserConverterService {

    @Autowired
    UserAuthService userAuthService

    @Resource(name='oauth2UserConverters')
    Map<String, OAuth2UserConverter> lookup = [:]

    UserInfo convert(String clientId, OAuth2User oAuth2User) {
        UserInfo userInfo
        OAuth2UserConverter converter = lookup.get(clientId.toLowerCase())
        if (converter) {
            userInfo = converter.convert(clientId, oAuth2User)
            if (!userInfo.usernameForDisplay) {
                userInfo.usernameForDisplay = userInfo.username
            }
        } else {
            throw new SkillsAuthorizationException("No OAuth2UserConverter configured for clientId [${clientId}]")
        }
        return userInfo
    }

    static interface OAuth2UserConverter {
        String getClientId()
        UserInfo convert(String clientId, OAuth2User oAuth2User)
    }

    static class GitHubUserConverter implements OAuth2UserConverter {
        static final String NAME = 'name'
        static final String EMAIL = 'email'

        String clientId = 'github'

        @Override
        UserInfo convert(String clientId, OAuth2User oAuth2User) {
            String username = oAuth2User.getName()
            assert username, "Error getting name attribute of oAuth2User [${oAuth2User}] from clientId [$clientId]"
            String email =  oAuth2User.attributes.get(EMAIL)
            if (!email) {
                throw new SkillsAuthorizationException("Email must be available in your public Github profile")
            }
            String name = oAuth2User.attributes.get(NAME)
            if (!name) {
                throw new SkillsAuthorizationException("Name must be available in your public Github profile")
            }
            String firstName = name?.tokenize()?.first()
            List tokens = name?.tokenize()
            tokens?.pop()
            String lastName = tokens?.join(' ')
            return new UserInfo(
                    username: "${username}-${clientId}",
                    email:email,
                    firstName: firstName,
                    lastName: lastName,
            )
        }
    }

    static class GoogleUserConverter implements OAuth2UserConverter {
        static final String FIRST_NAME = 'given_name'
        static final String LAST_NAME = 'family_name'
        static final String EMAIL = 'email'

        String clientId = 'google'

        @Override
        UserInfo convert(String clientId, OAuth2User oAuth2User) {
            String username = oAuth2User.getName()
            assert username, "Error getting name attribute of oAuth2User [${oAuth2User}] from clientId [$clientId]"
            String firstName =  oAuth2User.attributes.get(FIRST_NAME)
            String lastName =  oAuth2User.attributes.get(LAST_NAME)
            String email =  oAuth2User.attributes.get(EMAIL)
            assert firstName && lastName && email, "First Name [$firstName], Last Name [$lastName], and email [$email] must be available in your public Google profile"

            return new UserInfo(
                    username: "${username}-${clientId}",
                    email:email,
                    firstName: firstName,
                    lastName: lastName,
            )
        }
    }
}
