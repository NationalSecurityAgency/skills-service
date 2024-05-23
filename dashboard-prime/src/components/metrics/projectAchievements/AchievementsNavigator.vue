<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import dayjs from 'dayjs';
import MetricsService from "@/components/metrics/MetricsService.js";
import DateCell from "@/components/utils/table/DateCell.vue";
import AchievementType from "@/components/metrics/projectAchievements/AchievementType.vue";
import SkillsDataTable from "@/components/utils/table/SkillsDataTable.vue";
import InputText from "primevue/inputtext";
import SkillsCalendarInput from "@/components/utils/inputForm/SkillsCalendarInput.vue";
import SkillsDropDown from "@/components/utils/inputForm/SkillsDropDown.vue";

const route = useRoute();

const isLoading = ref(true);
const usernameFilter = ref('');
const fromDayFilter = ref('');
const toDayFilter = ref('');
const nameFilter = ref('');
const levels = {
  selected: '',
  available: [
    { value: '', text: 'Optionally select level' },
    { value: 1, text: 'Level 1' },
    { value: 2, text: 'Level 2' },
    { value: 3, text: 'Level 3' },
    { value: 4, text: 'Level 4' },
    { value: 5, text: 'Level 5' },
  ],
};
const achievementTypes = ref({
  selected: ['Overall', 'Subject', 'Skill', 'Badge'],
  available: ['Overall', 'Subject', 'Skill', 'Badge'],
});

const sortBy = ref('achievedOn');
const sortOrder = ref(-1);
const currentPage = ref(1);
const totalRows = ref(0);
const pageSize = ref(5);
const possiblePageSizes = [5, 10, 15, 20, 50];
const loadingTable = ref(false);

const items = ref([]);

onMounted(() => {
  reloadTable();
});

const pageChanged = (pagingInfo) => {
  currentPage.value = pagingInfo.page + 1;
  pageSize.value = pagingInfo.rows;
  reloadTable();
};

const sortTable = (sortContext) => {
  sortBy.value = sortContext.sortField;
  sortOrder.value = sortContext.sortOrder;

  // set to the first page
  currentPage.value = 1;
  reloadTable();
};

const reset = () => {
  usernameFilter.value = '';
  currentPage.value = 1;
  fromDayFilter.value = '';
  toDayFilter.value = '';
  nameFilter.value = '';
  levels.selected = '';
  achievementTypes.value.selected = achievementTypes.value.available;
  reloadTable();
};

const reloadTable = () => {
  loadingTable.value = true;
  const params = {
    pageSize: pageSize.value,
    currentPage: currentPage.value,
    usernameFilter: usernameFilter.value,
    fromDayFilter: fromDayFilter.value ? dayjs(fromDayFilter.value).format('YYYY-MM-DD') : '',
    toDayFilter: toDayFilter.value ? dayjs(toDayFilter.value).format('YYYY-MM-DD') : '',
    nameFilter: nameFilter.value,
    minLevel: levels.selected,
    achievementTypes: achievementTypes.value.selected,
    sortBy: sortBy.value ? sortBy.value : 'achievedOn',
    sortDesc: sortOrder.value !== 1,
  };

  MetricsService.loadChart(route.params.projectId, 'userAchievementsChartBuilder', params)
      .then((dataFromServer) => {
        isLoading.value = false;
        items.value = dataFromServer.items;
        totalRows.value = dataFromServer.totalNumItems;
        loadingTable.value = false;
      });
};
</script>

<template>
  <Card data-cy="achievementsNavigator">
    <template #header>
      <SkillsCardHeader title="Achievements"></SkillsCardHeader>
    </template>
    <template #content>
      <div class="flex pb-2">
        <div class="flex flex-1 flex-column border-right-1 pr-3 gap-2">
          <label for="user-name-filter">User Name Filter:</label>
          <InputText class="w-full"
                     v-model="usernameFilter"
                     id="user-name-filter"
                     data-cy="achievementsNavigator-usernameInput"
                     @keydown.enter="reloadTable"
                     aria-label="Skill name filter" />

          <div class="mt-4">
            <label>Types: </label>
            <div class="flex gap-2 mt-2" data-cy="achievementsNavigator-typeInput">
              <span v-for="tag in achievementTypes.available" :key="tag">
                <Checkbox v-model="achievementTypes.selected" :value="tag" :name="tag" :inputId="tag"></Checkbox>
                <label :for="tag" class="ml-2">
                  {{ tag }}
                </label>
              </span>
            </div>
          </div>
        </div>
        <div class="flex flex-1 flex-column gap-2 border-right-1 pl-2 pr-2">
          <SkillsCalendarInput v-model="fromDayFilter" id="from-date-filter" data-cy="achievementsNavigator-fromDateInput"
                               label="From Date:" name="fromDayFilter" input-class="w-full" :max-date="toDayFilter" />

          <SkillsDropDown
              label="Minimum Level (Subject & Skill Only)"
              name="levels-input-group"
              id="levels-input-group"
              data-cy="achievementsNavigator-levelsInput"
              placeholder="Optionally select level"
              optionLabel="text"
              optionValue="value"
              v-model="levels.selected"
              :options="levels.available" />
        </div>
        <div class="flex flex-1 flex-column gap-2 pl-2">
          <SkillsCalendarInput v-model="toDayFilter" id="to-date-filter" data-cy="achievementsNavigator-toDateInput"
                               label="To Date:" name="toDayFilter" input-class="w-full" :min-date="fromDayFilter" />

          <label for="name-filter">Name (Subject, Skill and Badge Only):</label>
          <InputText class="w-full"
                     v-model="nameFilter"
                     id="name-filter"
                     data-cy="achievementsNavigator-nameInput"
                     @keydown.enter="reloadTable" />
        </div>
      </div>
      <div class="flex pl-3 mb-3 mt-3">
        <SkillsButton size="small" @click="reloadTable" data-cy="achievementsNavigator-filterBtn" icon="fa fa-filter" label="Filter" />
        <SkillsButton size="small" @click="reset" class="ml-1" data-cy="achievementsNavigator-resetBtn" icon="fa fa-times" label="Reset" />
      </div>

      <SkillsDataTable class="mb-5"
                       data-cy="achievementsNavigator-table"
                       :value="items"
                       show-gridlines
                       striped-rows
                       lazy
                       :loading="loadingTable"
                       :total-records="totalRows"
                       :rows="pageSize"
                       :rowsPerPageOptions="possiblePageSizes"
                       v-model:sort-field="sortBy"
                       v-model:sort-order="sortOrder"
                       @page="pageChanged"
                       @sort="sortTable"
                       paginator
                       tableStoredStateId="achievementsNavigator-table">
        <Column field="userName" header="Username" sortable>
          <template #body="slotProps">
            <div class="flex">
              <div class="flex flex-1">
                <span>{{ slotProps.data.userName }}</span>
              </div>
              <div class="flex">
<!--                <router-link :to="{ name: 'ClientDisplayPreview', params: { projectId: projectId, userId: data.item.userId } }">-->
                  <SkillsButton size="small" data-cy="achievementsNavigator-clientDisplayBtn"><i class="fa fa-eye"/></SkillsButton>
<!--                </router-link>-->
              </div>
            </div>
          </template>
        </Column>
        <Column field="type" header="Type">
          <template #body="slotProps">
            <achievement-type :type="slotProps.data.type" />
          </template>
        </Column>
        <Column field="name" header="Name">
          <template #body="slotProps">
            <span v-if="slotProps.data.name === 'Overall'" class="font-light text-sm">
              N/A
            </span>
            <span v-else>
              {{ slotProps.data.name }}
            </span>
          </template>
        </Column>
        <Column field="level" header="Level">
          <template #body="slotProps">
            <span v-if="!slotProps.data.level" class="font-light text-sm">
              N/A
            </span>
            <span v-else>
              {{ slotProps.data.level }}
            </span>
          </template>
        </Column>
        <Column field="achievedOn" header="Date" sortable>
          <template #body="slotProps">
            <date-cell :value="slotProps.data.achievedOn" />
          </template>
        </Column>
      </SkillsDataTable>
    </template>
  </Card>
</template>

<style scoped>

</style>