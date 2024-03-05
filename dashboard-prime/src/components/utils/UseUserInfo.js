import { computed } from 'vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAuthState } from '@/stores/UseAuthState.js'

export const useUserInfo = () => {
  const appConfig = useAppConfig()
  const authState = useAuthState()

  const userInfo = computed(() => {
    return authState.userInfo
  })
  const isAuthenticated = computed(() => {
    return authState.isAuthenticated()
  })
  const isFormAuthenticatedUser = computed(() => {
    return isAuthenticated.value && !appConfig.isPkiAuthenticated
  })
  return {
    userInfo,
    isAuthenticated,
    isFormAuthenticatedUser
  }
}
