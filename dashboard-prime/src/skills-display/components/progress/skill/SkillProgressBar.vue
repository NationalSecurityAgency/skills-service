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
import { computed } from 'vue'
import VerticalProgressBar from '@/skills-display/components/progress/VerticalProgressBar.vue'

const props = defineProps({
  skill: Object,
  isLocked: {
    type: Boolean,
    default: false
  }
})

const isSkillsGroupWithChildren = computed(() => props.skill?.isSkillsGroupType)
const numChildSkillsComplete = computed(() => {
  return isSkillsGroupWithChildren.value ? props.skill.children.filter((childSkill) => childSkill.meta.complete).length : 0
})
const numSkillsRequired = computed(() => {
  if (isSkillsGroupWithChildren.value) {
    return props.skill.numSkillsRequired === -1 ? props.skill.children.length : props.skill.numSkillsRequired
  }
  return 0
})

const progressPercent = computed(() => {
  let totalPts
  if (isSkillsGroupWithChildren.value) {
    totalPts = Math.trunc((numChildSkillsComplete.value / numSkillsRequired.value) * 100)
  } else {
    totalPts = Math.trunc((props.skill.points / props.skill.totalPoints) * 100)
  }
  // this can happen when project admin adjusts skill definition after the points were achieved.
  if (totalPts > 100) {
    totalPts = 100
  }
  return totalPts
})

const progressBeforeToday = computed(() => {
  return ((props.skill.points - props.skill.todaysPoints) / props.skill.totalPoints) * 100;
})

</script>
<template>
  <vertical-progress-bar :total-progress="progressPercent" :total-progress-before-today="progressBeforeToday" :is-locked="isLocked">
  </vertical-progress-bar>
</template>

<style scoped>

</style>