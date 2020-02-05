package skills.auth

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClientException
import skills.auth.pki.PkiUserLookup
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.services.UserAttrsService
import skills.utils.RetryUtil

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

    @Profile
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

    String getCurrentUserId() {
        return getCurrentUser().username
    }

    /**
     * Abstracts dealing with PKI vs Password/Form modes when user id param is provided
     */
    String getUserName(String userIdParam) {
        return RetryUtil.withRetry(3) {
            return doGetUserName(userIdParam)
        }
    }

    @Transactional
    @Profile
    protected String doGetUserName(String userIdParam) {
        String userNameRes = userIdParam
        if (!userIdParam) {
            UserInfo userInfo = getCurrentUser()
            userNameRes =  userInfo.username
        } else if (authMode == AuthMode.PKI) {
            UserInfo userInfo
            try {
                userInfo = pkiUserLookup.lookupUserDn(userIdParam)
            } catch (Throwable e) {
                log.error("user-info-service lookup failed: ${e.getMessage()}")
                SkillException ske = new SkillException(e.getCause().getMessage())
                ske.errorCode = ErrorCode.UserNotFound
                throw ske
            }
            if (!userInfo) {
                log.error("received empty user information from lookup")
                throw new SkillException("User Info Service does not know about user with provided lookup id of [${userIdParam}]")
            }

            try {
                userAuthService.createOrUpdateUser(userInfo)
            } catch(Throwable e) {
                log.error("error during createOrUpdateUser", e);
                throw new SkillException("Failed to retrieve user info via [$userIdParam]")
            }

            userNameRes = userInfo.username
        }

        /**
         * Save UserAttrs as loading of user relies on those records to be present;
         * also it was decided to use db fk constraints to map to this table to ensure valid userId and to provide simple way to
         * remove users
         */
        if (!(authMode == AuthMode.PKI)) {
            userAttrsService.saveUserAttrs(userNameRes, new UserInfo(username: userNameRes, usernameForDisplay: userNameRes))
        }

        return userNameRes?.toLowerCase()
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
