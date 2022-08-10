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

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import skills.storage.model.auth.UserRole

class GrantedAuthoritiesUpdater {

    /**
     * Adds the specified UserRole to the list of GrantedAuthorities for the currently
     * authenticated user if it's not already present
     * @param userRole
     */
    public static void addUserRoleToCurrentUser(UserRole userRole) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication()

        Authentication updatedAuth
        if (authentication instanceof PreAuthenticatedAuthenticationToken) {
            Collection<GrantedAuthority> authorities = authentication.getAuthorities()
            updatedAuth = new PreAuthenticatedAuthenticationToken(authentication.getPrincipal(),
                    authentication.getCredentials(), addIfNotPresent(userRole, new ArrayList<>(authorities)))
        } else if (authentication instanceof UsernamePasswordAuthenticationToken) {
            Collection<GrantedAuthority> authorities = authentication.getAuthorities()
            updatedAuth = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(),
                    authentication.getCredentials(), addIfNotPresent(userRole, new ArrayList<>(authorities)))
        } else {
            throw new UnsupportedOperationException("Unrecognized Authentication type [${authentication?.getClass()}]")
        }

        SecurityContextHolder.getContext().setAuthentication(updatedAuth)
    }

    private static Collection<GrantedAuthority> addIfNotPresent(UserRole userRole, List<GrantedAuthority> authorities) {
        if(!authorities?.find {it instanceof UserSkillsGrantedAuthority
                && it.getRole().roleName == userRole.roleName && it.getRole().projectId == userRole.projectId}) {
            authorities.add(new UserSkillsGrantedAuthority(userRole))
        }
        return Collections.unmodifiableCollection(authorities)
    }

}
