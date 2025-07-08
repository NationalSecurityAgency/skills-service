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
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

const props = defineProps({
  skill: Object,
  isLocked: Boolean,
})
const attributes = useSkillsDisplayAttributesState()

const isPartiallyAchievedAndLocked = computed(() => {
  return props.isLocked && props.skill.points > 0 && props.skill.points !== props.skill.totalPoints;
})
const isFullyAchievedAndLocked = computed(() => {
  return props.isLocked && props.skill.points === props.skill.totalPoints;
})
const ptsLbl = computed(() => attributes.pointDisplayNamePlural?.toLowerCase())
</script>

<template>
  <div v-if="isPartiallyAchievedAndLocked || isFullyAchievedAndLocked" class="mt-2">
    <Message v-if="isPartiallyAchievedAndLocked" :closable="false">
      You were able to earn partial {{ ptsLbl }} before the prerequisites were added. Don't worry you get to keep the {{ptsLbl}}!!!
      Accomplish all of the prerequisites to unlock the rest of the {{ attributes.skillDisplayName.toLowerCase() }}'s {{ ptsLbl }}!
    </Message>

    <Message v-if="isFullyAchievedAndLocked" :closable="false" severity="success">
      Congrats! You completed this {{ attributes.skillDisplayName.toLowerCase() }} before the prerequisites were added. Don't worry, you get to keep the {{ ptsLbl }}!!!
    </Message>
  </div>
</template>

<style scoped>

</style>