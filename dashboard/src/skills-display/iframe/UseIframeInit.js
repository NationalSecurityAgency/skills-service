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

export const useIframeInit = (isLoadingApp, initializeEmbeddedDisplay = () => {}) => {

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
  const isRefreshingDisplay = ref(false)
  const authenticationExpiredMessage = 'Your authentication has expired. Waiting for the embedding application to re-authenticate.'
  const authenticationFailedMessage = 'Unable to reconnect your session. Please refresh the page to try again.'
  let authenticationRequested = false
  let recovery = null
  let disposed = false
  let resizeObserver = null
  let observedElement = null
  let heightListenerTimeout = null
  let handshakeTimeout = null
  let themeStyle = null
  let originalOverflowY = null
  let parentMessageGuard = null
  let trustedParentOrigin = null
  const displayBlocked = computed(() => initializationError.value || authenticationError.value || isLoadingApp.value || isRefreshingDisplay.value)
  const disconnectHeightListener = () => {
    clearTimeout(heightListenerTimeout)
    heightListenerTimeout = null
    resizeObserver?.disconnect()
    observedElement = null
  }
  onScopeDispose(() => {
    disposed = true
    clearTimeout(handshakeTimeout)
    disconnectHeightListener()
    if (parentMessageGuard) window.removeEventListener('message', parentMessageGuard, true)
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
    if (disposed || !displayInfo.isSkillsClientPath() || !(error instanceof SkillsDisplayIdentityChangedError)) return false
    // A learner change is a contract violation, not another recoverable expiration.
    // Keep the iframe blocked even if the parent subsequently sends the original
    // learner's token; only destroying/recreating the display starts a new lifetime.
    initializationError.value = error.message
    authState.clearAuthData()
    return true
  }

  const resetEmbeddedProgress = () => {
    // Startup establishes a new iframe lifetime; recovery is for the SAME learner.
    // Pinia outlives a remounted display, so discard cached progress and restore
    // loading flags before authenticated content mounts again.
    useUserProgressSummaryState().reset()
    useSkillsDisplaySubjectState().reset()
    useSkillsDisplayPointHistoryState().reset()
    displayAttributes.invalidateConfig()
  }

  const initializeDisplay = ({ startup = false } = {}) => {
    if (recovery) return recovery
    // Initial 401s must be able to request credentials from the parent. A new
    // token can replace startup immediately, without waiting for its old requests.
    if (!startup) parentState.isAuthenticating = true
    // Iframe authentication comes from the parent, never persisted dashboard state.
    // Superseded bootstrap may not have reached App's normal restoration boundary.
    authState.setRestoringSession(false)
    const initialization = (async () => {
      // Retry only after a token/version update supersedes this attempt, never
      // repeatedly retry a failure for the current generation.
      while (!disposed) {
        // App owns the whole routed subtree. Let it unmount before resetting
        // shared caches; retain this initializer and its fixed-learner binding.
        await nextTick()
        if (disposed) return
        const generation = parentState.authGeneration
        const requestConfig = { skillsDisplayHandleError: true }
        try {
          resetEmbeddedProgress()
          // Token rotation can supersede /public/config before startup stores it.
          // Recovery must replace that bootstrap request, not mount with empty app config.
          if (!appConfig.hasLoadedConfig) await appConfig.loadConfigState(requestConfig)
          if (disposed) return
          if (generation !== parentState.authGeneration) {
            throw new Error('Skills Display authentication changed during bootstrap')
          }
          await authState.fetchUser(false, requestConfig)
          if (disposed) return
          if (generation !== parentState.authGeneration || !authState.userInfo?.userId) {
            throw new Error('Unable to restore Skills Display authentication')
          }
          await displayAttributes.loadConfigStateIfNeeded(null, requestConfig)
          if (disposed) return
          if (generation !== parentState.authGeneration || !authState.userInfo?.userId) {
            throw new Error('Skills Display authentication was lost during recovery')
          }
          // Required iframe data is ready. Initialize optional presentation support
          // and mark App ready without loading unrelated dashboard settings.
          await initializeEmbeddedDisplay(requestConfig)
          if (disposed) return
          if (generation !== parentState.authGeneration || !authState.userInfo?.userId) {
            throw new Error('Skills Display authentication was lost during recovery')
          }
          authenticationError.value = ''
          return
        } catch (error) {
          if (disposed) return
          // Replacement recovery owns readiness and errors for superseded startup.
          if (startup && generation !== parentState.authGeneration) return
          if (handleIdentityChange(error)) return
          // PKI version refreshes have no bearer token. An actual 401 still sets
          // authenticationError and must stop rather than retrying indefinitely.
          const canRetry = parentState.authToken || (appConfig.isPkiAuthenticated && !authenticationError.value)
          if (generation !== parentState.authGeneration && canRetry) continue
          if (startup) {
            // The 401 watcher owns initial expiration UI and token acquisition.
            // Other startup failures must not mount learner content behind an error route.
            if (!authenticationError.value) {
              initializationError.value = 'Unable to load Skills Display. Please refresh the page to try again.'
            }
          } else {
            // A current-token failure during recovery is explicit, not a silent wait for a token.
            authenticationError.value = authenticationFailedMessage
          }
          return
        }
      }
    })().finally(() => {
      if (!startup) {
        if (!disposed) {
          parentState.isAuthenticating = false
          isRefreshingDisplay.value = false
        }
        recovery = null
      }
    })
    if (!startup) recovery = initialization
    return initialization
  }

  const loadedIframe = computed(() => completedHandshake.value && updatedAuthToken.value)

  const registerHeightListener = () => {
    clearTimeout(heightListenerTimeout)
    heightListenerTimeout = null
    if (disposed || !resizeObserver) return
    // This persistent root includes loading, authentication and initialization
    // errors as well as routed content. The SDK starts its container at 0px.
    // Do not observe the viewport-sized outer wrapper: that would feed host
    // height changes back into the reported content height.
    const elementToObserve = document.querySelector('#skilltree-main-container')
    if (elementToObserve) {
      if (appStyleObject.value['min-height']) elementToObserve.style.minHeight = appStyleObject.value['min-height']
      if (observedElement !== elementToObserve) {
        resizeObserver.disconnect()
        observedElement = elementToObserve
        resizeObserver.observe(elementToObserve)
      }
    } else {
      heightListenerTimeout = setTimeout(() => {
        registerHeightListener()
      }, 100);
    }
  }
  watch(displayBlocked, () => {
    // Recheck after branch changes, without disconnecting while an error is shown.
    if (resizeObserver) nextTick(registerHeightListener)
  })

  const handleHandshake =() => {
    log.debug('UseIframeInit.js: handleHandshake')
    if (disposed || parentMessageGuard) return
    // Postmate 1.x binds origins but does not verify the sender window, including
    // during its handshake. Only the actual embedding parent may supply the model
    // or invoke the child API; legitimate cross-origin embedding remains supported.
    parentMessageGuard = (event) => {
      const message = event.data
      if (!message || typeof message !== 'object') {
        // Postmate's pending handshake reads data.postmate without a null check.
        if (!completedHandshake.value) event.stopImmediatePropagation()
        return
      }
      if (!Object.prototype.hasOwnProperty.call(message, 'postmate')) return
      if (disposed || initializationError.value || Array.isArray(message) || event.source !== window.parent ||
        message.type !== 'application/x-postmate-v1+json' ||
        (trustedParentOrigin !== null && event.origin !== trustedParentOrigin)) {
        event.stopImmediatePropagation()
        return
      }
      if (message.postmate === 'handshake') {
        if (trustedParentOrigin !== null) {
          event.stopImmediatePropagation()
          return
        }
        const model = message.model
        // Postmate merges the parent's model onto the child API with assignment.
        // Do not allow that merge to replace methods or alter the API's prototype.
        const reservedKeys = ['updateAuthenticationToken', 'updateVersion', 'navigate', '__proto__', 'constructor', 'prototype']
        const rejectedReservedKey = model && typeof model === 'object' && !Array.isArray(model)
          ? reservedKeys.find((key) => Object.prototype.hasOwnProperty.call(model, key))
          : undefined
        if (!model || typeof model !== 'object' || Array.isArray(model) ||
          rejectedReservedKey) {
          // Intentionally reject before Postmate completes its handshake. A zero-height SDK
          // container may hide the error, but this malformed/hostile-model edge case does
          // not justify a separate error/height channel.
          event.stopImmediatePropagation()
          initializationError.value = 'Unable to initialize Skills Display. Please verify the embedding configuration and refresh the page.'
          if (rejectedReservedKey) {
            log.error(`Skills Display rejected the handshake model: reserved key [${rejectedReservedKey}] is not allowed.`)
          }
          return
        }
        trustedParentOrigin = event.origin
      } else if (!['call', 'request'].includes(message.postmate) || trustedParentOrigin === null ||
        typeof message.property !== 'string' ||
        !Object.prototype.hasOwnProperty.call(parentState.parentFrame?.model || {}, message.property)) {
        event.stopImmediatePropagation()
      }
    }
    window.addEventListener('message', parentMessageGuard, true)
    // Postmate leaves its child handshake pending indefinitely when no parent replies.
    // Fail closed with an actionable error instead of an unbounded loading screen.
    handshakeTimeout = setTimeout(() => {
      if (!disposed && !completedHandshake.value) {
        initializationError.value ||= 'Unable to initialize Skills Display. Please verify the embedding configuration and refresh the page.'
      }
    }, 10000)
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
          return initializeDisplay()
        }
      },
      updateVersion(newVersion) {
        if (disposed || !completedHandshake.value || initializationError.value) return
        if (newVersion != null && typeof newVersion !== 'string' &&
          (typeof newVersion !== 'number' || !Number.isFinite(newVersion))) return
        if ((newVersion ?? null) === displayAttributes.version) return
        displayAttributes.version = newVersion ?? null
        // Before authentication, startup will read the latest version. While
        // waiting for renewed credentials, leave the existing auth block intact.
        if (!updatedAuthToken.value || (authenticationError.value && !recovery)) return
        if (!parentState.authToken && !appConfig.isPkiAuthenticated) return
        isRefreshingDisplay.value = true
        // Reuse the existing stale-response guard without changing credentials
        // or starting a new learner lifetime. Recovery coalesces rapid updates,
        // verifies the same learner and remounts App's unchanged current route.
        parentState.setAuthToken(parentState.authToken)
        return initializeDisplay()
      },
      navigate(route) {
        if (disposed || !completedHandshake.value || initializationError.value) return
        if (typeof route !== 'string' && (!route || typeof route !== 'object' || Array.isArray(route))) return
        try {
          const resolved = router.resolve(route)
          // The iframe layout alone has no learner content. Only embedded child
          // routes may be selected by the parent, including during recovery.
          if (!resolved.matched.length || !displayInfo.isSkillsDisplayPath(resolved)) return
          if (log.isTraceEnabled()) {
            log.trace(`UseIframeInit.js: navigate: ${resolved.path}`)
          }
          // Parent values can name nonexistent routes or contain malformed params.
          // Invalid navigation must not leave the recovered display blank or reject
          // outside Postmate's fire-and-forget call handler.
          router.push(route).catch(() => log.warn('Skills Display ignored invalid parent navigation'))
        } catch {
          log.warn('Skills Display ignored invalid parent navigation')
        }
      },
    })
    if (appConfig.isPkiAuthenticated) {
      updatedAuthToken.value = true
    }
    return handshake.then((parent) => {
      clearTimeout(handshakeTimeout)
      if (disposed || initializationError.value) return
      log.debug('UseIframeInit.js: handshake.then')
      // Keep a height-only channel to the trusted Postmate parent even if model
      // validation fails. Authentication and navigation still require successful
      // initialization; an invalid model must never be stored in parentState.
      resizeObserver = new ResizeObserver(function(entries) {
        if (disposed) return
        // Ignore queued notifications for a disconnected root.
        const observedEntry = entries.find((entry) => entry.target === observedElement && entry.target.isConnected)?.contentRect;
        if (observedEntry && observedEntry.height > 0) {
          const newHeight = observedEntry.height + 10;
          log.debug(`UseIframeInit.js: changing height to [${newHeight}]`)
          parent.emit('height-changed', newHeight)
        } else {
          log.warn('UseIframeInit.js: resize event got height of 0')
        }
      });
      registerHeightListener()
      let serviceUrl
      try {
        serviceUrl = validateSkillsDisplayServiceUrl(parent.model.serviceUrl)
      } catch (error) {
        initializationError.value = 'Unable to initialize Skills Display. The service URL must use the same origin as the SkillTree iframe.'
        throw error
      }
      // Required model values must be validated before storing them or starting requests.
      if (typeof parent.model.projectId !== 'string' || !parent.model.projectId.trim()) {
        throw new Error('A Skills Display project ID is required')
      }
      const version = parent.model.version
      if (version != null && typeof version !== 'string' &&
        (typeof version !== 'number' || !Number.isFinite(version))) {
        throw new Error('Invalid Skills Display version')
      }
      const minHeight = parent.model.minHeight
      if (minHeight != null && (typeof minHeight !== 'string' || !CSS.supports('min-height', minHeight))) {
        throw new Error('Invalid Skills Display minimum height')
      }
      if (parent.model.userId != null && typeof parent.model.userId !== 'string') {
        throw new Error('Invalid Skills Display user ID')
      }
      if (parent.model.options != null && (typeof parent.model.options !== 'object' || Array.isArray(parent.model.options))) {
        throw new Error('Invalid Skills Display options')
      }
      // Embedded contract: the learner and optional display userId are fixed for
      // this iframe lifetime. Tokens may be renewed for the same learner and the
      // SDK version setter may refresh the skills version. Nullish handling preserves 0.
      parentState.beginAuthenticationSession()
      resetEmbeddedProgress()
      // Make sure to freeze the parent object so Pinia won't try to make it reactive
      // CORs won't allow this because parent object can't be changed from an iframe
      parentState.parentFrame = Object.freeze(parent)

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

      parentState.options = { ...parent.model.options }

      displayAttributes.version = version ?? null
      // A PKI display target is fixed by the handshake. It is distinct from the
      // authenticated principal bound by /app/userInfo, and has no live update API.
      displayAttributes.userId = parent.model.userId

      handleTheming(parent.model.theme);

      if (minHeight) {
        log.debug(`UseIframeInit.js: parent.model.minHeight: ${minHeight}`)
        appStyleObject.value['min-height'] = minHeight
      }

      // No scroll bars for iframe.
      originalOverflowY = document.body.style['overflow-y']
      document.body.style['overflow-y'] = 'hidden';

      displayAttributes.loadingConfig = false

      completedHandshake.value = true
      registerHeightListener()
      // The embedding host owns initial token acquisition and its failure UI.
      // An initial-authentication timeout is intentionally deferred to broader SDK
      // resilience work to avoid premature failure during slow authentication.
      parentState.parentFrame.emit('needs-authentication')
    }).catch(() => {
      clearTimeout(handshakeTimeout)
      if (disposed) return
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
    initializeDisplay,
    initializationError,
    authenticationError,
    isRefreshingDisplay,
    loadedIframe
  }
}
