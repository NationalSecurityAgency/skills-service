<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import SelfReportService from '@/components/skills/selfReport/SelfReportService'
import NoContent2 from '@/components/utils/NoContent2.vue'
import SelfReportInfoCards from '@/components/skills/selfReport/SelfReportInfoCards.vue'
import SelfReportApproval from '@/components/skills/selfReport/SelfReportApproval.vue'
import SelfReportApprovalHistory from '@/components/skills/selfReport/SelfReportApprovalHistory.vue'
import EmailNotEnabledWarning from '@/components/utils/EmailNotEnabledWarning.vue'

const route = useRoute();
const loading = ref(true);
const selfReportStats = ref([]);
const showEmailServiceWarning = ref(true);

const selfReportApprovalHistory = ref();

onMounted(() => {
  loadData();
});

const handleApprovalAction = () => {
  selfReportApprovalHistory.value.loadApprovalsHistory();
};

const loadData = () => {
  loading.value = true;
  SelfReportService.getSelfReportStats(route.params.projectId)
      .then((res) => {
        selfReportStats.value = res;
        if (!hasSkillsWithApprovals()) {
          showEmailServiceWarning.value = false;
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
      <self-report-info-cards :self-report-stats="selfReportStats" class="mb-3"/>
      <email-not-enabled-warning v-if="showEmailServiceWarning"/>

      <div v-if="hasSkillsWithApprovals()">
        <self-report-approval @approval-action="handleApprovalAction" :email-enabled="!showEmailServiceWarning"/>
        <self-report-approval-history ref="selfReportApprovalHistory" class="mt-3"/>
      </div>
      <no-content2 v-else title="No Skills Require Approval" data-cy="noApprovalTableMsg"
                   message="Currently there are no skills that require approval. Self Reporting type of 'Approval' can be configured when creating or editing a skill."
                   class="no-skills-msg pt-5"/>

    </div>
  </div>
</template>

<style scoped></style>
