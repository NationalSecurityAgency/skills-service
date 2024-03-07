import { computed } from 'vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

export const useUserTagsUtils = () => {
  const appConfig = useAppConfig()

  const showUserTagColumn = computed(() => !!(appConfig.usersTableAdditionalUserTagKey && appConfig.usersTableAdditionalUserTagLabel))
  const userTagKey = computed(() => appConfig.userTagKey)
  const userTagLabel = computed(() => appConfig.userTagLabel)

  return {
    showUserTagColumn,
    userTagKey,
    userTagLabel
  }
}