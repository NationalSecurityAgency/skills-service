import { useRoute } from 'vue-router'
import { computed } from 'vue'

export const useSkillsDisplayInfo = () => {
  const route = useRoute()

  const isSkillsDisplayPath = computed(() => {
    return route.path.startsWith('/static/clientPortal/')
  })

  return {
    isSkillsDisplayPath
  }
}