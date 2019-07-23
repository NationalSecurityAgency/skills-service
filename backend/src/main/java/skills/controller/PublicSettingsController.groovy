package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import skills.services.settings.SettingsService

@RestController
@RequestMapping("/app/public")
@Slf4j
@skills.profile.EnableCallStackProf
class PublicSettingsController {

    @Autowired
    SettingsService settingsService

    @RequestMapping(value = "/settings/group/{settingGroup}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<skills.controller.result.model.SettingsResult> getPublicSettings(@PathVariable("settingGroup") String settingGroup) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(settingGroup, "Setting Group")
        skills.controller.exceptions.SkillsValidator.isTrue(settingGroup.startsWith("public_"), "Setting Group [$settingGroup] must be prefixed with 'public_'")
        return settingsService.loadSettingsByType(null, settingGroup)
    }

    @RequestMapping(value = "/settings/{setting}/group/{settingGroup}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    skills.controller.result.model.SettingsResult getPublicSetting(@PathVariable("setting") String setting, @PathVariable("settingGroup") String settingGroup) {
        skills.controller.exceptions.SkillsValidator.isNotBlank(setting, "Setting Id")
        skills.controller.exceptions.SkillsValidator.isNotBlank(settingGroup, "Setting Group")
        skills.controller.exceptions.SkillsValidator.isTrue(settingGroup.startsWith("public_"), "Setting Group [$settingGroup] must be prefixed with 'public_'")
        return settingsService.getSetting(null, setting, settingGroup)
    }
}
