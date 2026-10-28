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
package skills.utils

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestContextManager
import org.springframework.test.context.TestPropertySource
import skills.intTests.utils.CertificateRegistry
import skills.intTests.utils.MockUserInfoService
import skills.storage.repos.UserAttrsRepo
import spock.lang.Specification

import static org.mockito.Mockito.mock

class MockUserInfoServiceSpecs extends Specification {

    def "distinct Spring contexts use independent PKI mock ports and close their own servers"() {
        TestContextManager firstManager = new TestContextManager(FirstContext)
        TestContextManager secondManager = new TestContextManager(SecondContext)
        FirstContext first = new FirstContext()
        SecondContext second = new SecondContext()

        when:
        firstManager.prepareTestInstance(first)
        secondManager.prepareTestInstance(second)

        then:
        first.mockUserInfoService.mockServer.isRunning()
        second.mockUserInfoService.mockServer.isRunning()
        int firstPort = first.mockUserInfoService.mockServer.httpsPort()
        int secondPort = second.mockUserInfoService.mockServer.httpsPort()
        firstPort > 0
        secondPort > 0
        firstPort != secondPort
        first.endpoints.userInfoUri == "https://localhost:${firstPort}/userInfo?dn={dn}"
        first.endpoints.userQueryUri == "https://localhost:${firstPort}/userQuery?query={query}"
        first.endpoints.healthCheckUri == "https://localhost:${firstPort}/status"
        second.endpoints.userInfoUri == "https://localhost:${secondPort}/userInfo?dn={dn}"
        second.endpoints.userQueryUri == "https://localhost:${secondPort}/userQuery?query={query}"
        second.endpoints.healthCheckUri == "https://localhost:${secondPort}/status"

        when:
        firstManager.afterTestClass()

        then:
        !first.mockUserInfoService.mockServer.isRunning()
        second.mockUserInfoService.mockServer.isRunning()

        when:
        secondManager.afterTestClass()

        then:
        !second.mockUserInfoService.mockServer.isRunning()

        cleanup:
        firstManager.afterTestClass()
        secondManager.afterTestClass()
    }

    @ContextConfiguration(classes = MockConfiguration)
    @TestPropertySource(properties = [
            'skills.authorization.authMode=PKI',
            // Stale fixed URLs must be overridden before endpoint consumers are initialized.
            'skills.authorization.userInfoUri=https://localhost:8181/userInfo?dn={dn}',
            'skills.authorization.userQueryUri=https://localhost:8181/userQuery?query={query}',
            'skills.authorization.userInfoHealthCheckUri=https://localhost:8181/status',
    ])
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    static class FirstContext {
        @Autowired
        MockUserInfoService mockUserInfoService

        @Autowired
        EndpointConsumer endpoints
    }

    @TestPropertySource(properties = 'test.context.id=second')
    static class SecondContext extends FirstContext {}

    @TestConfiguration(proxyBeanMethods = false)
    @Import([MockUserInfoService, CertificateRegistry])
    static class MockConfiguration {
        @Bean
        UserAttrsRepo userAttrsRepo() {
            return mock(UserAttrsRepo)
        }

        @Bean
        EndpointConsumer endpointConsumer() {
            return new EndpointConsumer()
        }
    }

    static class EndpointConsumer {
        @Value('${skills.authorization.userInfoUri}')
        String userInfoUri

        @Value('${skills.authorization.userQueryUri}')
        String userQueryUri

        @Value('${skills.authorization.userInfoHealthCheckUri}')
        String healthCheckUri
    }
}
