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