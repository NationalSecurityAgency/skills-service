<script setup>

import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

const attributes = useSkillsDisplayAttributesState()
const props = defineProps({
  skill: Object,
})
</script>

<template>
  <Message v-if="skill.copiedFromProjectId" icon="fas fa-exclamation-circle" severity="info" :closable="false" data-cy="catalogImportStatus">
    <span v-if="skill.selfReporting && skill.selfReporting.enabled">
      This {{ attributes.skillDisplayName.toLowerCase() }} is originally defined in <span class="font-bold font-italic">{{ skill.copiedFromProjectName }}</span> and re-used in this {{ attributes.projectDisplayName.toLowerCase() }}!
      <span v-if="!skill.achievedOn">This {{ attributes.skillDisplayName.toLowerCase() }} can be self-reported via the button below.</span>
    </span>
    <span v-else>
      This {{ attributes.skillDisplayName.toLowerCase() }} is originally defined in <span class="font-bold font-italic">{{ skill.copiedFromProjectName }}</span> and re-used in this {{ attributes.projectDisplayName.toLowerCase() }}!
      Navigate to <span class="font-bold font-italic">{{ skill.copiedFromProjectName }}</span> {{ attributes.projectDisplayName.toLowerCase() }} to perform <span class="font-bold font-italic">{{ skill.skill }}</span> {{ attributes.skillDisplayName.toLowerCase() }}.
    </span>
  </Message>
</template>

<style scoped>

</style>