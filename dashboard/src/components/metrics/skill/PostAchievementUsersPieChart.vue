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
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import MetricsService from "@/components/metrics/MetricsService.js";
import Chart from "primevue/chart";
import ChartDownloadControls from "@/components/metrics/common/ChartDownloadControls.vue";
import {useChartSupportColors} from "@/components/metrics/common/UseChartSupportColors.js";

const props = defineProps(['skillName']);
const route = useRoute();
const chartSupportColors = useChartSupportColors()

const chartJsOptions = ref();
const chartData = ref({})
const loading = ref(true);
const hasData = ref(false);


onMounted(() => {
  chartJsOptions.value = setChartOptions()
  loadData();
});

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'usagePostAchievementMetricsBuilder', { skillId: route.params.skillId })
      .then((dataFromServer) => {
        // need to check if the properties are defined so that 0 doesn't return false
        if (Object.prototype.hasOwnProperty.call(dataFromServer, 'totalUsersAchieved')
            && Object.prototype.hasOwnProperty.call(dataFromServer, 'usersPostAchievement')) {
          const usersWhoStoppedAfterAchieving = dataFromServer.totalUsersAchieved - dataFromServer.usersPostAchievement;
          hasData.value = true;
          chartData.value = {
            labels: ['stopped after achieving', 'performed Skill at least once after achieving'],
            datasets: [{
              label: 'Number of Users',
              data: [usersWhoStoppedAfterAchieving, dataFromServer.usersPostAchievement],
            }]
          }
        }
        loading.value = false;
      });
};

const setChartOptions = () => {
  const colors = chartSupportColors.getColors()
  return {
    responsive: true,
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
  }
}
const postAchievementUserCountsChartRef = ref(null)
</script>

<template>
  <Card data-cy="numUsersPostAchievement">
    <template #header>
      <SkillsCardHeader title="User Counts" title-tag="h4">
        <template #headerContent>
          <chart-download-controls v-if="hasData" :vue-chart-ref="postAchievementUserCountsChartRef" />
        </template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="No achievements yet for this skill." class="flex items-center justify-center">
        <div class="w-full max-w-[20rem] h-full max-h-[20rem]">
          <Chart ref="postAchievementUserCountsChartRef"
                 type="pie"
                 :data="chartData"
                 :options="chartJsOptions"
                 class="w-full h-full"/>
        </div>
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>