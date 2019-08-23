<template>
  <div class="row p-2 mb-2 border-bottom">
    <div class="col-6 pt-2 col-md-7">
      <span class="h4 text-uppercase">{{ title }}</span>
    </div>
    <div class="col-6 col-md-5 pt-0 text-right">
      <slot>
        <button v-if="action" type="button" :class="{'btn':true, 'btn-outline-primary':true, 'disabled':disabled}" v-on:click="addClicked">
          <span class="d-none d-sm-inline">{{ action }} </span> <i class="fas fa-plus-circle"/>
        </button>
        <i v-if="disabled" class="fas fa-exclamation-circle text-warning ml-1" style="pointer-events: all; font-size: 1.5rem;" v-b-tooltip.hover="disabledMsg"/>
      </slot>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'SubPageHeader',
    props: {
      title: {
        type: String,
        required: true,
      },
      action: {
        type: String,
        required: false,
      },
      disabled: {
        type: Boolean,
        required: false,
        default: false,
      },
      disabledMsg: {
        type: String,
        required: false,
      },
    },
    methods: {
      addClicked() {
        this.$emit('add-action');
      },
    },
  };
</script>

<style scoped>

</style>
