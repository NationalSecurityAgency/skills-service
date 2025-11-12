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
import MetricsService from '@/components/metrics/MetricsService.js';
import {useRoute} from 'vue-router';
import MetricsOverlay from '@/components/metrics/utils/MetricsOverlay.vue';
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";
import Chart from "primevue/chart";
import ChartDataLabels from 'chartjs-plugin-datalabels';
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";


const route = useRoute();
const props = defineProps({
  title: {
    type: String,
    required: false,
    default: 'Overall Levels',
  },
});
const layoutSizes = useLayoutSizesState()
const chartSupportColors = useChartSupportColors()

const isLoading = ref(true);
const isEmpty = ref(false);
const chartData = ref([])

onMounted(() => {
  chartJsOptions.value = setChartOptions();
  let localProps = { };
  // figure out if subjectId is passed based on the context (page it's being loaded from)
  if (route.params.subjectId) {
    localProps = { subjectId: route.params.subjectId };
  } else if (route.params.tagKey && route.params.tagFilter) {
    localProps = { tagKey: route.params.tagKey, tagFilter: route.params.tagFilter };
  }

  MetricsService.loadChart(route.params.projectId, 'numUsersPerLevelChartBuilder', localProps)
      .then((response) => {
        // sort by level
        const sorted = response.sort((item) => item.value).reverse();

        isEmpty.value = response.find((item) => item.count > 0) === undefined;
        chartData.value = {
          labels: sorted.map((item) => item.value),
          datasets: [{
            label: 'Number of Users',
            data: sorted.map((item) => item.count),
            backgroundColor: chartSupportColors.getBackgroundColorArray(sorted.length),
            borderColor: chartSupportColors.getBorderColorArray(sorted.length),
            borderWidth: 1,
            borderRadius: 6,
          }]
        }
        isLoading.value = false;
      });
});

const chartJsOptions = ref();
const levelsBarChart = ref(null)
const setChartOptions = () => {
  const colors = chartSupportColors.getColors()
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
        display: false,
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
      datalabels: {
        color: '#36A2EB',
        anchor: 'start',
        align: 'end',
        offset: -5,
        font: {
          size: 14,
        },
        backgroundColor: colors.surface100Color,
        borderColor: colors.surface600Color,
        borderWidth: 1,
        borderRadius: 4,
        padding: 5,
        formatter: function(value, context) {
          const numLevels = context.dataset.data.length
          const level = numLevels - context.dataIndex // highest levels comes first
          return `Level ${level}: ${value}`
        }
      }
    }
  };
}

</script>

<template>
  <Card data-cy="levelsChart" :style="`width: ${layoutSizes.tableMaxWidth}px;`">
    <template #header>
      <SkillsCardHeader :title="title">
        <template #headerContent>
        <chart-download-controls :vue-chart-ref="levelsBarChart" />
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="isLoading" :has-data="!isLoading && !isEmpty" no-data-icon="fa fa-info-circle" no-data-msg="No one reached Level 1 yet...">
        <Chart ref="levelsBarChart"
               id="levelsBarChart"
               type="bar"
               :data="chartData"
               :options="chartJsOptions"
               :plugins="[ChartDataLabels]"
               class="h-[30rem]" />
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>