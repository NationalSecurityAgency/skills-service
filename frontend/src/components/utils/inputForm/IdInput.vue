<template>
  <div class="form-group mt-0 mb-0">
    <div class="row">
      <div class="col">
        <label for="idInput">{{ label }}</label>
      </div>
      <div class="col text-right">
        <i class="fas fa-question-circle mr-1 text-secondary"
           v-b-tooltip.hover.left="'Enable to override auto-generated value.'"/>
        <b-link v-if="!canEdit" @click="toggle">Enable</b-link>
        <b-link v-else @click="toggle">Disable</b-link>
      </div>
    </div>
    <input type="text" class="form-control" id="idInput" v-model="internalValue" :disabled="!canEdit"
           @input="dataChanged">
  </div>
</template>

<script>
  export default {
    name: 'IdInput',
    props: {
      label: String,
      value: String,
    },
    data() {
      return {
        canEdit: false,
        internalValue: this.value,
      };
    },
    methods: {
      toggle() {
        this.canEdit = !this.canEdit;
      },
      dataChanged() {
        this.$emit('input', this.internalValue);
      },
    },
    watch: {
      value(newVal) {
        this.internalValue = newVal;
      },
    },
  };
</script>

<style scoped>

</style>
