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

import { onMounted, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserInfo } from '@/components/utils/UseUserInfo.js'
import { FilterMatchMode } from '@primevue/core/api'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import UserActionsService from '@/components/userActions/UserActionsService.js'
import InputText from 'primevue/inputtext'
import DateCell from '@/components/utils/table/DateCell.vue'
import Column from 'primevue/column'
import SingleUserAction from '@/components/userActions/SingleUserAction.vue'
import StartRecordingUserActionsDateWarning from '@/components/userActions/StartRecordingUserActionsDateWarning.vue'
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue'
import { useContentMaxWidthState } from '@/stores/UseContentMaxWidthState.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import TableNoRes from "@/components/utils/table/TableNoRes.vue";
import {useDebounceFn} from "@vueuse/core";

const route = useRoute()
const userInfo = useUserInfo()
const contentMaxWidthState = useContentMaxWidthState()
const responsive = useResponsiveBreakpoints()
const numberFormat = useNumberFormat()

const filterOptions = ref({
  loading: true,
  actions: [],
  items: [],
})
const tableOptions = ref({
  busy: false,
  bordered: true,
  outlined: true,
  stacked: 'md',
  sortBy: 'created',
  sortDesc: true,
  tableDescription: 'UserActions',
  fields: [],
  pagination: {
    server: true,
    currentPage: 1,
    totalRows: 1,
    pageSize: 10,
    possiblePageSizes: [10, 25, 50],
  }
})

const sortInfo = ref({ sortOrder: -1, sortBy: 'created' })
const isAllEvents = ref(true)
const totalRows = ref(0)
const items = ref([])
const expandedRows = ref([])

onMounted(() => {
  isAllEvents.value = !(route.params.projectId || route.params.quizId);
  const fields = [
    {
      key: 'userIdForDisplay',
      label: 'User',
      sortable: true,
      imageClass: 'fas fa-user-cog skills-color-skills',
    },
    {
      key: 'action',
      label: 'Action',
      sortable: true,
      imageClass: 'fas fa-stamp text-success',
    },
    {
      key: 'item',
      label: 'Item',
      sortable: true,
      imageClass: 'fas fa-clipboard-check skills-color-subjects',
    },
    {
      key: 'itemId',
      label: 'Item ID',
      sortable: true,
      imageClass: 'fas fa-fingerprint skills-color-points',
    }];
  if (isAllEvents.value) {
    fields.push({
      key: 'projectId',
      label: 'Project ID',
      sortable: true,
      imageClass: 'fas fa-tasks skills-color-projects',
    });
    fields.push({
      key: 'quizId',
      label: 'Quiz ID',
      sortable: true,
      imageClass: 'fas fa-spell-check skills-color-subjects',
    });
  }
  fields.push({
    key: 'created',
    label: 'Performed',
    sortable: true,
    imageClass: 'fas fa-clock text-warning',
  });
  tableOptions.value.fields = fields;

  loadData();
  loadFilterOptions();
})


const loadData = () => {
  tableOptions.value.busy = true
  const params = {
    limit: tableOptions.value.pagination.pageSize,
    page: tableOptions.value.pagination.currentPage,
    orderBy: sortInfo.value.sortBy,
    ascending: sortInfo.value.sortOrder === 1,
    projectIdFilter: encodeURIComponent(tableFilters.value.projectId.value || ''),
    itemFilter: encodeURIComponent(tableFilters.value.item.value || ''),
    userFilter: encodeURIComponent(tableFilters.value.userIdForDisplay.value || ''),
    quizFilter: encodeURIComponent(tableFilters.value.quizId.value || ''),
    itemIdFilter: encodeURIComponent(tableFilters.value.itemId.value || ''),
    actionFilter: encodeURIComponent(tableFilters.value.action.value || ''),
  };
  return UserActionsService.getDashboardActionsForEverything(route.params.projectId, route.params.quizId, params)
    .then((res) => {
      tableOptions.value.pagination.totalRows = res.totalCount;
      totalRows.value = res.totalCount;
      items.value = res.data;
    }).finally(() => {
    tableOptions.value.busy = false;
  });
}

const loadFilterOptions = () => {
  filterOptions.value.loading = true;
  UserActionsService.getDashboardActionsFilterOptions(route.params.projectId, route.params.quizId)
      .then((res) => {
        filterOptions.value.actions = res.actionFilterOptions.map((val) => ({ text: formatLabel(val), value: val }));
        filterOptions.value.items = res.itemFilterOptions.map((val) => ({ text: formatLabel(val), value: val }));
      }).finally(() => {
    filterOptions.value.loading = false;
  });
}

const formatLabel = (originalLabel) => {
  return originalLabel
      .replace(/([A-Z])/g, (match) => ` ${match}`)
      .replace(/^./, (match) => match.toUpperCase());
}

const userEnteredFilter = computed(() => {
  const f = tableFilters.value
  return f.global.value || f.userIdForDisplay.value || f.action.value ||
      f.item.value || f.itemId.value || f.projectId.value ||
      f.quizId.value
})
const clearFilter = () => {
  tableFilters.value.global.value = null
  tableFilters.value.userIdForDisplay.value = null
  tableFilters.value.action.value = null
  tableFilters.value.item.value = null
  tableFilters.value.itemId.value = null
  tableFilters.value.projectId.value = null
  tableFilters.value.quizId.value = null
  loadData()
}
const onFilter = useDebounceFn((filterEvent) => {
  tableOptions.value.pagination.currentPage = 1
  loadData()
}, 300)
const pageChanged = (pagingInfo) => {
  tableOptions.value.pagination.pageSize = pagingInfo.rows
  tableOptions.value.pagination.currentPage = pagingInfo.page + 1
  loadData()
}
const sortField = (column) => {
  // set to the first page
  tableOptions.value.pagination.currentPage = 1
  loadData()
}
const tableFilters = ref({
  global: { value: null, matchMode: FilterMatchMode.CONTAINS },
  userIdForDisplay: { value: null, matchMode: FilterMatchMode.STARTS_WITH },
  action: { value: null, matchMode: FilterMatchMode.STARTS_WITH },
  item: { value: null, matchMode: FilterMatchMode.IN },
  itemId: { value: null, matchMode: FilterMatchMode.EQUALS },
  projectId: { value: null, matchMode: FilterMatchMode.EQUALS },
  quizId: { value: null, matchMode: FilterMatchMode.EQUALS }
})

const pageAwareTitleLevel = computed(() => route.params.projectId ? 2 : 1)
</script>

<template>
  <div>
    <SubPageHeader title="Admin Activity History" :title-level="pageAwareTitleLevel">
      <template #underTitle v-if="!isAllEvents">
          <StartRecordingUserActionsDateWarning />
      </template>
    </SubPageHeader>

    <Card :pt="{ body: { class: 'p-0!' } }">
      <template #content>
        <div :style="contentMaxWidthState.main2ContentMaxWidthStyleObj">
            <SkillsDataTable
            :tableStoredStateId="`${route.name}-eventsTable`"
            aria-label="User Actions"
            :value="items"
            :loading="tableOptions.busy"
            show-gridlines
            striped-rows
            lazy
            paginator
            data-cy="dashboardActionsForEverything"
            v-model:expandedRows="expandedRows"
            v-model:filters="tableFilters"
            :globalFilterFields="['userIdForDisplay']"
            filterDisplay="row"
            @filter="onFilter"
            @page="pageChanged"
            @sort="sortField"
            :rows="tableOptions.pagination.pageSize"
            :rowsPerPageOptions="tableOptions.pagination.possiblePageSizes"
            :total-records="tableOptions.pagination.totalRows"
            v-model:sort-field="sortInfo.sortBy"
            v-model:sort-order="sortInfo.sortOrder">

          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ numberFormat.pretty(totalRows) }}</span>
          </template>

          <template #empty>
            <table-no-res :show-reset-filter="userEnteredFilter" @reset-filter="clearFilter"/>
          </template>

          <Column expander style="width: 20rem" :showFilterMenu="false" :class="{'flex': responsive.md.value }">
            <template #header>
              <span class="sr-only">Rows expand and collapse control - Not sortable</span>
            </template>
            <template #filter>
              <span class="sr-only">Rows expand and collapse control - No filtering</span>
            </template>>
          </Column>
          <Column field="userIdForDisplay" :showFilterMenu="false" header="User" :sortable="true" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fas fa-user-cog skills-color-skills mr-1" aria-hidden="true"></i>
            </template>
            <template #body="slotProps">
              <span :data-cy="`row${slotProps.index}-userId`">{{ userInfo.getUserDisplay(slotProps.data, true) }}</span>
            </template>
            <template #filter="{ filterModel, filterCallback }">
              <InputText v-model="filterModel.value"
                         type="text"
                         data-cy="userFilter"
                         class="p-column-filter"
                         style="min-width: 10rem"
                         @input="filterCallback()"
                         placeholder="Search by User" />
            </template>
          </Column>
          <Column field="action" header="Action" :showFilterMenu="false" :sortable="true" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fas fa-stamp text-success mr-1" aria-hidden="true"></i>
            </template>
            <template #body="slotProps">
              <span :data-cy="`row${slotProps.index}-${slotProps.field}`">{{ formatLabel(slotProps.data.action) }}</span>
            </template>
            <template #filter="{ filterModel, filterCallback }">
              <Select v-model="filterModel.value"
                        @change="filterCallback()"
                        :options="filterOptions.actions"
                        data-cy="actionFilter"
                        optionLabel="text"
                        optionValue="value"
                        placeholder="Select One"
                        class="p-column-filter"
                        style="min-width: 10rem"
                        :showClear="true">
              </Select>
            </template>
          </Column>
          <Column field="item" header="Item" :showFilterMenu="false" :sortable="true" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fas fa-clipboard-check skills-color-subjects mr-1" aria-hidden="true"></i>
            </template>
            <template #body="slotProps">
              <span :data-cy="`row${slotProps.index}-${slotProps.field}`">{{ formatLabel(slotProps.data.item) }}</span>
            </template>
            <template #filter="{ filterModel, filterCallback }">
              <Select v-model="filterModel.value"
                        @change="filterCallback()"
                        :options="filterOptions.items"
                        data-cy="itemFilter"
                        optionLabel="text"
                        optionValue="value"
                        placeholder="Select One"
                        class="p-column-filter"
                        style="min-width: 10rem"
                        :showClear="true">
              </Select>
            </template>
          </Column>
          <Column field="itemId" header="Item ID" :showFilterMenu="false" :sortable="true" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fas fa-fingerprint skills-color-points mr-1" aria-hidden="true"></i>
            </template>
            <template #body="slotProps">
              <span :data-cy="`row${slotProps.index}-${slotProps.field}`">{{ slotProps.data.itemId }}</span>
            </template>
            <template #filter="{ filterModel, filterCallback }">
              <InputText v-model="filterModel.value"
                         type="text"
                         class="p-column-filter"
                         data-cy="itemIdFilter"
                         style="min-width: 10rem"
                         @input="filterCallback()"
                         placeholder="Search by Item ID" />
            </template>
          </Column>
          <Column v-if="isAllEvents" field="projectId" header="Project ID" :showFilterMenu="false" :sortable="true" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fas fa-tasks skills-color-projects mr-1" aria-hidden="true"></i>
            </template>
            <template #body="slotProps">
              <span :data-cy="`row${slotProps.index}-${slotProps.field}`">{{ slotProps.data.projectId }}</span>
            </template>
            <template #filter="{ filterModel, filterCallback }">
              <InputText v-model="filterModel.value"
                         type="text"
                         class="p-column-filter"
                         data-cy="projectIdFilter"
                         style="min-width: 12rem"
                         @input="filterCallback()"
                         placeholder="Search by Project ID" />
            </template>
          </Column>
          <Column v-if="isAllEvents" field="quizId" header="Quiz ID" :showFilterMenu="false" :sortable="true" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fas fa-spell-check skills-color-subjects mr-1" aria-hidden="true"></i>
            </template>
            <template #body="slotProps">
              <span :data-cy="`row${slotProps.index}-${slotProps.field}`">{{ slotProps.data.quizId }}</span>
            </template>
            <template #filter="{ filterModel, filterCallback }">
              <InputText v-model="filterModel.value"
                         type="text"
                         class="p-column-filter"
                         data-cy="quizIdFilter"
                         style="min-width: 10rem"
                         @input="filterCallback()"
                         placeholder="Search by Quiz ID" />
            </template>
          </Column>
          <Column field="created" header="Performed" :sortable="false" :showFilterMenu="false" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fas fa-clock text-warning mr-1" aria-hidden="true"></i>
            </template>
            <template #filter>
              <span class="sr-only">Rows expand and collapse control - No filtering</span>
            </template>>
            <template #body="slotProps">
              <DateCell :value="slotProps.data.created" />
            </template>
          </Column>

          <template #expansion="slotProps">
            <SingleUserAction :data-cy="`row${slotProps.index}-expandedDetails`"
                              :action-id="slotProps.data.id" :item="slotProps.data.item" :action="slotProps.data.action" />
          </template>
        </SkillsDataTable>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped></style>
