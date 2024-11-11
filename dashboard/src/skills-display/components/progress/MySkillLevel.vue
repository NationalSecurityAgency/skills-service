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
import LevelsProgress from '@/skills-display/components/utilities/LevelsProgress.vue'
import TrophySvgIcon from '@/skills-display/components/progress/TrophySvgIcon.vue'

const props = defineProps({
  userProgress: Object,
})
const attributes = useSkillsDisplayAttributesState()

const level = computed(() => props.userProgress.skillsLevel)
const totalLevels = computed(() => props.userProgress.totalLevels)
</script>

<template>
  <div class="progress-circle-wrapper" data-cy="overallLevel">
    <div class="text-2xl font-medium" data-cy="overallLevelTitle">My {{ attributes.levelDisplayName }}</div>
    <div class="mt-4">
     <TrophySvgIcon :level="level" data-cy="trophyIcon" />
    </div>
    <div data-cy="overallLevelDesc" class="mt-3">
      {{ attributes.levelDisplayName }} <Tag severity="info">{{ level }}</Tag> out of <Tag>{{ totalLevels }}</Tag>
    </div>
    <div class="flex justify-content-center mt-2 overall-progress-stars-icons">
      <LevelsProgress :level="level" :totalLevels="totalLevels" data-cy="overallStars"/>
    </div>
  </div>
</template>

<style>
.overall-progress-stars-icons .p-rating-icon {
  width:2rem;
  height:2rem;
}
</style>
<style scoped>

</style>