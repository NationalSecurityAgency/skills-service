<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import SelfReportService from '@/components/skills/selfReport/SelfReportService';
import NoContent2 from "@/components/utils/NoContent2.vue";
import SelfReportInfoCards from "@/components/skills/selfReport/SelfReportInfoCards.vue";
import SelfReportApproval from "@/components/skills/selfReport/SelfReportApproval.vue";
import SelfReportApprovalHistory from "@/components/skills/selfReport/SelfReportApprovalHistory.vue";

const route = useRoute();

const loading = ref(true);
const selfReportStats = ref([]);
const showEmailServiceWarning = ref(false);
const isEmailEnabled = false;

onMounted(() => {
  loadData();
});

const handleApprovalAction = () => {
  // $refs.selfReportApprovalHistory.loadApprovalsHistory();
};

const loadData = () => {
  loading.value = true;
  SelfReportService.getSelfReportStats(route.params.projectId)
      .then((res) => {
        selfReportStats.value = res;
        if (hasSkillsWithApprovals()) {
          showEmailServiceWarning.value = !isEmailEnabled;
        }
      }).finally(() => {
    loading.value = false;
  });
};

const hasSkillsWithApprovals = () => {
  const approvalCount = selfReportStats.value.find((item) => item.value === 'Approval');
  return approvalCount !== undefined && approvalCount.count > 0;
};
</script>

<template>
  <div id="cross-projects-panel" ref="mainFocus">
    <skills-spinner :is-loading="loading" />

    <div v-if="!loading">
      <self-report-info-cards :self-report-stats="selfReportStats"/>
      <Message v-if="showEmailServiceWarning" severity="warn" icon="fa fa-exclamation-triangle" data-cy="selfReport_emailServiceWarning" :closable="false">
        Please note that email notifications are currently disabled. Email configuration has not been performed on this instance of SkillTree. Please contact the root administrator.
      </Message>
      <div v-if="hasSkillsWithApprovals()">
        <self-report-approval class="mt-3" @approval-action="handleApprovalAction" :email-enabled="!showEmailServiceWarning"/>
        <self-report-approval-history ref="selfReportApprovalHistory" class="mt-3"/>
      </div>
      <no-content2 v-else title="No Skills Require Approval" data-cy="noApprovalTableMsg"
                   message="Currently there are no skills that require approval. Self Reporting type of 'Approval' can be configured when creating or editing a skill."
                   class="no-skills-msg"/>

    </div>
  </div>
</template>

<style scoped></style>
