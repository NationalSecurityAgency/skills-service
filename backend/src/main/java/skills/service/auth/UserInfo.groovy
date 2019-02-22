package skills.service.auth

import groovy.transform.Canonical
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Canonical
class UserInfo implements UserDetails {
    String firstName
    String lastName
    String email
    String username
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
