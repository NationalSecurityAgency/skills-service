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
import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'
import SkillsDisplayPathAppendValues from '@/router/SkillsDisplayPathAppendValues.js'
import SkillsClientPath from '@/router/SkillsClientPath.js'

export const useSkillsDisplayInfo = () => {
  const route = useRoute()
  const router = useRouter()
  const skillsClientContextAppend = SkillsDisplayPathAppendValues.SkillsClient
  const localContextAppend = SkillsDisplayPathAppendValues.Local
  const progressAndRankingsRegex = /\/progress-and-rankings\/projects\/[^/]*/i
  const localTestRegex = /\/test-skills-display\/[^/]*/i
  const clientDisplayRegex = /\/static\/clientPortal\/index\.html/i
  const inceptionRegex = /\/administrator\/skills\/.*/i
  const skillDisplayPreviewRegex = /\/administrator\/projects\/.*\/users\/.*/i
  const regexes = [progressAndRankingsRegex, localTestRegex, clientDisplayRegex, inceptionRegex, skillDisplayPreviewRegex]
  const localTestContextAppend = SkillsDisplayPathAppendValues.LocalTest

  const isSkillsClientPath = () => {
    return window?.location?.pathname.startsWith(SkillsClientPath.RootUrl) || route.path?.startsWith('/test-skills-client/')
  }
  const getContextSpecificRouteName = (name) => {
    if (isSkillsClientPath()) {
      return `${name}${skillsClientContextAppend}`
    }
    if (route.path.startsWith('/progress-and-rankings')) {
      return `${name}${localContextAppend}`
    }
    if (route.path.match(inceptionRegex)) {
      return `${name}${SkillsDisplayPathAppendValues.Inception}`
    }
    if (route.path.match(skillDisplayPreviewRegex)) {
      return `${name}${SkillsDisplayPathAppendValues.SkillsDisplayPreview}`
    }
    return `${name}${localTestContextAppend}`
  }

  const cleanPath = (path) => {
    let cleanPath = path
    for (const regex of regexes) {
      cleanPath = cleanPath.replace(regex, '')
    }
    return cleanPath
  }

  const getRootUrl = () => {
    for (const regex of regexes) {
      let found = route.path.match(regex)
      if (found) {
        return found[0]
      }
    }
    return '/'
  }

  const isSkillsDisplayPath = (optionalpath = null) => {
    const pathToCheck = optionalpath || route.path
    for (const regex of regexes) {
      let found = pathToCheck.match(regex)
      if (found) {
        return true
      }
    }
    return false
  }

  const isLocalTestPath = () => {
    return route.path.startsWith('/test-skills-display/')
  }

  const routerPush = (pageName, params) => {
    router.push({ name: getContextSpecificRouteName(pageName), params })
  }

  const isHomePage = computed(() => {
    return route.name === getContextSpecificRouteName('SkillsDisplay')
  })
  const isSubjectPage = computed(() => {
    return route.name === getContextSpecificRouteName('SubjectDetailsPage')
  })
  const isGlobalBadgePage = computed(() => {
    return route.name === getContextSpecificRouteName('globalBadgeDetails') ||
      route.name === 'globalBadgeDetails'
  })

  const createToBadgeLink = (badge, globalBadgeUnderProjectId = null) => {
    let name = ''
    const params = { badgeId: badge.badgeId }

    if (badge.global) {
      name = getContextSpecificRouteName('globalBadgeDetails')
      if (!route.params.projectId) {
        if (!globalBadgeUnderProjectId) {
          throw new Error(`globalBadgeUnderProjectId is required for global badges, badgeId: [${badge.badgeId}]`)
        }
        params.projectId = globalBadgeUnderProjectId
      }
    } else {
      name = getContextSpecificRouteName('badgeDetails')
    }

    if (badge.projectId) {
      params.projectId = badge.projectId
    }

    const res = { name, params }
    return res
  }

  const isDependency = () => {
    const routeName = route.name
    return routeName === getContextSpecificRouteName('crossProjectSkillDetails') || routeName === getContextSpecificRouteName('crossProjectSkillDetailsUnderBadge')
  }
  const isCrossProject = () => {
    const routeName = route.name
    return routeName === getContextSpecificRouteName('crossProjectSkillDetails') || route.params.crossProjectId
  }

  return {
    isSkillsClientPath,
    isSkillsDisplayPath,
    skillsClientContextAppend,
    localContextAppend,
    localTestContextAppend,
    getContextSpecificRouteName,
    cleanPath,
    getRootUrl,
    routerPush,
    isSubjectPage,
    isGlobalBadgePage,
    createToBadgeLink,
    isDependency,
    isCrossProject,
    isLocalTestPath,
    isHomePage
  }
}