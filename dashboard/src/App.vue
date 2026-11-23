/*
Copyright 2024 SkillTree

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
import {computed, onBeforeMount, onMounted, ref, watch} from 'vue'
import axios from 'axios'
import {RouterView, useRoute} from 'vue-router'
import DashboardHeader from '@/components/header/DashboardHeader.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import {useCustomGlobalValidators} from '@/validators/UseCustomGlobalValidators.js'
import {useThemesHelper} from '@/components/header/UseThemesHelper.js'
import {useAppConfig} from '@/common-components/stores/UseAppConfig.js'
import {useAuthState} from '@/stores/UseAuthState.js'
import {useAppInfoState} from '@/stores/UseAppInfoState.js'
import {useAccessState} from '@/stores/UseAccessState.js'
import ConfirmDialog from 'primevue/confirmdialog'
import {useGlobalNavGuards} from '@/router/UseGlobalNavGuards.js'
import {useErrorHandling} from '@/interceptors/UseErrorHandling.js'
import CustomizableHeader from '@/components/customization/CustomizableHeader.vue'
import CustomizableFooter from '@/components/customization/CustomizableFooter.vue'
import {useSkillsDisplayInfo} from '@/skills-display/UseSkillsDisplayInfo.js'
import {useSkillsDisplayAttributesState} from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import {useIframeInit} from '@/skills-display/iframe/UseIframeInit.js'
import {useSkillsDisplayParentFrameState} from '@/skills-display/stores/UseSkillsDisplayParentFrameState.js'
import NewSoftwareVersion from '@/components/header/NewSoftwareVersion.vue'
import {usePageVisitService} from '@/components/utils/services/UsePageVisitService.js'
import {invoke, until} from '@vueuse/core'
import DashboardFooter from '@/components/header/DashboardFooter.vue'
import {useUserAgreementInterceptor} from '@/interceptors/UseUserAgreementInterceptor.js'
import PkiAppBootstrap from '@/components/access/PkiAppBootstrap.vue'
import ScrollToTop from '@/common-components/utilities/ScrollToTop.vue'
import IconManagerService from '@/components/utils/iconPicker/IconManagerService.js'
import log from 'loglevel';
import {useUserPreferences} from "@/stores/UseUserPreferences.js";
import {useMatomoSupport} from "@/stores/UseMatomoSupport.js";
import {useAiPromptState} from '@/common-components/utilities/learning-conent-gen/UseAiPromptState.js'

const authState = useAuthState()
const appInfoState = useAppInfoState()
const appConfig = useAppConfig()
const skillsDisplayInfo = useSkillsDisplayInfo()
const accessState = useAccessState()
const errorHandling = useErrorHandling()
const userAgreementInterceptor = useUserAgreementInterceptor()
const route = useRoute()
const matomo = useMatomoSupport()
const aiPrompts = useAiPromptState()

const customGlobalValidators = useCustomGlobalValidators()
const globalNavGuards = useGlobalNavGuards()
const skillsDisplayAttributes = useSkillsDisplayAttributesState()

const isAppLoaded = ref(false)
const isScrollToTopDisabled = computed(() => {
  return appConfig.disableScrollToTop === 'true' || appConfig.disableScrollToTop === true;
})
const isLoadingApp = computed(() => !isAppLoaded.value || appConfig.isLoadingConfig || authState.restoringSession || (skillsDisplayAttributes.loadingConfig && skillsDisplayInfo.isSkillsDisplayPath()))

const themeHelper = useThemesHelper()

const pageVisitService = usePageVisitService()
const userPreferences = useUserPreferences()
const initializeEmbeddedDisplay = (requestConfig) => {
  const parentState = useSkillsDisplayParentFrameState()
  // Icons are optional presentation support, not an iframe readiness dependency.
  // Catch synchronous errors too; HTTP 401s still use iframe authentication handling.
  Promise.resolve()
    .then(() => IconManagerService.refreshCustomIconCss(skillsDisplayAttributes.projectId, null, requestConfig))
    .catch((error) => {
      if (!axios.isCancel(error) && parentState.isCurrentAuthRequest(error?.config)) {
        log.warn('Unable to load optional Skills Display custom icons')
      }
    })
  try {
    initMatomo()
  } catch {
    log.warn('Unable to initialize optional Skills Display analytics')
  }
  isAppLoaded.value = true
}
const loadDashboardUserInfo = () => {
  pageVisitService.reportPageVisit(route.path, route.fullPath)
  // These settings are consumed by dashboard features, not the embedded learner UI.
  const loadRoot = accessState.loadIsRoot()
  const loadEmailEnabled = appInfoState.loadEmailEnabled()
  const loadAiPromptSettings = aiPrompts.loadAiPromptSettings()
  const loadUserPreferences = userPreferences.loadUserPreferences().then(() => {
    return themeHelper.loadTheme()
  })
  return Promise.all([loadRoot, loadEmailEnabled, loadAiPromptSettings, loadUserPreferences]).then(() => {
    isAppLoaded.value = true
  })
}
watch(() => authState.userInfo, async (newUserInfo) => {
  // Embedded readiness is owned by its startup/recovery path. Do not launch
  // dashboard-only loaders when the iframe stores a newly authenticated user.
  if (skillsDisplayInfo.isSkillsClientPath()) return
  if (newUserInfo) {
    await loadDashboardUserInfo()
    initMatomo()
  } else {
    isAppLoaded.value = true
  }
})

watch(() => themeHelper.currentTheme, (newTheme, oldTheme) => {
  document.documentElement.classList.toggle('st-dark-theme');
})

const iframeInit = useIframeInit(isLoadingApp, initializeEmbeddedDisplay)
const isLoadingDisplay = computed(() => isLoadingApp.value || iframeInit.isRefreshingDisplay.value)
onBeforeMount(() => {
  if (skillsDisplayInfo.isSkillsClientPath()) {
    log.trace('App.vue: skillsDisplayInfo.isSkillsClientPath()=true, initiating iframe handshake')
    iframeInit.handleHandshake()
  }
  errorHandling.registerErrorHandling()
  userAgreementInterceptor.register()
  customGlobalValidators.addValidators()
})

onMounted(() => {
  invoke(async () => {
    if (skillsDisplayInfo.isSkillsClientPath()) {
      log.trace('App.vue: skillsDisplayInfo.isSkillsClientPath()=true, waiting for iframe to load')
      await until(() => iframeInit.loadedIframe.value || Boolean(iframeInit.initializationError.value)).toBe(true)
      if (iframeInit.initializationError.value) return
      log.debug('App.vue: skillsDisplayInfo.isSkillsClientPath()=true, loaded iframe!')
      await iframeInit.initializeDisplay({ startup: true })
      return
    }
    loadConfigs().catch(() => {
      // Dashboard failures are displayed by the shared request-error handler.
    })
  })
})

const loadConfigs = () => {
  const loadUserAndDisplayConfig = () => {
    const loadDisplayConfig = () => {
      return skillsDisplayAttributes.loadConfigStateIfNeeded().then(() => {
        globalNavGuards.addNavGuards()
        if (!authState.isAuthenticated) {
          isAppLoaded.value = true
        }
        // Authenticated dashboard readiness is owned by the userInfo watcher.
        initMatomo()
      })
    }
    return authState.restoreSessionIfAvailable().finally(loadDisplayConfig)
  }
  // Dashboard startup still installs navigation guards when public config fails.
  return appConfig.loadConfigState().finally(loadUserAndDisplayConfig)
}

const initMatomo = () => {
  if (skillsDisplayInfo.isSkillsClientPath()){
    matomo.init({ isSkillsClient: true, projectId: skillsDisplayAttributes.projectId })
  } else if (authState.isAuthenticated) {
    matomo.init()
  }
}

const notSkillsClient = computed(() => !skillsDisplayInfo.isSkillsClientPath())
const showHeader = computed(() => notSkillsClient.value && authState.isAuthenticated)
const isPkiAndNeedsToBootstrap = computed(() => appConfig.isPkiAuthenticated && appConfig.needToBootstrap)
const isSAML2AndNeedsToBootstrap = computed(() => appConfig.isSAML2Authenticated && appConfig.needToBootstrap)
const inBootstrapMode = computed(() => ((isPkiAndNeedsToBootstrap.value || isSAML2AndNeedsToBootstrap.value) && notSkillsClient.value))
const isCustomizableHeader = computed(() => notSkillsClient.value && !isLoadingApp.value && !inBootstrapMode.value)
const isDashboardFooter = computed(() => notSkillsClient.value && !isLoadingApp.value && !inBootstrapMode.value)
</script>

<template>
  <div role="presentation"
       class="m-0 bg-surface-50 dark:bg-surface-950"
       :class="{ 'h-screen in-skills-client': skillsDisplayInfo.isSkillsClientPath() }">
    <VueAnnouncer class="sr-only" />

    <customizable-header v-if="isCustomizableHeader" role="region" aria-label="dynamic customizable header"></customizable-header>
    <div id="skilltree-main-container" :class="{ 'flow-root': skillsDisplayInfo.isSkillsClientPath() }">
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
      <div v-else-if="isLoadingDisplay" role="main" class="flex content-center justify-center flex-wrap" style="min-height: 40rem">
        <div class="flex items-center justify-center m-2">
          <skills-spinner :is-loading="true" class="text-center"/>
          <h1 class="text-sm sr-only">Loading...</h1>
        </div>
      </div>
      <div v-if="!isLoadingDisplay && !iframeInit.initializationError.value && !iframeInit.authenticationError.value" class="m-0">
        <pki-app-bootstrap v-if="inBootstrapMode" role="region"/>

        <div v-if="!inBootstrapMode" :class="{ 'overall-container' : notSkillsClient, 'sd-theme-background-color': !notSkillsClient }">
          <new-software-version  />
          <dashboard-header v-if="showHeader" role="banner" />
          <div role="main" id="mainContent1"
               :class="{ 'px-3': !skillsDisplayInfo.isSkillsClientPath() }"
               tabindex="-1"
               aria-label="Main content area, click tab to navigate">
            <RouterView />
          </div>
        </div>
        <ConfirmDialog :pt="{ root: { class: 'w-2/3' } }"></ConfirmDialog>
        <dashboard-footer v-if="isDashboardFooter" role="region" />
        <customizable-footer v-if="isDashboardFooter" role="region" aria-label="dynamic customizable footer"></customizable-footer>
        <scroll-to-top v-if="!isScrollToTopDisabled && !inBootstrapMode" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.overall-container {
  min-height: calc(100vh - 120px);
}

</style>

<style>
body a:hover, body a:focus {
  text-decoration: underline !important;
}

@media (forced-colors: active) {
  :focus {
    outline: 2px solid transparent;
  }
}
</style>
