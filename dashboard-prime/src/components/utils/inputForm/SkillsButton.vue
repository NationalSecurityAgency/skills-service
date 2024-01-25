<script setup>
const props = defineProps({
  label: String,
  icon: String,
  loading: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <Button :disabled="disabled || loading">
    <span>{{ label }}</span><i v-if="!loading" class="ml-1" :class="icon" aria-hidden="true" style="width: 0.9rem; height: 0.9rem;"></i>
    <ProgressSpinner v-if="loading" style="width: 0.9rem; height: 0.9rem;" class="ml-1" />
  </Button>
</template>

<style scoped>

</style>