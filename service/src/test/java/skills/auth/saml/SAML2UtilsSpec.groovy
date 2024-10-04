package skills.auth.saml

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import skills.auth.UserAuthService
import spock.lang.Specification
import spock.lang.Unroll
import skills.auth.UserInfo
import skills.auth.SkillsAuthorizationException

class SAML2UtilsSpec extends Specification {

    UserAuthService userAuthService = Mock()
    SAML2Utils saml2Utils = new SAML2Utils(userAuthService: userAuthService)

    def "convertToSkillsAuth should convert Saml2Authentication to Skills Authentication"() {
        given: "A valid Saml2Authentication object"
        Saml2AuthenticatedPrincipal principal = Mock()
        Saml2Authentication saml2Auth = new Saml2Authentication(principal, "saml2", null)
        principal.getName() >> "testUser"
        principal.getFirstAttribute(SAML2Utils.FIRST_NAME) >> "Test"
        principal.getFirstAttribute(SAML2Utils.LAST_NAME) >> "User"
        principal.getFirstAttribute(SAML2Utils.EMAIL) >> "test@example.com"

        when: "convertToSkillsAuth is called"
        def result = saml2Utils.convertToSkillsAuth(saml2Auth)

        then: "It should convert to a UsernamePasswordAuthenticationToken"
        result instanceof UsernamePasswordAuthenticationToken

        and: "It should call userAuthService to create or update the user"
        1 * userAuthService.createOrUpdateUser(_ as UserInfo) >> { UserInfo userInfo ->
            assert userInfo.username == "testUser"
            assert userInfo.firstName == "Test"
            assert userInfo.lastName == "User"
            assert userInfo.email == "test@example.com"
            return userInfo
        }
    }

    def "convertToSkillsAuth should throw exception when username is missing"() {
        given: "A Saml2Authentication object without a username"
        Saml2AuthenticatedPrincipal principal = Mock()
        Saml2Authentication saml2Auth = new Saml2Authentication(principal, "saml2", null)
        principal.getName() >> null  // Missing username

        when: "convertToSkillsAuth is called"
        saml2Utils.convertToSkillsAuth(saml2Auth)

        then: "It should throw a SkillsAuthorizationException"
        def e = thrown(SkillsAuthorizationException)
        e.message == "Username must be available for your SSO profile"

        and: "userAuthService should not be called"
        0 * userAuthService.createOrUpdateUser(_)
    }

    @Unroll
    def "convertToUserInfo should convert Saml2Authentication to UserInfo, persist=#persist"() {
        given: "A valid Saml2Authentication object"
        Saml2AuthenticatedPrincipal principal = Mock()
        Saml2Authentication saml2Auth = new Saml2Authentication(principal, "saml2", null)
        principal.getName() >> "testUser"
        principal.getFirstAttribute(SAML2Utils.FIRST_NAME) >> "Test"
        principal.getFirstAttribute(SAML2Utils.LAST_NAME) >> "User"
        principal.getFirstAttribute(SAML2Utils.EMAIL) >> "test@example.com"

        when: "convertToUserInfo is called with persist = #persist"
        def result = saml2Utils.convertToUserInfo(saml2Auth, persist)
        println "Persist: $persist, result: $result"

        then: "It should return a UserInfo object"
        result instanceof UserInfo
        result.username == "testUser"
        result.firstName == "Test"
        result.lastName == "User"
        result.email == "test@example.com"

        and: "It should call userAuthService.createOrUpdateUser only if persist is true"
        (persist ? 1 : 0) * userAuthService.createOrUpdateUser(_ as UserInfo) >> { UserInfo userInfo ->
            assert userInfo.username == "testUser"
            assert userInfo.firstName == "Test"
            assert userInfo.lastName == "User"
            assert userInfo.email == "test@example.com"
            return userInfo
        }

        where:
        persist << [true, false]
    }

    def "convertToUserInfo should throw exception when username is missing"() {
        given: "A Saml2Authentication object without a username"
        Saml2AuthenticatedPrincipal principal = Mock()
        Saml2Authentication saml2Auth = new Saml2Authentication(principal, "saml2", null)
        principal.getName() >> null  // Missing username

        when: "convertToUserInfo is called"
        saml2Utils.convertToUserInfo(saml2Auth, true)

        then: "It should throw a SkillsAuthorizationException"
        def e = thrown(SkillsAuthorizationException)
        e.message == "Username must be available for your SSO profile"

        and: "userAuthService should not be called"
        0 * userAuthService.createOrUpdateUser(_)
    }
}
