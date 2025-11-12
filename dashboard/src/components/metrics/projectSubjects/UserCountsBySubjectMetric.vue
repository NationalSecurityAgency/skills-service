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
import {computed, onMounted, ref} from 'vue'
import {useRoute} from 'vue-router';
import MetricsService from "@/components/metrics/MetricsService.js";
import MetricsOverlay from '@/components/metrics/utils/MetricsOverlay.vue'
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";
import Chart from "primevue/chart";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";

const route = useRoute();
const layoutSizes = useLayoutSizesState()
const chartSupportColors = useChartSupportColors()

onMounted(() => {
  chartJsOptions.value = setChartOptions()
  MetricsService.loadChart(route.params.projectId, 'numUsersPerSubjectPerLevelChartBuilder')
      .then((res) => {
        const maxNumLevels = Math.max(...res.map((item) => item.numUsersPerLevels.length))
        const levelsDatasets = []
        const backgroundColorsPerLevel = chartSupportColors.getBackgroundColorArray(maxNumLevels)
        const borderColorsPerLevel = chartSupportColors.getBorderColorArray(maxNumLevels)

        let hasData = false
        for (let level = 1; level <= maxNumLevels; level++) {
          const data = res.map((item) => item.numUsersPerLevels.find((item) => item.level === level)?.numberUsers || 0)
          if (data.some((item) => item > 0)) {
            hasData = true
          }
          levelsDatasets.push({
            label: `Level ${level}`,
            data,
            backgroundColor: backgroundColorsPerLevel[level - 1],
            borderColor: borderColorsPerLevel[level - 1],
            borderWidth: 1,
            borderRadius: 6,
            minBarLength: 4,
          })
        }
        if (hasData) {
          chartData.value = {
            labels: res.map((item) => item.subject),
            datasets: levelsDatasets
          }
        }

        loading.value = false;
      });
});

const loading = ref(true);
const chartData = ref({})

const hasData = computed(() => !loading.value && chartData.value?.datasets?.length > 0)

const userPerSubjectChart = ref(null)
const chartJsOptions = ref();
const setChartOptions = () => {
  const colors = chartSupportColors.getColors()
  return {
    responsive: true,
    maintainAspectRatio: false,
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
          color: colors.textMutedColor,
        },
        grid: {
          color: colors.contentBorderColor,
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
        },
      },
    }
  };
}
</script>

<template>
  <Card data-cy="userCountsBySubjectMetric" :style="`width: ${layoutSizes.tableMaxWidth}px;`">
    <template #header>
      <SkillsCardHeader title="Number of users for each level for each subject">
        <template #headerContent>
          <chart-download-controls :vue-chart-ref="userPerSubjectChart" />
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="Users have not achieved any levels, yet...">
        <Chart ref="userPerSubjectChart"
               id="userPerSubjectChart"
               type="bar"
               :data="chartData"
               :options="chartJsOptions"
               class="h-[30rem]" />
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>