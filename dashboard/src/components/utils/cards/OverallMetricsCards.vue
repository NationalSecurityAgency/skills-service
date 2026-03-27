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
import {computed, ref} from 'vue'
import MediaInfoCard from '@/components/utils/cards/MediaInfoCard.vue'
import Tag from 'primevue/tag'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { usePluralize } from '@/components/utils/misc/UsePluralize.js'
import SkillsButton from "@/components/utils/inputForm/SkillsButton.vue";
import ConfigureIncludedMetricsDialog from "@/components/utils/cards/ConfigureIncludedMetricsDialog.vue";
import {useNumberFormat} from "@/common-components/filter/UseNumberFormat.js";

const props = defineProps({
  data: {
    type: Object,
    required: true
  },
  showProjectCard: {
    type: Boolean,
    default: true
  },
  showBadgeCard: {
    type: Boolean,
    default: true
  }
})
const emits = defineEmits(['on-settings-changed'])

const colors = useColors()
const pluralize = usePluralize()
const nF = useNumberFormat()

const totalQuizzesAndSurveys = computed(() => {
  return props.data ? props.data.numTotalQuizzes + props.data.numTotalSurveys : 0
})

const configureIncludedMetricsType = ref('Project')
const showConfigureIncludedMetricsDialog = ref(false)
const openProjConfDialog = () => {
  configureIncludedMetricsType.value = 'Project'
  showConfigureIncludedMetricsDialog.value = true
}
const openQuizConfDialog = () => {
  configureIncludedMetricsType.value = 'Quiz'
  showConfigureIncludedMetricsDialog.value = true
}

const onSettingsChanged = () => {
  emits('on-settings-changed')
}
</script>

<template>
  <div class="flex flex-col lg:flex-row gap-2 mb-3 flex-wrap" data-cy="overallMetricsCards">
    <media-info-card
        v-if="showProjectCard"
        :title="`${nF.pretty(data.numTotalProjects)} ${pluralize.plural('Project', data.numTotalProjects)}`"
        :icon-class="`fa-solid fa-tasks ${colors.getTextClass(1)}`"
        data-cy="overallMetricsProjectsCard"
        class="flex-1">
      <template #right-of-title>
        <div class="flex justify-end w-full">
          <SkillsButton id="confIncludedProjects"
                        size="small"
                        icon="fa-solid fa-gear"
                        data-cy="confIncludedProjectsBtn"
                        aria-label="Configure Project Exclusion"
                        :track-for-focus="true"
                        @click="openProjConfDialog"/>
        </div>
      </template>
      <template #default>
        <div class="flex flex-col gap-1">
          <div v-if="data.numExcludedProjects > 0" data-cy="numExcludedProjects"><Tag severity="warn">{{ nF.pretty(data.numExcludedProjects) }}</Tag> {{pluralize.plural('Project', data.numExcludedProjects)}} Excluded</div>
          <div data-cy="numSkills"><Tag>{{ nF.pretty(data.numTotalSkills)}}</Tag> {{pluralize.plural('Skill', data.numTotalSkills)}}</div>
        </div>
      </template>
    </media-info-card>
    <media-info-card
        :title="`${nF.pretty(totalQuizzesAndSurveys)} ${pluralize.plural('Assessment', totalQuizzesAndSurveys)}`"
        :icon-class="`fa-solid fa-spell-check ${colors.getTextClass(2)}`"
        data-cy="overallMetricsQuizzesAndSurveysCard"
        class="flex-1">
      <template #right-of-title>
        <div class="flex justify-end w-full">
          <SkillsButton id="confQuizExclusion"
                        size="small"
                        icon="fa-solid fa-gear"
                        data-cy="confQuizExclusionBtn"
                        aria-label="Configure Quiz Exclusion"
                        :track-for-focus="true"
                        @click="openQuizConfDialog" />
        </div>
      </template>
      <template #default>
        <div class="flex flex-col gap-1">
          <div v-if="data.numExcludedQuizzesAndSurveys > 0" data-cy="numExcludedAssessments"><Tag severity="warn">{{ nF.pretty(data.numExcludedQuizzesAndSurveys) }}</Tag> {{pluralize.plural('Assessment', data.numExcludedQuizzesAndSurveys)}} Excluded</div>
          <div><Tag>{{ nF.pretty(data.numTotalQuizzes)}}</Tag> {{pluralize.plural('Quiz', data.numTotalQuizzes)}} and  <Tag>{{ nF.pretty(data.numTotalSurveys)}}</Tag> {{ pluralize.plural('Survey', data.numTotalSurveys)}}</div>
        </div>
      </template>
    </media-info-card>
    <media-info-card
        v-if="showBadgeCard"
        :title="`${nF.pretty(data.numTotalBadges)} ${pluralize.plural('Badge', data.numTotalBadges)}`"
        :icon-class="`fa-solid fa-award ${colors.getTextClass(4)}`"
        data-cy="overallMetricsBadgesCard"
        class="flex-1">
      <Tag>{{ nF.pretty(data.numTotalProjectBadges)}}</Tag> Project
      and <Tag>{{ nF.pretty(data.numTotalGlobalBadges)}}</Tag> Global
    </media-info-card>

    <configure-included-metrics-dialog
        v-if="showConfigureIncludedMetricsDialog"
        v-model="showConfigureIncludedMetricsDialog"
        :type="configureIncludedMetricsType"
        @on-saved="onSettingsChanged"/>
  </div>
</template>

<style scoped></style>
