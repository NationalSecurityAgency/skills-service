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
import { useSkillsDisplayParentFrameState } from '@/skills-display/stores/UseSkillsDisplayParentFrameState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useLog } from '@/components/utils/misc/useLog.js'
import Postmate from 'postmate'
import { ref, computed, watch, nextTick } from 'vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import ThemeHelper from '@/skills-display/theme/ThemeHelper.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useRouter } from 'vue-router'
import { validateSkillsDisplayServiceUrl } from '@/skills-display/iframe/SkillsDisplayServiceUrl.js'
import { useAuthState } from '@/stores/UseAuthState.js'

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

  watch(() => parentState.authenticationFailureRevision, () => {
    if (displayInfo.isSkillsClientPath() && completedHandshake.value && updatedAuthToken.value) {
      authenticationError.value = authenticationExpiredMessage
      if (parentState.isAuthenticating || authenticationRequested) return
      authenticationRequested = true
      // clearAuthData also clears the token; let it finish before requesting a replacement.
      nextTick(() => {
        if (authenticationRequested && authenticationError.value && !parentState.isAuthenticating) {
          parentState.parentFrame?.emit('needs-authentication')
        }
      })
    }
  }, { flush: 'sync' })

  const recoverAuthentication = () => {
    if (recovery) return recovery
    parentState.isAuthenticating = true
    recovery = (async () => {
      // Only retry when the parent supplies a newer token, never repeatedly retry a failed token.
      while (true) {
        const generation = parentState.authGeneration
        try {
          await authState.fetchUser(false)
          if (generation !== parentState.authGeneration || !authState.userInfo?.userId) {
            throw new Error('Unable to restore Skills Display authentication')
          }
          await displayAttributes.loadConfigStateIfNeeded()
          if (generation !== parentState.authGeneration || !authState.userInfo?.userId) {
            throw new Error('Skills Display authentication was lost during recovery')
          }
          authenticationError.value = ''
          return
        } catch {
          if (generation !== parentState.authGeneration && parentState.authToken) continue
          // A current-token failure during recovery is explicit, not a silent wait for a token.
          authenticationError.value = authenticationFailedMessage
          return
        }
      }
    })().finally(() => {
      parentState.isAuthenticating = false
      recovery = null
    })
    return recovery
  }

  const loadedIframe = computed(() => completedHandshake.value && updatedAuthToken.value)

  const registerHeightListener = (resizeObserver) => {
    const elementToObserve = document.querySelector("#skills-display-app")
    if (elementToObserve) {
      resizeObserver.observe(elementToObserve)
    } else {
      setTimeout(() => {
        registerHeightListener(resizeObserver)
      }, 100);
    }
  }

  const handleHandshake =() => {
    log.debug('UseIframeInit.js: handleHandshake')
    const handshake = new Postmate.Model({
      updateAuthenticationToken(authToken) {
        if (!completedHandshake.value || initializationError.value) return
        const needsRecovery = updatedAuthToken.value && (authenticationError.value || !authState.userInfo?.userId)
        authenticationRequested = false
        parentState.setAuthToken(authToken)
        updatedAuthToken.value = true
        if (authToken && needsRecovery) {
          return recoverAuthentication()
        }
      },
      updateVersion(newVersion) {
        if (!completedHandshake.value || initializationError.value) return
        displayAttributes.version = newVersion
        if (log.isTraceEnabled()) {
          log.trace(`UseIframeInit.js: updateVersion: ${newVersion}`)
        }
        displayInfo.routerPush('SkillsDisplayInIframe')
      },
      navigate(route) {
        if (!completedHandshake.value || initializationError.value) return
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
      log.debug('UseIframeInit.js: handshake.then')
      const serviceUrl = validateSkillsDisplayServiceUrl(parent.model.serviceUrl)
      // Make sure to freeze the parent object so Pinia won't try to make it reactive
      // CORs won't allow this because parent object can't be changed from an iframe
      parentState.parentFrame = Object.freeze(parent)
      const resizeObserver = new ResizeObserver(function(entries) {
        const observedEntry = entries[0].contentRect;
        if (observedEntry && observedEntry.height > 0) {
          const newHeight = observedEntry.height + 10;
          log.debug(`UseIframeInit.js: changing height to [${newHeight}]`)
          parentState.parentFrame.emit('height-changed', newHeight)
        } else {
          log.warn('UseIframeInit.js: resize event got height of 0')
        }
      });
      registerHeightListener(resizeObserver)

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
      // UserSkillsService.setUserId(parent.model.userId);
      // QuizRunService.setUserId(parent.model.userId);
      displayAttributes.userId = parent.model.userId

      handleTheming(parent.model.theme);

      parentState.parentFrame.emit('needs-authentication')

      if (parent.model.minHeight) {
        log.debug(`UseIframeInit.js: parent.model.minHeight: ${parent.model.minHeight}`)
        appStyleObject.value['min-height'] = parent.model.minHeight
      }

      // No scroll bars for iframe.
      document.body.style['overflow-y'] = 'hidden';

      // this.loadConfigs();
      displayAttributes.loadingConfig = false
      // this.getCustomIconCss();

      completedHandshake.value = true
    }).catch(() => {
      parentState.setAuthToken('')
      completedHandshake.value = false
      initializationError.value = 'Unable to initialize Skills Display. The service URL must use the same origin as the SkillTree iframe.'
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
    loadedIframe
  }
}
