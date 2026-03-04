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

class SimpleUserResult  implements ProjectUser {
    SimpleUserResult(ProjectUser user) {
        this.userId = user.userId
        this.firstUpdated = user.firstUpdated
        this.lastUpdated = user.lastUpdated
        this.totalPoints = user.totalPoints
        this.dn = user.dn
        this.firstName = user.firstName
        this.lastName = user.lastName
        this.email = user.email
        this.userIdForDisplay = user.userIdForDisplay
        this.userMaxLevel = user.userMaxLevel
        this.userTag = user.userTag
        this.levelProgress = new HashMap<String, Boolean>()
    }

    Map<String, Boolean> levelProgress
    String userId
    Date firstUpdated
    Date lastUpdated
    Integer totalPoints
    String dn
    String firstName
    String lastName
    String email
    String userIdForDisplay
    Integer userMaxLevel
    String userTag
}