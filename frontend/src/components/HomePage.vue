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
    <navigation :nav-items="navItems">
    </navigation>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import Navigation from './utils/Navigation';

  const { mapGetters } = createNamespacedHelpers('access');

  export default {
    name: 'HomePage',
    components: {
      Navigation,
    },
    data() {
      return {
        navItems: [
          { name: 'Projects', iconClass: 'fa-project-diagram', page: 'HomePage' },
          { name: 'Metrics', iconClass: 'fa-cogs', page: 'GlobalMetrics' },
        ],
      };
    },
    computed: {
      ...mapGetters(['isSupervisor']),
    },
    watch: {
      isSupervisor(newValue) {
        if (newValue) {
          this.navItems.splice(1, 0, { name: 'Badges', iconClass: 'fa-globe-americas', page: 'GlobalBadges' });
        }
      },
    },
    methods: {
      loadNavItems() {
        this.isSupervisor = this.$store.getters.isSupervisor;
        if (this.isSupervisor) {
          this.navItems.splice(1, 0, { name: 'Badges', iconClass: 'fa-globe-americas', page: 'GlobalBadges' });
        }
      },
    },
  };
</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
