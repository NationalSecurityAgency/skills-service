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
import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useAppVersionState = defineStore('appVersionState', () => {
  const latestLibVersion = ref('')
  const initialLibVersion = ref('')

  const updateLatestLibVersion = (incomingVersion) => {
    if (!initialLibVersion.value) {
      initialLibVersion.value = incomingVersion
    }
    if (!latestLibVersion.value || incomingVersion.localeCompare(latestLibVersion.value) > 0) {
      latestLibVersion.value = incomingVersion
    }
  }
  const isVersionDifferent = computed(() => {
    return initialLibVersion.value !== latestLibVersion.value
  })

  return {
    updateLatestLibVersion,
    isVersionDifferent
  }
})