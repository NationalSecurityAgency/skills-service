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
import { SkillsDisplayIdentityChangedError, useSkillsDisplayParentFrameState } from '@/skills-display/stores/UseSkillsDisplayParentFrameState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useLog } from '@/components/utils/misc/useLog.js'
import Postmate from 'postmate'
import { ref, computed, watch, nextTick, onScopeDispose } from 'vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import ThemeHelper from '@/skills-display/theme/ThemeHelper.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useRouter } from 'vue-router'
import { validateSkillsDisplayServiceUrl } from '@/skills-display/iframe/SkillsDisplayServiceUrl.js'
import { useAuthState } from '@/stores/UseAuthState.js'
import { useUserProgressSummaryState } from '@/skills-display/stores/UseUserProgressSummaryState.js'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import { useSkillsDisplayPointHistoryState } from '@/skills-display/stores/UseSkillsDisplayPointHistoryState.js'

export const useIframeInit = () => {

  const parentState = useSkillsDisplayParentFrameState()
  const displayAttributes = useSkillsDisplayAttributesState()
  const displayInfo = useSkillsDisplayInfo()
  const appConfig = useAppConfig()
  const router = useRouter()
  const log = useLog()
  const authState = useAuthState()
  const completedHandshake = ref(false)
  const updatedAuthToken = ref(false)
  const initializationError = ref('')
  const authenticationError = ref('')
  const authenticationExpiredMessage = 'Your authentication has expired. Waiting for the embedding application to re-authenticate.'
  const authenticationFailedMessage = 'Unable to reconnect your session. Please refresh the page to try again.'
  let authenticationRequested = false
  let recovery = null
  let disposed = false
  let resizeObserver = null
  let heightListenerTimeout = null
  let themeStyle = null
  let originalOverflowY = null
  onScopeDispose(() => {
    disposed = true
    clearTimeout(heightListenerTimeout)
    resizeObserver?.disconnect()
    themeStyle?.remove()
    if (originalOverflowY !== null) document.body.style['overflow-y'] = originalOverflowY
    if (completedHandshake.value) {
      // Invalidate in-flight work before another iframe initializer can reuse these stores.
      parentState.setAuthToken('')
      parentState.parentFrame = null
      parentState.isAuthenticating = false
    }
  })

  watch(() => parentState.authenticationFailureRevision, () => {
    if (displayInfo.isSkillsClientPath() && completedHandshake.value && updatedAuthToken.value && !initializationError.value) {
      authenticationError.value = authenticationExpiredMessage
      if (parentState.isAuthenticating || authenticationRequested) return
      authenticationRequested = true
      // clearAuthData also clears the token; let it finish before requesting a replacement.
      nextTick(() => {
        if (!disposed && authenticationRequested && authenticationError.value && !parentState.isAuthenticating) {
          parentState.parentFrame?.emit('needs-authentication')
        }
      })
    }
  }, { flush: 'sync' })

  const handleIdentityChange = (error) => {
    if (!displayInfo.isSkillsClientPath() || !(error instanceof SkillsDisplayIdentityChangedError)) return false
    // A learner change is a contract violation, not another recoverable expiration.
    // Keep the iframe blocked even if the parent subsequently sends the original
    // learner's token; only destroying/recreating the display starts a new lifetime.
    initializationError.value = error.message
    clearTimeout(heightListenerTimeout)
    resizeObserver?.disconnect()
    authState.clearAuthData()
    return true
  }

  const resetEmbeddedProgress = () => {
    // Startup establishes a new iframe lifetime; recovery is for the SAME learner.
    // Pinia outlives a remounted display, so discard cached progress and restore
    // loading flags at these explicit boundaries, before authenticated content mounts.
    // Do not install general learner/project/version switching watchers in shared stores.
    useUserProgressSummaryState().reset()
    useSkillsDisplaySubjectState().reset()
    useSkillsDisplayPointHistoryState().reset()
    displayAttributes.invalidateConfig()
  }

  const recoverAuthentication = () => {
    if (recovery) return recovery
    parentState.isAuthenticating = true
    recovery = (async () => {
      // Only retry when the parent supplies a newer token, never repeatedly retry a failed token.
      while (!disposed) {
        const generation = parentState.authGeneration
        try {
          resetEmbeddedProgress()
          await authState.fetchUser(false)
          if (disposed) return
          if (generation !== parentState.authGeneration || !authState.userInfo?.userId) {
            throw new Error('Unable to restore Skills Display authentication')
          }
          await displayAttributes.loadConfigStateIfNeeded()
          if (disposed) return
          if (generation !== parentState.authGeneration || !authState.userInfo?.userId) {
            throw new Error('Skills Display authentication was lost during recovery')
          }
          authenticationError.value = ''
          return
        } catch (error) {
          if (disposed) return
          if (handleIdentityChange(error)) return
          if (generation !== parentState.authGeneration && parentState.authToken) continue
          // A current-token failure during recovery is explicit, not a silent wait for a token.
          authenticationError.value = authenticationFailedMessage
          return
        }
      }
    })().finally(() => {
      if (!disposed) parentState.isAuthenticating = false
      recovery = null
    })
    return recovery
  }

  const loadedIframe = computed(() => completedHandshake.value && updatedAuthToken.value)

  const registerHeightListener = () => {
    if (disposed || initializationError.value || authenticationError.value) return
    const elementToObserve = document.querySelector("#skills-display-app")
    if (elementToObserve) {
      if (appStyleObject.value['min-height']) elementToObserve.style.minHeight = appStyleObject.value['min-height']
      resizeObserver.observe(elementToObserve)
    } else {
      heightListenerTimeout = setTimeout(() => {
        registerHeightListener()
      }, 100);
    }
  }
  watch(authenticationError, () => {
    clearTimeout(heightListenerTimeout)
    resizeObserver?.disconnect()
    if (resizeObserver && !authenticationError.value) nextTick(registerHeightListener)
  })

  const handleHandshake =() => {
    log.debug('UseIframeInit.js: handleHandshake')
    const handshake = new Postmate.Model({
      updateAuthenticationToken(authToken) {
        if (disposed || !completedHandshake.value || initializationError.value) return
        // Parent messages are untrusted values; never coerce objects into bearer headers.
        if (typeof authToken !== 'string' || !authToken.trim() || /[\r\n]/.test(authToken)) return
        if (authToken === parentState.authToken) return
        // The parent may renew credentials (including during startup), never the
        // learner. fetchUser verifies the service-authenticated identity before
        // recovery can remount the existing iframe for that same learner.
        const needsRecovery = updatedAuthToken.value
        authenticationRequested = false
        if (needsRecovery) authenticationError.value = authenticationExpiredMessage
        parentState.setAuthToken(authToken)
        updatedAuthToken.value = true
        if (needsRecovery) {
          return recoverAuthentication()
        }
      },
      updateVersion(newVersion) {
        if (disposed || !completedHandshake.value || initializationError.value) return
        displayAttributes.version = newVersion
        if (log.isTraceEnabled()) {
          log.trace(`UseIframeInit.js: updateVersion: ${newVersion}`)
        }
        displayInfo.routerPush('SkillsDisplayInIframe')
      },
      navigate(route) {
        if (disposed || !completedHandshake.value || initializationError.value) return
        if (log.isTraceEnabled()) {
          log.trace(`UseIframeInit.js: navigate: ${JSON.stringify(route)}`)
        }
        router.push(route)
      },
    })
    if (appConfig.isPkiAuthenticated) {
      updatedAuthToken.value = true
    }
    return handshake.then((parent) => {
      if (disposed) return
      log.debug('UseIframeInit.js: handshake.then')
      let serviceUrl
      try {
        serviceUrl = validateSkillsDisplayServiceUrl(parent.model.serviceUrl)
      } catch (error) {
        initializationError.value = 'Unable to initialize Skills Display. The service URL must use the same origin as the SkillTree iframe.'
        throw error
      }
      parentState.beginAuthenticationSession()
      resetEmbeddedProgress()
      // Make sure to freeze the parent object so Pinia won't try to make it reactive
      // CORs won't allow this because parent object can't be changed from an iframe
      parentState.parentFrame = Object.freeze(parent)
      resizeObserver = new ResizeObserver(function(entries) {
        if (disposed || initializationError.value || !parentState.parentFrame) return
        const observedEntry = entries[0]?.contentRect;
        if (observedEntry && observedEntry.height > 0) {
          const newHeight = observedEntry.height + 10;
          log.debug(`UseIframeInit.js: changing height to [${newHeight}]`)
          parentState.parentFrame.emit('height-changed', newHeight)
        } else {
          log.warn('UseIframeInit.js: resize event got height of 0')
        }
      });

      // will only display summary and component will not be interactive
      displayAttributes.isSummaryOnly = parent.model.isSummaryOnly ? parent.model.isSummaryOnly : false

      // whether to use an internal back button as opposed to the browser back button
      displayAttributes.internalBackButton = parent.model.internalBackButton == null || parent.model.internalBackButton
      if (log.isTraceEnabled()) {
        log.trace(`UseIframeInit.js: internalBackButton: ${displayAttributes.internalBackButton}`)
      }

      parentState.serviceUrl = serviceUrl

      displayAttributes.projectId = parent.model.projectId
      displayAttributes.serviceUrl = serviceUrl
      log.debug(`UseIframeInit.js: serviceUrl: [${displayAttributes.serviceUrl}], projectId: [${displayAttributes.projectId}]`)

      if (parent.model.options) {
        parentState.options = { ...parent.model.options }
      }

      if (parent.model.version) {
        displayAttributes.version = parent.model.version
      }
      // A PKI display target is fixed by the handshake. It is distinct from the
      // authenticated principal bound by /app/userInfo, and has no live update API.
      displayAttributes.userId = parent.model.userId

      handleTheming(parent.model.theme);

      if (parent.model.minHeight) {
        log.debug(`UseIframeInit.js: parent.model.minHeight: ${parent.model.minHeight}`)
        appStyleObject.value['min-height'] = parent.model.minHeight
      }

      // No scroll bars for iframe.
      originalOverflowY = document.body.style['overflow-y']
      document.body.style['overflow-y'] = 'hidden';

      displayAttributes.loadingConfig = false

      completedHandshake.value = true
      registerHeightListener()
      parentState.parentFrame.emit('needs-authentication')
    }).catch(() => {
      if (disposed) return
      clearTimeout(heightListenerTimeout)
      resizeObserver?.disconnect()
      parentState.parentFrame = null
      parentState.setAuthToken('')
      completedHandshake.value = false
      initializationError.value ||= 'Unable to initialize Skills Display. Please verify the embedding configuration and refresh the page.'
      log.error(initializationError.value)
    })
  }

  const appStyleObject = ref({})
  const themeState = useSkillsDisplayThemeState()
  const handleTheming = (theme) =>{
    if (theme) {
      const themeResArtifacts = ThemeHelper.build(theme);

      // populate store so JS can subscribe to those values and update styles
      themeResArtifacts.themeModule.forEach((value, key) => {
        themeState.setThemeByKey(key, value)
      });

      const style = document.createElement('style');
      themeStyle = style

      style.id = themeState.themeStyleId;
      style.appendChild(document.createTextNode(themeResArtifacts.css));

      const { body } = document;
      body.appendChild(style);
    }
  }

  return {
    handleHandshake,
    initializationError,
    authenticationError,
    handleIdentityChange,
    loadedIframe
  }
}
