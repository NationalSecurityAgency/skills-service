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
import { computed } from 'vue'
import MediaInfoCard from '@/components/utils/cards/MediaInfoCard.vue'
import Tag from 'primevue/tag'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { usePluralize } from '@/components/utils/misc/UsePluralize.js'

const props = defineProps({
  data: {
    type: Object,
    required: true
  }
})

const colors = useColors()
const pluralize = usePluralize()

const totalQuizzesAndSurveys = computed(() => {
  return props.data ? props.data.numTotalQuizzes + props.data.numTotalSurveys : 0
})
</script>

<template>
  <div class="flex flex-col lg:flex-row gap-2 mb-3 flex-wrap">
    <media-info-card
        :title="`${data.numTotalProjects} ${pluralize.plural('Project', data.numTotalProjects)}`"
        :icon-class="`fa-solid fa-tasks ${colors.getTextClass(1)}`"
        data-cy="overallMetricsProjectsCard"
        class="flex-1">
      <Tag>{{ data.numTotalSkills}}</Tag> {{pluralize.plural('Skill', data.numTotalSkills)}}
    </media-info-card>
    <media-info-card
        :title="`${totalQuizzesAndSurveys} ${pluralize.plural('Quiz', totalQuizzesAndSurveys)}/${pluralize.plural('Survey', totalQuizzesAndSurveys)}`"
        :icon-class="`fa-solid fa-spell-check ${colors.getTextClass(2)}`"
        data-cy="overallMetricsQuizzesAndSurveysCard"
        class="flex-1">
      <Tag>{{ data.numTotalQuizzes}}</Tag> {{pluralize.plural('Quiz', data.numTotalQuizzes)}} and  <Tag>{{ data.numTotalSurveys}}</Tag> {{ pluralize.plural('Survey', data.numTotalSurveys)}}
    </media-info-card>
    <media-info-card
        :title="`${data.numTotalBadges} ${pluralize.plural('Badge', data.numTotalBadges)}`"
        :icon-class="`fa-solid fa-award ${colors.getTextClass(4)}`"
        data-cy="overallMetricsBadgesCard"
        class="flex-1">
      <Tag>{{ data.numTotalGlobalBadges}}</Tag> Global {{ pluralize.plural('Badge', data.numTotalGlobalBadges) }}
    </media-info-card>
  </div>
</template>

<style scoped></style>
