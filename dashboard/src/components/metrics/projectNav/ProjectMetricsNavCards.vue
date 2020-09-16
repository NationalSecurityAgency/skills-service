<template>
  <div class="row">
    <div v-for="(navItem) in navCards" :key="navItem.title" class="col-md-6 col-xl-4 mt-2">
      <metric-nav-card :title="navItem.title" :subtitle="navItem.subtitle" :description="navItem.description"
                       :path-name="navItem.pathName"
                       :icon="navItem.icon"
      />
    </div>
  </div>
</template>

<script>
  import MetricNavCard from './MetricNavCard';

  export default {
    name: 'ProjectMetricsNavCards',
    props: ['navCards'],
    components: { MetricNavCard },
  };
</script>

<style scoped>

</style>
