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
import { computed, onMounted } from 'vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import UserOverallProgress from '@/skills-display/components/home/UserOverallProgress.vue'
import { useUserProgressSummaryState } from '@/skills-display/stores/UseUserProgressSummaryState.js'
import { useLog } from '@/components/utils/misc/useLog.js'
import MyRank from '@/skills-display/components/rank/MyRank.vue'
import PointProgressChart from '@/skills-display/components/progress/points/PointProgressChart.vue'
import SubjectTiles from '@/skills-display/components/subjects/SubjectTiles.vue'
import ProjectDescription from '@/skills-display/components/home/ProjectDescription.vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import MyBadges from '@/skills-display/components/badges/MyBadges.vue'
import TranscriptCard from '@/skills-display/components/userTranscript/TranscriptCard.vue'

const skillsDisplayTheme = useSkillsDisplayThemeState()
const userProgress = useUserProgressSummaryState()
const attributes = useSkillsDisplayAttributesState()
const log = useLog()

onMounted(() => {
  if (log.isDebugEnabled()) {
    log.debug(`SkillsDisplay.vue: onMounted for projectId=[${attributes.projectId}]`)
  }
  attributes.afterPropsAreSet().then(() => {
    userProgress.loadUserProgressSummary()
  })
})
const description = computed(() => userProgress.userProgressSummary?.projectDescription)
const showDescription = computed(() =>
  !attributes.isSummaryOnly
  && description.value
  && attributes.displayProjectDescription
)
const hasBadges = computed(() => {
  return userProgress.userProgressSummary && userProgress.userProgressSummary.badges && userProgress.userProgressSummary.badges.enabled;
})
</script>

<template>
  <div>
    <skills-spinner :is-loading="userProgress.loadingUserProgressSummary"/>
    <div v-if="!userProgress.loadingUserProgressSummary">
      <skills-title :back-button="false">{{ skillsDisplayTheme.landingPageTitle }}</skills-title>
      <project-description v-if="showDescription" :description="description" class="mt-3"/>
      <user-overall-progress class="mt-3"/>

      <div class="mt-3 flex flex-column lg:flex-row gap-3 align-items-stretch">
        <div class=" align-items-center">
          <my-rank />
        </div>
        <div class="flex-1 align-items-center">
          <point-progress-chart />
        </div>
        <div v-if="hasBadges" class="">
          <my-badges :num-badges-completed="userProgress.userProgressSummary.badges.numBadgesCompleted"></my-badges>
        </div>
      </div>

      <subject-tiles class="mt-3"/>

      <transcript-card
        v-if="userProgress.userProgressSummary && userProgress.userProgressSummary.totalSkills > 0"
        class="mt-2" />
    </div>



  </div>
</template>

<style scoped>

</style>