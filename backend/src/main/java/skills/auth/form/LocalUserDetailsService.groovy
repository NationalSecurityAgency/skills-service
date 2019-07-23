package skills.auth.form

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder

class LocalUserDetailsService implements UserDetailsService {

    @Autowired
    skills.auth.UserAuthService userAuthService

    @Autowired
    PasswordEncoder passwordEncoder

    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        skills.auth.UserInfo userInfo = userAuthService.loadByUserId(username)
        if (!userInfo) {
            throw new UsernameNotFoundException("Unknown user [$username]")
        }
        return userInfo
    }
}
