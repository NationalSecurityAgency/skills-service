import { computed } from 'vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

export const useUserTagsUtils = () => {
  const appConfig = useAppConfig()

  const showUserTagColumn = computed(() => !!(appConfig.usersTableAdditionalUserTagKey && appConfig.usersTableAdditionalUserTagLabel))
  const userTagKey = computed(() => appConfig.usersTableAdditionalUserTagKey)
  const userTagLabel = computed(() => appConfig.usersTableAdditionalUserTagLabel)

  return {
    showUserTagColumn,
    userTagKey,
    userTagLabel
  }
}