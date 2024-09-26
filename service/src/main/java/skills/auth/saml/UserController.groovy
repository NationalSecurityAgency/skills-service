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
package skills.auth.saml

import groovy.util.logging.Slf4j
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.web.bind.annotation.*
import skills.auth.SecurityMode
import skills.auth.UserAuthService
import skills.controller.exceptions.SkillsValidator

@Conditional(SecurityMode.SAML2Auth)
@RestController
@RequestMapping("/")
@Slf4j
class UserController {

    @Autowired
    UserAuthService userAuthService

    @GetMapping('userExists/{user}')
    boolean userExists(@PathVariable('user') String user) {
        return userAuthService.userExists(user)
    }

    @RequestMapping(value = "/grantFirstRoot", method = [RequestMethod.POST, RequestMethod.PUT])
    void grantFirstRoot(HttpServletRequest request) {
        SkillsValidator.isTrue(!userAuthService.rootExists(), 'A root user already exists! Granting additional root privileges requires a root user to grant them!')
        SkillsValidator.isNotNull(request.getUserPrincipal(), 'Granting the first root user is only available in SAML & PKI modes, but it looks like the request was not made by an authenticated account!')
        userAuthService.grantRoot(request.getUserPrincipal().name)
    }
}