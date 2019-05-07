<template>
  <div class="loading-container">
    <skills-spinner :is-loading="isLoading"/>
    <div v-if="!isLoading">
      <slot></slot>
    </div>
  </div>
</template>

<script>
  import SkillsSpinner from './SkillsSpinner';

  export default {
    name: 'LoadingContainer',
    components: {
      SkillsSpinner,
    },
    props: ['isLoading'],
  };
</script>

<style scoped>
  .loading-container {
    /* need to be relative for the loading bar to look right */
    position: relative;
    min-height: 5rem;
  }
</style>
