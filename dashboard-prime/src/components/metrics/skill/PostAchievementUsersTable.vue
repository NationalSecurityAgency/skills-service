<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import MetricsService from "@/components/metrics/MetricsService.js";
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import ModeSelector from "@/components/metrics/common/ModeSelector.vue";
import DateCell from "@/components/utils/table/DateCell.vue";
import { useUserInfo } from '@/components/utils/UseUserInfo.js'
import SkillsDataTable from "@/components/utils/table/SkillsDataTable.vue";

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
const totalRows = ref(1);
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
    sortBy: sortField.value ? sortField.value : 'userId',
    sortDesc: sortOrder.value !== 1,
  };
  console.log(queryParams);
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
    name: 'ClientDisplayPreviewSkill',
    params: {
      projectId: route.params.projectId,
      subjectId: route.params.subjectId,
      skillId: route.params.skillId,
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
            <span class="text-muted ml-2 d-none d-lg-inline-block">|</span>
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
                         striped-rows
                         show-gridlines
                         paginator
                         @page="pageChanged"
                         @sort="sortTable"
                         :rows="pageSize"
                         :rowsPerPageOptions="possiblePageSizes"
                         v-model:sort-field="sortField"
                         v-model:sort-order="sortOrder">
          <Column field="userId" header="User" sortable>
            <template #body="slotProps">
              <div class="flex">
                <div class="flex flex-1">
                  {{ userInfo.getUserDisplay(slotProps.data, true) }}
                </div>
<!--              <router-link :to="calculateClientDisplayRoute(slotProps.data.userId)">-->
                  <SkillsButton size="small" class="text-secondary"
                                :aria-label="`View details for user ${userInfo.getUserDisplay(slotProps.data)}`"
                                data-cy="usersTable_viewDetailsBtn"><i class="fa fa-user-alt" aria-hidden="true"/><span class="sr-only">view user details</span>
                  </SkillsButton>
<!--              </router-link>-->
              </div>
            </template>
          </Column>
          <Column field="count" header="Times Performed" sortable></Column>
          <Column field="date" header="Date Last Used" sortable>
            <template #body="slotProps">
              <date-cell :value="slotProps.data.date" />
            </template>
          </Column>
        </SkillsDataTable>
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>