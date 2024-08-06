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
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import LevelBreakdownMetric from "@/components/metrics/common/LevelBreakdownMetric.vue";
import UsersTableMetric from "@/components/metrics/userTags/UsersTableMetric.vue";

const route = useRoute();
const appConfig = useAppConfig();

onMounted(() => {
  buildTagCharts();
});

const tagCharts = ref(null);

const metricLabel = computed(() => {
  const chartInfo = tagCharts.value?.find((i) => i.key === route.params.tagKey);
  return chartInfo ? `${chartInfo.tagLabel}:` : '';
});

const metricValue = computed(() => {
  return route.params.tagFilter;
});

const metricTitle = computed(() => {
  return `${metricLabel.value} ${metricValue.value}`;
});

const buildTagCharts = () => {
  if (appConfig && appConfig.projectMetricsTagCharts) {
    const json = appConfig.projectMetricsTagCharts;
    const charts = JSON.parse(json);
    tagCharts.value = charts;
  }
  return [];
};
</script>

<template>
  <div>
    <div class="flex mb-3">
      <level-breakdown-metric :title="`Overall Levels for ${metricTitle}`" class="flex-1"/>
    </div>
    <users-table-metric :title="`Users for ${metricTitle}`" />
  </div>
</template>

<style scoped></style>
