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

import callStack.profiler.Profile
import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.storage.model.UserAttrs
import skills.storage.model.UserTag
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserTagRepo

import static skills.controller.exceptions.SkillException.NA

@Service
@Slf4j
class UserAttrsService {

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    UserTagRepo userTagsRepository

    @Value('#{"${skills.config.attrsAndUserTagsUpdateIntervalDays:7}"}')
    private int attrsAndUserTagsUpdateIntervalDays

    @Transactional
    @Profile
    UserAttrs saveUserAttrs(String userId, UserInfo userInfo) {
        validateUserId(userId)

        UserAttrs userAttrs = loadUserAttrsFromLocalDb(userId)
        boolean updateUserAttrs = false
        boolean updateUserTags = false

        if (!userAttrs) {
            // no userAttrs existed, creating for the first time
            userAttrs = new UserAttrs(userId: userId?.toLowerCase(), userIdForDisplay: userId)
            updateUserAttrs = true
            updateUserTags = true
        } else {
            updateUserAttrs = shouldUpdateUserAttrs(userInfo, userAttrs)
            updateUserTags = shouldUpdateUserTags(userAttrs)

            if (log.isTraceEnabled()) {
                log.trace('UserInfo/UserAttrs: \n\tfirstName [{}/{}]\n\tlastName [{}]/[{}]\n\temail [{}]/[{}]\n\tuserDn [{}]/[{}]\n\tnickname [{}]/[{}]\n\tusernameForDisplay [{}]/[{}]\n\tlandingPage [{}]/[{}]',
                        userInfo.firstName, userAttrs.firstName,
                        userInfo.lastName, userAttrs.lastName,
                        userInfo.email, userAttrs.email,
                        userInfo.userDn, userAttrs.dn,
                        userInfo.nickname, userAttrs.nickname,
                        userInfo.usernameForDisplay, userAttrs.userIdForDisplay,
                )
                log.trace('Updating UserAttrs [{}], UserTags [{}]', updateUserAttrs, updateUserTags)
            }
        }
        if (updateUserAttrs) {
            populate(userAttrs, userInfo, updateUserTags)
            saveUserAttrsInLocalDb(userAttrs)
        }
        if (updateUserTags) {
            replaceUserTags(userId?.toLowerCase(), userInfo)
        }
        return userAttrs
    }

    private void replaceUserTags(String userId, UserInfo userInfo) {
        userTagsRepository.deleteByUserId(userId)
        List<UserTag> userTags = userInfo.userTags.collect { new UserTag(userId: userId, key: it.key, value: it.value) }
        if (userTags) {
            userTagsRepository.saveAll(userTags)
        }
    }

    private boolean shouldUpdateUserAttrs(UserInfo userInfo, UserAttrs userAttrs) {
        return  (userInfo.firstName && userAttrs.firstName != userInfo.firstName) ||
                (userInfo.lastName && userAttrs.lastName != userInfo.lastName) ||
                (userInfo.email && userAttrs.email != userInfo.email) ||
                (userInfo.userDn && userAttrs.dn != userInfo.userDn) ||
                (userInfo.nickname != null && userAttrs.nickname != (userInfo.nickname ?: "")) ||
                (userInfo.usernameForDisplay && userAttrs.userIdForDisplay != userInfo.usernameForDisplay)
    }

    private boolean shouldUpdateUserTags(UserAttrs userAttrs) {
        use(TimeCategory) {
            return userAttrs.userTagsLastUpdated.before(attrsAndUserTagsUpdateIntervalDays.days.ago)
        }
    }

    private void populate(UserAttrs userAttrs, UserInfo userInfo, boolean updateUserTags) {
        userAttrs.firstName = userInfo.firstName ?: userAttrs.firstName
        userAttrs.lastName = userInfo.lastName ?: userAttrs.lastName
        userAttrs.email = userInfo.email ?: userAttrs.email
        userAttrs.dn = userInfo.userDn ?: userAttrs.dn
        userAttrs.nickname = (userInfo.nickname != null ? userInfo.nickname : userAttrs.nickname) ?: ""
        userAttrs.userIdForDisplay = userInfo.usernameForDisplay ?: userAttrs.userIdForDisplay
        if (updateUserTags) {
            userAttrs.userTagsLastUpdated = new Date()
        }
    }

    private void validateUserId(String userId) {
        if (StringUtils.isBlank(userId)) {
            throw new SkillException("userId must be present", NA, NA, ErrorCode.BadParam)
        }

        if (userId.equalsIgnoreCase("null")) {
            throw new SkillException("userId must not have a value of 'null'", NA, NA, ErrorCode.BadParam)
        }


        if (userId.contains(" ")) {
            throw new SkillException("Spaces are not allowed in user id. Provided [${userId}]", NA, NA, ErrorCode.BadParam)
        }
    }

    UserAttrs findByUserId(String userId) {
        return loadUserAttrsFromLocalDb(userId)
    }

    @Profile
    private void saveUserAttrsInLocalDb(UserAttrs userAttrs) {
        userAttrsRepo.save(userAttrs)
    }

    @Profile
    private UserAttrs loadUserAttrsFromLocalDb(String userId) {
        assert userId
        return userAttrsRepo.findByUserId(userId?.toLowerCase())
    }

}
