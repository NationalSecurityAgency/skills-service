<script setup>
import { computed, nextTick, onMounted, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { SkillsConfiguration, SkillsReporter } from '@skilltree/skills-client-js'
import IconManagerService from '@/components/utils/iconPicker/IconManagerService.js'
import DashboardHeader from '@/components/header/DashboardHeader.vue'
import router from '@/router/index.js'
import { usePageVisitService } from '@/components/utils/services/UsePageVisitService.js'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import { useCustomGlobalValidators } from '@/validators/UseCustomGlobalValidators.js'
import { useInceptionConfigurer } from '@/components/utils/UseInceptionConfigurer.js'
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'
import { useClientDisplayPath } from '@/stores/UseClientDisplayPath.js'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useQuizConfig } from '@/stores/UseQuizConfig.js'
import { useProjectInfo } from '@/common-components/stores/UseCurrentProjectInfo.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAuthState } from '@/stores/UseAuthState.js'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import { useAccessState } from '@/stores/UseAccessState.js'
import ConfirmDialog from 'primevue/confirmdialog'

const authState = useAuthState()
const appInfoState = useAppInfoState()
const appConfig = useAppConfig()
const projectInfo = useProjectInfo()
const route = useRoute()
const projConfig = useProjConfig()
const quizConfig = useQuizConfig()
const clientDisplayPath = useClientDisplayPath()
const pageVisitService = usePageVisitService()
const accessState = useAccessState()
const activeProjectId = computed(() => {
  return projectInfo.currentProjectId
})

const addCustomIconCSS = () => {
  // This must be done here AFTER authentication
  IconManagerService.refreshCustomIconCss(activeProjectId, accessState.isSupervisor)
}

const isAuthenticatedUser = computed(() => {
  return authState.isAuthenticated
})

const isLoadingApp = computed(() => {
  return appConfig.loadingConfig || authState.restoringSession
})
const showUserAgreement = computed(() => {
    return appInfoState.showUa
  })

const themeHelper = useThemesHelper()
themeHelper.configureDefaultThemeFileInHeadTag()

const isActiveProjectIdChange = (to, from) => to.params.projectId !== from.params.projectId
const isAdminPage = (route) => route.path.startsWith('/administrator')
const isLoggedIn = () => authState.isAuthenticated
const isPki = () => appConfig.isPkiAuthenticated
const getLandingPage = () => authState.userInfo?.landingPage === 'admin' ? 'AdminHomePage' : 'MyProgressPage'

const inceptionConfigurer = useInceptionConfigurer()
watch(() => authState.userInfo, async (newUserInfo) => {
  if (newUserInfo) {
    inceptionConfigurer.configure()
  }
})

const beforeEachNavGuard = (to, from, next) => {
  if (to.query) {
    const { skillsClientDisplayPath } = to.query
    clientDisplayPath.setClientPathInfo({
      path: skillsClientDisplayPath,
      fromDashboard: true
    })
  }
  const requestAccountPath = '/request-root-account'
  if (
    !isPki() &&
    !isLoggedIn() &&
    to.path !== requestAccountPath &&
    appConfig.needToBootstrap
  ) {
    next({ path: requestAccountPath })
  } else if (
    !isPki() &&
    to.path === requestAccountPath &&
    !appConfig.needToBootstrap
  ) {
    next({ name: getLandingPage() })
  } else {
    /* eslint-disable no-lonely-if */
    if (appInfoState.showUa && to.path !== '/user-agreement' && to.path !== '/skills-login') {
      let p = ''
      if (to.query?.redirect) {
        p = to.query.redirect
      } else {
        p = to.fullPath
      }
      const ua =
        p !== '/' ? { name: 'UserAgreement', query: { redirect: p } } : { name: 'UserAgreement' }
      next(ua)
    } else {
      if (to.path === '/') {
        const landingPageRoute = { name: getLandingPage() }
        next(landingPageRoute)
      }
      if (from && from.path !== '/error') {
        appInfoState.setPreviousUrl(from.fullPath)
      }
      if (isActiveProjectIdChange(to, from)) {
        projectInfo.setCurrentProjectId(to.params.projectId)
        if (isAdminPage(to) && to.params.projectId) {
          projConfig.loadProjConfigState({ projectId: to.params.projectId })
        }
      }
      if (
        to.path.startsWith('/administrator/quizzes/') &&
        to.params.quizId &&
        (!quizConfig.quizConfig || to.params.quizId !== from.params.quizId)
      ) {
        quizConfig.loadQuizConfigState({ quizId: to.params.quizId })
      }
      if (to.matched.some((record) => record.meta.requiresAuth)) {
        // this route requires auth, check if logged in if not, redirect to login page.
        if (!isLoggedIn()) {
          const newRoute = { query: { redirect: to.fullPath } }
          if (isPki()) {
            newRoute.name = getLandingPage()
          } else {
            newRoute.name = 'Login'
          }
          next(newRoute)
        } else {
          next()
        }
      } else {
        next()
      }
    }
  }
}

const addNavGuards = () => {
  router.beforeEach(beforeEachNavGuard)
  const DEFAULT_TITLE = 'SkillTree Dashboard'
  router.afterEach((to, from) => {
    if (to.meta.reportSkillId) {
      SkillsConfiguration.afterConfigure().then(() => {
        SkillsReporter.reportSkill(to.meta.reportSkillId)
      })
    }
    if (isPki() || isLoggedIn()) {
      pageVisitService.reportPageVisit(to.path, to.fullPath)
    }
    // Use next tick to handle router history correctly
    // see: https://github.com/vuejs/vue-router/issues/914#issuecomment-384477609
    nextTick(() => {
      let newTitle = DEFAULT_TITLE
      if (to && to.meta && to.meta.announcer && to.meta.announcer.message) {
        newTitle = `${DEFAULT_TITLE} - ${to.meta.announcer.message}`
      }
      document.title = newTitle
    })

    // this hack is needed because otherwise when navigating between
    // pages the focus is placed onto the next visible element which in case of
    // drilling-down (for example projects page into a single project page)
    // the focus is placed on the next tabbable element which happens to in the footer
    // (when skills.config.ui.supportLinkN properties are utilized)
    if (from.name !== to.name) {
      setTimeout(() => {
        nextTick(() => {
          const preSkipButtonPlaceholder = document.querySelector('#preSkipToContentPlaceholder')
          if (preSkipButtonPlaceholder) {
            preSkipButtonPlaceholder.setAttribute('tabindex', 0)
            preSkipButtonPlaceholder.focus()
            preSkipButtonPlaceholder.setAttribute('tabindex', -1)
          }
        })
      }, 150)
    }
  })

  if (!isLoggedIn() && router.currentRoute.value.name !== 'Login') {
    router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
  }
}

const customGlobalValidators = useCustomGlobalValidators()

onMounted(() => {
  appConfig.loadConfigState().finally(() => {
    customGlobalValidators.addValidators()
    if (isAdminPage(route) && route.params.projectId) {
      projConfig.loadProjConfigState({ projectId: route.params.projectId })
    }
    authState.restoreSessionIfAvailable().finally(() => {
      inceptionConfigurer.configure()
      addNavGuards()
      const navTo = (navItem) => {
        if (navItem) {
          router.push(navItem)
        }
      }
      beforeEachNavGuard(route, route, navTo)
      if (isAuthenticatedUser.value) {

        accessState.loadIsSupervisor().then(() => {
          addCustomIconCSS()
        })
        accessState.loadIsRoot()
        appInfoState.loadEmailEnabled()
      }
    })
  })
})
</script>

<template>
  <div role="presentation" class="surface-ground">
    <VueAnnouncer class="sr-only" />

    <!--    <customizable-header role="region" aria-label="dynamic customizable header"></customizable-header>-->
    <div id="app" class="px-3">
      <skills-spinner :is-loading="isLoadingApp" class="mt-8 text-center"/>
      <div v-if="!isLoadingApp" class="m-0">
        <div class="">
          <!--          <pki-app-bootstrap v-if="isPkiAndNeedsToBootstrap || isOAuthOnlyAndNeedsToBootstrap" role="alert"/>-->
          <dashboard-header v-if="isAuthenticatedUser && !showUserAgreement" role="banner" />
          <div role="main">
            <RouterView
              id="mainContent1"
              tabindex="-1"
              aria-label="Main content area, click tab to navigate" />
          </div>
        </div>
      </div>
    </div>
    <ConfirmDialog></ConfirmDialog>
    <!--    <dashboard-footer />-->
    <!--    <customizable-footer role="region" aria-label="dynamic customizable footer"></customizable-footer>-->
    <!--    <scroll-to-top v-if="!isScrollToTopDisabled" />-->
  </div>
</template>

<style scoped>
</style>
