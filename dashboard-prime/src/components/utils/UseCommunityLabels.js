import { useStore } from 'vuex'

export const useCommunityLabels = () => {
  const store = useStore()
  const beforeCommunityLabel = (store.getters.config && store.getters.config.userCommunityBeforeLabel) || '';
  const afterCommunityLabel = (store.getters.config && store.getters.config.userCommunityAfterLabel) || '';
  const currentUserCommunity = store.getters.userInfo?.userCommunity
  const showManageUserCommunity = Boolean(currentUserCommunity);
  const userCommunityRestrictedDescriptor = store.getters.config.userCommunityRestrictedDescriptor
  const userCommunityDocsLabel = (store.getters.config && store.getters.config.userCommunityDocsLabel) || 'Learn More';
  const userCommunityDocsLink = (store.getters.config && store.getters.config.userCommunityDocsLink) || null;
  const projectConfiguredUserCommunity = store.getters.projConfig?.project_community_value

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