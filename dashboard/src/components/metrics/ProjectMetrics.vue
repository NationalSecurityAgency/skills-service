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
import NumUsersPerDay from "@/components/metrics/common/NumUsersPerDay.vue";
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import UserTagTable from "@/components/metrics/common/UserTagTable.vue";
import UserTagChart from "@/components/metrics/common/UserTagChart.vue";

const appConfig = useAppConfig();
const tagCharts = ref(null);

onMounted(() => {
  buildTagCharts();
});

const buildTagCharts = () => {
  if (appConfig.projectMetricsTagCharts) {
    const json = appConfig.projectMetricsTagCharts;
    tagCharts.value = JSON.parse(json);
  }
  return [];
}
</script>

<template>
  <div>
    <div class="mb-6 flex">
        <num-users-per-day />
    </div>
    <div v-if="tagCharts"
         class="flex flex-col gap-8"
         data-cy="userTagCharts">
      <div class="" v-for="(tagChart, index) in tagCharts" :key="`${tagChart.key}-${index}`" style="min-width: 30vw;">
        <user-tag-table v-if="tagChart.type === 'table'"
                        :tag-chart="tagChart" />
        <user-tag-chart v-if="tagChart.type !== 'table'"
                        :chart-type="tagChart.type"
                        :tag-key="tagChart.key"
                        :title="tagChart.title" />
      </div>
    </div>
  </div>
</template>

<style scoped></style>
