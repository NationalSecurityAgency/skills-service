<script setup>
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import UserOverallProgress from '@/skills-display/components/home/UserOverallProgress.vue'
import { useUserProgressSummaryState } from '@/skills-display/stores/UseUserProgressSummaryState.js'
import { onMounted } from 'vue'
import { useLog } from '@/components/utils/misc/useLog.js'

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
  <!--    <subjects-container v-if="!isSummaryOnly" :subjects="displayData.userSkills.subjects" />-->
    </div>

  </div>
</template>

<style scoped>

</style>