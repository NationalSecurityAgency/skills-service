<script setup>
import ProgressSpinner from 'primevue/progressspinner';

defineProps(['isLoading']);
</script>

<template>
  <div v-if="isLoading" class="d-flex justify-content-center mt-5">
    <ProgressSpinner aria-label="Loading" style="width: 3rem; height: 3rem;" />
  </div>
</template>

<style scoped></style>
