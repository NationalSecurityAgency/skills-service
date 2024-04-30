<script setup>

import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js';

const preferences = useSkillsDisplayPreferencesState()
const props = defineProps({
  skill: Object,
})
</script>

<template>
  <Message v-if="skill.copiedFromProjectId" icon="fas fa-exclamation-circle" severity="info" :closable="false" data-cy="catalogImportStatus">
    <span v-if="skill.selfReporting && skill.selfReporting.enabled">
      This {{ preferences.skillDisplayName.toLowerCase() }} is originally defined in <span class="font-bold font-italic">{{ skill.copiedFromProjectName }}</span> and re-used in this {{ preferences.projectDisplayName.toLowerCase() }}!
      <span v-if="!skill.achievedOn">This {{ preferences.skillDisplayName.toLowerCase() }} can be self-reported via the button below.</span>
    </span>
    <span v-else>
      This {{ preferences.skillDisplayName.toLowerCase() }} is originally defined in <span class="font-bold font-italic">{{ skill.copiedFromProjectName }}</span> and re-used in this {{ preferences.projectDisplayName.toLowerCase() }}!
      Navigate to <span class="font-bold font-italic">{{ skill.copiedFromProjectName }}</span> {{ preferences.projectDisplayName.toLowerCase() }} to perform <span class="font-bold font-italic">{{ skill.skill }}</span> {{ preferences.skillDisplayName.toLowerCase() }}.
    </span>
  </Message>
</template>

<style scoped>

</style>