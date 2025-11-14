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
import {onMounted, ref} from 'vue'
import {useRoute} from 'vue-router';
import {useNumberFormat} from "@/common-components/filter/UseNumberFormat.js";
import QuizService from '@/components/quiz/QuizService.js';
import MetricsOverlay from '@/components/metrics/utils/MetricsOverlay.vue';
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";
import Chart from "primevue/chart";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";
import dayjs from "dayjs";

const route = useRoute()
const numberFormat = useNumberFormat()
const chartSupportColors = useChartSupportColors()

const loading = ref(false);
const hasData = ref(false);
const chartData = ref({})
const chartJsOptions = ref();

onMounted(()=> {
  chartJsOptions.value = setChartOptions()
  loading.value = true;
  QuizService.getUsageOverTime(route.params.quizId)
      .then((res) => {
        hasData.value = res && res.length > 0;
        const formatTimestamp = (timestamp) => dayjs(timestamp).format('YYYY-MM-DD')
        chartData.value = {
          datasets: [{
            label: '# of Runs',
            data: res.map((item) => {
              return {x: formatTimestamp(item.value), y: item.count}
            }),
            cubicInterpolationMode: 'monotone',
          }]
        }
      })
      .finally(() => {
        loading.value = false;
      });
})
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
        title: {
          display: true,
          text: '# of Runs',
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
        display: false,
      },
    }
  };
}

const runsOverTimeChartRef = ref(null)
</script>

<template>

  <Card>
    <template #header>
      <SkillsCardHeader title="Runs Over Time">
        <template #headerContent>
          <chart-download-controls v-if="hasData" :vue-chart-ref="runsOverTimeChartRef" />
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <MetricsOverlay :loading="loading" :has-data="hasData" no-data-msg="This chart needs at least 2 days worth of runs">
        <Chart ref="runsOverTimeChartRef"
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