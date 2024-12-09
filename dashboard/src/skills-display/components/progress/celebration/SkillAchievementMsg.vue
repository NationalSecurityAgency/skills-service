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
import {computed, onMounted} from "vue";
import {useConfettiEffects} from "@/skills-display/components/progress/celebration/UseConfettiEffects.js";
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";
import {useStorage} from "@vueuse/core";
import {useSkillsDisplayThemeState} from "@/skills-display/stores/UseSkillsDisplayThemeState.js";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

const props = defineProps({
  skill: Object,
})
const themeState = useSkillsDisplayThemeState()
const confettiEffects = useConfettiEffects()
const appConfig = useAppConfig()
const timeUtils = useTimeUtils()

const showCelebration = computed(() => props.skill.achievedOn && timeUtils.isWithinNDays(props.skill.achievedOn, 7))
const storageKey = computed(() => `celebration-proj-${props.skill.projectId}-skill-${props.skill.skillId}`)
const confettiAlreadyShown = useStorage(`${storageKey.value}-confetti-shown`, false)
const disableEncouragementsConfetti = computed(() => themeState.theme.disableEncouragementsConfetti || appConfig.disableEncouragementsConfetti)

const showConfetti = () => {
  confettiEffects.simpleConfetti()
}
onMounted(() => {
  if (showCelebration.value && !confettiAlreadyShown.value && !disableEncouragementsConfetti.value) {
    showConfetti()
    confettiAlreadyShown.value = true
  }
})

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
  <achievement-msg v-if="showCelebration" :storage-key="storageKey" data-cy="skillAchievementCelebrationMsg">
    <template #icon>
      <SkillsButton
          @click="showConfetti"
          text
          aria-label="Show Celebration Confetti"
      >
        <i class="text-4xl fas fa-graduation-cap" aria-hidden="true"></i>
      </SkillsButton>
    </template>

    <template #content>
      <div class="text-left">
        <span v-html="getRandomMessage()"/>
      </div>
    </template>
  </achievement-msg>
</template>

<style scoped>

</style>