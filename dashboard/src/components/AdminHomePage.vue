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
    <div class="bg-white m-0" style="height: 1rem;width:100%;"></div>
    <navigation :nav-items="navItems" data-cy="navigationmenu" role="navigation">
    </navigation>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import Navigation from './utils/Navigation';

  const { mapGetters } = createNamespacedHelpers('access');

  export default {
    name: 'AdminHomePage',
    components: {
      Navigation,
    },
    data() {
      return {
        isLoading: true,
      };
    },
    computed: {
      ...mapGetters([
        'isSupervisor',
        'isRoot',
      ]),
      headerOptions() {
        return {
          icon: 'fas fa-cubes',
          title: 'Home',
          subTitle: '',
          stats: [],
        };
      },
      navItems() {
        const items = [];
        items.push({
          name: 'Projects',
          iconClass: 'fa-tasks skills-color-projects',
          page: 'AdminHomePage',
        });

        if (this.isSupervisor || this.isRoot) {
          items.push({
            name: 'Global Badges',
            iconClass: 'fa-globe-americas skills-color-badges',
            page: 'GlobalBadges',
          });
          items.push({
            name: 'Metrics',
            iconClass: 'fa-chart-bar skills-color-metrics',
            page: 'MultipleProjectsMetricsPage',
          });
        }

        if (this.isRoot) {
          items.push({
            name: 'Contact Admins',
            iconClass: 'fas fa-mail-bulk',
            page: 'ContactAdmins',
          });
        }

        return items;
      },
    },
    mounted() {
    },
    methods: {
    },
  };
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
