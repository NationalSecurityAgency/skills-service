<template>
<div>
  <sub-page-header title="Metrics | Subjects"/>
  <user-counts-by-subject-metric />
  <subject-levels-over-time />
</div>
</template>

<script>
  import SubPageHeader from '@//components/utils/pages/SubPageHeader';
  import UserCountsBySubjectMetric from './UserCountsBySubjectMetric';
  import SubjectLevelsOverTime from './SubjectLevelsOverTime';

  export default {
    name: 'SubjectMetricsPage',
    components: { SubjectLevelsOverTime, SubPageHeader, UserCountsBySubjectMetric },
  };
</script>

<style scoped>

</style>
