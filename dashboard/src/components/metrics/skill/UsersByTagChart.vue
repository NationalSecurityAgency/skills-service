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
import Chart from "primevue/chart";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";

const route = useRoute();
const props = defineProps(['tag']);

const chartSupportColors = useChartSupportColors()
const layoutSizes = useLayoutSizesState()

const chartJsOptions = ref();
const chartData = ref({})
const hasData = computed(() => Boolean(chartData.value.labels?.length > 0))
const loading = ref(true);

onMounted(() => {
  chartJsOptions.value = setChartOptions()
  loadData();
});

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'skillAchievementsByTagBuilder', { skillId: route.params.skillId, userTagKey: props.tag.key })
      .then((dataFromServer) => {
        const keysFromServer = Object.keys(dataFromServer);
        const inProgressData = [];
        const achievedData = [];

        keysFromServer.forEach((label) => {
          inProgressData.push({ x: label, y: dataFromServer[label].numberInProgress });
          achievedData.push({ x: label, y: dataFromServer[label].numberAchieved });
        });

        const totalInProgressData = inProgressData.map((value) => value.y).filter((value) => value > 0);
        const totalAchievedData = achievedData.map((value) => value.y).filter((value) => value > 0);

        const convertToChartData = (data, label, colorIndex) => {
          return {
            label: label,
            data: data.map((item) => item.y),
            backgroundColor: chartSupportColors.getTranslucentColor(colorIndex),
            borderColor: chartSupportColors.getSolidColor(colorIndex),
            borderWidth: 1,
            borderRadius: 6,
            maxBarThickness: 15,
            minBarLength: 4,
          }
        }

        const datasets = []
        if (inProgressData.length > 0 && totalInProgressData.length > 0) {
          datasets.push(convertToChartData(inProgressData, 'In Progress', 0))
        }

        if (achievedData.length > 0 && totalAchievedData.length > 0) {
          datasets.push(convertToChartData(achievedData, 'Achieved', 1))
        }

        chartData.value = { labels: keysFromServer, datasets}

        loading.value = false;
      });
};

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
        position: 'top',
        labels: {
          color: colors.textColor,
          padding: 20,
          boxWidth: 12,
          usePointStyle: true,
          pointStyle: 'circle'
        }
      },
    }
  };
}

const topUserCountsChartRef = ref(null)
</script>

<template>
  <Card data-cy="numUsersByTag" :style="`max-width: ${layoutSizes.tableMaxWidth}px;`">
    <template #header>
      <SkillsCardHeader :title="`Top 20 ${tag.label} User Counts`">
        <template #headerContent>
          <chart-download-controls v-if="hasData" :vue-chart-ref="topUserCountsChartRef" />
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="No users currently working on this skill.">
        <Chart ref="topUserCountsChartRef"
               type="bar"
               :data="chartData"
               :options="chartJsOptions"
               class="min-h-[16em]"/>
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>