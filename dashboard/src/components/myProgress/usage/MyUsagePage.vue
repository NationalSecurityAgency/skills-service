<template>
    <div class="container-fluid">
      <sub-page-header title="My Usage" class="pt-4">
      </sub-page-header>

      <b-row class="my-4">
        <b-col class="my-summary-card">
          <event-history-chart :availableProjects="projects"></event-history-chart>
        </b-col>
      </b-row>
    </div>
</template>

<script>
  import SubPageHeader from '../../utils/pages/SubPageHeader';
  import EventHistoryChart from './EventHistoryChart';

  export default {
    name: 'MyUsagePage',
    components: { EventHistoryChart, SubPageHeader },
    props: ['projects'],
  };
</script>

<style scoped>

</style>
