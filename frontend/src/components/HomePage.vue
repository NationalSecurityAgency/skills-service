<template>
  <div>
    <navigation :nav-items="[
          {name: 'Projects', iconClass: 'fa-project-diagram', page: 'HomePage'},
          {name: 'Badges', iconClass: 'fa-award', page: 'GlobalBadges'},
          {name: 'Metrics', iconClass: 'fa-cogs', page: 'GlobalMetrics'}
        ]">
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
  };
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
