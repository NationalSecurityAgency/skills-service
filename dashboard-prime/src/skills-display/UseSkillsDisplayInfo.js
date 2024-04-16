import { useRoute } from 'vue-router'
import { computed } from 'vue'

export const useSkillsDisplayInfo = () => {
  const route = useRoute()
  const skillsClientContextAppend = 'SkillsClient'
  const localContextAppend = 'Local'
  const progressAndRankingsCleanRegex = /\/progress-and-rankings\/projects\/[^/]*/i
  const localTestContextAppend = 'LocalTest'
  const localTestCleanRegex = /\/test-skills-display\/[^/]*/i
  const isSkillsDisplayPath = computed(() => {
    return route.path.startsWith('/static/clientPortal/')
  })
  const getContextSpecificRouteName = (name) => {
    if (isSkillsDisplayPath.value) {
      return `${name}${skillsClientContextAppend}`
    }
    if (route.path.startsWith('/progress-and-rankings')) {
      return `${name}${localContextAppend}`
    }
    return `${name}${localTestContextAppend}`
  }

  const cleanPath = (path) => {
    let cleanPath = path.replace('/static/clientPortal/index.html', '')
    cleanPath = cleanPath.replace(progressAndRankingsCleanRegex, '')
    cleanPath = cleanPath.replace(localTestCleanRegex, '')
    return cleanPath || '/'
  }

  return {
    isSkillsDisplayPath,
    skillsClientContextAppend,
    localContextAppend,
    localTestContextAppend,
    getContextSpecificRouteName,
    cleanPath
  }
}