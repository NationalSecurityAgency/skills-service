package skills.service.auth

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import skills.service.datastore.services.UserAdminService

@Component
@Slf4j
class UserInfoService {

    @Value('${skills.authorization.authMode:#{T(skills.service.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Autowired
    UserAdminService userAdminService

    @Autowired
    UserDetailsService userDetailsService

    UserInfo getCurrentUser() {
        UserInfo currentUser
        def principal = SecurityContextHolder.getContext()?.authentication?.principal
        if (principal) {
            if (principal instanceof UserInfo) {
                currentUser = principal
            } else {
                log.info("Unexpected/Unauthenticated princial [${principal}]")
            }
        }
        return currentUser
    }

    /**
     * @param userId
     * @return if userId paramater is supplied, then return the correct userId based on authMode,
     * i.e. - do a DN lookup if in PKI mode, otherwise return the passed in userId as we do not
     * need to do lookup's in FORM auth mode.  If userId is not supplied, then return the current
     * user's userId.
     */
    String lookupUserId(String userId) {
        String retVal
        if (userId) {
            if (authMode == AuthMode.FORM) {
                retVal = userId
            } else {
                // we are in PKI auth mode so we need to lookup the user to convert from DN to username
                retVal = userDetailsService.loadUserByUsername(userId).username
            }
        } else {
            retVal = getCurrentUser().username
        }
        return retVal
    }
}
