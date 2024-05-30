import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'
import SkillsDisplayPathAppendValues from '@/router/SkillsDisplayPathAppendValues.js'

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
    return route.path.startsWith('/static/clientPortal/')
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
    return route.name === getContextSpecificRouteName('globalBadgeDetails')
  })

  const createToBadgeLink = (badge) => {
    const name = badge.global ? 'globalBadgeDetails' : 'badgeDetails'
    return { name: getContextSpecificRouteName(name), params: { badgeId: badge.badgeId } }
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