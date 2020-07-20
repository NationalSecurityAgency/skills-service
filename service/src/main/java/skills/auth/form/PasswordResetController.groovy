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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Lazy
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import skills.PublicProps
import skills.auth.SecurityMode
import skills.auth.UserAuthService
import skills.auth.UserInfo
import skills.controller.PublicPropsBasedValidator
import skills.controller.exceptions.SkillException
import skills.controller.result.model.RequestResult
import skills.services.PasswordResetService
import skills.storage.model.auth.PasswordResetToken
import skills.storage.model.auth.User

import javax.annotation.PostConstruct

@Conditional(SecurityMode.FormAuth)
@Slf4j
@RestController("/")
class PasswordResetController {

    @Autowired
    PasswordResetService resetService

    @Autowired
    UserAuthService userAuthService

    @Autowired
    PublicPropsBasedValidator propsBasedValidator

    @Autowired
    PasswordEncoder passwordEncoder

    @PostMapping("resetPassword")
    RequestResult requestPasswordReset(@RequestPart("userId") String userId) {
        log.info("requesting password reset for [${userId}]")
        User user = userAuthService.getUserRepository().findByUserId(userId)
        if (!user) {
            log.error("no user found for requested password reset")
            throw new SkillException("No user found for id [${userId}]")
        }
        resetService.createTokenAndNotifyUser(user)
        return RequestResult.success()
    }

    @PostMapping("performPasswordReset")
    RequestResult resetPassword(@RequestBody PasswordReset reset) {
        PasswordResetToken token = resetService.loadToken(reset.resetToken)
        if (token?.getUser()?.getUserId() != reset.userId) {
            throw new SkillException("Supplied reset token is not for the specified user")
        }

        if (!token.isValid()) {
            throw new SkillException("Reset token has expired")
        }

        log.info("reseting password for user [${reset.userId}]")
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minPasswordLength, "new_password", reset.password)
        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxPasswordLength, "new_password", reset.password)

        UserInfo userInfo = userAuthService.loadByUserId(token.user.userId)
        userInfo.password = passwordEncoder.encode(reset.password)
        userAuthService.createOrUpdateUser(userInfo)
        resetService.deleteToken(token.token)

        return RequestResult.success()
    }
}
