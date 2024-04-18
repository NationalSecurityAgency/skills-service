<script setup>
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import UserOverallProgress from '@/skills-display/components/home/UserOverallProgress.vue'
import { useUserProgressSummaryState } from '@/skills-display/stores/UseUserProgressSummaryState.js'
import { onMounted } from 'vue'
import { useLog } from '@/components/utils/misc/useLog.js'
import MyRank from '@/skills-display/components/rank/MyRank.vue'
import PointProgressChart from '@/skills-display/components/progress/points/PointProgressChart.vue'
import SubjectTiles from '@/skills-display/components/subjects/SubjectTiles.vue'

const skillsDisplayTheme = useSkillsDisplayThemeState()
const userProgress = useUserProgressSummaryState()
const log = useLog()

onMounted(() => {
  log.debug('SkillsDisplay.vue: onMounted')
  userProgress.loadUserProgressSummary()
})
</script>

<template>
  <div>
    <skills-spinner :is-loading="userProgress.loadingUserProgressSummary"/>
    <div v-if="!userProgress.loadingUserProgressSummary">
      <skills-title :back-button="false">{{ skillsDisplayTheme.landingPageTitle }}</skills-title>
  <!--    <project-description v-if="!isSummaryOnly && description && displayProjectDescription" :description="description"></project-description>-->
  <!--    <user-skills-header :display-data="displayData" class="mb-3"/>-->
      <user-overall-progress class="mt-3"/>

      <div class="mt-3 flex gap-4 align-items-stretch">
        <div class="flex align-items-center">
          <my-rank />
        </div>
        <div class="flex-1 align-items-center">
          <point-progress-chart />
        </div>
      </div>

      <subject-tiles class="mt-3"/>
    </div>



  </div>
</template>

<style scoped>

</style>