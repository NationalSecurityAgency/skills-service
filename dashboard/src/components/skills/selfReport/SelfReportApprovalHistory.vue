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
import { ref, onMounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SelfReportService from '@/components/skills/selfReport/SelfReportService';
import SkillsDataTable from "@/components/utils/table/SkillsDataTable.vue";
import DateCell from "@/components/utils/table/DateCell.vue";
import ShowMore from "@/components/skills/selfReport/ShowMore.vue";
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'

const route = useRoute();
const announcer = useSkillsAnnouncer();
const colors = useColors()
const responsive = useResponsiveBreakpoints()
const numberFormat = useNumberFormat()

const loading = ref(true);
const pageSize = ref(5);
const possiblePageSizes = ref([5, 10, 25]);
const currentPage = ref(1);
const sortOrder = ref(-1);
const sortBy = ref('approverActionTakenOn');
const expandedRows = ref({});
const totalRows = ref(null);

const filters = ref({
  skill: '',
  userId: '',
  approverUserId: '',
});
const items = ref([]);

onMounted(() => {
  loadApprovalsHistory();
})

const reset = () => {
  filters.value.skill = '';
  filters.value.userId = '';
  filters.value.approverUserId = '';
  loadApprovalsHistory();
};

const loadApprovalsHistory = () => {
  loading.value = true;

  const pageParams = {
    limit: pageSize.value,
    ascending: sortOrder.value === 1,
    page: currentPage.value,
    orderBy: sortBy.value,
    skillNameFilter: filters.value.skill,
    userIdFilter: filters.value.userId,
    approverUserIdFilter: filters.value.approverUserId,
  };
  SelfReportService.getApprovalsHistory(route.params.projectId, pageParams)
      .then((res) => {
        let approvals = res.data.map((item) => ({ selected: false, ...item }));
        totalRows.value = res.count;
        loading.value = false;

        const skillNameFilterExist = pageParams.skillNameFilter && pageParams.skillNameFilter.trim().length > 0;
        const userIdFilterExist = pageParams.userIdFilter && pageParams.userIdFilter.trim().length > 0;
        const approverUserIdFilterFilterExist = pageParams.approverUserIdFilter && pageParams.approverUserIdFilter.trim().length > 0;

        const hasFilter = skillNameFilterExist || userIdFilterExist || approverUserIdFilterFilterExist;
        if (hasFilter) {
          approvals = approvals.map((item) => {
            const skillNameHtml = skillNameFilterExist ? highlight(item.skillName, pageParams.skillNameFilter) : null;
            const userIdHtml = userIdFilterExist ? highlight(item.userIdForDisplay, pageParams.userIdFilter) : null;
            const approverUserIdHtml = approverUserIdFilterFilterExist ? highlight(item.approverUserIdForDisplay, pageParams.approverUserIdFilter) : null;
            return Object.assign(item, { skillNameHtml, userIdHtml, approverUserIdHtml });
          });
          nextTick(() => announcer.polite(`skill approval history has been filtered by selected criteria, there are ${res.count} total results`));
        }

        items.value = approvals;
      });
};

const highlight = (value, searchStr) => {
  const searchStringNorm = searchStr.trim().toLowerCase();
  const index = value.toLowerCase().indexOf(searchStringNorm);
  const htmlRel = `${value.substring(0, index)}<mark>${value.substring(index, index + searchStringNorm.length)}</mark>${value.substring(index + searchStringNorm.length)}`;
  return htmlRel;
};

const pageChanged = (pagingInfo) => {
  currentPage.value = pagingInfo.page + 1;
  pageSize.value = pagingInfo.rows;
  loadApprovalsHistory();
};

const sortTable = (sortContext) => {
  sortBy.value = sortContext.sortField;
  sortOrder.value = sortContext.sortOrder;

  // set to the first page
  currentPage.value = 1;
  loadApprovalsHistory();
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

defineExpose( {
  loadApprovalsHistory
})
</script>

<template>
  <Card :pt="{ body: { class: '!p-0' } }">
    <template #header>
      <SkillsCardHeader title="Approval History"></SkillsCardHeader>
    </template>
    <template #content>
      <div class="flex flex-col md:flex-row gap-2 pt-4 px-4">
        <div class="w-full">
          <label for="skill-filter" class="ml-1">Skill Name</label>
          <InputText type="text" class="w-full mt-2" placeholder="Skill Name" v-model="filters.skill" id="skill-filter"
                     v-on:keydown.enter="loadApprovalsHistory" data-cy="selfReportApprovalHistory-skillNameFilter" />
          </div>
        <div class="w-full">
          <label for="userId-filter" class="ml-1">User Id</label>
          <InputText type="text" class="w-full mt-2" placeholder="User Id" v-model="filters.userId" id="userId-filter"
                     v-on:keydown.enter="loadApprovalsHistory" data-cy="selfReportApprovalHistory-userIdFilter" />
          </div>
        <div class="w-full">
          <label for="approverUserId-filter" class="ml-1">Approver </label>
          <InputText type="text" class="w-full mt-2" placeholder="Approver" v-model="filters.approverUserId" id="approverUserId-filter"
                     v-on:keydown.enter="loadApprovalsHistory" data-cy="selfReportApprovalHistory-approverUserIdFilter" />
        </div>
      </div>
      <div class="flex gap-2 mt-6 mb-6 px-4">
        <SkillsButton size="small" @click="loadApprovalsHistory" data-cy="selfReportApprovalHistory-filterBtn" icon="fa fa-filter" label="Filter" />
        <SkillsButton size="small" @click="reset" class="ml-1" data-cy="selfReportApprovalHistory-resetBtn" label="Reset" icon="fa fa-times" />
      </div>
      <SkillsDataTable :value="items"
                       v-model:expandedRows="expandedRows"
                       tableStoredStateId="selfReportApprovalHistoryTable"
                       aria-label="Approval History"
                       pt:pcPaginator:paginatorContainer:aria-label="Approval History Paginator"
                       data-cy="selfReportApprovalHistoryTable" paginator lazy
                       :rows="pageSize"
                       :rowsPerPageOptions="possiblePageSizes"
                       :totalRecords="totalRows"
                       :busy="loading"
                       v-model:sort-field="sortBy"
                       v-model:sort-order="sortOrder"
                       @page="pageChanged"
                       data-key="id"
                       @sort="sortTable">
        <Column field="skillName" sortable :class="{'flex': responsive.md.value }">
          <template #header>
            <span class=""><i class="fas fa-graduation-cap mr-1" :class="colors.getTextClass(1)"/> Requested</span>
          </template>
          <template #body="slotProps">
            <div>
              <router-link
                  :data-cy="`viewSkillLink_${slotProps.data.skillId}`"
                  :to="{ name:'SkillOverview', params: { projectId: slotProps.data.projectId, subjectId: slotProps.data.subjectId, skillId: slotProps.data.skillId }}"
                  :aria-label="`View skill ${slotProps.data.skillName}  via link`"
                  target="_blank"><span v-if="slotProps.data.skillNameHtml" v-html="slotProps.data.skillNameHtml"></span><span v-else>{{ slotProps.data.skillName }}</span>
              </router-link>
              <Badge class="ml-2">+ {{ slotProps.data.points }} Points</Badge>
            </div>
            <div class="text-primary">by</div>
            <div class="italic"><span v-if="slotProps.data.userIdHtml" v-html="slotProps.data.userIdHtml"></span><span v-else>{{ slotProps.data.userIdForDisplay }}</span></div>
            <SkillsButton size="small" variant="outline-info"
                      class="mr-2 py-0 px-1"
                      @click="toggleRow(slotProps.data.id)"
                      :aria-label="`Show Justification for ${slotProps.data.name}`"
                      :data-cy="`expandDetailsBtn_${slotProps.data.skillId}`" :icon="expandedRows[slotProps.data.id] ? 'fa fa-minus-square' : 'fa fa-plus-square'" label="Justification">
            </SkillsButton>
          </template>
        </Column>
        <Column field="rejectedOn" sortable :class="{'flex': responsive.md.value }">
          <template #header>
            <span class=""><i class="fas fa-question-circle mr-1" :class="colors.getTextClass(2)"></i> Response</span>
          </template>
          <template #body="slotProps">
            <div v-if="slotProps.data.rejectedOn"><Badge variant="danger"><i class="fas fa-thumbs-down"></i> <span class="text-uppercase">Rejected</span></Badge></div>
            <div v-else><Badge variant="success"><i class="fas fa-thumbs-up"></i> <span class="text-uppercase">Approved</span></Badge></div>
            <div class="text-primary">by</div>
            <div class="italic"><span v-if="slotProps.data.approverUserIdHtml" v-html="slotProps.data.approverUserIdHtml"></span><span v-else>{{ slotProps.data.approverUserIdForDisplay }}</span></div>
            <div v-if="slotProps.data.message"><span class="text-primary text-break">Explanation:</span> <show-more :text="slotProps.data.message"/></div>
          </template>
        </Column>
        <Column field="requestedOn" sortable :class="{'flex': responsive.md.value }">
          <template #header>
            <span class=""><i class="fas fa-user-clock mr-1" :class="colors.getTextClass(3)"></i> Requested On</span>
          </template>
          <template #body="slotProps">
            <date-cell :value="slotProps.data.requestedOn" />
          </template>
        </Column>
        <Column field="approverActionTakenOn" sortable :class="{'flex': responsive.md.value }">
          <template #header>
            <span class=""><i class="fas fa-stopwatch mr-1" :class="colors.getTextClass(4)"></i> Response On</span>
          </template>
          <template #body="slotProps">
            <date-cell :value="slotProps.data.approverActionTakenOn" />
          </template>
        </Column>

        <template #expansion="slotProps">
          <div>
            <Card v-if="slotProps.data.requestMsg && slotProps.data.requestMsg.length > 0" class="ml-6">
              <template #header>
                <SkillsCardHeader title="Requested points with the following justification:"></SkillsCardHeader>
              </template>
              <template #content>
                <markdown-text class="d-inline-block" :text="slotProps.data.requestMsg" data-cy="approvalMessage" :instance-id="`${slotProps.data.id}`"/>
              </template>
            </Card>
            <Card v-else class="ml-6">
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
</template>

<style scoped>

</style>