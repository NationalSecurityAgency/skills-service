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
import {ref, onMounted, computed} from 'vue';
import { useRoute } from 'vue-router';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import Chart from 'primevue/chart';
import 'chartjs-adapter-dayjs-4/dist/chartjs-adapter-dayjs-4.esm';
import dayjs from 'dayjs';
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import MetricsService from "@/components/metrics/MetricsService.js";
import TimeLengthSelector from "@/components/metrics/common/TimeLengthSelector.vue";
import NumberFormatter from '@/components/utils/NumberFormatter.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js';
import { useThemesHelper } from '@/components/header/UseThemesHelper.js';
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";


const appConfig = useAppConfig();
const route = useRoute();
const props = defineProps({
  title: {
    type: String,
    required: false,
    default: 'Users per day',
  },
});

const themeState = useSkillsDisplayThemeState()
const themeHelper = useThemesHelper()
const layoutSizes = useLayoutSizesState()

const chartAxisColor = () => {
  if (themeState.theme.charts.axisLabelColor) {
    return themeState.theme.charts.axisLabelColor
  }
  return themeHelper.isDarkTheme ? 'white' : undefined
}

onMounted(() => {
  if (route.params.skillId) {
    localProps.value.skillId = route.params.skillId;
  } else if (route.params.subjectId) {
    localProps.value.skillId = route.params.subjectId;
  }
  loadData();

  chartData.value = setChartData();
  chartJsOptions.value = setChartOptions();
})

const loading = ref(true);
const distinctUsersOverTime = ref([]);
const distinctUsersOverTimeChartData = ref([]);
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
const chartOptions = ref({
  chart: {
    type: 'area',
    stacked: false,
    height: 350,
    zoom: {
      type: 'x',
      enabled: true,
      autoScaleYaxis: true,
    },
    toolbar: {
      autoSelected: 'zoom',
      offsetY: -30,
    },
  },
  dataLabels: {
    enabled: false,
  },
  markers: {
    size: 0,
  },
  fill: {
    type: 'gradient',
    gradient: {
      shadeIntensity: 1,
      inverseColors: false,
      opacityFrom: 0.5,
      opacityTo: 0,
      stops: [0, 90, 100],
    },
  },
  yaxis: {
    labels: {
      style: {
        colors: chartAxisColor()
      },
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
    title: {
      text: 'Distinct # of Users',
    },
  },
  xaxis: {
    type: 'datetime',
    labels: {
      style: {
        colors: chartAxisColor()
      }
    }
  },
  tooltip: {
    theme: themeHelper.isDarkTheme ? 'dark' : 'light',
    shared: false,
        y: {
      formatter(val) {
        return NumberFormatter.format(val);
      },
    },
  },
});

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
          distinctUsersOverTime.value = [{
            data: response.users.map((item) => [item.value, item.count]),
            name: 'Users',
          }, {
            data: response.newUsers.map((item) => [item.value, item.count]),
            name: 'New Users',
          }];
          const formatTimestamp = (timestamp) => {
            const format = localProps.value.byMonth ? 'YYYY-MM' : 'YYYY-MM-DD';
            return dayjs(timestamp).format(format)
          }
          distinctUsersOverTimeChartData.value = {
            datasets: [{
              label: 'Users',
              data: response.users.map((item) => {
                return {x: formatTimestamp(item.value), y: item.count}
              }),
              cubicInterpolationMode: 'monotone',
              order: 2, // Lower order means it will be drawn first (in the background)
              borderColor: getComputedStyle(document.documentElement).getPropertyValue('--p-cyan-500'),
              backgroundColor: getComputedStyle(document.documentElement).getPropertyValue('--p-cyan-100'),
            }, {
              label: 'New Users',
              data: response.newUsers.map((item) => {
                return {x: formatTimestamp(item.value), y: item.count}
              }),
              cubicInterpolationMode: 'monotone',
              order: 1,  // Higher order means it will be drawn last (on top)
              borderColor: getComputedStyle(document.documentElement).getPropertyValue('--p-green-500'),
              backgroundColor: getComputedStyle(document.documentElement).getPropertyValue('--p-green-100'),
            }]
          }
          chartJsOptions.value.scales.x.time.unit = byMonth.value ? 'month' : 'day';
        } else {
          distinctUsersOverTime.value = [];
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


const chartData = ref();
const chartJsOptions = ref();

const setChartData = () => {
  const documentStyle = getComputedStyle(document.documentElement);

  return {
    labels: ['January', 'February', 'March', 'April', 'May', 'June', 'July'],
    datasets: [
      {
        label: 'First Dataset',
        data: [65, 59, 80, 81, 56, 55, 40],
        fill: false,
        borderColor: documentStyle.getPropertyValue('--p-cyan-500'),
        tension: 0.4
      },
      {
        label: 'Second Dataset',
        data: [28, 48, 40, 19, 86, 27, 90],
        fill: false,
        borderColor: documentStyle.getPropertyValue('--p-gray-500'),
        tension: 0.4
      }
    ]
  };
};
const setChartOptions = () => {
  const documentStyle = getComputedStyle(document.documentElement);
  const textColor = documentStyle.getPropertyValue('--p-text-color');
  const textColorSecondary = documentStyle.getPropertyValue('--p-text-muted-color');
  const surfaceBorder = documentStyle.getPropertyValue('--p-content-border-color');

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
          <span class="mr-3">
            <Badge v-for="option in dateOptions" class="ml-2"
                   :class="{'can-select': byMonth !== option.value }"
                   :severity="byMonth === option.value ? 'success' : 'secondary'"
                   :key="option.label"
                   @click="dateOptionChanged(option.value)">
              {{option.label}}
            </Badge>
          </span>
          |
          <time-length-selector :options="timeSelectorOptions" @time-selected="updateTimeRange" ref="timeRangeSelector" :disable-days="byMonth" />
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasDataEnoughData" no-data-msg="This chart needs at least 2 days of user activity.">
        <chart-download-controls :vue-chart-ref="usersChartRef" />
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