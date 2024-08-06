/*
 * Copyright 2024 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
