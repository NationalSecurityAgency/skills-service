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
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import MyBadges from '@/skills-display/components/badges/MyBadges.vue'

const skillsDisplayTheme = useSkillsDisplayThemeState()
const userProgress = useUserProgressSummaryState()
const displayPreferences = useSkillsDisplayPreferencesState()
const attributes = useSkillsDisplayAttributesState()
const log = useLog()

onMounted(() => {
  log.debug('SkillsDisplay.vue: onMounted')
  userProgress.loadUserProgressSummary()
})
const description = computed(() => userProgress.userProgressSummary?.projectDescription)
const showDescription = computed(() =>
  !displayPreferences.isSummaryOnly
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

      <div class="mt-3 flex gap-4 align-items-stretch">
        <div class="flex align-items-center">
          <my-rank />
        </div>
        <div class="flex-1 align-items-center">
          <point-progress-chart />
        </div>
        <div v-if="hasBadges" class="flex">
          <my-badges :num-badges-completed="userProgress.userProgressSummary.badges.numBadgesCompleted"></my-badges>
        </div>
      </div>

      <subject-tiles class="mt-3"/>
    </div>



  </div>
</template>

<style scoped>

</style>