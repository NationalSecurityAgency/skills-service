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
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.bind.annotation.*
import skills.HealthChecker
import skills.UIConfigProperties
import skills.auth.AuthMode
import skills.auth.UserInfoService
import skills.controller.result.model.SettingsResult
import skills.icons.IconSetsIndexService
import skills.profile.EnableCallStackProf
import skills.services.AccessSettingsStorageService
import skills.services.FeatureService
import skills.services.SystemSettingsService
import skills.services.admin.ProjAdminService
import skills.services.admin.UserCommunityService
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

    @Value('${spring.security.saml2.registrationId:#{null}}')
    String regId

    @Autowired
    SettingsService settingsService

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    FeatureService featureService

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserCommunityService userCommunityService

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository

    @Autowired
    IconSetsIndexService iconSetsIndexService

    @RequestMapping(value = "/config", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    Map<String,Object> getConfig(){
        Map<String,Object> res = new HashMap<>(uiConfigProperties.ui)
        configureUserCommunityProps(res)
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

        if(authMode.name() == 'SAML2'){
            res['saml2RegistrationId'] = regId
        }

        updatedSpringListProps(res, "openaiTakingLongerThanExpectedMessages")

        return res
    }

    /**
     * @param prefix
     * @param currentProps - will be mutated in place
     * @return
     */
    private static void updatedSpringListProps(Map<String,Object> currentProps, String prefix) {
        String normalizeKey = prefix
        List<String> listOfValues = currentProps.findAll { it.key.startsWith(normalizeKey) }.sort { it.key }.collect { it.value?.toString() }
        currentProps.removeAll { it.key.startsWith(normalizeKey) }
        currentProps[normalizeKey] = listOfValues
    }

    @CrossOrigin(originPatterns = ['*'])
    @RequestMapping(value = "/clientDisplay/config", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    Map<String,Object> getClientDisplayConfig(@RequestParam(required = false) String projectId){
        String docsHost = uiConfigProperties.ui.docsHost
        Map<String,Object> res = new HashMap<>()
        configureUserCommunityProps(res)
        res["docsHost"] = docsHost
        res["maxSelfReportMessageLength"] = uiConfigProperties.ui.maxSelfReportMessageLength
        res["allowedAttachmentFileTypes"] = uiConfigProperties.ui.allowedAttachmentFileTypes
        res["allowedAttachmentMimeTypes"] = uiConfigProperties.ui.allowedAttachmentMimeTypes
        res["maxAttachmentSize"] = uiConfigProperties.ui.maxAttachmentSize
        res["attachmentWarningMessage"] = uiConfigProperties.ui.attachmentWarningMessage
        res["descriptionMaxLength"] = uiConfigProperties.ui.descriptionMaxLength
        res["paragraphValidationRegex"] = uiConfigProperties.ui.paragraphValidationRegex
        res["paragraphValidationMessage"] = uiConfigProperties.ui.paragraphValidationMessage
        res["motivationalSkillWarningGracePeriod"] = uiConfigProperties.ui.motivationalSkillWarningGracePeriod
        res["projectDisplayName"] = 'Project'
        res["subjectDisplayName"] = 'Subject'
        res["groupDisplayName"] = 'Group'
        res["skillDisplayName"] = 'Skill'
        res["levelDisplayName"] = 'Level'
        res["pointDisplayName"] = 'Point'
        res["groupDescriptionsOn"] = false
        res["groupInfoOnSkillPage"] = false
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
                    'point.displayName',
                    Settings.GROUP_DESCRIPTIONS.settingName,
                    Settings.SHOW_PROJECT_DESCRIPTION_EVERYWHERE.settingName,
                    Settings.GROUP_INFO_ON_SKILL_PAGE.settingName,
                    Settings.SHOW_PROJECT_DESCRIPTION_EVERYWHERE.settingName,
                    Settings.DISABLE_SKILLS_DISPLAY_ACHIEVEMENTS_CELEBRATIONS.settingName
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
            String customPointName = projectSettings?['point.displayName']
            if (customPointName) {
                res["pointDisplayName"] = customPointName
            }
            Boolean groupDescriptionsOn = Boolean.valueOf(projectSettings?[Settings.GROUP_DESCRIPTIONS.settingName])
            if (groupDescriptionsOn) {
                res["groupDescriptionsOn"] = groupDescriptionsOn
            }
            Boolean groupInfoOnSkillPage = Boolean.valueOf(projectSettings?[Settings.GROUP_INFO_ON_SKILL_PAGE.settingName])
            if (groupInfoOnSkillPage) {
                res["groupInfoOnSkillPage"] = groupInfoOnSkillPage
            }
            Boolean showDescription = Boolean.valueOf(projectSettings?[Settings.SHOW_PROJECT_DESCRIPTION_EVERYWHERE.settingName])
            if (showDescription) {
                res["displayProjectDescription"] = true
            } else {
                res["displayProjectDescription"] = false
            }

            Boolean disableAchievementsCelebrations = Boolean.valueOf(projectSettings?[Settings.DISABLE_SKILLS_DISPLAY_ACHIEVEMENTS_CELEBRATIONS.settingName])
            res["disableAchievementsCelebrations"] = disableAchievementsCelebrations

            String name = projAdminService.lookupProjectName(projectId)
            res["projectName"] = name

            if (userCommunityService.isUserCommunityConfigured()) {
                res["projectUserCommunityDescriptor"] = userCommunityService.getProjectUserCommunity(projectId)
            }
        }
        res['enablePageVisitReporting'] = enablePageVisitReporting
        return res
    }

    private configureUserCommunityProps(Map<String,String> res) {
        Boolean belongsToUserCommunity = userCommunityService.isUserCommunityMember(userInfoService.currentUserId)
        res['currentUsersCommunityDescriptor'] = belongsToUserCommunity ?
                uiConfigProperties.ui.userCommunityRestrictedDescriptor :
                uiConfigProperties.ui.defaultCommunityDescriptor
        if (!belongsToUserCommunity) {
            // remove all userCommunity keys
            Set<String> userCommunityKeys = res.keySet().findAll { StringUtils.startsWith(it, 'userCommunity') }
            userCommunityKeys.each { res.remove(it)}

            // remove supportLinks that are protected by user community
            String protectedConfigEnd = "userCommunityProtected"
            List<String> propsToRemove = res.findAll {
                it.key.endsWith(protectedConfigEnd) && it.value?.equalsIgnoreCase("true")
            }.collect { it.key.replaceAll(/${protectedConfigEnd}$/, "") }
            List<String> keysToRemove = res.findAll { Map.Entry<String, String> entry ->
                propsToRemove.find { entry.key.startsWith(it) }
            }.collect { it.key }
            keysToRemove.each { res.remove(it) }
        }
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

    @GetMapping('/iconSetIndexes')
    @ResponseBody
    Map getIconSetsIndexes() {
        iconSetsIndexService.getIconSetsIndexes()
    }
}
