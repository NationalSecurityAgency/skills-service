<template>
  <div class="modal-card">
    <modal-header :title="title" @cancel-clicked="$emit('cancel-clicked')"></modal-header>

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

      <a class="button is-success is-outlined" v-on:click="$emit('save-clicked')" :disabled="errors.any()">
        <span>Save</span>
        <span class="icon is-small">
            <i class="fas fa-arrow-circle-right"/>
          </span>
      </a>
    </div>
  </div>
</template>

<script>
  import ModalHeader from './ModalHeader';

  export default {
    name: 'Modal',
    inject: ['$validator'],
    components: { ModalHeader },
    props: ['title', 'isSaveButtonDisabled'],
  };
</script>

<style lang="scss" scoped>
  @import "../../../styles/palette";

  .modal-card {
    margin-left: 1rem;
    padding-left: 1rem;
    margin-right: 1rem;
    padding-right: 1rem;
    width: 100%;
  }

  .modal-card .label {
    color: $monochrome-palette-color1;
  }

  .modal-card .modal-card-foot {
    background-color: $background-color2;
  }

</style>
