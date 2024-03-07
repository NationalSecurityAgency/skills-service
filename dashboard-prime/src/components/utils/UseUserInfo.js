import { computed } from 'vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAuthState } from '@/stores/UseAuthState.js'

export const useUserInfo = () => {
  const appConfig = useAppConfig()
  const authState = useAuthState()

  const userInfo = computed(() => authState.userInfo)
  const isAuthenticated = computed(() => authState.isAuthenticated)
  const isFormAuthenticatedUser = computed(() => isAuthenticated.value && !appConfig.isPkiAuthenticated)
  const getUserDisplay = (props, fullName = false) => {
    const userDisplay = props.userIdForDisplay ? props.userIdForDisplay : props.userId;
    let userName = '';
    if (fullName && props.firstName && props.lastName) {
      userName = ` (${props.lastName}, ${props.firstName})`;
    }
    if (appConfig.oAuthProviders) {
      const indexOfDash = userDisplay.lastIndexOf('-');
      if (indexOfDash > 0) {
        const provider = userDisplay.substr(indexOfDash + 1);
        if (appConfig.oAuthProviders.includes(provider)) {
          return `${userDisplay.substr(0, indexOfDash)}${userName}`;
        }
      }
    }
    return `${userDisplay}${userName}`;
  }

  return {
    userInfo,
    isAuthenticated,
    isFormAuthenticatedUser,
    getUserDisplay
  }
}
