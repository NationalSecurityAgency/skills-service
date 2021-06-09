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
  <b-dropdown right variant="link" aria-label="User Settings Dropdown">
    <template slot="button-content">
      <i class="fas fa-user-circle" style="font-size: 1.55rem" aria-hidden="true"/>
      <span class="sr-only">settings menu</span>
    </template>
    <b-dropdown-text style="width: 14rem;">
      <div class="text-secondary"><i class="fas fa-key skills-color-loggedIn" aria-hidden="true"/> Logged in as</div>
      <div class="text-left text-primary font-weight-bold" data-cy="settingsButton-loggedInName">
        {{ displayName }}
      </div>
    </b-dropdown-text>
    <b-dropdown-divider />
    <b-dropdown-item href="#" :disabled="myProgressLinkDisabled"  @click="gotoMyProgress" data-cy="settingsButton-navToMyProgress">
      <span class="text-gray-700"> <i class="fas fa-chart-bar skills-color-progressAndRanking" aria-hidden="true"/><span class="link-name">Progress and Rankings</span></span>
    </b-dropdown-item>
    <b-dropdown-item href="#" :disabled="adminLinkDisabled"  @click="gotoAdmin" data-cy="settingsButton-navToProjectAdmin">
      <span class="text-gray-700"> <i class="fas fa-tasks skills-color-projectAdmin" aria-hidden="true"/><span class="link-name">Project Admin</span></span>
    </b-dropdown-item>
    <b-dropdown-item href="#" :disabled="settingsLinkDisabled" @click="gotoSettings" data-cy="settingsButton-navToSettings">
      <span class="text-gray-700"> <i class="fas fa-cog skills-color-settings" aria-hidden="true"/><span class="link-name">Settings</span></span>
    </b-dropdown-item>
    <template v-if="isFormAuthenticatedUser">
      <b-dropdown-divider />
      <b-dropdown-item href="#" @click="signOut">
        <span class="text-gray-700"> <i class="fas fa-sign-out-alt skills-color-loggedOut" aria-hidden="true"/><span class="link-name">Log Out</span></span>
      </b-dropdown-item>
    </template>
  </b-dropdown>
</template>

<script>
  import NavigationErrorMixin from '../utils/NavigationErrorMixin';

  export default {
    name: 'SettingsButton',
    mixins: [NavigationErrorMixin],
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
      avatarTxt() {
        const { userInfo } = this.$store.getters;
        return `${userInfo.first[0]}${userInfo.last[0]}`.toUpperCase();
      },
      displayName() {
        const { userInfo } = this.$store.getters;
        let displayName = userInfo.nickname;
        if (!displayName) {
          displayName = `${userInfo.first} ${userInfo.last}`;
        }
        return displayName;
      },
      myProgressLinkDisabled() {
        return this.$route.path && this.$route.name === 'MyProgressPage';
      },
      settingsLinkDisabled() {
        return this.$route.path && this.$route.name === 'GeneralSettings';
      },
      adminLinkDisabled() {
        return this.$route.path && this.$route.name === 'AdminHomePage';
      },
    },
    methods: {
      gotoSettings() {
        this.handlePush({ name: 'GeneralSettings' });
      },
      gotoAdmin() {
        this.handlePush({ name: 'AdminHomePage' });
      },
      gotoMyProgress() {
        this.handlePush({ name: 'MyProgressPage' });
      },
      signOut() {
        this.$store.dispatch('logout');
      },
    },
  };
</script>

<style scoped>
  .userName {
    max-width: 5rem;
    vertical-align: top
  }

  @media (min-width: 576px) {
    .userName {
      max-width: 9rem;
    }
  }

  @media (min-width: 1200px) {
    .userName {
      max-width: 12rem;
    }
  }

  .text-gray-700 > i {
    width: 1.6rem;
  }

  .sr-only {
    border: 0;
    clip: rect(0, 0, 0, 0);
    height: 1px;
    margin: -1px;
    overflow: hidden;
    padding: 0;
    position: absolute;
    width: 1px;
  }

</style>
