package skills.service.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.web.bind.annotation.*
import skills.service.auth.UserInfo
import skills.service.auth.UserInfoService
import skills.service.controller.exceptions.SkillException
import skills.service.controller.exceptions.SkillsValidator
import skills.service.datastore.services.AccessSettingsStorageService
import skills.storage.model.auth.AllowedOrigin
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole

@RestController
@RequestMapping("/admin")
@Slf4j
class AccessSettingsController {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserDetailsService userDetailsService

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @RequestMapping(value = "/projects/{projectId}/userRoles", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<UserRole> getUserRoles(@PathVariable("projectId") String projectId) {
        return accessSettingsStorageService.getUserRoles(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/users/{userId}/roles/{roleName}", method = RequestMethod.DELETE)
    void deleteUserRole(
            @PathVariable("projectId") String projectId,
            @PathVariable("userId") String userId, @PathVariable("roleName") RoleName roleName) {
        UserInfo toDelete = lookupUserInfo(userId)
        if (toDelete != userInfoService.currentUser) {
            accessSettingsStorageService.deleteUserRole(toDelete, projectId, roleName)
        } else {
            throw new SkillException("You cannot delete yourself.")
        }
    }

    @RequestMapping(value = "/projects/{projectId}/users/{userId}/roles/{roleName}", method = RequestMethod.PUT)
    UserRole addUserRole(
            @PathVariable("projectId") String projectId,
            @PathVariable("userId") String userId, @PathVariable("roleName") RoleName roleName) {
        accessSettingsStorageService.addUserRole(lookupUserInfo(userId), projectId, roleName)
    }

    @RequestMapping(value = "/projects/{projectId}/allowedOrigins", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<AllowedOrigin> getAllowedOrigins(@PathVariable("projectId") String projectId) {
        return accessSettingsStorageService.getAllowedOrigins(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/allowedOrigins", method = RequestMethod.PUT)
    AllowedOrigin saveOrUpdateAllowedOrigin(@PathVariable("projectId") String projectId, @RequestBody AllowedOrigin update) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isNotBlank(update.allowedOrigin, "Allowed Origin", projectId)
        SkillsValidator.isFirstOrMustEqualToSecond(update.projectId, projectId, "Project Id")

        return accessSettingsStorageService.saveOrUpdateAllowedOrigin(projectId, update)
    }

    @RequestMapping(value = "/projects/{projectId}/allowedOrigins/{allowedOriginId}", method = RequestMethod.DELETE)
    void deleteAllowedOrigin(
            @PathVariable("projectId") String projectId, @PathVariable("allowedOriginId") Integer allowedOriginId) {
        accessSettingsStorageService.deleteAllowedOrigin(projectId, allowedOriginId)
    }

    private UserInfo lookupUserInfo(String userId) {
        try {
            return userDetailsService.loadUserByUsername(userId)
        } catch (AuthenticationException e) {
            throw new SkillException(e.message)
        }
    }
}
