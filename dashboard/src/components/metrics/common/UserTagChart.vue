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
import {computed, onMounted, ref} from 'vue';
import {useRoute} from 'vue-router';
import MetricsService from "@/components/metrics/MetricsService.js";
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";
import SkillsCalendarInput from "@/components/utils/inputForm/SkillsCalendarInput.vue";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";
import Chart from "primevue/chart";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";

const route = useRoute();
const layoutSizes = useLayoutSizesState()
const announcer = useSkillsAnnouncer()
const timeUtils = useTimeUtils();
const chartSupportColors = useChartSupportColors()

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
  chartJsOptions.value = setChartOptions()
  loadData();
});

const isLoading = ref(true);
const isEmpty = ref(false);
const chartData = ref({})
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
          const { items } = dataFromServer;
          isEmpty.value = items.find((item) => item.count > 0) === undefined;

          chartData.value = {
            labels: items.map((item) => item.value),
            datasets: [{
              label: 'Number of Users',
              data: items.map((item) => item.count),
              backgroundColor: chartSupportColors.getBackgroundColorArray(items.length),
              borderColor: chartSupportColors.getBorderColorArray(items.length),
              borderWidth: 1,
              borderRadius: 6,
              maxBarThickness: 15,
              minBarLength: 4,
            }]
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

const isPieChart = computed(() => props.chartType === 'pie')
const chartJsOptions = ref();
const userTagChart = ref(null)
const setChartOptions = () => {
  const colors = chartSupportColors.getColors()
  if (isPieChart.value) {
    return {
      responsive: true,
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            color: colors.textColor,
            padding: 20,
            boxWidth: 12,
            usePointStyle: true,
            pointStyle: 'circle'
          },
        },
      }
    }
  }
  return {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y',
    scales: {
      x: {
        ticks: {
          color: colors.textMutedColor
        },
        grid: {
          color: colors.contentBorderColor,
        }
      },
      y: {
        beginAtZero: true,
        ticks: {
          color: colors.textMutedColor
        },
        grid: {
          color: colors.contentBorderColor
        }
      }
    },
    plugins: {
      legend: {
        display: false
      },
    }
  };
}
</script>

<template>
  <Card data-cy="userTagChart" :style="`width: ${layoutSizes.tableMaxWidth}px;`">
    <template #header>
      <SkillsCardHeader :title="titleInternal"></SkillsCardHeader>
    </template>
    <template #content>
      <div class="flex flex-wrap gap-2 items-center mb-2">
        <div>
          Filter by Date(s):
        </div>
        <div class="flex gap-2">
          <SkillsCalendarInput selectionMode="range"
                               name="filterRange"
                               v-model="filterRange"
                               :maxDate="new Date()"
                               :disabled="isLoading"
                               placeholder="Select a date range"
                               data-cy="metricsDateFilter" />
          <SkillsButton label="Filter" icon="fa-solid fa-search"  @click="applyDateFilter" :disabled="isLoading" data-cy="applyDateFilterButton" />
          <SkillsButton label="Clear" severity="danger" icon="fa-solid fa-eraser" @click="clearDateFilter" :disabled="isLoading" data-cy="clearDateFilterButton" />
        </div>
      </div>
        <metrics-overlay :loading="isLoading" :has-data="!isEmpty" no-data-msg="No data yet...">
          <chart-download-controls :vue-chart-ref="userTagChart" />
          <Chart ref="userTagChart"
                 id="userTagChart"
                 :type="props.chartType"
                 :data="chartData"
                 :options="chartJsOptions"
                 :class="{
                   'min-h-[16em] w-full': !isPieChart,
                 }" />
        </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>