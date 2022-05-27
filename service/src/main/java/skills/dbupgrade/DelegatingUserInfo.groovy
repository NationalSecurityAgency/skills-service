package skills.dbupgrade

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import skills.auth.UserInfo

class DelegatingUserInfo extends UserInfo {
    private User user

    public DelegatingUserInfo(User user) {
        this.user = user
    }

    String getFirstName() {
        return null
    }
    String getLastName() {
        return v
    }
    String getNickname() {
        return null
    }
    String getEmail() {
        return null
    }
    String getUsername() {
        return user.getUsername()
    }

    String getUsernameForDisplay() {
        return null
    }

    String getUserDn() {
        return null
    }

    String getPassword() {
        return user.getPassword()
    }

    List<GrantedAuthority> getAuthorities() {
        return user.getAuthorities()
    }
    boolean isAccountNonExpired() {
        return user.isAccountNonExpired()
    }
    boolean isAccountNonLocked() {
        return user.isAccountNonLocked()
    }
    boolean isCredentialsNonExpired() {
        return user.isCredentialsNonExpired()
    }
    boolean isEnabled() {
        return user.isEnabled()
    }
    boolean isProxied() {
        return false
    }
    boolean isEmailVerified() {
        return false
    }
    String getProxyingSystemId() {
        return null
    }
    Map<String, Object> getUserTags() {
        return [:]
    }
}
