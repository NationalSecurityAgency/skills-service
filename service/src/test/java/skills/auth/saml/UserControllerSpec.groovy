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
package skills.auth.saml
import org.springframework.mock.web.MockHttpServletRequest
import skills.auth.UserAuthService
import skills.auth.form.CreateAccountController
import spock.lang.Specification
import spock.lang.Unroll
import java.security.Principal

class UserControllerSpec extends Specification {

    UserAuthService userAuthService = Mock()
    CreateAccountController userController = new CreateAccountController(userAuthService : userAuthService)

    def "userExists should return true when user exists"() {
        given: "A user exists in the system"
        def username = "testUser"
        userAuthService.userExists(username) >> true

        when: "userExists is called"
        def result = userController.userExists(username)

        then: "The result should be true"
        result == true
    }

    def "userExists should return false when user does not exist"() {
        given: "A user does not exist in the system"
        def username = "unknownUser"
        userAuthService.userExists(username) >> false

        when: "userExists is called"
        def result = userController.userExists(username)

        then: "The result should be false"
        result == false
    }

    @Unroll
    def "grantFirstRoot should grant root privileges when no root user exists and user is authenticated"() {
        given: "No root user exists and a valid authenticated request"
        def principal = Mock(Principal)
        def mockRequest = new MockHttpServletRequest()
        mockRequest.setUserPrincipal(principal)
        principal.name >> username
        userAuthService.rootExists() >> false

        when: "grantFirstRoot is called"
        userController.grantFirstRoot(mockRequest)

        then: "grantRoot should be called with the authenticated username"
        1 * userAuthService.grantRoot(username)

        where:
        username = "authUser"
    }

    def "grantFirstRoot should throw an exception when root user already exists"() {
        given: "A root user already exists"
        def mockRequest = new MockHttpServletRequest()
        userAuthService.rootExists() >> true

        when: "grantFirstRoot is called"
        userController.grantFirstRoot(mockRequest)

        then: "An exception should be thrown"
        def e = thrown(skills.controller.exceptions.SkillException)
        e.message == 'A root user already exists! Granting additional root privileges requires a root user to grant them!'
        0 * userAuthService.grantRoot(_)
    }

    def "grantFirstRoot should throw an exception when request is not authenticated"() {
        given: "No authenticated user"
        def mockRequest = new MockHttpServletRequest()
        userAuthService.rootExists() >> false

        when: "grantFirstRoot is called"
        userController.grantFirstRoot(mockRequest)

        then: "An exception should be thrown"
        def e = thrown(skills.controller.exceptions.SkillException)
        e.message == 'Granting the first root user is only available in SAML & PKI modes, but it looks like the request was not made by an authenticated account! was not provided.'
        0 * userAuthService.grantRoot(_)
    }
}
