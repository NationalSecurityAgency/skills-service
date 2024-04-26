<script setup>
import { computed, onMounted, onBeforeMount, watch } from 'vue'
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
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useIframeInit } from '@/skills-display/iframe/UseIframeInit.js'
import NewSoftwareVersion from '@/components/header/NewSoftwareVersion.vue'
import { usePageVisitService } from '@/components/utils/services/UsePageVisitService.js'
import { invoke, until, useCounter } from '@vueuse/core'

const authState = useAuthState()
const appInfoState = useAppInfoState()
const appConfig = useAppConfig()
const skillsDisplayInfo = useSkillsDisplayInfo()
const accessState = useAccessState()
const errorHandling = useErrorHandling()
const route = useRoute()

const customGlobalValidators = useCustomGlobalValidators()
const globalNavGuards = useGlobalNavGuards()
const skillsDisplayAttributes = useSkillsDisplayAttributesState()

const addCustomIconCSS = () => {
  // This must be done here AFTER authentication
  IconManagerService.refreshCustomIconCss(route.params.projectId, accessState.isSupervisor)
}

const isLoadingApp = computed(() => appConfig.isLoadingConfig || authState.restoringSession || (skillsDisplayAttributes.loadingConfig && skillsDisplayInfo.isSkillsDisplayPath()))

const themeHelper = useThemesHelper()
themeHelper.configureDefaultThemeFileInHeadTag()

const inceptionConfigurer = useInceptionConfigurer()
const pageVisitService = usePageVisitService()
watch(() => authState.userInfo, async (newUserInfo) => {
  if (newUserInfo) {
    inceptionConfigurer.configure()
    pageVisitService.reportPageVisit(route.path, route.fullPath)
  }
})

const iframeInit = useIframeInit()
onBeforeMount(() => {
  iframeInit.handleHandshake()
  errorHandling.registerErrorHandling()
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

const loadConfigs = () => {
  appConfig.loadConfigState().finally(() => {
    authState.restoreSessionIfAvailable().finally(() => {
      skillsDisplayAttributes.loadConfigStateIfNeeded().then(() => {

        inceptionConfigurer.configure()
        globalNavGuards.addNavGuards()
        if (authState.isAuthenticated) {
          accessState.loadIsSupervisor().then(() => {
            addCustomIconCSS()
          })
          accessState.loadIsRoot()
          appInfoState.loadEmailEnabled()
        }
      })
    })
  })
}

const showHeader = computed(() => {
  return !skillsDisplayInfo.isSkillsClientPath() && authState.isAuthenticated && !appInfoState.showUa
})
</script>

<template>
  <div role="presentation" class="surface-ground m-0">
    <VueAnnouncer class="sr-only" />

    <customizable-header role="region" aria-label="dynamic customizable header"></customizable-header>
    <div id="app" :class="{ 'px-3': !skillsDisplayInfo.isSkillsClientPath() }">
      <skills-spinner :is-loading="isLoadingApp" class="mt-8 text-center" />
      <div v-if="!isLoadingApp" class="m-0">
        <div class="">
          <!--          <pki-app-bootstrap v-if="isPkiAndNeedsToBootstrap || isOAuthOnlyAndNeedsToBootstrap" role="alert"/>-->
          <new-software-version class="mb-3"/>
          <dashboard-header v-if="showHeader" role="banner" />
          <div role="main" id="mainContent1"
               tabindex="-1"
               aria-label="Main content area, click tab to navigate">
            <RouterView  />
          </div>
        </div>
      </div>
    </div>
    <ConfirmDialog></ConfirmDialog>
    <!--    <dashboard-footer />-->
    <customizable-footer role="region" aria-label="dynamic customizable footer"></customizable-footer>
    <!--    <scroll-to-top v-if="!isScrollToTopDisabled" />-->
  </div>
</template>

<style scoped>
</style>
