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

    @Autowired
    SettingsService settingsService

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @RequestMapping(value = "/config", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    Map<String,Object> getConfig(){
        Map<String,String> res = new HashMap<>(uiConfigProperties.ui)
        res["authMode"] = authMode.name()
        res["needToBootstrap"] = !accessSettingsStorageService.rootAdminExists()
        res["oAuthOnly"] = authMode == AuthMode.FORM && oAuthOnly
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

    final private static Map statusRes = [
            status: "OK",
    ]

    @CrossOrigin
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
