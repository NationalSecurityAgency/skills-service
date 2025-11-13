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
import {useRoute} from 'vue-router'
import MetricsService from '@/components/metrics/MetricsService.js'
import MetricsOverlay from '@/components/metrics/utils/MetricsOverlay.vue'
import Chart from "primevue/chart";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";

const route = useRoute();
const chartSupportColors = useChartSupportColors()

const loading = ref(true);
const isEmpty = ref(true);
const chartJsOptions = ref(null)
const chartData = ref({})

onMounted(() => {
  chartJsOptions.value = setChartOptions()
  MetricsService.loadChart(route.params.projectId, 'binnedUsagePostAchievementMetricsBuilder', { skillId: route.params.skillId })
    .then((res) => {
      updateChart(res);
      loading.value = false;
    });
});

const updateChart = (res) => {
  if (res && res.length > 0) {
    isEmpty.value = false;
    chartData.value = {
      labels: res.map((labeledCount) => labeledCount.label),
      datasets: [{
        label: "# of users",
        data: res.map((item) => item.count),
        backgroundColor: chartSupportColors.getBackgroundColorArray(res.length),
        borderColor: chartSupportColors.getBorderColorArray(res.length),
        borderWidth: 1,
        borderRadius: 6,
        minBarLength: 4,
      }]
    }
  }
};

const setChartOptions = () => {
  const colors = chartSupportColors.getColors()
  return {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        ticks: {
          color: colors.textMutedColor,
        },
        grid: {
          color: colors.contentBorderColor,
        }
      },
      y: {
        beginAtZero: true,
        ticks: {
          color: colors.textMutedColor,
          stepSize: 1,
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
const binnedNumUsersPostAchievementChartRef = ref(null)
</script>

<template>
  <Card data-cy="binnedNumUsersPostAchievement">
    <template #header>
      <SkillsCardHeader title="Usage" title-tag="h4">
        <template #headerContent>
          <chart-download-controls v-if="!isEmpty" :vue-chart-ref="binnedNumUsersPostAchievementChartRef" />
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <MetricsOverlay :loading="loading" :has-data="!isEmpty" no-data-msg="No achievements yet for this skill.">
        <Chart ref="binnedNumUsersPostAchievementChartRef"
               type="bar"
               :data="chartData"
               :options="chartJsOptions"
               class="min-h-[16em]"/>
      </MetricsOverlay>
      <div class="font-light text-sm">Number of times this Skill is performed per user after having fully achieved it.</div>
    </template>
  </Card>
</template>

<style scoped>

</style>