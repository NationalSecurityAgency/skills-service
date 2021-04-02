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

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User
import skills.auth.form.FormSecurityConfiguration
import skills.auth.form.oauth2.OAuth2UserConverterService
import spock.lang.Specification

class OAuth2UserConverterServiceSpec extends Specification {

    def "Github OAuth Converter"() {
        OAuth2UserConverterService service = new OAuth2UserConverterService()
        service.lookup = new FormSecurityConfiguration().oAuth2UserConverterMap()

        final String providerId = 'github'
        final String uuid = UUID.randomUUID().toString()
        final String userName = 'joeuser'
        final String firstName = 'Joe'
        final String lastName = 'User'
        final String fullName = "${firstName} ${lastName}"
        final String email = "${userName}@email.com"
        Map<String, Object> attributes = [
                name: fullName,
                login: userName,
                email: email,
        ]

        OAuth2User oAuth2User = createOAuth2User(uuid, attributes)
        UserInfo expected = new UserInfo(
                username: "${uuid}-${providerId}",
                usernameForDisplay: userName,
                email: email,
                firstName: firstName,
                lastName: lastName
        )

        when:
        UserInfo result = service.convert(providerId, oAuth2User)

        then:
        result == expected
    }

    def "Google OAuth Converter"() {
        OAuth2UserConverterService service = new OAuth2UserConverterService()
        service.lookup = new FormSecurityConfiguration().oAuth2UserConverterMap()

        final String providerId = 'google'
        final String uuid = UUID.randomUUID().toString()
        final String userName = 'joeuser'
        final String firstName = 'Joe'
        final String lastName = 'User'
        final String email = "${userName}@email.com"
        Map<String, Object> attributes = [
                given_name: firstName,
                family_name: lastName,
                email: email,
        ]

        OAuth2User oAuth2User = createOAuth2User(uuid, attributes)
        UserInfo expected = new UserInfo(
                username: "${uuid}-${providerId}",
                usernameForDisplay: userName,
                email: email,
                firstName: firstName,
                lastName: lastName
        )

        when:
        UserInfo result = service.convert(providerId, oAuth2User)

        then:
        result == expected
    }

    def "Gitlab OAuth Converter"() {
        OAuth2UserConverterService service = new OAuth2UserConverterService()
        service.lookup = new FormSecurityConfiguration().oAuth2UserConverterMap()

        final String providerId = 'gitlab'
        final String userName = 'joeuser'
        final String firstName = 'Joe'
        final String lastName = 'User'
        final String email = "${userName}@email.com"
        Map<String, Object> attributes = [
                name: "${firstName} ${lastName} ${userName}",
                email: email,
        ]

        OAuth2User oAuth2User = createOAuth2User(userName, attributes)
        UserInfo expected = new UserInfo(
                username: "${userName}-${providerId}",
                usernameForDisplay: userName,
                email: email,
                firstName: firstName,
                lastName: lastName
        )

        when:
        UserInfo result = service.convert(providerId, oAuth2User)

        then:
        result == expected
    }

    def "Keycloak OAuth Converter"() {
        OAuth2UserConverterService service = new OAuth2UserConverterService()
        service.lookup = new FormSecurityConfiguration().oAuth2UserConverterMap()

        final String providerId = 'keycloak'
        final String userName = 'joeuser'
        final String firstName = 'Joe'
        final String lastName = 'User'
        final String email = "${userName}@email.com"
        Map<String, Object> attributes = [
                preferred_username: userName,
                name: "${firstName} ${lastName}",
                given_name: firstName,
                family_name: lastName,
                email: email,
        ]

        OAuth2User oAuth2User = createOAuth2User(userName, attributes)
        UserInfo expected = new UserInfo(
                username: "${userName}-${providerId}",
                usernameForDisplay: userName,
                email: email,
                firstName: firstName,
                lastName: lastName
        )

        when:
        UserInfo result = service.convert(providerId, oAuth2User)

        then:
        result == expected
    }

    private OAuth2User createOAuth2User(String providerUserId, Map<String, Object> attributes) {
        return new OAuth2User() {
            @Override
            Map<String, Object> getAttributes() {
                return attributes
            }

            @Override
            Collection<? extends GrantedAuthority> getAuthorities() {
                return null
            }

            @Override
            String getName() {
                return providerUserId
            }
        }
    }
}
