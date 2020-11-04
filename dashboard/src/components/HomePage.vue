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
    <h2 class="bg-white m-0" style="height: 1rem;"></h2>
    <navigation :nav-items="navItems" data-cy="navigationmenu">
    </navigation>
  </div>
</template>

<script>
  import Navigation from './utils/Navigation';

  export default {
    name: 'HomePage',
    components: {
      Navigation,
    },
    data() {
      return {
        isLoading: true,
        navItems: [
          { name: 'Projects', iconClass: 'fa-tasks skills-color-projects', page: 'HomePage' },
          { name: 'Metrics', iconClass: 'fa-cogs skills-color-metrics', page: 'MultipleProjectsMetricsPage' },
        ],
      };
    },
    computed: {
      isSupervisor() {
        return this.$store.getters['access/isSupervisor'];
      },
      headerOptions() {
        return {
          icon: 'fas fa-cubes',
          title: 'Home',
          subTitle: '',
          stats: [],
        };
      },
    },
    mounted() {
      this.loadNavItems();
    },
    watch: {
      isSupervisor() {
        this.loadNavItems();
      },
    },
    methods: {
      loadNavItems() {
        const globalBadges = this.navItems.find((element) => element.name === 'Badges');
        if (this.isSupervisor) {
          if (!globalBadges) {
            this.navItems.splice(1, 0, { name: 'Badges', iconClass: 'fa-globe-americas skills-color-badges', page: 'GlobalBadges' });
          }
        } else if (globalBadges) {
          const idx = this.navItems.indexOf(globalBadges);
          if (idx >= 0) {
            this.navItems.splice(idx, 1);
          }
        }
        this.isLoading = false;
      },
    },
  };
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
