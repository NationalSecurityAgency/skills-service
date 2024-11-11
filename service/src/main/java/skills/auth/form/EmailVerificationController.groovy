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
package skills.auth.form

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.web.bind.annotation.*
import skills.auth.SecurityMode
import skills.auth.UserAuthService
import skills.auth.UserInfo
import skills.controller.PublicPropsBasedValidator
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.result.model.RequestResult
import skills.services.PasswordManagementService
import skills.services.UserAttrsService
import skills.storage.model.UserAttrs
import skills.storage.model.auth.UserToken

import static skills.services.PasswordManagementService.VERIFY_EMAIL_TOKEN_TYPE

@Conditional(SecurityMode.FormAuth)
@Slf4j
@RestController()
@RequestMapping("/")
class EmailVerificationController {

    @Autowired
    PasswordManagementService passwordMangementService

    @Autowired
    UserAuthService userAuthService

    @Autowired
    UserAttrsService userAttrsService

    @GetMapping('userEmailIsVerified/{email}')
    boolean userEmailIsVerified(@PathVariable('email') String email) {
        UserAttrs userAttrs = userAttrsService.findByUserId(email)
        return Boolean.valueOf(userAttrs?.emailVerified)
    }

    // used to send a secondary email after the initial account creation email verification was sent
    @PostMapping("resendEmailVerification/{userId}")
    RequestResult resendEmailVerification(@PathVariable("userId") String userId) {
        if (!userAuthService.userExists(userId)) {
            throw new SkillException("No user account exists for the specified userId [${userId}]")
        }
        passwordMangementService.createEmailVerificationTokenAndNotifyUser(userId)
       return RequestResult.success()
    }

    @PostMapping("verifyEmail")
    RequestResult verifyEmail(@RequestBody EmailVerification emailVerification) {
        if (StringUtils.isBlank(emailVerification.token)) {
            throw new SkillException("The supplied email verification token is blank.")
        }
        if (StringUtils.isBlank(emailVerification.email)) {
            throw new SkillException("The supplied email is blank.")
        }
        UserToken token = passwordMangementService.loadToken(emailVerification.token)
        if (token?.getUser()?.getUserId() != emailVerification.email) {
            throw new SkillException("The supplied email verification token does not exist or is not for the specified user.")
        }

        if (token.type != VERIFY_EMAIL_TOKEN_TYPE) {
            throw new SkillException("The supplied email verification token has invalid token type [${token.type}].")
        }

        if (!token.isValid()) {
            throw new SkillException("Your email verification code has expired.", ErrorCode.UserTokenExpired)
        }

        UserInfo userInfo = userAuthService.loadByUserId(token.user.userId)
        userInfo.emailVerified = true
        userAuthService.createOrUpdateUser(userInfo)
        passwordMangementService.deleteToken(token.token)

        log.info("email verified for user [${userInfo.username}]")
        return RequestResult.success()
    }
}
