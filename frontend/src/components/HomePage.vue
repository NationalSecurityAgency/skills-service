<template>
  <div>
    <navigation :nav-items="navItems">
    </navigation>
  </div>
</template>

<script>
  import Navigation from './utils/Navigation';
  import AccessService from './access/AccessService';

  export default {
    name: 'HomePage',
    components: {
      Navigation,
    },
    data() {
      return {
        isRoot: false,
        navItems: [
          { name: 'Projects', iconClass: 'fa-project-diagram', page: 'HomePage' },
          { name: 'Metrics', iconClass: 'fa-cogs', page: 'GlobalMetrics' },
        ],
      };
    },
    mounted() {
      this.loadNavItems();
    },
    methods: {
      loadNavItems() {
        AccessService.hasRole('ROLE_SUPERVISOR')
          .then((response) => {
            this.isRoot = response;
            if (this.isRoot) {
              this.navItems.splice(1, 0, { name: 'Badges', iconClass: 'fa-globe-americas', page: 'GlobalBadges' });
            }
          });
      },
    },
  };
</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
