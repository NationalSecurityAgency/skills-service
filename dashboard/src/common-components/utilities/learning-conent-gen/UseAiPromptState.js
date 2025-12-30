/*
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
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import SettingsService from '@/components/settings/SettingsService.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

export const useAiPromptState = defineStore('UseAiPromptState', () => {
  const loading = ref(true)
  const appConfig = useAppConfig()
  const aiPromptSettingsState = ref({})
  const loadAiPromptSettings = () => {
    if (appConfig.enableOpenAIIntegration) {
      loading.value = true
      return SettingsService.getAiPromptSettings().then((response) => {
        aiPromptSettingsState.value = response
      }).finally(() => {
        loading.value = false
      })
    } else {
      aiPromptSettingsState.value = {}
      loading.value = false
      return Promise.resolve({})
    }
  }

  const isLoading = computed(() => loading.value)

  const aiPromptSettings = computed(() => aiPromptSettingsState.value)

  const getAiPromptSetting = (name) => {
    return aiPromptSettings.value[name]
  }

  const updateAiPromptSetting = (name, value) => {
    aiPromptSettingsState.value[name] = value
  }

  const updateAiPromptSettings = (settings) => {
    aiPromptSettingsState.value = settings
  }
  const afterPromptsLoaded = () => {
    return new Promise((resolve) => {
      (function waitForLoading() {
        if (!isLoading.value) return resolve(aiPromptSettingsState.value);
        setTimeout(waitForLoading, 100);
        return isLoading.value;
      }());
    });
  }

  return {
    loadAiPromptSettings,
    aiPromptSettings,
    getAiPromptSetting,
    updateAiPromptSettings,
    isLoading,
    afterPromptsLoaded,
  }

})