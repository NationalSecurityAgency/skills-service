import { nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuizConfig } from '@/stores/UseQuizConfig.js'
import { useClientDisplayPath } from '@/stores/UseClientDisplayPath.js'
import { usePageVisitService } from '@/components/utils/services/UsePageVisitService.js'
import { useAuthState } from '@/stores/UseAuthState.js'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useProjectInfo } from '@/common-components/stores/UseCurrentProjectInfo.js'
import { SkillsConfiguration, SkillsReporter } from '@skilltree/skills-client-js'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import IconManagerService from '@/components/utils/iconPicker/IconManagerService.js'
import { useAccessState } from '@/stores/UseAccessState.js'

export const useGlobalNavGuards = () => {

  const quizConfig = useQuizConfig()
  const clientDisplayPath = useClientDisplayPath()
  const pageVisitService = usePageVisitService()
  const authState = useAuthState()
  const appInfoState = useAppInfoState()
  const appConfig = useAppConfig()
  const projectInfo = useProjectInfo()
  const accessState = useAccessState()
  const projConfig = useProjConfig()
  const router = useRouter()
  const route = useRoute()

  const isAdminPage = (route) => route.path.startsWith('/administrator')
  const isActiveProjectIdChange = (to, from) => to.params.projectId !== from.params.projectId
  const isLoggedIn = () => authState.isAuthenticated
  const isPki = () => appConfig.isPkiAuthenticated
  const getLandingPage = () => authState.userInfo?.landingPage === 'admin' ? 'AdminHomePage' : 'MyProgressPage'

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
          IconManagerService.refreshCustomIconCss(to.params.projectId, accessState.isSupervisor)
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

    const navTo = (navItem) => {
      if (navItem) {
        router.push(navItem)
      }
    }
    // because the navigation guards are added after App loaded must execute the very first time manually
    beforeEachNavGuard(route, {params: {}}, navTo)
  }

  return {
    addNavGuards
  }
}