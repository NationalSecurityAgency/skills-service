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

import groovy.time.TimeCategory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.intTests.utils.DefaultIntSpec
import skills.storage.model.UserAttrs
import skills.storage.model.UserTag
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserTagRepo
import spock.lang.IgnoreIf

class UserAttrsServiceSpec extends DefaultIntSpec {

    @Autowired
    UserAttrsService userAttrsService

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserTagRepo userTagRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    UserAttrsTestServiceWrapper userAttrsTestServiceWrapper

    def "do not override existing user attributes with null values"() {
        String userId = "${UserAttrsServiceSpec.getSimpleName()}User1"
        UserInfo userInfo = new UserInfo(
                firstName: "first",
                lastName: "last",
                nickname: "nick",
                userDn: "dn",
                email: "email",
                username: userId.toLowerCase(),
                usernameForDisplay: userId
        )
        userAttrsTestServiceWrapper.saveUserAttrs(userId, userInfo)
        when:
        userAttrsTestServiceWrapper.saveUserAttrs(userId, new UserInfo(
                username: userId.toLowerCase(),
                usernameForDisplay: userId
        ))
        then:
        UserAttrs userAttrs = userAttrsService.findByUserId(userId)
        userAttrs.userId == userInfo.username.toLowerCase()
        userAttrs.userIdForDisplay == userInfo.usernameForDisplay
        userAttrs.firstName == userInfo.firstName
        userAttrs.lastName == userInfo.lastName
        userAttrs.firstName == userInfo.firstName
        userAttrs.nickname == userInfo.nickname
        userAttrs.dn == userInfo.userDn
        userAttrs.email == userInfo.email
    }

    def "do not override existing user attributes with null values - update 1 value"() {
        String userId = "${UserAttrsServiceSpec.getSimpleName()}User1"
        UserInfo userInfo = new UserInfo(
                firstName: "first",
                lastName: "last",
                nickname: "nick",
                userDn: "dn",
                email: "email",
                username: userId.toLowerCase(),
                usernameForDisplay: userId
        )
        userAttrsTestServiceWrapper.saveUserAttrs(userId, userInfo)
        when:
        userAttrsTestServiceWrapper.saveUserAttrs(userId, new UserInfo(
                username: userId.toLowerCase(),
                usernameForDisplay: userId,
                lastName: "lastNew"
        ))
        then:
        UserAttrs userAttrs = userAttrsService.findByUserId(userId)
        userAttrs.userId == userInfo.username.toLowerCase()
        userAttrs.userIdForDisplay == userInfo.usernameForDisplay
        userAttrs.firstName == userInfo.firstName
        userAttrs.lastName == "lastNew"
        userAttrs.firstName == userInfo.firstName
        userAttrs.nickname == userInfo.nickname
        userAttrs.dn == userInfo.userDn
        userAttrs.email == userInfo.email
    }

    def "do not override existing user attributes with null values - update all value"() {
        String userId = "${UserAttrsServiceSpec.getSimpleName()}User1"
        UserInfo userInfo = new UserInfo(
                firstName: "first",
                lastName: "last",
                nickname: "nick",
                userDn: "dn",
                email: "email",
                username: userId.toLowerCase(),
                usernameForDisplay: userId
        )
        userAttrsTestServiceWrapper.saveUserAttrs(userId, userInfo)
        when:
        userAttrsTestServiceWrapper.saveUserAttrs(userId, new UserInfo(
                username: userId.toLowerCase(),
                usernameForDisplay: userId,
                firstName: "firstNew",
                lastName: "lastNew",
                nickname: "nickNew",
                userDn: "dnNew",
                email: "emailNew",
        ))
        then:
        UserAttrs userAttrs = userAttrsService.findByUserId(userId)
        userAttrs.userId == userInfo.username.toLowerCase()
        userAttrs.userIdForDisplay == userInfo.usernameForDisplay
        userAttrs.firstName == "firstNew"
        userAttrs.lastName == "lastNew"
        userAttrs.nickname == "nickNew"
        userAttrs.dn == "dnNew"
        userAttrs.email == "emailNew"
    }

    def "userInfoService.getUserName() does not override userNameForDisplay"() {
        String userId = "${UserAttrsServiceSpec.getSimpleName()}User1"
        UserInfo userInfo = new UserInfo(
                firstName: "Fake",
                lastName: "Fake",
                nickname: "Fake",
                userDn: "UserAttrsServiceSpecUser1",
                email: "userattrsservicespecuser1@fakeplace",
                username: userId.toLowerCase(),
                usernameForDisplay: "${userId} for display"
        )
        userAttrsTestServiceWrapper.saveUserAttrs(userId, userInfo)

        when:
        String res = userInfoService.getUserName(userId)

        then:
        res
        res == userInfo.username.toLowerCase()
        UserAttrs userAttrs = userAttrsService.findByUserId(userId)
        userAttrs.userId == userInfo.username.toLowerCase()
        userAttrs.userIdForDisplay == userInfo.usernameForDisplay
        userAttrs.firstName == userInfo.firstName
        userAttrs.lastName == userInfo.lastName
        userAttrs.firstName == userInfo.firstName
        userAttrs.nickname == userInfo.nickname
        userAttrs.dn == userInfo.userDn
        userAttrs.email == userInfo.email
    }

    def "userNameForDisplay will use userId if not in userInfo"() {
        String userId = "${UserAttrsServiceSpec.getSimpleName()}User1"
        UserInfo userInfo = new UserInfo(
                firstName: "first",
                lastName: "last",
                nickname: "nick",
                userDn: "dn",
                email: "email",
                username: userId.toLowerCase(),
        )

        when:
        userAttrsTestServiceWrapper.saveUserAttrs(userId, userInfo)

        then:
        !userInfo.usernameForDisplay
        UserAttrs userAttrs = userAttrsService.findByUserId(userId)
        userAttrs.userId == userInfo.username.toLowerCase()
        userAttrs.userIdForDisplay == userId
        userAttrs.firstName == userInfo.firstName
        userAttrs.lastName == userInfo.lastName
        userAttrs.firstName == userInfo.firstName
        userAttrs.nickname == userInfo.nickname
        userAttrs.dn == userInfo.userDn
        userAttrs.email == userInfo.email
    }

    def "userNameForDisplay will use userNameForDisplay if in userInfo"() {
        String userId = "${UserAttrsServiceSpec.getSimpleName()}User1"
        UserInfo userInfo = new UserInfo(
                firstName: "first",
                lastName: "last",
                nickname: "nick",
                userDn: "dn",
                email: "email",
                username: userId.toLowerCase(),
                usernameForDisplay: "${userId}-Display"
        )

        when:
        userAttrsTestServiceWrapper.saveUserAttrs(userId, userInfo)

        then:
        userInfo.usernameForDisplay
        UserAttrs userAttrs = userAttrsService.findByUserId(userId)
        userAttrs.userId == userInfo.username.toLowerCase()
        userAttrs.userIdForDisplay == userInfo.usernameForDisplay
        userAttrs.firstName == userInfo.firstName
        userAttrs.lastName == userInfo.lastName
        userAttrs.firstName == userInfo.firstName
        userAttrs.nickname == userInfo.nickname
        userAttrs.dn == userInfo.userDn
        userAttrs.email == userInfo.email
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "userTags will be inserted when creating new user"() {
        String userId = "${UserAttrsServiceSpec.getSimpleName()}User1"
        UserInfo userInfo = new UserInfo(
                firstName: "first",
                lastName: "last",
                nickname: "nick",
                userDn: "dn",
                email: "email",
                username: userId.toLowerCase(),
                usernameForDisplay: "${userId}-Display",
                userTags: [Organization : "XYZ", Agency: "ABC"],
        )

        when:
        userAttrsTestServiceWrapper.saveUserAttrs(userId, userInfo)

        then:

        List<UserTag> foundUserTags = userTagRepo.findAllByUserId(userId?.toLowerCase())
        foundUserTags
        foundUserTags.size() == userInfo.userTags.size()
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "userTags will be updated"() {
        String userId = "${UserAttrsServiceSpec.getSimpleName()}User1"
        UserInfo userInfo = new UserInfo(
                firstName: "first",
                lastName: "last",
                nickname: "nick",
                userDn: "dn",
                email: "email",
                username: userId.toLowerCase(),
                usernameForDisplay: "${userId}-Display",
                userTags: [Organization : "XYZ", Agency: "ABC"],
        )
        userAttrsService.attrsAndUserTagsUpdateIntervalHours = 7

        when:
        Date userTagsLastUpdated1 = userAttrsTestServiceWrapper.saveUserAttrs(userId, userInfo).userTagsLastUpdated
        List<UserTag> foundUserTags1 = userTagRepo.findAllByUserId(userId?.toLowerCase())

        // force userTagsLastUpdated date to be before attrsAndUserTagsUpdateIntervalDays
        UserAttrs userAttrs = userAttrsService.findByUserId(userId)
        use(TimeCategory) {
            userAttrs.userTagsLastUpdated = new Date()-8.hours
        }
        userAttrsRepo.save(userAttrs)

        // remove a userTag and update another
        userInfo.userTags.remove('Organization')
        userInfo.userTags.put('Agency', 'DEF')
        Date userTagsLastUpdated2 = userAttrsTestServiceWrapper.saveUserAttrs(userId, userInfo).userTagsLastUpdated
        List<UserTag> foundUserTags2 = userTagRepo.findAllByUserId(userId?.toLowerCase())

        then:

        assert foundUserTags1
        foundUserTags1.size() == 2
        foundUserTags1.find { it.key == 'Organization' && it.value  == 'XYZ'}
        foundUserTags1.find { it.key == 'Agency' && it.value  == 'ABC'}

        assert foundUserTags2
        foundUserTags2.size() == 1
        foundUserTags2.find { it.key == 'Agency' && it.value  == 'DEF'}

        userTagsLastUpdated1 < userTagsLastUpdated2
    }
}
