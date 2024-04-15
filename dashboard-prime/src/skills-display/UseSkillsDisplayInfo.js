import { useRoute } from 'vue-router'
import { computed } from 'vue'

export const useSkillsDisplayInfo = () => {
  const route = useRoute()
  const skillsClientContextAppend = 'SkillsClient'
  const localContextAppend = 'Local'
  const isSkillsDisplayPath = computed(() => {
    return route.path.startsWith('/static/clientPortal/')
  })
  const getContextSpecificRouteName = (name) => {
    if (isSkillsDisplayPath.value) {
      return `${name}${skillsClientContextAppend}`
    }
    return `${name}${localContextAppend}`
  }

  const cleanPath = (path) => {
    const cleanPath = path.replace('/static/clientPortal/index.html', '')
    return cleanPath || '/'
  }

  return {
    isSkillsDisplayPath,
    skillsClientContextAppend,
    localContextAppend,
    getContextSpecificRouteName,
    cleanPath
  }
}