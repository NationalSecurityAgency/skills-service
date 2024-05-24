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
      <div class="col-12">
        <level-breakdown-metric :title="`Overall Levels for ${metricTitle}`" />
      </div>
    </div>
    <users-table-metric :title="`Users for ${metricTitle}`" />
  </div>
</template>

<style scoped></style>
