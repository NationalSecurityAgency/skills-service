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
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router';
import dayjs from 'dayjs';
import MetricsService from "@/components/metrics/MetricsService.js";
import DateCell from "@/components/utils/table/DateCell.vue";
import AchievementType from "@/components/metrics/projectAchievements/AchievementType.vue";
import SkillsDataTable from "@/components/utils/table/SkillsDataTable.vue";
import InputText from "primevue/inputtext";
import SkillsCalendarInput from "@/components/utils/inputForm/SkillsCalendarInput.vue";
import SkillsDropDown from "@/components/utils/inputForm/SkillsDropDown.vue";
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import Column from 'primevue/column'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import TableNoRes from "@/components/utils/table/TableNoRes.vue";
import {useStorage} from "@vueuse/core";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

const route = useRoute();
const numberFormat = useNumberFormat()
const responsive = useResponsiveBreakpoints()
const isFlex = computed(() => responsive.md.value)
const appConfig = useAppConfig()

const showUserTagColumn = computed(() => {
  return !!(appConfig.usersTableAdditionalUserTagKey && appConfig.usersTableAdditionalUserTagLabel)
})

const tagKey = computed(() => {
  return appConfig.usersTableAdditionalUserTagKey;
});

const usernameFilter = ref('');
const fromDayFilter = ref();
const toDayFilter = ref();
const nameFilter = ref('');
const userTagFilter = ref('');
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
  selected: ['Overall', 'Subject', 'Skill', 'SkillsGroup', 'Badge'],
  available: ['Overall', 'Subject', 'Skill', 'SkillsGroup', 'Badge'],
});

const sortBy = ref('achievedOn');
const sortOrder = ref(-1);
const currentPage = ref(1);
const totalRows = ref(0);
const pageSize = useStorage('achievementsNavigator-pageSize', 5)
const possiblePageSizes = [5, 10, 15, 20, 50];
const loadingTable = ref(false);
const isExporting = ref(false)

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
  fromDayFilter.value = null;
  toDayFilter.value = null;
  userTagFilter.value = '';
  nameFilter.value = '';
  levels.selected = '';
  achievementTypes.value.selected = achievementTypes.value.available;
  reloadTable();
};

const reloadTable = () => {
  loadingTable.value = true;
  const params = getQueryParams();

  MetricsService.loadChart(route.params.projectId, 'userAchievementsChartBuilder', params)
      .then((dataFromServer) => {
        items.value = dataFromServer.items;
        totalRows.value = dataFromServer.totalNumItems;
        loadingTable.value = false;
      });
};

const exportAchievements = () => {
  loadingTable.value = true;
  isExporting.value = true;
  const params = getQueryParams();
  MetricsService.exportProjectUserAchievements(route.params.projectId, params)
      .then((dataFromServer) => {
        isExporting.value = false;
        loadingTable.value = false;
      });
};

const getQueryParams = () => {
  const orderBy = sortBy.value === appConfig.usersTableAdditionalUserTagKey ? 'userTag' : sortBy.value;
  return {
    pageSize: pageSize.value,
    currentPage: currentPage.value,
    usernameFilter: usernameFilter.value,
    fromDayFilter: fromDayFilter.value ? dayjs(fromDayFilter.value).format('YYYY-MM-DD') : '',
    toDayFilter: toDayFilter.value ? dayjs(toDayFilter.value).format('YYYY-MM-DD') : '',
    nameFilter: nameFilter.value,
    minLevel: levels.selected,
    achievementTypes: achievementTypes.value.selected,
    sortBy: orderBy,
    sortDesc: sortOrder.value !== 1,
    tagFilter: userTagFilter.value,
  };
}

const typeLabel = (type) => {
  if (type === 'SkillsGroup') return 'Group';
  return type;
};
</script>

<template>
  <Card data-cy="achievementsNavigator" :pt="{ body: { class: 'p-0!' } }">
    <template #header>
      <SkillsCardHeader title="Achievements"></SkillsCardHeader>
    </template>
    <template #content>
      <div class="p-4 flex flex-col gap-2">
        <div class="flex flex-col xl:flex-row gap-3">
          <div class="flex-1 flex flex-col gap-2">
            <label for="user-name-filter">User Name Filter:</label>
            <InputText class="w-full"
                       v-model="usernameFilter"
                       id="user-name-filter"
                       data-cy="achievementsNavigator-usernameInput"
                       @keydown.enter="reloadTable"
                       aria-label="Skill name filter" />
          </div>
          <div class="flex-1 flex flex-col gap-2">
            <label for="name-filter">Subject, Skill or Badge Name:</label>
            <InputText class="w-full"
                       v-model="nameFilter"
                       id="name-filter"
                       data-cy="achievementsNavigator-nameInput"
                       @keydown.enter="reloadTable" />
          </div>
          <div v-if="showUserTagColumn" class="flex-1 flex flex-col gap-2">
            <div>
              <label for="userTagFilter">{{ appConfig.usersTableAdditionalUserTagLabel }} Filter</label>
            </div>
            <InputText id="userTagFilter" v-model="userTagFilter" v-on:keydown.enter="reloadTable"
                       class="w-full"
                       data-cy="users-userTagFilter" aria-label="user tag filter" />
          </div>
        </div>
        <div class="flex flex-col xl:flex-row gap-3">
          <div class="flex-1">
            <SkillsCalendarInput
                v-model="fromDayFilter"
                id="from-date-filter"
                data-cy="achievementsNavigator-fromDateInput"
                label="From Date:"
                name="fromDayFilter"
                input-class="w-full"
                :max-date="toDayFilter" />
          </div>
          <div class="flex-1">
            <SkillsCalendarInput
                v-model="toDayFilter"
                id="to-date-filter"
                data-cy="achievementsNavigator-toDateInput"
                label="To Date:"
                name="toDayFilter"
                input-class="w-full"
                :min-date="fromDayFilter" />
          </div>
          <div class="flex-1">
            <SkillsDropDown
                label="Overall or Subject Minimum Level"
                name="levels-input-group"
                id="levels-input-group"
                data-cy="achievementsNavigator-levelsInput"
                placeholder="Optionally select level"
                optionLabel="text"
                optionValue="value"
                v-model="levels.selected"
                :options="levels.available" />
          </div>
        </div>
        <div class="flex flex-col gap-2 mt-1">
          <label>Types: </label>
          <div class="flex gap-2" data-cy="achievementsNavigator-typeInput">
                <span v-for="tag in achievementTypes.available" :key="tag">
                  <Checkbox v-model="achievementTypes.selected" :value="tag" :name="tag" :inputId="tag"></Checkbox>
                  <label :for="tag" class="ml-2">
                    {{ typeLabel(tag) }}
                  </label>
                </span>
          </div>
        </div>
        <div class="mt-3 mb-2">
          <SkillsButton aria-label="Filter" @click="reloadTable" data-cy="achievementsNavigator-filterBtn" icon="fa fa-filter" label="Filter" />
          <SkillsButton aria-label="Reset" @click="reset" class="ml-1" data-cy="achievementsNavigator-resetBtn" icon="fa fa-times" label="Reset" />
        </div>
      </div>

      <SkillsDataTable
        data-cy="achievementsNavigator-table"
        aria-label="Achievements"
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
        <template #loading>
          <div>
            <Message v-if="isExporting" icon="fas fa-download" severity="contrast" :closable="false">Exporting, please wait...</Message>
            <SkillsSpinner :is-loading="true"></SkillsSpinner>
          </div>
        </template>
        <template #header>
          <div class="flex justify-end flex-wrap">
            <SkillsButton
                :disabled="totalRows <= 0"
                size="small"
                icon="fas fa-download"
                label="Export"
                @click="exportAchievements"
                data-cy="exportAchievementsTableBtn" />
          </div>
        </template>
        <Column field="userName" header="Username" sortable :class="{'flex': isFlex }">
          <template #body="slotProps">
            <div class="flex">
              <div class="flex flex-1">
                <span>
                  {{ slotProps.data.userName }}
                </span>
              </div>
              <div class="flex ml-2">
                <router-link :to="{ name: 'SkillsDisplaySkillsDisplayPreviewProject', params: { projectId: route.paramsprojectId, userId: slotProps.data.userId } }" tabindex="-1">
                  <SkillsButton aria-label="View Project" size="small" data-cy="achievementsNavigator-clientDisplayBtn"><i class="fa fa-eye"/></SkillsButton>
                </router-link>
              </div>
            </div>
          </template>
        </Column>
        <Column v-if="showUserTagColumn"
                sortable
                :field="appConfig.usersTableAdditionalUserTagKey"
                :header="appConfig.usersTableAdditionalUserTagLabel"
                :class="{'flex': responsive.md.value }">
          <template #body="slotProps">
            <router-link
                v-if="showUserTagColumn && slotProps.data.userTag"
                :to="{ name: 'UserTagMetrics', params: { projectId: route.params.projectId, tagKey: tagKey, tagFilter: slotProps.data.userTag } }"
                class="text-info mb-0 pb-0 preview-card-title"
                :aria-label="`View metrics for ${slotProps.data.userTag}`"
                role="link"
                data-cy="usersTable_viewUserTagMetricLink">
              {{ slotProps.data.userTag }}
            </router-link>
          </template>
        </Column>
        <Column field="type" header="Type" :class="{'flex': isFlex }">
          <template #body="slotProps">
            <achievement-type :type="slotProps.data.type" />
          </template>
        </Column>
        <Column field="name" header="Name" :class="{'flex': isFlex }">
          <template #body="slotProps">
            <span v-if="slotProps.data.name === 'Overall'" class="font-light text-sm">
              N/A
            </span>
            <span v-else>
              {{ slotProps.data.name }}
            </span>
          </template>
        </Column>
        <Column field="level" header="Level" :class="{'flex': isFlex }">
          <template #body="slotProps">
            <span v-if="!slotProps.data.level" class="font-light text-sm">
              N/A
            </span>
            <span v-else>
              {{ slotProps.data.level }}
            </span>
          </template>
        </Column>
        <Column field="achievedOn" header="Date" sortable :class="{'flex': isFlex }">
          <template #body="slotProps">
            <date-cell :value="slotProps.data.achievedOn" />
          </template>
        </Column>

        <template #paginatorstart>
          <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ numberFormat.pretty(totalRows) }}</span>
        </template>

        <template #empty>
          <table-no-res data-cy="emptyTable" />
        </template>
      </SkillsDataTable>
    </template>
  </Card>
</template>

<style scoped>

</style>