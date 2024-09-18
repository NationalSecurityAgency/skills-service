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
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import MetricsOverlay from "@/components/metrics/utils/MetricsOverlay.vue";
import MetricsService from "@/components/metrics/MetricsService.js";

const props = defineProps(['skillName']);
const route = useRoute();

const series = ref([]);
const chartOptions = ref({
  chart: {
    height: 250,
    width: 250,
    type: 'pie',
    toolbar: {
      show: true,
      offsetX: 0,
      offsetY: -40,
    },
  },
  colors: ['#17a2b8', '#28a745'],
  labels: ['stopped after achieving', 'performed Skill at least once after achieving'],
  dataLabels: {
    enabled: false,
  },
  legend: {
    position: 'top',
    horizontalAlign: 'left',
  },
});
const loading = ref(true);
const hasData = ref(false);

onMounted(() => {
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
          series.value = [usersWhoStoppedAfterAchieving, dataFromServer.usersPostAchievement];
        }
        loading.value = false;
      });
};
</script>

<template>
  <Card data-cy="numUsersPostAchievement">
    <template #header>
      <SkillsCardHeader title="User Counts"></SkillsCardHeader>
    </template>
    <template #content>
      <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="No achievements yet for this skill.">
        <apexchart type="pie" height="350" :options="chartOptions" :series="series" class="mt-3"></apexchart>
      </metrics-overlay>
    </template>
  </Card>
</template>

<style scoped>

</style>