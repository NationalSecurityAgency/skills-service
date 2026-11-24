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
import SkillsDisplayIframeHost from '@/skills-display/iframe/SkillsDisplayIframeHost.vue'
import NewSoftwareVersion from '@/components/header/NewSoftwareVersion.vue'
import {usePageVisitService} from '@/components/utils/services/UsePageVisitService.js'
import DashboardFooter from '@/components/header/DashboardFooter.vue'
import {useUserAgreementInterceptor} from '@/interceptors/UseUserAgreementInterceptor.js'
import PkiAppBootstrap from '@/components/access/PkiAppBootstrap.vue'
import ScrollToTop from '@/common-components/utilities/ScrollToTop.vue'
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

onBeforeMount(() => {
  errorHandling.registerErrorHandling()
  userAgreementInterceptor.register()
  customGlobalValidators.addValidators()
})

onMounted(() => {
  if (skillsDisplayInfo.isSkillsClientPath()) return
  loadConfigs().catch(() => {
    // Dashboard failures are displayed by the shared request-error handler.
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
  if (authState.isAuthenticated) {
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
      <skills-display-iframe-host v-if="skillsDisplayInfo.isSkillsClientPath()" />
      <template v-else>
        <div v-if="isLoadingApp" role="main" class="flex content-center justify-center flex-wrap" style="min-height: 40rem">
          <div class="flex items-center justify-center m-2">
            <skills-spinner :is-loading="true" class="text-center"/>
            <h1 class="text-sm sr-only">Loading...</h1>
          </div>
        </div>
        <div v-else class="m-0">
          <pki-app-bootstrap v-if="inBootstrapMode" role="region"/>

          <div v-if="!inBootstrapMode" class="overall-container">
            <new-software-version  />
            <dashboard-header v-if="showHeader" role="banner" />
            <div role="main" id="mainContent1" class="px-3"
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
      </template>
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
