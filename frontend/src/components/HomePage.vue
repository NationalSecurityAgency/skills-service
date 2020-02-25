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
