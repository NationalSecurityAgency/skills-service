/**
 * Copyright 2026 SkillTree
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

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import skills.auth.UserAuthService
import skills.auth.UserInfo
import spock.lang.Specification

class SAML2UtilsSpec extends Specification {

    SAML2Utils utils = new SAML2Utils(userAuthService: Mock(UserAuthService))

    private Saml2Authentication authFor(String username, Map<String, String> attrs) {
        Map<String, List<Object>> attributes = attrs.collectEntries { k, v -> [(k): [v] as List<Object>] }
        def principal = new DefaultSaml2AuthenticatedPrincipal(username, attributes)
        return new Saml2Authentication(principal, "<saml/>", [new SimpleGrantedAuthority("ROLE_USER")])
    }

    def "display name is the first and last name when the IdP sends both"() {
        when:
        UserInfo res = utils.convertToUserInfo(authFor("jdoe@example.org", [firstName: "Jane", lastName: "Doe", email: "jdoe@example.org"]), false)

        then:
        res.username == "jdoe@example.org"
        res.usernameForDisplay == "Jane Doe"
        res.firstName == "Jane"
        res.lastName == "Doe"
    }

    def "display name must not contain the text 'null' when the IdP omits name attributes - #attrs"() {
        when:
        UserInfo res = utils.convertToUserInfo(authFor("jdoe@example.org", attrs), false)

        then:
        res.usernameForDisplay == expected

        where:
        attrs                                 | expected
        [email: "jdoe@example.org"]           | "jdoe@example.org"
        [firstName: "Jane"]                   | "Jane"
        [lastName: "Doe"]                     | "Doe"
        [firstName: "  ", lastName: "Doe"]   | "Doe"
    }
}
