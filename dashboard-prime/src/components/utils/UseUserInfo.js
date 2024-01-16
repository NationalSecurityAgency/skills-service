import { computed } from 'vue'
import { useStore } from 'vuex'

export const useUserInfo = () => {
  const store = useStore()

  const userInfo = computed(() => {
    return store.getters.userInfo
  })
  const isAuthenticated = computed(() => {
    return store.getters.isAuthenticated
  })
  const isFormAuthenticatedUser = computed(() => {
    return isAuthenticated.value && !store.getters.isPkiAuthenticated
  })
  return {
    userInfo,
    isAuthenticated,
    isFormAuthenticatedUser
  }
}
