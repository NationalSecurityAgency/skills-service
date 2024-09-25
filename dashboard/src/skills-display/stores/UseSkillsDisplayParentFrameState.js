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
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import axios from 'axios';
import { useLog } from '@/components/utils/misc/useLog.js'

export const useSkillsDisplayParentFrameState = defineStore('skillsDisplayParentFrameState', () => {
  const log = useLog()
  const authToken = ref('')
  const isAuthenticating = ref(false)
  const parentFrame = ref(null)
  const serviceUrl = ref('')
  const options = ref ({})
  const navigateMethodCalled = ref(false)
  const isLastViewedScrollSupported = computed(() => {
    if (!parentFrame.value) {
      return true
    }

    return options.value && Object.keys(options.value).length > 0;
  })
  const setAuthToken = (token) => {
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    if (log.isTraceEnabled()) {
      log.trace(`UseSkillsDisplayParentFrameState.js: axios.defaults.headers.common['Authorization'] = Bearer ${token}`)
    }
    authToken.value = token
  }
  return {
    authToken,
    setAuthToken,
    isAuthenticating,
    parentFrame,
    serviceUrl,
    options,
    isLastViewedScrollSupported,
    navigateMethodCalled
  }
})