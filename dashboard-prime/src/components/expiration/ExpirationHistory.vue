<script setup>
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { FilterMatchMode } from 'primevue/api';
import { useUserInfo } from '@/components/utils/UseUserInfo.js';
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import ExpirationService from '@/components/expiration/ExpirationService.js';
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue';
import InputText from 'primevue/inputtext';
import Column from 'primevue/column';
import DateCell from '@/components/utils/table/DateCell.vue';

const route = useRoute()
const userInfo = useUserInfo()
const announcer = useSkillsAnnouncer()
const responsive = useResponsiveBreakpoints()

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
    pageSize: 10,
    possiblePageSizes: [10, 25, 50],
  },
  items: [],
})

onMounted(() => {
  loadData();
})

const loadData = () => {
  tableOptions.value.busy = true
  const params = {
    limit: tableOptions.value.pagination.pageSize,
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
  skillName: { value: null, matchMode: FilterMatchMode.STARTS_WITH },
})
const getUrl = (item) => {
  return `/administrator/projects/${encodeURIComponent(projectId)}/subjects/${encodeURIComponent(item.subjectId)}/skills/${encodeURIComponent(item.skillId)}/`;
}
const calculateClientDisplayRoute = (props) => {
  return {
    name: 'ClientDisplayPreview',
    params: {
      projectId: projectId,
      userId: props.userId,
    },
  };
}
</script>

<template>
  <SubPageHeader title="Skill Expiration History" />

  <Card :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }">
    <template #content>
      <SkillsDataTable
          tableStoredStateId="expirationHistoryTable"
          :value="tableOptions.items" tableStyle="min-width: 50rem"
          :loading="tableOptions.busy"
          show-gridlines
          striped-rows
          lazy
          paginator
          data-cy="expirationHistoryTable"
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
          <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows }}</span>
        </template>

        <template #empty>
          <div class="flex justify-content-center flex-wrap h-12rem">
            <i class="flex align-items-center justify-content-center mr-1 fas fa-exclamation-circle fa-3x"
               aria-hidden="true"></i>
            <span class="w-full">
                <span class="flex align-items-center justify-content-center">There are no records to show</span>
                <span v-if="filtering" class="flex align-items-center justify-content-center">  Click
                    <SkillsButton class="flex flex align-items-center justify-content-center px-1"
                                  label="Reset"
                                  link
                                  size="small"
                                  @click="clearFilter"
                                  :aria-label="`Reset filter for $ {quizType} results`"
                                  data-cy="userResetBtn" /> to clear the existing filter.
              </span>
            </span>
          </div>
        </template>
        <Column field="skillName" header="Skill Name" :showFilterMenu="false" :sortable="true">
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
        <Column field="userIdForDisplay" :showFilterMenu="false" header="User" :sortable="true">
          <template #header>
            <i class="fas fa-user-cog skills-color-skills mr-1" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            <span :data-cy="`row${slotProps.index}-userId`">{{ userInfo.getUserDisplay(slotProps.data, true) }}</span>

            <!--            <router-link-->
            <!--                class="ml-1"-->
            <!--                data-cy="ViewUserDetailsBtn"-->
            <!--                :to="calculateClientDisplayRoute(slotProps.data)"-->
            <!--                target="_blank" rel="noopener">-->
            <!--              <SkillsButton-->
            <!--                  target="_blank"-->
            <!--                  outlined-->
            <!--                  severity="info"-->
            <!--                  size="small"-->
            <!--                  icon="fa fa-user-alt"-->
            <!--                  :aria-label="`View details for user ${userInfo.getUserDisplay(slotProps.data, true)}`">-->
            <!--              </SkillsButton>-->
            <!--            </router-link>-->
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
        <Column field="expiredOn" header="Expired On" :sortable="false">
          <template #header>
            <i class="fas fa-clock text-warning mr-1" aria-hidden="true"></i>
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
