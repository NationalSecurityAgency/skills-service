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


const getPtsTag = () => {
  return `<span class="p-tag p-component">${props.skill.points}</span>`
}
const messages = [
  `Congratulations on earning ${getPtsTag()} points! <span class="font-italic">Happy Learning!</span>`,
  `You've earned ${getPtsTag()} points! No big deal! You'are crushing it!`,
  `You're a rockstar! Keep earning those points!`
]
const getRandomMessage = () => {
  return messages[Math.floor(Math.random() * messages.length)]
}
</script>

<template>
  <achievement-msg
      :is-enabled="showCelebration"
      icon="fas fa-graduation-cap"
      :storage-key="storageKey" data-cy="skillAchievementCelebrationMsg">
    <template #content>
      <div class="text-left">
        <span v-html="getRandomMessage()"/>
      </div>
    </template>
  </achievement-msg>
</template>

<style scoped>

</style>