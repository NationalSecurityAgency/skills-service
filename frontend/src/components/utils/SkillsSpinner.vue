<template>
  <div v-if="isLoading" class="d-flex justify-content-center mt-5">
    <b-spinner label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
  </div>
</template>

<script>
  export default {
    name: 'SkillsSpinner',
    props: {
      isLoading: Boolean,
    },
  };
</script>

<style scoped>

</style>
