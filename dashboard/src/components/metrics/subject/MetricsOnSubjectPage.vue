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
    <num-users-per-day class="my-4" title="Subject's users per day" role="figure"/>
    <div v-for="tag of tags" :key="tag.key">
      <user-tags-by-level-chart :tag="tag" class="mb-4" />
    </div>
  </div>
</template>

<style scoped></style>
