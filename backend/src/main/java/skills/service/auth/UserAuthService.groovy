package skills.service.auth


import org.apache.commons.collections.CollectionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.GrantedAuthority
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
class UserAuthService {

    private static Collection<GrantedAuthority> EMPTY_ROLES = new ArrayList<>()

    @Autowired
    UserRepo userRepository

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

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

    @Transactional()
    UserInfo createUser(UserInfo userInfo) {
        accessSettingsStorageService.createAppUser(userInfo)
        return loadByUserId(userInfo.username)
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
        ServletRequestAttributes currentRequestAttributes = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
        HttpServletRequest httpServletRequest = currentRequestAttributes.getRequest()
        return httpServletRequest
    }
}
