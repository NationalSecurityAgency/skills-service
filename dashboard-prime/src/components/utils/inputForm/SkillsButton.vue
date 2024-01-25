<script setup>
const props = defineProps({
  label:{
    type: String,
    required: true,
  },
  icon: String,
  iconAfterLabel: {
    type: Boolean,
    default: true,
  },
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
    <span v-if="iconAfterLabel">{{ label }}<i v-if="!loading" class="ml-1" :class="icon" aria-hidden="true" style="width: 0.9rem; height: 0.9rem;"></i></span>
    <span v-else><i v-if="!loading" class="mr-1" :class="icon" aria-hidden="true" style="width: 0.9rem; height: 0.9rem;"></i>{{ label }}</span>
    <ProgressSpinner v-if="loading" style="width: 0.9rem; height: 0.9rem;" class="ml-1" />
  </Button>
</template>

<style scoped>

</style>