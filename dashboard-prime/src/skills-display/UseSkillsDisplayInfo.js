import { useRoute, useRouter } from 'vue-router'

export const useSkillsDisplayInfo = () => {
  const route = useRoute()
  const router = useRouter()
  const skillsClientContextAppend = 'SkillsClient'
  const localContextAppend = 'Local'
  const progressAndRankingsRegex = /\/progress-and-rankings\/projects\/[^/]*/i
  const localTestRegex = /\/test-skills-display\/[^/]*/i
  const clientDisplayRegex =/\/static\/clientPortal\/index\.html/i
  const regexes = [progressAndRankingsRegex, localTestRegex, clientDisplayRegex]
  const localTestContextAppend = 'LocalTest'

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
    return `${name}${localTestContextAppend}`
  }

  const cleanPath = (path) => {
    let cleanPath = path
    for(const regex of regexes) {
      cleanPath = cleanPath.replace(regex, '')
    }
    return cleanPath
  }

  const getRootUrl = () => {
    for(const regex of regexes) {
      let found = route.path.match(regex);
      if (found) {
        return found[0]
      }
    }
    return '/'
  }

  const isSkillsDisplayPath = (optionalpath = null) => {
    const pathToCheck = optionalpath || route.path
    for(const regex of regexes) {
      let found = pathToCheck.match(regex);
      if (found) {
        return true
      }
    }
    return false
  }

  const routerPush = (pageName, params) => {
    router.push({ name: getContextSpecificRouteName(pageName), params })
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
    routerPush
  }
}