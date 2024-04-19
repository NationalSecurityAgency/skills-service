<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SelfReportService from '@/components/skills/selfReport/SelfReportService';
import SkillsDataTable from "@/components/utils/table/SkillsDataTable.vue";
import DateCell from "@/components/utils/table/DateCell.vue";
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'

const route = useRoute();
const props = defineProps({
  emailEnabled: {
    type: Boolean,
    required: false
  }
});
const emit = defineEmits(['approval-action']);
const announcer = useSkillsAnnouncer();

const approvals = ref([]);
const loading = ref(true);
const pageSize = ref(5);
const possiblePageSizes = ref([5, 10, 25]);
const currentPage = ref(1);
const sortOrder = ref(-1);
const sortBy = ref('requestedOn');
const selectedItems = ref([]);
const expandedRows = ref({});
const totalRows = ref(null);
const emailSubscribed = ref(true);
const isEmailEnabled = ref(props.emailEnabled);
const reject = ref({
  showModal: false,
  rejectMsg: '',
});

onMounted(() => {
  loadApprovals();
  if (isEmailEnabled.value) {
    checkEmailSubscriptionStatus();
  }
});

const unsubscribeHelpMsg = computed(() => {
  if (emailSubscribed.value) {
    return 'Change to Unsubscribed to unsubscribe from all Skill Approval request emails';
  }
  return 'Change to Subscribed to receive Skill Approval request emails';
});
const pageChanged = (pagingInfo) => {
  currentPage.value = pagingInfo.page + 1;
  pageSize.value = pagingInfo.rows;
  loadApprovals();
};

const sortTable = (sortContext) => {
  sortBy.value = sortContext.sortField;
  sortOrder.value = sortContext.sortOrder;

  // set to the first page
  currentPage.value = 1;
  loadApprovals();
};

const loadApprovals = () => {
  loading.value = true;
  const pageParams = {
    limit: pageSize.value,
    ascending: sortOrder.value === 1,
    page: currentPage.value,
    orderBy: sortBy.value,
  };
  return SelfReportService.getApprovals(route.params.projectId, pageParams)
      .then((res) => {
        approvals.value = res.data.map((item) => ({ selected: false, ...item }));
        totalRows.value = res.count;
        loading.value = false;
      });
};

const approve = () => {
  loading.value = true;
  const idsToApprove = selectedItems.value.map((item) => item.id);
  SelfReportService.approve(route.params.projectId, idsToApprove)
      .then(() => {
        loadApprovals().then(() => {
          setTimeout(() => announcer.polite(`approved ${idsToApprove.length} skill approval request${idsToApprove.length > 1 ? 's' : ''}`), 0);
        });
        emit('approval-action', 'approved');
        selectedItems.value = [];
      });
};

const doReject = () => {
  loading.value = true;
  const ids = selectedItems.value.map((item) => item.id);
  SelfReportService.reject(route.params.projectId, ids, reject.value.rejectMsg)
      .then(() => {
        loadApprovals().then(() => {
          setTimeout(() => announcer.polite(`rejected ${ids.length} skill approval request${ids.length > 1 ? 's' : ''}`), 0);
        });
        emit('approval-action', 'rejected');
        reject.value.rejectMsg = '';
      });
};

const checkEmailSubscriptionStatus = () => {
  SelfReportService.isUserSubscribedToEmails(route.params.projectId).then((respData) => {
    emailSubscribed.value = respData;
  });
};

const toggleUnsubscribe = () => {
  if (emailSubscribed.value) {
    SelfReportService.subscribeUserToEmails(route.params.projectId).then(() => {
      nextTick(() => {
        announcer.polite('You have subscribed to self-report approval request emails for this project');
      });
    });
  } else {
    SelfReportService.unsubscribeUserFromEmails(route.params.projectId).then(() => {
      nextTick(() => {
        announcer.polite('You have unsubscribed from self-report approval request emails for this project');
      });
    });
  }
};

// :icon="data.detailsShowing ? 'fa fa-minus-square' : 'fa fa-plus-square'"
</script>

<template>
  <Card>
    <template #header>
      <SkillsCardHeader title="Self Reported Skills Requiring Approval"></SkillsCardHeader>
<!--      <div class="col text-right" v-if="isEmailEnabled" data-cy="unsubscribeContainer">-->
<!--        <b-form-checkbox v-model="emailSubscribed"-->
<!--                         name="unsubscribe-toggle"-->
<!--                         class="ml-2 mt-2"-->
<!--                         style="display: inline-block"-->
<!--                         v-on:input="toggleUnsubscribe"-->
<!--                         :aria-label="unsubscribeHelpMsg"-->
<!--                         data-cy="unsubscribeSwitch"-->
<!--                         switch>-->
<!--          {{ emailSubscribed ? 'Subscribed' : 'Unsubscribed' }}-->
<!--        </b-form-checkbox>-->
<!--      </div>      -->
    </template>
    <template #content>
      <div class="flex px-3 mb-3 mt-2">
        <div class="flex flex-1">
          <SkillsButton size="small" @click="loadApprovals" aria-label="Sync Records" data-cy="syncApprovalsBtn" class="mr-2 mt-1" icon="fas fa-sync-alt" />
        </div>
        <div class="flex flex-1 justify-content-end">
          <SkillsButton size="small" @click="reject.showModal=true" data-cy="rejectBtn" class="mt-1 ml-2" :disabled="selectedItems.length === 0" icon="fa fa-times-circle" label="Reject" />
          <SkillsButton size="small" @click="approve" data-cy="approveBtn" class="mt-1 ml-2" :disabled="selectedItems.length === 0" icon="fa fa-check" label="Approve" />
        </div>
      </div>

      <SkillsDataTable :value="approvals"
                       v-model:selection="selectedItems"
                       v-model:expandedRows="expandedRows"
                       tableStoredStateId="skillsReportApprovalTable"
                       data-cy="skillsReportApprovalTable" paginator lazy
                       :rows="pageSize"
                       :rowsPerPageOptions="possiblePageSizes"
                       :totalRecords="totalRows"
                       :busy="loading"
                       :sort-field="sortBy"
                       :sort-order="sortOrder"
                       @page="pageChanged"
                       data-key="id"
                       @sort="sortTable">
        <Column selectionMode="multiple" headerStyle="width: 3rem"></Column>
        <Column expander style="width: 1rem">
          <template #header>
            <span class="text-primary">Justification</span>
          </template>
        </Column>
        <Column field="request">
          <template #header>
            <span class="text-primary"><i class="fas fa-user-plus skills-color-crossProjects" /> Requested</span>
          </template>
          <template #body="slotProps">
            <div>
                <span>
                  <router-link class="ml-1" target="_blank" rel="noopener"
                      :to="{ name:'SkillOverview', params: { projectId: slotProps.data.projectId,
                                                             subjectId: slotProps.data.subjectId,
                                                             skillId: slotProps.data.skillId }}">
                        {{ slotProps.data.skillName }}
                  </router-link>
                </span>
                <Badge class="ml-2">+ {{ slotProps.data.points }} Points</Badge>
            </div>
          </template>
        </Column>
        <Column field="userId" sortable>
          <template #header>
            <span class="text-primary"><i class="fas fa-hand-pointer skills-color-skills" /> For User</span>
          </template>
          <template #body="slotProps">
            {{ slotProps.data.userIdForDisplay }}
          </template>
        </Column>
        <Column field="requestedOn" sortable>
          <template #header>
            <span class="text-primary"><i class="fas fa-clock skills-color-access" /> Requested On</span>
          </template>
          <template #body="slotProps">
            <date-cell :value="slotProps.data.requestedOn" />
          </template>
        </Column>

        <template #expansion="slotProps">
          <div>
            <Card v-if="slotProps.data.requestMsg && slotProps.data.requestMsg.length > 0" header="Requested points with the following justification:" class="ml-4">
              <template #content>
                <markdown-text class="d-inline-block" :text="slotProps.data.requestMsg" data-cy="approvalMessage"/>
              </template>
            </Card>
            <Card v-else>
              <template #content>
                No Justification supplied
              </template>
            </Card>
          </div>
        </template>

        <template #paginatorstart>
          <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows }}</span>
        </template>
      </SkillsDataTable>
    </template>
  </Card>
</template>

<style scoped>

</style>