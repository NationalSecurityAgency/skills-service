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

const route = useRoute();
const props = defineProps(['tag']);

const chartSupportColors = useChartSupportColors()

const chartJsOptions = ref();
const inProgressChartData = ref({})
const achievedChartData = ref({})
const hasInProgressData = computed(() => inProgressChartData.value.labels?.length > 0)
const hasAchievedData = computed(() => achievedChartData.value.labels?.length > 0)
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

        const convertToChartData = (data, label) => {
          return {
            labels: data.map((item) => item.x),
            datasets: [{
              label: label,
              data: data.map((item) => item.y),
              backgroundColor: chartSupportColors.getBackgroundColorArray(data.length),
              borderColor: chartSupportColors.getBorderColorArray(data.length),
              borderWidth: 1,
              borderRadius: 6,
              maxBarThickness: 15,
              minBarLength: 4,
            }]
          }
        }

        if (inProgressData.length > 0 && totalInProgressData.length > 0) {
          inProgressChartData.value = convertToChartData(inProgressData, 'In Progress')
        }

        if (achievedData.length > 0 && totalAchievedData.length > 0) {
          achievedChartData.value = convertToChartData(achievedData, 'Achieved')
        }

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
        display: false
      },
    }
  };
}

const topUserCountsInProgressChart = ref(null)
const topUserCountsAchievedChart = ref(null)
</script>

<template>
  <Card data-cy="numUsersByTag">
    <template #header>
      <SkillsCardHeader :title="`Top 20 ${tag.label} User Counts`"></SkillsCardHeader>
    </template>
    <template #content>
      <div class="flex flex-col xl:flex-row gap-6">
        <div class="flex flex-1">
          <Card data-cy="usersInProgressByTag" class="w-full">
            <template #header>
              <SkillsCardHeader title="In Progress" title-tag="h4">
                <template #headerContent>
                  <chart-download-controls v-if="hasInProgressData" :vue-chart-ref="topUserCountsInProgressChart" />
                </template>
              </SkillsCardHeader>
            </template>
            <template #content>
              <metrics-overlay :loading="loading" :has-data="hasInProgressData" no-data-msg="No users currently working on this skill.">
                <Chart ref="topUserCountsInProgressChart"
                       type="bar"
                       :data="inProgressChartData"
                       :options="chartJsOptions"
                       class="min-h-[16em]"/>
              </metrics-overlay>
            </template>
          </Card>
        </div>
        <div class="flex flex-1">
          <Card data-cy="usersAchievedByTag" class="w-full">
            <template #header>
              <SkillsCardHeader title="Achieved" title-tag="h4">
                <template #headerContent>
                  <chart-download-controls v-if="hasAchievedData" :vue-chart-ref="topUserCountsAchievedChart" />
                </template>
              </SkillsCardHeader>
            </template>
            <template #content>
              <metrics-overlay :loading="loading" :has-data="hasAchievedData" no-data-msg="No achievements yet for this skill.">
                <Chart ref="topUserCountsAchievedChart"
                       type="bar"
                       :data="achievedChartData"
                       :options="chartJsOptions"
                       class="min-h-[16em]"/>
              </metrics-overlay>
            </template>
          </Card>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>