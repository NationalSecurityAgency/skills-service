package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*
import skills.services.AccessSettingsStorageService
import skills.services.settings.SettingsService
import skills.settings.EmailConnectionInfo
import skills.settings.EmailSettingsService
import skills.storage.model.auth.UserRole

import java.security.Principal

@RestController
@RequestMapping('/root')
@Slf4j
@skills.profile.EnableCallStackProf
class RootController {

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    EmailSettingsService emailSettingsService

    @Value('#{securityConfig.authMode}}')
    skills.auth.AuthMode authMode = skills.auth.AuthMode.DEFAULT_AUTH_MODE

    @Autowired(required = false)
    skills.auth.pki.PkiUserLookup pkiUserLookup

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
    List<skills.controller.result.model.UserInfoRes> getNonRootUsers(@PathVariable('query') String query) {
        query = query.toLowerCase()
        if (authMode == skills.auth.AuthMode.FORM) {
            return accessSettingsStorageService.getNonRootUsers().findAll {
                it.userId.toLowerCase().contains(query)
            }.collect { new skills.controller.result.model.UserInfoRes(accessSettingsStorageService.findByUserIdIgnoreCase(it.userId)) }.unique()
        } else {
            List<String> rootUsers = accessSettingsStorageService.rootUsers.collect { it.userId.toLowerCase() }
            return pkiUserLookup?.suggestUsers(query)?.findAll {
                !rootUsers.contains(it.username.toLowerCase())
            }.unique().take(5).collect { new skills.controller.result.model.UserInfoRes(it) }
        }
    }

    @GetMapping('/isRoot')
    boolean isRoot(Principal principal) {
        return accessSettingsStorageService.isRoot(principal.name)
    }

    @PutMapping('/addRoot/{userKey}')
    skills.controller.result.model.RequestResult addRoot(@PathVariable('userKey') String userKey) {
        accessSettingsStorageService.addRoot(getUserId(userKey))
        return new skills.controller.result.model.RequestResult(success: true)
    }

    @DeleteMapping('/deleteRoot/{userId}')
    void deleteRoot(@PathVariable('userId') String userId) {
        skills.controller.exceptions.SkillsValidator.isTrue(accessSettingsStorageService.getRootAdminCount() > 1, 'At least one root user must exist at all times! Deleting another user will cause no root users to exist!')
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
    skills.controller.result.model.RequestResult saveGlobalSetting(@PathVariable("setting") String setting, @RequestBody skills.controller.request.model.SettingsRequest settingRequest) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(setting, "Setting Id")
        skills.controller.exceptions.SkillsValidator.isTrue(null == settingRequest.projectId, "Project Id must null for global settings")
        skills.controller.exceptions.SkillsValidator.isTrue(setting == settingRequest.setting, "Setting Id must equal")

        skills.controller.result.model.SettingsResult existingSetting = settingsService.getSetting(settingRequest.projectId, settingRequest.setting, settingRequest.settingGroup)
        if (existingSetting?.id) {
            settingRequest.id = existingSetting.id
            log.info("Updating existing global setting [{}]", existingSetting)
        } else {
            log.info("Adding new global setting [{}]", settingRequest)
        }
        settingsService.saveSetting(settingRequest)
        return new skills.controller.result.model.RequestResult(success: true)
    }

    private String getUserId(String userKey) {
        // userKey will be the userId when in FORM authMode, or the DN when in PKI auth mode.
        // When in PKI auth mode, the userDetailsService implementation will create the user
        // account if the user is not already a portal user (PkiUserDetailsService).
        // In the case of FORM authMode, the userKey is the userId and the user is expected
        // to already have a portal user account in the database
        if (authMode == skills.auth.AuthMode.PKI) {
            try {
                return userDetailsService.loadUserByUsername(userKey).username
            } catch (UsernameNotFoundException e) {
                throw new skills.controller.exceptions.SkillException("User [$userKey] does not exist")
            }
        } else {
            return userKey
        }
    }
}
