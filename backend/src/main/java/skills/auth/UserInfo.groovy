package skills.auth

import groovy.transform.Canonical
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Canonical
class UserInfo implements UserDetails {
    private static final long serialVersionUID = 1L

    String firstName
    String lastName
    String nickname
    String email
    String username
    String usernameForDisplay
    String userDn
    String password
    List<GrantedAuthority> authorities
    boolean accountNonExpired = true
    boolean accountNonLocked = true
    boolean credentialsNonExpired = true
    boolean enabled = true
    boolean proxied = false
    String proxyingSystemId
}
