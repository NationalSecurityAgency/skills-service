<template>
  <b-tooltip :label="msg" :position="position" animanted="true" type="is-info">
    <span><i class="fas fa-question-circle"></i></span>
  </b-tooltip>
</template>

<script>
  export default {
    name: 'HelpItem',
    props: ['msg', 'position'],
  };
</script>


<style lang="scss" scoped>
  @import "../../styles/palette";

  .fa-question-circle {
    color: $blue-palette-color2;
  }
</style>
