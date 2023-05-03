/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script>
  export default {
    name: 'CommunityLabelsMixin',
    computed: {
      beforeCommunityLabel() {
        return (this.$store.getters.config && this.$store.getters.config.userCommunityBeforeLabel) || 'Supports';
      },
      afterCommunityLabel() {
        return (this.$store.getters.config && this.$store.getters.config.userCommunityAfterLabel) || 'Community';
      },
      currentUserCommunity() {
        return this.$store.getters.userInfo?.userCommunity;
      },
      showManageUserCommunity() {
        return Boolean(this.currentUserCommunity);
      },
      userCommunityRestrictedDescriptor() {
        return this.$store.getters.config.userCommunityRestrictedDescriptor;
      },
    },
    methods: {
      isRestrictedUserCommunity(communityName) {
        return communityName ? communityName === this.userCommunityRestrictedDescriptor : false;
      },
    },
  };
</script>

<style scoped>

</style>
