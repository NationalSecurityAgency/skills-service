package skills.controller.result.model


import groovy.transform.EqualsAndHashCode
import skills.auth.UserInfo
import skills.storage.model.UserAttrs
import skills.storage.model.auth.User

@EqualsAndHashCode
class UserInfoRes {
    String userId
    String userIdForDisplay
    String first
    String last
    String nickname
    String dn

    UserInfoRes() {}

    UserInfoRes(UserInfo userInfo) {
        this.userId = userInfo.username
        this.first = userInfo.firstName
        this.last = userInfo.lastName
        this.nickname = userInfo.nickname
        this.dn = userInfo.userDn
        this.userIdForDisplay = userInfo.usernameForDisplay
    }

    UserInfoRes(UserAttrs userAttrs) {
        this.userId = userAttrs.userId
        this.first = userAttrs.firstName
        this.last = userAttrs.lastName
        this.nickname = userAttrs.nickname
        this.userIdForDisplay = userAttrs.userIdForDisplay
        this.dn = userAttrs.dn
    }
}
