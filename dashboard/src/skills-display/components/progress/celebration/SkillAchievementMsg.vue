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
import {useSkillsDisplayAttributesState} from "@/skills-display/stores/UseSkillsDisplayAttributesState.js";

const props = defineProps({
  skill: Object,
})
const timeUtils = useTimeUtils()
const attributes = useSkillsDisplayAttributesState()

const showCelebration = computed(() => props.skill.achievedOn && timeUtils.isWithinNDays(props.skill.achievedOn, 7))
const storageKey = computed(() => `celebration-proj-${props.skill.projectId}-skill-${props.skill.skillId}`)


const ptsTag = computed(() => {
  return `<span class="p-tag p-component">${props.skill.points}</span>`
})
const ptsLbl = attributes.pointDisplayNamePlural?.toLowerCase()
const messages = [
  `Congratulations on earning <points/> ${ptsLbl}! <span class="font-italic">Happy Learning!</span>`,
  `You've earned <points/> ${ptsLbl}! No big deal! You are crushing it!`,
  `You're a rockstar! Keep earning those ${ptsLbl}!`,
  `Way to go! <points/> ${ptsLbl} and counting!`,
  `Unstoppable! You've collected <points/> ${ptsLbl} and you're not slowing down!`,
  `You're on fire! Keep earning ${ptsLbl} and reaching new heights!`,
  `Excellent work! You've earned <points/> ${ptsLbl}, something to be proud of!`,
  `Celebrate your success! You've earned <points/> ${ptsLbl} and you deserve it!`,
  `You collected <points/> ${ptsLbl}! Keep pushing yourself!`,
  `You've earned <points/> ${ptsLbl}! Keep up the good work!`,
  `You've unlocked <points/> ${ptsLbl}! Keep striving for more!`,
  `Congratulations on <points/> ${ptsLbl}! Keep up the excellent work!`,
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