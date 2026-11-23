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

export class SkillsDisplayIdentityChangedError extends Error {
  constructor() {
    super('The authenticated learner has changed. The embedding application must destroy and recreate Skills Display to show a different learner.')
    this.name = 'SkillsDisplayIdentityChangedError'
  }
}

export const useSkillsDisplayParentFrameState = defineStore('skillsDisplayParentFrameState', () => {
  const authToken = ref('')
  const authGeneration = ref(0)
  const authenticationFailureRevision = ref(0)
  const isAuthenticating = ref(false)
  const parentFrame = ref(null)
  const serviceUrl = ref('')
  const options = ref ({})
  // Embedded contract: one authenticated learner per iframe lifetime. Tokens may
  // be renewed, but a different learner requires destroying and recreating the iframe.
  // Bind only to /app/userInfo verified by the service, never to a parent-supplied ID
  // or an unverified JWT claim. Native displays and admin previews are not bound here.
  let authenticatedUserId = null
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
    authGeneration.value += 1
    authToken.value = token
  }
  const clearAuthToken = () => {
    setAuthToken('')
    // A failed initial request must be observable even if no user was ever loaded.
    authenticationFailureRevision.value += 1
  }
  const beginAuthenticationSession = () => {
    // Only iframe initialization starts a new lifetime; clearing an expired token
    // must NOT forget which learner owns the existing display.
    authenticatedUserId = null
    setAuthToken('')
  }
  const verifyAuthenticatedUser = (userId) => {
    if (typeof userId !== 'string' || !userId) throw new Error('Skills Display authentication did not return a user ID')
    // Match the service's case-insensitive usernames, including proxy users whose
    // /app/userInfo ID retains the token's casing until an account is persisted.
    const canonicalUserId = userId.toLowerCase()
    if (authenticatedUserId !== null && authenticatedUserId !== canonicalUserId) throw new SkillsDisplayIdentityChangedError()
    authenticatedUserId = canonicalUserId
  }
  const captureAuthGeneration = () => {
    // Generations isolate old-token work for the SAME learner, not live user switching.
    // Axios rejects stale successes, but finally callbacks still need this guard.
    const generation = authGeneration.value
    return () => generation === authGeneration.value
  }
  const isCurrentAuthRequest = (config) => {
    return config?.skillsDisplayAuthGeneration === undefined || config.skillsDisplayAuthGeneration === authGeneration.value
  }
  const interceptor = axios.interceptors.request.use((config) => {
    try {
      validateSkillsDisplayServiceUrl(serviceUrl.value)
      // getUri uses Axios's URL/baseURL combination rules, including allowAbsoluteUrls.
      const destination = new URL(axios.getUri(config), window.location.href)
      if (destination.origin === window.location.origin && !destination.username && !destination.password) {
        // Keep this local to Axios config; do not transmit it as a header or URL parameter.
        config.skillsDisplayAuthGeneration = authGeneration.value
        if (authToken.value) {
          config.headers.set('Authorization', `Bearer ${authToken.value}`)
        }
      }
    } catch {
      // Invalid or untrusted URLs must never receive the display token.
    }
    return config
  })
  onScopeDispose(() => axios.interceptors.request.eject(interceptor))
  return {
    authToken,
    authGeneration,
    authenticationFailureRevision,
    setAuthToken,
    clearAuthToken,
    beginAuthenticationSession,
    verifyAuthenticatedUser,
    captureAuthGeneration,
    isCurrentAuthRequest,
    isAuthenticating,
    parentFrame,
    serviceUrl,
    options,
    isLastViewedScrollSupported,
    parentOrigin,
    parentPath
  }
})
