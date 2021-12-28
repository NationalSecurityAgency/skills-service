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
package skills.services

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.User
import skills.storage.model.auth.UserToken
import skills.storage.repos.PasswordResetTokenRepo
import skills.storage.repos.UserRepo
import spock.lang.IgnoreIf

import static skills.services.PasswordManagementService.VERIFY_EMAIL_TOKEN_TYPE

class ScheduledUserTokenCleanupSpec extends DefaultIntSpec {

    @Autowired
    PasswordResetTokenRepo tokenRepo

    @Autowired
    UserRepo userRepo

    @Autowired(required = false)
    ScheduledUserTokenCleanup cleanup

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "test ScheduledUserTokenCleanup"() {

        SkillsService createAcctService = createService()
        createAcctService.createUser([firstName: "John", lastName: "Doe", email: "jdoe@email.foo", password: "password"])
        createAcctService.createUser([firstName: "John", lastName: "Doe", email: "jdoe2@email.foo", password: "password"])
        User user = userRepo.findByUserId('jdoe@email.foo')
        String tokenValue1 = 'xyz123'
        UserToken expiredToken = new UserToken(user: user, token: tokenValue1, type: VERIFY_EMAIL_TOKEN_TYPE, expires: new Date()-15)
        tokenRepo.save(expiredToken)

        String tokenValue2 = 'abc456'
        UserToken nonexpiredToken = new UserToken(user: user, token: tokenValue2, type: VERIFY_EMAIL_TOKEN_TYPE, expires: new Date()-5)
        tokenRepo.save(nonexpiredToken)
        UserToken expiredTokenBeforeCleanup = tokenRepo.findByToken(tokenValue1)

        when:
        cleanup.cleanupExpiredTokens()
        UserToken expiredTokenAfterCleanup = tokenRepo.findByToken(tokenValue1)
        UserToken nonExpiredtokenAfterCleanup = tokenRepo.findByToken(tokenValue2)

        then:
        expiredTokenBeforeCleanup
        !expiredTokenAfterCleanup
        nonExpiredtokenAfterCleanup
    }
}
