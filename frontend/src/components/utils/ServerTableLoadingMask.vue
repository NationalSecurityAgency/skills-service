<template>
  <div>
    <div class="loading-overlay"></div>
    <div class="position-absolute align-center spinner">
      <b-spinner label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'ServerTableLoadingMask',
  };
</script>

<style scoped>
  /*
  NOTE - for this component to work properly, the vue-tables2 server table must have the following style applied to it:

  table {
    width: 100%;
    position: relative;
  }

  this will need to be present in the parent component that uses this component
   */
  .loading-overlay {
    position: absolute;
    top: 0;
    width: 100%;
    height: 100%;
    background-color: white;
    opacity: 0.9;
    filter: blur(5px);
  }

  .spinner {
    position: absolute;
    left: 50%;
    right: 50%;
    bottom: 60%;
    top: 40%;
  }
</style>
