/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        return null
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
