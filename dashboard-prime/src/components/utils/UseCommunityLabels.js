import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAuthState } from '@/stores/UseAuthState.js'

export const useCommunityLabels = () => {
  const projConfig = useProjConfig()
  const appConfig = useAppConfig()
  const authState = useAuthState()
  const beforeCommunityLabel = appConfig.userCommunityBeforeLabel.value
  const afterCommunityLabel = appConfig.userCommunityAfterLabel.value
  const currentUserCommunity = authState.userInfo?.userCommunity
  const showManageUserCommunity = Boolean(currentUserCommunity);
  const userCommunityRestrictedDescriptor = appConfig.userCommunityRestrictedDescriptor.value
  const userCommunityDocsLabel = appConfig.userCommunityDocsLabel.value;
  const userCommunityDocsLink = appConfig.userCommunityDocsLink.value || null;
  const projectConfiguredUserCommunity = projConfig.getProjectCommunityValue()

  const isRestrictedUserCommunity = (communityName) =>{
    return communityName ? communityName === userCommunityRestrictedDescriptor : false;
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