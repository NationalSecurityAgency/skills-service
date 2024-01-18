<script setup>
import SkillsSpinner from './SkillsSpinner.vue';

defineProps(['isLoading']);
</script>

<template>
  <div class="loading-container">
    <SkillsSpinner :is-loading="isLoading"/>
    <div v-if="!isLoading">
      <slot></slot>
    </div>
  </div>
</template>

<style scoped>
.loading-container {
  /* need to be relative for the loading bar to look right */
  position: relative;
  min-height: 5rem;
}
</style>
