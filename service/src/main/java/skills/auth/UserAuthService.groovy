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

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.apache.commons.collections4.CollectionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import skills.services.AccessSettingsStorageService
import skills.services.InceptionProjectService
import skills.services.settings.SettingsService
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.User
import skills.storage.model.auth.UserRole
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserRepo

import javax.servlet.http.HttpServletRequest

@Component
@Slf4j
class UserAuthService {

    private static Collection<GrantedAuthority> EMPTY_ROLES = new ArrayList<>()

    @Autowired
    UserRepo userRepository

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager

    @Autowired
    InceptionProjectService inceptionProjectService

    @Autowired
    SettingsService settingsService

    @Transactional(readOnly = true)
    Collection<GrantedAuthority> loadAuthorities(String userId) {
        return convertRoles(userRepository.findByUserId(userId?.toLowerCase())?.roles)
    }

    @Transactional(readOnly = true)
    @Profile
    UserInfo loadByUserId(String userId) {
        UserInfo userInfo
        User user = userRepository.findByUserId(userId?.toLowerCase())
        if (user) {
            UserAttrs userAttrs = userAttrsRepo.findByUserId(userId?.toLowerCase())
            userInfo = createUserInfo(user, userAttrs)
        }
        return userInfo
    }

    private UserInfo createUserInfo(User user, UserAttrs userAttrs) {
        return new UserInfo (
                username: user.userId,
                password: user.password,
                firstName: userAttrs.firstName,
                lastName: userAttrs.lastName,
                email: userAttrs.email,
                userDn: userAttrs.dn,
                nickname: userAttrs.nickname,
                authorities: convertRoles(user.roles),
                usernameForDisplay: userAttrs.userIdForDisplay
        )
    }

    @Transactional
    UserInfo createUser(UserInfo userInfo, boolean isSuperUser = false) {
        accessSettingsStorageService.createAppUser(userInfo, false)

        if (isSuperUser) {
            // super user gets assigned to Inception project
            inceptionProjectService.createInceptionAndAssignUser(userInfo)
        }

        return loadByUserId(userInfo.username)
    }

    @Transactional
    @Profile
    UserInfo createOrUpdateUser(UserInfo userInfo) {
        AccessSettingsStorageService.UserAndUserAttrsHolder userAndUserAttrs = accessSettingsStorageService.createAppUser(userInfo, true)
        return createUserInfo(userAndUserAttrs.user, userAndUserAttrs.userAttrs)
    }

    void autologin(UserInfo userInfo, String password) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userInfo, password, userInfo.getAuthorities())
        authenticationManager.authenticate(usernamePasswordAuthenticationToken)

        if (usernamePasswordAuthenticationToken.isAuthenticated()) {
            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken)
//            logger.debug(String.format("Auto login %s successfully!", username));
        }
    }

    private Collection<GrantedAuthority> convertRoles(List<UserRole> roles) {
        Collection<GrantedAuthority> grantedAuthorities = EMPTY_ROLES
        if (!CollectionUtils.isEmpty(roles)) {
            grantedAuthorities = new ArrayList<GrantedAuthority>(roles.size())
            for (UserRole role : roles) {
                if (shouldAddRole(role)) {
                    grantedAuthorities.add(new UserSkillsGrantedAuthority(role))
                }
            }
        }
        return grantedAuthorities
    }

    private boolean shouldAddRole(UserRole userRole) {
        boolean shouldAddRole = true
        if (userRole.roleName == RoleName.ROLE_PROJECT_ADMIN) {
            shouldAddRole = false
            String projectId = AuthUtils.getProjectIdFromRequest(servletRequest)
            if (projectId && projectId.equalsIgnoreCase(userRole.projectId)) {
                shouldAddRole = true
            }
        }
        return shouldAddRole
    }

    HttpServletRequest getServletRequest() {
        HttpServletRequest httpServletRequest
        try {
            ServletRequestAttributes currentRequestAttributes = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
            httpServletRequest = currentRequestAttributes.getRequest()
        } catch (Exception e) {
            log.warn("Unable to access current HttpServletRequest. Error Recieved [$e]")
        }
        return httpServletRequest
    }

    @Transactional(readOnly = true)
    boolean rootExists() {
        return accessSettingsStorageService.rootAdminExists()
    }

    @Transactional
    void grantRoot(String userId) {
        accessSettingsStorageService.grantRoot(userId)

        // super user gets assigned to Inception project
        inceptionProjectService.createInceptionAndAssignUser(userId)
    }

    @Transactional(readOnly = true)
    boolean userExists(String userId) {
        return userRepository.existsByUserIdIgnoreCase(userId)
    }
}
