package skills.service.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*
import skills.service.auth.AuthMode
import skills.service.auth.pki.PkiUserLookup
import skills.service.controller.exceptions.SkillException
import skills.service.controller.exceptions.SkillsValidator
import skills.service.controller.request.model.SettingsRequest
import skills.service.controller.result.model.RequestResult
import skills.service.controller.result.model.SettingsResult
import skills.service.controller.result.model.UserInfoRes
import skills.service.datastore.services.AccessSettingsStorageService
import skills.service.datastore.services.settings.SettingsService
import skills.service.settings.EmailConnectionInfo
import skills.service.settings.EmailSettingsService
import skills.storage.model.auth.UserRole

import java.security.Principal

@RestController
@RequestMapping('/root')
@Slf4j
class RootController {

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    EmailSettingsService emailSettingsService

    @Value('#{securityConfig.authMode}}')
    AuthMode authMode = AuthMode.DEFAULT_AUTH_MODE

    @Autowired(required = false)
    PkiUserLookup pkiUserLookup

    @Autowired
    UserDetailsService userDetailsService

    @Autowired
    SettingsService settingsService

    @GetMapping('/rootUsers')
    @ResponseBody
    List<UserRole> getRootUsers() {
        return accessSettingsStorageService.getRootUsers()
    }

    @GetMapping('/users/{query}')
    @ResponseBody
    List<UserInfoRes> getNonRootUsers(@PathVariable('query') String query) {
        query = query.toLowerCase()
        if (authMode == AuthMode.FORM) {
            return accessSettingsStorageService.getNonRootUsers().findAll {
                it.userId.toLowerCase().contains(query)
            }.collect { new UserInfoRes(accessSettingsStorageService.findByUserIdIgnoreCase(it.userId)) }.unique()
        } else {
            List<String> rootUsers = accessSettingsStorageService.rootUsers.collect { it.userId.toLowerCase() }
            return pkiUserLookup?.suggestUsers(query)?.findAll {
                !rootUsers.contains(it.username.toLowerCase())
            }.unique().take(5).collect { new UserInfoRes(it) }
        }
    }

    @GetMapping('/isRoot')
    boolean isRoot(Principal principal) {
        return accessSettingsStorageService.isRoot(principal.name)
    }

    @PutMapping('/addRoot/{userKey}')
    RequestResult addRoot(@PathVariable('userKey') String userKey) {
        accessSettingsStorageService.addRoot(getUserId(userKey))
        return new RequestResult(success: true)
    }

    @DeleteMapping('/deleteRoot/{userId}')
    void deleteRoot(@PathVariable('userId') String userId) {
        SkillsValidator.isTrue(accessSettingsStorageService.getRootAdminCount() > 1, 'At least one root user must exist at all times! Deleting another user will cause no root users to exist!')
        accessSettingsStorageService.deleteRoot(userId)
    }

    @PostMapping('/testEmailSettings')
    boolean testEmailSettings(@RequestBody EmailConnectionInfo emailConnectionInfo) {
        return emailSettingsService.testConnection(emailConnectionInfo)
    }

    @PostMapping('/saveEmailSettings')
    void saveEmailSettings(@RequestBody EmailConnectionInfo emailConnectionInfo) {
        emailSettingsService.updateConnectionInfo(emailConnectionInfo)
    }

    @RequestMapping(value = "/global/settings/{setting}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult saveGlobalSetting(@PathVariable("setting") String setting, @RequestBody SettingsRequest settingRequest) {
        SkillsValidator.isNotBlank(setting, "Setting Id")
        SkillsValidator.isTrue(null == settingRequest.projectId, "Project Id must null for global settings")
        SkillsValidator.isTrue(setting == settingRequest.setting, "Setting Id must equal")

        SettingsResult existingSetting = settingsService.getSetting(settingRequest.projectId, settingRequest.setting, settingRequest.settingGroup)
        if (existingSetting?.id) {
            settingRequest.id = existingSetting.id
            log.info("Updating existing global setting [{}]", existingSetting)
        } else {
            log.info("Adding new global setting [{}]", settingRequest)
        }
        settingsService.saveSetting(settingRequest)
        return new RequestResult(success: true)
    }

    private String getUserId(String userKey) {
        // userKey will be the userId when in FORM authMode, or the DN when in PKI auth mode.
        // When in PKI auth mode, the userDetailsService implementation will create the user
        // account if the user is not already a portal user (PkiUserDetailsService).
        // In the case of FORM authMode, the userKey is the userId and the user is expected
        // to already have a portal user account in the database
        if (authMode == AuthMode.PKI) {
            try {
                return userDetailsService.loadUserByUsername(userKey).username
            } catch (UsernameNotFoundException e) {
                throw new SkillException("User [$userKey] does not exist")
            }
        } else {
            return userKey
        }
    }
}
