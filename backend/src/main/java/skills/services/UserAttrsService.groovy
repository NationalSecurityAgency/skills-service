package skills.services

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.storage.model.UserAttrs
import skills.storage.repos.UserAttrsRepo

@Service
@Slf4j
class UserAttrsService {

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Transactional
    @Profile
    UserAttrs saveUserAttrs(String userId, UserInfo userInfo) {
        UserAttrs userAttrs = loadUserAttrsFromLocalDb(userId)
        boolean doSave = true
        if (!userAttrs) {
            userAttrs = new UserAttrs(userId: userId?.toLowerCase())
        } else {
            doSave = (userInfo.firstName && userAttrs.firstName != userInfo.firstName) ||
                    (userInfo.lastName && userAttrs.lastName != userInfo.lastName) ||
                    (userInfo.email && userAttrs.email != userInfo.email) ||
                    (userInfo.userDn && userAttrs.dn != userInfo.userDn) ||
                    (userInfo.nickname && userAttrs.nickname != (userInfo.nickname ?: "")) ||
                    (userInfo.usernameForDisplay && userAttrs.userIdForDisplay != userInfo.usernameForDisplay)

            log.trace('UserInfo/UserAttrs: \n\tfirstName [{}/{}]\n\tlastName [{}]/[{}]\n\temail [{}]/[{}]\n\tuserDn [{}]/[{}]\n\tnickname [{}]/[{}]\n\tusernameForDisplay [{}]/[{}]',
                    userInfo.firstName, userAttrs.firstName,
                    userInfo.lastName, userAttrs.lastName,
                    userInfo.email, userAttrs.email,
                    userInfo.userDn, userAttrs.dn,
                    userInfo.nickname, userAttrs.nickname,
                    userInfo.usernameForDisplay, userAttrs.userIdForDisplay,
            )
        }
        if (doSave) {
            userAttrs.firstName = userInfo.firstName ?: userAttrs.firstName
            userAttrs.lastName = userInfo.lastName ?: userAttrs.lastName
            userAttrs.email = userInfo.email ?: userAttrs.email
            userAttrs.dn = userInfo.userDn ?: userAttrs.dn
            userAttrs.nickname = (userInfo.nickname ?: userAttrs.nickname) ?: ""
            userAttrs.userIdForDisplay = userInfo.usernameForDisplay ?: userAttrs.userIdForDisplay
            saveUserAttrsInLocalDb(userAttrs)
        }
        return userAttrs
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
