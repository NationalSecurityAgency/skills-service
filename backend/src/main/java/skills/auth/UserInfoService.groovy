package skills.auth

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import skills.auth.pki.PkiUserLookup
import skills.controller.exceptions.SkillException
import skills.services.UserAttrsService

@Component
@Slf4j
class UserInfoService {

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Autowired(required = false)
    PkiUserLookup pkiUserLookup

    @Autowired
    UserAuthService userAuthService

    @Autowired
    UserAttrsService userAttrsService

    UserInfo getCurrentUser() {
        UserInfo currentUser
        def principal = SecurityContextHolder.getContext()?.authentication?.principal
        if (principal) {
            if (principal instanceof UserInfo) {
                log.trace("User principal {}", principal)
                currentUser = principal
            } else {
                log.info("Unexpected/Unauthenticated princial [${principal}]")
            }
        }
        return currentUser
    }

    /**
     * Abstracts dealing with PKI vs Password/Form modes when user id param is provided
     */
    String getUserName(String userIdParam) {
        String res = userIdParam
        if (!userIdParam) {
            UserInfo userInfo = getCurrentUser()
            res =  userInfo.username
        } else if (authMode == AuthMode.PKI) {
            UserInfo userInfo
            try {
                userInfo = pkiUserLookup.lookupUserDn(userIdParam)
                userAuthService.createOrUpdateUser(userInfo)
            } catch (Throwable e) {
                throw new SkillException("Failed to retrieve user info via [$userIdParam]")
            }
            if (!userInfo) {
                throw new SkillException("User Info Service does not know about user with provided lookup id of [${userIdParam}]")
            }

            res = userInfo.username
        }

        /**
         * Save UserAttrs as loading of user relies on those records to be present
         */
        if (!(authMode == AuthMode.PKI)) {
            userAttrsService.saveUserAttrs(userIdParam, new UserInfo(username: userIdParam, usernameForDisplay: userIdParam))
        }

        return res?.toLowerCase()
    }

    /**
     * @param userKey - this will be the user's DN in PKI authMode, or the actual userId in FORM authMode
     * @return return the correct userKey based on authMode,
     * i.e. - do a DN lookup if in PKI mode, otherwise just return the passed in userKey
     */
    String lookupUserId(String userKey) {
        assert userKey
        String userId
        if (authMode == AuthMode.FORM) {
            userId = userKey  // we are using FORM authMode, so the userKey is the userId
        } else {
            // we using PKI auth mode so we need to lookup the user to convert from DN to username
            userId = pkiUserLookup.lookupUserDn(userKey)?.username
        }
        return userId?.toLowerCase()
    }
}
