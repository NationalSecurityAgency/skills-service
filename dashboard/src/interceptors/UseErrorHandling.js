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
import axios from 'axios';
import { useRoute, useRouter } from 'vue-router'
import { useAuthState } from '@/stores/UseAuthState.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { userErrorState } from '@/stores/UserErrorState.js'
import { useSkillsDisplayInfo} from '@/skills-display/UseSkillsDisplayInfo.js'
import { useLog } from '@/components/utils/misc/useLog.js'
import { useUpgradeInProgressErrorChecker } from '@/components/utils/errors/UseUpgradeInProgressErrorChecker.js'
import { useSkillsDisplayParentFrameState } from '@/skills-display/stores/UseSkillsDisplayParentFrameState.js'
import { onScopeDispose } from 'vue'

export const useErrorHandling = () => {

  const route = useRoute()
  const router = useRouter()
  const authState = useAuthState()
  const appConfig = useAppConfig()
  const errorState = userErrorState()
  const upgradeInProgressErrorChecker = useUpgradeInProgressErrorChecker()

  const skillsDisplayInfo = useSkillsDisplayInfo()
  const log = useLog()
  const parentState = useSkillsDisplayParentFrameState()
  const isSupersededIframeRequest = (config) => skillsDisplayInfo.isSkillsClientPath() && !parentState.isCurrentAuthRequest(config)
  let interceptor = null
  onScopeDispose(() => {
    if (interceptor !== null) axios.interceptors.response.eject(interceptor)
  })

  const navToErrorPage = () => {
    const errPageName =
      skillsDisplayInfo.isSkillsClientPath() ? skillsDisplayInfo.getContextSpecificRouteName('SkillsDisplayErrorPage') : 'ErrorPage'
    log.debug(`Navigating to error page: ${errPageName}`)
    return router.push({ name: errPageName })
  }

  const errorResponseHandler = (error) => {
    // Token refresh can leave late requests for the SAME learner. Their failures
    // must not disrupt the recovered route, regardless of status or recovery timing.
    if (isSupersededIframeRequest(error?.config)) return Promise.reject(error)

    if (axios.isCancel(error)) {
      // Cancellation is not a successful empty response; iframe loaders must not store it.
      if (skillsDisplayInfo.isSkillsClientPath()) return Promise.reject(error)
      return Promise.resolve({ data: {} });
    }

    const config = error?.config || {}
    const errorCode = error?.response?.status;
    if (errorCode === 401 && skillsDisplayInfo.isSkillsClientPath() && config.skillsDisplayAuthGeneration !== undefined) {
      // Local error-handling opt-outs do not suppress iframe authentication recovery.
      // Recovery remounts learner content; losing rare unsaved quiz edits or component-local
      // video-credit retry state is accepted for this scoped fix. Failed writes are not replayed.
      authState.clearAuthData()
      return Promise.reject(error);
    }

    // check if the caller wants to handle all errors
    if (config.handleError === false) {
      return Promise.reject(error);
    }

    if (errorCode !== 401 && skillsDisplayInfo.isSkillsClientPath() && config.skillsDisplayHandleError === true) {
      // Only callers with an explicit local rejection handler own their error UI.
      // Being concurrent with recovery does not make an unrelated failure recoverable.
      return Promise.reject(error);
    }
    // check if the caller wants to handle a specific error status code
    if (Object.prototype.hasOwnProperty.call(config, 'handleErrorCode')) {
      const handledCodes = Array.isArray(config.handleErrorCode) ? config.handleErrorCode : String(config.handleErrorCode).split(',')
      if (handledCodes.some((code) => String(code).trim() === String(errorCode))) return Promise.reject(error)
    }

    const path = window.location.pathname;
    if (errorCode === 401) {
      authState.clearAuthData()
      if (skillsDisplayInfo.isSkillsClientPath()) {
        // The iframe handles re-authentication through its parent, not a dashboard login route.
        return Promise.reject(error);
      }
      if (path !== '/skills-login' && route.meta.requiresAuth) {
        let loginRoute = path !== '/' ? { name: 'Login', query: { redirect: path } } : { name: 'Login' };
        if (appConfig.isPkiAuthenticated) {
          loginRoute = path !== '/' ? { name: 'LandingPage', query: { redirect: path } } : { name: 'LandingPage' };
        }
        router.push(loginRoute);
      }
    } else if (errorCode === 403) {
      let explanation = 'You do not have permission to view this page';
      let ec;
      let projectId;
      if (error.response && error.response.data && error.response.data.explanation) {
        ({ explanation, errorCode: ec, projectId } = error.response.data);
      }
      if (explanation && ec === 'private_project' && !skillsDisplayInfo.isSkillsClientPath()) {
        router.push({ name: 'PrivateProjectAccessRequestPage', params: { projectId } });
      } else {
        errorState.setErrorParams('User Not Authorized', explanation, 'fas fa-shield-alt')
        // router.push({ name: 'ErrorPage'});
        navToErrorPage()
        return Promise.reject(error);
      }
    } else if (errorCode === 404) {
      let explanation;
      if (error.response && error.response.data && error.response.data.explanation) {
        ({ explanation } = error.response.data);
      }
      errorState.setErrorParams('Resource Not Found', explanation, 'fas fa-exclamation-triangle')
      navToErrorPage()
      return Promise.reject(error);
    } else if (upgradeInProgressErrorChecker.isUpgrading(error)) {
      if (skillsDisplayInfo.isSkillsClientPath()) {
        // The iframe router has no dashboard upgrade route.
        errorState.setErrorParams('Upgrade in Progress', 'SkillTree is being upgraded. Please try again later.', 'fas fa-tools')
        navToErrorPage()
      } else {
        upgradeInProgressErrorChecker.navToUpgradeInProgressPage()
      }
      return Promise.reject(error);
    } else {
      errorState.resetToDfeault()
      navToErrorPage()
      return Promise.reject(error);
    }
    return Promise.resolve({ data: {} });
  }

  const registerErrorHandling = () => {
    if (interceptor !== null) return
    log.debug('registering error handler')
    interceptor = axios.interceptors.response.use(
      (response) => {
        if (isSupersededIframeRequest(response.config)) {
          // A display has one learner, but older-token results can still overwrite
          // fresher progress. Reject centrally before any data loader applies them.
          throw new axios.CanceledError('Skills Display request was superseded', response.config)
        }
        return response
      },
      errorResponseHandler,
    );
  }

  return {
    registerErrorHandling
  }
}
