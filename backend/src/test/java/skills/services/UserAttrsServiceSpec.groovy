package skills.services

import org.springframework.beans.factory.annotation.Autowired
import skills.auth.UserInfo
import skills.intTests.utils.DefaultIntSpec
import skills.storage.model.UserAttrs

class UserAttrsServiceSpec extends DefaultIntSpec {

    @Autowired
    UserAttrsService userAttrsService

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
        userAttrsService.saveUserAttrs(userId, userInfo)
        when:
        userAttrsService.saveUserAttrs(userId, new UserInfo(
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
        userAttrsService.saveUserAttrs(userId, userInfo)
        when:
        userAttrsService.saveUserAttrs(userId, new UserInfo(
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
        userAttrsService.saveUserAttrs(userId, userInfo)
        when:
        userAttrsService.saveUserAttrs(userId, new UserInfo(
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
}
