/*
Copyright 2026 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import { computed, onBeforeMount, onMounted, ref } from 'vue'
import { RouterView } from 'vue-router'
import { until } from '@vueuse/core'
import axios from 'axios'
import { useIframeInit } from '@/skills-display/iframe/UseIframeInit.js'
import { useSkillsDisplayParentFrameState } from '@/skills-display/stores/UseSkillsDisplayParentFrameState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAuthState } from '@/stores/UseAuthState.js'
import { useMatomoSupport } from '@/stores/UseMatomoSupport.js'
import { useLog } from '@/components/utils/misc/useLog.js'
import IconManagerService from '@/components/utils/iconPicker/IconManagerService.js'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import NewSoftwareVersion from '@/components/header/NewSoftwareVersion.vue'
import ConfirmDialog from 'primevue/confirmdialog'
import ScrollToTop from '@/common-components/utilities/ScrollToTop.vue'

const parentState = useSkillsDisplayParentFrameState()
const displayAttributes = useSkillsDisplayAttributesState()
const appConfig = useAppConfig()
const authState = useAuthState()
const matomo = useMatomoSupport()
const log = useLog()
const isAppLoaded = ref(false)
const isLoadingApp = computed(() => !isAppLoaded.value || appConfig.isLoadingConfig || authState.restoringSession || displayAttributes.loadingConfig)
const isScrollToTopDisabled = computed(() => appConfig.disableScrollToTop === 'true' || appConfig.disableScrollToTop === true)

const initializeEmbeddedDisplay = (requestConfig) => {
  // Icons are optional presentation support, not an iframe readiness dependency.
  // Catch synchronous errors too; HTTP 401s still use iframe authentication handling.
  Promise.resolve()
    .then(() => IconManagerService.refreshCustomIconCss(displayAttributes.projectId, null, requestConfig))
    .catch((error) => {
      if (!axios.isCancel(error) && parentState.isCurrentAuthRequest(error?.config)) {
        log.warn('Unable to load optional Skills Display custom icons')
      }
    })
  try {
    matomo.init({ isSkillsClient: true, projectId: displayAttributes.projectId })
  } catch {
    log.warn('Unable to initialize optional Skills Display analytics')
  }
  isAppLoaded.value = true
}

// This host survives recovery. Only its routed subtree unmounts while the
// initializer retains the trusted parent, fixed learner and authentication state.
const iframeInit = useIframeInit(isLoadingApp, initializeEmbeddedDisplay)
const isLoadingDisplay = computed(() => isLoadingApp.value || iframeInit.isRefreshingDisplay.value)

onBeforeMount(() => {
  iframeInit.handleHandshake()
})
onMounted(async () => {
  await until(() => iframeInit.loadedIframe.value || Boolean(iframeInit.initializationError.value)).toBe(true)
  if (iframeInit.initializationError.value) return
  await iframeInit.initializeDisplay({ startup: true })
})
</script>

<template>
  <div v-if="iframeInit.initializationError.value" role="alert" data-cy="skillsDisplayInitError" class="p-4">
    {{ iframeInit.initializationError.value }}
  </div>
  <div v-else-if="iframeInit.authenticationError.value" class="flex min-h-80 items-center justify-center p-4 sm:p-6">
    <div role="alert"
         aria-labelledby="skills-display-authentication-title"
         data-cy="skillsDisplayAuthenticationError"
         class="w-full max-w-lg rounded-xl border border-slate-200 bg-white p-6 text-center shadow-sm dark:border-slate-700 dark:bg-slate-900 sm:p-8">
      <div aria-hidden="true" class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-blue-50 text-blue-700 dark:bg-blue-950 dark:text-blue-300">
        <i class="fas fa-lock text-2xl"></i>
      </div>
      <h1 id="skills-display-authentication-title" class="mb-3 text-xl font-semibold text-slate-900 dark:text-slate-100">
        Re-authentication required
      </h1>
      <p class="m-0 text-base leading-relaxed text-slate-700 dark:text-slate-300">
        {{ iframeInit.authenticationError.value }}
      </p>
    </div>
  </div>
  <div v-else-if="isLoadingDisplay" role="main" class="flex min-h-[40rem] content-center justify-center flex-wrap">
    <div class="flex items-center justify-center m-2">
      <skills-spinner :is-loading="true" class="text-center"/>
      <h1 class="text-sm sr-only">Loading...</h1>
    </div>
  </div>
  <div v-else class="m-0">
    <!-- The skills-client may replay the host deep link after renewal, especially with
         updateHistory disabled. Keep this notice outside RouterView. -->
    <div v-if="iframeInit.sessionRestored.value"
         class="flex items-center justify-between gap-4 border border-blue-200 bg-blue-50 p-4 text-blue-900 dark:border-blue-700 dark:bg-blue-950 dark:text-blue-100"
         data-cy="skillsDisplaySessionRestored">
      <span role="status" aria-live="polite">Your session was restored.</span>
      <button type="button" aria-label="Dismiss session restored notification"
              data-cy="dismissSkillsDisplaySessionRestored"
              class="rounded p-2 hover:bg-blue-100 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 dark:hover:bg-blue-900"
              @click="iframeInit.sessionRestored.value = false">
        <i class="fas fa-times" aria-hidden="true"></i>
      </button>
    </div>
    <div class="sd-theme-background-color">
      <new-software-version />
      <div role="main" id="mainContent1" tabindex="-1"
           aria-label="Main content area, click tab to navigate">
        <RouterView />
      </div>
    </div>
    <ConfirmDialog :pt="{ root: { class: 'w-2/3' } }"></ConfirmDialog>
    <scroll-to-top v-if="!isScrollToTopDisabled" />
  </div>
</template>
