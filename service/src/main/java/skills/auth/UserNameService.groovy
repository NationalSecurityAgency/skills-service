package skills.auth

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException

@Component
@Slf4j
class UserNameService {

    @Autowired
    UserDetailsService userDetailsService

    @Value('#{securityConfig.authMode}}')
    skills.auth.AuthMode authMode = skills.auth.AuthMode.DEFAULT_AUTH_MODE

    String normalizeUserId(String userKey) {
        // userKey will be the userId when in FORM authMode, or the DN when in PKI auth mode.
        // When in PKI auth mode, the userDetailsService implementation will create the user
        // account if the user is not already a portal user (PkiUserDetailsService).
        // In the case of FORM authMode, the userKey is the userId and the user is expected
        // to already have a portal user account in the database
        if (authMode == skills.auth.AuthMode.PKI) {
            try {
                return userDetailsService.loadUserByUsername(userKey?.toLowerCase()).username
            } catch (UsernameNotFoundException| BadCredentialsException e) {
                def e1 = new SkillException(e.getMessage())
                e1.errorCode = ErrorCode.UserNotFound
                throw e1
            }
        } else {
            return userKey?.toLowerCase()
        }
    }
}
