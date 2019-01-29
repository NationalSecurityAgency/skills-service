<template>
  <div class="loading-container">
    <b-loading :is-full-page="false" :active.sync="isLoading" :can-cancel="false"></b-loading>
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
