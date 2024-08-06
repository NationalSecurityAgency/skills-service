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
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import MetricsService from "@/components/metrics/MetricsService.js";
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import ModeSelector from "@/components/metrics/common/ModeSelector.vue";
import DateCell from "@/components/utils/table/DateCell.vue";
import { useUserInfo } from '@/components/utils/UseUserInfo.js'
import SkillsDataTable from "@/components/utils/table/SkillsDataTable.vue";
import SkillsDisplayPathAppendValues from "@/router/SkillsDisplayPathAppendValues.js";

defineProps(['skillName']);
const route = useRoute();
const userInfo = useUserInfo()

onMounted(() => {
  loadData();
})

const postAchievementUsers = ref([]);
const chartToLoad = ref('usagePostAchievementUsersBuilder');
const modeSelectorOptions = ref([
  {
    label: 'Still Using',
    value: 'usagePostAchievementUsersBuilder',
  },
  {
    label: 'Stopped',
    value: 'noUsagePostAchievementUsersBuilder',
  },
]);

const loading = ref(true);
const hasData = ref(false);
const currentPage = ref(1);
const totalRows = ref(0);
const pageSize = ref(5);
const sortField = ref('userId');
const sortOrder = ref(-1);
const possiblePageSizes = [5, 10, 15, 20, 50];

onMounted(() => {
  loadData();
});

const updateMode = (mode) => {
  chartToLoad.value = mode.value;
  loadData();
};

const loadData = () => {
  loading.value = true;
  const queryParams = {
    skillId: route.params.skillId,
    page: currentPage.value,
    pageSize: pageSize.value,
    sortBy: sortField.value,
    sortDesc: sortOrder.value !== 1,
  };
  MetricsService.loadChart(route.params.projectId, chartToLoad.value, queryParams).then((dataFromServer) => {
        if (dataFromServer) {
          hasData.value = true;
          totalRows.value = dataFromServer.totalCount;
          postAchievementUsers.value = dataFromServer.users;
        }
        loading.value = false;
      });
};

const pageChanged = (pagingInfo) => {
  currentPage.value = pagingInfo.page + 1;
  pageSize.value = pagingInfo.rows;
  loadData();
};

const sortTable = (sortContext) => {
  sortField.value = sortContext.sortField;
  sortOrder.value = sortContext.sortOrder;

  // set to the first page
  currentPage.value = 1;
  loadData();
};

const calculateClientDisplayRoute = (props) => {
  return {
    name: `SkillsDisplay${SkillsDisplayPathAppendValues.SkillsDisplayPreview}`,
    params: {
      projectId: route.params.projectId,
      userId: props.userId,
      dn: props.dn,
    },
  };
};
</script>

<template>
  <Card data-cy="postAchievementUserList" :no-padding="true">
    <template #header>
      <SkillsCardHeader title="Users that Achieved this Skill">
        <template #headerContent>
          <div class="d-block d-lg-inline-block">
            <mode-selector :options="modeSelectorOptions" @mode-selected="updateMode"/>
          </div>
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasData && postAchievementUsers?.length > 0" no-data-msg="No achievements yet for this skill.">
        <SkillsDataTable :value="postAchievementUsers"
                         data-cy="postAchievementUsers-table"
                         tableStoredStateId="postAchievementUsers-table"
                         aria-label="Achievement Users"
                         striped-rows
                         show-gridlines
                         paginator
                         lazy
                         @page="pageChanged"
                         @sort="sortTable"
                         :rows="pageSize"
                         :totalRecords="totalRows"
                         :rowsPerPageOptions="possiblePageSizes"
                         v-model:sort-field="sortField"
                         v-model:sort-order="sortOrder">
          <Column field="userId" header="User" sortable>
            <template #body="slotProps">
              <router-link :to="calculateClientDisplayRoute(slotProps.data)" tabindex="-1">
                {{ userInfo.getUserDisplay(slotProps.data, true) }}
              </router-link>
            </template>
          </Column>
          <Column field="count" header="Times Performed" sortable></Column>
          <Column field="date" header="Date Last Used" sortable>
            <template #body="slotProps">
              <date-cell :value="slotProps.data.date" />
            </template>
          </Column>

          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows }}</span>
          </template>
        </SkillsDataTable>
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>