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
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.storage.model.UserAttrs
import skills.storage.repos.UserAttrsRepo

import static skills.controller.exceptions.SkillException.NA

@Service
@Slf4j
class UserAttrsService {

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Transactional
    @Profile
    UserAttrs saveUserAttrs(String userId, UserInfo userInfo) {
        validateUserId(userId)

        UserAttrs userAttrs = loadUserAttrsFromLocalDb(userId)
        boolean doSave = true

        if (!userAttrs) {
            userAttrs = new UserAttrs(userId: userId?.toLowerCase(), userIdForDisplay: userId)
        } else {
            doSave = shouldUpdate(userInfo, userAttrs)

            if (log.isTraceEnabled()) {
                log.trace('UserInfo/UserAttrs: \n\tfirstName [{}/{}]\n\tlastName [{}]/[{}]\n\temail [{}]/[{}]\n\tuserDn [{}]/[{}]\n\tnickname [{}]/[{}]\n\tusernameForDisplay [{}]/[{}]\n\tlandingPage [{}]/[{}]',
                        userInfo.firstName, userAttrs.firstName,
                        userInfo.lastName, userAttrs.lastName,
                        userInfo.email, userAttrs.email,
                        userInfo.userDn, userAttrs.dn,
                        userInfo.nickname, userAttrs.nickname,
                        userInfo.usernameForDisplay, userAttrs.userIdForDisplay,
                )
            }
        }
        if (doSave) {
            populate(userAttrs, userInfo)
            try {
                saveUserAttrsInLocalDb(userAttrs)
            } catch (DataIntegrityViolationException dataIntegrityViolationException) {
                log.warn("${dataIntegrityViolationException.getMessage()} received when trying to save userAttrs for [${userId}], fetching and retrying")
                userAttrs = loadUserAttrsFromLocalDb(userId)
                if (!userAttrs) {
                    log.error(dataIntegrityViolationException.getMessage())
                    throw new SkillException("Received DataIntegrityViolation when attempting to insert UserAttrs for [${userId}] but entry does not exist")
                }
                if (shouldUpdate(userInfo, userAttrs)) {
                    populate(userAttrs, userInfo)
                    saveUserAttrsInLocalDb(userAttrs)
                }
            }
        }
        return userAttrs
    }

    private boolean shouldUpdate(UserInfo userInfo, UserAttrs userAttrs) {
        return (userInfo.firstName && userAttrs.firstName != userInfo.firstName) ||
                (userInfo.lastName && userAttrs.lastName != userInfo.lastName) ||
                (userInfo.email && userAttrs.email != userInfo.email) ||
                (userInfo.userDn && userAttrs.dn != userInfo.userDn) ||
                (userInfo.nickname !=null && userAttrs.nickname != (userInfo.nickname ?: "")) ||
                (userInfo.usernameForDisplay && userAttrs.userIdForDisplay != userInfo.usernameForDisplay)
    }

    private void populate(UserAttrs userAttrs, UserInfo userInfo) {
        userAttrs.firstName = userInfo.firstName ?: userAttrs.firstName
        userAttrs.lastName = userInfo.lastName ?: userAttrs.lastName
        userAttrs.email = userInfo.email ?: userAttrs.email
        userAttrs.dn = userInfo.userDn ?: userAttrs.dn
        userAttrs.nickname = (userInfo.nickname != null ? userInfo.nickname : userAttrs.nickname) ?: ""
        userAttrs.userIdForDisplay = userInfo.usernameForDisplay ?: userAttrs.userIdForDisplay
    }

    private void validateUserId(String userId) {
        if (!userId) {
            throw new SkillException("userId must be present", NA, NA, ErrorCode.BadParam)
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
        return userAttrsRepo.findByUserId(userId?.toLowerCase())
    }

}
