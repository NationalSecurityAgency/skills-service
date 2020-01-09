<template>
  <ValidationProvider :rules="rules" v-slot="{ errors }" :name="label">
    <div class="form-group mt-0 mb-0">
      <div class="row">
        <div class="col">
          <label for="idInput">{{ label }}</label>
        </div>
        <div class="col text-right">
          <i class="fas fa-question-circle mr-1 text-secondary"
             v-b-tooltip.hover.left="'Enable to override auto-generated value.'"/>
          <b-link v-if="!canEdit" @click="toggle">Enable</b-link>
          <span v-else>Enabled <i class="fa fa-check fa-sm text-muted"/></span>
        </div>
      </div>
      <input type="text" class="form-control" id="idInput" v-model="internalValue" :disabled="!canEdit"
             @input="dataChanged">
      <small class="form-text text-danger">{{ errors[0]}}</small>
    </div>
  </ValidationProvider>
</template>

<script>
  import { ValidationProvider } from 'vee-validate';

  export default {
    name: 'IdInput',
    components: {
      ValidationProvider,
    },
    props: {
      label: String,
      value: String,
      additionalValidationRules: [String],
    },
    data() {
      return {
        rules: 'required|minIdLength|maxIdLength|alpha_num',
        canEdit: false,
        internalValue: this.value,
      };
    },
    mounted() {
      if (this.additionalValidationRules) {
        this.rules = `${this.rules}|${this.additionalValidationRules}`;
      }
    },
    methods: {
      toggle() {
        this.canEdit = !this.canEdit;
        this.$emit('can-edit', this.canEdit);
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
