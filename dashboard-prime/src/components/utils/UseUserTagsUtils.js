import { computed } from 'vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

export const useUserTagsUtils = () => {
  const appConfig = useAppConfig()

  const showUserTagColumn = () => { return !!(appConfig.usersTableAdditionalUserTagKey && appConfig.usersTableAdditionalUserTagLabel) }
  const userTagKey = () => { return appConfig.usersTableAdditionalUserTagKey }
  const userTagLabel = () => { return appConfig.usersTableAdditionalUserTagLabel }

  return {
    showUserTagColumn,
    userTagKey,
    userTagLabel
  }
}