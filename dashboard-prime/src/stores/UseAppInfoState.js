/*
 * Copyright 2024 SkillTree
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

export const useAppInfoState = defineStore('useAppInfoState', () => {
  const previousUrlState = ref({})
  const setPreviousUrl = (url) => {
    previousUrlState.value = url
  }
  const previousUrl = computed(() => previousUrlState.value)

  const emailEnabledState = ref(false)
  const loadEmailEnabled = () => {
    return SettingsService.isEmailServiceSupported()
      .then((enabled) => {
        emailEnabledState.value = enabled
      })
  }
  const emailEnabled = computed(() => emailEnabledState.value)

  const showUaState = ref(false)
  const setShowUa = (newVal) => {
    showUaState.value = newVal
  }
  const showUa = computed(() => showUaState.value)

  return {
    previousUrl,
    setPreviousUrl,
    loadEmailEnabled,
    emailEnabled,
    showUa,
    setShowUa
  }

})