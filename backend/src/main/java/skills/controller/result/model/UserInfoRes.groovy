package skills.controller.result.model


import groovy.transform.EqualsAndHashCode
import skills.auth.UserInfo
import skills.storage.model.auth.User

@EqualsAndHashCode
class UserInfoRes {
    String userId
    String first
    String last
    String nickname
    String dn

    UserInfoRes() {  }

    UserInfoRes(UserInfo userInfo) {
        this.userId = userInfo.username
        this.first = userInfo.firstName
        this.last = userInfo.lastName
        this.nickname = userInfo.nickname
        this.dn = userInfo.userDn
    }

    UserInfoRes(User user) {
        userId = user.userId
        first =  user.userProps.find {it.setting == 'firstName'}?.value
        last =  user.userProps.find {it.setting == 'lastName'}?.value
        nickname =  user.userProps.find {it.setting == 'nickname'}?.value
        dn =  user.userProps.find {it.setting == 'DN'}?.value
    }
}
