<template>
  <div class="modal-card">
    <header class="modal-card-head">
      <p class="modal-card-title">{{ title }}</p>
      <button class="delete" aria-label="close" v-on:click="$emit('cancel-clicked')"></button>
    </header>

    <section class="modal-card-body">
      <slot name="content"></slot>
    </section>

    <div class="modal-card-foot skills-justify-content-right">
      <a class="button is-danger  is-outlined" v-on:click="$emit('cancel-clicked')">
        <span>Cancel</span>
        <span class="icon is-small">
              <i class="fas fa-stop-circle"/>
            </span>
      </a>

      <a class="button is-success is-outlined" v-on:click="$emit('save-clicked')">
        <span>Save</span>
        <span class="icon is-small">
            <i class="fas fa-arrow-circle-right"/>
          </span>
      </a>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'Modal',
    props: ['title', 'isSaveButtonDisabled'],
  };
</script>

<style lang="scss" scoped>
  @import "../../styles/palette";

  .modal-card {
    margin-left: 1rem;
    padding-left: 1rem;
    margin-right: 1rem;
    padding-right: 1rem;
  }

  .modal-card .modal-card-head {
    background-color: $green-palette-color4;
  }

  .modal-card .modal-card-head .modal-card-title {
    color: whitesmoke;
  }

  .modal-card .label {
    color: $monochrome-palette-color1;
  }

  .modal-card .modal-card-foot {
    background-color: $background-color2;
  }

</style>
