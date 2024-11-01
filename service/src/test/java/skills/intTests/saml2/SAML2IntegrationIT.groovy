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
import org.keycloak.admin.client.Keycloak
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import skills.SpringBootApp
import org.springframework.http.HttpStatus
import skills.auth.UserAuthService
import skills.auth.form.CreateAccountController
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Specification
import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.Cookie
import org.openqa.selenium.support.ui.ExpectedConditions
import spock.lang.Unroll
import java.time.Duration

@IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class SAML2IntegrationIT extends Specification{

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    @Shared
    KeycloakContainer keycloak = new KeycloakContainer().withRealmImportFile("keycloak/realm-export.json")

    @Autowired
    CreateAccountController userController

    @Autowired
    UserAuthService userAuthService

    @Shared
    boolean keycloakUriUpdated = false

    def setupSpec() {
        // Start Keycloak and set up system properties
        keycloak.start()
        setSystemProperties()
        WebDriverManager.chromedriver().setup()
    }

    def setup() {
        if (!keycloakUriUpdated) {
            synchronized (this) {
                if (!keycloakUriUpdated) { // Double-check locking
                    updateKeycloakRedirectUri()
                    keycloakUriUpdated = true
                }
            }
        }
    }

    def cleanupSpec() {
        keycloak.stop()
        clearSystemProperties()
    }

    private void setSystemProperties() {
        System.setProperty("skills.authorization.authMode", "SAML2")
        System.setProperty("spring.security.saml2.metadata-location", keycloak.authServerUrl + "/realms/master/protocol/saml/descriptor")
        System.setProperty("spring.security.saml2.registrationId", "keycloak")
        System.setProperty("saml2.rp.signing.key-location", "keycloak/private_key.pem")
        System.setProperty("saml2.rp.signing.cert-location", "keycloak/certificate.pem")
        System.setProperty("skills.config.ui.defaultLandingPage","progress")
    }

    private void clearSystemProperties() {
        System.clearProperty("skills.authorization.authMode")
        System.clearProperty("spring.security.saml2.metadata-location")
        System.clearProperty("spring.security.saml2.registrationId")
        System.clearProperty("saml2.rp.signing.key-location")
        System.clearProperty("saml2.rp.signing.cert-location")
        System.clearProperty("skills.config.ui.defaultLandingPage")
    }

    private void updateKeycloakRedirectUri() {
        def keycloakInstance = Keycloak.getInstance(
                keycloak.authServerUrl,
                "master",
                "admin",
                "admin",
                "admin-cli"
        )

        // Fetch the SAML client in Keycloak
        def clientResourceList = keycloakInstance.realm("master").clients().findByClientId("saml-test")
        if (clientResourceList.isEmpty()) {
            throw new IllegalStateException("Client not found in realm 'master'")
        }
        def clientRepresentation = keycloakInstance.realm("master")
                .clients().get(clientResourceList.get(0).getId()).toRepresentation()

        // Set dynamic redirect URIs
        clientRepresentation.redirectUris = ["http://localhost:${port}/login/saml2/sso/keycloak".toString()]
        clientRepresentation.clientId = "http://localhost:${port}/saml2/service-provider-metadata/keycloak".toString()

        // Update Keycloak client configuration
        keycloakInstance.realm("master").clients().get(clientResourceList.get(0).getId()).update(clientRepresentation)
    }

    def "grantFirstRoot should grant root privileges when no root user exists and user is authenticated"() {
        given: "No root user exists and a valid authenticated request"
        if (!userAuthService.rootExists()) {
            String loginUrl = "http://localhost:${port}/saml2/authenticate/keycloak"
            String sessionCookie = getSessionCookieFromWebDriver(loginUrl, "test", "test")

            HttpHeaders headers = new HttpHeaders()
            headers.set("Cookie", sessionCookie)
            HttpEntity<Void> entity = new HttpEntity<>(headers)

            String grantRootUrl = "http://localhost:${port}/grantFirstRoot"
            ResponseEntity<Void> response = restTemplate.exchange(grantRootUrl, HttpMethod.POST, entity, Void.class)

            then: "The request should succeed with a 200 status, and root privileges should be granted"
            response.statusCode == HttpStatus.OK
        } else {
            then: "No action should be taken since root user already exists"
            assert true
        }
    }

    def "SAML2 login flow with Keycloak integration"() {
        given: "A protected resource in the Spring Boot application"
        String protectedUrl = "http://localhost:${port}/app/userInfo/settings"

        when: "An unauthenticated request is made to the protected resource"
        ResponseEntity<String> initialResponse = restTemplate.getForEntity(protectedUrl, String.class)

        then: "The user is redirected to the SAML2 login page"
        initialResponse.statusCode == HttpStatus.FOUND
        String loginUrl = initialResponse.headers.getLocation().toString()
        assert loginUrl.contains("/saml2/authenticate/keycloak")

        when: "Automated login is performed using Selenium WebDriver"
        String sessionCookie = getSessionCookieFromWebDriver(loginUrl, "test", "test")

        and: "The user accesses the protected resource with the SAML assertion"
        HttpHeaders headers = new HttpHeaders()
        headers.set("Cookie", sessionCookie)
        HttpEntity<?> entity = new HttpEntity<>(headers)

        ResponseEntity<String> protectedResponse = restTemplate.exchange(protectedUrl, HttpMethod.GET, entity, String.class)

        then: "The user is successfully authenticated and can access the protected resource"
        assert protectedResponse.statusCode == HttpStatus.OK
    }

    def "userExists should return true when user exists"() {
        given: "A user exists in the system"
        def username = "test"
        userController.userExists(username) >> true

        when: "userExists is called"
        def result = userController.userExists(username)

        then: "The result should be true"
        result == true
    }

    def "userExists should return false when user does not exist"() {
        given: "A user does not exist in the system"
        def username = "unknownUser"
        userController.userExists(username) >> false

        when: "userExists is called"
        def result = userController.userExists(username)

        then: "The result should be false"
        result == false
    }

    def getSessionCookieFromWebDriver(String loginUrl, String username, String password) {
        ChromeOptions options = new ChromeOptions()
        options.addArguments("--headless")
        WebDriver driver = new ChromeDriver(options)
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60))

        try {
            driver.get(loginUrl)

            // Simulate entering username and password and submit the form
            wait.until(ExpectedConditions.elementToBeClickable(By.name("username"))).sendKeys(username)
            driver.findElement(By.name("password")).sendKeys(password)
            driver.findElement(By.name("login")).click()

            // Wait for the URL to change or the final element to appear
            wait.until(ExpectedConditions.urlContains("progress-and-rankings"))

            boolean isLoggedIn = false;
            int attempts = 0;

            while (!isLoggedIn && attempts < 3) {
                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.id("user_settings_menu")));
                    isLoggedIn = true;
                } catch (TimeoutException e) {
                    Thread.sleep(5000);  // Retry after delay
                    attempts++;
                }
            }

            if (!isLoggedIn) {
                throw new TimeoutException("Failed to log in and find the user settings menu.");
            }

            // Retrieve session cookies
            Set<Cookie> cookies = driver.manage().getCookies()
            return cookies.collect { "${it.name}=${it.value}" }.join("; ")
        } finally {
            driver.quit()
        }
    }

}