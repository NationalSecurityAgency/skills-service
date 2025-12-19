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
import {onMounted, ref, watch} from 'vue'
import {useRoute} from 'vue-router';
import {useNumberFormat} from '@/common-components/filter/UseNumberFormat.js';
import QuizService from '@/components/quiz/QuizService.js';
import MetricsOverlay from '@/components/metrics/utils/MetricsOverlay.vue';
import {useUserTagsUtils} from "@/components/utils/UseUserTagsUtils.js";
import Chart from "primevue/chart";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";

const props = defineProps({
  dateRange: Array,
})

const route = useRoute()
const numberFormat = useNumberFormat()
const userTagsUtils = useUserTagsUtils();
const chartSupportColors = useChartSupportColors()
const timeUtils = useTimeUtils()

const loading = ref(true);
const hasData = ref(false);
const chartJsOptions = ref();
const chartData = ref({})

watch(() => props.dateRange, (newDateRange) => {
  loadData()
});

onMounted(()=> {
  chartJsOptions.value = setChartOptions()
  loadData()
})

const loadData = () => {
  loading.value = true;
  const dateRange = timeUtils.prepareDateRange(props.dateRange)
  QuizService.getUserTagCounts(route.params.quizId, userTagsUtils.userTagKey(), dateRange.startDate, dateRange.endDate)
      .then((res) => {
        chartData.value = {
          labels: res.map((item) => item.value),
          datasets: [{
            label: '# of Runs',
            data: res.map((item) => item.count),
            backgroundColor: chartSupportColors.getBackgroundColorArray(res.length),
            borderColor: chartSupportColors.getBorderColorArray(res.length),
            borderWidth: 1,
            borderRadius: 6,
            minBarLength: 4,
            maxBarThickness: 40,
          }]
        }

        hasData.value = res.length > 0;
      })
      .finally(() => {
        loading.value = false;
      });
}

const setChartOptions = () => {
  const colors = chartSupportColors.getColors()
  return {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y',
    scales: {
      x: {
        title: {
          display: true,
          text: '# of Runs',
          color: colors.textMutedColor,
        },
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

const quizUserTagsChartRef = ref()
</script>

<template>
    <Card data-cy="quizUserTagsChart">
      <template #header>
        <SkillsCardHeader :title="`${userTagsUtils.userTagLabel()} Metrics (Top 20)`">
          <template #headerContent>
            <chart-download-controls v-if="hasData" :vue-chart-ref="quizUserTagsChartRef"/>
          </template>
        </SkillsCardHeader>
      </template>
      <template #content>
          <MetricsOverlay :loading="loading" :has-data="hasData" no-data-msg="No data yet...">
            <Chart ref="quizUserTagsChartRef"
                   type="bar"
                   :data="chartData"
                   :options="chartJsOptions"
                   class="min-h-[12em]"
            />
          </MetricsOverlay>

      </template>
    </Card>
</template>

<style scoped>
</style>