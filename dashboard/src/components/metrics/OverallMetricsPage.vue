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
import MediaInfoCard from '@/components/utils/cards/MediaInfoCard.vue'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'

const colors = useColors()

onMounted(() => {
  loadData()
})

const metricsData = ref({})
const hasData = computed(() => metricsData.value?.totalMetrics > 0)
const isLoading = ref(true)

const loadData = () => {
  // Placeholder for future metrics data loading
  isLoading.value = false
  metricsData.value = {
    totalMetrics: 0,
    numTotalProjects: 0,
    numTotalQuizzes: 0,
    numTotalSurveys: 0
  }
}

</script>

<template>
  <div>
    <SubPageHeader title="Overall Metrics" :title-level="1"> </SubPageHeader>
    <skills-spinner v-if="isLoading" :is-loading="isLoading" class="mt-6" />
    <div v-if="!isLoading">
      <div class="flex gap-2 mb-3">
        <media-info-card
          :title="`${metricsData.numTotalProjects} Projects`"
          :icon-class="`fa-solid fa-tasks ${colors.getTextClass(1)}`"
          class="flex-1"/>
        <media-info-card
          :title="`${metricsData.numTotalQuizzes} Quizzes`"
          :icon-class="`fa-solid fa-spell-check ${colors.getTextClass(2)}`"
          class="flex-1"/>
        <media-info-card
          :title="`${metricsData.numTotalSurveys} Surveys`"
          :icon-class="`fa-solid fa-clipboard-list ${colors.getTextClass(3)}`"
          class="flex-1"/>
      </div>
      <Card v-if="hasData" :pt="{ body: { class: 'p-0!' } }">
        <template #content>
          <div class="p-5">Metrics dashboard content will go here</div>
        </template>
      </Card>
      <no-content2
        v-if="!hasData"
        class="mt-6"
        title="No Metrics Data Available"
        message="Cross-project metrics and analytics data will display here once there is sufficient activity across your administered projects, quizzes and surveys"></no-content2>
    </div>
  </div>
</template>

<style scoped></style>
