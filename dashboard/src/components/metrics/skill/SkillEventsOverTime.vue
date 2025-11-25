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
import {onMounted, ref} from 'vue';
import {useRoute} from 'vue-router';
import dayjs from 'dayjs';
import TimeLengthSelector from "@/components/metrics/common/TimeLengthSelector.vue";
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import MetricsService from "@/components/metrics/MetricsService.js";
import Chart from "primevue/chart";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";

const props = defineProps(['skillName']);

const route = useRoute();
const chartSupportColors = useChartSupportColors()

const title = 'Skill events';
const loading = ref(true);
const hasData = ref(false);
const chartData = ref({})
const chartJsOptions = ref();
const animationEnded = ref(false)
const start = ref(dayjs().subtract(30, 'day').valueOf());
const timeSelectorOptions = [
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
];

onMounted(() => {
  chartJsOptions.value = setChartOptions()
  loadData();
});

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'skillEventsOverTimeChartBuilder', {
    skillId: route.params.skillId,
    start: start.value
  })
      .then((dataFromServer) => {
        const datasets = []
        const formatTimestamp = (timestamp) => dayjs(timestamp).format('YYYY-MM-DD')

        const hasAppliedSkillEvents = dataFromServer.countsByDay && dataFromServer.countsByDay.length > 1
        if (hasAppliedSkillEvents) {
          datasets.push({
            label: 'Applied Events',
            data: dataFromServer.countsByDay.map((item) => {
              return {x: formatTimestamp(item.timestamp), y: item.num}
            }),
            cubicInterpolationMode: 'monotone',
          })
        }
        const hasAllEvents = dataFromServer.allEvents && dataFromServer.allEvents.length > 0
        if (hasAllEvents) {
          datasets.push({
            label: 'All Events',
            data: dataFromServer.allEvents.map((item) => {
              return {x: formatTimestamp(item.timestamp), y: item.num}
            }),
            cubicInterpolationMode: 'monotone',
          })
        }
        chartData.value = {datasets}

        hasData.value = Boolean(hasAllEvents | hasAppliedSkillEvents);
        loading.value = false;
      });
};

const updateTimeRange = (timeEvent) => {
  start.value = timeEvent.startTime.valueOf();
  loadData();
};

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
          unit: 'month'
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
          color: colors.textColor,
          padding: 20,
          boxWidth: 12,
          usePointStyle: true,
          pointStyle: 'circle'
        }
      },
    },
    animation: {
      onComplete: function() {
        animationEnded.value = true;
      }
    },
  };
}

const skillEventsOverTimeChartRef = ref(null)
</script>

<template>
  <Card data-cy="appliedSkillEventsOverTimeMetric">
    <template #header>
      <SkillsCardHeader :title="title">
        <template #headerContent>
          <div class="flex items-center gap-2" v-if="hasData">
            <time-length-selector :options="timeSelectorOptions" @time-selected="updateTimeRange"/>
            <chart-download-controls :vue-chart-ref="skillEventsOverTimeChartRef"/>
          </div>
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="This chart needs at least 2 days of user activity.">
        <Chart ref="skillEventsOverTimeChartRef"
               type="line"
               :data="chartData"
               :options="chartJsOptions"
               :class="{ 'h-[30rem]': hasData, 'h-[16rem]': !hasData }" />
        <div v-if="animationEnded" data-cy="skillEventsOverTimeChart-animationEnded"></div>
      </metrics-overlay>
      <div class="font-light text-sm mt-2">Please Note: Only 'applied' events contribute to users' points and achievements. An event will not be applied if that skill has already reached its maximum points or has unfulfilled dependencies.</div>
    </template>
  </Card>
</template>

<style scoped>

</style>