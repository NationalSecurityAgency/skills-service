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
import MetricsService from "@/components/metrics/MetricsService.js";
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import StatsCard from "@/components/metrics/utils/StatsCard.vue";
import PostAchievementUsersPieChart from "@/components/metrics/skill/PostAchievementUsersPieChart.vue";
import BinnedPostAchievementUsage from "@/components/metrics/skill/BinnedPostAchievementUsage.vue";
import PostAchievementUsersTable from "@/components/metrics/skill/PostAchievementUsersTable.vue";
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import SkillEventsOverTime from "@/components/metrics/skill/SkillEventsOverTime.vue";
import SkillAchievedByUsersOverTime from "@/components/metrics/skill/SkillAchievedByUsersOverTime.vue";
import UsersByTagChart from "@/components/metrics/skill/UsersByTagChart.vue";
import NumberFormatter from '@/components/utils/NumberFormatter.js'

const appConfig = useAppConfig();
const route = useRoute();
const timeUtils = useTimeUtils();

const loading = ref(true);
const numUsersAchieved = ref(0);
const numUsersInProgress = ref(0);
const lastAchieved = ref(0);
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
  MetricsService.loadChart(route.params.projectId, 'singleSkillCountsChartBuilder', { skillId: route.params.skillId })
      .then((dataFromServer) => {
        numUsersAchieved.value = dataFromServer.numUsersAchieved;
        lastAchieved.value = dataFromServer.lastAchieved;
        numUsersInProgress.value = dataFromServer.numUsersInProgress;
        loading.value = false;
      });
})
</script>

<template>
  <div>
    <sub-page-header title="Metrics"/>
    <skills-spinner :is-loading="loading" />

    <!-- on FF charts end up pushing column to the next row; this is a workaround -->
    <div v-if="!loading" style="width: 99%;">
      <div class="flex mb-3 gap-4">
        <stats-card class="flex flex-1" title="Achieved" :statNum="numUsersAchieved" icon="fa fa-trophy text-info" data-cy="numUserAchievedStatCard">
          Number of users that achieved this skill
        </stats-card>
        <stats-card class="flex flex-1" title="In Progress" :statNum="numUsersInProgress" icon="fa fa-running text-primary" data-cy="inProgressStatCard">
          Number of Users with some points earned toward the skill
        </stats-card>
        <stats-card class="flex flex-1" title="Last Achieved" :statNum="lastAchieved" :calculate-time-from-now="true"
                    icon="fa fa-clock text-warning"  data-cy="lastAchievedStatCard">
          <span v-if="lastAchieved">This skill was last achieved on <span class="text-success">{{ timeUtils.formatDate(lastAchieved) }}</span></span>
          <span v-else>This skill was <span class="text-info">never</span> achieved.</span>
        </stats-card>
      </div>

      <skill-achieved-by-users-over-time class="mb-3"/>
      <skill-events-over-time class="mb-3"/>
      <div v-for="tag of tags" :key="tag.key">
        <users-by-tag-chart :tag="tag" class="mb-3" />
      </div>

      <Card data-cy="postAchievementContainers">
        <template #header>
          <SkillsCardHeader title="Post Achievement Metrics"></SkillsCardHeader>
        </template>
        <template #content>
          <div class="flex gap-4">
            <div class="flex mb-3">
              <post-achievement-users-pie-chart class="h-100"/>
            </div>
            <div class="flex flex-1 mb-3">
              <binned-post-achievement-usage class="h-100 w-full"/>
            </div>
          </div>
          <div class="flex">
            <div class="w-full mb-3">
              <post-achievement-users-table class="h-100" />
            </div>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped></style>
