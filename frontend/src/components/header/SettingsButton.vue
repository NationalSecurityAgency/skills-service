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
<template>
  <b-dropdown right variant="outline-info">
    <template slot="button-content">
      <i class="fas fa-user-astronaut pr-1"/>
      <span class="d-inline-block text-truncate" style="max-width: 9rem; vertical-align: top">{{ displayName }}</span>
    </template>
    <b-dropdown-item href="#"  @click="gotoSettings">
      <span class="text-info"> <i class="fas fa-cog" style="width: 1.5rem;"/>Settings</span>
    </b-dropdown-item>
    <b-dropdown-item v-if="isFormAuthenticatedUser" href="#" @click="signOut">
      <span class="text-info"> <i class="fas fa-sign-out-alt" style="width: 1.5rem;"/>Log Out</span>
    </b-dropdown-item>
  </b-dropdown>
</template>

<script>
  export default {
    name: 'SettingsButton',
    data() {
      return {
        userInfoLoaded: false,
      };
    },
    computed: {
      userInfo() {
        return this.$store.getters.userInfo;
      },
      isAuthenticatedUser() {
        return this.$store.getters.isAuthenticated;
      },
      isFormAuthenticatedUser() {
        return this.isAuthenticatedUser && !this.$store.getters.isPkiAuthenticated;
      },
      displayName() {
        const { userInfo } = this.$store.getters;
        let displayName = userInfo.nickname;
        if (!displayName) {
          displayName = `${userInfo.first} ${userInfo.last}`;
        }
        return displayName;
      },
    },
    methods: {
      gotoSettings() {
        this.$router.push({ name: 'GeneralSettings' });
      },
      signOut() {
        this.$store.dispatch('logout');
      },
    },
  };
</script>

<style scoped>


</style>
