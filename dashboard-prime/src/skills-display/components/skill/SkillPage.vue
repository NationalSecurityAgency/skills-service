<script setup>
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'

const displayPreferences = useSkillsDisplayPreferencesState()
</script>

<template>
  <div>
    <skills-title>{{ displayPreferences.skillDisplayName }} Overview</skills-title>
  </div>
</template>

<style scoped>

</style>