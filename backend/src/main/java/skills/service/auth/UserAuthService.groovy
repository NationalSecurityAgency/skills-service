package skills.service.auth

import groovy.util.logging.Slf4j
import org.apache.commons.collections.CollectionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import skills.service.datastore.services.AccessSettingsStorageService
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.User
import skills.storage.model.auth.UserRole
import skills.storage.repos.UserRepo

import javax.servlet.http.HttpServletRequest

@Component
@Slf4j
class UserAuthService {

    private static Collection<GrantedAuthority> EMPTY_ROLES = new ArrayList<>()

    @Autowired
    UserRepo userRepository

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    private AuthenticationManager authenticationManager

    @Autowired
    PasswordEncoder passwordEncoder


    @Transactional(readOnly = true)
    Collection<GrantedAuthority> loadAuthorities(String userId) {
        return convertRoles(userRepository.findByUserId(userId)?.roles)
    }

    @Transactional(readOnly = true)
    UserInfo loadByUserId(String userId) {
        UserInfo userInfo
        User user = userRepository.findByUserId(userId)
        if (user) {
            userInfo = new UserInfo (
                    username: user.userId,
                    password: user.password,
                    firstName: user.userProps.find {it.name =='firstName'}?.value,
                    lastName: user.userProps.find {it.name =='lastName'}?.value,
                    email: user.userProps.find {it.name =='email'}?.value,
                    userDn: user.userProps.find {it.name =='DN'}?.value,
                    authorities: convertRoles(userRepository.findByUserId(userId)?.roles)
            )
        }
        return userInfo
    }

    @Transactional
    UserInfo createUser(UserInfo userInfo) {
        accessSettingsStorageService.createAppUser(userInfo, false)
        return loadByUserId(userInfo.username)
    }

    @Transactional
    UserInfo createOrUpdateUser(UserInfo userInfo) {
        accessSettingsStorageService.createAppUser(userInfo, true)
        return loadByUserId(userInfo.username)
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
        boolean shouldAddRole = false
        if (userRole.roleName == RoleName.ROLE_APP_USER || userRole.roleName == RoleName.ROLE_SUPER_DUPER_USER) {
            shouldAddRole = true
        } else {
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
            log.warn("Unable to current request attributes. Error Recievedd [$e]")
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
    }

    @Transactional(readOnly = true)
    boolean userExists(String userId) {
        return userRepository.existsByUserId(userId)
    }
}
