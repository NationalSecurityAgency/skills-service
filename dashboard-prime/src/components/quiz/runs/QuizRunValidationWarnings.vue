<script setup>

const props = defineProps({
  errorsToShow: Array,
})
</script>

<template>
  <Message v-if="errorsToShow" severity="error" :closable="false" data-cy="questionErrors">
    <template #messageicon>
      <i class="fas fa-exclamation-triangle text-2xl" aria-hidden="true"></i>
    </template>
    <span class="mx-2">Please fix the following:</span>
    <div v-for="e in errorsToShow" :key="e" class="ml-4">
      - {{ e }}
    </div>
  </Message>
</template>

<style scoped>

</style>