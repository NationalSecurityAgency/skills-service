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
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { FilterMatchMode } from '@primevue/core/api'
import { useUserInfo } from '@/components/utils/UseUserInfo.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import ExpirationService from '@/components/expiration/ExpirationService.js'
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue'
import InputText from 'primevue/inputtext'
import Column from 'primevue/column'
import DateCell from '@/components/utils/table/DateCell.vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import TableNoRes from "@/components/utils/table/TableNoRes.vue";
import {useStorage} from "@vueuse/core";

const route = useRoute()
const userInfo = useUserInfo()
const responsive = useResponsiveBreakpoints()
const numberFormat = useNumberFormat()

const projectId = route.params.projectId;
const sortInfo = ref({ sortOrder: -1, sortBy: 'userIdForDisplay' })
const totalRows = ref(0)
const items = ref([])
const filters = ref({
  skillName: '',
  userIdForDisplay: '',
})
const filtering = ref(false)
const tableOptions = ref({
  busy: false,
  bordered: true,
  outlined: true,
  stacked: 'md',
  sortBy: 'userIdForDisplay',
  sortDesc: true,
  tableDescription: 'ExpirationHistory',
  fields: [
    {
      key: 'skillName',
      label: 'Skill Name',
      sortable: true,
    },
    {
      key: 'userIdForDisplay',
      label: 'User',
      sortable: true,
    },
    {
      key: 'expiredOn',
      label: 'Expired On',
      sortable: true,
    },
  ],
  pagination: {
    server: true,
    currentPage: 1,
    totalRows: 1,
    possiblePageSizes: [10, 25, 50],
  },
  items: [],
})
const pageSize = useStorage('expirationHistory-tablePageSize', 10)

onMounted(() => {
  loadData();
})

const loadData = () => {
  tableOptions.value.busy = true
  const params = {
    limit: pageSize.value,
    page: tableOptions.value.pagination.currentPage,
    orderBy: sortInfo.value.sortBy,
    ascending: sortInfo.value.sortOrder === 1,
    skillName: encodeURIComponent(tableFilters.value.skillName.value || ''),
    userIdForDisplay: encodeURIComponent(tableFilters.value.userIdForDisplay.value || ''),
  };
  return ExpirationService.getExpiredSkills(projectId, params).then((res) => {
    tableOptions.value.items = res.data;
    tableOptions.value.pagination.totalRows = res.totalCount;
    totalRows.value = res.totalCount;
  }).finally(() => {
    tableOptions.value.busy = false;
  });
}
const clearFilter = () => {
  tableFilters.value.global.value = null
  loadData().then(() => filtering.value = false)
}
const onFilter = (filterEvent) => {
  loadData().then(() => filtering.value = true)
}
const pageChanged = (pagingInfo) => {
  pageSize.value = pagingInfo.rows
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
  skillName: { value: null, matchMode: FilterMatchMode.STARTS_WITH },
})
const getUrl = (item) => {
  return `/administrator/projects/${encodeURIComponent(projectId)}/subjects/${encodeURIComponent(item.subjectId)}/skills/${encodeURIComponent(item.skillId)}/`;
}
const calculateClientDisplayRoute = (props) => {
  return {
    name: 'SkillsDisplaySkillsDisplayPreviewProject',
    params: {
      projectId: projectId,
      userId: props.userId,
    },
  };
}
</script>

<template>
  <SubPageHeader title="Skill Expiration History" />

  <Card :pt="{ body: { class: 'p-0!' } }">
    <template #content>
      <SkillsDataTable
          tableStoredStateId="expirationHistoryTable"
          :value="tableOptions.items"
          :loading="tableOptions.busy"
          show-gridlines
          striped-rows
          lazy
          paginator
          data-cy="expirationHistoryTable"
          aria-label="Skill Expiration History"
          v-model:filters="tableFilters"
          :globalFilterFields="['userIdForDisplay']"
          filterDisplay="row"
          @filter="onFilter"
          @page="pageChanged"
          @sort="sortField"
          :rows="pageSize"
          :rowsPerPageOptions="tableOptions.pagination.possiblePageSizes"
          :total-records="tableOptions.pagination.totalRows"
          v-model:sort-field="sortInfo.sortBy"
          v-model:sort-order="sortInfo.sortOrder">

        <template #paginatorstart>
          <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ numberFormat.pretty(totalRows) }}</span>
        </template>

        <template #empty>
          <table-no-res :showResetFilter="filtering" @resetFilter="clearFilter"/>
        </template>
        <Column field="skillName" header="Skill Name" :showFilterMenu="false" :sortable="true" :class="{'flex': responsive.md.value }">
          <template #header>
            <i class="fas fa-graduation-cap skills-color-skills mr-1" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            <a :data-cy="`row${slotProps.index}-${slotProps.field}`" :href="getUrl(slotProps.data)">{{ slotProps.data.skillName }}</a>
          </template>
          <template #filter="{ filterModel, filterCallback }">
            <InputText v-model="filterModel.value"
                       type="text"
                       class="p-column-filter"
                       data-cy="skillNameFilter"
                       style="min-width: 10rem"
                       @input="filterCallback()"
                       placeholder="Search by Skill Name" />
          </template>
        </Column>
        <Column field="userIdForDisplay" :showFilterMenu="false" header="User" :sortable="true" :class="{'flex': responsive.md.value }">
          <template #header>
            <i class="fas fa-user-cog skills-color-skills mr-1" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            <div class="flex gap-1">
              <span class="flex-1" :data-cy="`row${slotProps.index}-userId`">{{ userInfo.getUserDisplay(slotProps.data, true) }}</span>
              <router-link
                  class="ml-1 flex"
                  data-cy="ViewUserDetailsBtn"
                  :to="calculateClientDisplayRoute(slotProps.data)"
                  tabindex="-1"
                  target="_blank" rel="noopener">
                <SkillsButton
                    target="_blank"
                    outlined
                    severity="info"
                    size="small"
                    icon="fa fa-user-alt"
                    :aria-label="`View details for user ${userInfo.getUserDisplay(slotProps.data, true)}`">
                </SkillsButton>
              </router-link>
            </div>
          </template>
          <template #filter="{ filterModel, filterCallback }">
            <InputText v-model="filterModel.value"
                       type="text"
                       data-cy="userIdFilter"
                       class="p-column-filter"
                       style="min-width: 10rem"
                       @input="filterCallback()"
                       placeholder="Search by User" />
          </template>
        </Column>
        <Column field="expiredOn" header="Expired On" :sortable="false" :show-filter-menu="false" :class="{'flex': responsive.md.value }">
          <template #header>
            <i class="fas fa-clock text-warning mr-1" aria-hidden="true"></i>
          </template>
          <template #filter>
            <span class="sr-only">No filter for this column</span>
          </template>
          <template #body="slotProps">
            <DateCell :value="slotProps.data.expiredOn" />
          </template>
        </Column>
      </SkillsDataTable>
    </template>
  </Card>
</template>

<style scoped></style>
