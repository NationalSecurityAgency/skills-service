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
import {useAppConfig} from '@/common-components/stores/UseAppConfig.js'
import Chart from 'primevue/chart';
import 'chartjs-adapter-dayjs-4/dist/chartjs-adapter-dayjs-4.esm';
import dayjs from 'dayjs';
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import MetricsService from "@/components/metrics/MetricsService.js";
import TimeLengthSelector from "@/components/metrics/common/TimeLengthSelector.vue";
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";


const appConfig = useAppConfig();
const chartSupportColors = useChartSupportColors()
const route = useRoute();
const props = defineProps({
  title: {
    type: String,
    required: false,
    default: 'Users per day',
  },
});

const layoutSizes = useLayoutSizesState()

onMounted(() => {
  if (route.params.skillId) {
    localProps.value.skillId = route.params.skillId;
  } else if (route.params.subjectId) {
    localProps.value.skillId = route.params.subjectId;
  }
  loadData();
  chartJsOptions.value = setChartOptions();
})

const loading = ref(true);
const distinctUsersOverTimeChartData = ref({});
const hasDataEnoughData = ref(false);
const mutableTitle = ref(props.title);
const byMonth = ref(false);
const localProps = ref({
  start: dayjs().subtract(30, 'day').valueOf(),
  byMonth: false,
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
const dateOptions = [{ label: 'Day/Week', value: false}, { label: 'Month', value: true }]
const currentDateOption = ref('days');
const isUsingDays = computed(() => {
  return currentDateOption.value === 'days';
})

const updateTimeRange = (timeEvent) => {
  if (appConfig) {
    const oldestDaily = dayjs().subtract(appConfig.maxDailyUserEvents, 'day');
    if (!byMonth.value) {
      if (timeEvent.startTime < oldestDaily) {
        mutableTitle.value = 'Users per week';
      } else {
        mutableTitle.value = props.title;
      }
    } else {
      mutableTitle.value = 'Users per month';
    }
  }
  localProps.value.start = timeEvent.startTime.valueOf();
  currentDateOption.value = timeEvent.durationUnit;
  loadData();
};

const allZeros = (data) => {
  return data.filter((item) => item.count > 0).length === 0;
};

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'distinctUsersOverTimeForProject', localProps.value)
      .then((response) => {

        if (response && response.users?.length > 1 && !allZeros(response.users)) {
          hasDataEnoughData.value = true;
          const formatTimestamp = (timestamp) => {
            const format = localProps.value.byMonth ? 'YYYY-MM' : 'YYYY-MM-DD';
            return dayjs(timestamp).format(format)
          }

          const addZeroPointAtStart = (data) => {
            if (data && data.length > 0) {
              const earliestDate = data[0].value
              const prevDay = dayjs(earliestDate).subtract(1, 'day').toDate()
              data.unshift({
                value: formatTimestamp(prevDay),
                count: 0
              })
            }

            return data
          }

          const usersData = addZeroPointAtStart(response.users)
          const usersSeriesData = usersData.map((item) => {
            return {x: formatTimestamp(item.value), y: item.count}
          })

          const newUsersData = addZeroPointAtStart(response.newUsers)
          const newUsersSeriesData = newUsersData.map((item) => {
            return {x: formatTimestamp(item.value), y: item.count}
          })
          distinctUsersOverTimeChartData.value = {
            datasets: [{
              label: 'Users',
              data: usersSeriesData,
              cubicInterpolationMode: 'monotone',
              order: 2, // Lower order means it will be drawn first (in the background)
              borderColor: getComputedStyle(document.documentElement).getPropertyValue('--p-cyan-500'),
              backgroundColor: getComputedStyle(document.documentElement).getPropertyValue('--p-cyan-100'),
            }, {
              label: 'New Users',
              data: newUsersSeriesData,
              cubicInterpolationMode: 'monotone',
              order: 1,  // Higher order means it will be drawn last (on top)
              borderColor: getComputedStyle(document.documentElement).getPropertyValue('--p-green-500'),
              backgroundColor: getComputedStyle(document.documentElement).getPropertyValue('--p-green-100'),
            }]
          }
          chartJsOptions.value.scales.x.time.unit = byMonth.value ? 'month' : 'day';
        } else {
          distinctUsersOverTimeChartData.value = [];
          hasDataEnoughData.value = false;
        }
        loading.value = false;
      });
};

const timeRangeSelector = ref(null);

const dateOptionChanged = (option) => {
  byMonth.value = option
  localProps.value.byMonth = option

  if(byMonth.value) {
    mutableTitle.value = 'Users per month';
  } else {
    mutableTitle.value = 'Users per week';
  }

  if(isUsingDays.value && byMonth.value) {
    timeRangeSelector.value.handleClick(1);
  } else {
    loadData()
  }
}

const chartJsOptions = ref();
const setChartOptions = () => {
  const colors = chartSupportColors.getColors()
  const textColor = colors.textColor
  const textColorSecondary = colors.textMutedColor
  const surfaceBorder =  colors.contentBorderColor

  return {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        type: 'time',
        time: {
          unit: 'day',
          displayFormats: {
            'day': 'MMM D, YYYY', // Example: Jan 1, 2023
            'month': 'MMM YYYY', // Example: Jan 2023
            'year': 'YYYY'        // Example: 2023
          },
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
        title: {
          display: true,
          text: 'Distinct # of Users',
          color: textColorSecondary,
          font: {
            size: 12,
            weight: '500'
          },
          padding: { left: 10, right: 5 }
        }
      }
    },
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: textColor,
          padding: 20,
          boxWidth: 12,
          usePointStyle: true,
          pointStyle: 'circle'
        }
      },
      tooltip: {
        callbacks: {
          title: (context) => {
            const date = new Date(context[0].parsed.x)
            const formatStr = byMonth.value ? 'MMM YYYY' :'MMM D, YYYY'
            return dayjs(date).format(formatStr)
          },
        },
      }
    }
  };
}

const usersChartRef = ref(null)
</script>

<template>
  <Card data-cy="distinctNumUsersOverTime" class="w-full" :style="`width: ${layoutSizes.tableMaxWidth}px;`">
    <template #header>
      <SkillsCardHeader :title="mutableTitle">
        <template #headerContent>
          <div class="flex gap-2 items-center">
          <div class="flex gap-1">
            <Badge v-for="option in dateOptions"
                   :class="{'can-select': byMonth !== option.value }"
                   :severity="byMonth === option.value ? 'success' : 'secondary'"
                   :key="option.label"
                   @click="dateOptionChanged(option.value)">
              {{option.label}}
            </Badge>
          </div>
          |
          <time-length-selector :options="timeSelectorOptions" @time-selected="updateTimeRange" ref="timeRangeSelector" :disable-days="byMonth" />

          <chart-download-controls :vue-chart-ref="usersChartRef" />
          </div>
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasDataEnoughData" no-data-msg="This chart needs at least 2 days of user activity.">
        <Chart ref="usersChartRef"
               id="usersTimeChart"
               type="line"
               :data="distinctUsersOverTimeChartData"
               :options="chartJsOptions"
               class="h-[30rem]" />
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>
.can-select {
  cursor: pointer;
}
</style>