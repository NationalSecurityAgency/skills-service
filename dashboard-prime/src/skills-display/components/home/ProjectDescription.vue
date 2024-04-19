<script setup>
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'

defineProps({
  description: {
    type: String,
    required: true
  }
})
</script>

<template>
  <Card data-cy="projectDescription" :pt="{ content: { class: 'my-0 py-0'}}">
    <template #content>
      <markdown-text v-if="description" :text="description" />
    </template>
  </Card>
</template>

<style scoped>

</style>