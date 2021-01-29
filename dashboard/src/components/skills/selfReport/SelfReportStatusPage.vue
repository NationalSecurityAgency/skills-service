<template>
  <div id="cross-projects-panel">
    <sub-page-header title="Self Report"/>
    <self-report-info-cards />
    <self-report-approval class="mt-3"/>
  </div>
</template>

<script>
  import SubPageHeader from '../../utils/pages/SubPageHeader';
  import SelfReportInfoCards from './SelfReportInfoCards';
  import SelfReportApproval from './SelfReportApproval';

  export default {
    name: 'SelfReportStatusPage',
    components: { SelfReportApproval, SelfReportInfoCards, SubPageHeader },
  };
</script>

<style scoped>

</style>
