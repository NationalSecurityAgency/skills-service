<template>
  <div class="row p-2 mb-2 border-bottom">
    <div class="col-8">
      <span class="h2 text-uppercase">{{ title }}</span>
    </div>
    <div class="col-4 text-right">
      <slot>
        <button v-if="action" type="button" class="btn btn-outline-primary" v-on:click="addClicked">
          <span class="d-none d-sm-inline">{{ action }} </span> <i class="fas fa-plus-circle"/>
        </button>
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
