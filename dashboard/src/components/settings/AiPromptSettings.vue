/*
Copyright 2025 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import { onMounted, ref, computed } from 'vue'
import { object, string, boolean } from 'yup'
import { useForm } from 'vee-validate'
import SettingsService from '@/components/settings/SettingsService.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAiPromptState } from '@/common-components/utilities/learning-conent-gen/UseAiPromptState.js';
import AiPromptSettingsTextarea from '@/components/settings/AiPromptSettingsTextarea.vue'

const appConfig = useAppConfig()
const aiPrompts = useAiPromptState()
const loadingPrompts = ref(true)
const isLoading = computed(() => loadingPrompts.value || aiPrompts.isLoading)
const schema = object({
  systemInstructionsIsDefault: boolean(),
  systemInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('System Instructions'),
  newSkillDescriptionInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('New Skill Description'),
  newBadgeDescriptionInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('New Badge Description'),
  newSubjectDescriptionInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('New Subject Description'),
  newSkillGroupDescriptionInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('New Skill Group Description'),
  newProjectDescriptionInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('New Project Description'),
  newQuizDescriptionInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('New Quiz Description'),
  newSurveyDescriptionInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('New Survey Description'),
  existingDescriptionInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('Existing Skill Description'),
  followOnConvoInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('Follow on Conversation'),
  newQuizInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('New Quiz'),
  updateQuizInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('Update Quiz'),
  singleQuestionInstructionsMultipleChoice: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('New Quiz Question Multiple Answers'),
   singleQuestionInstructionsSingleChoice: string()
       .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('New Quiz Question Multiple Choice'),
  singleQuestionInstructionsTextInput: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('New Quiz Question Text Input'),
  singleQuestionInstructionsMatching: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('New Quiz Question Matching'),
  updateSingleQuestionTypeChangedToMultipleChoiceInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('Update Quiz Question to Multiple Choice'),
  updateSingleQuestionTypeChangedToSingleChoiceInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('Update Quiz Question to Single Choice'),
  updateSingleQuestionTypeChangedToTextInputInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('Update Quiz Question to Text Input'),
  updateSingleQuestionTypeChangedToMatchingInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('Update Quiz Question to Matching'),
  inputTextQuizGradingInstructions: string()
      .required()
      .max(appConfig.maxAiPromptLength)
      .noScript()
      .label('Input Text Quiz Grading Instructions'),
});

const { handleSubmit, setFieldValue, values, validate, resetField, resetForm, meta } = useForm({
  validationSchema: schema
});

const onSubmit = handleSubmit((values) => {
  saveAiPromptSettings(values).then(() => {
    resetForm({ values: { ...values } });
  });
});

const settingGroup = 'GLOBAL.AIPROMPTS';
const isSaving = ref(false);
const saveMessage = ref('');

onMounted(() => {
  loadAiPromptSettings()
});

const promptSettings = ref([]);
const loadAiPromptSettings = () => {
  loadingPrompts.value = true
  aiPrompts.afterPromptsLoaded().then(() => {
    convertFromSettings(aiPrompts.aiPromptSettings)
  }).finally(() => {
    loadingPrompts.value = false
  })
};
const convertFromSettings = (settings) => {
  const keys = Object.keys(settings);
  keys.forEach((key) => {
    setFieldValue(key, settings[key].value);
    setFieldValue(`${key}IsDefault`, settings[key].isDefault);
    promptSettings.value.push({
      setting: key,
      value: settings[key].value,
      label: settings[key].label,
      isDefault: settings[key].isDefault
    });
  });
  validate()
};

const resetToDefault = (name) => {
  SettingsService.getDefaultAiPromptSettings(name).then((response) => {
    setFieldValue(name, response);
    setFieldValue(`${name}IsDefault`, true);
  });
}
const updateSetting = (name) => {
  saveAiPromptSettings(values, name).then(() => {
    resetField(name, { value: values[name] })
  })
}
const saveAiPromptSettings = (values, name = null) => {
  isSaving.value = true
  saveMessage.value = ''
  const settings = convertToSettings(name? { [name]: values[name] } : values)
  return SettingsService.saveAiPromptSettings(settings).then((result) => {
    aiPrompts.updateAiPromptSettings(result)
    saveMessage.value = 'AI Prompt Settings Saved!'
  }).catch(() => {
    saveMessage.value = 'Failed to Save the AI Prompt Settings!'
  }).finally(() => {
    isSaving.value = false
  })
}
const convertToSettings = (values) => {
  const settings = []
  const keys = Object.keys(values);
  keys.forEach((key) => {
    // Skip any keys that end with 'IsDefault'
    if (!key.endsWith('IsDefault')) {
      settings.push({
        settingGroup,
        value: values[key],
        setting: `aiPrompt.${key}`
      });
    }
  });
  return settings;
};
</script>

<template>
  <div v-if="!isLoading" data-cy="aiPromptSettings">
    <AiPromptSettingsTextarea v-for="aiPromptSetting in promptSettings" :key="aiPromptSetting.setting"
                              :name="aiPromptSetting.setting"
                              :label="aiPromptSetting.label"
                              @update-setting="updateSetting"
                              @reset-to-default="resetToDefault" />

    <Message v-if="saveMessage" :sticky="false" :life="10000">{{saveMessage}}</Message>
    <InlineMessage v-if="meta.dirty"
                   severity="warn"
                   class="my-2"
                   data-cy="unsavedChangesAlert"
                   aria-label="Settings have been changed, do not forget to save">
      Unsaved Changes
    </InlineMessage>
    <SkillsButton v-on:click="onSubmit" :disabled="!meta.valid || !meta.dirty || isSaving" data-cy="aiPromptSettingsSave"
                  label="Save All" :icon="isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right'" >
    </SkillsButton>
  </div>
</template>

<style scoped>

</style>