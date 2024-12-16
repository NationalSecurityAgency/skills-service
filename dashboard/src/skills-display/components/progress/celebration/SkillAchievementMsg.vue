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

import AchievementMsg from "@/skills-display/components/progress/celebration/AchievementMsg.vue";
import {computed} from "vue";
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";

const props = defineProps({
  skill: Object,
})
const timeUtils = useTimeUtils()

const showCelebration = computed(() => props.skill.achievedOn && timeUtils.isWithinNDays(props.skill.achievedOn, 7))
const storageKey = computed(() => `celebration-proj-${props.skill.projectId}-skill-${props.skill.skillId}`)


const ptsTag = computed(() => {
  return `<span class="p-tag p-component">${props.skill.points}</span>`
})
const messages = [
  `Congratulations on earning <points/> points! <span class="font-italic">Happy Learning!</span>`,
  `You've earned <points/> points! No big deal! You are crushing it!`,
  `You're a rockstar! Keep earning those points!`,
  `Way to go! <points/> points and counting!`,
  `Unstoppable! You've collected <points/> points and you're not slowing down!`,
  `You're on fire! Keep earning points and reaching new heights!`,
  `Excellent work! You've earned <points/> points, something to be proud of!`,
  `Celebrate your success! You've earned <points/> points and you deserve it!`,
  `You collected <points/> points! Keep pushing yourself!`,
  `You've earned <points/> points! Keep up the good work!`,
  `You've unlocked <points/> points! Keep striving for more!`,
  `Congratulations on <points/> points! Keep up the excellent work!`,
]
const randomMessage = computed(() => {
  const foundMessage = messages[Math.floor(Math.random() * messages.length)]
  return foundMessage.replace('<points/>', ptsTag.value)
})
</script>

<template>
  <achievement-msg
      :is-enabled="showCelebration"
      icon="fas fa-graduation-cap"
      :storage-key="storageKey" data-cy="skillAchievementCelebrationMsg">
    <template #content>
      <div class="text-left">
        <span v-html="randomMessage"/>
      </div>
    </template>
  </achievement-msg>
</template>

<style scoped>

</style>