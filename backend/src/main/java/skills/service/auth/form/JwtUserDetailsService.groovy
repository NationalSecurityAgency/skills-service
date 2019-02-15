package skills.service.auth.form

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import skills.service.auth.UserAuthService
import skills.service.auth.UserInfo

class JwtUserDetailsService implements UserDetailsService {

    @Autowired
    UserAuthService userAuthService

    @Autowired
    PasswordEncoder passwordEncoder

    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserInfo userInfo = userAuthService.loadByUserId(username)
        if (!userInfo) {
            throw new UsernameNotFoundException("Unknown user [$username]")
        }
        return userInfo
    }
}
