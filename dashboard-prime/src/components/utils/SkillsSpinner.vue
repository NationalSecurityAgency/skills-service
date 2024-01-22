<script setup>
import ProgressSpinner from 'primevue/progressspinner';

defineProps(['isLoading']);
</script>

<template>
  <div v-if="isLoading" class="d-flex justify-content-center my-5 text-center">
    <ProgressSpinner aria-label="Loading" style="width: 3rem; height: 3rem;" />
  </div>
</template>

<style scoped></style>
