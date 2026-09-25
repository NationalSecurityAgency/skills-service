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
import { computed, onScopeDispose, ref } from 'vue'
import axios from 'axios';
import { validateSkillsDisplayServiceUrl } from '@/skills-display/iframe/SkillsDisplayServiceUrl.js'

export const useSkillsDisplayParentFrameState = defineStore('skillsDisplayParentFrameState', () => {
  const authToken = ref('')
  const isAuthenticating = ref(false)
  const parentFrame = ref(null)
  const serviceUrl = ref('')
  const options = ref ({})
  const isLastViewedScrollSupported = computed(() => {
    if (!parentFrame.value) {
      return true
    }

    return options.value && Object.keys(options.value).length > 0;
  })
  const parentOrigin = computed(() => {
    return parentFrame.value?.parentOrigin;
  })
  const parentPath = computed(() => {
    return options.value?.parentPath || '/skilltree';
  })
  const setAuthToken = (token) => {
    authToken.value = token
  }
  const interceptor = axios.interceptors.request.use((config) => {
    if (!authToken.value) {
      return config
    }
    try {
      validateSkillsDisplayServiceUrl(serviceUrl.value)
      // getUri uses Axios's URL/baseURL combination rules, including allowAbsoluteUrls.
      const destination = new URL(axios.getUri(config), window.location.href)
      if (destination.origin === window.location.origin && !destination.username && !destination.password) {
        config.headers.set('Authorization', `Bearer ${authToken.value}`)
      }
    } catch {
      // Invalid or untrusted URLs must never receive the display token.
    }
    return config
  })
  onScopeDispose(() => axios.interceptors.request.eject(interceptor))
  return {
    authToken,
    setAuthToken,
    isAuthenticating,
    parentFrame,
    serviceUrl,
    options,
    isLastViewedScrollSupported,
    parentOrigin,
    parentPath
  }
})
