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
import MetricsService from "@/components/metrics/MetricsService.js";
import {useRoute} from 'vue-router';
import Chart from "primevue/chart";
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";

const props = defineProps(['tag']);
const route = useRoute();

const layoutSizes = useLayoutSizesState()
const chartSupportColors = useChartSupportColors()

const loading = ref(true);
const chartJsOptions = ref();
const chartData = ref({})

onMounted(() => {
  chartJsOptions.value = setChartOptions()
  loadData();
})

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'achievementsByTagPerLevelMetricsBuilder', { subjectId: route.params.subjectId, userTagKey: props.tag.key })
      .then((dataFromServer) => {
        if (dataFromServer && Object.keys(dataFromServer.data).length > 0) {
          const userData = dataFromServer.data;
          const tags = Object.keys(userData);

          if (tags) {
            const categories = userData.map((a) => a.tag);
            const numberOfLevels = dataFromServer.totalLevels;

            const datasets = []
            const convertToChartData = (data, label, colorIndex) => {
              return {
                label: label,
                data: data,
                backgroundColor: chartSupportColors.getTranslucentColor(colorIndex),
                borderColor: chartSupportColors.getSolidColor(colorIndex),
                borderWidth: 1,
                borderRadius: 6,
                minBarLength: 4,
              }
            }

            for (let level = 1; level <= numberOfLevels; level += 1) {
              const dataForLevel = [];
              tags.forEach((tag) => {
                if (userData[tag].value[level] > 0) {
                  dataForLevel.push(userData[tag].value[level]);
                } else {
                  dataForLevel.push(0);
                }
              });
              if (dataForLevel.length > 0) {
                datasets.push(convertToChartData(dataForLevel, `Level ${level}`, level-1));
              }
            }
            chartData.value = {
              labels: categories,
              datasets: datasets,
            }
          }
        }
        loading.value = false;
      });
};

const userTagsByLevelChartRef = ref(null)
const hasData = computed(() => chartData.value.labels && chartData.value.labels?.length > 0)

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
</script>

<template>
  <Card :data-cy="`numUsersByTag-${tag.key}`" :style="`width: ${layoutSizes.tableMaxWidth}px;`">
    <template #header>
      <SkillsCardHeader :title="`Top 20 ${tag.label} Level Breakdown`">
        <template #headerContent>
          <chart-download-controls v-if="hasData" :vue-chart-ref="userTagsByLevelChartRef" />
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="No users currently">
        <Chart ref="userTagsByLevelChartRef"
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