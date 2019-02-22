<template>
  <header class="modal-card-head">
    <p class="modal-card-title">{{ title }}</p>
    <button class="delete" aria-label="close" v-on:click="$emit('cancel-clicked')"></button>
  </header>
</template>

<script>
  export default {
    name: 'ModalHeader',
    props: ['title'],
  };
</script>

<style lang="scss" scoped>
  @import "../../../styles/palette";

  .modal-card-head {
    background-color: $green-palette-color4;
  }

  .modal-card-head .modal-card-title {
    color: whitesmoke;
  }

</style>
