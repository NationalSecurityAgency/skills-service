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
  import Navigation from './utils/Navigation';

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
      isSupervisor() {
        return this.$store.getters['access/isSupervisor'];
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
        const globalBadges = this.navItems.find(element => element.name === 'Badges');
        if (this.isSupervisor) {
          if (!globalBadges) {
            this.navItems.splice(1, 0, { name: 'Badges', iconClass: 'fa-globe-americas', page: 'GlobalBadges' });
          }
        } else if (globalBadges) {
          const idx = this.navItems.indexOf(globalBadges);
          if (idx >= 0) {
            this.navItems.splice(idx, 1);
          }
        }
      },
    },
  };
</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
