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
import Message from 'primevue/message'

const props = defineProps({
  icon: String,
  severity: String,
})

const msgSeverity = computed(() => props.severity || 'info')
const msgIcon = computed(() => {
  if (props.icon) {
    return props.icon
  }
  const severityIcons = {
    success: 'fas fa-check-circle',
    error: 'fas fa-exclamation-triangle',
  };
  return severityIcons[props.severity] || 'fas fa-exclamation-circle';
})
</script>

<template>
  <Message
      class="inline-message"
      :pt="{ root: { class: 'm-0 border-surface rounded-border border-1' }, icon: { class: 'm-0'}}"
      :severity="msgSeverity"
      :icon="msgIcon"><slot /></Message>
</template>

<style scoped>

</style>