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
import { useUserTagChartConfig } from '@/components/metrics/common/UserTagChartConfig.js';
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";
import SkillsCalendarInput from "@/components/utils/inputForm/SkillsCalendarInput.vue";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";

const route = useRoute();
const userTagChartConfig = useUserTagChartConfig();
const layoutSizes = useLayoutSizesState()
const announcer = useSkillsAnnouncer()
const timeUtils = useTimeUtils();

const props = defineProps({
  tagKey: {
    type: String,
    required: true,
  },
  chartType: {
    type: String,
    required: true,
    validator: (value) => (['pie', 'bar'].indexOf(value) >= 0),
  },
  title: {
    type: String,
    required: false,
    default: 'Users',
  },
})

onMounted(() => {
  if (props.chartType === 'pie') {
    chartOptions.value = userTagChartConfig.pieChartOptions;
  }
  if (props.chartType === 'bar') {
    chartOptions.value = userTagChartConfig.barChartOptions;
  }
  loadData();
});

const isLoading = ref(true);
const isEmpty = ref(false);
const series = ref([]);
const chartOptions = ref({});
const heightInPx = ref(350);
const titleInternal = ref(props.title);
const filterRange = ref([]);

const loadData = () => {
  isLoading.value = true;
  const dateRange = timeUtils.prepareDateRange(filterRange.value)

  const params = {
    tagKey: props.tagKey,
    currentPage: 1,
    pageSize: 20,
    sortDesc: true,
    tagFilter: '',
    fromDayFilter: dateRange.startDate,
    toDayFilter: dateRange.endDate,
  };

  MetricsService.loadChart(route.params.projectId, 'numUsersPerTagBuilder', params)
      .then((dataFromServer) => {
        if (dataFromServer) {
          const localSeries = [];
          const labels = [];
          const { items } = dataFromServer;
          items.forEach((data) => {
            localSeries.push(data.count);
            labels.push(data.value);
          });
          if (props.chartType === 'pie') {
            series.value = localSeries;
          }
          if (props.chartType === 'bar') {
            series.value = [{
              name: 'Number of Users',
              data: localSeries,
            }];
          }
          chartOptions.value = Object.assign(chartOptions.value, { labels });
          isEmpty.value = items.find((item) => item.count > 0) === undefined;

          if (items.length > 10) {
            heightInPx.value = 600;
          }
          if (dataFromServer.totalNumItems > params.pageSize) {
            titleInternal.value = `${titleInternal.value} (Top ${params.pageSize})`;
          }
        }
        isLoading.value = false;
      });
};

const applyDateFilter = () => {
  announcer.polite(`Results have been filtered by date, from ${filterRange.value[0]}` + filterRange.value.length > 1 ? ` to ${filterRange.value[1]}` : '')
  loadData()
};

const clearDateFilter = () => {
  announcer.polite("Clearing the date range filter")
  filterRange.value = [];
  loadData()
};
</script>

<template>
  <Card data-cy="userTagChart" :style="`width: ${layoutSizes.tableMaxWidth}px;`">
    <template #header>
      <SkillsCardHeader :title="titleInternal"></SkillsCardHeader>
    </template>
    <template #content>
      <skills-spinner :is-loading="isLoading" v-if="isLoading" />
      <div class="flex flex-wrap gap-2 items-center mb-2">
        <div>
          Filter by Date(s):
        </div>
        <div class="flex gap-2">
          <SkillsCalendarInput selectionMode="range" name="filterRange" v-model="filterRange" :maxDate="new Date()" placeholder="Select a date range" data-cy="metricsDateFilter" />
          <SkillsButton label="Filter" icon="fa-solid fa-search"  @click="applyDateFilter" data-cy="applyDateFilterButton" />
          <SkillsButton label="Clear" severity="danger" icon="fa-solid fa-eraser" @click="clearDateFilter" data-cy="clearDateFilterButton" />
        </div>
      </div>
      <div v-if="!isLoading">
        <metrics-overlay :loading="isLoading" :has-data="!isEmpty" no-data-msg="No data yet...">
          <apexchart :type="chartType" width="100%" :height="`${heightInPx}px`"  :options="chartOptions" :series="series"></apexchart>
        </metrics-overlay>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>