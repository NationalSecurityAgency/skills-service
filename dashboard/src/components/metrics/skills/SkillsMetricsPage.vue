<template>
  <div>
    <sub-page-header title="Metrics | Skills"/>
    <skills-usage-metrics />
  </div>
</template>

<script>
  import SubPageHeader from '@//components/utils/pages/SubPageHeader';
  import SkillsUsageMetrics from './SkillsUsageMetrics';

  export default {
    name: 'SkillsMetricsPage',
    components: { SkillsUsageMetrics, SubPageHeader },
  };
</script>

<style scoped>

</style>
