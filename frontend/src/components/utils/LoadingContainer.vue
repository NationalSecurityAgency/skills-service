<template>
  <div class="loading-container">
    <div v-if="isLoading" class="d-flex justify-content-center mt-5">
      <b-spinner label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
    </div>
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
