<script setup>
import { computed } from 'vue'

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