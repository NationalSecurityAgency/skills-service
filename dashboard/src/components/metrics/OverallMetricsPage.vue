/*
Copyright 2026 SkillTree

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
import { computed, onMounted, ref } from 'vue'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import NoContent2 from '@/components/utils/NoContent2.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import MetricsService from '@/components/metrics/MetricsService.js'
import OverallMetricsCards from '@/components/utils/cards/OverallMetricsCards.vue'
import NumUsersPerDay from '@/components/metrics/common/NumUsersPerDay.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import UserTagTable from '@/components/metrics/common/UserTagTable.vue'
import UserTagChart from '@/components/metrics/common/UserTagChart.vue'

const appConfig = useAppConfig();
const tagCharts = ref(null);

onMounted(() => {
  loadData()
  buildTagCharts();
})
const buildTagCharts = () => {
  if (appConfig.projectMetricsTagCharts) {
    const json = appConfig.overallMetricsTagCharts;
    tagCharts.value = JSON.parse(json);
  }
  return [];
}

const isLoading = ref(true)
const metricsData = ref({})
const hasData = computed(() => (metricsData.value?.numTotalProjects + metricsData.value?.numTotalQuizzes + metricsData.value?.numTotalSurveys) > 0)
const projectIds = computed(()=> {
  if (hasData.value) {
    return metricsData.value.projectInfo.map(project => project.projectId);
  } else {
    return []
  }
})
const quizIds = computed(()=> {
  if (hasData.value) {
    return metricsData.value.quizInfo.map(quiz => quiz.quizId);
  } else {
    return []
  }
})

const loadData = () => {
  MetricsService.getOverallMetrics()
    .then((response) => {
      metricsData.value = response
    })
    .finally(() => {
      isLoading.value = false
    })
}
</script>

<template>
  <div>
    <SubPageHeader title="Overall Metrics" :title-level="1"> </SubPageHeader>
    <skills-spinner v-if="isLoading" :is-loading="isLoading" class="mt-6" />
    <div v-if="!isLoading">
      <OverallMetricsCards :data="metricsData" />
      <div class="flex">
        <NumUsersPerDay :title="'Overall Users per day'" :project-ids="projectIds" :quiz-ids="quizIds"/>
      </div>
      <div v-if="tagCharts"
           class="flex flex-col gap-4 mt-4"
           data-cy="userTagCharts">
        <div v-for="(tagChart, index) in tagCharts" :key="`${tagChart.key}-${index}`" style="min-width: 30vw;">
          <user-tag-table v-if="tagChart.type === 'table'"
                          :tag-chart="tagChart" :project-ids="projectIds" :quiz-ids="quizIds" />
          <user-tag-chart v-if="tagChart.type !== 'table'"
                          :project-ids="projectIds" :quiz-ids="quizIds"
                          :chart-type="tagChart.type"
                          :tag-key="tagChart.key"
                          :title="tagChart.title" />
        </div>
      </div>
      <no-content2
        v-if="!hasData"
        class="mt-6"
        title="No Metrics Data Available"
        message="Cross-project metrics and analytics data will display here once there is sufficient activity across your administered projects, quizzes and surveys"></no-content2>
    </div>
  </div>
</template>

<style scoped></style>
