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
package skills.intTests.saml2

import dasniko.testcontainers.keycloak.KeycloakContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.ResponseEntity
import skills.SpringBootApp
import spock.lang.Shared
import spock.lang.Specification


@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class SAML2IntegrationIT extends Specification{

    @LocalServerPort
    int port

    @Autowired
    private TestRestTemplate restTemplate

    @Shared
    KeycloakContainer keycloak = new KeycloakContainer().withRealmImportFile("keycloak/realm-export.json");


    def setupSpec() {
        keycloak.start()

        // Set the SAML IDP metadata for the Keycloak instance
        System.setProperty("skills.authorization.authMode","SAML2");
        System.setProperty("spring.security.saml2.metadata-location",keycloak.authServerUrl + "/realms/saml-test/protocol/saml/descriptor");
        System.setProperty("spring.security.saml2.registrationId","keycloak");

    }

    def cleanupSpec() {
        keycloak.stop()
    }

    def "test SAML2 login redirects to Keycloak"() {

        when: "A request is made to the /app/userInfo/settings endpoint"
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:${port}/app/userInfo/settings", String.class)

        then: "The status is a 3xx redirection"
        response.statusCode == HttpStatus.FOUND

        and: "The Location header redirects to the Keycloak SAML2 login page"
        response.headers.getLocation().toString() == "http://localhost:${port}/saml2/authenticate/keycloak"

    }

}