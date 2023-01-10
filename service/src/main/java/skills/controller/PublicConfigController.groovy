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
package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.bind.annotation.*
import skills.HealthChecker
import skills.UIConfigProperties
import skills.auth.AuthMode
import skills.controller.result.model.SettingsResult
import skills.profile.EnableCallStackProf
import skills.services.AccessSettingsStorageService
import skills.services.FeatureService
import skills.services.SystemSettingsService
import skills.services.settings.Settings
import skills.services.settings.SettingsService

@RestController
@RequestMapping("/public")
@Slf4j
@EnableCallStackProf
class PublicConfigController {

    @Autowired
    HealthChecker healthChecker

    @Autowired
    UIConfigProperties uiConfigProperties

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Value('${skills.authorization.authMode:#{T(skills.auth.AuthMode).DEFAULT_AUTH_MODE}}')
    AuthMode authMode

    @Value('#{"${skills.authorization.oAuthOnly:false}"}')
    Boolean oAuthOnly

    @Value('#{"${skills.config.expirationGracePeriod:7}"}')
    int expirationGracePeriod

    @Value('#{"${skills.config.expireUnusedProjectsOlderThan:180}"}')
    int expireUnusedProjectsOlderThan

    @Value('${skills.config.ui.enablePageVisitReporting:#{false}}')
    Boolean enablePageVisitReporting

    @Autowired
    SettingsService settingsService

    @Autowired
    FeatureService featureService

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @RequestMapping(value = "/config", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    Map<String,Object> getConfig(){
        Map<String,String> res = new HashMap<>(uiConfigProperties.ui)
        res["authMode"] = authMode.name()
        res["needToBootstrap"] = !accessSettingsStorageService.rootAdminExists()
        res["oAuthOnly"] = authMode == AuthMode.FORM && oAuthOnly
        res["verifyEmailAddresses"] = featureService.isEmailVerificationFeatureEnabled()
        res["expirationGracePeriod"] = expirationGracePeriod
        res["expireUnusedProjectsOlderThan"] = expireUnusedProjectsOlderThan
        List<SettingsResult> customizationSettings = settingsService.getGlobalSettingsByGroup(SystemSettingsService.CUSTOMIZATION)
        customizationSettings?.each {
            if (Settings.GLOBAL_CUSTOM_HEADER.settingName == it.setting) {
                res["customHeader"] = it.value
            } else if (Settings.GLOBAL_CUSTOM_FOOTER.settingName == it.setting) {
                res["customFooter"] = it.value
            }
        }
        def oAuthProviders = clientRegistrationRepository?.collect {it?.registrationId }
        if (oAuthProviders) {
            res['oAuthProviders'] = oAuthProviders
        }
        return res
    }

    @CrossOrigin(originPatterns = ['*'])
    @RequestMapping(value = "/clientDisplay/config", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    Map<String,Object> getClientDisplayConfig(@RequestParam(required = false) String projectId){
        String docsHost = uiConfigProperties.ui.docsHost
        Map<String,String> res = new HashMap<>()
        res["docsHost"] = docsHost
        res["maxSelfReportMessageLength"] = uiConfigProperties.ui.maxSelfReportMessageLength
        res["allowedAttachmentFileTypes"] = uiConfigProperties.ui.allowedAttachmentFileTypes
        res["allowedAttachmentMimeTypes"] = uiConfigProperties.ui.allowedAttachmentMimeTypes
        res["maxAttachmentSize"] = uiConfigProperties.ui.maxAttachmentSize
        res["attachmentWarningMessage"] = uiConfigProperties.ui.attachmentWarningMessage
        res["descriptionMaxLength"] = uiConfigProperties.ui.descriptionMaxLength
        res["paragraphValidationRegex"] = uiConfigProperties.ui.paragraphValidationRegex
        res["paragraphValidationMessage"] = uiConfigProperties.ui.paragraphValidationMessage
        res["projectDisplayName"] = 'Project'
        res["subjectDisplayName"] = 'Subject'
        res["groupDisplayName"] = 'Group'
        res["skillDisplayName"] = 'Skill'
        res["levelDisplayName"] = 'Level'
        res["groupDescriptionsOn"] = false
        res["displayProjectDescription"] = true
        if (Boolean.valueOf(uiConfigProperties.dbUpgradeInProgress)) {
            res["dbUpgradeInProgress"] = uiConfigProperties.dbUpgradeInProgress
        }
        if (projectId) {
            Map<String, String> projectSettings = settingsService.getProjectSettings(projectId, [
                    'project.displayName',
                    'subject.displayName',
                    'group.displayName',
                    'skill.displayName',
                    'level.displayName',
                    Settings.GROUP_DESCRIPTIONS.settingName,
                    Settings.SHOW_PROJECT_DESCRIPTION_EVERYWHERE.settingName
            ])?.collectEntries {
                [it.setting, it.value]
            }

            String customProjectName = projectSettings?['project.displayName']
            if (customProjectName) {
                res["projectDisplayName"] = customProjectName
            }
            String customSubjectName = projectSettings?['subject.displayName']
            if (customSubjectName) {
                res["subjectDisplayName"] = customSubjectName
            }
            String customGroupName = projectSettings?['group.displayName']
            if (customGroupName) {
                res["groupDisplayName"] = customGroupName
            }
            String customSkillName = projectSettings?['skill.displayName']
            if (customSkillName) {
                res["skillDisplayName"] = customSkillName
            }
            String customLevelName = projectSettings?['level.displayName']
            if (customLevelName) {
                res["levelDisplayName"] = customLevelName
            }
            Boolean groupDescriptionsOn = Boolean.valueOf(projectSettings?[Settings.GROUP_DESCRIPTIONS.settingName])
            if (groupDescriptionsOn) {
                res["groupDescriptionsOn"] = groupDescriptionsOn
            }
            Boolean showDescription = Boolean.valueOf(projectSettings?[Settings.SHOW_PROJECT_DESCRIPTION_EVERYWHERE.settingName])
            if (showDescription) {
                res["displayProjectDescription"] = true
            } else {
                res["displayProjectDescription"] = false
            }

        }
        res['enablePageVisitReporting'] = enablePageVisitReporting
        return res
    }

    final private static Map statusRes = [
            status: "OK",
    ]

    @CrossOrigin(originPatterns = ['*'])
    @RequestMapping(value = "/status", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    def status() {
        healthChecker.checkRequiredServices()
        Map<String,String> res = new HashMap<>(statusRes)
        res['clientLib'] = uiConfigProperties.client
        def oAuthProviders = clientRegistrationRepository?.collect {it?.registrationId }
        if (oAuthProviders) {
            res['oAuthProviders'] = oAuthProviders
        }
        return res
    }

    @RequestMapping(value = "/isAlive", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    def isAlive() {
        healthChecker.checkRequiredServices(false)
        Map<String,String> res = new HashMap<>(statusRes)
        return res
    }
}
