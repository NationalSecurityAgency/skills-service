package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.auth.UserInfo
import skills.storage.model.UserAttrs
import skills.storage.repos.UserAttrsRepo

@Service
@Slf4j
class UserAttrsService {

    @Autowired
    UserAttrsRepo userAttrsRepo

    void saveUserAttrs(String userId, UserInfo userInfo) {
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)
        boolean doSave = true
        if (!userAttrs) {
            userAttrs = new UserAttrs(userId: userId)
        } else {
            doSave = userAttrs.firstName != userInfo.firstName ||
                    userAttrs.lastName != userInfo.lastName ||
                    userAttrs.email != userInfo.email ||
                    userAttrs.dn != userInfo.userDn ||
                    userAttrs.nickname != (userInfo.nickname ?: "") ||
                    userAttrs.userIdForDisplay != userInfo.usernameForDisplay
        }
        if (doSave) {
            userAttrs.firstName = userInfo.firstName
            userAttrs.lastName = userInfo.lastName
            userAttrs.email = userInfo.email
            userAttrs.dn = userInfo.userDn
            userAttrs.nickname = userInfo.nickname ?: ""
            userAttrs.userIdForDisplay = userInfo.usernameForDisplay
            userAttrsRepo.save(userAttrs)
        }
    }

    UserAttrs findByUserId(String userId) {
        return userAttrsRepo.findByUserIdIgnoreCase(userId)
    }

}
