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
import { computed, onMounted, onBeforeMount, watch, ref } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import IconManagerService from '@/components/utils/iconPicker/IconManagerService.js'
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
import {usePrimeVue} from "primevue/config";
import ScrollToTop from "@/common-components/utilities/ScrollToTop.vue";

const authState = useAuthState()
const appInfoState = useAppInfoState()
const appConfig = useAppConfig()
const skillsDisplayInfo = useSkillsDisplayInfo()
const accessState = useAccessState()
const errorHandling = useErrorHandling()
const userAgreementInterceptor = useUserAgreementInterceptor()
const route = useRoute()
const PrimeVue = usePrimeVue()

const customGlobalValidators = useCustomGlobalValidators()
const globalNavGuards = useGlobalNavGuards()
const skillsDisplayAttributes = useSkillsDisplayAttributesState()

const addCustomIconCSS = () => {
  // This must be done here AFTER authentication
  IconManagerService.refreshCustomIconCss(route.params.projectId, accessState.isSupervisor)
}

const isAppLoaded = ref(false)
const isScrollToTopDisabled = computed(() => {
  return appConfig.disableScrollToTop === 'true' || appConfig.disableScrollToTop === true;
})
const isLoadingApp = computed(() => !isAppLoaded.value || appConfig.isLoadingConfig || authState.restoringSession || (skillsDisplayAttributes.loadingConfig && skillsDisplayInfo.isSkillsDisplayPath()))

const themeHelper = useThemesHelper()
themeHelper.configureDefaultThemeFileInHeadTag()

const inceptionConfigurer = useInceptionConfigurer()
const pageVisitService = usePageVisitService()
watch(() => authState.userInfo, async (newUserInfo) => {
  if (newUserInfo) {
    inceptionConfigurer.configure()
    pageVisitService.reportPageVisit(route.path, route.fullPath)
    loadUserRoles()
    appInfoState.loadEmailEnabled()
    themeHelper.loadTheme()
  }

  isAppLoaded.value = true
})

watch(() => themeHelper.currentTheme, (newTheme, oldTheme) => {
  PrimeVue.changeTheme(oldTheme.value, newTheme.value, 'theme-link')
})

const iframeInit = useIframeInit()
onBeforeMount(() => {
  iframeInit.handleHandshake()
  errorHandling.registerErrorHandling()
  userAgreementInterceptor.register()
  customGlobalValidators.addValidators()
})

onMounted(() => {
  invoke(async () => {
    if (skillsDisplayInfo.isSkillsClientPath()) {
      await until(iframeInit.loadedIframe).toBe(true)
    }
    loadConfigs()
  })
})

const loadUserRoles = () => {
  const loadSuperVisorRole = accessState.loadIsSupervisor().then(() => {
    addCustomIconCSS()
  })
  const loadRootRole = accessState.loadIsRoot()
  return Promise.all([loadSuperVisorRole, loadRootRole])
}
const loadConfigs = () => {
  appConfig.loadConfigState().finally(() => {
    authState.restoreSessionIfAvailable().finally(() => {
      skillsDisplayAttributes.loadConfigStateIfNeeded().then(() => {

        inceptionConfigurer.configure()
        globalNavGuards.addNavGuards()
        if (!authState.isAuthenticated) {
          isAppLoaded.value = true
        }
        // do not need to set isAppLoaded to true here because it will be handled
        // by the watch of authState.userInfo
      })
    })
  })
}

const notSkillsClient = computed(() => !skillsDisplayInfo.isSkillsClientPath())
const showHeader = computed(() => notSkillsClient.value && authState.isAuthenticated)
const isPkiAndNeedsToBootstrap = computed(() => appConfig.isPkiAuthenticated && appConfig.needToBootstrap)
const inBootstrapMode = computed(() => isPkiAndNeedsToBootstrap.value && notSkillsClient.value)
const isCustomizableHeader = computed(() => notSkillsClient.value && !isLoadingApp.value && !inBootstrapMode.value)
const isDashboardFooter = computed(() => notSkillsClient.value && !isLoadingApp.value && !inBootstrapMode.value)
</script>

<template>
  <div role="presentation"
       :class="{
          'skills-dark-theme': themeHelper.isDarkTheme,
          'skills-light-theme': !themeHelper.isDarkTheme
       }"
       class="m-0 surface-ground skills-light-theme">
    <VueAnnouncer class="sr-only" />

    <customizable-header v-if="isCustomizableHeader" role="region" aria-label="dynamic customizable header"></customizable-header>
    <div id="skilltree-main-container">
      <div v-if="isLoadingApp" role="main" class="text-center">
        <skills-spinner :is-loading="true" class="mt-8 text-center"/>
        <h1 class="text-sm sr-only">Loading...</h1>
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
      </div>
    </div>
    <ConfirmDialog>
      <template #message="slotProps">
        <div>
          <Message :closable="false">{{ slotProps.message.message }}</Message>
        </div>
      </template>
    </ConfirmDialog>
    <dashboard-footer v-if="isDashboardFooter" role="region" />
    <customizable-footer v-if="isDashboardFooter" role="region" aria-label="dynamic customizable footer"></customizable-footer>
    <scroll-to-top v-if="!isScrollToTopDisabled && !inBootstrapMode" />
  </div>
</template>

<style scoped>
.overall-container {
  min-height: calc(100vh - 100px);
}

</style>

<style>
body a, a:link, a:visited {
  text-decoration: none !important;
}

body .skills-light-theme a, a:link {
  color: #2f64bd !important;
}

body .skills-light-theme a:visited {
  color: #784f9f !important;
}

body .skills-dark-theme a, a:link {
  color: #99befb !important;
}

body .skills-dark-theme a:visited {
  color: #d5aafb !important;
}

body a:hover, body a:focus {
  text-decoration: underline !important;
}
</style>
