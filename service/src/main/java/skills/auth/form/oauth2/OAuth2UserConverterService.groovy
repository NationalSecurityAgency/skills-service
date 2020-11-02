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

import org.apache.commons.lang3.StringUtils
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

    UserInfo convert(String providerId, OAuth2User oAuth2User) {
        UserInfo userInfo
        OAuth2UserConverter converter = lookup.get(providerId.toLowerCase())
        if (converter) {
            userInfo = converter.convert(providerId, oAuth2User)
            if (!userInfo.usernameForDisplay) {
                userInfo.usernameForDisplay = StringUtils.substringBeforeLast(userInfo.username, '-')
            }
        } else {
            throw new SkillsAuthorizationException("No OAuth2UserConverter configured for providerId [${providerId}]")
        }
        return userInfo
    }

    static interface OAuth2UserConverter {
        String getProviderId()
        UserInfo convert(String providerId, OAuth2User oAuth2User)
    }

    static class GitHubUserConverter implements OAuth2UserConverter {
        static final String NAME = 'name'
        static final String EMAIL = 'email'
        static final String LOGIN = 'login'

        String providerId = 'github'

        @Override
        UserInfo convert(String providerId, OAuth2User oAuth2User) {
            String username = oAuth2User.getName()
            assert username, "Error getting name attribute of oAuth2User [${oAuth2User}] from providerId [$providerId]"
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
            String login = oAuth2User.attributes.get(LOGIN)
            return new UserInfo(
                    username: "${username}-${providerId}",
                    usernameForDisplay: login,
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

        String providerId = 'google'

        @Override
        UserInfo convert(String providerId, OAuth2User oAuth2User) {
            String username = oAuth2User.getName()
            assert username, "Error getting name attribute of oAuth2User [${oAuth2User}] from providerId [$providerId]"
            String firstName =  oAuth2User.attributes.get(FIRST_NAME)
            String lastName =  oAuth2User.attributes.get(LAST_NAME)
            String email =  oAuth2User.attributes.get(EMAIL)
            String usernameForDisplay = StringUtils.substringBeforeLast(email, '@')
            assert firstName && lastName && email, "First Name [$firstName], Last Name [$lastName], and email [$email] must be available in your public Google profile"

            return new UserInfo(
                    username: "${username}-${providerId}",
                    usernameForDisplay: usernameForDisplay,
                    email:email,
                    firstName: firstName,
                    lastName: lastName,
            )
        }
    }

    static class GitLabUserConverter implements OAuth2UserConverter {
        static final String NAME = 'name'
        static final String EMAIL = 'email'

        String providerId = 'gitlab'

        @Override
        UserInfo convert(String providerId, OAuth2User oAuth2User) {
            String username = oAuth2User.getName()
            assert username, "Error getting name attribute of oAuth2User [${oAuth2User}] from providerId [$providerId]"
            String email =  oAuth2User.attributes.get(EMAIL)
            if (!email) {
                throw new SkillsAuthorizationException("Email must be available in your public GitLab profile")
            }
            String name = oAuth2User.attributes.get(NAME)
            if (!name) {
                throw new SkillsAuthorizationException("Name must be available in your public GitLab profile")
            }
            String firstName = name?.tokenize()?.first()
            List tokens = name?.tokenize()
            tokens?.pop()
            tokens?.remove(username)
            String lastName = tokens?.join(' ')

            return new UserInfo(
                    username: "${username}-${providerId}",
                    email:email,
                    firstName: firstName,
                    lastName: lastName,
            )
        }
    }

    static class Auth0UserConverter implements OAuth2UserConverter {
        static final String USERNAME = 'nickname'
        static final String NAME = 'name'
        static final String EMAIL = 'email'

        String providerId = 'auth0'

        @Override
        UserInfo convert(String providerId, OAuth2User oAuth2User) {
            String username = oAuth2User.attributes.get(USERNAME)
            assert username, "Error getting name attribute of oAuth2User [${oAuth2User}] from providerId [$providerId]"
            String email =  oAuth2User.attributes.get(EMAIL)
            if (!email) {
                throw new SkillsAuthorizationException("Email must be available in your public GitLab profile")
            }
            String name = oAuth2User.attributes.get(NAME)
            if (!name) {
                throw new SkillsAuthorizationException("Name must be available in your public GitLab profile")
            }
            String firstName = name?.tokenize()?.first()
            List tokens = name?.tokenize()
            tokens?.pop()
            tokens?.remove(username)
            String lastName = tokens?.join(' ')

            return new UserInfo(
                    username: "${username}-${providerId}",
                    email:email,
                    firstName: firstName,
                    lastName: lastName,
            )
        }
    }

    static class HydraUserConverter implements OAuth2UserConverter {
        String providerId = 'hydra'
        @Override
        UserInfo convert(String providerId, OAuth2User oAuth2User) {
            String email = oAuth2User.getName()
            List tokens = email.tokenize('@')
            String username = tokens.first()
            String firstName = username;
            String lastName = StringUtils.substringBeforeLast(tokens.last(), '.')
            assert email, "Error getting email attribute of oAuth2User [${oAuth2User}] from providerId [$providerId]"
            assert username, "Error getting username attribute of oAuth2User [${oAuth2User}] from providerId [$providerId]"
            assert firstName, "Error getting firstName attribute of oAuth2User [${oAuth2User}] from providerId [$providerId]"
            assert lastName, "Error getting lastName attribute of oAuth2User [${oAuth2User}] from providerId [$providerId]"

            return new UserInfo(
                    username: "${username}-${providerId}",
                    email:email,
                    firstName: firstName,
                    lastName: lastName,
            )
        }
    }
}
