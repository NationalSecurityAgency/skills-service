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
import MetricsService from "@/components/metrics/MetricsService.js";
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import Chart from "primevue/chart";
import dayjs from "dayjs";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";

const route = useRoute();
const chartSupportColors = useChartSupportColors()

const chartData = ref({})
const chartJsOptions = ref();
const loading = ref(true);
const hasData = ref(false);

onMounted(() => {
  chartJsOptions.value = setChartOptions()
  loadData();
});

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'numUserAchievedOverTimeChartBuilder', {skillId: route.params.skillId})
      .then((dataFromServer) => {
        hasData.value = dataFromServer.achievementCounts && dataFromServer.achievementCounts.length > 0;
        if (hasData.value) {
          const formatTimestamp = (timestamp) => dayjs(timestamp).format('YYYY-MM-DD')

          const achievements = dataFromServer.achievementCounts
          const prevDay = dayjs(achievements[0].timestamp).subtract(1, 'day').toDate()
          achievements.unshift({timestamp: prevDay.getTime(), num: 0 })
          chartData.value = {
            datasets: [{
              label: 'Users',
              data: achievements.map((item) => {
                return {x: formatTimestamp(item.timestamp), y: item.num}
              }),
              cubicInterpolationMode: 'monotone',
            }]
          }
        }
        loading.value = false;
      });
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
        display: false,
      },
    }
  };
}

const skillsAchievedOverTimeChartRef = ref(null)
</script>

<template>
  <Card data-cy="numUsersAchievedOverTimeMetric">
    <template #header>
      <SkillsCardHeader title="Achievements over time">
        <template #headerContent>
          <chart-download-controls v-if="hasData" :vue-chart-ref="skillsAchievedOverTimeChartRef" />
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="No achievements yet for this skill.">
      <Chart ref="skillsAchievedOverTimeChartRef"
             type="line"
             :data="chartData"
             :options="chartJsOptions"
             :class="{ 'h-[16rem]' : !hasData, 'h-[30rem]' : hasData }"
      />
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>