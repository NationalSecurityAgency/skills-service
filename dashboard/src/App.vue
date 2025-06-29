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
import { computed, onBeforeMount, onMounted, ref, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import DashboardHeader from '@/components/header/DashboardHeader.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import { useCustomGlobalValidators } from '@/validators/UseCustomGlobalValidators.js'
import { useInceptionConfigurer } from '@/components/utils/UseInceptionConfigurer.js'
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAuthState } from '@/stores/UseAuthState.js'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import { useAccessState } from '@/stores/UseAccessState.js'
import ConfirmDialog from 'primevue/confirmdialog'
import { useGlobalNavGuards } from '@/router/UseGlobalNavGuards.js'
import { useErrorHandling } from '@/interceptors/UseErrorHandling.js'
import CustomizableHeader from '@/components/customization/CustomizableHeader.vue'
import CustomizableFooter from '@/components/customization/CustomizableFooter.vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useIframeInit } from '@/skills-display/iframe/UseIframeInit.js'
import NewSoftwareVersion from '@/components/header/NewSoftwareVersion.vue'
import { usePageVisitService } from '@/components/utils/services/UsePageVisitService.js'
import { invoke, until } from '@vueuse/core'
import DashboardFooter from '@/components/header/DashboardFooter.vue'
import { useUserAgreementInterceptor } from '@/interceptors/UseUserAgreementInterceptor.js'
import PkiAppBootstrap from '@/components/access/PkiAppBootstrap.vue'
import ScrollToTop from '@/common-components/utilities/ScrollToTop.vue'
import IconManagerService from '@/components/utils/iconPicker/IconManagerService.js'
import log from 'loglevel';
import {useUserPreferences} from "@/stores/UseUserPreferences.js";

const authState = useAuthState()
const appInfoState = useAppInfoState()
const appConfig = useAppConfig()
const skillsDisplayInfo = useSkillsDisplayInfo()
const accessState = useAccessState()
const errorHandling = useErrorHandling()
const userAgreementInterceptor = useUserAgreementInterceptor()
const route = useRoute()

const customGlobalValidators = useCustomGlobalValidators()
const globalNavGuards = useGlobalNavGuards()
const skillsDisplayAttributes = useSkillsDisplayAttributesState()

const isAppLoaded = ref(false)
const isScrollToTopDisabled = computed(() => {
  return appConfig.disableScrollToTop === 'true' || appConfig.disableScrollToTop === true;
})
const isLoadingApp = computed(() => !isAppLoaded.value || appConfig.isLoadingConfig || authState.restoringSession || (skillsDisplayAttributes.loadingConfig && skillsDisplayInfo.isSkillsDisplayPath()))

const themeHelper = useThemesHelper()

const addCustomIconCSSForClientDisplay = () => {
  if (skillsDisplayInfo.isSkillsClientPath()) {
    IconManagerService.refreshCustomIconCss(skillsDisplayAttributes.projectId, false)
  }
}
const inceptionConfigurer = useInceptionConfigurer()
const pageVisitService = usePageVisitService()
const userPreferences = useUserPreferences()
const loadUserAndDisplayInfo = () => {
  inceptionConfigurer.configure()
  pageVisitService.reportPageVisit(route.path, route.fullPath)
  const loadRoot = accessState.loadIsRoot()
  const loadSupervisor = accessState.loadIsSupervisor()
  const loadEmailEnabled = appInfoState.loadEmailEnabled()
  const loadCustomIconCSS = addCustomIconCSSForClientDisplay()
  const promises = [loadRoot, loadSupervisor, loadEmailEnabled, loadCustomIconCSS]
  if (!skillsDisplayInfo.isSkillsClientPath()) {
    const loadUserPreferences =
        userPreferences.loadUserPreferences().then(() => {
          return themeHelper.loadTheme()
        })
    promises.push(loadUserPreferences)
  }
  return Promise.all(promises).then(() => {
    isAppLoaded.value = true
  })
}
watch(() => authState.userInfo, async (newUserInfo) => {
  if (newUserInfo) {
    await loadUserAndDisplayInfo()
  } else {
    isAppLoaded.value = true
  }
})

watch(() => themeHelper.currentTheme, (newTheme, oldTheme) => {
  document.documentElement.classList.toggle('st-dark-theme');
})

const iframeInit = useIframeInit()
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
      await until(iframeInit.loadedIframe).toBe(true)
      log.debug('App.vue: skillsDisplayInfo.isSkillsClientPath()=true, loaded iframe!')
    }
    loadConfigs()
  })
})

const restoreSessionIfAvailable = () => {
  if (skillsDisplayInfo.isSkillsClientPath()) {
    authState.setRestoringSession(false)
    return Promise.resolve()
  }
  return authState.restoreSessionIfAvailable()
}

const loadConfigs = () => {
  appConfig.loadConfigState().finally(() => {
    restoreSessionIfAvailable().finally(() => {
      skillsDisplayAttributes.loadConfigStateIfNeeded().then(() => {

        inceptionConfigurer.configure()
        if (!skillsDisplayInfo.isSkillsClientPath()) {
          globalNavGuards.addNavGuards()
        }
        if (!authState.isAuthenticated) {
          isAppLoaded.value = true
        }
        if (skillsDisplayInfo.isSkillsClientPath()) {
          loadUserAndDisplayInfo()
        } else {
          // do not need to set isAppLoaded to true here because it will be handled
          // by the watch of authState.userInfo
        }
      })
    })
  })
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
<!--  :class="{ 'st-dark-theme': themeHelper.isDarkTheme, 'st-light-theme': !themeHelper.isDarkTheme }"-->
  <div role="presentation"
       class="m-0 bg-surface-50 dark:bg-surface-950">
    <VueAnnouncer class="sr-only" />

    <customizable-header v-if="isCustomizableHeader" role="region" aria-label="dynamic customizable header"></customizable-header>
    <div id="skilltree-main-container">
      <div v-if="isLoadingApp" role="main" class="flex content-center justify-center flex-wrap" style="min-height: 40rem">
        <div class="flex items-center justify-center m-2">
          <skills-spinner :is-loading="true" class="text-center"/>
          <h1 class="text-sm sr-only">Loading...</h1>
        </div>
      </div>
      <div v-if="!isLoadingApp" class="m-0">
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
body a, a:link, a:visited {
  //text-decoration: none !important;
}

body .st-light-theme a, a:link {
  //color: #2f64bd !important;
}

body .st-light-theme a:visited {
  //color: #784f9f !important;
}

body .st-dark-theme a, a:link {
  //color: #99befb !important;
}

body .st-dark-theme a:visited {
  //color: #d5aafb !important;
}

body a:hover, body a:focus {
  text-decoration: underline !important;
}

@media (forced-colors: active) {
  :focus {
    outline: 2px solid transparent;
  }
}
</style>
