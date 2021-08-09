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
    Map<String, Object> userTags = [:]
}
