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
package skills.settings

import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import skills.services.openai.OpenAIService
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.GlobalSettingsRequest
import skills.controller.request.model.SettingsRequest
import skills.controller.result.model.SettingsResult
import skills.services.settings.SettingsService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.utils.InputSanitizer

import java.lang.reflect.Modifier

@Service
@Slf4j
@ConditionalOnProperty(
        name = "skills.db.startup",
        havingValue = "true",
        matchIfMissing = true)
class AiPromptSettingsService {

    static final String settingsGroup = 'GLOBAL.AIPROMPTS'

    static final String systemInstructions = 'aiPrompt.systemInstructions'
    static final String newSkillDescriptionInstructions = 'aiPrompt.newSkillDescriptionInstructions'
    static final String newBadgeDescriptionInstructions = 'aiPrompt.newBadgeDescriptionInstructions'
    static final String newSubjectDescriptionInstructions = 'aiPrompt.newSubjectDescriptionInstructions'
    static final String newSkillGroupDescriptionInstructions = 'aiPrompt.newSkillGroupDescriptionInstructions'
    static final String newProjectDescriptionInstructions = 'aiPrompt.newProjectDescriptionInstructions'
    static final String newQuizDescriptionInstructions = 'aiPrompt.newQuizDescriptionInstructions'
    static final String newSurveyDescriptionInstructions = 'aiPrompt.newSurveyDescriptionInstructions'
    static final String existingDescriptionInstructions = 'aiPrompt.existingDescriptionInstructions'
    static final String followOnConvoInstructions = 'aiPrompt.followOnConvoInstructions'
    static final String newQuizInstructions = 'aiPrompt.newQuizInstructions'
    static final String updateQuizInstructions = 'aiPrompt.updateQuizInstructions'
    static final String singleQuestionInstructionsMultipleChoice = 'aiPrompt.singleQuestionInstructionsMultipleChoice'
    static final String singleQuestionInstructionsSingleChoice = 'aiPrompt.singleQuestionInstructionsSingleChoice'
    static final String singleQuestionInstructionsTextInput = 'aiPrompt.singleQuestionInstructionsTextInput'
    static final String singleQuestionInstructionsMatching = 'aiPrompt.singleQuestionInstructionsMatching'
    static final String updateSingleQuestionTypeToMultipleChoiceChangedInstructions = 'aiPrompt.updateSingleQuestionTypeChangedToMultipleChoiceInstructions'
    static final String updateSingleQuestionTypeChangedToSingleChoiceInstructions = 'aiPrompt.updateSingleQuestionTypeChangedToSingleChoiceInstructions'
    static final String updateSingleQuestionTypeChangedToTextInputInstructions = 'aiPrompt.updateSingleQuestionTypeChangedToTextInputInstructions'
    static final String updateSingleQuestionTypeChangedToMatchingInstructions = 'aiPrompt.updateSingleQuestionTypeChangedToMatchingInstructions'
    static final String inputTextQuizGradingInstructions = 'aiPrompt.inputTextQuizGradingInstructions'

    @Autowired
    SettingsService settingsService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    OpenAIService openAIService

    @PostConstruct
    void init() {
        updateOpenAiSystemMsg()
    }

    AiPromptSettings fetchAiPromptSettings() {
        List<SettingsResult> aiPromptSettings = settingsService.getGlobalSettingsByGroup(settingsGroup)
        return convert(aiPromptSettings)
    }

    AiPromptSettings updateAiPromptsSettings(List<GlobalSettingsRequest> aiPromptSettings) {
        aiPromptSettings.each {
            it.settingGroup = settingsGroup
        }
        Map<String, String> aiPromptSettingsMap = aiPromptSettings.collectEntries { setting ->
            String key = setting.setting.replaceAll(/^aiPrompt\./, '')
            [(setting.setting): setting.value == AiPromptSettingDefaults."${key}" ? null : setting.value]
        }
        storeSettings(aiPromptSettingsMap)
        updateOpenAiSystemMsg()  // update system message for openai as it may have changed
        Map actionAttributes = aiPromptSettings.properties
                .findAll { key, val -> key != "password" && val instanceof String || val instanceof Number }
                .collectEntries { key, val -> [(key): val] }
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Create,
                item: DashboardItem.Settings,
                actionAttributes: actionAttributes,
                itemId: "AiPromptSettings",
        ))
        return fetchAiPromptSettings()
    }

    static String getDefaultSetting(String setting){
        def propertyNames = AiPromptSettingDefaults.declaredFields
                .findAll { !it.synthetic && (Modifier.isPublic(it.modifiers) || Modifier.isStatic(it.modifiers)) }
                .collect { it.name }
        SkillsValidator.isTrue(propertyNames.contains(setting), "Invalid setting: ${setting}")
        return AiPromptSettingDefaults."${setting}"
    }

    private void storeSettings(Map<String, String> aiPromptSettingsMap) {
        saveOrUpdateGlobalGroup(settingsGroup, aiPromptSettingsMap)
    }

    static AiPromptSettings convert(List<SettingsResult> aiPromptGroupSettings) {
        AiPromptSettings info = new AiPromptSettings()

        // Initialize all settings with default values
        info.systemInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.systemInstructions, label: 'System Instructions', isDefault: true)
        info.newSkillDescriptionInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.newSkillDescriptionInstructions, label: 'New Skill Description', isDefault: true)
        info.newBadgeDescriptionInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.newBadgeDescriptionInstructions, label: 'New Badge Description', isDefault: true)
        info.newSubjectDescriptionInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.newSubjectDescriptionInstructions, label: 'New Subject Description', isDefault: true)
        info.newSkillGroupDescriptionInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.newSkillGroupDescriptionInstructions, label: 'New Skill Group Description', isDefault: true)
        info.newProjectDescriptionInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.newProjectDescriptionInstructions, label: 'New Project Description', isDefault: true)
        info.newQuizDescriptionInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.newQuizDescriptionInstructions, label: 'New Quiz Description', isDefault: true)
        info.newSurveyDescriptionInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.newSurveyDescriptionInstructions, label: 'New Survey Description', isDefault: true)
        info.existingDescriptionInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.existingDescriptionInstructions, label: 'Existing Description', isDefault: true)
        info.followOnConvoInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.followOnConvoInstructions, label: 'Follow on Conversation', isDefault: true)
        info.newQuizInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.newQuizInstructions, label: 'New Quiz', isDefault: true)
        info.updateQuizInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.updateQuizInstructions, label: 'Update Quiz', isDefault: true)
        info.singleQuestionInstructionsMultipleChoice = new AiPromptSetting(value: AiPromptSettingDefaults.singleQuestionInstructionsMultipleChoice, label: 'New Single Question Multiple Choice', isDefault: true)
        info.singleQuestionInstructionsSingleChoice = new AiPromptSetting(value: AiPromptSettingDefaults.singleQuestionInstructionsSingleChoice, label: 'New Single Question Single Choice', isDefault: true)
        info.singleQuestionInstructionsTextInput = new AiPromptSetting(value: AiPromptSettingDefaults.singleQuestionInstructionsTextInput, label: 'New Single Question Text Input', isDefault: true)
        info.singleQuestionInstructionsMatching = new AiPromptSetting(value: AiPromptSettingDefaults.singleQuestionInstructionsMatching, label: 'New Single Question Matching', isDefault: true)
        info.updateSingleQuestionTypeChangedToMultipleChoiceInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.updateSingleQuestionTypeChangedToMultipleChoiceInstructions, label: 'Update Single Question Type to Multiple Choice', isDefault: true)
        info.updateSingleQuestionTypeChangedToSingleChoiceInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.updateSingleQuestionTypeChangedToSingleChoiceInstructions, label: 'Update Single Question Type to Single Choice', isDefault: true)
        info.updateSingleQuestionTypeChangedToTextInputInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.updateSingleQuestionTypeChangedToTextInputInstructions, label: 'Update Single Question Type to Text Input', isDefault: true)
        info.updateSingleQuestionTypeChangedToMatchingInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.updateSingleQuestionTypeChangedToMatchingInstructions, label: 'Update Single Question Type to Matching', isDefault: true)
        info.inputTextQuizGradingInstructions = new AiPromptSetting(value: AiPromptSettingDefaults.inputTextQuizGradingInstructions, label: 'Input Text Quiz Grading Instructions', isDefault: true)

        if (aiPromptGroupSettings) {
            Map<String, String> mappedSettings = aiPromptGroupSettings.collectEntries { [it.setting, it.value] }

            def updateSetting = { String settingName, String settingKey ->
                if (mappedSettings[settingKey] != null) {
                    info[settingName].value = InputSanitizer.unsanitizeForMarkdown(mappedSettings[settingKey])
                    info[settingName].isDefault = false
                }
            }

            // Update each setting if it exists in the mapped settings
            updateSetting('systemInstructions', systemInstructions)
            updateSetting('newSkillDescriptionInstructions', newSkillDescriptionInstructions)
            updateSetting('newBadgeDescriptionInstructions', newBadgeDescriptionInstructions)
            updateSetting('newSubjectDescriptionInstructions', newSubjectDescriptionInstructions)
            updateSetting('newSkillGroupDescriptionInstructions', newSkillGroupDescriptionInstructions)
            updateSetting('newProjectDescriptionInstructions', newProjectDescriptionInstructions)
            updateSetting('newQuizDescriptionInstructions', newQuizDescriptionInstructions)
            updateSetting('newSurveyDescriptionInstructions', newSurveyDescriptionInstructions)
            updateSetting('existingDescriptionInstructions', existingDescriptionInstructions)
            updateSetting('followOnConvoInstructions', followOnConvoInstructions)
            updateSetting('newQuizInstructions', newQuizInstructions)
            updateSetting('updateQuizInstructions', updateQuizInstructions)
            updateSetting('singleQuestionInstructionsMultipleChoice', singleQuestionInstructionsMultipleChoice)
            updateSetting('singleQuestionInstructionsSingleChoice', singleQuestionInstructionsSingleChoice)
            updateSetting('singleQuestionInstructionsTextInput', singleQuestionInstructionsTextInput)
            updateSetting('singleQuestionInstructionsMatching', singleQuestionInstructionsMatching)
            updateSetting('updateSingleQuestionTypeChangedToMultipleChoiceInstructions', updateSingleQuestionTypeToMultipleChoiceChangedInstructions)
            updateSetting('updateSingleQuestionTypeChangedToSingleChoiceInstructions', updateSingleQuestionTypeChangedToSingleChoiceInstructions)
            updateSetting('updateSingleQuestionTypeChangedToTextInputInstructions', updateSingleQuestionTypeChangedToTextInputInstructions)
            updateSetting('updateSingleQuestionTypeChangedToMatchingInstructions', updateSingleQuestionTypeChangedToMatchingInstructions)
            updateSetting('inputTextQuizGradingInstructions', inputTextQuizGradingInstructions)
        }

        return info
    }

    private void saveOrUpdateGlobalGroup(String settingsGroup, Map<String, String> settingsMap) {
        List<SettingsRequest> settingsRequests = []
        List<SettingsRequest> deleteIfExist = []
        settingsMap.each { String setting, String value ->
            GlobalSettingsRequest gsr = new GlobalSettingsRequest(settingGroup: settingsGroup, setting: setting, value: InputSanitizer.sanitizeDescription(value))
            if (value) {
                settingsRequests << gsr
            } else {
                // if there's no value specified, we need to check if that setting previously had a value and delete it
                deleteIfExist << gsr
            }
        }
        settingsService.saveSettings(settingsRequests)
        deleteIfExist.each {
            settingsService.deleteGlobalSetting(it.setting)
        }
    }

    private void updateOpenAiSystemMsg() {
        AiPromptSettings aiPromptSettings = fetchAiPromptSettings()
        openAIService.systemMsg = aiPromptSettings.systemInstructions.value
    }
}
