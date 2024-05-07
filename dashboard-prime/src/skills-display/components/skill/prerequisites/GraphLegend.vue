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