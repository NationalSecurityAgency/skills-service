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
  <div>
    <page-header :loading="isLoading" :options="headerOptions"/>

    <navigation v-if="userIdForDisplay" :nav-items="[
          {name: 'Client Display', iconClass: 'fa-user', page: 'ClientDisplayPreview'},
          {name: 'Performed Skills', iconClass: 'fa-award', page: 'UserSkillEvents'},
          {name: 'Metrics', iconClass: 'fa-chart-bar', page: 'UserMetrics'},
        ]">
    </navigation>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import Navigation from '../utils/Navigation';
  import PageHeader from '../utils/pages/PageHeader';
  import UsersService from './UsersService';

  const { mapActions, mapGetters } = createNamespacedHelpers('users');

  export default {
    name: 'UserPage',
    components: {
      PageHeader,
      Navigation,
    },
    data() {
      return {
        userTitle: '',
        userIdForDisplay: '',
        isLoading: true,
      };
    },
    created() {
      this.userTitle = this.$route.params.userId;
      this.userIdForDisplay = this.$route.params.userId;
      if (this.$store.getters.isPkiAuthenticated) {
        UsersService.getUserInfo(this.$route.params.projectId, this.$route.params.userId)
          .then((result) => {
            this.userIdForDisplay = result.userIdForDisplay;
            this.userTitle = result.first && result.last ? `${result.first} ${result.last}` : result.userIdForDisplay;
            this.loadUserDetails();
          });
      } else {
        this.loadUserDetails();
      }
      this.loadUserDetails();
    },
    computed: {
      ...mapGetters([
        'numSkills',
        'userTotalPoints',
      ]),
      headerOptions() {
        return {
          icon: 'fas fa-user',
          title: `USER: ${this.userTitle}`,
          subTitle: `ID: ${this.userIdForDisplay}`,
          stats: [{
            label: 'Skills',
            count: this.numSkills,
          }, {
            label: 'Points',
            count: this.userTotalPoints,
          }],
        };
      },
    },
    methods: {
      ...mapActions([
        'loadUserDetailsState',
      ]),
      loadUserDetails() {
        this.isLoading = true;
        this.loadUserDetailsState({ projectId: this.$route.params.projectId, userId: this.$route.params.userId })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };
</script>

<style scoped>
  .version-select {
    width: 7rem;
  }
</style>
