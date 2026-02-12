/**
 * Copyright 2025 SkillTree
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
package skills.intTests.ai

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.services.openai.OpenAIService
import skills.controller.result.model.SettingsResult
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.settings.SettingsService
import skills.settings.AiPromptSettingDefaults
import skills.settings.AiPromptSettingsService

class AiPromptSettingsSpecs extends DefaultIntSpec {
    SkillsService rootSkillsService

    @Autowired
    OpenAIService openAIService

    @Autowired
    SettingsService settingsService

    def setup() {
        rootSkillsService = createRootSkillService()
        List<SettingsResult> aiPromptSettings = settingsService.getGlobalSettingsByGroup(AiPromptSettingsService.settingsGroup)
        aiPromptSettings.each { it ->
            settingsService.deleteGlobalSetting(it.setting)
        }
    }

    def "all ai prompts are set to their default values before being updated"() {
        when:
        def aiPromptSettings = rootSkillsService.getAiPromptSettings()

        then:
        aiPromptSettings
        aiPromptSettings.each { key, value ->
            assert value.isDefault == true
            assert value.value == AiPromptSettingDefaults."${key}".replaceAll(/^aiPrompt\./, '')
        }
    }
    
    def "root user can call call ai prompt settings endpoints"() {
        when: true
        then:
        !validateForbidden { rootSkillsService.getAiPromptSettings() }
        !validateForbidden { rootSkillsService.getDefaultAiPromptSetting(AiPromptSettingsService.newSkillDescriptionInstructions) }
        !validateForbidden { rootSkillsService.saveAiPromptSettings([
                [setting: AiPromptSettingsService.systemInstructions, value: "This is an updated systemMsg 1", settingGroup: AiPromptSettingsService.settingsGroup]
        ])}
    }

    def "non-root user can only call get ai prompt settings endpoints"() {
        when: true
        then:
        !validateForbidden { skillsService.getAiPromptSettings() }
        validateForbidden { skillsService.getDefaultAiPromptSetting(AiPromptSettingsService.newSkillDescriptionInstructions) }
        validateForbidden { skillsService.saveAiPromptSettings([
                [setting: AiPromptSettingsService.systemInstructions, value: "This is an updated systemMsg 2", settingGroup: AiPromptSettingsService.settingsGroup]
        ])}
    }

    def "systemInstructions changes are reflected in the OpenAIService bean"() {
        String systemMsgBeforeUpdate = openAIService.systemMsg
        when:
        rootSkillsService.saveAiPromptSettings([
            [setting: AiPromptSettingsService.systemInstructions, value: "This is an updated systemMsg 3", settingGroup: AiPromptSettingsService.settingsGroup]
        ])
        String systemMsgAfterUpdate = openAIService.systemMsg

        then:
        systemMsgBeforeUpdate
        systemMsgAfterUpdate
        systemMsgBeforeUpdate != systemMsgAfterUpdate
        systemMsgAfterUpdate == "This is an updated systemMsg 3"
    }

    def "prevent inject into ai prompt settings"() {
        when:
        def setting = [
            setting: AiPromptSettingsService.systemInstructions,
            value: "this is a description <a href='http://somewhere' onclick='doNefariousStuff()'>I'm a link</a>",
            settingGroup: AiPromptSettingsService.settingsGroup
        ]
        rootSkillsService.saveAiPromptSettings([setting])
        def aiPromptSettingsAfter = skillsService.getAiPromptSettings()

        then:
        aiPromptSettingsAfter
        aiPromptSettingsAfter.systemInstructions.value == 'this is a description <a href="http://somewhere">I\'m a link</a>'
        aiPromptSettingsAfter.systemInstructions.isDefault == false
    }

    private static boolean validateForbidden(Closure c) {
        try {
            c.call()
            return false
        } catch (SkillsClientException skillsClientException) {
            return skillsClientException.httpStatus == HttpStatus.FORBIDDEN
        }
    }

}
