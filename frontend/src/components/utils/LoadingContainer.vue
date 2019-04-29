<template>
  <div class="loading-container">
    <b-spinner  v-if="isLoading" label="Loading..."></b-spinner>
    <div v-if="!isLoading">
      <slot></slot>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'LoadingContainer',
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
