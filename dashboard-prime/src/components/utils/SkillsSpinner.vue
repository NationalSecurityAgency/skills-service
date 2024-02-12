<script setup>
defineProps({
  isLoading: {
    type: Boolean,
    required: true,
  },
  extraClass: {
    type: String,
    default: 'my-5'
  }
});
</script>

<template>
  <div v-if="isLoading" class="d-flex justify-content-center text-center" :class="extraClass">
    <ProgressSpinner aria-label="Loading" style="width: 3rem; height: 3rem;" />
  </div>
</template>

<style scoped></style>
