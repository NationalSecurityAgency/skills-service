<script setup>
defineProps({
  isLoading: {
    type: Boolean,
    required: true,
  },
  sizeInRem: {
    type: Number,
    default: 3
  }
});
</script>

<template>
  <div v-if="isLoading" class="d-flex justify-content-center text-center">
    <ProgressSpinner aria-label="Loading" :style="`width: ${sizeInRem}rem; height: ${sizeInRem}rem;`" />
  </div>
</template>

<style scoped></style>
