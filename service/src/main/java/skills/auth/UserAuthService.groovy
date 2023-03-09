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
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.collections4.CollectionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import skills.services.AccessSettingsStorageService
import skills.services.admin.ProjAdminService
import skills.services.inception.InceptionProjectService
import skills.services.settings.SettingsService
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.User
import skills.storage.model.auth.UserRole
import skills.storage.repos.QuizToSkillDefRepo
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserRepo
import skills.storage.repos.UserRoleRepo

@Component
@Slf4j
class UserAuthService {

    private static Collection<GrantedAuthority> EMPTY_ROLES = new ArrayList<>()

    @Autowired
    UserRepo userRepository

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    @Lazy
    private AuthenticationConfiguration authenticationConfiguration

    @Autowired
    InceptionProjectService inceptionProjectService

    @Autowired
    SettingsService settingsService

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    ApproverRoleDecider approverRoleDecider

    @Autowired(required = false)  // only for SecurityMode.FormAuth
    @Qualifier('formSecurityFilterChain')
    SecurityFilterChain formSecurityFilterChain

    @Autowired(required = false)  // only for SecurityMode.FormAuth
    SecurityContextRepository securityContextRepository

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Value('#{"${skills.authorization.verifyEmailAddresses:false}"}')
    Boolean verifyEmailAddresses

    private AutoLoginProcessor autoLoginProcessor

    @PostConstruct
    void init() {
        if (formSecurityFilterChain) {
            autoLoginProcessor = new AutoLoginProcessor(formSecurityFilterChain.getFilters().find { it.class == UsernamePasswordAuthenticationFilter }, securityContextRepository)
        }
    }

    @Transactional(readOnly = true)
    Collection<GrantedAuthority> loadAuthorities(String userId) {
        List<UserRole> userRoles = userRoleRepo.findAllByUserId(userId?.toLowerCase())
        return convertRoles(userRoles)
    }

    @Transactional(readOnly = true)
    @Profile
    UserInfo loadByUserId(String userId) {
        UserInfo userInfo
        User user = userRepository.findByUserId(userId?.toLowerCase())
        if (user) {
            UserAttrs userAttrs = userAttrsRepo.findByUserId(userId?.toLowerCase())
            userInfo = createUserInfo(user, userAttrs)
            if (verifyEmailAddresses) {
                userInfo.accountNonLocked = userInfo.emailVerified
            }
        }
        return userInfo
    }

    private UserInfo createUserInfo(User user, UserAttrs userAttrs) {
        List<UserRole> userRoles = userRoleRepo.findAllByUserId(user.userId.toLowerCase())
        return new UserInfo (
                username: user.userId,
                password: user.password,
                firstName: userAttrs.firstName,
                lastName: userAttrs.lastName,
                email: userAttrs.email,
                emailVerified: Boolean.valueOf(userAttrs.emailVerified),
                userDn: userAttrs.dn,
                nickname: userAttrs.nickname,
                authorities: convertRoles(userRoles),
                usernameForDisplay: userAttrs.userIdForDisplay,
        )
    }

    @Transactional
    UserInfo createUser(UserInfo userInfo) {
        accessSettingsStorageService.createAppUser(userInfo, false)
        return loadByUserId(userInfo.username)
    }

    @Transactional
    @Profile
    UserInfo createOrUpdateUser(UserInfo userInfo) {
        AccessSettingsStorageService.UserAndUserAttrsHolder userAndUserAttrs = accessSettingsStorageService.createAppUser(userInfo, true)
        return createUserInfo(userAndUserAttrs.user, userAndUserAttrs.userAttrs)
    }

    /**
     * Loads information for the specified user from the database but DOES NOT create a user
     * if no record already exists
     *
     * @param userInfo
     * @return
     */
    @Transactional(readOnly=true)
    @Profile
    UserInfo get(UserInfo userInfo) {
        AccessSettingsStorageService.UserAndUserAttrsHolder userAndUserAttrs = accessSettingsStorageService.get(userInfo)
        if (userAndUserAttrs) {
            return createUserInfo(userAndUserAttrs.user, userAndUserAttrs.userAttrs)
        }
        return null
    }

    void autologin(UserInfo userInfo, String password, HttpServletRequest request, HttpServletResponse response) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userInfo, password, userInfo.getAuthorities())
        autoLoginProcessor.autoLogin(usernamePasswordAuthenticationToken, request, response)
    }

    private Collection<GrantedAuthority> convertRoles(List<UserRole> roles) {
        Collection<GrantedAuthority> grantedAuthorities = EMPTY_ROLES
        List<UserRole> addedRoles
        if (!CollectionUtils.isEmpty(roles)) {
            grantedAuthorities = new ArrayList<GrantedAuthority>(roles.size())
            for (UserRole role : roles) {
                if (shouldAddRole(role)) {
                    addedRoles = addedRoles ?: []
                    addedRoles.add(role)
                    grantedAuthorities.add(new UserSkillsGrantedAuthority(role))
                }
            }
            UserRole quizReadOnlyRole = checkQuizReadOnlyRole(roles, addedRoles)
            if (quizReadOnlyRole) {
                grantedAuthorities.add(new UserSkillsGrantedAuthority(quizReadOnlyRole))
            }
        }
        return grantedAuthorities
    }

    /**
     * quiz read only role is assigned if
     * - the user is not already quiz admin (of course!)
     * - this user is an admin of a project where quiz was associated to a skill;
     *   in this case project admin is allowed to view (but not mutate) quiz definition
     */
    @CompileStatic
    private UserRole checkQuizReadOnlyRole(List<UserRole> roles, List<UserRole> addedRoles) {
        UserRole role
        HttpServletRequest request = getServletRequest()
        String quizId = AuthUtils.getQuizIdFromRequest(request)
        String method = request?.method
        boolean isGetMethod =  method && method == HttpMethod.GET.toString()
        if (isGetMethod && quizId) {
            boolean isNotQuizAdmin = !addedRoles?.find { it.roleName == RoleName.ROLE_QUIZ_ADMIN }
            if (isNotQuizAdmin) {
                List<UserRole> projectAdminRoles = roles.findAll { it.roleName == RoleName.ROLE_PROJECT_ADMIN }
                if (projectAdminRoles) {
                    List<String> projectIds = projectAdminRoles.collect { it.projectId }
                    if (quizToSkillDefRepo.existQuizIdToOneOfTheProjectIdsAssociation(quizId, projectIds)) {
                        role = new UserRole(userId: projectAdminRoles.first().userId, quizId: quizId, roleName: RoleName.ROLE_QUIZ_READ_ONLY)
                    }
                }
            }
        }

        return role
    }

    private boolean shouldAddRole(UserRole userRole) {
        boolean shouldAddRole = true
        if (userRole.roleName == RoleName.ROLE_PROJECT_ADMIN) {
            shouldAddRole = false
            String projectId = AuthUtils.getProjectIdFromRequest(servletRequest)
            if (projectId && userRole.projectId && projectId.equalsIgnoreCase(userRole.projectId)) {
                shouldAddRole = true
            }
        }
        if (userRole.roleName == RoleName.ROLE_PROJECT_APPROVER) {
            shouldAddRole = approverRoleDecider.shouldGrantApproverRole(servletRequest, userRole)
        }
        if (userRole.roleName == RoleName.ROLE_QUIZ_ADMIN) {
            shouldAddRole = false
            String quizId = AuthUtils.getQuizIdFromRequest(servletRequest)
            if (quizId && userRole.quizId && quizId.equalsIgnoreCase(userRole.quizId)) {
                shouldAddRole = true
            }
        }
        return shouldAddRole
    }

    HttpServletRequest getServletRequest() {
        HttpServletRequest httpServletRequest
        try {
            ServletRequestAttributes currentRequestAttributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
            httpServletRequest = currentRequestAttributes?.getRequest()
        } catch (Exception e) {
            log.warn("Unable to access current HttpServletRequest. Error Recieved [$e]", e)
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

        projAdminService.pinAllExistingProjectsWhereUserIsAdminExceptInception(userId)
    }

    @Transactional(readOnly = true)
    boolean userExists(String userId) {
        return userRepository.existsByUserIdIgnoreCase(userId)
    }

    static class AutoLoginProcessor extends UsernamePasswordAuthenticationFilter {
        private UsernamePasswordAuthenticationFilter delegate
        private UsernamePasswordAuthenticationToken currentAuthRequest = null

        AutoLoginProcessor(UsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter, SecurityContextRepository securityContextRepository) {
            this.delegate = usernamePasswordAuthenticationFilter
            this.setSecurityContextRepository(securityContextRepository)
            this.setAuthenticationSuccessHandler(this.delegate.getSuccessHandler())
            this.setSessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy())
        }

        void autoLogin(UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToke, HttpServletRequest request, HttpServletResponse response) {
            this.currentAuthRequest = usernamePasswordAuthenticationToke
            super.doFilter(request, response, () -> {})
            this.currentAuthRequest = null
        }

        @Override
        Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
            return this.delegate.getAuthenticationManager().authenticate(this.currentAuthRequest);
        }

        @Override
        protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
            return true
        }
    }
}
