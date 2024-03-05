import { computed } from 'vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAuthState } from '@/stores/UseAuthState.js'

export const useUserInfo = () => {
  const appConfig = useAppConfig()
  const authState = useAuthState()

  const userInfo = computed(() => authState.userInfo)
  const isAuthenticated = computed(() => authState.isAuthenticated)
  const isFormAuthenticatedUser = computed(() => isAuthenticated.value && !appConfig.isPkiAuthenticated)
  return {
    userInfo,
    isAuthenticated,
    isFormAuthenticatedUser
  }
}
