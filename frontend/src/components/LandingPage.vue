<template>
  <div>
    <navigation :nav-items="[
          {name: 'Projects', iconClass: 'fa-project-diagram', page: 'HomePage'},
          {name: 'Badges', iconClass: 'fa-award', page: 'CrossProjectBadges'},
          // {name: 'Settings', iconClass: 'fa-cogs', page: 'GeneralSettings'}
        ]">
    </navigation>

  </div>
</template>

<script>
  import Navigation from './utils/Navigation';

  export default {
    name: 'LandingPage',
    components: {
      Navigation,
    },
  };
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
