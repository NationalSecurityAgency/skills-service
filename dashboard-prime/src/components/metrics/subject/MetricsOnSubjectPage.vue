<script setup>
import { ref, onMounted } from 'vue';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import LevelBreakdownMetric from "@/components/metrics/common/LevelBreakdownMetric.vue";
import NumUsersPerDay from "@/components/metrics/common/NumUsersPerDay.vue";
import UserTagsByLevelChart from "@/components/metrics/common/UserTagsByLevelChart.vue";

const appConfig = useAppConfig();
const tags = ref([]);

onMounted(() => {
  const localTags = [];
  const userPageTags = appConfig.projectMetricsTagCharts;
  if (userPageTags) {
    const tagSections = JSON.parse(userPageTags);
    tagSections.forEach((section) => {
      localTags.push({
        key: section.key, label: section.tagLabel,
      });
    });
  }
  tags.value = localTags;
})
</script>

<template>
  <div>
    <sub-page-header title="Metrics"/>
    <level-breakdown-metric title="Subject Levels"/>
    <num-users-per-day class="my-3" title="Subject's users per day" role="figure"/>
    <div v-for="tag of tags" :key="tag.key">
      <user-tags-by-level-chart :tag="tag" class="mb-3" />
    </div>
  </div>
</template>

<style scoped></style>
