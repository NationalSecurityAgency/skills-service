<template>
  <div class="text-center pt-5 w-100">
    <div class="text-center">
      <b-spinner variant="info" label="Loading" class="align-middle" style="width: 3rem; height: 3rem;" />
      <h4 class="text-muted mt-2">Loading...</h4>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'MetricsSpinner',
  };
</script>

<style scoped>
.card-img-overlay {
  z-index: 9999 !important;
  background-color: rgba(255,255,255,0.5);
}
</style>
