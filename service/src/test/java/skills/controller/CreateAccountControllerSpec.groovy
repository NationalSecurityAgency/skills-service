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
package skills.controller

import skills.auth.AuthMode
import skills.auth.UserInfo
import skills.auth.form.CreateAccountController
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import spock.lang.Specification

import javax.servlet.http.HttpServletResponse

class CreateAccountControllerSpec extends Specification {

    def "in oAuthOnly mode, throws SkillException if createAccount is called"() {

        when:
        CreateAccountController createAccountController = new CreateAccountController(oAuthOnly: true)

        UserInfo userInfo = Mock()
        HttpServletResponse httpServletResponse = Mock()
        createAccountController.createAppUser(userInfo, httpServletResponse)

        then:
        SkillException e = thrown(SkillException)
        e.getMessage() == "Username/Password account creation is disabled for this installation of the SkillTree"
        e.errorCode == ErrorCode.AccessDenied
    }

    def "in PKI mode, throws SkillException if createAccount is called"() {

        when:
        CreateAccountController createAccountController = new CreateAccountController(authMode: AuthMode.PKI)

        UserInfo userInfo = Mock()
        HttpServletResponse httpServletResponse = Mock()
        createAccountController.createAppUser(userInfo, httpServletResponse)

        then:
        SkillException e = thrown(SkillException)
        e.getMessage() == "Username/Password account creation is disabled for this installation of the SkillTree"
        e.errorCode == ErrorCode.AccessDenied
    }

}
