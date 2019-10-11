package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*
import skills.controller.result.model.RequestResult
import skills.controller.result.model.UserRoleRes
import skills.services.AccessSettingsStorageService
import skills.storage.model.auth.RoleName

@RestController
@RequestMapping("/admin")
@Slf4j
@skills.profile.EnableCallStackProf
class AccessSettingsController {

    @Autowired
    skills.auth.UserInfoService userInfoService

    @Autowired
    UserDetailsService userDetailsService

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Value('#{securityConfig.authMode}}')
    skills.auth.AuthMode authMode = skills.auth.AuthMode.DEFAULT_AUTH_MODE

    @RequestMapping(value = "/projects/{projectId}/userRoles", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<UserRoleRes> getProjectUserRoles(@PathVariable("projectId") String projectId) {
        return accessSettingsStorageService.getUserRolesForProjectId(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/users/{userId}/roles", method = RequestMethod.GET)
    List<UserRoleRes> getUserRoles(
            @PathVariable("projectId") String projectId,
            @PathVariable("userId") String userId) {
        accessSettingsStorageService.getUserRolesForProjectIdAndUserId(projectId, userId?.toLowerCase())
    }

    @RequestMapping(value = "/projects/{projectId}/users/{userId}/roles/{roleName}", method = RequestMethod.DELETE)
    RequestResult deleteUserRole(
            @PathVariable("projectId") String projectId,
            @PathVariable("userId") String userId, @PathVariable("roleName") RoleName roleName) {
        accessSettingsStorageService.deleteUserRole(userId?.toLowerCase(), projectId, roleName)
        return new RequestResult(success: true)
    }

    @RequestMapping(value = "/projects/{projectId}/users/{userKey}/roles/{roleName}", method = [RequestMethod.PUT, RequestMethod.POST])
    RequestResult addUserRole(
            @PathVariable("projectId") String projectId,
            @PathVariable("userKey") String userKey, @PathVariable("roleName") RoleName roleName) {
        accessSettingsStorageService.addUserRole(getUserId(userKey), projectId, roleName)
        return new RequestResult(success: true)
    }

    private String getUserId(String userKey) {
        // userKey will be the userId when in FORM authMode, or the DN when in PKI auth mode.
        // When in PKI auth mode, the userDetailsService implementation will create the user
        // account if the user is not already a portal user (PkiUserDetailsService).
        // In the case of FORM authMode, the userKey is the userId and the user is expected
        // to already have a portal user account in the database
        if (authMode == skills.auth.AuthMode.PKI) {
            try {
                return userDetailsService.loadUserByUsername(userKey?.toLowerCase()).username
            } catch (UsernameNotFoundException e) {
                throw new skills.controller.exceptions.SkillException("User [$userKey] does not exist")
            }
        } else {
            return userKey?.toLowerCase()
        }
    }

}
