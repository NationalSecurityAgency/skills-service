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
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAuthState } from '@/stores/UseAuthState.js'
import { computed } from 'vue'

export const useCommunityLabels = () => {
  const projConfig = useProjConfig()
  const appConfig = useAppConfig()
  const authState = useAuthState()
  const beforeCommunityLabel = computed(() => {
    return appConfig.userCommunityBeforeLabel
  })
  const afterCommunityLabel =  computed(() => appConfig.userCommunityAfterLabel)
  const currentUserCommunity =  computed(() => authState.currentUserCommunity)
  const showManageUserCommunity = computed(() => authState.showUserCommunityInfo)
  const userCommunityRestrictedDescriptor =  computed(() => appConfig.userCommunityRestrictedDescriptor)
  const userCommunityDocsLabel = computed(() => appConfig.userCommunityDocsLabel);
  const userCommunityDocsLink = computed(() =>  appConfig.userCommunityDocsLink)
  const projectConfiguredUserCommunity = computed(() =>projConfig.getProjectCommunityValue())

  const isRestrictedUserCommunity = (communityName) =>{
    return communityName ? communityName === userCommunityRestrictedDescriptor.value : false;
  }
  return {
    beforeCommunityLabel,
    afterCommunityLabel,
    currentUserCommunity,
    showManageUserCommunity,
    userCommunityRestrictedDescriptor,
    userCommunityDocsLabel,
    userCommunityDocsLink,
    projectConfiguredUserCommunity,
    isRestrictedUserCommunity
  }
}