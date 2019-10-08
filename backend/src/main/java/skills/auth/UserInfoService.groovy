package skills.auth

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import skills.auth.pki.PkiUserLookup

@Component
@Slf4j
class UserInfoService {

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Autowired(required = false)
    PkiUserLookup pkiUserLookup

    UserInfo getCurrentUser() {
        UserInfo currentUser
        def principal = SecurityContextHolder.getContext()?.authentication?.principal
        if (principal) {
            if (principal instanceof UserInfo) {
                log.trace("User principal {}", principal)
                currentUser = principal

                if (authMode == AuthMode.PKI) {
                    currentUser = pkiUserLookup.lookupUserDn(principal.userDn)
                    log.trace("Pki user {} for [{}] dn", currentUser, principal)
                }
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
        if (!userIdParam) {
            return getCurrentUser().username
        }
        if (authMode == AuthMode.PKI) {
            try {
                return pkiUserLookup.lookupUserDn(userIdParam).username
            } catch (UsernameNotFoundException e) {
                throw new skills.controller.exceptions.SkillException("User [$userIdParam] does not exist")
            }
        }

        return userIdParam
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
