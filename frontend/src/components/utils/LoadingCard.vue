<template>
  <div v-if="loading" class="card h-100 w-100">
    <div class="card-body h-100 text-center">
        <b-spinner variant="info" label="Spinning" style="width: 3rem; height: 3rem;"  class="mt-5"></b-spinner>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'LoadingCard',
    props: {
      loading: Boolean,
    },
  };
</script>

<style scoped>

</style>
