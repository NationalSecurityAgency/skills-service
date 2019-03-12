<template>
  <header class="modal-card-head" :class="headerClassBackground">
    <p class="modal-card-title">{{ title }}</p>
    <button class="delete" aria-label="close" v-on:click="$emit('cancel-clicked')"></button>
  </header>
</template>

<script>
  export default {
    name: 'ModalHeader',
    props: {
      title: {
        type: String,
      },
      isError: {
        default: false,
        type: Boolean,
      },
    },
    computed: {
      headerClassBackground() {
        return this.isError ? 'modal-card-head-error-bg' : 'modal-card-head-normal-bg';
      },
    },
  };
</script>

<style lang="scss" scoped>
  @import "../../../styles/palette";

  .modal-card-head-normal-bg {
    background-color: $green-palette-color4;
  }

  .modal-card-head-error-bg {
    background-color: $red-palette-color3;
  }

  .modal-card-head .modal-card-title {
    color: whitesmoke;
  }

</style>
