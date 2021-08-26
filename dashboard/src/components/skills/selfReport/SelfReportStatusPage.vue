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
  <div id="cross-projects-panel">
    <sub-page-header title="Self Report"/>
    <skills-spinner :is-loading="loading" />
    <div v-if="!loading">
      <self-report-info-cards :self-report-stats="selfReportStats"/>
      <div v-if="showEmailServiceWarning" class="alert alert-warning mt-2" data-cy="selfReport_emailServiceWarning">
        <i class="fa fa-exclamation-triangle" aria-hidden="true"/> Please note that email notifications are currently disabled. Email configuration has not been performed on this instance of SkillTree. Please contact the root administrator.
      </div>
      <div v-if="hasSkillsWithApprovals()">
        <self-report-approval class="mt-3" @approval-action="handleApprovalAction"/>
        <self-report-approval-history ref="selfReportApprovalHistory" class="mt-3"/>
      </div>
      <no-content2 v-else title="No Skills Require Approval" data-cy="noApprovalTableMsg"
                   message="Currently there are no skills that require approval. Self Reporting type of 'Approval' can be configured when creating or editing a skill."
                   class="no-skills-msg"/>

    </div>
  </div>
</template>

<script>
  import SelfReportService from '@/components/skills/selfReport/SelfReportService';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import NoContent2 from '@/components/utils/NoContent2';
  import SubPageHeader from '../../utils/pages/SubPageHeader';
  import SelfReportInfoCards from './SelfReportInfoCards';
  import SelfReportApproval from './SelfReportApproval';
  import SelfReportApprovalHistory from './SelfReportApprovalHistory';

  export default {
    name: 'SelfReportStatusPage',
    components: {
      SelfReportApprovalHistory,
      NoContent2,
      SkillsSpinner,
      SelfReportApproval,
      SelfReportInfoCards,
      SubPageHeader,
    },
    data() {
      return {
        loading: true,
        projectId: this.$route.params.projectId,
        selfReportStats: [],
        showEmailServiceWarning: false,
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      handleApprovalAction() {
        this.$refs.selfReportApprovalHistory.loadApprovalsHistory();
      },
      loadData() {
        this.loading = true;
        SelfReportService.getSelfReportStats(this.projectId)
          .then((res) => {
            this.selfReportStats = res;
            if (this.hasSkillsWithApprovals()) {
              SelfReportService.isEmailServiceSupported()
                .then((isEmailSupported) => {
                  this.showEmailServiceWarning = !isEmailSupported;
                  this.loading = false;
                });
            } else {
              this.loading = false;
            }
          });
      },
      hasSkillsWithApprovals() {
        const approvalCount = this.selfReportStats.find((item) => item.value === 'Approval');
        return approvalCount !== undefined && approvalCount.count > 0;
      },
    },
  };
</script>

<style scoped>
.no-skills-msg {
  margin-top: 4rem;
}
</style>
