/**
 * Copyright 2024 SkillTree
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
import groovy.json.JsonSlurper
import org.openqa.selenium.WebDriver
import org.openqa.selenium.By
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import skills.SpringBootApp
import org.springframework.http.HttpStatus
import skills.auth.UserAuthService
import skills.auth.form.CreateAccountController
import skills.intTests.utils.DataResetHelper
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Specification
import org.openqa.selenium.Cookie
import java.time.Duration

@IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT, classes = SpringBootApp,
        properties = [
                'server.port=10100',
                'skills.authorization.authMode=SAML2',
                'spring.security.saml2.registrationId=keycloak',
                'saml2.rp.signing.key-location=keycloak/fake_test_key.pem',
                'saml2.rp.signing.cert-location=keycloak/fake_test_certificate.pem',
                'spring.security.saml2.metadata-location=http://localhost:10101/realms/master/protocol/saml/descriptor'
        ])
class SAML2IntegrationIT extends Specification{

    @Shared
    int port = 10100

    @Shared
    int keycloakPort = 10101

    @Autowired
    TestRestTemplate restTemplate

    @Shared
    KeycloakContainer keycloak = new KeycloakContainer().withRealmImportFile("keycloak/realm-export.json")

    @Autowired
    CreateAccountController userController

    @Autowired
    UserAuthService userAuthService

    @Shared
    WebDriver driver

    @Shared
    String skillServiceUrl

    @Autowired
    DataResetHelper dataResetHelper

    JsonSlurper jsonSlurper = new JsonSlurper()

    def setupSpec() {

        skillServiceUrl = "http://localhost:${port}".toString()
        // Start Keycloak and set up system properties

        keycloak.setPortBindings(["${keycloakPort}:8080".toString()])
        keycloak.start()
        driver = new HtmlUnitDriver(true)

    }

    def setup() {
        dataResetHelper.resetData()
    }

    def cleanupSpec() {
        keycloak.stop()
        if (driver != null) {
            driver.quit()
        }
    }


    def "SAML2 login flow with Keycloak integration"() {
        given: "A protected resource in the Spring Boot application"
        String protectedUrl = "${skillServiceUrl}/app/userInfo/settings"

        when: "An unauthenticated request is made to the protected resource"
        ResponseEntity<String> initialResponse = restTemplate.getForEntity(protectedUrl, String.class)

        then: "The user is redirected to the SAML2 login page"
        initialResponse.statusCode == HttpStatus.FOUND
        String loginUrl = initialResponse.headers.getLocation().toString()
        assert loginUrl.contains("/saml2/authenticate?registrationId=keycloak")

        when: "Automated login is performed using Selenium WebDriver"
        String sessionCookie = getSessionCookieFromWebDriver(loginUrl, "test", "test")

        and: "The user accesses the protected resource with the SAML assertion"
        HttpHeaders headers = new HttpHeaders()
        headers.set("Cookie", sessionCookie)
        HttpEntity<?> entity = new HttpEntity<>(headers)

        ResponseEntity<String> protectedResponse = restTemplate.exchange(protectedUrl, HttpMethod.GET, entity, String.class)

        then: "The user is successfully authenticated and can access the protected resource"
        assert protectedResponse.statusCode == HttpStatus.OK
        restTemplate.exchange("${skillServiceUrl}/userExists/test", HttpMethod.GET, entity, String.class).body == "true"
        restTemplate.exchange("${skillServiceUrl}/userExists/unknownUser", HttpMethod.GET, entity, String.class).body == "false"
        jsonSlurper.parseText(restTemplate.exchange("${skillServiceUrl}/app/userInfo", HttpMethod.GET, entity, String.class).body).userId == "test"
    }

    def getSessionCookieFromWebDriver(String loginUrl, String username, String password) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60))
        driver.get(loginUrl)
        driver.findElement (By.name("username")).sendKeys(username)
        driver.findElement(By.name("password")).sendKeys(password)
        driver.findElement(By.name("login")).click()
        // Retrieve session cookies
        Set<Cookie> cookies = driver.manage().getCookies()
        return cookies.collect { "${it.name}=${it.value}" }.join("; ")

    }

}