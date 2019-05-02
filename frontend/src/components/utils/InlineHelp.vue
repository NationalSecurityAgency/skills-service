<template>
  <i class="fas fa-question-circle text-secondary" v-b-tooltip.hover="msg"/>
</template>

<script>
  export default {
    name: 'InlineHelp',
    props: {
      msg: String,
    },
  };
</script>

<style scoped>

</style>
