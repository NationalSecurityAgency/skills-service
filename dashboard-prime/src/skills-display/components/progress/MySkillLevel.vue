<script setup>
import { computed } from 'vue'
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import { useUserProgressSummaryState } from '@/skills-display/stores/UseUserProgressSummaryState.js'

const userProgress = useUserProgressSummaryState()
const skillsDisplayPreferences = useSkillsDisplayPreferencesState()

const level = computed(() => userProgress.userProgressSummary.skillsLevel)
const totalLevels = computed(() => userProgress.userProgressSummary.totalLevels)
</script>

<template>
  <div class="progress-circle-wrapper">
    <label class="text-2xl">My {{ skillsDisplayPreferences.levelDisplayName }}</label>
    <div class="mt-4">
      <div class="fa-stack skills-icon trophy-stack">
        <i class="fa fa-trophy fa-stack-2x" />
        <i class="fa fa-star fa-stack-1x trophy-star" />
        <strong class="fa-stack-1x trophy-text">{{ level }}</strong>
      </div>
    </div>
    <div data-cy="overallLevelDesc" class="mt-3">
      {{ skillsDisplayPreferences.levelDisplayName }} <Tag severity="info">{{ level }}</Tag> out of <Tag>{{ totalLevels }}</Tag>
    </div>
    <div class="flex justify-content-center mt-2 progress-stars-icons">
      <Rating v-model="level" :stars="totalLevels" readonly :cancel="false" />
    </div>
  </div>
</template>

<style>
.progress-stars-icons .p-rating-icon {
  width:2rem;
  height:2rem;
}
</style>
<style scoped>

/* Font awesome gives a 2em width which doesnt fit the full trophy at font-size:60px. A bug? */
.trophy-stack.fa-stack {
  width: 3em;
  font-size: 60px;
}

.trophy-text {
  margin-top: -0.65em;
  font-size: 0.5em;
  color: #333;
}

.skills-icon {
  display: inline-block;
  color: #b1b1b1;
  margin: 5px 0;
}

.trophy-star {
  color: #ffffff;
  margin-top: -0.35em;
  font-size: 0.9em;
}

.trophy-text {
  margin-top: -0.65em;
  font-size: 0.5em;
  color: #333;
}
</style>