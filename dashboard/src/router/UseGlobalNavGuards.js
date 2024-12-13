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
import { nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuizConfig } from '@/stores/UseQuizConfig.js'
import { usePageVisitService } from '@/components/utils/services/UsePageVisitService.js'
import { useAuthState } from '@/stores/UseAuthState.js'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useProjectInfo } from '@/common-components/stores/UseCurrentProjectInfo.js'
import { SkillsConfiguration, SkillsReporter } from '@skilltree/skills-client-js'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import IconManagerService from '@/components/utils/iconPicker/IconManagerService.js'
import { useAccessState } from '@/stores/UseAccessState.js'
import { useInviteOnlyProjectState } from '@/stores/UseInviteOnlyProjectState.js'
import { useLog } from '@/components/utils/misc/useLog.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import PathAppendValues from '@/router/SkillsDisplayPathAppendValues.js'

export const useGlobalNavGuards = () => {

  const quizConfig = useQuizConfig()
  const pageVisitService = usePageVisitService()
  const authState = useAuthState()
  const appInfoState = useAppInfoState()
  const appConfig = useAppConfig()
  const projectInfo = useProjectInfo()
  const inviteOnlyProjectState = useInviteOnlyProjectState()
  const accessState = useAccessState()
  const projConfig = useProjConfig()
  const router = useRouter()
  const route = useRoute()
  const skillsDisplayAttributes = useSkillsDisplayAttributesState()

  const log = useLog()

  const isAdminPage = (route) => route.path?.toLowerCase().startsWith('/administrator') && !route.path?.toLowerCase().startsWith('/administrator/skills')
  const isGlobalAdminPage = (route) => route.path?.toLowerCase().startsWith('/administrator/globalbadges')
  const isActiveProjectIdChange = (to, from) => to.params.projectId !== from.params.projectId
  const isLoggedIn = () => authState.isAuthenticated
  const isPki = () => appConfig.isPkiAuthenticated
  const getLandingPage = () => authState.userInfo?.landingPage === 'admin' ? 'AdminHomePage' : 'MyProgressPage'

  const registrationId = () => appConfig.saml2RegistrationId;
  const isSaml2 = () => appConfig.isSAML2Authenticated;

  const beforeEachNavGuard = (to, from, next) => {
    const { skillsClientDisplayPath } = to.query
    const requestRootAccountPath = '/request-root-account'
    const skillsLoginPath = '/skills-login'

    if (
        !isPki() && !isSaml2() &&
        !isLoggedIn() &&
        to.path?.toLowerCase() !== requestRootAccountPath.toLowerCase() &&
        appConfig.needToBootstrap
    ) {
      next({ path: requestRootAccountPath })
    } else if (
        to.path?.toLowerCase() === requestRootAccountPath.toLowerCase() &&
        !appConfig.needToBootstrap
    ) {
      next({ name: getLandingPage() })
    } else if (
      ((isPki() || isLoggedIn()) &&
      (to.path === skillsLoginPath || to.path === `${skillsLoginPath}/`))
    ) {
      next(route.query.redirect || { name: getLandingPage() })
    } else {
      /* eslint-disable no-lonely-if */
      if (appInfoState.showUa && to.path?.toLowerCase() !== '/user-agreement' && to.path?.toLowerCase() !== '/skills-login') {
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
            inviteOnlyProjectState.loadInviteOnlySetting(to.params.projectId)
          }
          IconManagerService.refreshCustomIconCss(to.params.projectId, accessState.isSupervisor)
        }
        if (isGlobalAdminPage(to)) {
          IconManagerService.refreshCustomIconCss(null, true)
        }
        if (
          to.path?.toLowerCase().startsWith('/administrator/quizzes/') &&
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
          		newRoute.name = getLandingPage();
	          } else if (isSaml2()) {
                window.location = `/saml2/authenticate/${registrationId()}`;
              } else {
	            newRoute.name = 'Login';
	          }
            next(newRoute)
          } else {
            if((to.name?.endsWith(PathAppendValues.Local) || to.name?.endsWith(PathAppendValues.Inception)) && skillsClientDisplayPath) {
              const newRoute = to.path + skillsClientDisplayPath;
              const nextRoute = '/redirect?nextPage=' + newRoute
              next(nextRoute)
            }
            else {
              next()
            }
          }
        } else {
          next()
        }
      }
    }

  }

  const addNavGuards = () => {
    router.beforeEach(beforeEachNavGuard)
    router.beforeEach((to, from, next) => {
      skillsDisplayAttributes.loadConfigStateIfNeeded(to)
      next()
    })
    const DEFAULT_TITLE = 'SkillTree Dashboard'
    router.afterEach((to, from) => {

      log.debug(`GlobalNavGuard: afterEach nav to:${to.path}`)
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

    // this is handled in beforeEachNavGuard
    // if (!isLoggedIn() && router.currentRoute.value.name !== 'Login') {
    //   router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    // }

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
