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
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'

const themeState = useSkillsDisplayThemeState()

const legendItems = computed(() => {
  return [
    { label: 'Skill', color: themeState.graphSkillColor, iconClass: 'fa-graduation-cap' },
    { label: 'Badge', color: themeState.graphBadgeColor, iconClass: 'fa-award' },
  ]
})

</script>

<template>
  <div class='' data-cy="graphLegend" aria-hidden="true">
    <span class="font-italic text-color-secondary">Legend:</span>
    <span v-for="item in legendItems" v-bind:key="item.label" class="ml-3">
      <Avatar :icon="`fas ${item.iconClass}`" :style="`color: ${item.color}`" />
      <span class="ml-1">{{ item.label }}</span>
    </span>
  </div>
</template>

<style scoped>

</style>