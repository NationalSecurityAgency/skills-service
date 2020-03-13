/*
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

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.client.HttpClientErrorException
import skills.auth.pki.PkiUserLookup
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.services.UserAttrsService
import spock.lang.Specification

class UserInfoServiceSpec extends Specification {

    def "Get id of current user"( ){
        SecurityContext context = Mock()
        Authentication authentication = Mock()
        UserInfo userInfo = Mock()
        SecurityContextHolder.setContext(context)

        1 * context.getAuthentication() >> authentication
        1 * authentication.getPrincipal() >> userInfo
        1 * userInfo.getUsername() >> "foo"

        when:
        UserInfoService userInfoService = new UserInfoService()
        String userId = userInfoService.getCurrentUserId()

        then:
        userId == "foo"
    }

    def "in pki auth mode, do not look up user by dn if userId is supplied with a type of ID" (){
        PkiUserLookup userLookup = Mock()
        0 * userLookup.lookupUserDn("simpleId")

        when:
        UserInfoService userInfoService = new UserInfoService()
        userInfoService.authMode = AuthMode.PKI
        userInfoService.pkiUserLookup = userLookup
        String userId = userInfoService.getUserName("simpleId", true, "ID")

        then:
        userId == "simpleid"
    }

    def "in pki auth mode, look up user by dn if no id type is specified"() {
        UserInfo userInfo = Mock()
        PkiUserLookup userLookup = Mock()
        UserAuthService authService = Mock()
        1 * userLookup.lookupUserDn("fakeDn") >> userInfo
        1 * authService.createOrUpdateUser(userInfo)
        1 * userInfo.getUsername() >> "simpleId"

        when:
        UserInfoService userInfoService = new UserInfoService()
        userInfoService.authMode = AuthMode.PKI
        userInfoService.pkiUserLookup = userLookup
        userInfoService.userAuthService = authService
        String userId = userInfoService.getUserName("fakeDn", true)

        then:
        userId == "simpleid"
    }

    def "in pki auth mode, look up user by dn if id type of DN is specified"() {
        UserInfo userInfo = Mock()
        PkiUserLookup userLookup = Mock()
        UserAuthService authService = Mock()
        1 * userLookup.lookupUserDn("fakeDn") >> userInfo
        1 * authService.createOrUpdateUser(userInfo)
        1 * userInfo.getUsername() >> "simpleId"

        when:
        UserInfoService userInfoService = new UserInfoService()
        userInfoService.authMode = AuthMode.PKI
        userInfoService.pkiUserLookup = userLookup
        userInfoService.userAuthService = authService
        String userId = userInfoService.getUserName("fakeDn", true, "DN")

        then:
        userId == "simpleid"
    }

    def "in pki auth mode, throws SkillException with UserNotFound code if error occurs on lookup service"() {
        PkiUserLookup userLookup = Mock()
        HttpClientErrorException exception = Mock()
        1 * exception.getResponseBodyAsString() >> "Lookup Exception"
        1 * userLookup.lookupUserDn("fakeDn") >> { throw exception }

        when:
        UserInfoService userInfoService = new UserInfoService()
        userInfoService.authMode = AuthMode.PKI
        userInfoService.pkiUserLookup = userLookup

        userInfoService.getUserName("fakeDn", false)

        then:
        SkillException e = thrown(SkillException)
        e.doNotRetry
        e.getMessage() == "Lookup Exception"
        e.errorCode == ErrorCode.UserNotFound
    }

    def "in pki auth mode, throws SkillException with UserNotFound code if error occurs on lookup service - alternate"() {
        PkiUserLookup userLookup = Mock()
        Exception mockRootException = Mock()
        HttpClientErrorException exception = Mock()
        1 * exception.getResponseBodyAsString() >> "Lookup Exception"
        1 * userLookup.lookupUserDn("fakeDn") >> { throw mockRootException }
        mockRootException.getCause() >> exception

        when:
        UserInfoService userInfoService = new UserInfoService()
        userInfoService.authMode = AuthMode.PKI
        userInfoService.pkiUserLookup = userLookup

        userInfoService.getUserName("fakeDn", false)

        then:
        SkillException e = thrown(SkillException)
        e.doNotRetry
        e.getMessage() == "Lookup Exception"
        e.errorCode == ErrorCode.UserNotFound
    }

    def "in pki auth mode, throws SkillException if lookup returns no result"(){
        UserInfo userInfo = Mock()
        PkiUserLookup userLookup = Mock()
        UserAuthService authService = Mock()
        4 * userLookup.lookupUserDn("fakeDn") >> null
        0 * authService.createOrUpdateUser(userInfo)
        0 * userInfo.getUsername()

        when:
        UserInfoService userInfoService = new UserInfoService()
        userInfoService.authMode = AuthMode.PKI
        userInfoService.pkiUserLookup = userLookup
        userInfoService.userAuthService = authService
        userInfoService.getUserName("fakeDn", true)

        then:
        thrown(SkillException)
    }

    def "in form mode, returns user id when userIdParam is supplied"() {
        UserAttrsService userAttrsService = Mock()
        1 * userAttrsService.saveUserAttrs("simpleId", _ as UserInfo)

        when:
        UserInfoService userInfoService = new UserInfoService()
        userInfoService.authMode = AuthMode.FORM
        userInfoService.userAttrsService = userAttrsService

        String userId = userInfoService.getUserName("simpleId")

        then:
        userId == "simpleid"
    }

    def "in form mode, returns user id of current user if no userIdParam is supplied"() {
        UserAttrsService userAttrsService = Mock()
        SecurityContext context = Mock()
        Authentication authentication = Mock()
        UserInfo userInfo = Mock()
        SecurityContextHolder.setContext(context)

        1 * context.getAuthentication() >> authentication
        1 * authentication.getPrincipal() >> userInfo
        1 * userInfo.getUsername() >> "simpleId"
        1 * userAttrsService.saveUserAttrs("simpleId", _ as UserInfo)

        when:
        UserInfoService userInfoService = new UserInfoService()
        userInfoService.authMode = AuthMode.FORM
        userInfoService.userAttrsService = userAttrsService

        String userId = userInfoService.getUserName(null)

        then:
        userId == "simpleid"
    }

}
