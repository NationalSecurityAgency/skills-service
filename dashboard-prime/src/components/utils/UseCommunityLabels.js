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