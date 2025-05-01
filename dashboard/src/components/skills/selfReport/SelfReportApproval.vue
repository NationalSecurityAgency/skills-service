/*
Copyright 2024 SkillTree

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
<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SelfReportService from '@/components/skills/selfReport/SelfReportService';
import SkillsDataTable from "@/components/utils/table/SkillsDataTable.vue";
import DateCell from "@/components/utils/table/DateCell.vue";
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import RejectSkillModal from "@/components/skills/selfReport/ApproveOrRejectSkillModal.vue";
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'


const route = useRoute();
const appInfo = useAppInfoState()
const emit = defineEmits(['approval-action']);
const announcer = useSkillsAnnouncer();
const colors = useColors()
const responsive = useResponsiveBreakpoints()
const numberFormat = useNumberFormat()

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
const isEmailEnabled = computed(() => appInfo.emailEnabled)
const showApproveOrRejectModal = ref(false);
const requestType = ref('Reject');

onMounted(() => {
  loadApprovals();
  if (isEmailEnabled.value) {
    checkEmailSubscriptionStatus();
  }
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

const showApproveModal = () => {
  requestType.value = 'Approve';
  showApproveOrRejectModal.value = true;
}

const doApprove = (idsToApprove) => {
  loading.value = true;
  loadApprovals().then(() => {
    setTimeout(() => announcer.polite(`approved ${idsToApprove.length} skill approval request${idsToApprove.length > 1 ? 's' : ''}`), 0);
    emit('approval-action', 'approved');
    selectedItems.value = [];
  });
  closeModal();
};

const doReject = (idsToReject) => {
  loading.value = true;
  loadApprovals().then(() => {
    setTimeout(() => announcer.polite(`rejected ${idsToReject.length} skill approval request${idsToReject.length > 1 ? 's' : ''}`), 0);
    emit('approval-action', 'rejected');
    selectedItems.value = [];
  });
  closeModal();
};

const showRejectModal = () => {
  requestType.value = 'Reject';
  showApproveOrRejectModal.value = true;
}

const closeModal = () => {
  showApproveOrRejectModal.value = false;
}

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

const toggleRow = (row) => {
  if(expandedRows.value[row]) {
    delete expandedRows.value[row];
  }
  else {
    expandedRows.value[row] = true;
  }

  expandedRows.value = { ...expandedRows.value };
}
</script>

<template>
  <Card :pt="{ body: { class: 'p-0!' } }">
    <template #header>
      <SkillsCardHeader title="Self Reported Skills Requiring Approval">
        <template #headerContent>
          <div v-if="isEmailEnabled" data-cy="unsubscribeContainer" class="flex content-center items-center">
            {{ emailSubscribed ? 'Subscribed' : 'Unsubscribed' }} <ToggleSwitch v-model="emailSubscribed"
                                                                               @update:modelValue="toggleUnsubscribe"
                                                                               aria-label="Enable to receive Skill Approval request emails"
                                                                               class="ml-2"
                                                                               data-cy="unsubscribeSwitch" />
          </div>
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <div class="flex p-4 gap-2 flex-col sm:flex-row">
        <div class="flex flex-1 justify-center sm:justify-start">
          <SkillsButton size="small" @click="loadApprovals" aria-label="Sync Records" data-cy="syncApprovalsBtn" class="" icon="fas fa-sync-alt" />
        </div>
        <div class="flex flex-1 justify-center sm:justify-end">
          <SkillsButton size="small" @click="showRejectModal" data-cy="rejectBtn" class="" :disabled="selectedItems.length === 0" icon="fa fa-times-circle" label="Reject" />
          <SkillsButton size="small" @click="showApproveModal" data-cy="approveBtn" class="ml-2" :disabled="selectedItems.length === 0" icon="fa fa-check" label="Approve" />
        </div>
      </div>

      <SkillsDataTable :value="approvals"
                       v-model:selection="selectedItems"
                       v-model:expandedRows="expandedRows"
                       tableStoredStateId="skillsReportApprovalTable"
                       data-cy="skillsReportApprovalTable" paginator lazy
                       aria-label="Approval"
                       :rows="pageSize"
                       :rowsPerPageOptions="possiblePageSizes"
                       :totalRecords="totalRows"
                       :busy="loading"
                       v-model:sort-field="sortBy"
                       v-model:sort-order="sortOrder"
                       @page="pageChanged"
                       data-key="id"
                       pt:pcPaginator:paginatorContainer:aria-label="Approval Paginator"
                       @sort="sortTable">
        <Column selectionMode="multiple" :class="{'flex': responsive.md.value }">
          <template #header>
            <span class="mr-1 lg:mr-0 lg:hidden"><i class="fas fa-check-double"
                                                    aria-hidden="true"></i> Select Rows:</span>
          </template>
        </Column>
        <Column field="request" :class="{'flex': responsive.md.value }">
          <template #header>
            <span class="mr-1"><i class="fas fa-user-plus" :class="colors.getTextClass(1)"/> Requested</span>
          </template>
          <template #body="slotProps">
            <div>
                <span>
                  <router-link class="ml-1" target="_blank" rel="noopener"
                               :data-cy="`viewSkillLink_${slotProps.data.skillId}`"
                      :to="{ name:'SkillOverview', params: { projectId: slotProps.data.projectId,
                                                             subjectId: slotProps.data.subjectId,
                                                             skillId: slotProps.data.skillId }}">
                        {{ slotProps.data.skillName }}
                  </router-link>
                </span>
                <Badge class="ml-2">+ {{ slotProps.data.points }} Points</Badge>
            </div>
            <SkillsButton size="small" variant="outline-info"
                      class="mr-2 py-0 px-1 mt-1 ml-2 lg:ml-0"
                      @click="toggleRow(slotProps.data.id)"
                      :aria-label="`Show Justification for ${slotProps.data.name}`"
                      :data-cy="`expandDetailsBtn_${slotProps.data.skillId}`" :icon="expandedRows[slotProps.data.id] ? 'fa fa-minus-square' : 'fa fa-plus-square'" label="Justification">
            </SkillsButton>
          </template>
        </Column>
        <Column field="userId" :sortable="true" :class="{'flex': responsive.md.value }">
          <template #header>
            <span class="mr-1"><i class="fas fa-hand-pointer" :class="colors.getTextClass(2)"/> For User</span>
          </template>
          <template #body="slotProps">
            {{ slotProps.data.userIdForDisplay }}
          </template>
        </Column>
        <Column field="requestedOn" :sortable="true" :class="{'flex': responsive.md.value }">
          <template #header>
            <span class="mr-1"><i class="fas fa-clock" :class="colors.getTextClass(3)" /> Requested On</span>
          </template>
          <template #body="slotProps">
            <date-cell :value="slotProps.data.requestedOn" />
          </template>
        </Column>

        <template #expansion="slotProps">
          <div>
            <Card v-if="slotProps.data.requestMsg && slotProps.data.requestMsg.length > 0" header="Requested points with the following justification:" class="ml-6">
              <template #content>
                <markdown-text class="d-inline-block" :text="slotProps.data.requestMsg" data-cy="approvalMessage" :instance-id="`${slotProps.data.id}`"/>
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
          <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ numberFormat.pretty(totalRows) }}</span>
        </template>

        <template #empty>
          There are no records to show
        </template>
      </SkillsDataTable>
    </template>
  </Card>
  <RejectSkillModal v-model="showApproveOrRejectModal" @do-reject="doReject" @do-approve="doApprove" @done="closeModal" :selected-items="selectedItems" :request-type="requestType"/>
</template>

<style scoped>

</style>