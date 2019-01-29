package skills.service.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.service.auth.UserInfo
import skills.service.auth.UserInfoService
import skills.storage.model.auth.AllowedOrigin
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.service.datastore.services.AccessSettingsStorageService

@RestController
@RequestMapping("/admin")
@Slf4j
class AccessSettingsController {


    @Autowired
    UserInfoService userInfoService

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @RequestMapping(value = "/projects/{projectId}/userRoles", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<UserRole> getUserRoles(@PathVariable("projectId") String projectId) {
        return accessSettingsStorageService.getUserRoles(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/users/{userDn}/roles/{roleName}", method = RequestMethod.DELETE)
    void deleteUserRole(
            @PathVariable("projectId") String projectId,
            @PathVariable("userDn") String userDn, @PathVariable("roleName") RoleName roleName) {
        accessSettingsStorageService.deleteUserRole(lookupUserInfo(userDn), projectId, roleName)
    }

    @RequestMapping(value = "/projects/{projectId}/users/{userDn}/roles/{roleName}", method = RequestMethod.PUT)
    UserRole createUserRole(
            @PathVariable("projectId") String projectId,
            @PathVariable("userDn") String userDn, @PathVariable("roleName") RoleName roleName) {
        accessSettingsStorageService.addUserRole(lookupUserInfo(userDn), projectId, roleName)
    }

    @RequestMapping(value = "/projects/{projectId}/allowedOrigins", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<AllowedOrigin> getAllowedOrigins(@PathVariable("projectId") String projectId) {
        return accessSettingsStorageService.getAllowedOrigins(projectId)
    }

    @RequestMapping(value = "/projects/{projectId}/allowedOrigins", method = RequestMethod.PUT)
    AllowedOrigin saveOrUpdateAllowedOrigin(@PathVariable("projectId") String projectId, @RequestBody AllowedOrigin update) {
        assert update
        assert update.allowedOrigin
        assert update.projectId
        assert update.projectId == projectId

        return accessSettingsStorageService.saveOrUpdateAllowedOrigin(projectId, update)
    }

    @RequestMapping(value = "/projects/{projectId}/allowedOrigins/{allowedOriginId}", method = RequestMethod.DELETE)
    void deleteAllowedOrigin(
            @PathVariable("projectId") String projectId, @PathVariable("allowedOriginId") Integer allowedOriginId) {
        accessSettingsStorageService.deleteAllowedOrigin(projectId, allowedOriginId)
    }

    private UserInfo lookupUserInfo(String userDn) {
        return userInfoService.lookupUser(userDn)
    }
}
