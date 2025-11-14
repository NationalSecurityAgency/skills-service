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
import {computed, onMounted, ref, watch} from 'vue'
import {useAppConfig} from '@/common-components/stores/UseAppConfig.js';
import dayjs from 'dayjs'
import MetricsService from "@/components/metrics/MetricsService.js";
import MetricsOverlay from '@/components/metrics/utils/MetricsOverlay.vue';
import TimeLengthSelector from "@/components/metrics/common/TimeLengthSelector.vue";
import Chart from "primevue/chart";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";

const props = defineProps({
  availableProjects: {
    type: Array,
    required: true,
  },
  title: {
    type: String,
    required: false,
    default: 'Your Daily Usage History',
  },
});

const appConfig = useAppConfig()
const chartSupportColors = useChartSupportColors()

const timeLengthSelector = ref(null);
const loading = ref(true);
const hasData = ref(false);
const mutableTitle = ref(props.title);
const projects = ref({
    selected: [],
    available: [],
  });
const timeProps = ref({
  start: dayjs().subtract(30, 'day').valueOf(),
});
const timeSelectorOptions = ref([
  {
    length: 30,
    unit: 'days',
  },
  {
    length: 6,
    unit: 'months',
  },
  {
    length: 1,
    unit: 'year',
  },
]);
const chartData = ref({})
const chartJsOptions = ref();

onMounted(() => {
  chartJsOptions.value = setChartOptions()
  projects.value.available = props.availableProjects.map((proj) => ({ ...proj }));
  const numProjectsToSelect = Math.min(props.availableProjects.length, 4);
  const availableSortedByMostPoints = projects.value.available.sort((a, b) => b.points - a.points);
  projects.value.selected = availableSortedByMostPoints.slice(0, numProjectsToSelect);
  // loadData() is not called here because of the watch on `projects.selected`, which is triggered by the assignment above
})

const enoughOverallProjects = computed(() => {
  return props.availableProjects && props.availableProjects.length > 0;
});
const enoughProjectsSelected = computed(() => {
  return projects.value.selected && projects.value.selected.length > 0;
});
const noDataMessage = computed(() => {
  if (!enoughOverallProjects.value) {
    return 'There are no projects available.';
  }
  if (!enoughProjectsSelected.value) {
    return 'Please select at least one project from the list above.';
  }
  return 'There are no events for the selected project(s) and time period.';
});
watch(() => projects.value.selected, () => {
  timeProps.value.projIds = projects.value.selected.map((project) => project.projectId);
  loadData();
})

const updateTimeRange = (timeEvent) => {
  if (appConfig.maxDailyUserEvents) {
    const oldestDaily = dayjs().subtract(appConfig.maxDailyUserEvents, 'day');
    if (timeEvent.startTime < oldestDaily) {
      mutableTitle.value = 'Events per week';
    } else {
      mutableTitle.value = props.title;
    }
  }
  timeProps.value.start = timeEvent.startTime.valueOf();
  loadData();
}
const loadData = () => {
  loading.value = true;
  if (enoughOverallProjects.value && enoughProjectsSelected.value) {
    MetricsService.loadMyMetrics('allProjectsSkillEventsOverTimeMetricsBuilder', timeProps.value)
        .then((response) => {
          if (response && response.length > 0 && notAllZeros(response)) {
            hasData.value = true;

            const formatTimestamp = (timestamp) => dayjs(timestamp).format('YYYY-MM-DD')
            const datasets = response.map((item) => {
              const proj = props.availableProjects.find(({ projectId }) => projectId === item.project);
              return {
                label: proj.projectName,
                data: item.countsByDay.map((item) => {
                  return {x: formatTimestamp(item.timestamp), y: item.num}
                }),
                cubicInterpolationMode: 'monotone',
              };
            });
            chartData.value = {
              datasets: datasets,
            };
          } else {
            chartData.value = {}
            hasData.value = false;
          }
          loading.value = false;
        });
  } else {
    hasData.value = false;
    loading.value = false;
  }
};
const notAllZeros = (data) => {
  return data.filter((item) => item.countsByDay.find((it) => it.num > 0)).length > 0;
}

const setChartOptions = () => {
  const colors = chartSupportColors.getColors()
  const textColorSecondary = colors.textMutedColor
  const surfaceBorder =  colors.contentBorderColor

  return {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        type: 'time',
        time: {
          unit: 'day'
        },
        ticks: {
          color: textColorSecondary
        },
        grid: {
          color: surfaceBorder,
          drawOnChartArea: false  // Ensures no grid lines are drawn in the chart area
        }
      },
      y: {
        beginAtZero: true,
        ticks: {
          stepSize: 1,
          color: textColorSecondary
        },
        grid: {
          color: surfaceBorder
        },
      }
    },
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: colors.textColor,
          padding: 20,
          boxWidth: 12,
          usePointStyle: true,
          pointStyle: 'circle'
        }
      }
    }
  };
}

const eventHistoryChartRef = ref(null)
</script>

<template>
  <Card data-cy="eventHistoryChart">
    <template #header>
      <SkillsCardHeader :title="mutableTitle" title-tag="h2">
        <template #headerContent>
          <div class="flex gap-2 items-center">
            <time-length-selector ref="timeLengthSelector" :options="timeSelectorOptions" @time-selected="updateTimeRange"/>
            <chart-download-controls v-if="hasData" :vue-chart-ref="eventHistoryChartRef" />
          </div>
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <div class="flex w-full mb-2">
        <MultiSelect
            v-model="projects.selected"
            :options="projects.available"
            display="chip"
            optionLabel="projectName"
            aria-label="Select projects"
            placeholder="Select projects"
            class="w-full"
            :selection-limit="5"
            data-cy="eventHistoryChartProjectSelector">

        </MultiSelect>
      </div>

      <MetricsOverlay :loading="loading" :has-data="hasData" :no-data-msg="noDataMessage">
        <Chart ref="eventHistoryChartRef"
               type="line"
               :data="chartData"
               :options="chartJsOptions"
               class="h-[30rem]" />
      </MetricsOverlay>
    </template>
  </Card>
</template>

<style scoped>

</style>