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
    String landingPage

    UserInfoRes() {}

    UserInfoRes(UserInfo userInfo) {
        this.userId = userInfo.username
        this.first = userInfo.firstName
        this.last = userInfo.lastName
        this.nickname = userInfo.nickname
        this.dn = userInfo.userDn
        this.userIdForDisplay = userInfo.usernameForDisplay
        this.landingPage = userInfo.landingPage
    }

    UserInfoRes(UserAttrs userAttrs) {
        this.userId = userAttrs.userId
        this.first = userAttrs.firstName
        this.last = userAttrs.lastName
        this.nickname = userAttrs.nickname
        this.userIdForDisplay = userAttrs.userIdForDisplay
        this.dn = userAttrs.dn
        this.landingPage = userAttrs.landingPage
    }
}
