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
    <page-header :loading="isLoading" :options="headerOptions">
      <span slot="right-of-header">
        <i v-if="badge && badge.endDate" class="fas fa-gem ml-2" style="font-size: 1.6rem; color: purple;"></i>
      </span>
    </page-header>

    <navigation :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap text-teal', page: 'BadgeSkills'},
          {name: 'Users', iconClass: 'fa-users text-blue', page: 'BadgeUsers'},
          {name: 'Metrics', iconClass: 'fa-chart-bar text-purple', page: 'BadgeMetrics'},
        ]">
    </navigation>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import Navigation from '../utils/Navigation';
  import PageHeader from '../utils/pages/PageHeader';

  const { mapActions, mapGetters, mapMutations } = createNamespacedHelpers('badges');

  export default {
    name: 'BadgePage',
    components: {
      PageHeader,
      Navigation,
    },
    data() {
      return {
        isLoading: true,
        projectId: '',
        badgeId: '',
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      this.badgeId = this.$route.params.badgeId;
    },
    mounted() {
      this.loadBadge();
    },
    computed: {
      ...mapGetters([
        'badge',
      ]),
      headerOptions() {
        if (!this.badge) {
          return {};
        }
        return {
          icon: 'fas fa-award text-purple',
          title: `BADGE: ${this.badge.name}`,
          subTitle: `ID: ${this.badge.badgeId}`,
          stats: [{
            label: 'Skills',
            count: this.badge.numSkills,
            icon: 'fas fa-graduation-cap text-teal',
          }, {
            label: 'Points',
            count: this.badge.totalPoints,
            icon: 'far fa-arrow-alt-circle-up text-blue',
          }],
        };
      },
    },
    methods: {
      ...mapActions([
        'loadBadgeDetailsState',
      ]),
      ...mapMutations([
        'setBadge',
      ]),
      loadBadge() {
        this.isLoading = false;
        if (this.$route.params.badge) {
          this.setBadge(this.$route.params.badge);
          this.isLoading = false;
        } else {
          this.loadBadgeDetailsState({ projectId: this.projectId, badgeId: this.badgeId })
            .finally(() => {
              this.isLoading = false;
            });
        }
      },
    },
  };
</script>

<style scoped>

</style>
